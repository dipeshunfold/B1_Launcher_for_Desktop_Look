package com.bluelight.computer.winlauncher.prolauncher.model;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.bluelight.computer.winlauncher.prolauncher.database.AppDatabase;
import com.bluelight.computer.winlauncher.prolauncher.database.AppItem;
import com.bluelight.computer.winlauncher.prolauncher.database.AppItemDao;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppItemViewModel extends AndroidViewModel {
    private final AppItemDao appItemDao;
    private final LiveData<List<AppItem>> allItems;
    private final ExecutorService databaseExecutor;


    public AppItemViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        appItemDao = db.appItemDao();
        allItems = appItemDao.getAllItems();
        databaseExecutor = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<AppItem>> getAllItems() {
        return allItems;
    }


    public LiveData<List<AppItem>> getItemsForPage(int page) {
        return appItemDao.getItemsForPage(page);
    }


}