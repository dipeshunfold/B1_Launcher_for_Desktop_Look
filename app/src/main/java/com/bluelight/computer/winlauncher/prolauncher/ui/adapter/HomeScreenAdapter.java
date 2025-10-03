//package com.bluelight.computer.winlauncher.prolauncher.ui.adapter;
//
//import androidx.annotation.NonNull;
//import androidx.fragment.app.Fragment;
//import androidx.fragment.app.FragmentActivity;
//import androidx.viewpager2.adapter.FragmentStateAdapter;
//
//import com.bluelight.computer.winlauncher.prolauncher.ui.fragment.HomeScreenFragment;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//
//public class HomeScreenAdapter extends FragmentStateAdapter {
//
//    private final List<Integer> pageIds = new ArrayList<>();
//
//    public HomeScreenAdapter(@NonNull FragmentActivity fragmentActivity) {
//        super(fragmentActivity);
//    }
//
//    @NonNull
//    @Override
//    public Fragment createFragment(int position) {
//        int pageId = pageIds.get(position);
//        return HomeScreenFragment.newInstance(pageId);
//    }
//
//    @Override
//    public int getItemCount() {
//        return pageIds.size();
//    }
//
//    @Override
//    public long getItemId(int position) {
//        return pageIds.get(position);
//    }
//
//    @Override
//    public boolean containsItem(long itemId) {
//        return pageIds.contains((int) itemId);
//    }
//
//
//    public void addPage(int newPageId) {
//
//        if (!pageIds.contains(newPageId)) {
//            pageIds.add(newPageId);
//
//            Collections.sort(pageIds);
//
//            notifyDataSetChanged();
//        }
//    }
//
//    public void removePage(int pageAdapterPosition) {
//        if (pageIds.size() > 1 && pageAdapterPosition >= 0 && pageAdapterPosition < pageIds.size()) {
//            pageIds.remove(pageAdapterPosition);
//            notifyItemRemoved(pageAdapterPosition);
//
//            notifyItemRangeChanged(pageAdapterPosition, pageIds.size());
//        }
//    }
//
//    public void setPageIds(List<Integer> ids) {
//        pageIds.clear();
//        pageIds.addAll(ids);
//        notifyDataSetChanged();
//    }
//
//
//    public int getPagePosition(int pageId) {
//        return pageIds.indexOf(pageId);
//    }
//
//    public int getPageId(int position) {
//        if (position >= 0 && position < pageIds.size()) {
//            return pageIds.get(position);
//        }
//        return -1;
//    }
//}


package com.bluelight.computer.winlauncher.prolauncher.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.bluelight.computer.winlauncher.prolauncher.ui.fragment.HomeScreenFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeScreenAdapter extends FragmentStateAdapter {

    private final List<Integer> pageIds = new ArrayList<>();

    public HomeScreenAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        // Initialize with at least one page
        if (pageIds.isEmpty()) {
            pageIds.add(0);
        }
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position < 0 || position >= pageIds.size()) {
            return HomeScreenFragment.newInstance(0);
        }
        int pageId = pageIds.get(position);
        return HomeScreenFragment.newInstance(pageId);
    }

    @Override
    public int getItemCount() {
        return pageIds.size();
    }

    @Override
    public long getItemId(int position) {
        return pageIds.get(position);
    }

    @Override
    public boolean containsItem(long itemId) {
        return pageIds.contains((int) itemId);
    }

    public void addPage(int newPageId) {
        if (!pageIds.contains(newPageId)) {
            pageIds.add(newPageId);
            // Sort to maintain order
            Collections.sort(pageIds);
            // Use notifyItemInserted for better performance
            int newPosition = pageIds.indexOf(newPageId);
            notifyItemInserted(newPosition);
            // Ensure ViewPager2 fully refreshes fragments when pages change
            notifyDataSetChanged();
        }
    }

    public void removePage(int pageAdapterPosition) {
        if (pageIds.size() > 1 && pageAdapterPosition >= 0 && pageAdapterPosition < pageIds.size()) {
            int removedPageId = pageIds.get(pageAdapterPosition);
            pageIds.remove(pageAdapterPosition);
            notifyItemRemoved(pageAdapterPosition);
            // Notify about the range change for remaining items
            notifyItemRangeChanged(pageAdapterPosition, pageIds.size() - pageAdapterPosition);
            // Force full refresh to avoid stale fragments after removal
            notifyDataSetChanged();
        }
    }

    public int getPagePosition(int pageId) {
        return pageIds.indexOf(pageId);
    }

    public int getPageId(int position) {
        if (position >= 0 && position < pageIds.size()) {
            return pageIds.get(position);
        }
        return -1;
    }

    public List<Integer> getPageIds() {
        return new ArrayList<>(pageIds);
    }

    public void setPageIds(List<Integer> ids) {
        pageIds.clear();
        pageIds.addAll(ids);
        Collections.sort(pageIds);
        notifyDataSetChanged();
    }

    public boolean hasPage(int pageId) {
        return pageIds.contains(pageId);
    }
}