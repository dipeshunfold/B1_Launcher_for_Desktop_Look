package com.bluelight.computer.winlauncher.prolauncher.apicalling;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface GoogleSuggestApi {
    @GET("/complete/search?client=firefox")
    Call<String> getSuggestions(@Query("q") String query);
}
