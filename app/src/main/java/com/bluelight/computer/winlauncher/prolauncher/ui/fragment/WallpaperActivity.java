package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.explorewallpaper.ExploreWallpaperActivity;
import com.bluelight.computer.winlauncher.prolauncher.ui.adapter.WallpaperAdapter;

import java.util.ArrayList;
import java.util.List;

public class WallpaperActivity extends AppCompatActivity {


    private LinearLayout contentContainer;
    private ImageView btnClose, exploreMore;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallpaper);

        // System bars behavior
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


        contentContainer = findViewById(R.id.content_container);
        btnClose = findViewById(R.id.btnClose);
        exploreMore = findViewById(R.id.explore_more);
        recyclerView = findViewById(R.id.wallpaper_recycler_view);

        // Apply insets so content doesn’t overlap status/nav bars
        ViewCompat.setOnApplyWindowInsetsListener(contentContainer, (v, insets) -> {
            Insets sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), sysBars.top, v.getPaddingRight(), sysBars.bottom);
            recyclerView.setPadding(
                    recyclerView.getPaddingLeft(),
                    recyclerView.getPaddingTop(),
                    recyclerView.getPaddingRight(),
                    sysBars.bottom
            );
            return insets;
        });

        exploreMore.setOnClickListener(v -> {
            Intent intent = new Intent(WallpaperActivity.this, ExploreWallpaperActivity.class);
            startActivity(intent);
        });

        btnClose.setOnClickListener(v -> finish());

        contentContainer.setOnClickListener(v -> { /* consume */ });

        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setHasFixedSize(true);

        List<Integer> wallpaperList = getWallpaperList();
        WallpaperAdapter adapter = new WallpaperAdapter(wallpaperList, wallpaperResId -> {
            SetWallpaperDialogFragment.newInstance(wallpaperResId)
                    .show(getSupportFragmentManager(), SetWallpaperDialogFragment.TAG);
        });
        recyclerView.setAdapter(adapter);
    }

    private List<Integer> getWallpaperList() {
        List<Integer> wallpapers = new ArrayList<>();
        wallpapers.add(R.drawable.wallpaper_0);
        wallpapers.add(R.drawable.wallpaper_1);
        wallpapers.add(R.drawable.wallpaper_2);
        wallpapers.add(R.drawable.wallpaper_3);
        wallpapers.add(R.drawable.wallpaper_4);
        wallpapers.add(R.drawable.wallpaper_5);
        wallpapers.add(R.drawable.wallpaper_6);
        wallpapers.add(R.drawable.wallpaper_7);
        wallpapers.add(R.drawable.wallpaper_8);
        wallpapers.add(R.drawable.wallpaper_9);
        return wallpapers;
    }
}