package com.bluelight.computer.winlauncher.prolauncher.model;

import android.graphics.drawable.Drawable;

public class DefaultTaskbarApp {
    public final String packageName;
    public final Drawable icon;
    public final CharSequence label;

    public DefaultTaskbarApp(String packageName, Drawable icon, CharSequence label) {
        this.packageName = packageName;
        this.icon = icon;
        this.label = label;
    }
}