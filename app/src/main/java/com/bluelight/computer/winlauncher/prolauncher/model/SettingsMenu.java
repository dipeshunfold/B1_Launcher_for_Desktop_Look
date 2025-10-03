package com.bluelight.computer.winlauncher.prolauncher.model;

import androidx.fragment.app.Fragment;

public class SettingsMenu {
    private final int icon;
    private final String title;
    private final Class<? extends Fragment> fragmentClass;
    private final String searchableKeywords;

    public SettingsMenu(int icon, String title, Class<? extends Fragment> fragmentClass, String searchableKeywords) {
        this.icon = icon;
        this.title = title;
        this.fragmentClass = fragmentClass;
        this.searchableKeywords = title.toLowerCase() + " " + searchableKeywords.toLowerCase();
    }

    public int getIcon() {
        return icon;
    }

    public String getTitle() {
        return title;
    }

    public Class<? extends Fragment> getFragmentClass() {
        return fragmentClass;
    }

    public String getSearchableKeywords() {
        return searchableKeywords;
    }
}