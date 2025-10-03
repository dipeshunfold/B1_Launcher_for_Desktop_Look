package com.bluelight.computer.winlauncher.prolauncher.explorewallpaper; // Adjust package name

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class ApiResponse {
    @SerializedName("test_new_arrival")
    private List<String> testNewArrival; // Changed from int to List<String>

    @SerializedName("test_categories")
    private int testCategories; // Assuming this is still an int based on the full API context, though not shown in the provided JSON snippet.

    @SerializedName("image_categories")
    private Map<String, Integer> imageCategories;

    // Getters
    public List<String> getTestNewArrival() {
        return testNewArrival;
    }

    public int getTestCategories() {
        return testCategories;
    }

    public Map<String, Integer> getImageCategories() {
        return imageCategories;
    }
}