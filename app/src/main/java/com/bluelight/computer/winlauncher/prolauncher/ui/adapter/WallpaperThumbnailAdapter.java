package com.bluelight.computer.winlauncher.prolauncher.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

public class WallpaperThumbnailAdapter extends RecyclerView.Adapter<WallpaperThumbnailAdapter.ViewHolder> {

    private final List<Integer> wallpaperIds;
    private final OnWallpaperClickListener listener;
    private int selectedPosition = 0;

    public WallpaperThumbnailAdapter(List<Integer> wallpaperIds, OnWallpaperClickListener listener) {
        this.wallpaperIds = wallpaperIds;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wallpaper_thumbnail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int wallpaperResId = wallpaperIds.get(position);


        Glide.with(holder.itemView.getContext())
                .load(wallpaperResId)
                .skipMemoryCache(true)  // Avoid stale caches
                .diskCacheStrategy(DiskCacheStrategy.NONE)  // No disk for tiny thumbs
                .override(160, 240)  // Force tiny size (adjust to your XML: e.g., if thumbnails are 100dp wide, use 100 * density)                .centerCrop()
                .into(holder.thumbnail);


        holder.selectionBorder.setVisibility(position == selectedPosition ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (selectedPosition != holder.getAdapterPosition()) {

                notifyItemChanged(selectedPosition);
                selectedPosition = holder.getAdapterPosition();
                notifyItemChanged(selectedPosition);

                listener.onWallpaperClick(wallpaperIds.get(selectedPosition));
            }
        });
    }

    @Override
    public int getItemCount() {
        return wallpaperIds.size();
    }

    public int getSelectedWallpaperId() {
        if (wallpaperIds != null && selectedPosition < wallpaperIds.size()) {
            return wallpaperIds.get(selectedPosition);
        }

        return R.drawable.wallpaper_0;
    }

    public interface OnWallpaperClickListener {
        void onWallpaperClick(int wallpaperResId);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        View selectionBorder;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.img_thumbnail);
            selectionBorder = itemView.findViewById(R.id.selection_border);
        }
    }
}