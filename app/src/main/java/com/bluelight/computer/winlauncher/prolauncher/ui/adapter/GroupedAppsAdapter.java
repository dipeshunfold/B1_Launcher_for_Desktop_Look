package com.bluelight.computer.winlauncher.prolauncher.ui.adapter;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.database.AppDatabase1;
import com.bluelight.computer.winlauncher.prolauncher.model.AppInfo;
import com.bluelight.computer.winlauncher.prolauncher.ui.fragment.AppSelectionAdapter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GroupedAppsAdapter extends RecyclerView.Adapter<GroupedAppsAdapter.ViewHolder> {

    private final Context context;
    private final List<AppInfo> groupedAppsList;
    private final String groupName;

    public GroupedAppsAdapter(Context context, List<AppInfo> groupedAppsList, String groupName) {
        this.context = context;
        this.groupedAppsList = groupedAppsList;
        this.groupName = groupName;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_app_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo app = groupedAppsList.get(position);
        holder.appName.setText(app.label);
        holder.appName.setSelected(true);


        if (app.icon != null) {


            Glide.with(context)
                    .load(app.icon)
                    .override(100, 100)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.appIcon);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(app.packageName.toString());
            if (launchIntent != null) {
                context.startActivity(launchIntent);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            showPopupMenu(v, app, position);
            return true;
        });
    }


    private void showPopupMenu(View anchorView, AppInfo app, int position) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customMenuView = inflater.inflate(R.layout.popup_custom_menu, null);
        PopupWindow popupWindow = new PopupWindow(customMenuView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(20);
        LinearLayout menuItemsContainer = customMenuView.findViewById(R.id.menu_items_container);

        addMenuItem(menuItemsContainer, R.drawable.ic_open, "Open", () -> {
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(app.packageName.toString());
            if (launchIntent != null) context.startActivity(launchIntent);
            else Toast.makeText(context, "Could not open app", Toast.LENGTH_SHORT).show();
            popupWindow.dismiss();
        });

        addMenuItem(menuItemsContainer, R.drawable.ic_change_app, "Change App", () -> {
            showAppSelectionDialog(position);
            popupWindow.dismiss();
        });

        addMenuItem(menuItemsContainer, R.drawable.ic_remove, "Remove", () -> {
            removeAppFromGroup(app, position);
            popupWindow.dismiss();
        });

        popupWindow.showAsDropDown(anchorView);
    }

    private void showAppSelectionDialog(int position) {

        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setContentView(R.layout.dialog_add_app);


        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(lp);
        }


        EditText searchField = dialog.findViewById(R.id.et_search_apps);
        RecyclerView recyclerView = dialog.findViewById(R.id.rv_apps);
        Button cancelButton = dialog.findViewById(R.id.button_cancel);

        recyclerView.setLayoutManager(new GridLayoutManager(context, 3));


        PackageManager pm = context.getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> allAppsResolveInfo = pm.queryIntentActivities(mainIntent, 0);


        List<AppInfo> appListForAdapter = new ArrayList<>();
        for (ResolveInfo ri : allAppsResolveInfo) {
            AppInfo app = new AppInfo();
            app.label = ri.loadLabel(pm).toString();
            app.packageName = ri.activityInfo.packageName;
            app.icon = ri.loadIcon(pm);
            appListForAdapter.add(app);
        }


        AppSelectionAdapter adapter = new AppSelectionAdapter(appListForAdapter, selectedApp -> {

            updateAppInGroup(position, selectedApp);
            dialog.dismiss();
        });
        recyclerView.setAdapter(adapter);


        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());


        dialog.show();
    }

    private void updateAppInGroup(int position, AppInfo newApp) {

        String oldPackageName = groupedAppsList.get(position).packageName.toString();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {

            AppDatabase1.getDatabase(context).groupedAppDao()
                    .updateAppInGroup(oldPackageName, newApp.packageName.toString(), groupName);


            handler.post(() -> {
                if (position >= 0 && position < groupedAppsList.size()) {


                    groupedAppsList.set(position, newApp);

                    this.notifyItemChanged(position);
                    Toast.makeText(context, "App changed successfully!", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void addMenuItem(LinearLayout container, int iconResId, String title, Runnable action) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View menuItemView = inflater.inflate(R.layout.item_custom_menu, container, false);
        ImageView iconView = menuItemView.findViewById(R.id.menu_item_icon);
        TextView titleView = menuItemView.findViewById(R.id.menu_item_title);
        iconView.setImageResource(iconResId);
        titleView.setText(title);
        menuItemView.setOnClickListener(v -> action.run());
        container.addView(menuItemView);
    }

    private void removeAppFromGroup(AppInfo app, int position) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            AppDatabase1.getDatabase(context).groupedAppDao()
                    .deleteAppFromGroup(app.packageName.toString(), groupName);
            handler.post(() -> {
                groupedAppsList.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, groupedAppsList.size());
                Toast.makeText(context, app.label + " removed.", Toast.LENGTH_SHORT).show();
            });
        });
    }

    @Override
    public int getItemCount() {
        return groupedAppsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView appName;
        ImageView appIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            appName = itemView.findViewById(R.id.tv_app_name_grid);
            appIcon = itemView.findViewById(R.id.iv_app_icon_grid);
            appName.setSelected(true);
        }
    }
}