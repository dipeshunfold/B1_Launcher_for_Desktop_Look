package com.bluelight.computer.winlauncher.prolauncher.model;

import android.net.Uri;

import java.io.File;


public class AudioItem {
    public final File file;
    public final Uri contentUri;

    public AudioItem(File file, Uri contentUri) {
        this.file = file;
        this.contentUri = contentUri;
    }
}