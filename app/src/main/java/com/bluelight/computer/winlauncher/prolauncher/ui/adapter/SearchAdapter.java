package com.bluelight.computer.winlauncher.prolauncher.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {

    private final OnSuggestionClickListener listener;
    private final List<String> suggestions = new ArrayList<>();

    public SearchAdapter(List<String> initialSuggestions, OnSuggestionClickListener listener) {
        this.suggestions.addAll(initialSuggestions);
        this.listener = listener;
    }

    public void updateList(List<String> newSuggestions) {
        final SuggestionDiffCallback diffCallback = new SuggestionDiffCallback(this.suggestions, newSuggestions);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.suggestions.clear();
        this.suggestions.addAll(newSuggestions);

        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_suggestion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String suggestion = suggestions.get(position);
        holder.text.setText(suggestion);
        holder.itemView.setOnClickListener(v -> listener.onSuggestionClicked(suggestion));
    }

    @Override
    public int getItemCount() {
        return suggestions != null ? suggestions.size() : 0;
    }

    public interface OnSuggestionClickListener {
        void onSuggestionClicked(String suggestion);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView text;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.tv_suggestion);
        }
    }

    private static class SuggestionDiffCallback extends DiffUtil.Callback {

        private final List<String> oldList;
        private final List<String> newList;

        SuggestionDiffCallback(List<String> oldList, List<String> newList) {
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
            return Objects.equals(oldList.get(oldItemPosition), newList.get(newItemPosition));
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return Objects.equals(oldList.get(oldItemPosition), newList.get(newItemPosition));
        }
    }
}