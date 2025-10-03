package com.bluelight.computer.winlauncher.prolauncher.ui.adapter;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;

import java.util.List;
import java.util.Set;

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.AppViewHolder> {

    private final List<ResolveInfo> appList;
    private final PackageManager packageManager;
    private final OnAppActionListener listener;
    private final Set<String> presentOnHomePackages;
    private final Set<String> quickAccessPackages;

    public AppListAdapter(List<ResolveInfo> appList, PackageManager pm, Set<String> present, Set<String> quickAccess, OnAppActionListener listener) {
        this.appList = appList;
        this.packageManager = pm;
        this.presentOnHomePackages = present;
        this.quickAccessPackages = quickAccess;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_app, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        holder.bind(appList.get(position));
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }


    public enum AppAction {ADD, REMOVE}

    public interface OnAppActionListener {
        void onAppAction(ResolveInfo appInfo, AppAction action);

        void onAppLongClick(ResolveInfo appInfo, boolean isCurrentlyInQuickAccess);
    }

    class AppViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon, actionIcon, starIcon;
        TextView appName;

        AppViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.app_icon);
            appName = itemView.findViewById(R.id.app_name);
            actionIcon = itemView.findViewById(R.id.action_icon);
            starIcon = itemView.findViewById(R.id.quick_access_star);
        }

        void bind(final ResolveInfo resolveInfo) {
            appName.setText(resolveInfo.loadLabel(packageManager));


            com.bluelight.computer.winlauncher.prolauncher.ui.adapter.CustomIconLoader.loadIcon(resolveInfo, packageManager, appIcon);


            final String packageName = resolveInfo.activityInfo.packageName;
            final boolean isPresent = presentOnHomePackages.contains(packageName);
            final boolean isQuickAccess = quickAccessPackages.contains(packageName);
            actionIcon.setImageResource(isPresent ? R.drawable.ic_remove_circle : R.drawable.ic_add_circle);
            starIcon.setImageResource(isQuickAccess ? R.drawable.ic_star_filled : R.drawable.ic_star_border);
            itemView.setOnClickListener(v -> listener.onAppAction(resolveInfo, isPresent ? AppAction.REMOVE : AppAction.ADD));
            itemView.setOnLongClickListener(v -> {
                listener.onAppLongClick(resolveInfo, isQuickAccess);
                return true;
            });
        }
    }
}