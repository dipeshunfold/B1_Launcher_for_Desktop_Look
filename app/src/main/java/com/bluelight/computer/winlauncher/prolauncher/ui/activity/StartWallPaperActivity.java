package com.bluelight.computer.winlauncher.prolauncher.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.model.SettingsViewModel;
import com.bluelight.computer.winlauncher.prolauncher.model.prefrance.WallpaperPreferenceHelper;
import com.bluelight.computer.winlauncher.prolauncher.ui.adapter.WallpaperThumbnailAdapter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.List;

public class StartWallPaperActivity extends AppCompatActivity {

    private ImageView imgWallpaperPreview;
    private WallpaperThumbnailAdapter adapter;
    private List<Integer> wallpaperList;
    private SettingsViewModel settingsViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_start_wall_paper);
        View mainLayout = findViewById(R.id.main);

        ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return windowInsets;
        });

        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        imgWallpaperPreview = findViewById(R.id.img_wallpaper_preview);
        RecyclerView recyclerView = findViewById(R.id.wallpaper_recycler_view);
        TextView btnSkip = findViewById(R.id.btnSkip);

        recyclerView.setHasFixedSize(true);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        loadWallpaperData();

        adapter = new WallpaperThumbnailAdapter(wallpaperList, wallpaperResId -> {
            Glide.with(this)
                    .load(wallpaperResId)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)  // Add this for consistency/caching, like in thumbnails
                    .centerCrop()
                    .into(imgWallpaperPreview);
        });
        recyclerView.setAdapter(adapter);

        Glide.with(this)
                .load(wallpaperList.get(0))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imgWallpaperPreview);

        btnSkip.setOnClickListener(v -> saveAndProceed());
    }

    private void loadWallpaperData() {
        wallpaperList = new ArrayList<>();
        wallpaperList.add(R.drawable.wallpaper_0);
        wallpaperList.add(R.drawable.wallpaper_1);
        wallpaperList.add(R.drawable.wallpaper_2);
        wallpaperList.add(R.drawable.wallpaper_3);
        wallpaperList.add(R.drawable.wallpaper_4);
        wallpaperList.add(R.drawable.wallpaper_5);
        wallpaperList.add(R.drawable.wallpaper_6);
        wallpaperList.add(R.drawable.wallpaper_7);
        wallpaperList.add(R.drawable.wallpaper_8);
        wallpaperList.add(R.drawable.wallpaper_9);
    }

    private void saveAndProceed() {
        int selectedWallpaperId = adapter.getSelectedWallpaperId();

        WallpaperPreferenceHelper.setWallpaper(this, selectedWallpaperId);
        settingsViewModel.setAppWallpaper(selectedWallpaperId);

        Intent intent = new Intent(this, HelloActivity.class);
        startActivity(intent);

        finish();
    }


    @Override
    public void onBackPressed() {

    }
}