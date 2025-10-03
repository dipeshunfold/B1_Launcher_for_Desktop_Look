package com.bluelight.computer.winlauncher.prolauncher.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ColumnFirstGridLayoutManager extends GridLayoutManager {
    private static final String TAG = "ColumnFirstGridLayoutMgr";
    private int[] itemSpans;
    private int totalSpace = 0;
    private int spanCount = 4; // Default span count
    private SparseArray<View> viewCache = new SparseArray<>();

    public ColumnFirstGridLayoutManager(Context context, int spanCount) {
        super(context, spanCount);
        this.spanCount = spanCount;
        setSpanCount(spanCount);
    }

    public ColumnFirstGridLayoutManager(Context context, int spanCount, int orientation, boolean reverseLayout) {
        super(context, spanCount, orientation, reverseLayout);
        this.spanCount = spanCount;
        setSpanCount(spanCount);
    }

    @Override
    public void setSpanCount(int spanCount) {
        super.setSpanCount(spanCount);
        this.spanCount = spanCount;
        itemSpans = null;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        try {
            if (getItemCount() == 0) {
                removeAndRecycleAllViews(recycler);
                return;
            }

            // Calculate available space
            int width = getWidth() - getPaddingLeft() - getPaddingRight();
            int height = getHeight() - getPaddingTop() - getPaddingBottom();
            
            // Calculate item dimensions
            int itemWidth = width / spanCount;
            
            // Track the current position in each column
            int[] columnTops = new int[spanCount];
            
            // First, detach and cache all views
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                int position = getPosition(child);
                viewCache.put(position, child);
            }
            detachAndScrapAttachedViews(recycler);
            
            // Layout all items in column-first order
            for (int i = 0; i < getItemCount(); i++) {
                // Find the column with minimum height
                int minColumn = 0;
                for (int col = 1; col < spanCount; col++) {
                    if (columnTops[col] < columnTops[minColumn]) {
                        minColumn = col;
                    }
                }
                
                // Get or create the view
                View view = viewCache.get(i);
                if (view == null) {
                    view = recycler.getViewForPosition(i);
                    addView(view);
                } else {
                    attachView(view);
                    viewCache.remove(i);
                }
                
                // Measure the view
                measureChildWithMargins(view, 0, 0);
                
                // Calculate position
                int left = getPaddingLeft() + (minColumn * itemWidth);
                int top = getPaddingTop() + columnTops[minColumn];
                int right = left + itemWidth;
                int bottom = top + view.getMeasuredHeight();
                
                // Update column height
                columnTops[minColumn] = bottom;
                
                // Layout the view
                layoutDecorated(view, left, top, right, bottom);
            }
            
            // Recycle any views that are no longer needed
            for (int i = 0; i < viewCache.size(); i++) {
                recycler.recycleView(viewCache.valueAt(i));
            }
            viewCache.clear();
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onLayoutChildren: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean supportsPredictiveItemAnimations() {
        return false;
    }
    
    @Override
    public void onLayoutCompleted(RecyclerView.State state) {
        super.onLayoutCompleted(state);
        viewCache.clear();
    }
}