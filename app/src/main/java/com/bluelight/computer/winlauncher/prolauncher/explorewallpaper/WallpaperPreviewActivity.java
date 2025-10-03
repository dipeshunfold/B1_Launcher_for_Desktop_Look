package com.bluelight.computer.winlauncher.prolauncher.explorewallpaper;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.ui.fragment.SetWallpaperDialogFragment;

import java.util.List;

public class WallpaperPreviewActivity extends AppCompatActivity {

    private ViewPager2 wallpaperViewPager;
    private ImageView setWallpaperButton;
    private ImageView imgBack;
    private List<String> wallpaperUrls;
    private int initialPosition;
    private LinearLayout toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set content view before any window modifications
        setContentView(R.layout.activity_wallpaper_preview);

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

        wallpaperViewPager = findViewById(R.id.wallpaperViewPager);
        setWallpaperButton = findViewById(R.id.setWallpaperButton);
        imgBack = findViewById(R.id.img_back);
        toolbar = findViewById(R.id.toolbar); // Make sure this ID exists in your layout

        // Apply window insets to handle status bar properly
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;

            // Update toolbar layout params to account for status bar
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) toolbar.getLayoutParams();
            params.topMargin = statusBarHeight;
            toolbar.setLayoutParams(params);

            return insets;
        });

        imgBack.setOnClickListener(v -> onBackPressed());

        // Get wallpapers from intent
        wallpaperUrls = getIntent().getStringArrayListExtra("wallpaperUrls");
        String selectedUrl = getIntent().getStringExtra("wallpaperUrl");
        initialPosition = wallpaperUrls != null ? wallpaperUrls.indexOf(selectedUrl) : 0;

        if (wallpaperUrls == null || wallpaperUrls.isEmpty()) {
            Toast.makeText(this, "No wallpapers available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set up ViewPager adapter
        WallpaperPagerAdapter pagerAdapter = new WallpaperPagerAdapter(wallpaperUrls);
        wallpaperViewPager.setAdapter(pagerAdapter);
        wallpaperViewPager.setCurrentItem(initialPosition, false);

        // Set wallpaper on button click to open the dialog
        setWallpaperButton.setOnClickListener(v -> openSetWallpaperDialog());
    }

    private void openSetWallpaperDialog() {
        int currentPosition = wallpaperViewPager.getCurrentItem();
        if (currentPosition >= 0 && currentPosition < wallpaperUrls.size()) {
            String currentUrl = wallpaperUrls.get(currentPosition);
            // Open SetWallpaperDialogFragment with the selected URL
            SetWallpaperDialogFragment.newInstance(currentUrl)
                    .show(getSupportFragmentManager(), SetWallpaperDialogFragment.TAG);
        } else {
            Toast.makeText(this, "Please select a wallpaper first.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Ensure status bar remains visible
            WindowInsetsControllerCompat windowInsetsController =
                    ViewCompat.getWindowInsetsController(getWindow().getDecorView());
            if (windowInsetsController != null) {
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars());
            }
        }
    }
}