package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.model.AudioItem;
import com.bluelight.computer.winlauncher.prolauncher.my_interface.IOnBackPressed;
import com.bluelight.computer.winlauncher.prolauncher.ui.adapter.FileAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SdcardFragment extends BaseFileFragment implements IOnBackPressed {

    private File currentDirectory;
    private File sdCardRoot;

    private HorizontalScrollView pathScrollView;
    private LinearLayout pathContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sdcard, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        pathScrollView = view.findViewById(R.id.pathScrollView);
        pathContainer = view.findViewById(R.id.pathContainer);


        sdCardRoot = getRemovableSdCardRoot();
        if (sdCardRoot != null) {
            loadDirectory(sdCardRoot);
        } else {
            Toast.makeText(getContext(), "Removable SD Card not found.", Toast.LENGTH_LONG).show();
            updatePathBreadcrumbs(null);
        }
        setupAdapter();
        return view;
    }

    @Override
    protected void refreshData() {
        if (currentDirectory != null) {
            loadDirectory(currentDirectory);
        } else if (sdCardRoot != null) {
            loadDirectory(sdCardRoot);
        }
    }

    private void loadDirectory(File directory) {
        if (directory == null || !directory.canRead()) {
            Toast.makeText(getContext(), "Access Denied", Toast.LENGTH_SHORT).show();
            return;
        }

        currentDirectory = directory;
        updatePathBreadcrumbs(directory);
        File[] files = directory.listFiles();
        List<File> fileListForAdapter = new ArrayList<>();
        if (files != null) {
            Arrays.sort(files, (f1, f2) -> {
                if (f1.isDirectory() != f2.isDirectory()) return f1.isDirectory() ? -1 : 1;
                return f1.getName().compareToIgnoreCase(f2.getName());
            });
            fileListForAdapter.addAll(Arrays.asList(files));
        }

        adapter = new FileAdapter(getContext(), new ArrayList<>(fileListForAdapter));
        adapter.setOnItemClickListener(position -> {
            if (actionListener != null) {
                actionListener.onListItemClicked();
            }
            File clickedFile = adapter.getDisplayList().get(position);
            if (clickedFile.isDirectory()) {
                loadDirectory(clickedFile);
            } else {
                openFile(clickedFile, new ArrayList<>(adapter.getOriginalList()));
            }
        });

        recyclerView.setAdapter(adapter);
        recyclerView.scrollToPosition(0);
        adapter.setOnSelectionChangedListener(this);
    }

    private void updatePathBreadcrumbs(File directory) {
        pathContainer.removeAllViews();
        if (getContext() == null) return;

        if (sdCardRoot == null || directory == null) {
            addPathSegment("SD Card Not Found", null);
            return;
        }

        String rootPath = sdCardRoot.getPath();
        String currentPath = directory.getPath();

        addPathSegment("SD Card", sdCardRoot);

        if (!currentPath.equals(rootPath)) {
            String relativePath = currentPath.substring(rootPath.length());
            String[] segments = relativePath.split("/");
            StringBuilder pathBuilder = new StringBuilder(rootPath);

            for (String segment : segments) {
                if (segment.isEmpty()) continue;

                pathBuilder.append("/").append(segment);
                File segmentFile = new File(pathBuilder.toString());

                addPathSeparator();
                addPathSegment(segment, segmentFile);
            }
        }

        pathScrollView.post(() -> pathScrollView.fullScroll(View.FOCUS_RIGHT));
    }

    private void addPathSegment(String name, @Nullable File path) {
        TextView segmentView = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.breadcrumb_segment, pathContainer, false);
        segmentView.setText(name);
        if (path != null) {
            segmentView.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onListItemClicked();
                loadDirectory(path);
            });
        }
        pathContainer.addView(segmentView);
    }

    private void addPathSeparator() {
        TextView separatorView = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.breadcrumb_separator, pathContainer, false);
        pathContainer.addView(separatorView);
    }

    @Override
    public boolean onBackPressed() {
        if (currentDirectory != null && sdCardRoot != null && !currentDirectory.equals(sdCardRoot) && currentDirectory.getParentFile() != null) {
            loadDirectory(currentDirectory.getParentFile());
            return true;
        }
        return false;
    }

    private File getRemovableSdCardRoot() {
        if (getContext() == null) return null;
        File[] externalDirs = ContextCompat.getExternalFilesDirs(getContext(), null);
        for (File dir : externalDirs) {
            if (dir != null && Environment.isExternalStorageRemovable(dir)) {
                String state = Environment.getStorageState(dir);
                if (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                    String path = dir.getAbsolutePath();
                    int index = path.indexOf("/Android/data");
                    if (index > 0) {
                        return new File(path.substring(0, index));
                    }
                }
            }
        }
        return null;
    }


    private void openFile(File fileToOpen, ArrayList<File> contextFileList) {
        String mimeType = getMimeType(fileToOpen.getAbsolutePath());
        if (mimeType == null) mimeType = "*/*";

        if (mimeType.startsWith("image/")) {
            ArrayList<File> imagesInFolder = contextFileList.stream()
                    .filter(f -> !f.isDirectory() && getMimeType(f.getAbsolutePath()) != null && getMimeType(f.getAbsolutePath()).startsWith("image/"))
                    .collect(Collectors.toCollection(ArrayList::new));
            int position = imagesInFolder.indexOf(fileToOpen);
            if (position != -1 && actionListener != null) {
                actionListener.onImageClicked(imagesInFolder, position);
            }
        } else if (mimeType.startsWith("video/")) {
            ArrayList<File> videosInFolder = contextFileList.stream()
                    .filter(f -> !f.isDirectory() && getMimeType(f.getAbsolutePath()) != null && getMimeType(f.getAbsolutePath()).startsWith("video/"))
                    .collect(Collectors.toCollection(ArrayList::new));
            int position = videosInFolder.indexOf(fileToOpen);
            if (position != -1 && actionListener != null) {
                actionListener.onVideoClicked(videosInFolder, position);
            }
        } else if (mimeType.startsWith("audio/")) {
            ArrayList<AudioItem> audioItemsInFolder = new ArrayList<>();
            ArrayList<File> audioFilesInFolder = new ArrayList<>();
            for (File f : contextFileList) {
                if (!f.isDirectory()) {
                    String mt = getMimeType(f.getAbsolutePath());
                    if (mt != null && mt.startsWith("audio/")) {
                        Uri contentUri = getContentUriFromFile(f);
                        if (contentUri != null) {
                            audioItemsInFolder.add(new AudioItem(f, contentUri));
                            audioFilesInFolder.add(f);
                        }
                    }
                }
            }
            int position = audioFilesInFolder.indexOf(fileToOpen);
            if (position != -1 && actionListener != null) {
                actionListener.onAudioClicked(audioItemsInFolder, position);
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