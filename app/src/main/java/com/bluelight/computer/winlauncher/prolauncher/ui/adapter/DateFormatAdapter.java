package com.bluelight.computer.winlauncher.prolauncher.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;

import java.util.List;

public class DateFormatAdapter extends RecyclerView.Adapter<DateFormatAdapter.FormatViewHolder> {

    private final List<String> dateFormats;
    private final OnFormatSelectedListener listener;
    private final int selectedPosition;

    public DateFormatAdapter(List<String> dateFormats, String currentFormat, OnFormatSelectedListener listener) {
        this.dateFormats = dateFormats;
        this.listener = listener;
        this.selectedPosition = dateFormats.indexOf(currentFormat);
    }

    @NonNull
    @Override
    public FormatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_date_format, parent, false);
        return new FormatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FormatViewHolder holder, int position) {
        String format = dateFormats.get(position);
        holder.bind(format, position == selectedPosition);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFormatSelected(format);
            }
        });
    }

    @Override
    public int getItemCount() {
        return dateFormats.size();
    }

    public interface OnFormatSelectedListener {
        void onFormatSelected(String format);
    }

    static class FormatViewHolder extends RecyclerView.ViewHolder {
        RadioButton radioButton;
        TextView formatText;

        FormatViewHolder(@NonNull View itemView) {
            super(itemView);
            radioButton = itemView.findViewById(R.id.date_format_radio_button);
            formatText = itemView.findViewById(R.id.date_format_text);
        }

        void bind(String format, boolean isSelected) {
            formatText.setText(format);
            radioButton.setChecked(isSelected);
        }
    }
}