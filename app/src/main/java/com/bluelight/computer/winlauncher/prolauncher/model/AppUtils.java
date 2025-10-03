package com.bluelight.computer.winlauncher.prolauncher.model;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppUtils {


    public static List<AppInfo> cachedApps = null;


    public static List<AppInfo> getInstalledApps(Context context) {
        PackageManager pm = context.getPackageManager();
        List<AppInfo> apps = new ArrayList<>();

        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> allApps = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo ri : allApps) {
            AppInfo app = new AppInfo();
            app.label = ri.loadLabel(pm);
            app.packageName = ri.activityInfo.packageName;
            app.icon = ri.loadIcon(pm); // REMOVE THIS LINE - THIS IS THE OOM CAUSE
            apps.add(app);
        }

        Collections.sort(apps, (a, b) -> a.label.toString().compareToIgnoreCase(b.label.toString()));

        return apps;
    }
}