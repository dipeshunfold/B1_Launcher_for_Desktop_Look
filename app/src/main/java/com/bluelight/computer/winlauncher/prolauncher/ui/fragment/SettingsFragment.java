package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.model.SettingsMenu;

import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends Fragment {


    private RecyclerView navRecyclerView;
    private SettingsMenuAdapter menuAdapter;
    private List<SettingsMenu> menuItems;
    private TextView contentTitle;
    private EditText etSearch;
    private ImageView btnBack, btnSettingRate, btnSettingShare, btnSettingClose;
    private FrameLayout settingsFragmentContainer;


    private OnSettingsClosedListener mListener;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof OnSettingsClosedListener) {

            mListener = (OnSettingsClosedListener) context;
        } else {

            throw new RuntimeException(context + " must implement OnSettingsClosedListener");
        }
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        navRecyclerView = view.findViewById(R.id.nav_recycler_view);
        contentTitle = view.findViewById(R.id.content_title);
        etSearch = view.findViewById(R.id.et_search);
        settingsFragmentContainer = view.findViewById(R.id.settings_fragment_container);
        btnBack = view.findViewById(R.id.btnBack);
        btnSettingClose = view.findViewById(R.id.btnSettingClose);
        btnSettingShare = view.findViewById(R.id.btnSettingShare);
        btnSettingRate = view.findViewById(R.id.btnSettingRate);


        setupNavigationMenu();
        setupClickListeners();
        setupSearch();


        if (savedInstanceState == null && !menuItems.isEmpty()) {
            loadSettingsFragment(menuItems.get(0));
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mListener = null;
    }

    private void setupClickListeners() {

        btnBack.setOnClickListener(v -> closeFragment());
        btnSettingClose.setOnClickListener(v -> closeFragment());


        btnSettingShare.setOnClickListener(v -> shareApp());
        btnSettingRate.setOnClickListener(v -> rateApp());


        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

                closeFragment();
            }
        });
    }


    private void setupNavigationMenu() {
        navRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        menuItems = new ArrayList<>();

        menuItems.add(new SettingsMenu(R.drawable.ic_personalization, "Personalization", PersonalizationFragment.class, "wallpapers taskbar color desktop grid size hide icons theme"));
        menuItems.add(new SettingsMenu(R.drawable.ic_system, "System", SystemFragment.class, "set default launcher system setting notification settings show status bar & nav keys"));
        menuItems.add(new SettingsMenu(R.drawable.ic_apps, "Apps", AppsFragment.class, "show recent apps"));
        menuItems.add(new SettingsMenu(R.drawable.ic_advanced, "Advance Features", AdvanceFeaturesFragment.class, "enable cortana transparent taskbar show contact icon show taskbar time"));
        menuItems.add(new SettingsMenu(R.drawable.ic_general, "General", GeneralFragment.class, "desktop item text color date formate device name"));
        menuItems.add(new SettingsMenu(R.drawable.ic_backup, "Backup & Restore", BackupRestoreFragment.class, "backup data restore data"));

        menuAdapter = new SettingsMenuAdapter(requireContext(), menuItems, this::loadSettingsFragment);
        navRecyclerView.setAdapter(menuAdapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                menuAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });


        menuAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (menuAdapter.getItemCount() == 1) {
                    settingsFragmentContainer.setVisibility(View.VISIBLE);
                    loadSettingsFragment(menuAdapter.getItem(0));
                } else if (menuAdapter.getItemCount() == 0) {
                    settingsFragmentContainer.setVisibility(View.GONE);
                    contentTitle.setText("No results found");
                } else {
                    settingsFragmentContainer.setVisibility(View.VISIBLE);
                }
            }
        });
    }


    private void closeFragment() {
        if (mListener != null) {
            mListener.onSettingsClosed();
        }
    }


    private void loadSettingsFragment(SettingsMenu item) {
        if (item == null || item.getFragmentClass() == null) return;
        contentTitle.setVisibility(View.VISIBLE);
        contentTitle.setText(item.getTitle());
        try {
            Fragment fragment = item.getFragmentClass().getConstructor().newInstance();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.settings_fragment_container, fragment)
                    .commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void shareApp() {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
            String shareMessage = "Check out this awesome launcher I'm using!\n\n";
            String packageName = requireActivity().getPackageName();
            shareMessage += "https://play.google.com/store/apps/details?id=" + packageName;
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void rateApp() {
        String packageName = requireActivity().getPackageName();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
        }
    }


    public interface OnSettingsClosedListener {
        void onSettingsClosed();
    }
}