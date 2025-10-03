package com.bluelight.computer.winlauncher.prolauncher.model;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.provider.Telephony;
import android.telecom.TelecomManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.database.AppDatabase;
import com.bluelight.computer.winlauncher.prolauncher.database.AppItem;
import com.bluelight.computer.winlauncher.prolauncher.database.AppItemDao;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskbarViewModel extends AndroidViewModel {
    private final AppItemDao appItemDao;
    private final ExecutorService databaseWriteExecutor;

    private final MediatorLiveData<List<Object>> combinedTaskbarItems = new MediatorLiveData<>();
    private final List<DefaultTaskbarApp> defaultApps = new ArrayList<>();

    public TaskbarViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        appItemDao = db.appItemDao();
        databaseWriteExecutor = Executors.newSingleThreadExecutor();

        loadDefaultApps(application);

        LiveData<List<AppItem>> pinnedItemsFromDb = appItemDao.getPinnedItems();

        combinedTaskbarItems.addSource(pinnedItemsFromDb, userPinnedItems -> {
            List<Object> combinedList = new ArrayList<>();
            combinedList.addAll(defaultApps);
            if (userPinnedItems != null) {
                combinedList.addAll(userPinnedItems);
            }
            combinedTaskbarItems.setValue(combinedList);
        });
    }

    public LiveData<List<Object>> getCombinedTaskbarItems() {
        return combinedTaskbarItems;
    }

    public void togglePinStatus(AppItem item) {
        item.isPinned = !item.isPinned;
        databaseWriteExecutor.execute(() -> appItemDao.update(item));
    }

    private void loadDefaultApps(Context context) {
        PackageManager pm = context.getPackageManager();

        String defaultDialerPkg = getDefaultDialerPackage(context);
        String defaultSmsPkg = Telephony.Sms.getDefaultSmsPackage(context);

        String[] defaultPackages = {
                defaultDialerPkg,
                defaultSmsPkg,
                "com.android.chrome",
        };

        for (String pkgName : defaultPackages) {
            if (pkgName == null || pkgName.isEmpty()) continue;

            try {
                Intent intent = pm.getLaunchIntentForPackage(pkgName);
                if (intent != null) {
                    ResolveInfo info = pm.resolveActivity(intent, 0);
                    if (info != null) {
                        Drawable icon;
                        CharSequence label = info.loadLabel(pm);
                        if (pkgName.equals(defaultDialerPkg)) {
                            icon = ContextCompat.getDrawable(context, R.drawable.custom_phone_icon);
                        } else if (pkgName.equals(defaultSmsPkg)) {
                            icon = ContextCompat.getDrawable(context, R.drawable.custom_message_icon);
                        } else if (pkgName.equals("com.android.chrome")) {
                            icon = ContextCompat.getDrawable(context, R.drawable.custom_crome_icon);
                        } else {
                            icon = info.loadIcon(pm);
                        }

                        if (icon != null) {
                            defaultApps.add(new DefaultTaskbarApp(pkgName, icon, label));
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    private String getDefaultDialerPackage(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
            if (telecomManager != null) {
                return telecomManager.getDefaultDialerPackage();
            }
        } else {
            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(dialIntent, 0);
            if (resolveInfo != null) {
                return resolveInfo.activityInfo.packageName;
            }
        }
        return null;
    }
}