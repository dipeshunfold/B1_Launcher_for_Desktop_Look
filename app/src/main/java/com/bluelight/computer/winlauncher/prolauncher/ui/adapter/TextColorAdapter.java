package com.bluelight.computer.winlauncher.prolauncher.ui.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;

import java.util.Arrays;
import java.util.List;

public class TextColorAdapter extends RecyclerView.Adapter<TextColorAdapter.TextColorViewHolder> {
    private final List<Integer> textColors = Arrays.asList(Color.BLACK, Color.WHITE);
    private final OnTextColorSelectedListener listener;
    private int selectedPosition;


    public TextColorAdapter(int initialColor, OnTextColorSelectedListener listener) {
        this.listener = listener;
        this.selectedPosition = textColors.indexOf(initialColor);
        if (this.selectedPosition == -1) {
            this.selectedPosition = 1;
        }
    }

    @NonNull
    @Override
    public TextColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_text_color, parent, false);
        return new TextColorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TextColorViewHolder holder, int position) {
        int color = textColors.get(position);


        GradientDrawable background = (GradientDrawable) holder.colorView.getBackground().mutate();
        background.setColor(color);

        holder.checkmark.setVisibility(position == selectedPosition ? View.VISIBLE : View.GONE);
        holder.checkmark.setColorFilter(color == Color.BLACK ? Color.WHITE : Color.BLACK);


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
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onTextColorSelected(textColors.get(selectedPosition));
            }
        });
    }

    @Override
    public int getItemCount() {
        return textColors.size();
    }


    public interface OnTextColorSelectedListener {
        void onTextColorSelected(int color);
    }


    static class TextColorViewHolder extends RecyclerView.ViewHolder {
        View colorView;
        ImageView checkmark;

        TextColorViewHolder(@NonNull View itemView) {
            super(itemView);
            colorView = itemView.findViewById(R.id.text_color_view);
            checkmark = itemView.findViewById(R.id.text_checkmark);
        }
    }
}