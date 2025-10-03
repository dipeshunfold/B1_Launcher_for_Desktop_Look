package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.model.AppInfos;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class RecentFragment extends Fragment {

    private AppGridAdapter adapter;
    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private static final int REFRESH_INTERVAL = 2000; // 2 seconds

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recent, container, false);
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        if (enter && nextAnim == R.anim.slide_up) {
            Animation rootFadeIn = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in_background);
            Animation contentSlideUp = AnimationUtils.loadAnimation(getContext(), R.anim.slide_up);
            if (getView() != null) {
                View content = getView().findViewById(R.id.content_container);
                content.startAnimation(contentSlideUp);
            }
            return rootFadeIn;
        } else if (!enter && nextAnim == R.anim.slide_down) {
            Animation rootFadeOut = AnimationUtils.loadAnimation(getContext(), R.anim.fade_out_background);
            Animation contentSlideDown = AnimationUtils.loadAnimation(getContext(), R.anim.slide_down);

            if (getView() != null) {
                View content = getView().findViewById(R.id.content_container);
                content.startAnimation(contentSlideDown);
            }
            return rootFadeOut;
        } else {
            return super.onCreateAnimation(transit, enter, nextAnim);
        }
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FrameLayout rootLayout = view.findViewById(R.id.root_layout);
        CardView contentContainer = view.findViewById(R.id.content_container);
        applyBlurToContentContainer(view);

        rootLayout.setOnClickListener(v -> closeFragment());
        contentContainer.setOnClickListener(v -> {
        });

        if (!hasUsageStatsPermission()) {
            Toast.makeText(getContext(), "Usage access permission is required.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            closeFragment();
            return;
        }
        setupRecyclerView(view);
    }

    private void applyBlurToContentContainer(View view) {
        CardView contentContainer = view.findViewById(R.id.content_container);
        ShapeableImageView blurBackground = view.findViewById(R.id.blur_background);

        contentContainer.post(() -> {
            View rootView = requireActivity().getWindow().getDecorView().findViewById(android.R.id.content);

            // Check if rootView has valid dimensions before creating fullBitmap
            if (rootView.getWidth() <= 0 || rootView.getHeight() <= 0) {
                Log.e("BlurDebug", "Root view has zero width or height, cannot create full bitmap for blur.");
                requireActivity().runOnUiThread(() -> {
                    blurBackground.setVisibility(View.GONE);
                });
                return;
            }

            Bitmap fullBitmap = Bitmap.createBitmap(rootView.getWidth(), rootView.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(fullBitmap);
            rootView.draw(canvas);

            int[] location = new int[2];
            contentContainer.getLocationOnScreen(location);
            int x = location[0];
            int y = location[1];
            int width = contentContainer.getWidth();
            int height = contentContainer.getHeight();

            // Ensure coordinates are within bitmap bounds
            x = Math.max(0, Math.min(x, fullBitmap.getWidth() - 1));
            y = Math.max(0, Math.min(y, fullBitmap.getHeight() - 1));
            width = Math.min(width, fullBitmap.getWidth() - x);
            height = Math.min(height, fullBitmap.getHeight() - y);

            Log.d("BlurDebug", "Position: x=" + x + ", y=" + y + ", width=" + width + ", height=" + height);

            // Add this check to prevent IllegalArgumentException if width or height is 0 or less
            if (width <= 0 || height <= 0) {
                Log.e("BlurDebug", "Content container has zero width or height, cannot apply blur.");
                fullBitmap.recycle(); // Important to recycle the fullBitmap if we can't proceed
                requireActivity().runOnUiThread(() -> {
                    blurBackground.setVisibility(View.GONE); // Hide blur background if dimensions are invalid
                });
                return;
            }

            Bitmap croppedBitmap = Bitmap.createBitmap(fullBitmap, x, y, width, height);
            fullBitmap.recycle(); // Recycle fullBitmap after cropping

            float scale = 0.5f;
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, (int) (croppedBitmap.getWidth() * scale), (int) (croppedBitmap.getHeight() * scale), true);
            croppedBitmap.recycle(); // Recycle croppedBitmap

            Bitmap blurredBitmap = blurBitmap(scaledBitmap, requireContext(), 15f);
            Bitmap finalBitmap = Bitmap.createScaledBitmap(blurredBitmap, width, height, true);
            if (scaledBitmap != blurredBitmap) {
                blurredBitmap.recycle(); // Recycle blurredBitmap if it's a new instance
            }
            scaledBitmap.recycle(); // Recycle scaledBitmap

            requireActivity().runOnUiThread(() -> {
                blurBackground.setImageBitmap(finalBitmap);
                blurBackground.setVisibility(View.VISIBLE);
            });
        });
    }

    private Bitmap blurBitmap(Bitmap bitmap, Context context, float radius) {
        RenderScript renderScript = RenderScript.create(context);
        Allocation input = Allocation.createFromBitmap(renderScript, bitmap);
        Allocation output = Allocation.createTyped(renderScript, input.getType());

        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
        blurScript.setInput(input);
        blurScript.setRadius(Math.min(radius, 25f));
        blurScript.forEach(output);
        output.copyTo(bitmap);

        input.destroy();
        output.destroy();
        blurScript.destroy();
        renderScript.destroy();

        return bitmap;
    }

    private void setupRecyclerView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        adapter = new AppGridAdapter(new ArrayList<>(), getContext());
        recyclerView.setAdapter(adapter);
        refreshHandler = new Handler(Looper.getMainLooper());
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                refreshRecentApps();
                refreshHandler.postDelayed(this, REFRESH_INTERVAL);
            }
        };
        refreshHandler.post(refreshRunnable);
    }

    private void refreshRecentApps() {
        UsageStatsManager usm = (UsageStatsManager) requireContext().getSystemService(Context.USAGE_STATS_SERVICE);
        ActivityManager activityManager = (ActivityManager) requireContext().getSystemService(Context.ACTIVITY_SERVICE);
        PackageManager pm = requireContext().getPackageManager();

        List<AppInfos> appList = new ArrayList<>();
        Set<String> addedPackages = new HashSet<>();

        long currentTime = System.currentTimeMillis();

        // Try multiple approaches to get recent apps
        Log.d("RecentFragment", "Starting recent apps detection...");

        // Approach 1: Try getRecentTasks (might work on some devices)
        try {
            @SuppressWarnings("deprecation")
            List<ActivityManager.RecentTaskInfo> recentTasks = activityManager.getRecentTasks(20, 0);
            Log.d("RecentFragment", "getRecentTasks returned " + recentTasks.size() + " tasks");

            for (ActivityManager.RecentTaskInfo taskInfo : recentTasks) {
                if (appList.size() >= 9) break;

                if (taskInfo.baseIntent != null && taskInfo.baseIntent.getComponent() != null) {
                    String packageName = taskInfo.baseIntent.getComponent().getPackageName();

                    if (!addedPackages.contains(packageName) &&
                            !packageName.equals(requireContext().getPackageName()) &&
                            !isSystemLauncherApp(packageName)) {

                        if (addAppToList(packageName, pm, appList, addedPackages)) {
                            Log.d("RecentFragment", "Added from recent tasks: " + packageName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w("RecentFragment", "getRecentTasks failed: " + e.getMessage());
        }

        // Approach 2: Use very recent usage stats (last 10 minutes) for apps that were just used
        if (appList.size() < 9) {
            Log.d("RecentFragment", "Trying usage stats approach...");

            long[] timeWindows = {
                    5 * 60 * 1000,   // 5 minutes
                    10 * 60 * 1000,  // 10 minutes
                    30 * 60 * 1000,  // 30 minutes
                    60 * 60 * 1000   // 1 hour
            };

            for (long timeWindow : timeWindows) {
                if (appList.size() >= 9) break;

                long startTime = currentTime - timeWindow;

                try {
                    Map<String, UsageStats> stats = usm.queryAndAggregateUsageStats(startTime, currentTime);
                    Log.d("RecentFragment", "Usage stats for last " + (timeWindow / 60000) + " minutes: " +
                            (stats != null ? stats.size() : 0) + " apps");

                    if (stats != null && !stats.isEmpty()) {
                        List<UsageStats> sortedStats = new ArrayList<>();

                        for (UsageStats stat : stats.values()) {
                            // Look for apps that were used recently and had meaningful interaction
                            if (stat.getLastTimeUsed() > startTime &&
                                    stat.getTotalTimeInForeground() > 1000) { // At least 1 second
                                sortedStats.add(stat);
                            }
                        }

                        // Sort by last used time (most recent first)
                        Collections.sort(sortedStats, (a, b) -> Long.compare(b.getLastTimeUsed(), a.getLastTimeUsed()));

                        for (UsageStats stat : sortedStats) {
                            if (appList.size() >= 9) break;

                            String packageName = stat.getPackageName();

                            if (!addedPackages.contains(packageName) &&
                                    !packageName.equals(requireContext().getPackageName()) &&
                                    !isSystemLauncherApp(packageName)) {

                                if (addAppToList(packageName, pm, appList, addedPackages)) {
                                    long minutesAgo = (currentTime - stat.getLastTimeUsed()) / (60 * 1000);
                                    Log.d("RecentFragment", "Added from usage stats: " + packageName +
                                            " (used " + minutesAgo + " minutes ago)");
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.w("RecentFragment", "Usage stats failed for " + (timeWindow / 60000) + " minutes: " + e.getMessage());
                }
            }
        }

        // Approach 3: Try running app processes (for apps that might still be active)
        if (appList.size() < 9) {
            Log.d("RecentFragment", "Trying running processes approach...");

            try {
                List<ActivityManager.RunningAppProcessInfo> runningProcesses = activityManager.getRunningAppProcesses();
                if (runningProcesses != null) {
                    Log.d("RecentFragment", "Found " + runningProcesses.size() + " running processes");

                    // Sort by importance (foreground apps first)
                    Collections.sort(runningProcesses, (a, b) -> Integer.compare(a.importance, b.importance));

                    for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                        if (appList.size() >= 9) break;

                        // Only consider foreground and visible apps
                        if (processInfo.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
                            for (String packageName : processInfo.pkgList) {
                                if (appList.size() >= 9) break;

                                if (!addedPackages.contains(packageName) &&
                                        !packageName.equals(requireContext().getPackageName()) &&
                                        !isSystemLauncherApp(packageName)) {

                                    if (addAppToList(packageName, pm, appList, addedPackages)) {
                                        Log.d("RecentFragment", "Added from running processes: " + packageName);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.w("RecentFragment", "Running processes failed: " + e.getMessage());
            }
        }

        Log.d("RecentFragment", "Final result: " + appList.size() + " recent apps found");
        if (appList.size() == 0) {
            Log.d("RecentFragment", "No recent apps found");
        }
        adapter.updateAppList(appList);
    }

    private boolean addAppToList(String packageName, PackageManager pm, List<AppInfos> appList, Set<String> addedPackages) {
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);

            // Include all user apps and important system apps
            if (shouldIncludeApp(appInfo, packageName)) {
                String appName = pm.getApplicationLabel(appInfo).toString();
                Drawable appIcon = pm.getApplicationIcon(appInfo);
                appList.add(new AppInfos(appName, appIcon, packageName));
                addedPackages.add(packageName);
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("RecentFragment", "App not found: " + packageName);
        }
        return false;
    }

    private boolean shouldIncludeApp(ApplicationInfo appInfo, String packageName) {
        // Always include non-system apps (user-installed apps)
        if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
            return true;
        }

        // For system apps, be more inclusive
        String packageLower = packageName.toLowerCase();

        // Include common system apps that users interact with
        return isImportantSystemApp(packageName) ||
                packageLower.contains("filemanager") ||
                packageLower.contains("files") ||
                packageLower.contains("cleaner") ||
                packageLower.contains("manager") ||
                packageLower.contains("gallery") ||
                packageLower.contains("camera") ||
                packageLower.contains("music") ||
                packageLower.contains("video") ||
                packageLower.contains("photo") ||
                packageLower.contains("dialer") ||
                packageLower.contains("phone") ||
                packageLower.contains("messaging") ||
                packageLower.contains("mms") ||
                packageLower.contains("sms") ||
                packageLower.contains("calculator") ||
                packageLower.contains("clock") ||
                packageLower.contains("calendar") ||
                packageLower.contains("settings") ||
                packageLower.contains("contacts") ||
                packageLower.contains("browser") ||
                packageLower.contains("chrome") ||
                packageLower.contains("maps") ||
                packageLower.contains("gmail") ||
                packageLower.contains("drive") ||
                packageLower.contains("youtube") ||
                packageLower.contains("playstore") ||
                packageLower.contains("store") ||
                // Check for typical manufacturer apps
                packageLower.contains("miui") ||
                packageLower.contains("xiaomi") ||
                packageLower.contains("samsung") ||
                packageLower.contains("huawei") ||
                packageLower.contains("oppo") ||
                packageLower.contains("vivo") ||
                packageLower.contains("oneplus");
    }

    private boolean isSystemLauncherApp(String packageName) {
        // Don't include system launcher apps or our own launcher
        if (packageName.equals(requireContext().getPackageName())) {
            return true;
        }

        // Check if it's a system launcher
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        PackageManager pm = requireContext().getPackageManager();

        try {
            List<android.content.pm.ResolveInfo> resolveInfos = pm.queryIntentActivities(homeIntent, 0);
            for (android.content.pm.ResolveInfo resolveInfo : resolveInfos) {
                if (packageName.equals(resolveInfo.activityInfo.packageName)) {
                    // Check if it's a system app launcher
                    ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                    return (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                }
            }
        } catch (Exception e) {
            Log.w("RecentFragment", "Error checking launcher apps", e);
        }

        return false;
    }

    private boolean isImportantSystemApp(String packageName) {
        // Include some important system apps that users might want to see in recent
        return packageName.contains("chrome") ||
                packageName.contains("browser") ||
                packageName.contains("camera") ||
                packageName.contains("gallery") ||
                packageName.contains("music") ||
                packageName.contains("video") ||
                packageName.contains("phone") ||
                packageName.contains("contacts") ||
                packageName.contains("messages") ||
                packageName.contains("email") ||
                packageName.contains("calendar");
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) requireContext().getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), requireContext().getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void closeFragment() {
        if (isAdded() && getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.post(refreshRunnable);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    public class AppGridAdapter extends RecyclerView.Adapter<AppGridAdapter.ViewHolder> {

        private final Context context;
        private final List<AppInfos> appList = new ArrayList<>();

        public AppGridAdapter(List<AppInfos> appList, Context context) {
            this.context = context;
            this.appList.addAll(appList);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.grid_app_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AppInfos appInfo = appList.get(position);

            Glide.with(context).load(appInfo.icon).error(R.mipmap.ic_launcher).into(holder.iconView);

            holder.nameView.setText(appInfo.name);
            holder.itemView.setOnClickListener(v -> {
                Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(appInfo.packageName);
                if (launchIntent != null) {
                    context.startActivity(launchIntent);
                } else {
                    Toast.makeText(context, "Could not open app", Toast.LENGTH_SHORT).show();
                }
            });
        }


        @Override
        public int getItemCount() {
            return appList.size();
        }

        public void updateAppList(List<AppInfos> newAppList) {
            final AppDiffCallback diffCallback = new AppDiffCallback(this.appList, newAppList);
            final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

            this.appList.clear();
            this.appList.addAll(newAppList);
            diffResult.dispatchUpdatesTo(this);
        }


        public class ViewHolder extends RecyclerView.ViewHolder {
            final ShapeableImageView iconView;
            final TextView nameView;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                iconView = itemView.findViewById(R.id.app_icon);
                nameView = itemView.findViewById(R.id.app_name);
            }
        }

        public class AppDiffCallback extends DiffUtil.Callback {

            private final List<AppInfos> oldList;
            private final List<AppInfos> newList;

            public AppDiffCallback(List<AppInfos> oldList, List<AppInfos> newList) {
                this.oldList = oldList;
                this.newList = newList;
            }

            @Override
            public int getOldListSize() {
                return oldList.size();
            }

            @Override
            public int getNewListSize() {
                return newList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return oldList.get(oldItemPosition).packageName.equals(newList.get(newItemPosition).packageName);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                AppInfos oldApp = oldList.get(oldItemPosition);
                AppInfos newApp = newList.get(newItemPosition);
                return Objects.equals(oldApp.name, newApp.name);
            }
        }
    }
}