package com.bluelight.computer.winlauncher.prolauncher.explorewallpaper;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.List;

public class CategoryWallpaperAdapter extends RecyclerView.Adapter<CategoryWallpaperAdapter.WallpaperViewHolder> {

    private final Context context;
    private final List<String> wallpaperUrls;
    private final OnWallpaperClickListener listener;

    public CategoryWallpaperAdapter(Context context, List<String> wallpaperUrls, OnWallpaperClickListener listener) {
        this.context = context;
        this.wallpaperUrls = wallpaperUrls;
        this.listener = listener;
    }

    @NonNull
    @Override
    public WallpaperViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_wallpaper, parent, false);
        return new WallpaperViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WallpaperViewHolder holder, int position) {
        String imageUrl = wallpaperUrls.get(position);

        // Show a placeholder while loading
//        holder.wallpaperImage.setImageResource(R.drawable.placeholder);

        Glide.with(context)
                .load(imageUrl)
                .centerCrop()
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                Target<Drawable> target, boolean isFirstResource) {
                        Log.e("GlideError", "Failed to load: " + imageUrl, e);
                        // Set a placeholder on error
//                        holder.wallpaperImage.setImageResource(R.drawable.error_placeholder);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                                                   Target<Drawable> target, DataSource dataSource,
                                                   boolean isFirstResource) {
                        return false;
                    }
                })
                .into(holder.wallpaperImage);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onWallpaperClick(imageUrl);
            }
        });
    }

    @Override
    public int getItemCount() {
        return wallpaperUrls.size();
    }

    public interface OnWallpaperClickListener {
        void onWallpaperClick(String imageUrl);
    }

    public static class WallpaperViewHolder extends RecyclerView.ViewHolder {
        ImageView wallpaperImage;

        public WallpaperViewHolder(@NonNull View itemView) {
            super(itemView);
            wallpaperImage = itemView.findViewById(R.id.wallpaperImageView);
        }
    }
}