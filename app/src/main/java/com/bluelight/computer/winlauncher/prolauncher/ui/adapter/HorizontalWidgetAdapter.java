package com.bluelight.computer.winlauncher.prolauncher.ui.adapter;

import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;

import java.util.List;
import java.util.Set;

public class HorizontalWidgetAdapter extends RecyclerView.Adapter<HorizontalWidgetAdapter.WidgetViewHolder> {

    private final Context context;
    private final List<AppWidgetProviderInfo> widgetList;
    private final Set<String> presentOnHomeProviders;
    private final WidgetListAdapter.OnWidgetActionListener listener; // Re-use the existing listener interface

    public HorizontalWidgetAdapter(Context context, List<AppWidgetProviderInfo> widgetList, Set<String> presentOnHomeProviders, WidgetListAdapter.OnWidgetActionListener listener) {
        this.context = context;
        this.widgetList = widgetList;
        this.presentOnHomeProviders = presentOnHomeProviders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public WidgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_widget_horizontal, parent, false);
        return new WidgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WidgetViewHolder holder, int position) {
        holder.bind(widgetList.get(position));
    }

    @Override
    public int getItemCount() {
        return widgetList.size();
    }

    class WidgetViewHolder extends RecyclerView.ViewHolder {
        ImageView widgetPreview, actionIcon;
        TextView widgetName;
        TextView widgetSize;

        WidgetViewHolder(@NonNull View itemView) {
            super(itemView);
            widgetPreview = itemView.findViewById(R.id.widget_preview);
            widgetName = itemView.findViewById(R.id.widget_name);
            actionIcon = itemView.findViewById(R.id.action_icon);
            widgetSize = itemView.findViewById(R.id.widget_size);
        }

        void bind(final AppWidgetProviderInfo widgetInfo) {
            widgetName.setText(widgetInfo.loadLabel(context.getPackageManager()));
            widgetPreview.setImageDrawable(widgetInfo.loadPreviewImage(context, 0));

            // Calculate approximate widget size in grid cells (e.g., 2x2)
            // minWidth/minHeight are in dp, we need to convert to an approximate grid size.
            // A common approach is to divide by a base cell size (e.g., 40-50dp)
            int minWidthDp = widgetInfo.minWidth;
            int minHeightDp = widgetInfo.minHeight;

            // Using a common base of ~40-50dp per grid unit. Adjust if your launcher's grid is different.
            // This is an estimation, as actual grid units can vary by launcher.
            int cols = (int) Math.max(1, Math.round(minWidthDp / 48.0));
            int rows = (int) Math.max(1, Math.round(minHeightDp / 48.0));

            widgetSize.setText(String.format("%d × %d", cols, rows));

            final String providerString = widgetInfo.provider.flattenToString();
            final boolean isPresent = presentOnHomeProviders.contains(providerString);
            actionIcon.setImageResource(isPresent ? R.drawable.ic_remove_circle : R.drawable.ic_add_circle);
            itemView.setOnClickListener(v -> listener.onWidgetAction(widgetInfo, isPresent ? WidgetListAdapter.WidgetAction.REMOVE : WidgetListAdapter.WidgetAction.ADD));
        }
    }
}