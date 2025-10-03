package com.bluelight.computer.winlauncher.prolauncher.model;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsViewModel extends ViewModel {

    // Existing LiveData for various settings
    private final MutableLiveData<Boolean> cortanaEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> contactsEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> timeEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> taskbarTransparent = new MutableLiveData<>();
    private final MutableLiveData<Boolean> recentsButtonEnabled = new MutableLiveData<>();
    private final MutableLiveData<Integer> taskbarBgColor = new MutableLiveData<>();
    private final MutableLiveData<Integer> taskbarTextColor = new MutableLiveData<>();
    private final MutableLiveData<String> dateFormat = new MutableLiveData<>();
    private final MutableLiveData<Integer> systemBarsMode = new MutableLiveData<>();
    private final MutableLiveData<Boolean> hideTaskbarIcons = new MutableLiveData<>();
    private final MutableLiveData<Integer> desktopTextColor = new MutableLiveData<>();
    private final MutableLiveData<Integer> iconSize = new MutableLiveData<>();

    // Wallpaper related LiveData
    private final MutableLiveData<List<String>> allWallpaperUrls = new MutableLiveData<>();
    private final MutableLiveData<String> appWallpaperUrl = new MutableLiveData<>();
    private final MutableLiveData<Integer> appWallpaperResId = new MutableLiveData<>();

    // Assuming WallpaperRepository is defined elsewhere.
    private final WallpaperRepository wallpaperRepository = new WallpaperRepository();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public void setAppWallpaper(String url) {
        appWallpaperUrl.setValue(url);
        appWallpaperResId.setValue(-1); // Clear resource ID when setting URL
    }

    public LiveData<String> getAppWallpaperUrl() {
        return appWallpaperUrl;
    }

    public void setAppWallpaper(int resId) {
        appWallpaperResId.setValue(resId);
        appWallpaperUrl.setValue(null); // Clear URL when setting resource ID
    }

    public LiveData<Integer> getAppWallpaperResId() {
        return appWallpaperResId;
    }

    public LiveData<List<String>> getAllWallpaperUrls() {
        return allWallpaperUrls;
    }

    public void loadWallpaperUrls() {
        executorService.execute(() -> {
            List<String> urls = wallpaperRepository.getNewArrivalWallpaperUrls();
            allWallpaperUrls.postValue(urls);
        });
    }

    // --- Existing settings-related LiveData and methods ---
    public LiveData<Boolean> isCortanaEnabled() {
        return cortanaEnabled;
    }

    public void setCortanaEnabled(boolean isEnabled) {
        cortanaEnabled.setValue(isEnabled);
    }

    public LiveData<Boolean> isContactsEnabled() {
        return contactsEnabled;
    }

    public void setContactsEnabled(boolean isEnabled) {
        contactsEnabled.setValue(isEnabled);
    }

    public LiveData<Boolean> isTimeEnabled() {
        return timeEnabled;
    }

    public void setTimeEnabled(boolean isEnabled) {
        timeEnabled.setValue(isEnabled);
    }

    public LiveData<Boolean> isTaskbarTransparent() {
        return taskbarTransparent;
    }

    public void setTaskbarTransparent(boolean isTransparent) {
        taskbarTransparent.setValue(isTransparent);
    }

    public LiveData<Boolean> isRecentsButtonEnabled() {
        return recentsButtonEnabled;
    }

    public void setRecentsButtonEnabled(boolean isEnabled) {
        recentsButtonEnabled.setValue(isEnabled);
    }

    public LiveData<Integer> getTaskbarBgColor() {
        return taskbarBgColor;
    }

    public void setTaskbarBgColor(int color) {
        taskbarBgColor.setValue(color);
    }

    public LiveData<Integer> getTaskbarTextColor() {
        return taskbarTextColor;
    }

    public void setTaskbarTextColor(int color) {
        taskbarTextColor.setValue(color);
    }

    public LiveData<String> getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String format) {
        dateFormat.setValue(format);
    }

    public LiveData<Integer> getSystemBarsMode() {
        return systemBarsMode;
    }

    public void setSystemBarsMode(int mode) {
        systemBarsMode.setValue(mode);
    }

    public LiveData<Boolean> shouldHideTaskbarIcons() {
        return hideTaskbarIcons;
    }

    public void setHideTaskbarIcons(boolean hide) {
        hideTaskbarIcons.setValue(hide);
    }

    public LiveData<Integer> getDesktopTextColor() {
        return desktopTextColor;
    }

    public void setDesktopTextColor(int color) {
        desktopTextColor.setValue(color);
    }

    public LiveData<Integer> getIconSize() {
        return iconSize;
    }

    public void setIconSize(int size) {
        iconSize.setValue(size);
    }
}