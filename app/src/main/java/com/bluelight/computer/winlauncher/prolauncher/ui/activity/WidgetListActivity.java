package com.bluelight.computer.winlauncher.prolauncher.ui.activity;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.model.GroupedWidgetItem;
import com.bluelight.computer.winlauncher.prolauncher.ui.adapter.WidgetListAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WidgetListActivity extends AppCompatActivity {
    private Set<String> presentOnHomeProviders;
    private WidgetListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_list);
        setTitle("Select a Widget"); // This sets the activity title, not the TextView in the layout

        presentOnHomeProviders = (Set<String>) getIntent().getSerializableExtra("present_providers");
        if (presentOnHomeProviders == null) presentOnHomeProviders = new HashSet<>();

        RecyclerView recyclerView = findViewById(R.id.apps_recycler_view);

        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        List<AppWidgetProviderInfo> widgetProviders = appWidgetManager.getInstalledProviders();
        PackageManager pm = getPackageManager(); // Get PackageManager instance

        // Group widgets by their PROVIDING APPLICATION'S NAME
        Map<String, List<AppWidgetProviderInfo>> groupedWidgetsMap = new HashMap<>();
        for (AppWidgetProviderInfo info : widgetProviders) {
            String appLabel = "Unknown App"; // Default fallback
            try {
                // Get the package name from the widget's provider ComponentName
                ComponentName providerComponent = info.provider;
                String packageName = providerComponent.getPackageName();

                // Get ApplicationInfo for that package name
                ApplicationInfo applicationInfo = pm.getApplicationInfo(packageName, 0);

                // Get the display label for the application
                appLabel = pm.getApplicationLabel(applicationInfo).toString();
            } catch (PackageManager.NameNotFoundException e) {
                // Fallback to package name if application label cannot be found
                appLabel = info.provider.getPackageName();
            }

            // Add the widget to the list corresponding to its application label
            groupedWidgetsMap.computeIfAbsent(appLabel, k -> new ArrayList<>()).add(info);
        }

        // Sort app labels alphabetically (case-insensitive) for consistent grouping order
        List<String> sortedAppLabels = new ArrayList<>(groupedWidgetsMap.keySet());
        Collections.sort(sortedAppLabels, String.CASE_INSENSITIVE_ORDER);

        // Prepare the list of GroupedWidgetItem for the main adapter
        List<GroupedWidgetItem> groupedWidgetData = new ArrayList<>();
        for (String label : sortedAppLabels) {
            groupedWidgetData.add(new GroupedWidgetItem(label)); // Add header item (application name)
            List<AppWidgetProviderInfo> widgetsForApp = groupedWidgetsMap.get(label);

            // Sort individual widgets within each app group by their specific widget label (case-insensitive)
            Collections.sort(widgetsForApp, Comparator.comparing(o -> o.loadLabel(pm), String.CASE_INSENSITIVE_ORDER));
            groupedWidgetData.add(new GroupedWidgetItem(widgetsForApp)); // Add widget group item
        }

        FrameLayout root_layout = findViewById(R.id.root_layout);
        LinearLayout content_container = findViewById(R.id.content_container);
        content_container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Keep the click listener to consume touch events and prevent closing when clicking on the content
            }
        });

        root_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Close activity when clicking outside the content container
            }
        });

        adapter = new WidgetListAdapter(this, groupedWidgetData, presentOnHomeProviders, (widgetInfo, action) -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("picked_widget_info", widgetInfo);
            resultIntent.putExtra("action", action);
            setResult(RESULT_OK, resultIntent);
            String providerString = widgetInfo.provider.flattenToString();
            if (action == WidgetListAdapter.WidgetAction.ADD) {
                presentOnHomeProviders.add(providerString);
            } else {
                presentOnHomeProviders.remove(providerString);
            }
            // Notify the main adapter that the data has changed. This will trigger the nested
            // adapters to update their individual widget items (e.g., add/remove icon).
            adapter.notifyDataSetChanged();
        });
        recyclerView.setAdapter(adapter);
    }
}