package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.model.SettingsMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SettingsMenuAdapter extends RecyclerView.Adapter<SettingsMenuAdapter.ViewHolder> implements Filterable {

    private final Context context;
    private final List<SettingsMenu> menuList;
    private final OnMenuItemClickListener listener;
    private List<SettingsMenu> menuListFiltered;
    private int selectedPosition = 0;

    public SettingsMenuAdapter(Context context, List<SettingsMenu> menuList, OnMenuItemClickListener listener) {
        this.context = context;
        this.menuList = menuList;
        this.menuListFiltered = new ArrayList<>(menuList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_settings_menu, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SettingsMenu item = menuListFiltered.get(position);
        holder.bind(item, position);
    }

    @Override
    public int getItemCount() {
        return menuListFiltered.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String charString = constraint.toString().toLowerCase(Locale.ROOT).trim();
                List<SettingsMenu> filteredList;
                if (charString.isEmpty()) {
                    filteredList = new ArrayList<>(menuList);
                } else {
                    filteredList = new ArrayList<>();
                    for (SettingsMenu row : menuList) {
                        if (row.getSearchableKeywords().contains(charString)) {
                            filteredList.add(row);
                        }
                    }
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = filteredList;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                menuListFiltered = (ArrayList<SettingsMenu>) results.values;
                selectedPosition = menuListFiltered.isEmpty() ? RecyclerView.NO_POSITION : 0;
                notifyDataSetChanged();
            }
        };
    }


    public SettingsMenu getItem(int position) {
        if (position >= 0 && position < menuListFiltered.size()) {
            return menuListFiltered.get(position);
        }
        return null;
    }


    public interface OnMenuItemClickListener {
        void onMenuItemClick(SettingsMenu item);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView menuIcon;
        TextView menuTitle;
        View selectionIndicator;
        LinearLayout menu_item_layout;

        ViewHolder(View itemView) {
            super(itemView);
            menuIcon = itemView.findViewById(R.id.menu_icon);
            menuTitle = itemView.findViewById(R.id.menu_title);
            selectionIndicator = itemView.findViewById(R.id.selection_indicator);
            menu_item_layout = itemView.findViewById(R.id.menu_item_layout);
        }

        void bind(final SettingsMenu item, final int position) {
            menuIcon.setImageResource(item.getIcon());
            menuTitle.setText(item.getTitle());
            if (selectedPosition == position) {
                selectionIndicator.setVisibility(View.VISIBLE);
                menuTitle.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
                menu_item_layout.setBackgroundColor(context.getResources().getColor(R.color.white));
            } else {
                selectionIndicator.setVisibility(View.INVISIBLE);
                menuTitle.setTextColor(ContextCompat.getColor(context, android.R.color.black));
                menu_item_layout.setBackgroundColor(context.getResources().getColor(R.color.light_gray));
            }

            itemView.setOnClickListener(v -> {
                if (getAdapterPosition() == RecyclerView.NO_POSITION) return;

                notifyItemChanged(selectedPosition);
                selectedPosition = getAdapterPosition();
                notifyItemChanged(selectedPosition);

                listener.onMenuItemClick(item);
            });
        }
    }
}