package com.bluelight.computer.winlauncher.prolauncher.ui.adapter;

import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.model.GroupedWidgetItem;

import java.util.List;
import java.util.Set;

public class WidgetListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_WIDGET_GROUP = 1;
    private final Context context;
    private final List<GroupedWidgetItem> groupedWidgetData; // Now uses the new GroupedWidgetItem model
    private final Set<String> presentOnHomeProviders;
    private final OnWidgetActionListener listener;

    public WidgetListAdapter(Context context, List<GroupedWidgetItem> groupedWidgetData, Set<String> presentProviders, OnWidgetActionListener listener) {
        this.context = context;
        this.groupedWidgetData = groupedWidgetData;
        this.presentOnHomeProviders = presentProviders;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return groupedWidgetData.get(position).type == GroupedWidgetItem.ItemType.HEADER ? VIEW_TYPE_HEADER : VIEW_TYPE_WIDGET_GROUP;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_widget_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_widget_group_horizontal, parent, false);
            return new WidgetGroupViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        GroupedWidgetItem item = groupedWidgetData.get(position);
        if (holder.getItemViewType() == VIEW_TYPE_HEADER) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            headerHolder.bind(item.headerTitle);
        } else {
            WidgetGroupViewHolder groupHolder = (WidgetGroupViewHolder) holder;
            groupHolder.bind(item.widgets, context, presentOnHomeProviders, listener);
        }
    }

    @Override
    public int getItemCount() {
        return groupedWidgetData.size();
    }

    public enum WidgetAction {ADD, REMOVE}

    public interface OnWidgetActionListener {
        void onWidgetAction(AppWidgetProviderInfo widgetInfo, WidgetAction action);
    }

    // ViewHolder for headers
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView headerTitle;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            headerTitle = itemView.findViewById(R.id.header_title);
        }

        void bind(String title) {
            headerTitle.setText(title);
        }
    }

    // ViewHolder for horizontal RecyclerView of widgets
    static class WidgetGroupViewHolder extends RecyclerView.ViewHolder {
        RecyclerView horizontalRecyclerView;

        WidgetGroupViewHolder(@NonNull View itemView) {
            super(itemView);
            horizontalRecyclerView = itemView.findViewById(R.id.horizontal_widget_recycler_view);
            // Setup LinearLayoutManager for horizontal scrolling
            horizontalRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
            // Optional: improve performance for nested RecyclerViews
            horizontalRecyclerView.setHasFixedSize(true);
            horizontalRecyclerView.setItemViewCacheSize(20);
            horizontalRecyclerView.setDrawingCacheEnabled(true);
            horizontalRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        }

        void bind(List<AppWidgetProviderInfo> widgets, Context context, Set<String> presentOnHomeProviders, OnWidgetActionListener listener) {
            HorizontalWidgetAdapter adapter = (HorizontalWidgetAdapter) horizontalRecyclerView.getAdapter();
            if (adapter == null) {
                adapter = new HorizontalWidgetAdapter(context, widgets, presentOnHomeProviders, listener);
                horizontalRecyclerView.setAdapter(adapter);
            } else {
                // If the list of widgets in a group can change, you'd need a method in HorizontalWidgetAdapter
                // to update its data and call adapter.notifyDataSetChanged().
                // For this scenario, assuming the list `widgets` itself might change (e.g., due to filtering),
                // or just refreshing add/remove icons.
                adapter.notifyDataSetChanged(); // Just notify to refresh state of add/remove icons
            }
        }
    }
}