package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

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
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RecentFragment extends Fragment {


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
        UsageStatsManager usm = (UsageStatsManager) requireContext().getSystemService(Context.USAGE_STATS_SERVICE);
        long currentTime = System.currentTimeMillis();
        long startTime = currentTime - 1000L * 3600 * 24 * 7;
        Map<String, UsageStats> aggregatedStats = usm.queryAndAggregateUsageStats(startTime, currentTime);
        List<UsageStats> usageStatsList = new ArrayList<>(aggregatedStats.values());
        Collections.sort(usageStatsList, (o1, o2) -> Long.compare(o2.getLastTimeUsed(), o1.getLastTimeUsed()));

        PackageManager pm = requireContext().getPackageManager();
        List<AppInfos> appList = new ArrayList<>();
        int count = 0;
        for (UsageStats stats : usageStatsList) {
            if (count >= 9) break;
            try {
                if (stats.getLastTimeUsed() == 0) continue;

                ApplicationInfo appInfo = pm.getApplicationInfo(stats.getPackageName(), 0);
                if (!appInfo.packageName.equals(requireContext().getPackageName()) && (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    String appName = pm.getApplicationLabel(appInfo).toString();
                    Drawable appIcon = pm.getApplicationIcon(appInfo);
                    String packageName = stats.getPackageName();
                    appList.add(new AppInfos(appName, appIcon, packageName));
                    count++;
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
        }

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerView.setAdapter(new AppGridAdapter(appList, getContext()));
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