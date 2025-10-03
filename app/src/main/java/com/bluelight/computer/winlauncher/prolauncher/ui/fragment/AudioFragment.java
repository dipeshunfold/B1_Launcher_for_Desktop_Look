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
import com.bluelight.computer.winlauncher.prolauncher.model.AudioItem;
import com.bluelight.computer.winlauncher.prolauncher.my_interface.IOnBackPressed;
import com.bluelight.computer.winlauncher.prolauncher.ui.adapter.FileAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;


public class AudioFragment extends BaseFileFragment implements IOnBackPressed, AudioPlayerPagerAdapter.AudioControlsListener {

    private ViewPager2 viewPager;
    private AudioPlayerPagerAdapter pagerAdapter;
    private ViewPager2.OnPageChangeCallback pageChangeCallback;

    private HorizontalScrollView pathScrollView;
    private LinearLayout pathContainer;
    private boolean isShowingFolders = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_audio, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        viewPager = view.findViewById(R.id.viewPager);
        pathScrollView = view.findViewById(R.id.pathScrollView);
        pathContainer = view.findViewById(R.id.pathContainer);
        viewPager.setOffscreenPageLimit(1);

        initializePageChangeCallback();
        refreshData();
        setupAdapter();
        return view;
    }

    @Override
    protected void refreshData() {
        loadAudioFolders();
    }

    private void loadAudioFolders() {
        isShowingFolders = true;
        updatePathBreadcrumbs(null);
        ArrayList<File> folderList = new ArrayList<>();
        HashSet<File> audioFolders = new HashSet<>();
        ContentResolver contentResolver = requireContext().getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Audio.Media.DATA};
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        try (Cursor cursor = contentResolver.query(uri, projection, selection, null, null)) {
            if (cursor != null) {
                int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                while (cursor.moveToNext()) {
                    File file = new File(cursor.getString(pathColumn));
                    if (file.exists() && file.getParentFile() != null) {
                        audioFolders.add(file.getParentFile());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        folderList.addAll(audioFolders);
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
        ArrayList<File> audioList = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (isAudioFile(file.getName())) {
                    audioList.add(file);
                }
            }
        }
        Collections.sort(audioList, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));

        adapter = new FileAdapter(getContext(), audioList);
        adapter.setOnItemClickListener(position -> {
            if (actionListener != null) actionListener.onListItemClicked();
            File clickedFile = adapter.getDisplayList().get(position);
            ArrayList<AudioItem> audioItemsForPlayer = buildAudioItemsFromFiles(new ArrayList<>(adapter.getOriginalList()));
            int correctPositionInPager = -1;
            for (int i = 0; i < audioItemsForPlayer.size(); i++) {
                if (audioItemsForPlayer.get(i).file.equals(clickedFile)) {
                    correctPositionInPager = i;
                    break;
                }
            }

            if (correctPositionInPager != -1) {
                showAudioPlayer(audioItemsForPlayer, correctPositionInPager);
            }
        });
        recyclerView.setAdapter(adapter);
        adapter.setOnSelectionChangedListener(this);
    }

    private ArrayList<AudioItem> buildAudioItemsFromFiles(List<File> files) {
        ArrayList<AudioItem> items = new ArrayList<>();
        for (File file : files) {
            Uri contentUri = getContentUriFromFile(file);
            if (contentUri != null) {
                items.add(new AudioItem(file, contentUri));
            }
        }
        return items;
    }

    private void updatePathBreadcrumbs(@Nullable File folder) {
        pathContainer.removeAllViews();
        if (folder == null) {
            pathScrollView.setVisibility(View.GONE);
            return;
        }
        pathScrollView.setVisibility(View.VISIBLE);
        addPathSegment("Music", null);
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
                loadAudioFolders();
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
            hideAudioPlayer();
            return true;
        }
        if (!isShowingFolders) {
            loadAudioFolders();
            return true;
        }
        return false;
    }

    public void showAudioPlayer(ArrayList<AudioItem> items, int initialPosition) {
        if (viewPager == null || recyclerView == null || getContext() == null) return;
        pagerAdapter = new AudioPlayerPagerAdapter(getContext(), items, this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.unregisterOnPageChangeCallback(pageChangeCallback);
        viewPager.registerOnPageChangeCallback(pageChangeCallback);
        viewPager.setCurrentItem(initialPosition, false);
        pagerAdapter.playSong(initialPosition);
        viewPager.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        pathScrollView.setVisibility(View.GONE);
    }

    private void hideAudioPlayer() {
        if (pagerAdapter != null) pagerAdapter.releasePlayer();
        if (viewPager != null) {
            viewPager.setVisibility(View.GONE);
            viewPager.unregisterOnPageChangeCallback(pageChangeCallback);
        }
        if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
        if (pathScrollView != null && !isShowingFolders) {
            pathScrollView.setVisibility(View.VISIBLE);
        }
    }

    private boolean isAudioFile(String fileName) {
        String name = fileName.toLowerCase();
        return name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".m4a") || name.endsWith(".ogg") || name.endsWith(".flac");
    }

    @Override
    public void onPause() {
        super.onPause();
        if (pagerAdapter != null) {
            pagerAdapter.releasePlayer();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pagerAdapter != null) pagerAdapter.releasePlayer();
        if (viewPager != null && pageChangeCallback != null) {
            viewPager.unregisterOnPageChangeCallback(pageChangeCallback);
        }
    }

    private void initializePageChangeCallback() {
        this.pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (pagerAdapter != null) {
                    pagerAdapter.playSong(position);
                }
            }
        };
    }

    @Override
    public void onNextClicked() {
        if (viewPager != null && pagerAdapter != null && pagerAdapter.getItemCount() > 0) {
            int currentPos = viewPager.getCurrentItem();
            int nextPos = (currentPos + 1) % pagerAdapter.getItemCount();
            viewPager.setCurrentItem(nextPos, true);
        }
    }

    @Override
    public void onPreviousClicked() {
        if (viewPager != null && pagerAdapter != null && pagerAdapter.getItemCount() > 0) {
            int currentPos = viewPager.getCurrentItem();
            int prevPos = (currentPos - 1 + pagerAdapter.getItemCount()) % pagerAdapter.getItemCount();
            viewPager.setCurrentItem(prevPos, true);
        }
    }
}