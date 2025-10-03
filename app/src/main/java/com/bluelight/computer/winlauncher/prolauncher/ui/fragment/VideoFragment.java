package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

import android.content.ContentResolver;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.my_interface.IOnBackPressed;
import com.bluelight.computer.winlauncher.prolauncher.ui.adapter.FileAdapter;
import com.bluelight.computer.winlauncher.prolauncher.ui.adapter.ViewPagerAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class VideoFragment extends BaseFileFragment implements IOnBackPressed {

    private ViewPager2 viewPager;
    private HorizontalScrollView pathScrollView;
    private LinearLayout pathContainer;
    private boolean isShowingFolders = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        viewPager = view.findViewById(R.id.viewPager);
        pathScrollView = view.findViewById(R.id.pathScrollView);
        pathContainer = view.findViewById(R.id.pathContainer);
        viewPager.setOffscreenPageLimit(1);


        refreshData();
        setupAdapter();
        return view;
    }

    @Override
    protected void refreshData() {
        loadVideoFolders();
    }

    private void loadVideoFolders() {
        isShowingFolders = true;
        updatePathBreadcrumbs(null);
        ArrayList<File> folderList = new ArrayList<>();
        HashSet<File> videoFolders = new HashSet<>();
        ContentResolver contentResolver = requireContext().getContentResolver();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Video.Media.DATA};
        try (Cursor cursor = contentResolver.query(uri, projection, null, null, null)) {
            if (cursor != null) {
                int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                while (cursor.moveToNext()) {
                    File file = new File(cursor.getString(pathColumn));
                    if (file.exists() && file.getParentFile() != null) {
                        videoFolders.add(file.getParentFile());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        folderList.addAll(videoFolders);
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
        ArrayList<File> videoList = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (isVideoFile(file.getName())) {
                    videoList.add(file);
                }
            }
        }
        Collections.sort(videoList, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));

        adapter = new FileAdapter(getContext(), videoList);
        adapter.setOnItemClickListener(position -> {
            if (actionListener != null) actionListener.onListItemClicked();
            File clickedVideo = adapter.getDisplayList().get(position);
            ArrayList<File> pagerContext = new ArrayList<>(adapter.getOriginalList());
            int correctPositionInPager = pagerContext.indexOf(clickedVideo);
            if (correctPositionInPager != -1) {
                playVideoInViewPager(pagerContext, correctPositionInPager);
            }
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
        addPathSegment("Videos", null);
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
                loadVideoFolders();
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
        if (viewPager != null && viewPager.getVisibility() == View.VISIBLE) {
            viewPager.setAdapter(null);
            viewPager.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            return true;
        }
        if (!isShowingFolders) {
            loadVideoFolders();
            return true;
        }
        return false;
    }

    public void playVideoInViewPager(ArrayList<File> videoList, int position) {
        if (viewPager == null || recyclerView == null) return;
        ViewPagerAdapter pagerAdapter = new ViewPagerAdapter(getContext(), videoList);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(position, false);
        viewPager.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private boolean isVideoFile(String fileName) {
        String lowerCaseName = fileName.toLowerCase();
        return lowerCaseName.endsWith(".mp4") || lowerCaseName.endsWith(".mkv") ||
                lowerCaseName.endsWith(".avi") || lowerCaseName.endsWith(".webm") ||
                lowerCaseName.endsWith(".mov") || lowerCaseName.endsWith(".3gp");
    }
}