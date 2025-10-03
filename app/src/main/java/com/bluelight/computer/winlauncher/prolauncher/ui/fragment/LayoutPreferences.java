package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

import android.content.Context;
import android.content.SharedPreferences;

public class LayoutPreferences {
    private SharedPreferences prefs;
    private static final String PREF_LAYOUT_MODE = "layout_mode";

    public LayoutPreferences(Context context) {
        prefs = context.getSharedPreferences("layout_prefs", Context.MODE_PRIVATE);
    }

    public void saveLayoutMode(BaseFileFragment.LayoutMode mode) {
        prefs.edit().putString(PREF_LAYOUT_MODE, mode.name()).apply();
    }

    public BaseFileFragment.LayoutMode getSavedLayoutMode() {
        String modeName = prefs.getString(PREF_LAYOUT_MODE, BaseFileFragment.LayoutMode.GRID.name());
        return BaseFileFragment.LayoutMode.valueOf(modeName);
    }
}