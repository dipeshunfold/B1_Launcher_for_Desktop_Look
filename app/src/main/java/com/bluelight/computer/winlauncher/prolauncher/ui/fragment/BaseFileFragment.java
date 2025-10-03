package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.my_interface.FragmentActionListener;
import com.bluelight.computer.winlauncher.prolauncher.ui.adapter.FileAdapter;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class BaseFileFragment extends Fragment implements FileAdapter.OnSelectionChangedListener {
    private final SortType currentSortType = SortType.NAME;
    private final SortOrder currentSortOrder = SortOrder.ASC;
    protected RecyclerView recyclerView;
    protected FileAdapter adapter;
    protected FragmentActionListener actionListener;
    protected LayoutPreferences layoutPreferences;
    private ActivityResultLauncher<IntentSenderRequest> deleteResultLauncher;

    public enum LayoutMode {LIST, GRID}

    protected LayoutMode currentLayoutMode = LayoutMode.GRID;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Fragment parent = getParentFragment();
        if (parent instanceof FragmentActionListener) {
            actionListener = (FragmentActionListener) parent;
        } else {
            throw new RuntimeException("The parent fragment must implement FragmentActionListener");
        }
        layoutPreferences = new LayoutPreferences(context);
        currentLayoutMode = layoutPreferences.getSavedLayoutMode();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setupLayoutManager();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerFileOperationLaunchers();
        setRetainInstance(true); // This helps retain the fragment instance across configuration changes

    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save any necessary state
        outState.putSerializable("layoutMode", currentLayoutMode);
    }

    protected void setupAdapter() {
        if (recyclerView != null) {
            setupLayoutManager();
            recyclerView.setAdapter(adapter);
        }
    }


    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            // Restore any saved state
            currentLayoutMode = (LayoutMode) savedInstanceState.getSerializable("layoutMode");
            setupLayoutManager();
        }
    }

