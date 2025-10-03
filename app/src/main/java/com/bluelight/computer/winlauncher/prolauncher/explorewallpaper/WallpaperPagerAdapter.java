package com.bluelight.computer.winlauncher.prolauncher.explorewallpaper; // Your package

// ... (other imports)

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

public class WallpaperPagerAdapter extends RecyclerView.Adapter<WallpaperPagerAdapter.WallpaperPagerViewHolder> {

    private final List<String> wallpaperUrls;
    private final OnWallpaperClickListener listener; // Member variable to hold the listener

    // Constructor updated to include listener
    public WallpaperPagerAdapter(List<String> wallpaperUrls, OnWallpaperClickListener listener) {
        this.wallpaperUrls = wallpaperUrls;
        this.listener = listener;
    }

    // Overloaded constructor for cases where no listener is immediately needed (e.g., in WallpaperPreviewActivity)
    public WallpaperPagerAdapter(List<String> wallpaperUrls) {
        this(wallpaperUrls, null); // Call the other constructor with a null listener
    }

    @NonNull
    @Override
    public WallpaperPagerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wallpaper_viewpager, parent, false);
        return new WallpaperPagerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WallpaperPagerViewHolder holder, int position) {
        String url = wallpaperUrls.get(position);
        Glide.with(holder.itemView.getContext())
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(holder.imageView);

        // Set click listener for the item
        if (listener != null) {
            holder.itemView.setOnClickListener(v -> listener.onWallpaperClick(url));
        }
    }

    @Override
    public int getItemCount() {
        return wallpaperUrls.size();
    }

    /**
     * Interface definition for a callback to be invoked when a wallpaper item is clicked.
     */
    public interface OnWallpaperClickListener {
        /**
         * Called when a wallpaper item in the ViewPager is clicked.
         *
         * @param wallpaperUrl The URL of the clicked wallpaper.
         */
        void onWallpaperClick(String wallpaperUrl);
    }

    static class WallpaperPagerViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public WallpaperPagerViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.wallpaperImageViewPager);
        }
    }
}