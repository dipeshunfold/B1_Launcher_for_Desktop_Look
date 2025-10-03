package com.bluelight.computer.winlauncher.prolauncher.explorewallpaper;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;

import java.util.ArrayList;
import java.util.List;

public class CategoryActivity extends AppCompatActivity implements CategoryWallpaperAdapter.OnWallpaperClickListener {

    private final int itemsPerPage = 12;
    ImageView img_back;
    LinearLayout header;
    private RecyclerView wallpapersRecyclerView;
    private ProgressBar loadingProgressBar;
    private TextView categoryTitleTextView;
    private CategoryWallpaperAdapter adapter;
    private List<String> wallpaperUrls;
    // Pagination variables
    private String currentCategoryName;
    private int totalImageCount;
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean allItemsLoaded = false;
    private final boolean loadInAscendingOrder = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        // Handle status bar for different Android versions
        WindowCompat.setDecorFitsSystemWindows(getWindow(), Build.VERSION.SDK_INT < Build.VERSION_CODES.R);

        // Make status bar translucent
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        // Ensure content doesn't overlap with status bar
        View decorView = getWindow().getDecorView();
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), decorView);

        if (windowInsetsController != null) {
            // Show system bars
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars());

            // Set light status bar icons for better visibility
            windowInsetsController.setAppearanceLightStatusBars(true);

            // Set behavior to show transient bars when swiping
            windowInsetsController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }

        wallpapersRecyclerView = findViewById(R.id.wallpapersRecyclerView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        categoryTitleTextView = findViewById(R.id.categoryTitleTextView);
        img_back = findViewById(R.id.img_back);
        header = findViewById(R.id.header);

        // Apply window insets to handle status bar properly
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;

            // Update header layout params to account for status bar
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) header.getLayoutParams();
            params.topMargin = statusBarHeight;
            header.setLayoutParams(params);

            return insets;
        });

        img_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        currentCategoryName = getIntent().getStringExtra("categoryName");
        totalImageCount = getIntent().getIntExtra("imageCount", 0);

        String toastMessage = "Category: " + currentCategoryName + ", Total Images: " + totalImageCount;
        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
        Log.d("CategoryActivity", toastMessage);

        if (currentCategoryName == null || totalImageCount <= 0) {
            Toast.makeText(this, "Invalid category data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String displayTitle = currentCategoryName.replace("test_", "").replace("_", " ");
        displayTitle = displayTitle.substring(0, 1).toUpperCase() + displayTitle.substring(1);
        categoryTitleTextView.setText(displayTitle);

        wallpaperUrls = new ArrayList<>();
        setupRecyclerView();
        loadWallpapers();
    }

    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        wallpapersRecyclerView.setLayoutManager(layoutManager);

        adapter = new CategoryWallpaperAdapter(this, wallpaperUrls, this);
        wallpapersRecyclerView.setAdapter(adapter);

        wallpapersRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy <= 0) return;

                GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();

                // Load more when we've scrolled to the last item
                if (!isLoading && !allItemsLoaded && lastVisibleItemPosition >= totalItemCount - 4) {
                    loadWallpapers();
                }
            }
        });
    }

    private void loadWallpapers() {
        if (allItemsLoaded || isLoading) {
            return;
        }

        isLoading = true;
        showLoading(true);

        // Calculate how many items to load
        int itemsToLoad = Math.min(itemsPerPage, totalImageCount - wallpaperUrls.size());

        if (itemsToLoad <= 0) {
            allItemsLoaded = true;
            isLoading = false;
            showLoading(false);
            return;
        }

        List<String> newUrls = new ArrayList<>();

        if (loadInAscendingOrder) {
            // Load in ascending order (1, 2, 3, ...)
            int startIndex = currentPage * itemsPerPage + 1;
            int endIndex = Math.min(startIndex + itemsToLoad - 1, totalImageCount);

            for (int i = startIndex; i <= endIndex; i++) {
                String url = ApiService.BASE_URL + currentCategoryName + "/" + i + ".png";
                newUrls.add(url);
            }
        } else {
            // Load in descending order (50, 49, 48, ...)
            int startIndex = totalImageCount - (currentPage * itemsPerPage);
            int endIndex = Math.max(startIndex - itemsToLoad + 1, 1);

            for (int i = startIndex; i >= endIndex; i--) {
                String url = ApiService.BASE_URL + currentCategoryName + "/" + i + ".png";
                newUrls.add(url);
            }
        }

        // Simulate network delay for testing
        wallpapersRecyclerView.postDelayed(() -> {
            int startPosition = wallpaperUrls.size();
            wallpaperUrls.addAll(newUrls);
            adapter.notifyItemRangeInserted(startPosition, newUrls.size());

            currentPage++;

            // Check if all items are loaded
            if (wallpaperUrls.size() >= totalImageCount) {
                allItemsLoaded = true;
                Log.d("Pagination", "All items loaded. Total: " + wallpaperUrls.size());
            }

            isLoading = false;
            showLoading(false);

            Log.d("Pagination", "Loaded " + newUrls.size() + " items. Total: " + wallpaperUrls.size() +
                    ", Page: " + currentPage + ", All loaded: " + allItemsLoaded);
        }, 1000);
    }

    private void showLoading(boolean isVisible) {
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onWallpaperClick(String imageUrl) {
        Intent intent = new Intent(CategoryActivity.this, WallpaperPreviewActivity.class);
        intent.putExtra("wallpaperUrl", imageUrl);
        intent.putStringArrayListExtra("wallpaperUrls", new ArrayList<>(wallpaperUrls));
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}