package com.bluelight.computer.winlauncher.prolauncher.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;

import java.util.List;

public class BackgroundColorAdapter extends RecyclerView.Adapter<BackgroundColorAdapter.ColorViewHolder> {

    private final List<Integer> colors;
    private final OnColorSelectedListener listener;
    private int selectedPosition = -1;

    public BackgroundColorAdapter(Context context, List<Integer> colors, int initialColor, OnColorSelectedListener listener) {
        this.colors = colors;
        this.listener = listener;
        if (colors.contains(initialColor)) {
            this.selectedPosition = colors.indexOf(initialColor);
        }
    }

    @NonNull
    @Override
    public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_background_color, parent, false);
        return new ColorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
        int color = colors.get(position);


        GradientDrawable background = (GradientDrawable) holder.colorView.getBackground().mutate();
        background.setColor(color);

        holder.checkmark.setVisibility(position == selectedPosition ? View.VISIBLE : View.GONE);


        if (position == selectedPosition) {

            int strokeWidth = 6;
            int strokeColor = R.color.colorPrimary;
            background.setStroke(strokeWidth, strokeColor);
        } else {

            background.setStroke(0, Color.TRANSPARENT);
        }


        holder.itemView.setOnClickListener(v -> {
            int clickedPosition = holder.getBindingAdapterPosition();
            if (clickedPosition == RecyclerView.NO_POSITION || clickedPosition == selectedPosition) {
                return;
            }

            int previousSelected = selectedPosition;
            selectedPosition = clickedPosition;


            if (previousSelected != -1) {
                notifyItemChanged(previousSelected);
            }
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onBackgroundColorSelected(colors.get(selectedPosition));
            }
        });
    }

    @Override
    public int getItemCount() {
        return colors.size();
    }

    public interface OnColorSelectedListener {
        void onBackgroundColorSelected(int color);
    }

    static class ColorViewHolder extends RecyclerView.ViewHolder {
        View colorView;
        ImageView checkmark;

        ColorViewHolder(@NonNull View itemView) {
            super(itemView);
            colorView = itemView.findViewById(R.id.color_view);
            checkmark = itemView.findViewById(R.id.checkmark);
        }
    }
}