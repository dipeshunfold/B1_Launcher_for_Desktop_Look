package com.bluelight.computer.winlauncher.prolauncher.explorewallpaper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class SliderAdapter extends RecyclerView.Adapter<SliderAdapter.SliderViewHolder> {

    private static final int MULTIPLIER = 1000;
    private final Context context;
    private final List<String> originalImageUrls;
    private final List<String> infiniteImageUrls;
    private final OnSliderItemClickListener listener;

    public SliderAdapter(Context context, List<String> imageUrls, OnSliderItemClickListener listener) {
        this.context = context;
        this.originalImageUrls = imageUrls;
        this.listener = listener;
        this.infiniteImageUrls = new ArrayList<>();
        for (int i = 0; i < MULTIPLIER; i++) {
            infiniteImageUrls.addAll(imageUrls);
        }
    }

    @NonNull
    @Override
    public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_slider_image, parent, false);
        return new SliderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
        String imageUrl = infiniteImageUrls.get(position);
        Glide.with(context)
                .load(imageUrl)
                .centerCrop()
                .into(holder.imageView);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                int originalPosition = position % originalImageUrls.size();
                listener.onSliderItemClick(originalImageUrls.get(originalPosition));
            }
        });
    }

    @Override
    public int getItemCount() {
        return infiniteImageUrls.size();
    }

    public int getOriginalItemCount() {
        return originalImageUrls.size();
    }

    public interface OnSliderItemClickListener {
        void onSliderItemClick(String imageUrl);
    }

    public static class SliderViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public SliderViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.sliderImageView);
        }
    }
}