//    private void setupLayoutManager() {
//        int spanCount = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 5 : 3;
//        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), spanCount);
//        recyclerView.setLayoutManager(layoutManager);
//    }


    protected void setupLayoutManager() {
        if (currentLayoutMode == LayoutMode.LIST) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        } else {
            int spanCount = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 5 : 3;
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), spanCount));
        }

        if (adapter != null) {
            adapter.setLayoutMode(currentLayoutMode);
        }
    }

    public void filter(String query) {
        if (adapter != null) {
            adapter.filter(query);
        }
    }

    public void sortFileList(SortType newSortType) {
        if (adapter != null) {
            adapter.sort(newSortType);
        } else {
            Toast.makeText(getContext(), "Adapter not initialized.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSelectionChanged(int count) {
        if (actionListener != null) {
            actionListener.onSelectionChanged(count);
        }
    }

    public FileAdapter getAdapter() {
        return adapter;
    }

    protected void setAdapter(FileAdapter newAdapter) {
        this.adapter = newAdapter;
        recyclerView.setAdapter(this.adapter);
        applyLayoutManager();
    }

    private void registerFileOperationLaunchers() {
        deleteResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Toast.makeText(getContext(), "Files deleted successfully", Toast.LENGTH_SHORT).show();
                        refreshData();
                    } else {
                        Toast.makeText(getContext(), "Delete request was cancelled or failed.", Toast.LENGTH_SHORT).show();
                    }
                    if (adapter != null) adapter.clearSelections();
                });
    }

    public void deleteSelectedFiles(List<File> filesToDelete) {
        if (filesToDelete.isEmpty()) {
            Toast.makeText(getContext(), "No files selected", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Files")
                .setMessage("Delete " + filesToDelete.size() + " selected files?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    int deletedCount = 0;
                    for (File file : filesToDelete) {
                        if (file.exists() && file.delete()) {
                            requireContext().getContentResolver().delete(MediaStore.Files.getContentUri("external"), MediaStore.Files.FileColumns.DATA + "=?", new String[]{file.getAbsolutePath()});
                            deletedCount++;
                        }
                    }
                    Toast.makeText(getContext(), deletedCount + "/" + filesToDelete.size() + " files deleted", Toast.LENGTH_SHORT).show();
                    if (deletedCount > 0) refreshData();
                    if (adapter != null) adapter.clearSelections();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void renameSelectedFile(File fileToRename) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_rename, null);
        EditText input = dialogView.findViewById(R.id.rename_input);
        input.setText(fileToRename.getName());
        Button btnOk = dialogView.findViewById(R.id.btn_ok);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        AlertDialog renameDialog = new AlertDialog.Builder(getContext()).setView(dialogView).create();
        btnOk.setOnClickListener(v -> {
            String newName = input.getText().toString().trim();
            if (newName.isEmpty() || newName.equals(fileToRename.getName())) {
                input.setError("Enter a different name");
                return;
            }
            File newFile = new File(fileToRename.getParentFile(), newName);
            if (fileToRename.renameTo(newFile)) {
                requireContext().getContentResolver().delete(MediaStore.Files.getContentUri("external"), MediaStore.Files.FileColumns.DATA + "=?", new String[]{fileToRename.getAbsolutePath()});
                Intent scan = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                scan.setData(Uri.fromFile(newFile));
                requireContext().sendBroadcast(scan);
                Toast.makeText(getContext(), "Renamed to “" + newName + "”", Toast.LENGTH_SHORT).show();
                refreshData();
            } else {
                Toast.makeText(getContext(), "Rename failed", Toast.LENGTH_SHORT).show();
            }
            if (adapter != null) adapter.clearSelections();
            renameDialog.dismiss();
        });
        btnCancel.setOnClickListener(v -> renameDialog.dismiss());
        renameDialog.show();
    }

    public void shareSelectedFiles(List<File> filesToShare) {
        if (filesToShare.isEmpty()) return;
        ArrayList<Uri> uris = new ArrayList<>();
        for (File file : filesToShare) {
            try {
                Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", file);
                uris.add(uri);
            } catch (IllegalArgumentException e) {
                Toast.makeText(getContext(), "Error sharing " + file.getName(), Toast.LENGTH_LONG).show();
                return;
            }
        }
        Intent intent = new Intent();
        if (uris.size() == 1) {
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
            intent.setType(getMimeType(uris.get(0).toString()));
        } else {
            intent.setAction(Intent.ACTION_SEND_MULTIPLE);
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            intent.setType("*/*");
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Share Files..."));
        if (adapter != null) adapter.clearSelections();
    }

    public void showPropertiesDialog(File file) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.dialog_properties, null);
        ImageView dialogIcon = view.findViewById(R.id.dialog_icon);
        TextView dialogFileName = view.findViewById(R.id.dialog_file_name);
        TextView dialogFileType = view.findViewById(R.id.dialog_file_type);
        TextView dialogFileSize = view.findViewById(R.id.dialog_file_size);
        TableRow dialogRowDimensions = view.findViewById(R.id.dialog_row_dimensions);
        TextView dialogDimensions = view.findViewById(R.id.dialog_dimensions);
        TextView dialogFilePath = view.findViewById(R.id.dialog_file_path);
        TextView dialogModifiedDate = view.findViewById(R.id.dialog_modified_date);
        Button okButton = view.findViewById(R.id.dialog_ok_button);
        String fileName = file.getName();
        String extension = getFileExtension(fileName);
        String humanReadableType = getHumanReadableFileType(extension);
        String formattedSize = Formatter.formatFileSize(getContext(), file.length());
        String path = file.getParent();
        String formattedDate = new SimpleDateFormat("MMM dd, yyyy, hh:mm:ss a", Locale.getDefault()).format(new Date(file.lastModified()));
        dialogFileName.setText(fileName);
        dialogFileType.setText(humanReadableType);
        dialogFileSize.setText(formattedSize + " (" + String.format("%,d", file.length()) + " bytes)");
        dialogFilePath.setText(path);
        dialogModifiedDate.setText(formattedDate);
        dialogIcon.setImageResource(getFileIconResource(extension));
        dialogFileName.setSelected(true);
        if (isImageFile(extension)) {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                int width = options.outWidth;
                int height = options.outHeight;
                if (width > 0 && height > 0) {
                    dialogDimensions.setText(width + " x " + height + " pixels");
                    dialogRowDimensions.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) { /* Ignore */ }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(view);
        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        okButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        if (adapter != null) {
            adapter.clearSelections();
        }
    }

    protected Uri getContentUriFromFile(File file) {
        String filePath = file.getAbsolutePath();
        Uri filesUri = MediaStore.Files.getContentUri("external");
        String[] projection = {MediaStore.Files.FileColumns._ID};
        String selection = MediaStore.Files.FileColumns.DATA + "=?";
        String[] selectionArgs = {filePath};
        try (Cursor cursor = requireContext().getContentResolver().query(filesUri, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
                long id = cursor.getLong(idColumn);
                return ContentUris.withAppendedId(filesUri, id);
            }
        } catch (Exception e) {
            Log.e("BaseFileFragment", "Error getting content URI", e);
        }
        return null;
    }

    protected abstract void refreshData();

    protected String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        }
        return type;
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }

    private String getHumanReadableFileType(String extension) {
        switch (extension.toLowerCase()) {
            case "jpg":
            case "jpeg":
                return "JPEG Image";
            case "png":
                return "PNG Image";
            case "gif":
                return "GIF Image";
            case "bmp":
                return "Bitmap Image";
            case "webp":
                return "WebP Image";
            case "mp4":
                return "MP4 Video";
            case "mkv":
                return "MKV Video";
            case "mp3":
                return "MP3 Audio";
            case "wav":
                return "WAV Audio";
            case "pdf":
                return "PDF Document";
            case "txt":
                return "Text Document";
            case "doc":
            case "docx":
                return "Word Document";
            case "xls":
            case "xlsx":
                return "Excel Spreadsheet";
            case "zip":
                return "ZIP Archive";
            case "rar":
                return "RAR Archive";
            case "apk":
                return "Android Package";
            default:
                if (extension.isEmpty()) return "File";
                return extension.toUpperCase() + " File";
        }
    }

    private boolean isImageFile(String extension) {
        String ext = extension.toLowerCase();
        return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("gif") || ext.equals("bmp") || ext.equals("webp");
    }

    private int getFileIconResource(String extension) {
        Map<String, Integer> iconMap = new HashMap<>();
        iconMap.put("pdf", R.drawable.ic_file_pdf);
        iconMap.put("txt", R.drawable.ic_file_txt);
        iconMap.put("doc", R.drawable.ic_file_word);
        iconMap.put("docx", R.drawable.ic_file_word);
        iconMap.put("xls", R.drawable.ic_file_excel);
        iconMap.put("xlsx", R.drawable.ic_file_excel);
        iconMap.put("zip", R.drawable.ic_file_zip);
        iconMap.put("rar", R.drawable.ic_file_rar);
        iconMap.put("apk", R.drawable.ic_file_apk);
        iconMap.put("mp3", R.drawable.ic_audios);
        iconMap.put("wav", R.drawable.ic_audios);
        if (isImageFile(extension)) {
            return R.drawable.ic_image;
        }
        Integer iconRes = iconMap.get(extension.toLowerCase());
        return (iconRes != null) ? iconRes : R.drawable.ic_doccuement;
    }

    private void applyLayoutManager() {
        if (currentLayoutMode == LayoutMode.LIST) {


            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        } else { // GRID mode
            int spanCount = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 5 : 3;
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), spanCount));
        }

        if (adapter != null) {
            adapter.setLayoutMode(currentLayoutMode);
        }
    }

//    public LayoutMode toggleLayoutMode() {
//        currentLayoutMode = (currentLayoutMode == LayoutMode.LIST) ? LayoutMode.GRID : LayoutMode.LIST;
//        applyLayoutManager();
//        return currentLayoutMode;
//    }


    public LayoutMode toggleLayoutMode() {
        currentLayoutMode = (currentLayoutMode == LayoutMode.LIST) ? LayoutMode.GRID : LayoutMode.LIST;
        setupLayoutManager();
        return currentLayoutMode;
    }


    public enum SortType {NAME, DATE, SIZE}


    public enum SortOrder {ASC, DESC}


//    public enum LayoutMode {LIST, GRID}

}