package com.bluelight.computer.winlauncher.prolauncher.explorewallpaper;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Map;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private final Context context;
    // The data for this adapter will be a List of Map.Entry objects
    private final List<Map.Entry<String, Integer>> categoryList;
    private final OnCategoryClickListener listener;

    public CategoryAdapter(Context context, List<Map.Entry<String, Integer>> categoryList, OnCategoryClickListener listener) {
        this.context = context;
        this.categoryList = categoryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the same layout you used for GridLayout items
        View view = LayoutInflater.from(context).inflate(R.layout.item_category_button, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Map.Entry<String, Integer> entry = categoryList.get(position);
        String categoryName = entry.getKey();
        int imageCount = entry.getValue();

        // Construct the thumbnail URL
        String thumbnailUrl = ApiService.BASE_URL + categoryName + "/thumbnail.png";
        Glide.with(context)
                .load(thumbnailUrl)
                .centerCrop() // Or fitCenter, depending on your design
                .into(holder.categoryImage);

        // Format and set the display title
        String displayTitle = categoryName.replace("test_", "");
        displayTitle = displayTitle.substring(0, 1).toUpperCase() + displayTitle.substring(1);
        holder.categoryTitle.setText(displayTitle);
        Log.d("CategoryAdapter", "Binding category: " + categoryName + ", Thumbnail: " + thumbnailUrl); // Debug log

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryClick(categoryName, imageCount);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    public interface OnCategoryClickListener {
        void onCategoryClick(String categoryName, int imageCount);
    }

    // ViewHolder class
    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView categoryImage;
        TextView categoryTitle;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryImage = itemView.findViewById(R.id.categoryImage);
            categoryTitle = itemView.findViewById(R.id.categoryTitle);
        }
    }
}
