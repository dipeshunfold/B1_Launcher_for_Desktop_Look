package com.bluelight.computer.winlauncher.prolauncher.ui.activity;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.model.prefrance.PreferenceHelper;
import com.bluelight.computer.winlauncher.prolauncher.ui.adapter.AppListAdapter;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppListActivity extends AppCompatActivity {
    private Set<String> quickAccessPackages;
    private AppListAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_list);
        setTitle("Manage Apps");

        Serializable serializableExtra = getIntent().getSerializableExtra("present_packages");
        Set<String> presentOnHomePackages = (serializableExtra instanceof Set) ? (Set<String>) serializableExtra : new HashSet<>();


        quickAccessPackages = PreferenceHelper.getAllowedPackages(this);
        if (quickAccessPackages == null) {
            quickAccessPackages = new HashSet<>();
        }

        RecyclerView recyclerView = findViewById(R.id.apps_recycler_view);
        FrameLayout root_layout = findViewById(R.id.root_layout);
        RelativeLayout content_container = findViewById(R.id.content_container);
        content_container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        root_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(0, R.anim.slide_down);

            }
        });
        int orientation = getResources().getConfiguration().orientation;
        int spanCount = orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE ? 3 : 1;
        recyclerView.setLayoutManager(new GridLayoutManager(this, spanCount));

        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> allApps = getPackageManager().queryIntentActivities(mainIntent, 0);
        Collections.sort(allApps, new ResolveInfo.DisplayNameComparator(getPackageManager()));

        adapter = new AppListAdapter(allApps, getPackageManager(), presentOnHomePackages, quickAccessPackages, new AppListAdapter.OnAppActionListener() {
            @Override
            public void onAppAction(ResolveInfo appInfo, AppListAdapter.AppAction action) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("picked_app_info", appInfo);
                resultIntent.putExtra("action", action);
                setResult(RESULT_OK, resultIntent);


                finish();
            }

            @Override
            public void onAppLongClick(ResolveInfo appInfo, boolean isCurrentlyInQuickAccess) {
                showQuickAccessDialog(appInfo, isCurrentlyInQuickAccess);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void showQuickAccessDialog(ResolveInfo appInfo, boolean isCurrentlyInQuickAccess) {
        String appName = appInfo.loadLabel(getPackageManager()).toString();
        String message = isCurrentlyInQuickAccess
                ? "Remove '" + appName + "' from Quick Access list?"
                : "Add '" + appName + "' to Quick Access list?";
        String buttonText = isCurrentlyInQuickAccess ? "Remove" : "Add";

        new AlertDialog.Builder(this)
                .setTitle("Quick Access")
                .setMessage(message)
                .setPositiveButton(buttonText, (dialog, which) -> {
                    String packageName = appInfo.activityInfo.packageName;
                    if (isCurrentlyInQuickAccess) {
                        PreferenceHelper.removePackage(this, packageName);
                        quickAccessPackages.remove(packageName);
                    } else {
                        PreferenceHelper.addPackage(this, packageName);
                        quickAccessPackages.add(packageName);
                    }

                    adapter.notifyDataSetChanged();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}