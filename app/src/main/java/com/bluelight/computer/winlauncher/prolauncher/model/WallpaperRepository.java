// package com.bluelight.computer.winlauncher.prolauncher.model; // Adjust package name if needed

package com.bluelight.computer.winlauncher.prolauncher.model; // This package aligns with SettingsViewModel

import android.util.Log;

import com.bluelight.computer.winlauncher.prolauncher.explorewallpaper.ApiResponse;
import com.bluelight.computer.winlauncher.prolauncher.explorewallpaper.ApiService;
import com.bluelight.computer.winlauncher.prolauncher.explorewallpaper.RetrofitClient;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import retrofit2.Response;

public class WallpaperRepository {

    private static final String TAG = "WallpaperRepository";
    private final ApiService apiService;

    public WallpaperRepository() {
        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
    }

    /**
     * Fetches the list of new arrival wallpaper URLs from the API.
     * This method performs a synchronous network request, so it should
     * be called on a background thread.
     *
     * @return A list of wallpaper URLs, or an empty list if the request fails.
     */
    public List<String> getNewArrivalWallpaperUrls() {
        try {
            Response<ApiResponse> response = apiService.getWallpaperInfo().execute();
            if (response.isSuccessful() && response.body() != null) {
                return response.body().getTestNewArrival();
            } else {
                Log.e(TAG, "API call failed: " + response.code() + " " + response.message());
                return Collections.emptyList();
            }
        } catch (IOException e) {
            Log.e(TAG, "Network error fetching wallpapers", e);
            return Collections.emptyList();
        }
    }

    // You might add methods to fetch wallpapers by category if you expand your API
    /*
    public List<String> getCategoryWallpaperUrls(String categoryName) {
        // Implement logic to fetch based on categories if your API supports it.
        // For now, we only have 'test_new_arrival'.
        return Collections.emptyList();
    }
    */
}