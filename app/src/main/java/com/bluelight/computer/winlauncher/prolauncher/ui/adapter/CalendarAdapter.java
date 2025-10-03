package com.bluelight.computer.winlauncher.prolauncher.ui.adapter;

import android.graphics.Color;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {

    private final LocalDate today;
    private final List<LocalDate> days;

    private int todayPosition = -1;

    public CalendarAdapter(ArrayList<LocalDate> days) {
        this.days = days;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.today = LocalDate.now();

            this.todayPosition = this.days.indexOf(this.today);
        } else {
            this.today = null;
        }
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.day_cell, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LocalDate date = days.get(position);
        holder.bind(date, position);
    }

    @Override
    public int getItemCount() {
        return days.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView dayOfMonth;
        final int defaultTextColor;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dayOfMonth = itemView.findViewById(R.id.tv_day_cell);

            defaultTextColor = ContextCompat.getColor(itemView.getContext(), R.color.black);
        }


        void bind(LocalDate date, int position) {
            if (date == null) {

                dayOfMonth.setText("");
                dayOfMonth.setBackground(null);
            } else {
                dayOfMonth.setText(String.valueOf(date.getDayOfMonth()));


                if (position == todayPosition) {

                    dayOfMonth.setBackgroundResource(R.drawable.ic_launcher_background);
                    dayOfMonth.setTextColor(Color.WHITE);
                } else {


                    dayOfMonth.setBackground(null);
                    dayOfMonth.setTextColor(defaultTextColor);
                }
            }
        }
    }
}