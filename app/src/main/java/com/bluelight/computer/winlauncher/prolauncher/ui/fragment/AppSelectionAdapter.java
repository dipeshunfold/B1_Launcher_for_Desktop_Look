package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.model.AppInfo;

import java.util.ArrayList;
import java.util.List;

public class AppSelectionAdapter extends RecyclerView.Adapter<AppSelectionAdapter.AppViewHolder> {

    private static final float ADAPTIVE_ICON_SCALE_FACTOR = 1.4f;
    private final List<AppInfo> allApps;
    private final OnAppSelectedListener listener;
    private final List<AppInfo> filteredApps;

    public AppSelectionAdapter(List<AppInfo> apps, OnAppSelectedListener listener) {
        this.allApps = new ArrayList<>(apps);
        this.filteredApps = new ArrayList<>(apps);
        this.listener = listener;
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_selection, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppInfo app = filteredApps.get(position);
        holder.bind(app);
    }

    @Override
    public int getItemCount() {
        return filteredApps.size();
    }


    public void filter(String query) {
        filteredApps.clear();
        if (query.isEmpty()) {
            filteredApps.addAll(allApps);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (AppInfo app : allApps) {
                if (app.label.toString().toLowerCase().contains(lowerCaseQuery)) {
                    filteredApps.add(app);
                }
            }
        }
        notifyDataSetChanged();
    }

    private Bitmap createIconBitmap(Drawable drawable) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable adaptiveIcon = (AdaptiveIconDrawable) drawable;

            int width = adaptiveIcon.getIntrinsicWidth();
            int height = adaptiveIcon.getIntrinsicHeight();

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);


            Drawable backgroundDrawable = adaptiveIcon.getBackground();
            if (backgroundDrawable != null) {
                backgroundDrawable.setBounds(0, 0, width, height);
                backgroundDrawable.draw(canvas);
            }


            Drawable foregroundDrawable = adaptiveIcon.getForeground();
            if (foregroundDrawable != null) {

                int scaledSize = (int) (width * ADAPTIVE_ICON_SCALE_FACTOR);
                int offset = (width - scaledSize) / 2;


                foregroundDrawable.setBounds(offset, offset, scaledSize + offset, scaledSize + offset);
                foregroundDrawable.draw(canvas);
            }

            return bitmap;
        } else {
            if (drawable instanceof BitmapDrawable) {

                return ((BitmapDrawable) drawable).getBitmap();
            }


            int width = drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : 100;
            int height = drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : 100;

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);

            return bitmap;
        }
    }


    public interface OnAppSelectedListener {
        void onAppSelected(AppInfo app);
    }

    class AppViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView label;

        AppViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.app_icon);
            label = itemView.findViewById(R.id.app_label);
        }

        void bind(final AppInfo app) {
            icon.setImageDrawable(app.icon);
            Bitmap iconBitmap = createIconBitmap(app.icon);
            icon.setImageBitmap(iconBitmap);
            label.setText(app.label);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAppSelected(app);
                }
            });
        }
    }

}