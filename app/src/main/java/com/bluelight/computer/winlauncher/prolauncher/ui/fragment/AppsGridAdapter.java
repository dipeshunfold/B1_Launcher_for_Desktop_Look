package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

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

public class AppsGridAdapter extends RecyclerView.Adapter<AppsGridAdapter.AppViewHolder> {

    private final List<ResolveInfo> apps;
    private final PackageManager packageManager;
    private final OnAppClickListener listener;

    public AppsGridAdapter(List<ResolveInfo> apps, PackageManager packageManager, OnAppClickListener listener) {
        this.apps = apps;
        this.packageManager = packageManager;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_grid_social, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        ResolveInfo appInfo = apps.get(position);
        holder.bind(appInfo, packageManager, listener);
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    public interface OnAppClickListener {
        void onAppClick(ResolveInfo app);
    }

    static class AppViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;

        AppViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.iv_app_icon_grid);
            appName = itemView.findViewById(R.id.tv_app_name_grid);
            appName.setSelected(true);
        }

        void bind(final ResolveInfo appInfo, PackageManager pm, final OnAppClickListener listener) {
            if (appName != null) {
                appName.setText(appInfo.loadLabel(pm));
            }
            if (appIcon != null) {
                appIcon.setImageDrawable(appInfo.loadIcon(pm));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAppClick(appInfo);
                }
            });
        }
    }
}