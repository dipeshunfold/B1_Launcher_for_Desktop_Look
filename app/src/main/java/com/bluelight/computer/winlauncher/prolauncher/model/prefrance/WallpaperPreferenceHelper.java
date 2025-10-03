package com.bluelight.computer.winlauncher.prolauncher.model.prefrance;

import android.content.Context;
import android.content.SharedPreferences;

public class WallpaperPreferenceHelper {

    private static final String PREF_NAME = "wallpaper_prefs";
    private static final String KEY_WALLPAPER_RES_ID = "app_wallpaper_res_id";
    private static final String KEY_WALLPAPER_URL = "app_wallpaper_url";

    public static void setWallpaper(Context context, int resId) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
        editor.putInt(KEY_WALLPAPER_RES_ID, resId);
        editor.putString(KEY_WALLPAPER_URL, null); // Clear URL when setting resource ID
        editor.apply();
    }

    public static int getWallpaper(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_WALLPAPER_RES_ID, -1);
    }

    public static void setWallpaperUrl(Context context, String url) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(KEY_WALLPAPER_URL, url);
        editor.putInt(KEY_WALLPAPER_RES_ID, -1); // Clear resource ID when setting URL
        editor.apply();
    }

    public static String getWallpaperUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_WALLPAPER_URL, null);
    }
}