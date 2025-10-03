package com.bluelight.computer.winlauncher.prolauncher.model;

import android.graphics.drawable.Drawable;

public class AppInfos {
    public String name;
    public Drawable icon;
    public String packageName;

    public AppInfos(String name, Drawable icon, String packageName) {
        this.name = name;
        this.icon = icon;
        this.packageName = packageName;
    }
}