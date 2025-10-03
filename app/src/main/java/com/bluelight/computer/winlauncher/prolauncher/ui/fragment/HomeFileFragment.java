package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.ChangeBounds;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.model.AudioItem;
import com.bluelight.computer.winlauncher.prolauncher.my_interface.FragmentActionListener;
import com.bluelight.computer.winlauncher.prolauncher.my_interface.IOnBackPressed;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HomeFileFragment extends Fragment implements FragmentActionListener {

    private static final int LEGACY_STORAGE_PERMISSION_CODE = 101;
    private LinearLayout lnrName;
    private ImageView btnNavigation, btnBack, btnMinimize, btnResize, btnClose, btnGridIcon;
    private TextView tvCurrentFolder;
    private EditText etSearch;
    private LinearLayout lnrThisPc;
    private boolean isNavVisible = false;
    private LinearLayout lnrRename, lnrDelete, lnrShare, lnrProperties, lnrShort, lnrGrid;
    private ActivityResultLauncher<Intent> manageStorageResultLauncher;
    private ViewGroup parentLayout;

    private Fragment pendingFragment = null;
    private String pendingTitle = null;
    private LayoutPreferences layoutPreferences;
    private View lnrInfo;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        layoutPreferences = new LayoutPreferences(requireContext());

        manageStorageResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (Environment.isExternalStorageManager()) {
                            Toast.makeText(getContext(), "All Files Access Granted", Toast.LENGTH_SHORT).show();
                            executePendingAction();
                        } else {
                            Toast.makeText(getContext(), "Permission is required to browse files.", Toast.LENGTH_LONG).show();
                            clearPendingAction();
                        }
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home_file, container, false);
        initializeViews(rootView);
        setupListeners();
        requestLoadFragment(new ThisPcFragment(), "This PC");
        return rootView;
    }

    private void initializeViews(View view) {
        lnrName = view.findViewById(R.id.lnrName);

        btnNavigation = view.findViewById(R.id.btnNavigation);
        btnBack = view.findViewById(R.id.btnBack);
        btnMinimize = view.findViewById(R.id.btnMinimize);
        btnResize = view.findViewById(R.id.btnResize);
        btnClose = view.findViewById(R.id.btnClose);
        tvCurrentFolder = view.findViewById(R.id.tvCurrentFolder);
        etSearch = view.findViewById(R.id.et_search);
        lnrThisPc = view.findViewById(R.id.lnrThisPc);
        lnrRename = view.findViewById(R.id.lnrRename);
        lnrDelete = view.findViewById(R.id.lnrDelete);
        lnrShare = view.findViewById(R.id.lnrShare);
        lnrProperties = view.findViewById(R.id.lnrProperties);
        lnrShort = view.findViewById(R.id.lnrShort);
        lnrGrid = view.findViewById(R.id.lnrGrid);
        btnGridIcon = view.findViewById(R.id.btnGrid);
        parentLayout = view.findViewById(R.id.nav_drawer_container);

        updateGridButtonIcon(layoutPreferences.getSavedLayoutMode());

        view.findViewById(R.id.btnThisPc).setOnClickListener(v -> requestLoadFragment(new ThisPcFragment(), "This PC"));
        view.findViewById(R.id.btnPhotos).setOnClickListener(v -> requestLoadFragment(new PhotoFragment(), "Pictures"));
        view.findViewById(R.id.btnVideos).setOnClickListener(v -> requestLoadFragment(new VideoFragment(), "Videos"));
        view.findViewById(R.id.btnAudios).setOnClickListener(v -> requestLoadFragment(new AudioFragment(), "Music"));
        view.findViewById(R.id.btnDocuments).setOnClickListener(v -> requestLoadFragment(new DocumentFragment(), "Documents"));
        view.findViewById(R.id.btnDownload).setOnClickListener(v -> requestLoadFragment(new DownloadFragment(), "Download"));
        view.findViewById(R.id.btnPhoneStorage).setOnClickListener(v -> requestLoadFragment(new PhoneStorageFragment(), "Local Disk (C:)"));
        view.findViewById(R.id.btnSdcard).setOnClickListener(v -> requestLoadFragment(new SdcardFragment(), "SD Card (D:)"));
        view.findViewById(R.id.txtThisPc).setOnClickListener(v -> requestLoadFragment(new ThisPcFragment(), "This PC"));
        view.findViewById(R.id.txtPhotos).setOnClickListener(v -> requestLoadFragment(new PhotoFragment(), "Pictures"));
        view.findViewById(R.id.txtVideos).setOnClickListener(v -> requestLoadFragment(new VideoFragment(), "Videos"));
        view.findViewById(R.id.txtAudios).setOnClickListener(v -> requestLoadFragment(new AudioFragment(), "Music"));
        view.findViewById(R.id.txtDocuments).setOnClickListener(v -> requestLoadFragment(new DocumentFragment(), "Documents"));
        view.findViewById(R.id.txtDownload).setOnClickListener(v -> requestLoadFragment(new DownloadFragment(), "Download"));
        view.findViewById(R.id.txtPhoneStorage).setOnClickListener(v -> requestLoadFragment(new PhoneStorageFragment(), "Local Disk (C:)"));
        view.findViewById(R.id.txtSdcard).setOnClickListener(v -> requestLoadFragment(new SdcardFragment(), "SD Card (D:)"));

        if (isSdCardAvailable()) {
            view.findViewById(R.id.txtSdcard).setVisibility(View.VISIBLE);
            view.findViewById(R.id.btnSdcard).setVisibility(View.VISIBLE);
        } else {
            view.findViewById(R.id.txtSdcard).setVisibility(View.GONE);
            view.findViewById(R.id.btnSdcard).setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        btnNavigation.setOnClickListener(v -> toggleNavigation());
        btnBack.setOnClickListener(v -> onBackPressed());
        btnMinimize.setOnClickListener(v -> minimizeFragment());
        btnResize.setOnClickListener(v -> resizeFragment());
        btnClose.setOnClickListener(v -> closeFragment());
        lnrRename.setOnClickListener(v -> requestRename());
        lnrDelete.setOnClickListener(v -> requestDelete());
        lnrShare.setOnClickListener(v -> requestShare());
        lnrProperties.setOnClickListener(v -> requestProperties());
        lnrShort.setOnClickListener(v -> showSortDialog());

        lnrGrid.setOnClickListener(v -> {
            BaseFileFragment activeFragment = getActiveBaseFileFragment();
            if (activeFragment != null) {
                BaseFileFragment.LayoutMode newMode = activeFragment.toggleLayoutMode();
                layoutPreferences.saveLayoutMode(newMode);
                updateGridButtonIcon(newMode);
            }
        });


        etSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                lnrThisPc.setVisibility(View.GONE);
            } else {
                lnrThisPc.setVisibility(View.VISIBLE);
                hideKeyboard(v);
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                BaseFileFragment activeFragment = getActiveBaseFileFragment();
                if (activeFragment != null) {
                    activeFragment.filter(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    etSearch.clearFocus();
                }
            }
        });
    }

    private void updateGridButtonIcon(BaseFileFragment.LayoutMode mode) {
        if (btnGridIcon == null) return;
        if (mode == BaseFileFragment.LayoutMode.GRID) {
            btnGridIcon.setImageResource(R.drawable.ic_list_view);
        } else {
            btnGridIcon.setImageResource(R.drawable.layout);
        }
    }

    private void hideKeyboard(View view) {
        if (getContext() != null) {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    public void onBackPressed() {
        if (!isAdded()) return;
        if (etSearch.hasFocus() || etSearch.getText().length() > 0) {
            etSearch.setText("");
            etSearch.clearFocus();
            return;
        }

        FragmentManager fm = getChildFragmentManager();
        Fragment currentFragment = fm.findFragmentById(R.id.content_container);

        if (currentFragment instanceof IOnBackPressed) {
            if (((IOnBackPressed) currentFragment).onBackPressed()) {
                return;
            }
        }

        if (fm.getBackStackEntryCount() > 1) {
            fm.popBackStack();
            fm.executePendingTransactions();
            if (fm.getBackStackEntryCount() > 0) {
                FragmentManager.BackStackEntry entry = fm.getBackStackEntryAt(fm.getBackStackEntryCount() - 1);
                tvCurrentFolder.setText(entry.getName());
            }
        } else {
            closeFragment();
        }
        onSelectionChanged(0);
        etSearch.setText("");
    }

    @Override
    public void requestLoadFragment(Fragment fragment, String title) {
        if (hasStoragePermission()) {
            loadFragmentInternal(fragment, title);
        } else {
            this.pendingFragment = fragment;
            this.pendingTitle = title;
            requestStoragePermission();
        }
    }

    private void loadFragmentInternal(Fragment fragment, String title) {
        if (!isAdded()) return;
        FragmentManager fm = getChildFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.content_container, fragment, title);
        ft.addToBackStack(title);
        ft.commit();
        tvCurrentFolder.setText(title);
        btnBack.setVisibility(View.VISIBLE);
        onSelectionChanged(0);
        etSearch.setText("");
        etSearch.clearFocus();
    }

    @Override
    public void onListItemClicked() {
        etSearch.clearFocus();
    }

    private void showSortDialog() {
        BaseFileFragment activeFragment = getActiveBaseFileFragment();
        if (activeFragment == null) {
            Toast.makeText(getContext(), "No active folder to sort.", Toast.LENGTH_SHORT).show();
            return;
        }
        final CharSequence[] options = {"Sort by Name", "Sort by Date", "Sort by Size"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Sort Files");
        builder.setItems(options, (dialog, item) -> {
            switch (item) {
                case 0:
                    activeFragment.sortFileList(BaseFileFragment.SortType.NAME);
                    break;
                case 1:
                    activeFragment.sortFileList(BaseFileFragment.SortType.DATE);
                    break;
                case 2:
                    activeFragment.sortFileList(BaseFileFragment.SortType.SIZE);
                    break;
            }
        });
        builder.show();
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            showManageStorageRationale();
        } else {
            showLegacyStorageRationale();
        }
    }

    private void showManageStorageRationale() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("All Files Access Required")
                .setMessage("To browse and manage all your files, this app needs special permission. Please tap 'Go to Settings' and enable 'Allow access to manage all files'.")
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse(String.format("package:%s", requireContext().getPackageName())));
                        manageStorageResultLauncher.launch(intent);
                    } catch (Exception e) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        manageStorageResultLauncher.launch(intent);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Toast.makeText(getContext(), "Permission denied. File browsing is disabled.", Toast.LENGTH_SHORT).show();
                    clearPendingAction();
                })
                .setCancelable(false)
                .show();
    }

    private void showLegacyStorageRationale() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Storage Permission Needed")
                .setMessage("This app needs permission to read your storage to display your files.")
                .setPositiveButton("OK", (dialog, which) -> requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, LEGACY_STORAGE_PERMISSION_CODE))
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Toast.makeText(getContext(), "Permission denied. File browsing is disabled.", Toast.LENGTH_SHORT).show();
                    clearPendingAction();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LEGACY_STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "Permission Granted", Toast.LENGTH_SHORT).show();
                executePendingAction();
            } else {
                Toast.makeText(getContext(), "Permission is required to browse files.", Toast.LENGTH_SHORT).show();
                clearPendingAction();
            }
        }
    }

    private void executePendingAction() {
        if (pendingFragment != null && pendingTitle != null) {
            loadFragmentInternal(pendingFragment, pendingTitle);
        }
        clearPendingAction();
    }

    private void clearPendingAction() {
        this.pendingFragment = null;
        this.pendingTitle = null;
    }

    private boolean isSdCardAvailable() {
        if (!isAdded()) return false;
        File[] externalStorageFiles = ContextCompat.getExternalFilesDirs(requireContext(), null);
        for (File file : externalStorageFiles) {
            if (file != null && Environment.isExternalStorageRemovable(file)) {
                String state = Environment.getExternalStorageState(file);
                if (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void toggleNavigation() {
        long duration = 250;
        ChangeBounds changeBoundsTransition = new ChangeBounds();
        changeBoundsTransition.setDuration(duration);
        changeBoundsTransition.setInterpolator(new AccelerateDecelerateInterpolator());
        if (isNavVisible) {
            lnrName.animate().alpha(0f).setDuration(duration / 2).withEndAction(() -> {
                TransitionManager.beginDelayedTransition(parentLayout, changeBoundsTransition);
                lnrName.setVisibility(View.GONE);
            });
        } else {
            lnrName.setAlpha(0f);
            lnrName.setVisibility(View.VISIBLE);
            TransitionManager.beginDelayedTransition(parentLayout, changeBoundsTransition);
            lnrName.animate().alpha(1f).setDuration(duration / 2).setStartDelay(duration / 3);
        }
        isNavVisible = !isNavVisible;
    }

    private void closeFragment() {
        if (getParentFragmentManager() != null) {
            getParentFragmentManager().popBackStack();
        }
    }

    @Override
    public void onImageClicked(ArrayList<File> imageList, int position) {
        PhotoFragment photoFragment = new PhotoFragment();
        requestLoadFragment(photoFragment, "Pictures");
        if (getView() != null) {
            getView().post(() -> photoFragment.showImageInViewPager(imageList, position));
        }
    }

    @Override
    public void onVideoClicked(ArrayList<File> videoList, int position) {
        VideoFragment videoFragment = new VideoFragment();
        requestLoadFragment(videoFragment, "Videos");
        if (getView() != null) {
            getView().post(() -> videoFragment.playVideoInViewPager(videoList, position));
        }
    }

    @Override
    public void onAudioClicked(ArrayList<AudioItem> audioItems, int position) {
        AudioFragment audioFragment = new AudioFragment();
        requestLoadFragment(audioFragment, "Music");
        if (getView() != null) {
            getView().post(() -> audioFragment.showAudioPlayer(audioItems, position));
        }
    }

    @Override
    public void onSelectionChanged(int count) {
        boolean singleSelection = count == 1;
        lnrRename.setEnabled(singleSelection);
        lnrProperties.setEnabled(singleSelection);
        lnrRename.setAlpha(singleSelection ? 1.0f : 0.5f);
        lnrProperties.setAlpha(singleSelection ? 1.0f : 0.5f);
        boolean anySelection = count > 0;
        lnrDelete.setEnabled(anySelection);
        lnrShare.setEnabled(anySelection);
        lnrDelete.setAlpha(anySelection ? 1.0f : 0.5f);
        lnrShare.setAlpha(anySelection ? 1.0f : 0.5f);
    }

    private BaseFileFragment getActiveBaseFileFragment() {
        if (!isAdded()) return null;
        Fragment fragment = getChildFragmentManager().findFragmentById(R.id.content_container);
        if (fragment instanceof BaseFileFragment) {
            return (BaseFileFragment) fragment;
        }
        return null;
    }

    @Override
    public void requestDelete() {
        BaseFileFragment activeFragment = getActiveBaseFileFragment();
        if (activeFragment != null && activeFragment.getAdapter() != null) {
            List<File> selectedFiles = activeFragment.getAdapter().getSelectedFiles();
            if (!selectedFiles.isEmpty()) {
                activeFragment.deleteSelectedFiles(selectedFiles);
            }
        }
    }

    @Override
    public void requestShare() {
        BaseFileFragment activeFragment = getActiveBaseFileFragment();
        if (activeFragment != null && activeFragment.getAdapter() != null) {
            List<File> selectedFiles = activeFragment.getAdapter().getSelectedFiles();
            if (!selectedFiles.isEmpty()) {
                activeFragment.shareSelectedFiles(selectedFiles);
            }
        }
    }

    @Override
    public void requestRename() {
        BaseFileFragment activeFragment = getActiveBaseFileFragment();
        if (activeFragment != null && activeFragment.getAdapter() != null) {
            List<File> selectedFiles = activeFragment.getAdapter().getSelectedFiles();
            if (selectedFiles.size() == 1) {
                activeFragment.renameSelectedFile(selectedFiles.get(0));
            } else {
                Toast.makeText(getContext(), "Please select exactly one file to rename.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void requestProperties() {
        BaseFileFragment activeFragment = getActiveBaseFileFragment();
        if (activeFragment != null && activeFragment.getAdapter() != null) {
            List<File> selectedFiles = activeFragment.getAdapter().getSelectedFiles();
            if (selectedFiles.size() == 1) {
                activeFragment.showPropertiesDialog(selectedFiles.get(0));
            } else {
                Toast.makeText(getContext(), "Please select exactly one file for properties.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void minimizeFragment() {
        if (getView() != null) getView().setVisibility(View.GONE);
    }

    private void resizeFragment() {
        View view = getView();
        if (view == null) return;
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params.height == ViewGroup.LayoutParams.MATCH_PARENT) {
            params.height = (int) (350 * getResources().getDisplayMetrics().density);
        } else {
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        }
        view.setLayoutParams(params);
    }
}