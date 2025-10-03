package com.bluelight.computer.winlauncher.prolauncher.ui.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.database.AppDatabase;
import com.bluelight.computer.winlauncher.prolauncher.database.AppItem;
import com.bluelight.computer.winlauncher.prolauncher.database.AppItemDao;
import com.bluelight.computer.winlauncher.prolauncher.model.AppItemViewModel;
import com.bluelight.computer.winlauncher.prolauncher.model.SafeAppWidgetHost;
import com.bluelight.computer.winlauncher.prolauncher.model.SettingsViewModel;
import com.bluelight.computer.winlauncher.prolauncher.model.TaskbarViewModel;
import com.bluelight.computer.winlauncher.prolauncher.model.prefrance.PreferenceHelper;
import com.bluelight.computer.winlauncher.prolauncher.model.prefrance.WallpaperPreferenceHelper;
import com.bluelight.computer.winlauncher.prolauncher.ui.adapter.AppListAdapter;
import com.bluelight.computer.winlauncher.prolauncher.ui.adapter.HomeScreenAdapter;
import com.bluelight.computer.winlauncher.prolauncher.ui.adapter.TaskbarAdapter;
import com.bluelight.computer.winlauncher.prolauncher.ui.adapter.WidgetListAdapter;
import com.bluelight.computer.winlauncher.prolauncher.ui.fragment.AddAppFragment;
import com.bluelight.computer.winlauncher.prolauncher.ui.fragment.AdvanceFeaturesFragment;
import com.bluelight.computer.winlauncher.prolauncher.ui.fragment.AppListFragment;
import com.bluelight.computer.winlauncher.prolauncher.ui.fragment.AppsFragment;
import com.bluelight.computer.winlauncher.prolauncher.ui.fragment.CalendarFragment;
import com.bluelight.computer.winlauncher.prolauncher.ui.fragment.ColorPickerFragment;
import com.bluelight.computer.winlauncher.prolauncher.ui.fragment.ContactFragment;
import com.bluelight.computer.winlauncher.prolauncher.ui.fragment.GeneralFragment;
import com.bluelight.computer.winlauncher.prolauncher.ui.fragment.HomeScreenFragment;
import com.bluelight.computer.winlauncher.prolauncher.ui.fragment.LauncherMenuDialogFragment;
import com.bluelight.computer.winlauncher.prolauncher.ui.fragment.NotificationFragment;
import com.bluelight.computer.winlauncher.prolauncher.ui.fragment.RecentFragment;
import com.bluelight.computer.winlauncher.prolauncher.ui.fragment.SearchFragment;
import com.bluelight.computer.winlauncher.prolauncher.ui.fragment.SettingsFragment;
import com.bluelight.computer.winlauncher.prolauncher.ui.fragment.SystemFragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class LauncherActivity extends AppCompatActivity implements HomeScreenFragment.PageInteractionListener, ColorPickerFragment.OnColorSelectedListener, AddAppFragment.OnAppPickedListener, SettingsFragment.OnSettingsClosedListener {

    public static final String ACTION_ICON_SIZE_CHANGED = "com.protheme.launcher.ICON_SIZE_CHANGED";
    public static final String ACTION_HIDE_TASKBAR_ICONS_CHANGED = "com.protheme.launcher.winx2.HIDE_TASKBAR_ICONS_CHANGED";
    private static final String TAG = "LauncherActivity";

    private static final String PREFS_NAME = "LauncherPrefs";
    private static final String KEY_HIDE_ICONS = "key_hide_taskbar_icons";
    private static final String KEY_FIRST_RUN_PROMPT_SHOWN = "first_run_prompt_shown";

    private static final int APPWIDGET_HOST_ID = 1024;
    private static final int REQUEST_PICK_APP = 3;
    private static final int REQUEST_PICK_WIDGET = 4;
    private static final int REQUEST_CREATE_APPWIDGET = 5;
    private static final List<String> DEFAULT_APP_PACKAGES_ORDERED = Arrays.asList("com.whatsapp", "com.google.android.youtube", "com.google.android.googlequicksearchbox");
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    // Edge scroll tuned for smoother cross-page dragging
    private final float EDGE_SCROLL_THRESHOLD_RATIO = 0.12f; // slightly easier to trigger
    private final long PAGE_SCROLL_DELAY_MS = 220; // faster page scroll
    private LinearLayout lnrDateTime;
    private RelativeLayout lnrTaskBar;
    private ImageView btnHome, btnSearch, btnContact, btnSetting, btnRecent;
    private TextView txtTime, txtDate;
    private ViewPager2 viewPager;
    private RecyclerView taskbarRecyclerView;
    private View rootLayout;
    private ImageView imgWall;
    private DotsIndicator dotsIndicator;
    private HomeScreenAdapter pagerAdapter;
    private TaskbarAdapter taskbarAdapter;
    private TaskbarViewModel taskbarViewModel;
    private SettingsViewModel settingsViewModel;
    private final BroadcastReceiver wallpaperChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.protheme.launcher.WALLPAPER_CHANGED".equals(intent.getAction())) {
                String wallpaperUrl = intent.getStringExtra("wallpaper_url");
                int wallpaperResId = intent.getIntExtra("wallpaper_res_id", -1);

                if (wallpaperUrl != null && !wallpaperUrl.isEmpty()) {
                    WallpaperPreferenceHelper.setWallpaperUrl(context, wallpaperUrl);
                    settingsViewModel.setAppWallpaper(wallpaperUrl);
                } else if (wallpaperResId != -1) {
                    WallpaperPreferenceHelper.setWallpaper(context, wallpaperResId);
                    settingsViewModel.setAppWallpaper(wallpaperResId);
                } else {
                    WallpaperPreferenceHelper.setWallpaper(context, R.drawable.wallpaper_1);
                    settingsViewModel.setAppWallpaper(R.drawable.wallpaper_1);
                }
                updateAppBackgroundDisplay();
            }
        }
    };
    private SharedPreferences prefs;
    private final BroadcastReceiver timeUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateTaskbarTimeAndDate();
        }
    };
    private final BroadcastReceiver hideIconsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_HIDE_TASKBAR_ICONS_CHANGED.equals(intent.getAction())) {
                applyTaskbarVisibility();
            }
        }
    };
    private AppItemDao appItemDao;
    private ExecutorService databaseWriteExecutor;
    private AppWidgetManager appWidgetManager;
    private AppWidgetHost appWidgetHost;
    private UninstallReceiver uninstallReceiver;
    // Cross-page dragging fields - IMPROVED
    private FrameLayout mDragOverlay;
    private ImageView mDragView;
    private AppItem mDragItem;
    private int mOriginalPageIndex;
    private int mDragItemOriginalRow;
    private int mDragItemOriginalCol;
    private float mDragOffsetX, mDragOffsetY;
    private boolean isCrossPageDragging = false;
    private boolean mWasDragging = false;
    private final Handler mPageScrollHandler = new Handler(Looper.getMainLooper());
    private final Runnable mScrollLeftRunnable = new Runnable() {
        @Override
        public void run() {
            if (isCrossPageDragging && viewPager.getCurrentItem() > 0) {
                // Only scroll if we have actual pages to scroll to (prevent infinite page creation)
                int currentItem = viewPager.getCurrentItem();
                if (currentItem > 0) {
                    viewPager.setCurrentItem(currentItem - 1, true);
                }
            }
            // Don't repost - let ACTION_MOVE logic re-arm if still at edge
        }
    };
    private final Runnable mScrollRightRunnable = new Runnable() {
        @Override
        public void run() {
            if (isCrossPageDragging) {
                int currentItem = viewPager.getCurrentItem();
                int itemCount = pagerAdapter.getItemCount();
                // Only scroll if we have actual pages to scroll to (prevent infinite page creation)
                if (currentItem < itemCount - 1) {
                    viewPager.setCurrentItem(currentItem + 1, true);
                }
            }
            // Don't repost - let ACTION_MOVE logic re-arm if still at edge
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setThemeBasedOnOrientation(getResources().getConfiguration());

        setContentView(R.layout.activity_launcher);

        // Initialize grid size based on current orientation
        HomeScreenFragment.initializeGridSize(this);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        new WindowInsetsControllerCompat(getWindow(), findViewById(android.R.id.content))
                .setAppearanceLightStatusBars(false);

        // Configure status bar visibility based on INITIAL orientation
        configureStatusBarForOrientation(getResources().getConfiguration());

        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        taskbarViewModel = new ViewModelProvider(this).get(TaskbarViewModel.class);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        databaseWriteExecutor = Executors.newSingleThreadExecutor();
        AppDatabase db = AppDatabase.getDatabase(this);
        appItemDao = db.appItemDao();
        appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        appWidgetHost = new SafeAppWidgetHost(getApplicationContext(), APPWIDGET_HOST_ID);

        bindViews();
        setupSystemWindowInsets();
        setupClickListeners();
        setupObservers();
        pagerAdapter = new HomeScreenAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        dotsIndicator.attachTo(viewPager);

        loadPagesFromDb();
        setupTaskbar();
        setupUninstallReceiver();

        setInitialUiState();
        showDialogOnFirstRun();

        // Improved page change callback for smoother transitions
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (dotsIndicator != null) {
//                    dotsIndicator.setSelection(position);
                }
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                // Smooth page scrolling during drag
                if (isCrossPageDragging) {
                    mDragView.setAlpha(0.8f - (positionOffset * 0.3f));
                }
            }
        });
    }

    private void bindViews() {
        rootLayout = findViewById(R.id.main_activity_root);
        imgWall = findViewById(R.id.imgWall);
        viewPager = findViewById(R.id.view_pager);
        dotsIndicator = findViewById(R.id.dots_indicator);
        lnrTaskBar = findViewById(R.id.lnrTaskBar);
        lnrDateTime = findViewById(R.id.lnrDateTime);
        btnHome = findViewById(R.id.btnHome);
        btnSearch = findViewById(R.id.btnSearch);
        btnContact = findViewById(R.id.btnContact);
        btnSetting = findViewById(R.id.btnSetting);
        btnRecent = findViewById(R.id.btnRecent);
        txtTime = findViewById(R.id.txtTime);
        txtDate = findViewById(R.id.txtdate);
        taskbarRecyclerView = findViewById(R.id.taskbar_recycler_view);
        mDragOverlay = findViewById(R.id.drag_overlay);
    }

    private void setupSystemWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_activity_root), (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return windowInsets;
        });
    }

    private void setupClickListeners() {
        btnHome.setOnClickListener(v -> toggleLauncherFragment());
        btnSearch.setOnClickListener(v -> toggleSearchFragment());
        btnContact.setOnClickListener(v -> toggleContactFragment());
        lnrDateTime.setOnClickListener(v -> toggleDateTimeFragment());
        btnSetting.setOnClickListener(v -> toggleNotificationFragment());
        btnRecent.setOnClickListener(v -> toggleRecentFragment());
    }

    private void setupObservers() {
        settingsViewModel.isCortanaEnabled().observe(this, this::updateCortanaVisibility);
        settingsViewModel.isContactsEnabled().observe(this, this::updateContactsVisibility);
        settingsViewModel.isTimeEnabled().observe(this, this::updateTimeVisibility);
        settingsViewModel.isTaskbarTransparent().observe(this, this::updateTaskbarBackground);
        settingsViewModel.isRecentsButtonEnabled().observe(this, this::updateRecentsButtonVisibility);
        settingsViewModel.getSystemBarsMode().observe(this, this::updateSystemUiVisibility);
        settingsViewModel.getDateFormat().observe(this, this::applyDateFormat);

        settingsViewModel.getTaskbarBgColor().observe(this, color -> applyTaskbarColors());
        settingsViewModel.getTaskbarTextColor().observe(this, color -> applyTaskbarColors());
        settingsViewModel.shouldHideTaskbarIcons().observe(this, shouldHide -> {
            if (shouldHide != null) {
                taskbarRecyclerView.setVisibility(shouldHide ? View.GONE : View.VISIBLE);
            }
        });
        settingsViewModel.getIconSize().observe(this, size -> {
            if (pagerAdapter != null) {
                pagerAdapter.notifyDataSetChanged();
            }
        });

        taskbarViewModel.getCombinedTaskbarItems().observe(this, combinedItems -> {
            if (combinedItems != null) {
                taskbarAdapter.setTaskbarItems(combinedItems);
            }
        });

        settingsViewModel.getDesktopTextColor().observe(this, color -> {
            if (pagerAdapter != null) {
                pagerAdapter.notifyDataSetChanged();
            }
        });

        settingsViewModel.getAppWallpaperUrl().observe(this, url -> updateAppBackgroundDisplay());
        settingsViewModel.getAppWallpaperResId().observe(this, resId -> updateAppBackgroundDisplay());
    }

    public void updateAppBackgroundDisplay() {
        String wallpaperUrl = settingsViewModel.getAppWallpaperUrl().getValue();
        Integer wallpaperResId = settingsViewModel.getAppWallpaperResId().getValue();

        Log.d(TAG, "updateAppBackgroundDisplay triggered. URL: " + wallpaperUrl + ", ResId: " + wallpaperResId);

        imgWall.setImageDrawable(null);
        rootLayout.setBackground(null);

        if (wallpaperUrl != null && !wallpaperUrl.isEmpty()) {
            Log.d(TAG, "Loading wallpaper from URL: " + wallpaperUrl);
            Glide.with(this)
                    .load(wallpaperUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(imgWall);

            Glide.with(this)
                    .load(wallpaperUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            rootLayout.setBackground(resource);
                            Log.d(TAG, "rootLayout background set from URL: " + wallpaperUrl);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            rootLayout.setBackground(placeholder);
                            Log.d(TAG, "rootLayout background cleared for URL: " + wallpaperUrl);
                        }
                    });

        } else if (wallpaperResId != null && wallpaperResId != -1 && wallpaperResId != 0) {
            Log.d(TAG, "Loading wallpaper from Resource ID: " + wallpaperResId);
            imgWall.setImageResource(wallpaperResId);
            rootLayout.setBackgroundResource(wallpaperResId);
        } else {
            Log.d(TAG, "Loading default wallpaper (wallpaper_1)");
            imgWall.setImageResource(R.drawable.wallpaper_1);
            rootLayout.setBackgroundResource(R.drawable.wallpaper_1);
        }
    }

    private void setInitialUiState() {
        updateCortanaVisibility(prefs.getBoolean(AdvanceFeaturesFragment.KEY_CORTANA, true));
        updateContactsVisibility(prefs.getBoolean(AdvanceFeaturesFragment.KEY_SHOW_CONTACTS, true));
        updateTimeVisibility(prefs.getBoolean(AdvanceFeaturesFragment.KEY_SHOW_TIME, true));
        updateRecentsButtonVisibility(prefs.getBoolean(AppsFragment.KEY_SHOW_RECENT, true));
        applyTaskbarColors();
        applyDateFormat(prefs.getString(GeneralFragment.KEY_DATE_FORMAT, "dd/MM/yyyy"));
        updateTaskbarTimeAndDate();
        updateSystemUiVisibility(prefs.getInt(SystemFragment.KEY_SYSTEM_BARS_MODE, SystemFragment.MODE_SHOW_STATUS_ONLY));

        syncWallpaperStateFromPreferences();
    }

    private void syncWallpaperStateFromPreferences() {
        String currentUrl = WallpaperPreferenceHelper.getWallpaperUrl(this);
        int currentResId = WallpaperPreferenceHelper.getWallpaper(this);

        if (currentUrl != null && !currentUrl.isEmpty()) {
            settingsViewModel.setAppWallpaper(currentUrl);
            Log.d(TAG, "syncWallpaperState: Set ViewModel URL from Prefs: " + currentUrl);
        } else if (currentResId != -1) {
            settingsViewModel.setAppWallpaper(currentResId);
            Log.d(TAG, "syncWallpaperState: Set ViewModel ResId from Prefs: " + currentResId);
        } else {
            WallpaperPreferenceHelper.setWallpaper(this, R.drawable.wallpaper_1);
            settingsViewModel.setAppWallpaper(R.drawable.wallpaper_1);
            Log.d(TAG, "syncWallpaperState: Set ViewModel and Prefs to Default Wallpaper.");
        }
        updateAppBackgroundDisplay();
    }

    private void updateCortanaVisibility(boolean isEnabled) {
        btnSearch.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
    }

    private void updateContactsVisibility(boolean isEnabled) {
        btnContact.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
    }

    private void updateTimeVisibility(boolean isEnabled) {
        lnrDateTime.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
    }

    private void updateRecentsButtonVisibility(boolean isEnabled) {
        btnRecent.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
    }

    private void updateTaskbarBackground(boolean isTransparent) {
        applyTaskbarColors();
    }

    private void applyTaskbarColors() {
        if (lnrTaskBar == null) return;
        boolean isTransparent = prefs.getBoolean(AdvanceFeaturesFragment.KEY_TRANSPARENT_TASKBAR, false);
        int bgColor = prefs.getInt("taskbar_bg_color", ContextCompat.getColor(this, R.color.white));
        int textColor = prefs.getInt("taskbar_text_color", ContextCompat.getColor(this, R.color.black));

        int finalBgColor = isTransparent ? (50 << 24) | (bgColor & 0x00FFFFFF) : bgColor;

        lnrTaskBar.setBackgroundColor(finalBgColor);
        txtTime.setTextColor(textColor);
        txtDate.setTextColor(textColor);
    }

    private void updateTaskbarTimeAndDate() {
        txtTime.setText(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date()));
        applyDateFormat(prefs.getString(GeneralFragment.KEY_DATE_FORMAT, "dd/MM/yyyy"));
    }

    private void applyDateFormat(String format) {
        if (txtDate == null || format == null) return;
        try {
            txtDate.setText(new SimpleDateFormat(format, Locale.getDefault()).format(new Date()));
        } catch (Exception e) {
            txtDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
        }
    }

    private void updateSystemUiVisibility(int mode) {
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (windowInsetsController == null) {
            return;
        }

        switch (mode) {
            case SystemFragment.MODE_SHOW_ALL:
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars());
                break;
            case SystemFragment.MODE_SHOW_NAV_ONLY:
                windowInsetsController.show(WindowInsetsCompat.Type.navigationBars());
                windowInsetsController.hide(WindowInsetsCompat.Type.statusBars());
                windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                break;
            case SystemFragment.MODE_SHOW_STATUS_ONLY:
                windowInsetsController.show(WindowInsetsCompat.Type.statusBars());
                windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars());
                windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                break;
            case SystemFragment.MODE_HIDE_ALL:
            default:
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
                windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                break;
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onStart() {
        super.onStart();
        appWidgetHost.startListening();
        setupWallpaperChangeReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter timeFilter = new IntentFilter();
        timeFilter.addAction(Intent.ACTION_TIME_TICK);
        timeFilter.addAction(Intent.ACTION_TIME_CHANGED);
        timeFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(timeUpdateReceiver, timeFilter);

        IntentFilter hideIconsFilter = new IntentFilter(ACTION_HIDE_TASKBAR_ICONS_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(hideIconsReceiver, hideIconsFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(hideIconsReceiver, hideIconsFilter);
        }

        syncWallpaperStateFromPreferences();
        updateAppBackgroundDisplay();

        // Re-apply status bar configuration when returning to launcher (with error handling)
        try {
            configureStatusBarForOrientation(getResources().getConfiguration());
        } catch (Exception e) {
            Log.e(TAG, "Error configuring status bar in onResume", e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(timeUpdateReceiver);
        unregisterReceiver(hideIconsReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            appWidgetHost.stopListening();
        } catch (Exception ex) {
            Log.w(TAG, "AppWidgetHost listener could not be stopped.", ex);
        }
        try {
            unregisterReceiver(wallpaperChangeReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering wallpaperChangeReceiver", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (uninstallReceiver != null) {
            try {
                unregisterReceiver(uninstallReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering uninstallReceiver", e);
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Update grid size when orientation changes
        HomeScreenFragment.initializeGridSize(this);

        // Set theme first
        setThemeBasedOnOrientation(newConfig);

        // Post configuration to avoid timing issues during recreate
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                configureStatusBarForOrientation(newConfig);
            } catch (Exception e) {
                Log.e(TAG, "Error configuring status bar in onConfigurationChanged", e);
            }
        }, 100);

        // Recreate activity to apply theme changes
        recreate();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else if (!isMyAppLauncherDefault()) {
            super.onBackPressed();
        }
    }

    private boolean isMyAppLauncherDefault() {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        final ResolveInfo res = getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return res != null && res.activityInfo != null && getPackageName().equals(res.activityInfo.packageName);
    }

    /**
     * Configure status bar and navigation bar visibility based on orientation
     * Works on Android 7+ (API 24+) through Android 16
     */
    private void configureStatusBarForOrientation(Configuration config) {
        Window window = getWindow();
        View decorView = window.getDecorView();

        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // LANDSCAPE: Hide both status bar and navigation bar for fullscreen experience

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ (API 30+) - Use WindowInsetsController
                WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, decorView);
                if (controller != null) {
                    controller.hide(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
                    controller.setSystemBarsBehavior(
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    );
                }
            } else {
                // Android 7-10 (API 24-29) - Use system UI flags
                int flags = View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                decorView.setSystemUiVisibility(flags);
            }

        } else {
            // PORTRAIT: Show status bar, hide navigation bar

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ (API 30+)
                WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, decorView);
                if (controller != null) {
                    controller.show(WindowInsetsCompat.Type.statusBars());
                    controller.hide(WindowInsetsCompat.Type.navigationBars());
                    controller.setSystemBarsBehavior(
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    );
                    // Set light/dark status bar icons based on your theme
                    controller.setAppearanceLightStatusBars(false); // false = white icons
                }
            } else {
                // Android 7-10 (API 24-29)
                int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                decorView.setSystemUiVisibility(flags);
            }
        }
    }

    public void toggleFragment(Class<? extends Fragment> fragmentClass, int inAnim, int outAnim) {
        FragmentManager fm = getSupportFragmentManager();
        Fragment currentFragment = fm.findFragmentById(R.id.fragment_container);

        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        if (currentFragment == null || !fragmentClass.isInstance(currentFragment)) {
            try {
                Fragment newFragment = fragmentClass.getConstructor().newInstance();
                FragmentTransaction ft = fm.beginTransaction();
                ft.setCustomAnimations(inAnim, 0, 0, outAnim);

                ft.replace(R.id.fragment_container, newFragment);

                ft.addToBackStack(null);

                ft.commit();
            } catch (Exception e) {
                Log.e(TAG, "Failed to create fragment instance", e);
            }
        }
    }

    private void toggleLauncherFragment() {
        toggleFragment(AppListFragment.class, R.anim.slide_up, R.anim.slide_down);
    }

    private void toggleSearchFragment() {
        toggleFragment(SearchFragment.class, R.anim.slide_up, R.anim.slide_down);
    }

    private void toggleContactFragment() {
        toggleFragment(ContactFragment.class, R.anim.slide_up, R.anim.slide_down);
    }

    private void toggleDateTimeFragment() {
        toggleFragment(CalendarFragment.class, R.anim.slide_up, R.anim.slide_down);
    }

    private void toggleNotificationFragment() {
        toggleFragment(NotificationFragment.class, R.anim.slide_in_from_right, R.anim.slide_out_to_right);
    }

    private void toggleRecentFragment() {
        toggleFragment(RecentFragment.class, R.anim.slide_up, R.anim.slide_down);
    }

    @Override
    public void onColorSelected(int backgroundColor, int textColor) {
        prefs.edit()
                .putInt("taskbar_bg_color", backgroundColor)
                .putInt("taskbar_text_color", textColor)
                .apply();
        settingsViewModel.setTaskbarBgColor(backgroundColor);
        settingsViewModel.setTaskbarTextColor(textColor);
    }

    @Override
    public void onSetDefault() {
        int bgColor = ContextCompat.getColor(this, R.color.white);
        int textColor = ContextCompat.getColor(this, R.color.black);
        prefs.edit()
                .putInt("taskbar_bg_color", bgColor)
                .putInt("taskbar_text_color", textColor)
                .apply();
        settingsViewModel.setTaskbarBgColor(bgColor);
        settingsViewModel.setTaskbarTextColor(textColor);
    }

    @Override
    public void onSettingsClosed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        }
    }

    @Override
    public void onHomeScreenLongPressed() {
        showLauncherMenuDialog();
    }

    @Override
    public void onAppRemoved(AppItem appItem) {
        Log.d(TAG, "Item removed: " + appItem.getName());

        // Refresh the current page to update the UI
        if (viewPager != null && viewPager.getAdapter() != null) {
            int currentPage = viewPager.getCurrentItem();
            Fragment fragment = getSupportFragmentManager().findFragmentByTag("f" + currentPage);
            if (fragment instanceof HomeScreenFragment) {
                ((HomeScreenFragment) fragment).forceRefreshLayoutFromDb();
            }
        }
    }

    @Override
    public AppWidgetHost getAppWidgetHost() {
        return this.appWidgetHost;
    }

    @Override
    public void onPageEmpty(int pageId) {
        if (pagerAdapter.getItemCount() > 1) {
            // First delete any page markers or items from the database
            databaseWriteExecutor.execute(() -> {
                // Delete any items associated with this page (including hidden markers)
                appItemDao.deletePage(pageId);

                // Then update the UI on the main thread
                mainThreadHandler.post(() -> {
                    int adapterPosition = pagerAdapter.getPagePosition(pageId);
                    if (adapterPosition != -1) {
                        pagerAdapter.removePage(adapterPosition);
                        // Re-attach dots indicator so it reflects the new page count
                        if (dotsIndicator != null) {
                            dotsIndicator.attachTo(viewPager);
                        }
                        Toast.makeText(this, "Empty page removed", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallow) {
        viewPager.setUserInputEnabled(!disallow);
    }

    @Override
    public void onAppInfoRequested(String packageName) {
        if (packageName == null) return;
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", packageName, null);
            intent.setData(uri);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open app info", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error opening app info for " + packageName, e);
        }
    }

    @Override
    public void onUninstallRequested(String packageName) {
        if (packageName == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Uninstall App")
                .setMessage("Are you sure you want to uninstall this app?")
                .setPositiveButton("Uninstall", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + packageName));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onAppPicked(ResolveInfo appInfo, AppListAdapter.AppAction action) {
        if (appInfo == null || action == null) return;
        if (action == AppListAdapter.AppAction.ADD) {
            handleAddItem(appInfo, -1, null);
        } else {
            removeAppByPackageName(appInfo.activityInfo.packageName);
        }
        getSupportFragmentManager().popBackStack();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CREATE_APPWIDGET) {
            int widgetId = (data != null) ? data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) : -1;
            if (resultCode == RESULT_OK && widgetId != -1) {
                AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(widgetId);
                if (info != null) {
                    handleAddItem(null, widgetId, info);
                } else {
                    appWidgetHost.deleteAppWidgetId(widgetId);
                }
            } else if (widgetId != -1) {
                appWidgetHost.deleteAppWidgetId(widgetId);
            }
            return;
        }

        if (resultCode != RESULT_OK || data == null) return;

        if (requestCode == REQUEST_PICK_APP) {
            ResolveInfo pickedAppInfo = data.getParcelableExtra("picked_app_info");
            AppListAdapter.AppAction action = (AppListAdapter.AppAction) data.getSerializableExtra("action");
            if (pickedAppInfo != null && action != null) {
                if (action == AppListAdapter.AppAction.ADD) {
                    handleAddItem(pickedAppInfo, -1, null);
                } else {
                    removeAppByPackageName(pickedAppInfo.activityInfo.packageName);
                }
            }
        } else if (requestCode == REQUEST_PICK_WIDGET) {
            AppWidgetProviderInfo pickedWidgetInfo = data.getParcelableExtra("picked_widget_info");
            WidgetListAdapter.WidgetAction action = (WidgetListAdapter.WidgetAction) data.getSerializableExtra("action");
            if (pickedWidgetInfo != null && action != null) {
                if (action == WidgetListAdapter.WidgetAction.ADD) {
                    addNewWidgetToScreen(pickedWidgetInfo);
                } else {
                    removeWidgetByProvider(pickedWidgetInfo);
                }
            }
        }
    }

    private void setupTaskbar() {
        taskbarAdapter = new TaskbarAdapter(this);
        taskbarRecyclerView.setHasFixedSize(true);
        taskbarRecyclerView.setItemViewCacheSize(20);
        taskbarRecyclerView.setDrawingCacheEnabled(true);
        taskbarRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        taskbarRecyclerView.setAdapter(taskbarAdapter);
        taskbarRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        applyTaskbarVisibility();
    }

    public void applyTaskbarVisibility() {
        boolean iconsAreHidden = prefs.getBoolean(KEY_HIDE_ICONS, false);
        taskbarRecyclerView.setVisibility(iconsAreHidden ? View.INVISIBLE : View.VISIBLE);
    }

    private void setupWallpaperChangeReceiver() {
        IntentFilter filter = new IntentFilter("com.protheme.launcher.WALLPAPER_CHANGED");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wallpaperChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(wallpaperChangeReceiver, filter);
        }
    }

    private void setupUninstallReceiver() {
        uninstallReceiver = new UninstallReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(uninstallReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(uninstallReceiver, filter);
        }
    }

    public void loadPagesFromDb() {
        AppItemViewModel viewModel = new ViewModelProvider(this).get(AppItemViewModel.class);
        viewModel.getAllItems().observe(this, allItems -> {
            if (allItems == null) return;
            if (allItems.isEmpty()) {
                populateDefaultHomeScreen();
            } else {
                List<Integer> dbPageIndexes = allItems.stream()
                        .map(item -> item.page)
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());

                // Ensure page 0 always exists
                if (!dbPageIndexes.contains(0)) {
                    dbPageIndexes.add(0, 0);
                }

                // Merge with any pages currently known by the adapter (e.g., newly added empty pages)
                List<Integer> merged = new ArrayList<>(pagerAdapter.getPageIds());
                for (Integer id : dbPageIndexes) {
                    if (!merged.contains(id)) merged.add(id);
                }
                merged = merged.stream().distinct().sorted().collect(Collectors.toList());

                pagerAdapter.setPageIds(merged);
                if (viewPager.getCurrentItem() >= pagerAdapter.getItemCount()) {
                    viewPager.setCurrentItem(pagerAdapter.getItemCount() - 1, false);
                }
            }
        });
    }

    private void showDialogOnFirstRun() {
        boolean hasPromptBeenShown = prefs.getBoolean(KEY_FIRST_RUN_PROMPT_SHOWN, false);
        if (hasPromptBeenShown) return;

        final Dialog dialog = new Dialog(this);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        dialog.setContentView(R.layout.dialog_set_default_launcher);

        Button goToSettingsButton = dialog.findViewById(R.id.button_go_to_setting);
        Button laterButton = dialog.findViewById(R.id.button_later);

        goToSettingsButton.setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
            dialog.dismiss();
        });

        laterButton.setOnClickListener(v -> {
            dialog.dismiss();
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.setCancelable(false);

        dialog.show();

        prefs.edit().putBoolean(KEY_FIRST_RUN_PROMPT_SHOWN, true).apply();
    }

    private void showLauncherMenuDialog() {
        View taskbarView = findViewById(R.id.lnrTaskBar);
        int taskbarHeight = 0;
        if (taskbarView != null) {
            taskbarHeight = taskbarView.getHeight();
            if (taskbarHeight == 0) {
                // Attempt to measure if not laid out yet
                taskbarView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                taskbarHeight = taskbarView.getMeasuredHeight();
            }
        }

        // Include bottom system bar (navigation) inset to stay above it
        int bottomSystemInset = 0;
        View root = findViewById(android.R.id.content);
        if (root != null) {
            WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(root);
            if (insets != null) {
                bottomSystemInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            }
        }

        // Add a small safe margin in landscape to avoid touching the taskbar visually
        int extraSafePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        int bottomOffset = taskbarHeight + bottomSystemInset + extraSafePx;

        LauncherMenuDialogFragment menuDialog = LauncherMenuDialogFragment.newInstance(bottomOffset);
        menuDialog.show(getSupportFragmentManager(), "LauncherMenuDialog");
    }

    public void showGridOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_grid_options, null);
        builder.setView(dialogView);
        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();

        final RadioGroup radioGroup = dialogView.findViewById(R.id.radio_group_icon_size);
        int currentSize = PreferenceHelper.getIconSize(this);
        if (currentSize == PreferenceHelper.ICON_SIZE_SMALL) radioGroup.check(R.id.radio_small);
        else if (currentSize == PreferenceHelper.ICON_SIZE_LARGE)
            radioGroup.check(R.id.radio_large);
        else radioGroup.check(R.id.radio_medium);

        dialogView.findViewById(R.id.btn_save).setOnClickListener(v -> {
            int selectedId = radioGroup.getCheckedRadioButtonId();
            int newSize;
            if (selectedId == R.id.radio_small) newSize = PreferenceHelper.ICON_SIZE_SMALL;
            else if (selectedId == R.id.radio_large) newSize = PreferenceHelper.ICON_SIZE_LARGE;
            else newSize = PreferenceHelper.ICON_SIZE_MEDIUM;
            PreferenceHelper.setIconSize(this, newSize);

            // Update ViewModel immediately for Android 15+ compatibility
            settingsViewModel.setIconSize(newSize);

            // Send broadcast for backward compatibility
            sendBroadcast(new Intent(ACTION_ICON_SIZE_CHANGED));

            // Force immediate UI refresh for Android 15+
            forceImmediateUIRefresh();

            Toast.makeText(this, "Icon size updated!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
    }

    public void showResetConfirmationDialog() {
        final Dialog dialog = new Dialog(this);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.setContentView(R.layout.dialog_reset_layout);

        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(layoutParams);
        }

        Button resetButton = dialog.findViewById(R.id.button_reset);
        Button cancelButton = dialog.findViewById(R.id.button_cancel);

        resetButton.setOnClickListener(v -> {
            resetLayoutToDefault();
            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> {
            dialog.dismiss();
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.show();
    }

    private void resetLayoutToDefault() {
        databaseWriteExecutor.execute(() -> {
            List<AppItem> allItems = appItemDao.getAllItemsNow();
            if (allItems != null) {
                for (AppItem item : allItems) {
                    if (item.type == AppItem.Type.WIDGET) {
                        appWidgetHost.deleteAppWidgetId(item.appWidgetId);
                    }
                    if (item.customIconPath != null) {
                        File customIconFile = new File(item.customIconPath);
                        if (customIconFile.exists()) {
                            customIconFile.delete();
                        }
                    }
                }
            }
            appItemDao.deleteAllItems();
            mainThreadHandler.post(this::populateDefaultHomeScreen);
        });
    }

    private void populateDefaultHomeScreen() {
        databaseWriteExecutor.execute(() -> {
            if (!appItemDao.getItemsForPageNow(0).isEmpty()) return;
            List<AppItem> defaultItemsToInsert = new ArrayList<>();
            defaultItemsToInsert.add(createSpecialItem("Google Apps", AppItem.Type.GOOGLE_FOLDER, 0, 0));
            defaultItemsToInsert.add(createSpecialItem("Social Media", AppItem.Type.SOCIAL_MEDIA_FOLDER, 1, 0));
            defaultItemsToInsert.add(createSpecialItem("User", AppItem.Type.USER, 2, 0));
            defaultItemsToInsert.add(createSpecialItem("This PC", AppItem.Type.FILE_MANAGER_ACTION, 3, 0));
            defaultItemsToInsert.add(createSpecialItem("Wallpapers", AppItem.Type.WALLPAPER_ACTION, 4, 0));
            defaultItemsToInsert.add(createSpecialItem("Setting", AppItem.Type.SETTING, 5, 0));

            PackageManager pm = getPackageManager();
            List<ResolveInfo> allInstalledApps = pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
            int[][] defaultAppPositions = {{0, 1}, {1, 1}, {2, 1}};
            for (int i = 0; i < DEFAULT_APP_PACKAGES_ORDERED.size() && i < defaultAppPositions.length; i++) {
                String packageName = DEFAULT_APP_PACKAGES_ORDERED.get(i);
                int[] position = defaultAppPositions[i];
                for (ResolveInfo info : allInstalledApps) {
                    if (info.activityInfo.packageName.equals(packageName)) {
                        AppItem newItem = new AppItem(info.loadLabel(pm).toString(), AppItem.Type.APP, position[0], position[1], info.activityInfo);
                        newItem.page = 0;
                        defaultItemsToInsert.add(newItem);
                        break;
                    }
                }
            }
            appItemDao.insertAll(defaultItemsToInsert);
        });
    }

    private AppItem createSpecialItem(String name, AppItem.Type type, int row, int col) {
        AppItem item = new AppItem(name, type, row, col, null);
        item.page = 0;
        item.originalName = name;
        return item;
    }

    public void showAppPicker() {
        Intent intent = new Intent(this, AppListActivity.class);
        databaseWriteExecutor.execute(() -> {
            List<AppItem> allItems = appItemDao.getAllItemsNow();
            HashSet<String> presentPackages = new HashSet<>();
            if (allItems != null) {
                for (AppItem item : allItems) {
                    if (item.type == AppItem.Type.APP && item.packageName != null) {
                        presentPackages.add(item.packageName);
                    }
                }
            }
            intent.putExtra("present_packages", presentPackages);
            startActivityForResult(intent, REQUEST_PICK_APP);
        });
    }

    public void showWidgetList() {
        Intent intent = new Intent(this, WidgetListActivity.class);
        databaseWriteExecutor.execute(() -> {
            List<AppItem> allItems = appItemDao.getAllItemsNow();
            HashSet<String> presentProviders = new HashSet<>();
            if (allItems != null) {
                for (AppItem item : allItems) {
                    if (item.type == AppItem.Type.WIDGET) {
                        AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(item.appWidgetId);
                        if (info != null) presentProviders.add(info.provider.flattenToString());
                    }
                }
            }
            intent.putExtra("present_providers", presentProviders);
            startActivityForResult(intent, REQUEST_PICK_WIDGET);
        });
    }

    private void addNewWidgetToScreen(AppWidgetProviderInfo info) {
        int widgetId = appWidgetHost.allocateAppWidgetId();
        if (appWidgetManager.bindAppWidgetIdIfAllowed(widgetId, info.provider)) {
            if (info.configure != null) {
                Intent cfg = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
                cfg.setComponent(info.configure);
                cfg.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                try {
                    startActivityForResult(cfg, REQUEST_CREATE_APPWIDGET);
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException trying to launch widget configuration activity for " + info.provider.flattenToShortString() + ": " + e.getMessage(), e);
                    Toast.makeText(this, "Cannot configure widget. It might require special permissions or is not exported. Removing it.", Toast.LENGTH_LONG).show();
                    appWidgetHost.deleteAppWidgetId(widgetId);
                } catch (Exception e) {
                    Log.e(TAG, "Error launching widget configuration activity for " + info.provider.flattenToShortString() + ": " + e.getMessage(), e);
                    Toast.makeText(this, "Cannot configure widget. An error occurred. Removing it.", Toast.LENGTH_LONG).show();
                    appWidgetHost.deleteAppWidgetId(widgetId);
                }
            } else {
                handleAddItem(null, widgetId, info);
            }
        } else {
            Intent bind = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
            bind.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            bind.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider);
            try {
                startActivityForResult(bind, REQUEST_CREATE_APPWIDGET);
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException trying to bind widget for " + info.provider.flattenToShortString() + ": " + e.getMessage(), e);
                Toast.makeText(this, "Cannot bind widget. It might require special permissions. Removing it.", Toast.LENGTH_LONG).show();
                appWidgetHost.deleteAppWidgetId(widgetId);
            } catch (Exception e) {
                Log.e(TAG, "Error binding widget for " + info.provider.flattenToShortString() + ": " + e.getMessage(), e);
                Toast.makeText(this, "Cannot bind widget. An error occurred. Removing it.", Toast.LENGTH_LONG).show();
                appWidgetHost.deleteAppWidgetId(widgetId);
            }
        }
    }

    private void handleAddItem(ResolveInfo appInfo, int appWidgetId, AppWidgetProviderInfo widgetInfo) {
        databaseWriteExecutor.execute(() -> {
            int requiredRowSpan = 1;
            int requiredColSpan = 1;
            if (widgetInfo != null) {
                DisplayMetrics dm = getResources().getDisplayMetrics();
                float density = dm.density;
                int screenWidthPx = dm.widthPixels;
                int bottomMarginPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, dm);
                int gridHeightPx = dm.heightPixels - bottomMarginPx;
                // Use the static grid size variables
                int cols = HomeScreenFragment.COLS;
                int rows = HomeScreenFragment.ROWS;

                int cellWidthPx = screenWidthPx / cols;
                int cellHeightPx = (gridHeightPx > 0 ? gridHeightPx : 0) / rows;

                int minHeightPx;
                if ((widgetInfo.resizeMode & AppWidgetProviderInfo.RESIZE_VERTICAL) != 0 && widgetInfo.minResizeHeight > 0) {
                    minHeightPx = (int) (widgetInfo.minResizeHeight * density);
                } else {
                    minHeightPx = (int) (widgetInfo.minHeight * density);
                }
                int minWidthPx;
                if ((widgetInfo.resizeMode & AppWidgetProviderInfo.RESIZE_HORIZONTAL) != 0 && widgetInfo.minResizeWidth > 0) {
                    minWidthPx = (int) (widgetInfo.minResizeWidth * density);
                } else {
                    minWidthPx = (int) (widgetInfo.minWidth * density);
                }

                requiredColSpan = (int) Math.ceil((double) minWidthPx / cellWidthPx);
                requiredRowSpan = (int) Math.ceil((double) minHeightPx / cellHeightPx);

                requiredColSpan = Math.max(1, Math.min(requiredColSpan, cols));
                requiredRowSpan = Math.max(1, Math.min(requiredRowSpan, rows));
            }

            List<Integer> currentPageIds = appItemDao.getAllItemsNow().stream().map(item -> item.page).distinct().sorted().collect(Collectors.toList());
            if (currentPageIds.isEmpty()) currentPageIds.add(0);

            int targetPageId = -1;
            int[] pos = null;
            // For single-cell apps, try to place them at the last position on existing pages
            if (requiredRowSpan == 1 && requiredColSpan == 1) {
                for (int pageId : currentPageIds) {
                    pos = appItemDao.findLastFreeCellOnPageNow(pageId, HomeScreenFragment.ROWS, HomeScreenFragment.COLS);
                    if (pos != null) {
                        targetPageId = pageId;
                        break;
                    }
                }
            } else {
                // For widgets or multi-cell items, use first available position
                for (int pageId : currentPageIds) {
                    pos = appItemDao.findFirstFreeCellOnPageNow(pageId, requiredRowSpan, requiredColSpan, HomeScreenFragment.ROWS, HomeScreenFragment.COLS);
                    if (pos != null) {
                        targetPageId = pageId;
                        break;
                    }
                }
            }
            boolean isNewPage = false;
            if (targetPageId == -1) {
                targetPageId = currentPageIds.isEmpty() ? 0 : currentPageIds.get(currentPageIds.size() - 1) + 1;
                pos = new int[]{0, 0, targetPageId};
                isNewPage = true;
            }
            if (pos == null) {
                mainThreadHandler.post(() -> Toast.makeText(this, "Error: Could not find space.", Toast.LENGTH_SHORT).show());
                if (appWidgetId != -1) {
                    appWidgetHost.deleteAppWidgetId(appWidgetId);
                }
                return;
            }
            final AppItem newItem;
            if (appInfo != null) {
                newItem = new AppItem(appInfo.loadLabel(getPackageManager()).toString(), AppItem.Type.APP, pos[0], pos[1], appInfo.activityInfo);
            } else {
                newItem = new AppItem(widgetInfo.loadLabel(getPackageManager()), AppItem.Type.WIDGET, pos[0], pos[1], null);
                newItem.setAppWidgetId(appWidgetId);
                newItem.rowSpan = requiredRowSpan;
                newItem.colSpan = requiredColSpan;
            }
            newItem.page = targetPageId;
            appItemDao.insert(newItem);

            final int finalTargetPageId = targetPageId;
            final boolean finalIsNewPage = isNewPage;
            mainThreadHandler.post(() -> {
                if (finalIsNewPage) pagerAdapter.addPage(finalTargetPageId);
                int adapterPosition = pagerAdapter.getPagePosition(finalTargetPageId);
                if (adapterPosition != -1) viewPager.setCurrentItem(adapterPosition, true);
            });
        });
    }

    private void removeAppByPackageName(String packageName) {
        databaseWriteExecutor.execute(() -> appItemDao.deleteByPackageName(packageName));
    }

    private void removeWidgetByProvider(AppWidgetProviderInfo appWidgetInfo) {
        databaseWriteExecutor.execute(() -> {
            List<AppItem> allItems = appItemDao.getAllItemsNow();
            for (AppItem item : allItems) {
                if (item.type == AppItem.Type.WIDGET) {
                    AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(item.appWidgetId);
                    if (info != null && info.provider.equals(appWidgetInfo.provider)) {
                        appWidgetHost.deleteAppWidgetId(item.appWidgetId);
                        appItemDao.delete(item);
                        break;
                    }
                }
            }
        });
    }

//    public void addNewPage() {
//        databaseWriteExecutor.execute(() -> {
//            List<Integer> pageIds = appItemDao.getAllItemsNow().stream().map(item -> item.page).distinct().sorted().collect(Collectors.toList());
//            int newPageId = pageIds.isEmpty() ? 0 : pageIds.get(pageIds.size() - 1) + 1;
//            mainThreadHandler.post(() -> {
//                pagerAdapter.addPage(newPageId);
//                viewPager.setCurrentItem(pagerAdapter.getItemCount() - 1, true);
//                Toast.makeText(this, "New page added", Toast.LENGTH_SHORT).show();
//            });
//        });
//    }


    public void addNewPage() {
        Log.d(TAG, "addNewPage: Starting to add new page");

        databaseWriteExecutor.execute(() -> {
            try {
                // Get current page IDs from database
                List<Integer> pageIdsFromDb = appItemDao.getAllItemsNow()
                        .stream()
                        .map(item -> item.page)
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());

                Log.d(TAG, "addNewPage: Current pages in DB: " + pageIdsFromDb);

                // If no pages exist, start with 0
                if (pageIdsFromDb.isEmpty()) {
                    pageIdsFromDb.add(0);
                }

                // Calculate new page ID (max + 1) considering adapter pages as well
                int maxIdDb = pageIdsFromDb.get(pageIdsFromDb.size() - 1);
                int maxIdAdapter = pagerAdapter.getPageIds().stream().mapToInt(Integer::intValue).max().orElse(maxIdDb);
                int newPageId = Math.max(maxIdDb, maxIdAdapter) + 1;
                Log.d(TAG, "addNewPage: New page ID will be: " + newPageId);

                // Check if this page already exists in adapter OR DB
                boolean pageExistsInAdapter = pagerAdapter.hasPage(newPageId);
                boolean pageExistsInDb = pageIdsFromDb.contains(newPageId);
                Log.d(TAG, "addNewPage: Page exists in adapter: " + pageExistsInAdapter);

                if (!pageExistsInAdapter) {
                    // Create a placeholder item in the database for this page to ensure it persists
                    // This is a dummy item that will be invisible but ensures the page exists in DB
                    AppItem placeholderItem = new AppItem("__PAGE_MARKER__", AppItem.Type.APP, -1, -1, null);
                    placeholderItem.page = newPageId;
                    placeholderItem.isSpecialItem = true; // Mark as special item instead of using setVisible
                    appItemDao.insert(placeholderItem);

                    final int finalNewPageId = newPageId;
                    mainThreadHandler.post(() -> {
                        try {
                            Log.d(TAG, "addNewPage: Adding page to adapter: " + finalNewPageId);

                            // Add the page to adapter
                            pagerAdapter.addPage(finalNewPageId);
                            // Re-attach dots indicator so it reflects the new page count
                            if (dotsIndicator != null) {
                                dotsIndicator.attachTo(viewPager);
                            }

                            // Get the position of the new page
                            int newPosition = pagerAdapter.getPagePosition(finalNewPageId);
                            Log.d(TAG, "addNewPage: New page position: " + newPosition);

                            if (newPosition != -1) {
                                // Set current item to new page with smooth scroll
                                viewPager.setCurrentItem(newPosition, true);

                                // Ensure adapter/UI fully aware (fallback refresh)
                                pagerAdapter.notifyDataSetChanged();

                                Toast.makeText(this, "New page added successfully!", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "addNewPage: Page added successfully");
                            } else {
                                Log.e(TAG, "addNewPage: Failed to get position for new page");
                                Toast.makeText(this, "Failed to add new page", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "addNewPage: Error in main thread: " + e.getMessage(), e);
                            Toast.makeText(this, "Error adding page: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    mainThreadHandler.post(() -> {
                        Toast.makeText(this, pageExistsInDb ? "Page already exists" : "Page already in adapter", Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "addNewPage: Page " + newPageId + " already exists in adapter");
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "addNewPage: Error in background thread: " + e.getMessage(), e);
                mainThreadHandler.post(() ->
                        Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    public void removeCurrentPage() {
        if (pagerAdapter.getItemCount() <= 1) {
            Toast.makeText(this, "Cannot remove the last page", Toast.LENGTH_SHORT).show();
            return;
        }
        int currentPosition = viewPager.getCurrentItem();
        int pageIdToRemove = pagerAdapter.getPageId(currentPosition);
        new AlertDialog.Builder(this)
                .setTitle("Remove Page")
                .setMessage("Are you sure you want to remove this page and all its items?")
                .setPositiveButton("Remove", (d, w) -> {
                    databaseWriteExecutor.execute(() -> {
                        List<AppItem> itemsOnPage = appItemDao.getItemsForPageNow(pageIdToRemove);
                        for (AppItem item : itemsOnPage) {
                            if (item.getType() == AppItem.Type.WIDGET) {
                                appWidgetHost.deleteAppWidgetId(item.getAppWidgetId());
                            }
                            if (item.customIconPath != null) {
                                new File(item.customIconPath).delete();
                            }
                        }
                        appItemDao.deletePage(pageIdToRemove);
                        mainThreadHandler.post(() -> {
                            pagerAdapter.removePage(currentPosition);
                            // Re-attach dots indicator so it reflects the new page count
                            if (dotsIndicator != null) {
                                dotsIndicator.attachTo(viewPager);
                            }
                            Toast.makeText(this, "Page removed", Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // IMPROVED CROSS-PAGE DRAGGING METHODS
    @Override
    public void startCrossPageDrag(AppItem item, Bitmap iconBitmap, Rect initialRect,
                                   int originalPage, int originalRow, int originalCol,
                                   float initialTouchRawX, float initialTouchRawY) {
        if (isCrossPageDragging) {
            Log.w(TAG, "Already dragging an item. Ignoring new drag request.");
            return;
        }

        mDragItem = item;
        mDragItemOriginalRow = originalRow;
        mDragItemOriginalCol = originalCol;
        mOriginalPageIndex = originalPage;
        isCrossPageDragging = true;
        mWasDragging = true;
        Log.d(TAG, "startCrossPageDrag: mDragItem set to " + mDragItem.getName() + " (ID: " + mDragItem.id + ") from Page: " + mOriginalPageIndex);

        // Hide the original item view in the source fragment
        removeItemFromPage(item);

        // Create drag view with improved visual feedback
        if (mDragView == null) {
            mDragView = new ImageView(this);
            mDragOverlay.addView(mDragView);
        }
        mDragView.setImageBitmap(iconBitmap);
        mDragView.setAlpha(0.95f);
        mDragView.setElevation(16f);

        // Set initial position and size with smooth scaling
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(initialRect.width(), initialRect.height());
        mDragView.setLayoutParams(params);
        mDragView.setX(initialRect.left);
        mDragView.setY(initialRect.top);
        // Remove aggressive scale to avoid pop effect
        mDragView.setScaleX(1.0f);
        mDragView.setScaleY(1.0f);

        // Calculate offset from top-left of the view to the touch point
        mDragOffsetX = initialTouchRawX - initialRect.left;
        mDragOffsetY = initialTouchRawY - initialRect.top;

        mDragOverlay.setVisibility(View.VISIBLE);
        mDragOverlay.setOnTouchListener(new ImprovedDragOverlayTouchListener());
        // Make sure parents do not intercept during cross-page drag
        requestDisallowInterceptTouchEvent(true);

        requestDisallowInterceptTouchEvent(true);
    }

    private void revertDraggedItemToOriginalPosition() {
        Log.d(TAG, "Reverting dragged item to original position.");

        if (mDragOverlay != null) {
            mDragOverlay.removeAllViews();
            mDragOverlay.setVisibility(View.GONE);
            mDragView = null;
        }

        final AppItem itemToRevert = mDragItem;
        if (itemToRevert == null) {
            Log.e(TAG, "mDragItem is null during revertDraggedItemToOriginalPosition, cannot revert.");
            requestDisallowInterceptTouchEvent(false);
            return;
        }

        final int originalPageId = mOriginalPageIndex;
        final int originalRow = mDragItemOriginalRow;
        final int originalCol = mDragItemOriginalCol;

        databaseWriteExecutor.execute(() -> {
            itemToRevert.page = originalPageId;
            itemToRevert.setPosition(originalRow, originalCol);
            appItemDao.update(itemToRevert);

            mainThreadHandler.post(() -> {
                Toast.makeText(LauncherActivity.this, "Item reverted to its original position.", Toast.LENGTH_SHORT).show();

                HomeScreenFragment originalFragment = (HomeScreenFragment) getSupportFragmentManager().findFragmentByTag("f" + pagerAdapter.getPagePosition(originalPageId));
                if (originalFragment != null) {
                    originalFragment.forceRefreshLayoutFromDb();
                    Log.d(TAG, "Forced refresh on original fragment (Page " + originalPageId + ") after revert.");
                } else {
                    Log.w(TAG, "Original fragment (Page " + originalPageId + ") not found in FragmentManager to force refresh after revert.");
                }
            });
        });

        requestDisallowInterceptTouchEvent(false);
        mDragItem = null;
        mDragItemOriginalRow = -1;
        mDragItemOriginalCol = -1;
        mOriginalPageIndex = -1;
    }

//    private void stopCrossPageDrag(MotionEvent event) {
//        if (!isCrossPageDragging) return;
//
//        isCrossPageDragging = false;
//        mPageScrollHandler.removeCallbacks(mScrollLeftRunnable);
//        mPageScrollHandler.removeCallbacks(mScrollRightRunnable);
//
//        Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
//        if (!(currentFragment instanceof HomeScreenFragment)) {
//            Log.e(TAG, "Current fragment is not HomeScreenFragment. Cannot drop item.");
//            revertDraggedItemToOriginalPosition();
//            return;
//        }
//        HomeScreenFragment targetFragment = (HomeScreenFragment) currentFragment;
//
//        int[] overlayCoords = new int[2];
//        mDragOverlay.getLocationOnScreen(overlayCoords);
//
//        float dropRawX = event.getRawX();
//        float dropRawY = event.getRawY();
//
//        int[] targetFragmentRootCoords = new int[2];
//        targetFragment.getView().getLocationOnScreen(targetFragmentRootCoords);
//
//        float dropXRelativeToFragment = dropRawX - targetFragmentRootCoords[0];
//        float dropYRelativeToFragment = dropRawY - targetFragmentRootCoords[1];
//
//        int newCol = (int) (dropXRelativeToFragment / targetFragment.getCellWidth());
//        int newRow = (int) (dropYRelativeToFragment / targetFragment.getCellHeight());
//
//        final int finalNewRow = Math.max(0, Math.min(newRow, HomeScreenFragment.ROWS - mDragItem.getRowSpan()));
//        final int finalNewCol = Math.max(0, Math.min(newCol, HomeScreenFragment.COLS - mDragItem.getColSpan()));
//
//        final AppItem droppedAppItem = mDragItem;
//        if (droppedAppItem == null) {
//            Log.e(TAG, "mDragItem is null during stopCrossPageDrag, cannot process drop. Reverting.");
//            revertDraggedItemToOriginalPosition();
//            return;
//        }
//
//        final int currentDragItemOriginalRow = mDragItemOriginalRow;
//        final int currentDragItemOriginalCol = mDragItemOriginalCol;
//        final int currentOriginalPageIndex = mOriginalPageIndex;
//
//        View targetFragmentRoot = targetFragment.getView();
//        if (targetFragmentRoot != null) {
//            float animateToXOnScreen = targetFragmentRootCoords[0] + finalNewCol * targetFragment.getCellWidth();
//            float animateToYOnScreen = targetFragmentRootCoords[1] + finalNewRow * targetFragment.getCellHeight();
//
//            mDragView.animate()
//                    .x(animateToXOnScreen - overlayCoords[0])
//                    .y(animateToYOnScreen - overlayCoords[1])
//                    .scaleX(1.0f)
//                    .scaleY(1.0f)
//                    .alpha(1.0f)
//                    .setDuration(250)
//                    .withEndAction(() -> {
//                        if (mDragOverlay != null) {
//                            mDragOverlay.removeAllViews();
//                            mDragOverlay.setVisibility(View.GONE);
//                            mDragView = null;
//                        }
//
//                        targetFragment.placeItemOnGridWithDisplacement(droppedAppItem, finalNewRow, finalNewCol,
//                                currentDragItemOriginalRow, currentDragItemOriginalCol,
//                                currentOriginalPageIndex);
//
//                        requestDisallowInterceptTouchEvent(false);
//                        mDragItem = null;
//                        mDragItemOriginalRow = -1;
//                        mDragItemOriginalCol = -1;
//                        mOriginalPageIndex = -1;
//                    })
//                    .start();
//        } else {
//            Log.e(TAG, "Target fragment root view is null. Reverting dragged item.");
//            revertDraggedItemToOriginalPosition();
//        }
//    }

    @Override
    public void removeItemFromPage(AppItem item) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("f" + pagerAdapter.getPagePosition(item.page));
        if (fragment instanceof HomeScreenFragment) {
            ((HomeScreenFragment) fragment).removeItemViewAndClearCells(item);
            Log.d(TAG, "removeItemFromPage: Removed view and cleared cells for " + item.getName() + " from Page: " + item.page);
        }
    }

    @Override
    public void onItemDroppedOnPage(AppItem item, int newRow, int newCol, int originalPageId) {
        Log.d(TAG, "onItemDroppedOnPage called, but primary drop handling is in placeItemOnGridWithDisplacement.");
    }

    @Override
    public HomeScreenAdapter getPagerAdapter() {
        return pagerAdapter;
    }

    private void cleanupDragState() {
        isCrossPageDragging = false;
        mWasDragging = false;
        mPageScrollHandler.removeCallbacks(mScrollLeftRunnable);
        mPageScrollHandler.removeCallbacks(mScrollRightRunnable);

        if (mDragOverlay != null) {
            mDragOverlay.setVisibility(View.GONE);
            mDragOverlay.setOnTouchListener(null);
        }

        requestDisallowInterceptTouchEvent(false);

    }

//    private class ImprovedDragOverlayTouchListener implements View.OnTouchListener {
//        private float lastTouchX, lastTouchY;
//
//        @Override
//        public boolean onTouch(View v, MotionEvent event) {
//            switch (event.getAction()) {
//                case MotionEvent.ACTION_DOWN:
//                    lastTouchX = event.getRawX();
//                    lastTouchY = event.getRawY();
//                    break;
//
//                case MotionEvent.ACTION_MOVE:
//                    float newX = event.getRawX() - mDragOffsetX;
//                    float newY = event.getRawY() - mDragOffsetY;
//
//                    // Smooth movement with interpolation for better feel
//                    float currentX = mDragView.getX();
//                    float currentY = mDragView.getY();
//                    float interpolatedX = currentX + (newX - currentX) * 0.7f;
//                    float interpolatedY = currentY + (newY - currentY) * 0.7f;
//


//                    mDragView.setX(interpolatedX);
//                    mDragView.setY(interpolatedY);
//
//                    // Improved edge detection with visual feedback
//                    int screenWidth = getResources().getDisplayMetrics().widthPixels;
//                    float leftEdgeThreshold = screenWidth * EDGE_SCROLL_THRESHOLD_RATIO;
//                    float rightEdgeThreshold = screenWidth * (1.0f - EDGE_SCROLL_THRESHOLD_RATIO);
//
//                    if (event.getRawX() < leftEdgeThreshold) {
//                        if (viewPager.getCurrentItem() > 0) {
//                            mPageScrollHandler.removeCallbacks(mScrollRightRunnable);
//                            mPageScrollHandler.postDelayed(mScrollLeftRunnable, PAGE_SCROLL_DELAY_MS);
//                            // Visual feedback for edge scrolling
//                            mDragView.setAlpha(0.7f);
//                        }
//                    } else if (event.getRawX() > rightEdgeThreshold) {
//                        if (viewPager.getCurrentItem() < pagerAdapter.getItemCount() - 1) {
//                            mPageScrollHandler.removeCallbacks(mScrollLeftRunnable);
//                            mPageScrollHandler.postDelayed(mScrollRightRunnable, PAGE_SCROLL_DELAY_MS);
//                            mDragView.setAlpha(0.7f);
//                        }
//                    } else {
//                        mPageScrollHandler.removeCallbacks(mScrollLeftRunnable);
//                        mPageScrollHandler.removeCallbacks(mScrollRightRunnable);
//                        mDragView.setAlpha(0.9f);
//                    }
//                    mWasDragging = true;
//                    break;
//
//                case MotionEvent.ACTION_UP:
//                case MotionEvent.ACTION_CANCEL:
//                    if (mWasDragging) {
//                        stopCrossPageDrag(event);
//                    } else {
//                        revertDraggedItemToOriginalPosition();
//                    }
//                    mWasDragging = false;
//                    break;
//            }
//            return true;
//        }
//    }

    private void stopCrossPageDrag(MotionEvent event) {
        if (!isCrossPageDragging) {
            Log.w(TAG, "stopCrossPageDrag called but not currently dragging");
            return;
        }

        Log.d(TAG, "Stopping cross-page drag");
        isCrossPageDragging = false;
        mPageScrollHandler.removeCallbacks(mScrollLeftRunnable);
        mPageScrollHandler.removeCallbacks(mScrollRightRunnable);

        // Safety check for mDragView
        if (mDragView == null) {
            Log.e(TAG, "mDragView is null in stopCrossPageDrag, cannot animate");
            cleanupDragState();
            return;
        }

        Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
        if (!(currentFragment instanceof HomeScreenFragment)) {
            Log.e(TAG, "Current fragment is not HomeScreenFragment. Cannot drop item.");
            revertDraggedItemToOriginalPosition();
            return;
        }

        HomeScreenFragment targetFragment = (HomeScreenFragment) currentFragment;

        try {
            int[] overlayCoords = new int[2];
            mDragOverlay.getLocationOnScreen(overlayCoords);

            float dropRawX = event.getRawX();
            float dropRawY = event.getRawY();

            View targetFragmentView = targetFragment.getView();
            if (targetFragmentView == null) {
                Log.e(TAG, "Target fragment view is null");
                revertDraggedItemToOriginalPosition();
                return;
            }

            int[] targetFragmentRootCoords = new int[2];
            targetFragmentView.getLocationOnScreen(targetFragmentRootCoords);

            float dropXRelativeToFragment = dropRawX - targetFragmentRootCoords[0];
            float dropYRelativeToFragment = dropRawY - targetFragmentRootCoords[1];

            int newCol = (int) (dropXRelativeToFragment / targetFragment.getCellWidth());
            int newRow = (int) (dropYRelativeToFragment / targetFragment.getCellHeight());

            final int finalNewRow = Math.max(0, Math.min(newRow, HomeScreenFragment.ROWS - mDragItem.getRowSpan()));
            final int finalNewCol = Math.max(0, Math.min(newCol, HomeScreenFragment.COLS - mDragItem.getColSpan()));

            final AppItem droppedAppItem = mDragItem;
            if (droppedAppItem == null) {
                Log.e(TAG, "mDragItem is null during stopCrossPageDrag");
                revertDraggedItemToOriginalPosition();
                return;
            }

            final int currentDragItemOriginalRow = mDragItemOriginalRow;
            final int currentDragItemOriginalCol = mDragItemOriginalCol;
            final int currentOriginalPageIndex = mOriginalPageIndex;

            // Animate to final position
            float animateToXOnScreen = targetFragmentRootCoords[0] + finalNewCol * targetFragment.getCellWidth();
            float animateToYOnScreen = targetFragmentRootCoords[1] + finalNewRow * targetFragment.getCellHeight();

            mDragView.animate()
                    .x(animateToXOnScreen - overlayCoords[0])
                    .y(animateToYOnScreen - overlayCoords[1])
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(1.0f)
                    .setDuration(250)
                    .withEndAction(() -> {
                        cleanupDragOverlay();

                        // Process the drop
                        targetFragment.placeItemOnGridWithDisplacement(droppedAppItem, finalNewRow, finalNewCol,
                                currentDragItemOriginalRow, currentDragItemOriginalCol,
                                currentOriginalPageIndex);

                        requestDisallowInterceptTouchEvent(false);
                        resetDragState();
                    })
                    .start();

        } catch (Exception e) {
            Log.e(TAG, "Error in stopCrossPageDrag: " + e.getMessage(), e);
            revertDraggedItemToOriginalPosition();
        }
    }

    private void cleanupDragOverlay() {
        if (mDragOverlay != null) {
            mDragOverlay.removeAllViews();
            mDragOverlay.setVisibility(View.GONE);
            mDragOverlay.setOnTouchListener(null);
        }
        mDragView = null;
    }

    private void resetDragState() {
        mDragItem = null;
        mDragItemOriginalRow = -1;
        mDragItemOriginalCol = -1;
        mOriginalPageIndex = -1;
        isCrossPageDragging = false;
        mWasDragging = false;
    }

    public HomeScreenFragment getCurrentFragment() {
        return (HomeScreenFragment) getSupportFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
    }

    /**
     * Force immediate UI refresh for Android 15+ compatibility
     * This method ensures that icon size changes are reflected immediately
     */
    public void forceImmediateUIRefresh() {
        // For Android 15+ (API 35+), we need to force a more aggressive UI refresh
        if (Build.VERSION.SDK_INT >= 35) {
            // Force refresh all visible fragments
            FragmentManager fragmentManager = getSupportFragmentManager();
            for (int i = 0; i < fragmentManager.getBackStackEntryCount() + 1; i++) {
                Fragment fragment = fragmentManager.findFragmentByTag("f" + i);
                if (fragment instanceof HomeScreenFragment) {
                    ((HomeScreenFragment) fragment).forceRefreshLayoutFromDb();
                }
            }

            // Force refresh the pager adapter
            if (pagerAdapter != null) {
                pagerAdapter.notifyDataSetChanged();
            }

            // Force refresh the current fragment specifically
            HomeScreenFragment currentFragment = getCurrentFragment();
            if (currentFragment != null) {
                currentFragment.forceRefreshLayoutFromDb();
            }

            // Additional refresh for Android 15+ - invalidate views
            if (rootLayout != null) {
                rootLayout.invalidate();
                rootLayout.requestLayout();
            }

            // Force a UI thread refresh
            mainThreadHandler.post(() -> {
                if (viewPager != null) {
                    viewPager.invalidate();
                    viewPager.requestLayout();
                }
            });
        } else {
            // For older Android versions, use the existing mechanism
            if (pagerAdapter != null) {
                pagerAdapter.notifyDataSetChanged();
            }
        }
    }

    private class UninstallReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getData() == null) return;
            if (Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())) {
                String removedPackageName = intent.getData().getSchemeSpecificPart();
                if (removedPackageName != null) {
                    Log.d(TAG, "Package removed: " + removedPackageName);
                    removeAppByPackageName(removedPackageName);
                }
            }
        }
    }

    private class ImprovedDragOverlayTouchListener implements View.OnTouchListener {
        private float lastTouchX, lastTouchY;
        private boolean isListenerActive = true;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // Safety check: if drag view is null or listener is not active, ignore events
            if (mDragView == null || !isListenerActive || !isCrossPageDragging) {
                Log.w(TAG, "Drag overlay touch ignored - mDragView null or listener inactive");
                return true; // Consume the event to prevent further processing
            }

            try {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastTouchX = event.getRawX();
                        lastTouchY = event.getRawY();
                        Log.d(TAG, "Drag overlay ACTION_DOWN");
                        break;

                    case MotionEvent.ACTION_MOVE:
                        // Double-check that mDragView is not null
                        if (mDragView == null) {
                            Log.e(TAG, "mDragView is null during ACTION_MOVE");
                            isListenerActive = false;
                            return true;
                        }

                        float newX = event.getRawX() - mDragOffsetX;
                        float newY = event.getRawY() - mDragOffsetY;

                        // Get current position safely
                        float currentX = mDragView.getX();
                        float currentY = mDragView.getY();

                        // Smooth movement with optimized interpolation for better responsiveness
                        float interpolatedX = currentX + (newX - currentX) * 0.8f;
                        float interpolatedY = currentY + (newY - currentY) * 0.8f;

                        // Apply the new position
                        mDragView.setX(interpolatedX);
                        mDragView.setY(interpolatedY);

                        // Optimized edge detection - check less frequently and use more efficient logic
                        int screenWidth = getResources().getDisplayMetrics().widthPixels;
                        float leftEdgeThreshold = screenWidth * EDGE_SCROLL_THRESHOLD_RATIO;
                        float rightEdgeThreshold = screenWidth * (1.0f - EDGE_SCROLL_THRESHOLD_RATIO);

                        // Only check edge scrolling every few move events to reduce overhead
                        if (event.getAction() == MotionEvent.ACTION_MOVE && event.getEventTime() % 3 == 0) {
                            if (event.getRawX() < leftEdgeThreshold) {
                                if (viewPager.getCurrentItem() > 0) {
                                    mPageScrollHandler.removeCallbacks(mScrollRightRunnable);
                                    mPageScrollHandler.postDelayed(mScrollLeftRunnable, PAGE_SCROLL_DELAY_MS);
                                    mDragView.setAlpha(0.7f);
                                }
                            } else if (event.getRawX() > rightEdgeThreshold) {
                                if (viewPager.getCurrentItem() < pagerAdapter.getItemCount() - 1) {
                                    mPageScrollHandler.removeCallbacks(mScrollLeftRunnable);
                                    mPageScrollHandler.postDelayed(mScrollRightRunnable, PAGE_SCROLL_DELAY_MS);
                                    mDragView.setAlpha(0.7f);
                                }
                            } else {
                                mPageScrollHandler.removeCallbacks(mScrollLeftRunnable);
                                mPageScrollHandler.removeCallbacks(mScrollRightRunnable);
                                mDragView.setAlpha(0.9f);
                            }
                        }
                        mWasDragging = true;
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        Log.d(TAG, "Drag overlay ACTION_UP/CANCEL, mWasDragging: " + mWasDragging);

                        // Safety check
                        if (mDragView == null) {
                            Log.e(TAG, "mDragView is null during ACTION_UP");
                            cleanupDragState();
                            return true;
                        }

                        if (mWasDragging) {
                            stopCrossPageDrag(event);
                        } else {
                            revertDraggedItemToOriginalPosition();
                        }
                        mWasDragging = false;
                        isListenerActive = false; // Deactivate listener after drag ends
                        break;
                }
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error in drag overlay touch listener: " + e.getMessage(), e);
                cleanupDragState();
                return true;
            }
        }
    }

    private void setThemeBasedOnOrientation(Configuration configuration) {
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setTheme(R.style.LandscapeTheme);
        } else {
            setTheme(R.style.Theme_YourAppTheme);
        }
    }
}