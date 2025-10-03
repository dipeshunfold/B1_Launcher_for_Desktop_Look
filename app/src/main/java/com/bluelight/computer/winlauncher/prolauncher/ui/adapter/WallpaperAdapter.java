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

public class WallpaperAdapter extends RecyclerView.Adapter<WallpaperAdapter.WallpaperViewHolder> {

    private final List<Integer> wallpaperResIds;
    private final OnWallpaperClickListener listener;

    public WallpaperAdapter(List<Integer> wallpaperResIds, OnWallpaperClickListener listener) {
        this.wallpaperResIds = wallpaperResIds;
        this.listener = listener;
    }

    @NonNull
    @Override
    public WallpaperViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wallpaper, parent, false);
        return new WallpaperViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WallpaperViewHolder holder, int position) {
        int resId = wallpaperResIds.get(position);


        Glide.with(holder.itemView.getContext())
                .load(resId)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(holder.imageView);

        holder.itemView.setOnClickListener(v -> listener.onWallpaperClick(resId));
    }

    @Override
    public int getItemCount() {
        return wallpaperResIds.size();
    }

    public interface OnWallpaperClickListener {
        void onWallpaperClick(int resId);
    }

    static class WallpaperViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public WallpaperViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.wallpaperImageView);
        }
    }
}