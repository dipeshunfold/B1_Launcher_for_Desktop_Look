package com.bluelight.computer.winlauncher.prolauncher.explorewallpaper; // Adjust package name


import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
    String BASE_URL = "https://storage.googleapis.com/launcher_for_desktop_look/";

    @GET("test_bginfo.json")
    Call<ApiResponse> getWallpaperInfo();
}