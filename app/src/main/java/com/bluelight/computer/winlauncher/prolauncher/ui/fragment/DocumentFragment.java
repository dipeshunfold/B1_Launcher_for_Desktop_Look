package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.my_interface.IOnBackPressed;
import com.bluelight.computer.winlauncher.prolauncher.ui.adapter.FileAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class DocumentFragment extends BaseFileFragment implements IOnBackPressed {

    private LinearLayout permissionLayout;
    private HorizontalScrollView pathScrollView;
    private LinearLayout pathContainer;
    private boolean isShowingFolders = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_document, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        permissionLayout = view.findViewById(R.id.permissionLayout);
        AppCompatButton btnGrantPermission = view.findViewById(R.id.btnGrantPermission);
        pathScrollView = view.findViewById(R.id.pathScrollView);
        pathContainer = view.findViewById(R.id.pathContainer);

        btnGrantPermission.setOnClickListener(v -> requestStoragePermission());


        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkPermissionAndLoadFiles();
        setupAdapter();
    }

    @Override
    protected void refreshData() {
        loadDocumentFolders();
    }

    private void checkPermissionAndLoadFiles() {
        if (hasStoragePermission()) {
            recyclerView.setVisibility(View.VISIBLE);
            permissionLayout.setVisibility(View.GONE);
            refreshData();
        } else {
            recyclerView.setVisibility(View.GONE);
            permissionLayout.setVisibility(View.VISIBLE);
        }
    }

    private void loadDocumentFolders() {
        isShowingFolders = true;
        updatePathBreadcrumbs(null);
        ArrayList<File> folderList = new ArrayList<>();
        HashSet<File> documentFolders = new HashSet<>();
        ContentResolver contentResolver = requireContext().getContentResolver();
        Uri uri = MediaStore.Files.getContentUri("external");
        String[] mimeTypes = {"application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation", "text/plain"};
        StringBuilder selectionMimeType = new StringBuilder();
        for (int i = 0; i < mimeTypes.length; i++) {
            selectionMimeType.append(MediaStore.Files.FileColumns.MIME_TYPE + "=?");
            if (i < mimeTypes.length - 1) selectionMimeType.append(" OR ");
        }
        String[] projection = {MediaStore.Files.FileColumns.DATA};
        try (Cursor cursor = contentResolver.query(uri, projection, selectionMimeType.toString(), mimeTypes, null)) {
            if (cursor != null) {
                int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
                while (cursor.moveToNext()) {
                    File file = new File(cursor.getString(pathColumn));
                    if (file.exists() && file.getParentFile() != null) {
                        documentFolders.add(file.getParentFile());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        folderList.addAll(documentFolders);
        Collections.sort(folderList, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));

        adapter = new FileAdapter(getContext(), folderList);
        adapter.setOnItemClickListener(position -> {
            if (actionListener != null) actionListener.onListItemClicked();
            File clickedFolder = adapter.getDisplayList().get(position);
            if (clickedFolder.isDirectory()) {
                loadFolderContents(clickedFolder);
            }
        });
        recyclerView.setAdapter(adapter);
        adapter.setOnSelectionChangedListener(this);
    }

    private void loadFolderContents(File folder) {
        isShowingFolders = false;
        updatePathBreadcrumbs(folder);
        ArrayList<File> documentList = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (isDocumentFile(file.getName())) {
                    documentList.add(file);
                }
            }
        }
        Collections.sort(documentList, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));

        adapter = new FileAdapter(getContext(), documentList);
        adapter.setOnItemClickListener(position -> {
            if (actionListener != null) actionListener.onListItemClicked();
            File clickedDocument = adapter.getDisplayList().get(position);
            openFile(clickedDocument);
        });
        recyclerView.setAdapter(adapter);
        adapter.setOnSelectionChangedListener(this);
    }

    private void updatePathBreadcrumbs(@Nullable File folder) {
        pathContainer.removeAllViews();
        if (folder == null) {
            pathScrollView.setVisibility(View.GONE);
            return;
        }
        pathScrollView.setVisibility(View.VISIBLE);
        addPathSegment("Documents", null);
        addPathSeparator();
        addPathSegment(folder.getName(), folder);
    }

    private void addPathSegment(String name, @Nullable File path) {
        if (getContext() == null) return;
        TextView segmentView = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.breadcrumb_segment, pathContainer, false);
        segmentView.setText(name);
        if (path == null) {
            segmentView.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onListItemClicked();
                loadDocumentFolders();
            });
        }
        pathContainer.addView(segmentView);
    }

    private void addPathSeparator() {
        if (getContext() == null) return;
        TextView separatorView = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.breadcrumb_separator, pathContainer, false);
        pathContainer.addView(separatorView);
    }

    @Override
    public boolean onBackPressed() {
        if (!isShowingFolders) {
            loadDocumentFolders();
            return true;
        }
        return false;
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
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.fromParts("package", requireActivity().getPackageName(), null));
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        } else {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
        }
    }

    private void openFile(File file) {
        try {
            Uri fileUri = FileProvider.getUriForFile(requireContext(), requireContext().getApplicationContext().getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String mimeType = getMimeType(file.getAbsolutePath());
            if (mimeType == null) mimeType = "*/*";
            intent.setDataAndType(fileUri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "No app found to open this file type.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error opening file.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private boolean isDocumentFile(String fileName) {
        String ext = getFileExtension(fileName);
        return ext.equals("pdf") || ext.equals("doc") || ext.equals("docx") ||
                ext.equals("xls") || ext.equals("xlsx") || ext.equals("ppt") ||
                ext.equals("pptx") || ext.equals("txt");
    }

    public String getMimeType(String url) {
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
}