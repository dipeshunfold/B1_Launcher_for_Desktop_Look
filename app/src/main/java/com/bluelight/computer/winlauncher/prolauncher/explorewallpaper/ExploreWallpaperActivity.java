package com.bluelight.computer.winlauncher.prolauncher.explorewallpaper;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bluelight.computer.winlauncher.prolauncher.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExploreWallpaperActivity extends AppCompatActivity implements SliderAdapter.OnSliderItemClickListener, CategoryAdapter.OnCategoryClickListener {

    private static final String TAG = "ExploreWallpaperActivity";
    private static final int SLIDER_DELAY_MS = 3000;
    private final Handler sliderHandler = new Handler(Looper.getMainLooper());
    private ViewPager2 newArrivalsViewPager;
    private RecyclerView categoriesRecyclerView;
    private CategoryAdapter categoryAdapter;
    private List<Map.Entry<String, Integer>> categoryDataList;
    private ApiService apiService;
    private ApiResponse currentApiResponse;
    private Runnable sliderRunnable;
    private ImageView img_back;
    private LinearLayout toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore_wallpaper);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), Build.VERSION.SDK_INT < Build.VERSION_CODES.R);

        getWindow().setStatusBarColor(Color.TRANSPARENT);

        View decorView = getWindow().getDecorView();
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), decorView);

        if (windowInsetsController != null) {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars());
            windowInsetsController.setAppearanceLightStatusBars(true);
            windowInsetsController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }

        newArrivalsViewPager = findViewById(R.id.newArrivalsViewPager);
        categoriesRecyclerView = findViewById(R.id.categoriesRecyclerView);
        img_back = findViewById(R.id.img_back);
        toolbar = findViewById(R.id.toolbar);

        categoriesRecyclerView.setLayoutManager(new GridLayoutManager(this, 1));

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) toolbar.getLayoutParams();
            params.topMargin = statusBarHeight;
            toolbar.setLayoutParams(params);
            return insets;
        });

        img_back.setOnClickListener(v -> {
            Log.d(TAG, "Back button clicked");
            onBackPressed();
        });

        categoryDataList = new ArrayList<>();
        categoryAdapter = new CategoryAdapter(this, categoryDataList, this);
        categoriesRecyclerView.setAdapter(categoryAdapter);

        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        fetchWallpaperData();

        sliderRunnable = new Runnable() {
            @Override
            public void run() {
                if (newArrivalsViewPager.getAdapter() != null) {
                    int currentItem = newArrivalsViewPager.getCurrentItem();
                    int itemCount = newArrivalsViewPager.getAdapter().getItemCount();
                    SliderAdapter adapter = (SliderAdapter) newArrivalsViewPager.getAdapter();
                    int originalItemCount = adapter.getOriginalItemCount();

                    if (itemCount > 0) {
                        int nextItem = currentItem + 1;
                        Log.d(TAG, "Auto-scrolling to item: " + nextItem + ", total items: " + itemCount);
                        newArrivalsViewPager.setCurrentItem(nextItem, true);

                        if (nextItem >= itemCount - originalItemCount || nextItem <= originalItemCount) {
                            Log.d(TAG, "Resetting to middle position: " + (itemCount / 2));
                            newArrivalsViewPager.setCurrentItem(itemCount / 2, false);
                        }
                    }
                }
                sliderHandler.postDelayed(this, SLIDER_DELAY_MS);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Starting auto-scroll");
        startSliderAutoScroll();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Stopping auto-scroll");
        stopSliderAutoScroll();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Cleaning up Handler");
        stopSliderAutoScroll();
        sliderHandler.removeCallbacksAndMessages(null); // Ensure all callbacks are removed
    }

    private void fetchWallpaperData() {
        Log.d(TAG, "Fetching wallpaper data");
        apiService.getWallpaperInfo().enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentApiResponse = response.body();
                    Log.d(TAG, "API response received successfully");
                    populateNewArrivals(currentApiResponse.getTestNewArrival());
                    populateCategories(currentApiResponse.getImageCategories());
                    startSliderAutoScroll();
                } else {
                    Toast.makeText(ExploreWallpaperActivity.this, "Failed to get API response", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Response not successful: " + response.code() + " " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Toast.makeText(ExploreWallpaperActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "API call failed", t);
            }
        });
    }

    private void populateNewArrivals(List<String> newArrivalCategories) {
        List<String> newArrivalImageUrls = new ArrayList<>();
        if (newArrivalCategories == null || newArrivalCategories.isEmpty()) {
            Log.w(TAG, "No new arrivals to display in slider");
            newArrivalsViewPager.setVisibility(View.GONE);
            return;
        }

        for (String categoryName : newArrivalCategories) {
            String url = ApiService.BASE_URL + "test_new_arrival/" + categoryName + ".png";
            newArrivalImageUrls.add(url);
            Log.d(TAG, "New Arrival Slider URL: " + url);
        }
        SliderAdapter sliderAdapter = new SliderAdapter(this, newArrivalImageUrls, this);
        newArrivalsViewPager.setAdapter(sliderAdapter);
        newArrivalsViewPager.setCurrentItem(sliderAdapter.getItemCount() / 2, false);
        newArrivalsViewPager.setVisibility(View.VISIBLE);
        Log.d(TAG, "Slider initialized with " + newArrivalImageUrls.size() + " items, starting at position: " + (sliderAdapter.getItemCount() / 2));
    }

    private void populateCategories(Map<String, Integer> categories) {
        categoryDataList.clear();
        if (categories != null) {
            categoryDataList.addAll(categories.entrySet());
        }
        categoryAdapter.notifyDataSetChanged();
        Log.d(TAG, "Populated categories. Count: " + categoryDataList.size());
    }

    private void startSliderAutoScroll() {
        if (sliderRunnable != null && newArrivalsViewPager.getAdapter() != null) {
            sliderHandler.removeCallbacks(sliderRunnable); // Clear any existing callbacks
            sliderHandler.postDelayed(sliderRunnable, SLIDER_DELAY_MS);
            Log.d(TAG, "Auto-scroll started");
        } else {
            Log.w(TAG, "Cannot start auto-scroll: Runnable or adapter is null");
        }
    }

    private void stopSliderAutoScroll() {
        if (sliderRunnable != null) {
            sliderHandler.removeCallbacks(sliderRunnable);
            Log.d(TAG, "Auto-scroll stopped");
        }
    }

    @Override
    public void onSliderItemClick(String imageUrl) {
        if (currentApiResponse != null && currentApiResponse.getImageCategories() != null) {
            String categoryName = null;
            int lastSlashIndex = imageUrl.lastIndexOf('/');
            int dotPngIndex = imageUrl.lastIndexOf(".png");

            if (lastSlashIndex != -1 && dotPngIndex != -1 && dotPngIndex > lastSlashIndex) {
                categoryName = imageUrl.substring(lastSlashIndex + 1, dotPngIndex);
            }

            if (categoryName != null && currentApiResponse.getImageCategories().containsKey(categoryName)) {
                int imageCount = currentApiResponse.getImageCategories().get(categoryName);
                Log.d(TAG, "Slider item clicked: Category = " + categoryName + ", ImageCount = " + imageCount);
                onCategoryClick(categoryName, imageCount);
            } else {
                Toast.makeText(this, "Could not find category data for clicked slider item.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Category name not found or invalid for URL: " + imageUrl);
            }
        } else {
            Toast.makeText(this, "API data not yet loaded, please try again.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "API data not loaded for slider click");
        }
    }

    @Override
    public void onCategoryClick(String categoryName, int imageCount) {
        Log.d(TAG, "Category clicked: " + categoryName + ", ImageCount: " + imageCount);
        Intent intent = new Intent(ExploreWallpaperActivity.this, CategoryActivity.class);
        intent.putExtra("categoryName", categoryName);
        intent.putExtra("imageCount", imageCount);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed called");
        super.onBackPressed();
    }
}