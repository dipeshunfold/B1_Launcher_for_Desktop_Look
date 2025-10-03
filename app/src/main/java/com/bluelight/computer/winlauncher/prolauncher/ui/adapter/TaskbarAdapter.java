package com.bluelight.computer.winlauncher.prolauncher.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.database.AppItem;
import com.bluelight.computer.winlauncher.prolauncher.model.DefaultTaskbarApp;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TaskbarAdapter extends RecyclerView.Adapter<TaskbarAdapter.ViewHolder> {

    private final Context context;
    private final List<Object> combinedItems = new ArrayList<>();

    public TaskbarAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.taskbar_icon_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Object item = combinedItems.get(position);
        PackageManager pm = context.getPackageManager();
        String packageName = getPackageNameFromItem(item);


        Object glideLoadModel = getGlideLoadModel(item);


        Glide.with(context)
                .load(glideLoadModel)
                .error(R.mipmap.ic_launcher)
                .override(100, 100)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.iconView);

        holder.itemView.setOnClickListener(v -> {
            if (packageName != null) {
                Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
                if (launchIntent != null) {
                    context.startActivity(launchIntent);
                } else {
                    Toast.makeText(context, "App not found", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    @Nullable
    private Object getGlideLoadModel(Object item) {
        if (item instanceof AppItem) {
            AppItem appItem = (AppItem) item;

            if (appItem.customIconPath != null && !appItem.customIconPath.isEmpty()) {
                File iconFile = new File(appItem.customIconPath);
                if (iconFile.exists()) {
                    return iconFile;
                }
            }

            try {
                return context.getPackageManager().getApplicationIcon(appItem.packageName);
            } catch (PackageManager.NameNotFoundException e) {
                return null;
            }
        } else if (item instanceof DefaultTaskbarApp) {

            return ((DefaultTaskbarApp) item).icon;
        }
        return null;
    }


    @Nullable
    private String getPackageNameFromItem(Object item) {
        if (item instanceof AppItem) {
            return ((AppItem) item).packageName;
        } else if (item instanceof DefaultTaskbarApp) {
            return ((DefaultTaskbarApp) item).packageName;
        }
        return null;
    }


    @Override
    public int getItemCount() {
        return combinedItems.size();
    }


    public void setTaskbarItems(List<Object> newItems) {
        final TaskbarDiffCallback diffCallback = new TaskbarDiffCallback(this.combinedItems, newItems);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.combinedItems.clear();
        this.combinedItems.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView iconView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.taskbar_app_icon);
        }
    }


    private class TaskbarDiffCallback extends DiffUtil.Callback {
        private final List<Object> oldList;
        private final List<Object> newList;

        public TaskbarDiffCallback(List<Object> oldList, List<Object> newList) {
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
            String oldPackage = getPackageNameFromItem(oldList.get(oldItemPosition));
            String newPackage = getPackageNameFromItem(newList.get(newItemPosition));

            return oldPackage != null && oldPackage.equals(newPackage);
        }


        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Object oldItem = oldList.get(oldItemPosition);
            Object newItem = newList.get(newItemPosition);

            if (oldItem instanceof AppItem && newItem instanceof AppItem) {

                String oldPath = ((AppItem) oldItem).customIconPath;
                String newPath = ((AppItem) newItem).customIconPath;
                return Objects.equals(oldPath, newPath);
            }

            return true;
        }
    }
}