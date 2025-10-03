package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.model.AudioItem;
import com.bluelight.computer.winlauncher.prolauncher.my_interface.IOnBackPressed;
import com.bluelight.computer.winlauncher.prolauncher.ui.adapter.FileAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Collectors;

public class DownloadFragment extends BaseFileFragment implements IOnBackPressed {
    private HorizontalScrollView pathScrollView;
    private LinearLayout pathContainer;
    private boolean isShowingFolders = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_download, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        pathScrollView = view.findViewById(R.id.pathScrollView);
        pathContainer = view.findViewById(R.id.pathContainer);


        refreshData();
        setupAdapter();
        return view;
    }

    @Override
    protected void refreshData() {
        loadDownloadFolders();
    }

    private void loadDownloadFolders() {
        isShowingFolders = true;
        updatePathBreadcrumbs(null);
        ArrayList<File> folderList = new ArrayList<>();
        HashSet<File> downloadFolders = new HashSet<>();
        ContentResolver contentResolver = requireContext().getContentResolver();
        Uri uri = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Downloads.DATA};

        try (Cursor cursor = contentResolver.query(uri, projection, null, null, null)) {
            if (cursor != null) {
                int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATA);
                while (cursor.moveToNext()) {
                    String path = cursor.getString(pathColumn);
                    if (path != null) {
                        File file = new File(path);
                        if (file.exists() && file.getParentFile() != null) {
                            downloadFolders.add(file.getParentFile());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Failed to load download folders.", Toast.LENGTH_SHORT).show();
        }

        folderList.addAll(downloadFolders);
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
        ArrayList<File> fileList = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files != null) {
            Collections.addAll(fileList, files);
        }
        Collections.sort(fileList, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));

        adapter = new FileAdapter(getContext(), fileList);
        adapter.setOnItemClickListener(position -> {
            if (actionListener != null) actionListener.onListItemClicked();
            File clickedFile = adapter.getDisplayList().get(position);
            openFile(clickedFile, new ArrayList<>(adapter.getOriginalList()));
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
        addPathSegment("Download", null);
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
                loadDownloadFolders();
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
            loadDownloadFolders();
            return true;
        }
        return false;
    }

    private void openFile(File fileToOpen, ArrayList<File> currentFileList) {
        String mimeType = getMimeType(fileToOpen.getAbsolutePath());
        if (mimeType == null) mimeType = "*/*";

        if (mimeType.startsWith("image/")) {
            ArrayList<File> imagesInList = currentFileList.stream()
                    .filter(f -> {
                        String mt = getMimeType(f.getAbsolutePath());
                        return mt != null && mt.startsWith("image/");
                    })
                    .collect(Collectors.toCollection(ArrayList::new));
            int position = imagesInList.indexOf(fileToOpen);
            if (position != -1 && actionListener != null) {
                actionListener.onImageClicked(imagesInList, position);
            }
        } else if (mimeType.startsWith("video/")) {
            ArrayList<File> videosInList = currentFileList.stream()
                    .filter(f -> {
                        String mt = getMimeType(f.getAbsolutePath());
                        return mt != null && mt.startsWith("video/");
                    })
                    .collect(Collectors.toCollection(ArrayList::new));
            int position = videosInList.indexOf(fileToOpen);
            if (position != -1 && actionListener != null) {
                actionListener.onVideoClicked(videosInList, position);
            }
        } else if (mimeType.startsWith("audio/")) {
            ArrayList<AudioItem> audioItemsInList = new ArrayList<>();
            ArrayList<File> audioFilesInList = new ArrayList<>();
            for (File f : currentFileList) {
                String mt = getMimeType(f.getAbsolutePath());
                if (mt != null && mt.startsWith("audio/")) {
                    Uri contentUri = getContentUriFromFile(f);
                    if (contentUri != null) {
                        audioItemsInList.add(new AudioItem(f, contentUri));
                        audioFilesInList.add(f);
                    }
                }
            }
            int position = audioFilesInList.indexOf(fileToOpen);
            if (position != -1 && actionListener != null) {
                actionListener.onAudioClicked(audioItemsInList, position);
            }
        } else {
            openWithSystemChooser(fileToOpen, mimeType);
        }
    }

    private void openWithSystemChooser(File file, String mimeType) {
        try {
            Uri fileProviderUri = FileProvider.getUriForFile(requireContext(), requireContext().getApplicationContext().getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileProviderUri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Open with..."));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "No application found to open this file.", Toast.LENGTH_SHORT).show();
        } catch (IllegalArgumentException e) {
            Toast.makeText(getContext(), "Could not create a shareable link for this file.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
}