package com.bluelight.computer.winlauncher.prolauncher.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.model.AppInfo;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AllAppsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private static final int DEFAULT_ICON_SIZE = 100;
    private final Context context;
    private final List<AppInfo> fullAppsList;
    private final List<Object> itemsList = new ArrayList<>();
    private final AppInteractionListener listener;


    public AllAppsAdapter(Context context, List<AppInfo> appsList, AppInteractionListener listener) {
        this.context = context;
        this.listener = listener;

        Collections.sort(appsList, Comparator.comparing(app -> app.label.toString().toLowerCase()));
        this.fullAppsList = new ArrayList<>(appsList);

        setHasStableIds(true);

        updateList(processAppsWithHeaders(this.fullAppsList));
    }

    private List<Object> processAppsWithHeaders(List<AppInfo> apps) {
        List<Object> processedList = new ArrayList<>();
        char lastHeader = '\0';
        for (AppInfo app : apps) {
            if (app.label == null || app.label.length() == 0) continue;
            char firstChar = Character.toUpperCase(app.label.charAt(0));
            if (firstChar != lastHeader) {
                lastHeader = firstChar;
                processedList.add(String.valueOf(firstChar));
            }
            processedList.add(app);
        }
        return processedList;
    }

    @Override
    public int getItemViewType(int position) {
        return (itemsList.get(position) instanceof String) ? TYPE_HEADER : TYPE_ITEM;
    }

    @Override
    public long getItemId(int position) {
        Object item = itemsList.get(position);
        if (item instanceof AppInfo) {
            return ((AppInfo) item).packageName.hashCode();
        }
        return item.hashCode();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_app_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_app, parent, false);
            return new AppViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == TYPE_HEADER) {
            ((HeaderViewHolder) holder).headerTitle.setText((String) itemsList.get(position));
        } else {
            AppInfo app = (AppInfo) itemsList.get(position);
            AppViewHolder appHolder = (AppViewHolder) holder;

            appHolder.appName.setText(app.label);


            Glide.with(context)
                    .load(app.icon)
                    .override(DEFAULT_ICON_SIZE, DEFAULT_ICON_SIZE)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(appHolder.appIcon);
            appHolder.itemView.setOnClickListener(v -> {
                Intent launchIntent = context.getPackageManager()
                        .getLaunchIntentForPackage((String) app.packageName);
                if (launchIntent != null) {
                    context.startActivity(launchIntent);
                }
            });

            appHolder.itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onAppLongPressed(v, app);
                }
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return itemsList.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<AppInfo> filteredApps;

                if (constraint == null || constraint.length() == 0) {
                    filteredApps = new ArrayList<>(fullAppsList);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();
                    filteredApps = new ArrayList<>();
                    for (AppInfo app : fullAppsList) {
                        if (app.label != null &&
                                app.label.toString().toLowerCase().contains(filterPattern)) {
                            filteredApps.add(app);
                        }
                    }
                }

                FilterResults results = new FilterResults();
                results.values = processAppsWithHeaders(filteredApps);
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results.values instanceof List) {
                    List<Object> newItems = (List<Object>) results.values;
                    if (!itemsList.equals(newItems)) {
                        updateList(newItems);
                    }
                }
            }
        };
    }

    private void updateList(List<Object> newList) {
        DiffUtil.DiffResult diffResult =
                DiffUtil.calculateDiff(new AppDiffCallback(itemsList, newList), false);

        itemsList.clear();
        itemsList.addAll(newList);
        diffResult.dispatchUpdatesTo(this);
    }

    public interface AppInteractionListener {
        void onAppLongPressed(View anchorView, AppInfo appInfo);
    }

    public static class AppViewHolder extends RecyclerView.ViewHolder {
        TextView appName;
        ImageView appIcon;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            appName = itemView.findViewById(R.id.app_name);
            appIcon = itemView.findViewById(R.id.app_icon);
            appName.setSelected(true);
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView headerTitle;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            headerTitle = itemView.findViewById(R.id.tv_header);
        }
    }

    private static class AppDiffCallback extends DiffUtil.Callback {
        private final List<Object> oldList;
        private final List<Object> newList;

        AppDiffCallback(List<Object> oldList, List<Object> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            Object oldItem = oldList.get(oldItemPosition);
            Object newItem = newList.get(newItemPosition);

            if (oldItem instanceof AppInfo && newItem instanceof AppInfo) {
                return ((AppInfo) oldItem).packageName.equals(((AppInfo) newItem).packageName);
            } else if (oldItem instanceof String && newItem instanceof String) {
                return oldItem.equals(newItem);
            }
            return false;
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Object oldItem = oldList.get(oldItemPosition);
            Object newItem = newList.get(newItemPosition);

            if (oldItem instanceof AppInfo && newItem instanceof AppInfo) {
                return ((AppInfo) oldItem).label.equals(((AppInfo) newItem).label);
            } else if (oldItem instanceof String && newItem instanceof String) {
                return oldItem.equals(newItem);
            }
            return false;
        }
    }
}