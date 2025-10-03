package com.bluelight.computer.winlauncher.prolauncher.model.prefrance;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class PreferenceHelper {

    public static final int ICON_SIZE_SMALL = 0;
    public static final int ICON_SIZE_MEDIUM = 1;
    public static final int ICON_SIZE_LARGE = 2;
    private static final String PREFS_NAME = "LauncherPrefs";
    private static final String KEY_ALLOWED_PACKAGES = "allowedPackages";
    private static final String KEY_ICON_SIZE = "icon_size_pref";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static Set<String> getAllowedPackages(Context context) {
        Set<String> savedSet = getPrefs(context).getStringSet(KEY_ALLOWED_PACKAGES, null);
        return savedSet == null ? null : new HashSet<>(savedSet);
    }

    public static void setAllowedPackages(Context context, Set<String> packages) {
        getPrefs(context).edit().putStringSet(KEY_ALLOWED_PACKAGES, packages).apply();
    }

    public static void addPackage(Context context, String packageName) {
        Set<String> currentPackages = getAllowedPackages(context);
        Set<String> newPackages = currentPackages == null ? new HashSet<>() : currentPackages;
        newPackages.add(packageName);
        setAllowedPackages(context, newPackages);
    }

    public static void removePackage(Context context, String packageName) {
        Set<String> currentPackages = getAllowedPackages(context);
        if (currentPackages != null) {
            currentPackages.remove(packageName);
            setAllowedPackages(context, currentPackages);
        }
    }


    public static void setIconSize(Context context, int size) {
        getPrefs(context).edit().putInt(KEY_ICON_SIZE, size).apply();
    }

    public static int getIconSize(Context context) {

        return getPrefs(context).getInt(KEY_ICON_SIZE, ICON_SIZE_LARGE);
    }
}