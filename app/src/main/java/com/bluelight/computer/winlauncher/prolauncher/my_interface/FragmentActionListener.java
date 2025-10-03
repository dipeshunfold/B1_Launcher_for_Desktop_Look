package com.bluelight.computer.winlauncher.prolauncher.my_interface;

import androidx.fragment.app.Fragment;

import com.bluelight.computer.winlauncher.prolauncher.model.AudioItem;

import java.io.File;
import java.util.ArrayList;

public interface FragmentActionListener {
    void onImageClicked(ArrayList<File> imageList, int position);

    void onVideoClicked(ArrayList<File> videoList, int position);

    void onAudioClicked(ArrayList<AudioItem> audioItems, int position);

    void requestLoadFragment(Fragment fragment, String title);

    void onSelectionChanged(int count);

    void requestDelete();

    void requestShare();

    void requestRename();

    void requestProperties();

    void onListItemClicked();

}