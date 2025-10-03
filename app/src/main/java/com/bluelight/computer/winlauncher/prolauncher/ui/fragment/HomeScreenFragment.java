package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.database.AppDatabase;
import com.bluelight.computer.winlauncher.prolauncher.database.AppItem;
import com.bluelight.computer.winlauncher.prolauncher.database.AppItemDao;
import com.bluelight.computer.winlauncher.prolauncher.model.AppItemViewModel;
import com.bluelight.computer.winlauncher.prolauncher.model.SettingsViewModel;
import com.bluelight.computer.winlauncher.prolauncher.model.TaskbarViewModel;
import com.bluelight.computer.winlauncher.prolauncher.model.prefrance.PreferenceHelper;
import com.bluelight.computer.winlauncher.prolauncher.ui.activity.LauncherActivity;
import com.bluelight.computer.winlauncher.prolauncher.ui.adapter.HomeScreenAdapter;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class HomeScreenFragment extends Fragment {


    public static final String KEY_PROFILE_SETUP_COMPLETE = "key_profile_setup_complete";
    public static final String KEY_PROFILE_NAME = "key_profile_name";
    public static final String KEY_PROFILE_IMAGE_PATH = "key_profile_image_path";
    public static final int ROWS_PORTRAIT = 6;
    public static final int COLS_PORTRAIT = 4;
    // Landscape: 4 x 6 grid for 24 cells to match portrait mode
    public static final int ROWS_LANDSCAPE = 4;
    public static final int COLS_LANDSCAPE = 6;
    private static final int LEGACY_STORAGE_PERMISSION_CODE = 101;
    private static final float ADAPTIVE_ICON_SCALE_FACTOR = 0.8f;
    private static final String TAG = "HomeScreenFragment";
    private static final String ARG_PAGE_INDEX = "page_index";
    private static final List<String> SOCIAL_MEDIA_PACKAGES = Arrays.asList("com.facebook.katana", "com.instagram.android", "com.linkedin.android", "com.whatsapp", "com.twitter.android", "com.snapchat.android");
    private static final List<String> GOOGLE_PACKAGES = Arrays.asList("com.google.android.youtube", "com.google.android.gm", "com.google.android.apps.maps", "com.google.android.apps.photos", "com.google.android.apps.docs", "com.android.chrome", "com.google.android.googlequicksearchbox");
    private static final String PREFS_NAME = "LauncherPrefs";
    private static final String KEY_FIRST_RUN_PROMPT_SHOWN = "first_run_prompt_shown";

    // PERFORMANCE OPTIMIZATIONS - Add these caching and thread pool improvements
    private static final int CORE_POOL_SIZE = 2;
    private static final int MAXIMUM_POOL_SIZE = 4;
    private static final long KEEP_ALIVE_TIME = 30L;
    private static final int BITMAP_CACHE_SIZE = 50; // Cache up to 50 icon bitmaps
    private final LruCache<String, Bitmap> iconBitmapCache = new LruCache<String, Bitmap>(BITMAP_CACHE_SIZE) {
        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            return bitmap.getByteCount() / 1024; // Size in KB
        }
    };
    private final Map<String, View> viewCache = new HashMap<>();
    private final AtomicBoolean isLayoutInProgress = new AtomicBoolean(false);
    private final Object layoutLock = new Object();

    // Dynamic grid size based on orientation
    public static int ROWS;
    public static int COLS;
    private AppItem[][] occupiedCells;
    private SharedPreferences prefs;
    private int pageIndex;
    private PageInteractionListener listener;
    private FrameLayout rootLayout;
    private AppWidgetManager appWidgetManager;
    private AppWidgetHost appWidgetHost;
    private int cellWidth;
    private int cellHeight;
    private AppItemDao appItemDao;
    private ExecutorService databaseWriteExecutor;
    private AppItemViewModel viewModel;
    private BroadcastReceiver iconSizeChangeReceiver;
    private boolean isReceiverRegistered = false;
    private ActivityResultLauncher<Intent> pickProfileImageLauncher;
    private ActivityResultLauncher<Intent> changeIconLauncher;
    private AppItem itemPendingIconChange;
    private TaskbarViewModel taskbarViewModel;
    private SettingsViewModel settingsViewModel;
    private AppItemViewModel appItemViewModel;
    private ImageView dialogProfileImageView;
    private Handler homeScreenHandler;
    private ActivityResultLauncher<Intent> manageStorageResultLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private Fragment pendingFragment = null;
    private String pendingTitle = null;
    private PopupWindow currentPopupWindow;

    public static HomeScreenFragment newInstance(int pageIndex) {
        HomeScreenFragment fragment = new HomeScreenFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE_INDEX, pageIndex);
        fragment.setArguments(args);
        return fragment;
    }

    // Static method to initialize grid size from any context
    public static void initializeGridSize(Context context) {
        if (context != null) {
            int orientation = context.getResources().getConfiguration().orientation;
            if (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                ROWS = ROWS_LANDSCAPE;
                COLS = COLS_LANDSCAPE;
            } else {
                ROWS = ROWS_PORTRAIT;
                COLS = COLS_PORTRAIT;
            }
        }
    }

    // Compact non-widget items row-major in landscape to avoid big gaps while preserving portraits untouched
    private List<AppItem> compactItemsForLandscape(List<AppItem> items) {
        if (items == null) return new ArrayList<>();
        // Temporary occupancy
        AppItem[][] temp = new AppItem[ROWS][COLS];

        // Place widgets first at their current or first fitting positions
        List<AppItem> widgets = new ArrayList<>();
        List<AppItem> others = new ArrayList<>();
        for (AppItem it : items) {
            if (it.getType() == AppItem.Type.WIDGET || it.getRowSpan() > 1 || it.getColSpan() > 1)
                widgets.add(it);
            else others.add(it);
        }

        // Keep widgets where they fit for this render only; do not persist moves
        for (AppItem w : widgets) {
            int r = Math.max(0, Math.min(w.getRow(), ROWS - Math.max(1, w.getRowSpan())));
            int c = Math.max(0, Math.min(w.getCol(), COLS - Math.max(1, w.getColSpan())));
            if (!isRegionFree(temp, r, c, w.getRowSpan(), w.getColSpan())) {
                int[] pos = findFirstFreeRegion(temp, w.getRowSpan(), w.getColSpan());
                if (pos != null) {
                    // Set position in-memory only for this render pass
                    w.setPosition(pos[0], pos[1]);
                    r = pos[0];
                    c = pos[1];
                } else {
                    // No room on this page during landscape; skip rendering this widget in compact view
                    continue;
                }
            }
            for (int rr = r; rr < r + w.getRowSpan(); rr++)
                for (int cc = c; cc < c + w.getColSpan(); cc++) temp[rr][cc] = w;
        }

        // Now pack apps and special items row-major (top-left to bottom-right)
        // Keep their relative order by sorting by typePriority then by original (row, col)
        others.sort((a, b) -> {
            int pa = typePriority(a);
            int pb = typePriority(b);
            if (pa != pb) return Integer.compare(pa, pb);
            if (a.getRow() != b.getRow()) return Integer.compare(a.getRow(), b.getRow());
            return Integer.compare(a.getCol(), b.getCol());
        });

        for (AppItem it : others) {
            int[] pos = findFirstFreeRegion(temp, 1, 1);
            if (pos == null) {
                // No room left on this page in LANDSCAPE: do NOT persistently move to next page.
                // Keep original page and position; skip re-positioning for this render pass to avoid surprises.
                // This prevents items from jumping to pageIndex+1 unexpectedly.
                continue;
            }
            int nr = pos[0], nc = pos[1];
            // Update in-memory only for this render pass
            it.setPosition(nr, nc);
            it.setRowSpan(1);
            it.setColSpan(1);
            temp[nr][nc] = it;
        }

        return items;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof PageInteractionListener) {
            listener = (PageInteractionListener) context;
        } else {
            throw new RuntimeException(context + " must implement PageInteractionListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        homeScreenHandler = new Handler(Looper.getMainLooper());

        // Initialize grid size based on orientation
        initializeGridSize();

        manageStorageResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (Environment.isExternalStorageManager()) {
                            Toast.makeText(getContext(), "All Files Access Granted", Toast.LENGTH_SHORT).show();
                            executePendingAction();
                        } else {
                            Toast.makeText(getContext(), "Permission is required to browse files.", Toast.LENGTH_LONG).show();
                            clearPendingAction();
                        }
                    }
                });
        prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        pickProfileImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri imageUri = result.getData().getData();
                if (imageUri != null) {
                    prefs.edit().putString(KEY_PROFILE_IMAGE_PATH, imageUri.toString()).apply();
                    if (dialogProfileImageView != null) {
                        dialogProfileImageView.setImageURI(imageUri);
                    }
                }
            }
        });

        changeIconLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                Uri imageUri = result.getData().getData();
                if (itemPendingIconChange != null) {
                    saveIconAndUpdateDb(imageUri, itemPendingIconChange);
                }
            }
            itemPendingIconChange = null;
        });

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                launchGalleryForProfile();
            } else {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Permission denied. Cannot select image.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        if (getArguments() != null) {
            pageIndex = getArguments().getInt(ARG_PAGE_INDEX);
        }
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        appItemDao = db.appItemDao();
        databaseWriteExecutor = Executors.newSingleThreadExecutor();
        appWidgetManager = AppWidgetManager.getInstance(requireContext());
        appWidgetHost = new AppWidgetHost(requireContext(), 1); // Use your host ID

        appWidgetHost = listener.getAppWidgetHost();
        viewModel = new ViewModelProvider(this).get(AppItemViewModel.class);
        setupIconSizeChangeReceiver();
        taskbarViewModel = new ViewModelProvider(this).get(TaskbarViewModel.class);
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        appItemViewModel = new ViewModelProvider(requireActivity()).get(AppItemViewModel.class);

        showDialogOnFirstRun();
    }

    private void initializeGridSize() {
        if (getContext() != null) {
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                ROWS = ROWS_LANDSCAPE;
                COLS = COLS_LANDSCAPE;
            } else {
                ROWS = ROWS_PORTRAIT;
                COLS = COLS_PORTRAIT;
            }
            // Initialize occupiedCells array with the correct size
            occupiedCells = new AppItem[ROWS][COLS];
        }
    }

    private void executePendingAction() {
        if (pendingFragment != null && pendingTitle != null) {
            loadFragmentInternal(pendingFragment, pendingTitle);
        }
        clearPendingAction();
    }

    private void clearPendingAction() {
        this.pendingFragment = null;
        this.pendingTitle = null;
    }

    private void showDialogOnFirstRun() {
        final SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean hasPromptBeenShown = prefs.getBoolean(KEY_FIRST_RUN_PROMPT_SHOWN, false);

        if (hasPromptBeenShown) {
            checkAndShowProfileDialog();
            return;
        }

        final Dialog dialog = new Dialog(getActivity());
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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_screen, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootLayout = view.findViewById(R.id.root_layout);
        rootLayout.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onHomeScreenLongPressed();
            }
            return true;
        });

        // Re-initialize grid size in case orientation changed
        initializeGridSize();

        rootLayout.post(() -> {
            if (!isAdded()) return;
            int orientation = getResources().getConfiguration().orientation;
            int gridHeight;
            if (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                // Landscape: reserve actual taskbar height plus minimal safe area
                int taskbarHeight = 0;
                View taskbar = requireActivity().findViewById(R.id.lnrTaskBar);
                if (taskbar != null) {
                    taskbarHeight = taskbar.getHeight();
                    if (taskbarHeight == 0) {
                        taskbar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                        taskbarHeight = taskbar.getMeasuredHeight();
                    }
                }
                // Use only taskbar height with no extra padding for maximum space usage
                int reservedBottom = taskbarHeight;
                gridHeight = rootLayout.getHeight() - reservedBottom;
            } else {
                // Portrait: keep existing behavior exactly
                int bottomMarginPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, getResources().getDisplayMetrics());
                gridHeight = rootLayout.getHeight() - bottomMarginPx;
            }
            cellWidth = rootLayout.getWidth() / COLS;
            cellHeight = (gridHeight > 0 ? gridHeight : 0) / ROWS;
            observeViewModels();
        });
    }

    public int getCellWidth() {
        return cellWidth;
    }

    public int getCellHeight() {
        return cellHeight;
    }

    private void checkAndShowProfileDialog() {
        boolean profileSetupComplete = prefs.getBoolean(KEY_PROFILE_SETUP_COMPLETE, false);
        if (profileSetupComplete) {
            return;
        }
        if (!isAdded()) {
            Log.w(TAG, "checkAndShowProfileDialog: Fragment not added, cannot show profile dialog.");
            return;
        }
        Context context = requireContext();
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_set_profile, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView).setCancelable(false);
        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialogProfileImageView = dialogView.findViewById(R.id.iv_profile_image);
        EditText etProfileName = dialogView.findViewById(R.id.et_profile_name);
        Button btnSaveProfile = dialogView.findViewById(R.id.btn_save_profile);

        String savedName = prefs.getString(KEY_PROFILE_NAME, "");
        etProfileName.setText(savedName);
        String savedImagePath = prefs.getString(KEY_PROFILE_IMAGE_PATH, null);
        if (savedImagePath != null) {
            try {
                Uri imageUri = Uri.parse(savedImagePath);
                dialogProfileImageView.setImageURI(imageUri);
            } catch (Exception e) {
                dialogProfileImageView.setImageResource(R.drawable.ic_user);
            }
        }
        dialogProfileImageView.setOnClickListener(v -> handleImagePick());
        btnSaveProfile.setOnClickListener(v -> {
            String name = etProfileName.getText().toString().trim();
            if (name.isEmpty()) {
                etProfileName.setError("Name is required");
                return;
            }
            prefs.edit()
                    .putString(KEY_PROFILE_NAME, name)
                    .putBoolean(KEY_PROFILE_SETUP_COMPLETE, true)
                    .apply();
            Toast.makeText(context, "Profile Saved!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        dialog.show();
    }

    private void handleImagePick() {
        if (!isAdded()) {
            Log.w(TAG, "handleImagePick: Fragment not added, skipping image pick.");
            return;
        }

        String permission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            launchGalleryForProfile();
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    private void launchGalleryForProfile() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            pickProfileImageLauncher.launch(intent);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                pickProfileImageLauncher.launch(intent);
            } else {
                Toast.makeText(requireContext(), "No app found to pick an image.", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "No activity found to handle ACTION_PICK or ACTION_GET_CONTENT for images.");
            }
        }
    }

    private void observeViewModels() {
        appItemViewModel.getItemsForPage(pageIndex).observe(getViewLifecycleOwner(), items -> {
            if (items != null && isAdded()) {
                syncLayoutWithDb(items);
                if (items.isEmpty() && listener != null) {
                    listener.onPageEmpty(pageIndex);
                }
            }
        });
        settingsViewModel.getIconSize().observe(getViewLifecycleOwner(), newSize -> {
            if (isAdded()) {
                forceRefreshLayoutFromDb();
            }
        });
        settingsViewModel.getDesktopTextColor().observe(getViewLifecycleOwner(), newColor -> {
            if (isAdded()) {
                forceRefreshLayoutFromDb();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getContext() != null && !isReceiverRegistered) {
            IntentFilter filter = new IntentFilter(LauncherActivity.ACTION_ICON_SIZE_CHANGED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireContext().registerReceiver(iconSizeChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                ContextCompat.registerReceiver(requireContext(), iconSizeChangeReceiver, filter, ContextCompat.RECEIVER_EXPORTED);
            }
            isReceiverRegistered = true;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getContext() != null && isReceiverRegistered) {
            getContext().unregisterReceiver(iconSizeChangeReceiver);
            isReceiverRegistered = false;
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Save current page and scroll position
        int currentPage = pageIndex;
        ViewPager viewPager = getActivity() != null ? getActivity().findViewById(R.id.viewPager) : null;

        // Initialize grid size on the main thread
        initializeGridSize();

        // Handle widget updates on a background thread
        databaseWriteExecutor.execute(() -> {
            try {
                // Get all widgets
                List<AppItem> allWidgets = appItemDao.getWidgetsSync();

                // Store widget states
                for (AppItem widget : allWidgets) {
                    if (widget.getType() == AppItem.Type.WIDGET) {
                        // Store original dimensions if not set
                        if (widget.getOriginalWidth() <= 0 || widget.getOriginalHeight() <= 0) {
                            widget.setOriginalDimensions(widget.getColSpan(), widget.getRowSpan());
                            appItemDao.update(widget);
                        }
                    }
                }

                // Update UI on main thread
                homeScreenHandler.post(() -> {
                    if (!isAdded() || getActivity() == null) return;

                    // Restore the original page
                    if (viewPager != null && currentPage < viewPager.getAdapter().getCount()) {
                        viewPager.setCurrentItem(currentPage, false);
                    }

                    // Update all widgets to ensure proper sizing
                    updateWidgetsOnUiThread();

                    // Force a layout update
                    if (rootLayout != null) {
                        rootLayout.post(() -> {
                            rootLayout.requestLayout();
                            rootLayout.invalidate();
                        });
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error in onConfigurationChanged", e);
            }
        });
    }

    private void updateWidgetsOnUiThread() {
        if (!isAdded() || appItemDao == null || rootLayout == null) return;

        databaseWriteExecutor.execute(() -> {
            try {
                List<AppItem> widgets = appItemDao.getWidgetsSync();
                homeScreenHandler.post(() -> {
                    if (!isAdded() || rootLayout == null) return;

                    // Remove existing widget views
                    List<View> viewsToRemove = new ArrayList<>();
                    for (int i = 0; i < rootLayout.getChildCount(); i++) {
                        View child = rootLayout.getChildAt(i);
                        if (child.getTag() instanceof AppItem) {
                            AppItem item = (AppItem) child.getTag();
                            if (item.getType() == AppItem.Type.WIDGET) {
                                viewsToRemove.add(child);
                            }
                        }
                    }

                    for (View view : viewsToRemove) {
                        rootLayout.removeView(view);
                    }

                    // Add all widgets back with correct dimensions
                    for (AppItem widget : widgets) {
                        if (widget.getType() == AppItem.Type.WIDGET && widget.getPage() == pageIndex) {
                            AppWidgetProviderInfo appWidgetInfo = appWidgetManager.getAppWidgetInfo(widget.getAppWidgetId());
                            if (appWidgetInfo != null) {
                                // Remove existing view if any
                                if (widget.getView() != null) {
                                    View oldView = widget.getView();
                                    if (oldView.getParent() != null) {
                                        ((ViewGroup) oldView.getParent()).removeView(oldView);
                                    }
                                }

                                // Create new widget view
                                addWidgetToGrid(appWidgetInfo, widget);
                            }
                        }
                    }

                    // Force layout update
                    rootLayout.post(() -> {
                        rootLayout.requestLayout();
                        rootLayout.invalidate();
                    });
                });
            } catch (Exception e) {
                Log.e(TAG, "Error updating widgets", e);
            }
        });
    }

    private void updateWidgetLayout(AppItem widget) {
        if (widget.getType() != AppItem.Type.WIDGET || widget.getView() == null) {
            return;
        }

        View view = widget.getView();
        ViewGroup.LayoutParams params = view.getLayoutParams();

        if (params == null) {
            return;
        }

        // Calculate new dimensions based on cell size and original spans
        int newWidth = cellWidth * widget.getOriginalWidth();
        int newHeight = cellHeight * widget.getOriginalHeight();

        // Ensure widget doesn't exceed screen bounds
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int maxWidgetWidth = displayMetrics.widthPixels - (int) (16 * displayMetrics.density);
        int maxWidgetHeight = displayMetrics.heightPixels - (int) (16 * displayMetrics.density);

        newWidth = Math.min(newWidth, maxWidgetWidth);
        newHeight = Math.min(newHeight, maxWidgetHeight);

        // Update widget spans based on available space
        widget.setColSpan((int) Math.ceil((float) newWidth / cellWidth));
        widget.setRowSpan((int) Math.ceil((float) newHeight / cellHeight));

        // Update layout parameters
        params.width = newWidth;
        params.height = newHeight;

        if (params instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams frameParams = (FrameLayout.LayoutParams) params;
            frameParams.leftMargin = widget.getCol() * cellWidth;
            frameParams.topMargin = widget.getRow() * cellHeight;
            view.setLayoutParams(frameParams);
        } else {
            view.setLayoutParams(params);
        }

        // Request layout to apply changes
        view.requestLayout();
    }

    // Move overflow items from next pages back to the current page until it reaches portrait capacity (24)
    private void rebalancePagesForPortrait() {
        databaseWriteExecutor.execute(() -> {
            try {
                final int capacity = ROWS_PORTRAIT * COLS_PORTRAIT;
                List<Integer> pageIds = appItemDao.getAllPageIds();
                if (pageIds == null || pageIds.isEmpty()) return;
                for (int i = 0; i < pageIds.size(); i++) {
                    int pageId = pageIds.get(i);
                    List<AppItem> current = appItemDao.getItemsForPageNow(pageId);
                    if (current == null) continue;
                    int count = current.size();
                    if (count >= capacity) continue;
                    // Pull from subsequent pages until full or no more items
                    for (int j = i + 1; j < pageIds.size() && count < capacity; j++) {
                        int fromPage = pageIds.get(j);
                        List<AppItem> fromItems = appItemDao.getItemsForPageNow(fromPage);
                        if (fromItems == null || fromItems.isEmpty()) continue;
                        // Sort by row, col to maintain order
                        fromItems.sort((a, b) -> {
                            if (a.getRow() != b.getRow())
                                return Integer.compare(a.getRow(), b.getRow());
                            return Integer.compare(a.getCol(), b.getCol());
                        });
                        for (AppItem it : new ArrayList<>(fromItems)) {
                            if (count >= capacity) break;
                            int[] free = appItemDao.findFirstFreeCellOnPageNow(pageId, 1, 1, ROWS_PORTRAIT, COLS_PORTRAIT);
                            if (free == null) break;
                            it.page = pageId;
                            it.setPosition(free[0], free[1]);
                            it.setRowSpan(1);
                            it.setColSpan(1);
                            appItemDao.update(it);
                            count++;
                        }
                    }
                    // After pulling, compact the page row-major to remove gaps
                    compactPageRowMajor(pageId, ROWS_PORTRAIT, COLS_PORTRAIT);
                }
                // Ensure UI refresh after DB updates complete
                if (homeScreenHandler != null) {
                    homeScreenHandler.post(this::forceRefreshLayoutFromDb);
                }
            } catch (Exception ignored) {
            }
        });
    }

    // Compact items on a page in row-major order within the given grid size.
    private void compactPageRowMajor(int pageId, int rows, int cols) {
        List<AppItem> items = appItemDao.getItemsForPageNow(pageId);
        if (items == null || items.isEmpty()) return;
        // Keep special items before apps for consistency
        items.sort((a, b) -> {
            int pa = typePriority(a);
            int pb = typePriority(b);
            if (pa != pb) return Integer.compare(pa, pb);
            if (a.getRow() != b.getRow()) return Integer.compare(a.getRow(), b.getRow());
            return Integer.compare(a.getCol(), b.getCol());
        });
        boolean[][] occupied = new boolean[rows][cols];
        for (AppItem it : items) {
            // Find first free cell
            int[] pos = null;
            outer:
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (!occupied[r][c]) {
                        pos = new int[]{r, c};
                        break outer;
                    }
                }
            }
            if (pos == null) break;
            it.setPosition(pos[0], pos[1]);
            it.setRowSpan(1);
            it.setColSpan(1);
            appItemDao.update(it);
            occupied[pos[0]][pos[1]] = true;
        }
    }

    private void setupIconSizeChangeReceiver() {
        iconSizeChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null && LauncherActivity.ACTION_ICON_SIZE_CHANGED.equals(intent.getAction())) {
                    if (isAdded()) {
                        // Immediate refresh for better responsiveness
                        homeScreenHandler.post(() -> {
                            if (isAdded()) {
                                forceRefreshLayoutFromDb();
                            }
                        });
                    }
                }
            }
        };
    }

    public void forceRefreshLayoutFromDb() {
        databaseWriteExecutor.execute(() -> {
            List<AppItem> currentItems = appItemDao.getItemsForPageNow(pageIndex);
            homeScreenHandler.post(() -> {
                if (isAdded()) {
                    syncLayoutWithDb(currentItems);

                    // Additional refresh for Android 15+ compatibility
                    if (android.os.Build.VERSION.SDK_INT >= 35) {
                        // Force view invalidation and layout
                        if (rootLayout != null) {
                            rootLayout.invalidate();
                            rootLayout.requestLayout();
                        }

                        // Force a delayed refresh to ensure changes are applied
                        homeScreenHandler.postDelayed(() -> {
                            if (isAdded() && rootLayout != null) {
                                rootLayout.invalidate();
                                rootLayout.requestLayout();
                            }
                        }, 50);
                    }
                }
            });
        });
    }

    private void syncLayoutWithDb(List<AppItem> itemsFromDb) {
        if (!isAdded() || rootLayout == null || cellWidth == 0) return;
        rootLayout.removeAllViews();
        initializeGrid();

        // Filter out placeholder items used for page persistence
        List<AppItem> filteredItems = itemsFromDb.stream()
                .filter(item -> !("__PAGE_MARKER__".equals(item.getName()) && item.isSpecialItem))
                .collect(Collectors.toList());

        // Before laying out, normalize all items for the current grid (handle landscape/portrait switch)
        List<AppItem> normalizedItems = normalizeItemsForCurrentGrid(filteredItems);
        // In landscape, compact items to fill the grid row-major and move overflow to next page
        if (getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            normalizedItems = compactItemsForLandscape(normalizedItems);
        }
        PackageManager pm = requireContext().getPackageManager();
        normalizedItems.sort((a, b) -> {
            if (a.getType() != AppItem.Type.APP && b.getType() == AppItem.Type.APP) return -1;
            if (a.getType() == AppItem.Type.APP && b.getType() != AppItem.Type.APP) return 1;
            if (a.getCol() != b.getCol()) return Integer.compare(a.getCol(), b.getCol());
            return Integer.compare(a.getRow(), b.getRow());
        });
        for (AppItem dbItem : normalizedItems) {
            dbItem.name = dbItem.originalName;
            if (dbItem.type == AppItem.Type.APP) {
                Intent intent = pm.getLaunchIntentForPackage(dbItem.packageName);
                if (intent != null) {
                    ResolveInfo resolveInfo = pm.resolveActivity(intent, 0);
                    if (resolveInfo != null) {
                        dbItem.setActivityInfo(resolveInfo.activityInfo);
                        if (dbItem.name == null) {
                            dbItem.name = resolveInfo.loadLabel(pm).toString();
                        }
                        addAppIconToGrid(dbItem);
                        markCellsOccupied(dbItem, dbItem.getRow(), dbItem.getCol(), dbItem.getRowSpan(), dbItem.getColSpan());
                    } else {
                        Log.w(TAG, "App '" + dbItem.packageName + "' is invalid (uninstalled) and will be removed from DB.");
                        removeItem(dbItem);
                    }
                } else {
                    Log.w(TAG, "App '" + dbItem.packageName + "' is invalid (uninstalled) and will be removed from DB.");
                    removeItem(dbItem);
                }
            } else if (dbItem.type == AppItem.Type.WIDGET) {
                AppWidgetProviderInfo appWidgetInfo = appWidgetManager.getAppWidgetInfo(dbItem.getAppWidgetId());
                if (appWidgetInfo != null) {
                    if (dbItem.name == null) {
                        dbItem.name = appWidgetInfo.loadLabel(pm);
                    }
                    addWidgetToGrid(appWidgetInfo, dbItem);
                    markCellsOccupied(dbItem, dbItem.getRow(), dbItem.getCol(), dbItem.getRowSpan(), dbItem.getColSpan());
                } else {
                    Log.w(TAG, "Widget with ID " + dbItem.getAppWidgetId() + " could not be loaded. Removing from DB.");
                    removeItem(dbItem);
                }
            } else {
                addAppIconToGrid(dbItem);
                markCellsOccupied(dbItem, dbItem.getRow(), dbItem.getCol(), dbItem.getRowSpan(), dbItem.getColSpan());
            }
        }
    }

    // Ensure items are within bounds for current ROWS/COLS and resolve collisions.
    private List<AppItem> normalizeItemsForCurrentGrid(List<AppItem> itemsFromDb) {
        if (itemsFromDb == null) return new ArrayList<>();
        // Work on a copy to avoid mutating the input list order elsewhere
        List<AppItem> items = new ArrayList<>(itemsFromDb);

        // Temporary occupancy grid for collision detection while normalizing
        AppItem[][] tempOccupied = new AppItem[ROWS][COLS];

        // Sort items by column first, then row (column-major order)
        items.sort((a, b) -> {
            // First sort by type priority (folders first, then apps, then widgets)
            int typeCompare = Integer.compare(typePriority(a), typePriority(b));
            if (typeCompare != 0) return typeCompare;

            // For same type, sort by column first, then row (column-major order)
            if (a.getCol() != b.getCol()) {
                return Integer.compare(a.getCol(), b.getCol());
            }
            return Integer.compare(a.getRow(), b.getRow());
        });

        for (AppItem item : items) {
            // Clamp spans to grid
            int rowSpan = Math.max(1, Math.min(item.getRowSpan(), ROWS));
            int colSpan = Math.max(1, Math.min(item.getColSpan(), COLS));

            // Clamp position to fit within grid
            int newRow = Math.max(0, Math.min(item.getRow(), ROWS - rowSpan));
            int newCol = Math.max(0, Math.min(item.getCol(), COLS - colSpan));

            // If region is occupied, find first free region
            if (!isRegionFree(tempOccupied, newRow, newCol, rowSpan, colSpan)) {
                int[] pos = findFirstFreeRegion(tempOccupied, rowSpan, colSpan);
                if (pos != null) {
                    newRow = pos[0];
                    newCol = pos[1];
                } else {
                    // If widget cannot fit on this page, move it to next page; otherwise skip placing
                    if (item.getType() == AppItem.Type.WIDGET) {
                        AppItem widgetItem = item; // capture effectively final
                        moveWidgetToNextPage(widgetItem);
                    }
                    // Skip adding to this page
                    continue;
                }
            }

            // Mark occupied in temp grid
            for (int r = newRow; r < newRow + rowSpan; r++) {
                for (int c = newCol; c < newCol + colSpan; c++) {
                    tempOccupied[r][c] = item;
                }
            }

            // Persist only in PORTRAIT. In LANDSCAPE, adjust in-memory for rendering, do not update DB.
            boolean isLandscape = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
            if (newRow != item.getRow() || newCol != item.getCol() || rowSpan != item.getRowSpan() || colSpan != item.getColSpan()) {
                item.setPosition(newRow, newCol);
                item.setRowSpan(rowSpan);
                item.setColSpan(colSpan);
                if (!isLandscape) {
                    dbUpdate(item);
                }
            }
        }

        return items;
    }

    /**
     * Determines the priority of an {@link AppItem} based on its type.
     * Lower numbers indicate higher priority.
     *
     * @param item The AppItem whose priority is to be determined.
     * @return An integer representing the priority, where 0 is the highest priority.
     */
    private int typePriority(AppItem item) {
        // Lower number means higher priority
        switch (item.getType()) {
            case SETTINGS:
            case WALLPAPER_ACTION:
            case FILE_MANAGER_ACTION:
            case USER:
            case SOCIAL_MEDIA_FOLDER:
            case GOOGLE_FOLDER:
                return 0;
            case APP:
                return 1;
            case WIDGET:
            default:
                return 2;
        }
    }

    private boolean isRegionFree(AppItem[][] grid, int startRow, int startCol, int rowSpan, int colSpan) {
        for (int r = startRow; r < startRow + rowSpan; r++) {
            for (int c = startCol; c < startCol + colSpan; c++) {
                if (r < 0 || r >= ROWS || c < 0 || c >= COLS) return false;
                if (grid[r][c] != null) return false;
            }
        }
        return true;
    }

    /**
     * Finds the first free region in the grid that can fit the specified dimensions.
     * Implements column-first search (top to bottom, left to right).
     *
     * @param grid    The grid to search in
     * @param rowSpan Number of rows the region should span
     * @param colSpan Number of columns the region should span
     * @return int array with [row, col] of the top-left corner of the free region, or null if no space found
     */
    private int[] findFirstFreeRegion(AppItem[][] grid, int rowSpan, int colSpan) {
        // Search column by column (left to right)
        for (int c = 0; c <= COLS - colSpan; c++) {
            // Then search top to bottom within each column
            for (int r = 0; r <= ROWS - rowSpan; r++) {
                if (isRegionFree(grid, r, c, rowSpan, colSpan)) {
                    return new int[]{r, c};
                }
            }
        }
        return null;
    }

    private void addAppIconToGrid(AppItem appItem) {
        if (!isAdded() || getContext() == null) return;

        // Find the first available position in column-major order
        int[] pos = findFirstFreeCell();
        if (pos != null) {
            appItem.setPosition(pos[0], pos[1]);
        }

        LinearLayout itemContainer = new LinearLayout(requireContext());
        itemContainer.setOrientation(LinearLayout.VERTICAL);
        itemContainer.setGravity(Gravity.CENTER);

        ShapeableImageView iconView = new ShapeableImageView(requireContext());
        Drawable appIcon = null;
        boolean applyCustomStyling = false;

        if (appItem.customIconPath != null && !appItem.customIconPath.isEmpty()) {
            appIcon = Drawable.createFromPath(appItem.customIconPath);
            applyCustomStyling = true;
        }
        if (appIcon == null) {
            switch (appItem.getType()) {
                case WALLPAPER_ACTION:
                    appIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_wallpapers);
                    break;
                case FILE_MANAGER_ACTION:
                    appIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_file);
                    break;
                case USER:
                    appIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_user);
                    break;
                case SOCIAL_MEDIA_FOLDER:
                    appIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_folder_social);
                    break;
                case GOOGLE_FOLDER:
                    appIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_folder_social);
                    break;
                case SETTINGS:
                    appIcon = ContextCompat.getDrawable(requireContext(), R.drawable.img_system_setting);
                    break;
                case APP:
                    applyCustomStyling = true;
                    try {
                        appIcon = requireContext().getPackageManager().getApplicationIcon(appItem.packageName);
                    } catch (Exception e) {
                        appIcon = ContextCompat.getDrawable(requireContext(), android.R.drawable.sym_def_app_icon);
                    }
                    break;
            }
        }

        // Compute icon size
        int currentSizePref = PreferenceHelper.getIconSize(requireContext());
        int orientation = getResources().getConfiguration().orientation;
        int iconSize;
        if (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            // Landscape: maximize icon size while leaving adequate space for 2-line labels
            int reservedLabelDp = 35; // Increased to prevent text cutoff with larger icons
            int reservedLabelPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, reservedLabelDp, getResources().getDisplayMetrics());
            int baseSize = Math.max(1, Math.min(cellWidth, Math.max(1, cellHeight - reservedLabelPx)));

            float factor;
            switch (currentSizePref) {
                case PreferenceHelper.ICON_SIZE_SMALL:
                    factor = 1.55f; // Increased significantly
                    break;
                case PreferenceHelper.ICON_SIZE_LARGE:
                    factor = 1.55f; // Much larger
                    break;
                default:
                    factor = 1.55f; // Noticeably larger
                    break;
            }
            iconSize = Math.max(1, (int) (baseSize * factor));
        } else {
            // Portrait: preserve previous behavior exactly (do not change approved look)
            switch (currentSizePref) {
                case PreferenceHelper.ICON_SIZE_SMALL:
                    iconSize = (int) (cellWidth * 0.50f);
                    break;
                case PreferenceHelper.ICON_SIZE_LARGE:
                    iconSize = (int) (cellWidth * 0.70f);
                    break;
                default:
                    iconSize = (int) (cellWidth * 0.63f);
                    break;
            }
        }
        LinearLayout.LayoutParams iconLayoutParams = new LinearLayout.LayoutParams(iconSize, iconSize);

        if (applyCustomStyling) {
            ShapeAppearanceModel shapeAppearanceModel = ShapeAppearanceModel
                    .builder(requireContext(), R.style.ShapeAppearanceOverlay_AppIcon, 0)
                    .build();
            iconView.setShapeAppearanceModel(shapeAppearanceModel);
            iconView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white));
            int elevation = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
            iconView.setElevation(elevation);
        }

        iconView.setLayoutParams(iconLayoutParams);
        iconView.setAdjustViewBounds(true);
        iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        float density = getResources().getDisplayMetrics().density;
        int paddingInPx = (int) (3 * density);
        iconView.setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx);

        Bitmap iconBitmap = createIconBitmap(appIcon, iconSize, iconSize);
        iconView.setImageBitmap(iconBitmap);

        TextView nameView = new TextView(requireContext());
        nameView.setText(appItem.getName());
        int desktopColor = prefs.getInt(GeneralFragment.KEY_DESKTOP_TEXT_COLOR, Color.WHITE);
        nameView.setTextColor(desktopColor);
        float textSp = (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) ? 9.5f : 12f;
        nameView.setTextSize(textSp);
        nameView.setShadowLayer(2f, 2f, 2f, Color.parseColor("#55000000"));
        nameView.setGravity(Gravity.CENTER);
        nameView.setMaxLines(2);
        nameView.setEllipsize(TextUtils.TruncateAt.END);
        nameView.setSelected(true);

        // Set line spacing for better text readability in landscape with 2 lines
        if (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            nameView.setLineSpacing(1.0f, 1.0f);
        }

        LinearLayout.LayoutParams nameParams;
        if (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            // Set max width to cell width to prevent overflow
            nameParams = new LinearLayout.LayoutParams(cellWidth - (int) (8 * density), ViewGroup.LayoutParams.WRAP_CONTENT);
        } else {
            nameParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        int nameTopMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, getResources().getDisplayMetrics());
        nameParams.topMargin = nameTopMargin;
        nameView.setLayoutParams(nameParams);

        itemContainer.addView(iconView);
        itemContainer.addView(nameView);
        itemContainer.setTag(appItem); // Tag the container with the AppItem

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(cellWidth, cellHeight);
        params.leftMargin = appItem.getCol() * cellWidth;
        params.topMargin = appItem.getRow() * cellHeight;

        // Add vertical margin between cells in landscape mode for better spacing
        if (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            int verticalMarginDp = 8; // Add spacing between rows
            int verticalMarginPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, verticalMarginDp, getResources().getDisplayMetrics());
            params.topMargin = appItem.getRow() * cellHeight + (appItem.getRow() * verticalMarginPx);
        }

        itemContainer.setLayoutParams(params);
        itemContainer.setOnTouchListener(new AppIconTouchListener(itemContainer, appItem));
        appItem.setView(itemContainer);

        rootLayout.addView(itemContainer);
    }

    private Bitmap createIconBitmap(Drawable drawable, int width, int height) {
        if (drawable == null) {
            drawable = ContextCompat.getDrawable(requireContext(), android.R.drawable.sym_def_app_icon);
        }
        if (drawable == null) { // Fallback if even default icon is null
            return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable adaptiveIcon = (AdaptiveIconDrawable) drawable;
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            int scaledSize = (int) (width / ADAPTIVE_ICON_SCALE_FACTOR);
            int offset = (width - scaledSize) / 2;

            Drawable backgroundDrawable = adaptiveIcon.getBackground();
            if (backgroundDrawable != null) {
                backgroundDrawable.setBounds(0, 0, width, height);
                backgroundDrawable.draw(canvas);
            }
            Drawable foregroundDrawable = adaptiveIcon.getForeground();
            if (foregroundDrawable != null) {
                foregroundDrawable.setBounds(offset, offset, scaledSize + offset, scaledSize + offset);
                foregroundDrawable.draw(canvas);
            }
            return bitmap;
        } else {
            if (drawable instanceof BitmapDrawable) {
                return ((BitmapDrawable) drawable).getBitmap();
            }
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, width, height);
            drawable.draw(canvas);
            return bitmap;
        }
    }

    private void addWidgetToGrid(AppWidgetProviderInfo appWidgetInfo, AppItem widgetItem) {
        if (!isAdded() || getContext() == null) return;

        // Check if appWidgetHost is null and get it from listener if needed
        if (appWidgetHost == null) {
            if (listener != null) {
                appWidgetHost = listener.getAppWidgetHost();
            }
            if (appWidgetHost == null) {
                Log.e(TAG, "AppWidgetHost is null, cannot create widget view");
                return;
            }
        }

        // Check if widget fits on current page, if not, move to next page
        if (!canWidgetFitOnCurrentPage(widgetItem)) {
            moveWidgetToNextPage(widgetItem);
            return;
        }

        final Context appContext = requireContext().getApplicationContext();
        final AppWidgetHostView hostView = appWidgetHost.createView(appContext, widgetItem.getAppWidgetId(), appWidgetInfo);
        hostView.setAppWidget(widgetItem.getAppWidgetId(), appWidgetInfo);

        // Store original widget dimensions if not set
        if (widgetItem.getOriginalWidth() == 0 || widgetItem.getOriginalHeight() == 0) {
            int originalWidth = widgetItem.getColSpan();
            int originalHeight = widgetItem.getRowSpan();
            widgetItem.setOriginalDimensions(originalWidth, originalHeight);
        }

        hostView.post(() -> {
            if (!isAdded()) return;

            // Use original dimensions for consistency across orientation changes
            int widgetWidthPx = cellWidth * widgetItem.getOriginalWidth();
            int widgetHeightPx = cellHeight * widgetItem.getOriginalHeight();

            // Ensure widget doesn't exceed screen bounds in current orientation
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            int maxWidgetWidth = displayMetrics.widthPixels - (int) (16 * displayMetrics.density);
            int maxWidgetHeight = displayMetrics.heightPixels - (int) (16 * displayMetrics.density);

            widgetWidthPx = Math.min(widgetWidthPx, maxWidgetWidth);
            widgetHeightPx = Math.min(widgetHeightPx, maxWidgetHeight);

            // Update widget item's span based on available space
            widgetItem.setColSpan((int) Math.ceil((float) widgetWidthPx / cellWidth));
            widgetItem.setRowSpan((int) Math.ceil((float) widgetHeightPx / cellHeight));

            // Apply padding and calculate dp values
            int paddingPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, displayMetrics);
            float density = displayMetrics.density;

            int minWidthDp = (int) ((widgetWidthPx - (2 * paddingPx)) / density);
            int minHeightDp = (int) ((widgetHeightPx - (2 * paddingPx)) / density);

            // Set widget options with calculated dimensions
            Bundle options = new Bundle();
            options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, minWidthDp);
            options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, minHeightDp);
            options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minWidthDp);
            options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHeightDp);

            // Apply the widget options
            appWidgetManager.updateAppWidgetOptions(widgetItem.getAppWidgetId(), options);

            // Update the widget layout parameters
            ViewGroup.LayoutParams params = hostView.getLayoutParams();
            if (params == null) {
                params = new ViewGroup.LayoutParams(widgetWidthPx, widgetHeightPx);
            } else {
                params.width = widgetWidthPx;
                params.height = widgetHeightPx;
            }
            hostView.setLayoutParams(params);
        });
        FrameLayout widgetContainer = new FrameLayout(requireContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(cellWidth * widgetItem.getColSpan(), cellHeight * widgetItem.getRowSpan());
        params.leftMargin = widgetItem.getCol() * cellWidth;
        params.topMargin = widgetItem.getRow() * cellHeight;
        widgetContainer.setLayoutParams(params);
        hostView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        widgetContainer.addView(hostView);
        // The touchOverlay is crucial for intercepting touches for drag-and-drop
        View touchOverlay = new View(requireContext());
        touchOverlay.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        widgetContainer.addView(touchOverlay);
        widgetItem.setView(widgetContainer);
        widgetContainer.setTag(widgetItem); // Tag the container with the AppItem
//        touchOverlay.setOnTouchListener(new WidgetTouchListener(widgetContainer, widgetItem, hostView));

        touchOverlay.setOnTouchListener(new WidgetTouchListener(widgetContainer, widgetItem, hostView, touchOverlay));
        rootLayout.addView(widgetContainer);
    }

    private boolean canWidgetFitOnCurrentPage(AppItem widgetItem) {
        int requiredRowSpan = widgetItem.getRowSpan();
        int requiredColSpan = widgetItem.getColSpan();

        // Check if widget dimensions exceed grid bounds
        if (requiredRowSpan > ROWS || requiredColSpan > COLS) {
            return false;
        }

        // Check if there's enough space for the widget
        for (int r = 0; r <= ROWS - requiredRowSpan; r++) {
            for (int c = 0; c <= COLS - requiredColSpan; c++) {
                boolean canFit = true;
                for (int dr = 0; dr < requiredRowSpan && canFit; dr++) {
                    for (int dc = 0; dc < requiredColSpan && canFit; dc++) {
                        if (occupiedCells[r + dr][c + dc] != null) {
                            canFit = false;
                            break;
                        }
                    }
                }
                if (canFit) {
                    return true;
                }
            }
        }
        return false;
    }

    private void moveWidgetToNextPage(AppItem widgetItem) {
        if (listener != null && listener.getPagerAdapter() != null) {
            // Find next available page or create new one
            List<Integer> currentPageIds = new ArrayList<>();
            databaseWriteExecutor.execute(() -> {
                List<AppItem> allItems = appItemDao.getAllItemsNow();
                if (allItems != null) {
                    currentPageIds.addAll(allItems.stream()
                            .map(item -> item.page)
                            .distinct()
                            .sorted()
                            .collect(Collectors.toList()));
                }

                int nextPageId = currentPageIds.isEmpty() ? 0 : currentPageIds.get(currentPageIds.size() - 1) + 1;

                // Try to find space on existing pages first
                for (int pageId : currentPageIds) {
                    int[] pos = appItemDao.findFirstFreeCellOnPageNow(pageId, widgetItem.getRowSpan(),
                            widgetItem.getColSpan(), HomeScreenFragment.ROWS, HomeScreenFragment.COLS);
                    if (pos != null) {
                        widgetItem.page = pageId;
                        widgetItem.setPosition(pos[0], pos[1]);
                        appItemDao.update(widgetItem);

                        homeScreenHandler.post(() -> {
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Widget moved to page " + (pageId + 1), Toast.LENGTH_SHORT).show();
                                forceRefreshLayoutFromDb();
                            }
                        });
                        return;
                    }
                }

                // Create new page if no space found
                widgetItem.page = nextPageId;
                widgetItem.setPosition(0, 0);
                appItemDao.update(widgetItem);

                homeScreenHandler.post(() -> {
                    if (isAdded()) {
                        listener.getPagerAdapter().addPage(nextPageId);
                        Toast.makeText(getContext(), "Widget moved to new page " + (nextPageId + 1), Toast.LENGTH_SHORT).show();
                        forceRefreshLayoutFromDb();
                    }
                });
            });
        }
    }

    private void handleSpecialItemClick(AppItem item) {
        if (!isAdded()) {
            Log.w(TAG, "handleSpecialItemClick: Fragment not added, skipping click.");
            return;
        }

        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        switch (item.getType()) {
            case WALLPAPER_ACTION:
                Intent intent = new Intent(getActivity(), WallpaperActivity.class);
                startActivity(intent);
                break;
            case FILE_MANAGER_ACTION:
                if (hasStoragePermission()) {
                    HomeFileFragment homeFileFragment = new HomeFileFragment();
                    transaction.setCustomAnimations(R.anim.slide_up, R.anim.fade_out, R.anim.fade_in, R.anim.slide_down);
                    transaction.replace(R.id.fragment_container, homeFileFragment);
                    transaction.addToBackStack(null);
                    transaction.commit();
                } else {
                    requestStoragePermission(new HomeFileFragment(), "HomeFileFragment");
                }
                break;
            case USER:
                if (hasStoragePermission()) {
                    HomeFileFragment homeFileFragment = new HomeFileFragment();
                    transaction.setCustomAnimations(R.anim.slide_up, R.anim.fade_out, R.anim.fade_in, R.anim.slide_down);
                    transaction.replace(R.id.fragment_container, homeFileFragment);
                    transaction.addToBackStack(null);
                    transaction.commit();
                } else {
                    requestStoragePermission(new HomeFileFragment(), "HomeFileFragment");
                }
                break;
            case SOCIAL_MEDIA_FOLDER:
                showAppsDialog(getAppsForPackages(SOCIAL_MEDIA_PACKAGES), "Social Media");
                return;
            case SETTINGS:
                Intent intent1 = new Intent(android.provider.Settings.ACTION_SETTINGS);
                intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent1);
                return;
            case GOOGLE_FOLDER:
                showAppsDialog(getAppsForPackages(GOOGLE_PACKAGES), "Google Apps");
        }
    }

    private void requestStoragePermission(Fragment fragment, String title) {
        if (!isAdded()) {
            Log.w(TAG, "requestStoragePermission: Fragment not added, skipping permission request.");
            return;
        }
        this.pendingFragment = fragment;
        this.pendingTitle = title;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            showManageStorageRationale();
        } else {
            showLegacyStorageRationale();
        }
    }

    private boolean hasStoragePermission() {
        if (!isAdded()) {
            Log.w(TAG, "hasStoragePermission: Fragment not added, returning false.");
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void loadFragmentInternal(Fragment fragment, String title) {
        if (!isAdded()) return;
        // Use Activity's FragmentManager because the container (R.id.fragment_container) lives in the Activity layout
        FragmentManager fm = requireActivity().getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.fragment_container, fragment);
        ft.addToBackStack(title);
        ft.commit();
    }

    private List<ResolveInfo> getAppsForPackages(List<String> packages) {
        if (!isAdded()) {
            Log.w(TAG, "getAppsForPackages: Fragment not added, returning empty list.");
            return new ArrayList<>();
        }
        PackageManager pm = requireContext().getPackageManager();
        List<ResolveInfo> apps = new ArrayList<>();
        for (String pkg : packages) {
            try {
                Intent launchIntent = pm.getLaunchIntentForPackage(pkg);
                if (launchIntent != null) {
                    ResolveInfo ri = pm.resolveActivity(launchIntent, 0);
                    if (ri != null) {
                        apps.add(ri);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting app info for " + pkg, e);
            }
        }
        return apps;
    }

    private void showAppsDialog(List<ResolveInfo> apps, String title) {
        if (!isAdded()) {
            Log.w(TAG, "showAppsDialog: Fragment not added, cannot show apps dialog.");
            return;
        }
        if (apps == null || apps.isEmpty()) {
            Toast.makeText(requireContext(), "No apps found", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_app_grid, null);
        builder.setView(dialogView);
        TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);
        RecyclerView recyclerView = dialogView.findViewById(R.id.apps_recycler_view);
        dialogTitle.setText(title);

        final AlertDialog dialog = builder.create();
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        AppsGridAdapter adapter = new AppsGridAdapter(apps, requireContext().getPackageManager(), app -> {
            Intent launchIntent = requireContext().getPackageManager().getLaunchIntentForPackage(app.activityInfo.packageName);
            if (launchIntent != null) {
                startActivity(launchIntent);
            }
            dialog.dismiss();
        });
        recyclerView.setAdapter(adapter);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();
    }

    private void saveIconAndUpdateDb(Uri imageUri, AppItem itemToUpdate) {
        databaseWriteExecutor.execute(() -> {
            if (getContext() == null) return;
            try {
                InputStream inputStream = getContext().getContentResolver().openInputStream(imageUri);
                String packageName = itemToUpdate.packageName != null ? itemToUpdate.packageName : "item";
                String fileName = "icon_" + packageName + "_" + System.currentTimeMillis() + ".png";
                File destinationFile = new File(getContext().getFilesDir(), fileName);
                FileOutputStream outputStream = new FileOutputStream(destinationFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = inputStream.read(buf)) > 0) {
                    outputStream.write(buf, 0, len);
                }
                outputStream.close();
                inputStream.close();
                if (itemToUpdate.customIconPath != null) {
                    new File(itemToUpdate.customIconPath).delete();
                }
                itemToUpdate.customIconPath = destinationFile.getAbsolutePath();
                appItemDao.update(itemToUpdate);
                homeScreenHandler.post(this::forceRefreshLayoutFromDb);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save custom icon", e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to set new icon", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void showRemoveItemDialog(final AppItem item) {
        if (!isAdded()) {
            return;
        }

        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_remove_item);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(layoutParams);
        }

        TextView messageTextView = dialog.findViewById(R.id.dialog_message);
        Button removeButton = dialog.findViewById(R.id.button_remove);
        Button cancelButton = dialog.findViewById(R.id.button_cancel);

        messageTextView.setText("Remove '" + item.getName() + "' from the home screen?");

        removeButton.setOnClickListener(v -> {
            removeItem(item);
            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> {
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showAppOptionsMenu(View anchorView, AppItem appItem) {
        if (!isAdded()) {
            Log.w(TAG, "showAppOptionsMenu: Fragment not added, cannot show options menu.");
            return;
        }
        LayoutInflater inflater = (LayoutInflater) requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customMenuView = inflater.inflate(R.layout.popup_custom_menu, null);

        PopupWindow popupWindow = new PopupWindow(
                customMenuView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(20);

        // Store reference to current popup window
        if (currentPopupWindow != null && currentPopupWindow.isShowing()) {
            currentPopupWindow.dismiss();
        }
        currentPopupWindow = popupWindow;

        LinearLayout menuItemsContainer = customMenuView.findViewById(R.id.menu_items_container);

        addMenuItem(menuItemsContainer, R.drawable.ic_rename, "Rename", () -> {
            showRenameDialog(appItem);
            popupWindow.dismiss();
        });

        boolean isRegularApp = appItem.getType() == AppItem.Type.APP;
        if (isRegularApp) {
            addMenuItem(menuItemsContainer, R.drawable.ic_change_icon, "Change Icon", () -> {
                itemPendingIconChange = appItem;
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                changeIconLauncher.launch(intent);
                popupWindow.dismiss();
            });

            boolean hasCustomIcon = appItem.customIconPath != null && !appItem.customIconPath.isEmpty();
            if (hasCustomIcon) {
                addMenuItem(menuItemsContainer, R.drawable.ic_reset_icon, "Reset Icon", () -> {
                    if (appItem.customIconPath != null) {
                        new File(appItem.customIconPath).delete();
                    }
                    appItem.customIconPath = null;
                    dbUpdate(appItem);
                    popupWindow.dismiss();
                });
            }
        }

        if (isRegularApp) {
            String pinTitle = appItem.isPinned ? "Unpin from Taskbar" : "Pin to Taskbar";
            int pinIcon = appItem.isPinned ? R.drawable.ic_unpin : R.drawable.ic_pin;
            addMenuItem(menuItemsContainer, pinIcon, pinTitle, () -> {
                togglePinStatus(appItem);
                popupWindow.dismiss();
            });
        }

        if (isRegularApp) {
            addMenuItem(menuItemsContainer, R.drawable.ic_infos, "App Info", () -> {
                if (listener != null) listener.onAppInfoRequested(appItem.packageName);
                popupWindow.dismiss();
            });
        }

        if (isRegularApp) {
            addMenuItem(menuItemsContainer, R.drawable.ic_uninstall, "Uninstall", () -> {
                if (listener != null) listener.onUninstallRequested(appItem.packageName);
                popupWindow.dismiss();
            });
        }

        addMenuItem(menuItemsContainer, R.drawable.ic_remove, "Remove App", () -> {
            removeItem(appItem);
            popupWindow.dismiss();
        });

        customMenuView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );

        setPopupWindowPosition(anchorView, popupWindow, customMenuView);
    }

    private void setPopupWindowPosition(View anchorView, PopupWindow popupWindow, View contentView) {
        Rect displayFrame = new Rect();
        anchorView.getWindowVisibleDisplayFrame(displayFrame);
        int screenWidth = displayFrame.width();
        int screenHeight = displayFrame.height();

        int[] anchorLocation = new int[2];
        anchorView.getLocationOnScreen(anchorLocation);

        int popupWidth = contentView.getMeasuredWidth();
        int popupHeight = contentView.getMeasuredHeight();

        int xPos = anchorLocation[0] + anchorView.getWidth();
        int yPos = anchorLocation[1] - (popupHeight / 2) + (anchorView.getHeight() / 2);

        if (xPos + popupWidth > screenWidth) {
            xPos = anchorLocation[0] - popupWidth;
        }

        if (yPos + popupHeight > screenHeight) {
            yPos = screenHeight - popupHeight;
        }

        if (yPos < displayFrame.top) {
            yPos = displayFrame.top;
        }

        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, xPos, yPos);
    }

    private void dismissCurrentPopup() {
        if (currentPopupWindow != null && currentPopupWindow.isShowing()) {
            currentPopupWindow.dismiss();
            currentPopupWindow = null;
        }
    }

    private void addMenuItem(LinearLayout container, int iconResId, String title, Runnable action) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View itemView = inflater.inflate(R.layout.item_custom_menu, container, false);

        ImageView icon = itemView.findViewById(R.id.menu_item_icon);
        TextView titleView = itemView.findViewById(R.id.menu_item_title);

        icon.setImageResource(iconResId);
        titleView.setText(title);

        itemView.setOnClickListener(v -> action.run());

        container.addView(itemView);
    }

    private void togglePinStatus(AppItem appItem) {
        if (appItem == null || !isAdded()) return;
        boolean isCurrentlyPinned = appItem.isPinned;
        taskbarViewModel.togglePinStatus(appItem);
        if (isAdded()) {
            if (isCurrentlyPinned) {
                Toast.makeText(getContext(), "'" + appItem.getName() + "' unpinned from taskbar", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "'" + appItem.getName() + "' pinned to taskbar", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showRenameDialog(final AppItem appItem) {
        if (!isAdded()) {
            Log.w(TAG, "showRenameDialog: Fragment not added, cannot show rename dialog.");
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Rename App");
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(appItem.getName());
        input.selectAll();
        FrameLayout container = new FrameLayout(requireContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.leftMargin = margin;
        params.rightMargin = margin;
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                appItem.setName(newName);
                dbUpdate(appItem);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void removeItem(AppItem item) {
        if (item == null || !isAdded()) return;

        if (item.getType() == AppItem.Type.WIDGET) {
            appWidgetHost.deleteAppWidgetId(item.getAppWidgetId());
        }

        if (item.customIconPath != null) {
            new File(item.customIconPath).delete();
        }

        dbDelete(item);
        if (listener != null) {
            listener.onAppRemoved(item);
        }
    }

    private void dbUpdate(AppItem item) {
        databaseWriteExecutor.execute(() -> appItemDao.update(item));
    }

    private void dbDelete(AppItem item) {
        databaseWriteExecutor.execute(() -> appItemDao.delete(item));
    }

    private void initializeGrid() {
        // Re-initialize grid size in case orientation changed
        initializeGridSize();

        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                occupiedCells[i][j] = null;
            }
        }
    }

    private void markCellsOccupied(AppItem item, int startRow, int startCol, int rowSpan, int colSpan) {
        for (int r = startRow; r < startRow + rowSpan && r < ROWS; r++) {
            for (int c = startCol; c < startCol + colSpan && c < COLS; c++) {
                if (r < ROWS && c < COLS) occupiedCells[r][c] = item;
            }
        }
    }

    private void clearCellsFor(AppItem item) {
        if (item == null) return;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (occupiedCells[r][c] == item) occupiedCells[r][c] = null;
            }
        }
    }

    private int countEmptyCells() {
        int count = 0;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (occupiedCells[r][c] == null) count++;
            }
        }
        return count;
    }

    /**
     * Finds the first free cell in the grid.
     * Implements column-first search (top to bottom, left to right).
     *
     * @return int array with [row, col] of the first free cell, or null if no free cells
     */
    private int[] findFirstFreeCell() {
        // First find the first column with available space
        for (int c = 0; c < COLS; c++) {
            // Check if there's space in this column
            for (int r = 0; r < ROWS; r++) {
                if (occupiedCells[r][c] == null) {
                    // Found a free cell in this column
                    return new int[]{r, c};
                }
            }
            // If we get here, the column is full, move to next column
        }
        return null; // No free cells found
    }

    private void animateViewToCell(View view, int row, int col, int rowSpan, int colSpan) {
        if (view == null || cellWidth == 0) return;
        float newX = col * cellWidth;
        float newY = row * cellHeight;
        view.animate().x(newX).y(newY).setDuration(250).start();
    }

    private void revertDraggedItemToOriginalPosition(AppItem item, int originalPageId, int originalRow, int originalCol) {
        if (!isAdded()) return;

        databaseWriteExecutor.execute(() -> {
            item.page = originalPageId;
            item.setPosition(originalRow, originalCol);
            appItemDao.update(item);
            homeScreenHandler.post(() -> {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Item reverted to original position.", Toast.LENGTH_SHORT).show();
                    forceRefreshLayoutFromDb();
                }
            });
        });
    }

    private void showManageStorageRationale() {
        if (!isAdded()) {
            Log.w(TAG, "showManageStorageRationale: Fragment not added, cannot show rationale.");
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("All Files Access Required")
                .setMessage("To browse and manage all your files, this app needs special permission. Please tap 'Go to Settings' and enable 'Allow access to manage all files'.")
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse(String.format("package:%s", requireContext().getPackageName())));
                        manageStorageResultLauncher.launch(intent);
                    } catch (Exception e) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        manageStorageResultLauncher.launch(intent);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Permission denied. File browsing is disabled.", Toast.LENGTH_SHORT).show();
                    }
                    clearPendingAction();
                })
                .setCancelable(false)
                .show();
    }

    private void showLegacyStorageRationale() {
        if (!isAdded()) {
            Log.w(TAG, "showLegacyStorageRationale: Fragment not added, cannot show rationale.");
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Storage Permission Needed")
                .setMessage("This app needs permission to read your storage to display your files.")
                .setPositiveButton("OK", (dialog, which) -> {
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, LEGACY_STORAGE_PERMISSION_CODE);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Permission denied. File browsing is disabled.", Toast.LENGTH_SHORT).show();
                    }
                    clearPendingAction();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LEGACY_STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Permission Granted", Toast.LENGTH_SHORT).show();
                }
                executePendingAction();
            } else {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Permission is required to browse files.", Toast.LENGTH_SHORT).show();
                }
                clearPendingAction();
            }
        }
    }

    public void removeItemViewAndClearCells(AppItem item) {
        if (!isAdded() || rootLayout == null || item == null || item.getView() == null) {
            Log.w(TAG, "removeItemViewAndClearCells: Fragment not added or item view null. Cannot remove.");
            return;
        }
        homeScreenHandler.post(() -> {
            rootLayout.removeView(item.getView());
            clearCellsFor(item);
            if (countEmptyCells() == ROWS * COLS && listener != null) {
                listener.onPageEmpty(pageIndex);
            }
        });
    }

    public void placeItemOnGridWithDisplacement(AppItem droppedItem, int initialTargetRow, int initialTargetCol,
                                                int originalDroppedItemRow, int originalDroppedItemCol, int originalDroppedItemPageId) {
        if (!isAdded() || rootLayout == null || cellWidth == 0) {
            Log.e(TAG, "placeItemOnGridWithDisplacement: Fragment not added, rootLayout null, or cellWidth 0. Aborting drop.");
            revertDraggedItemToOriginalPosition(droppedItem, originalDroppedItemPageId, originalDroppedItemRow, originalDroppedItemCol);
            return;
        }

        droppedItem.page = pageIndex;

        int requiredRowSpan = droppedItem.getRowSpan();
        int requiredColSpan = droppedItem.getColSpan();

        int targetCol = Math.max(0, Math.min(initialTargetCol, COLS - requiredColSpan));
        int targetRow = Math.max(0, Math.min(initialTargetRow, ROWS - requiredRowSpan));

        AppItem[][] tempOccupiedCells = new AppItem[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            System.arraycopy(occupiedCells[r], 0, tempOccupiedCells[r], 0, COLS);
        }

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (tempOccupiedCells[r][c] == droppedItem) {
                    tempOccupiedCells[r][c] = null;
                }
            }
        }

        List<AppItem> itemsToDisplace = new ArrayList<>();
        boolean isInvalidDrop = false;

        for (int r = targetRow; r < targetRow + requiredRowSpan && r < ROWS; r++) {
            for (int c = targetCol; c < targetCol + requiredColSpan && c < COLS; c++) {
                if (r >= ROWS || c >= COLS) {
                    isInvalidDrop = true;
                    break;
                }
                AppItem existingItem = tempOccupiedCells[r][c];
                if (existingItem != null && existingItem.id != droppedItem.id) {
                    if (existingItem.getType() == AppItem.Type.WIDGET) {
                        isInvalidDrop = true;
                        break;
                    } else {
                        if (!itemsToDisplace.contains(existingItem)) {
                            itemsToDisplace.add(existingItem);
                        }
                    }
                }
            }
            if (isInvalidDrop) break;
        }

        if (isInvalidDrop) {
            homeScreenHandler.post(() -> Toast.makeText(getContext(), "Cannot place item on an occupied area (widget conflict or invalid overlap). Reverting.", Toast.LENGTH_SHORT).show());
            revertDraggedItemToOriginalPosition(droppedItem, originalDroppedItemPageId, originalDroppedItemRow, originalDroppedItemCol);
            return;
        }

        if (droppedItem.getType() == AppItem.Type.APP && itemsToDisplace.size() == 1 &&
                droppedItem.getRowSpan() == 1 && droppedItem.getColSpan() == 1) {
            AppItem targetItem = itemsToDisplace.get(0);
            if (targetItem.getType() == AppItem.Type.APP && targetItem.getRowSpan() == 1 && targetItem.getColSpan() == 1) {
                swapAppItems(droppedItem, targetItem, targetRow, targetCol,
                        originalDroppedItemRow, originalDroppedItemCol, originalDroppedItemPageId);
                homeScreenHandler.post(() -> Toast.makeText(getContext(), "Apps swapped successfully!", Toast.LENGTH_SHORT).show());
                return;
            }
        }

        clearCellsFor(droppedItem);

        markCellsOccupied(droppedItem, targetRow, targetCol, requiredRowSpan, requiredColSpan);

        List<AppItem> displacedItemsUpdated = new ArrayList<>();
        for (AppItem displacedItem : itemsToDisplace) {
            clearCellsFor(displacedItem);
            int[] newPos = findFirstFreeCell();
            if (newPos != null) {
                displacedItem.setPosition(newPos[0], newPos[1]);
                displacedItem.page = pageIndex;
                markCellsOccupied(displacedItem, newPos[0], newPos[1], displacedItem.getRowSpan(), displacedItem.getColSpan());
                displacedItemsUpdated.add(displacedItem);
            } else {
                Log.e(TAG, "Not enough space for displaced item: " + displacedItem.getName() + ". Reverting dropped item.");
                homeScreenHandler.post(() -> Toast.makeText(getContext(), "Not enough space to displace existing items. Reverting.", Toast.LENGTH_SHORT).show());
                revertDraggedItemToOriginalPosition(droppedItem, originalDroppedItemPageId, originalDroppedItemRow, originalDroppedItemCol);
                return;
            }
        }

        List<AppItem> itemsToUpdateDb = new ArrayList<>(displacedItemsUpdated);
        droppedItem.setPosition(targetRow, targetCol);
        // Ensure the dropped item is assigned to the current (target) page so it doesn't jump elsewhere
        droppedItem.page = pageIndex;
        itemsToUpdateDb.add(0, droppedItem);

        databaseWriteExecutor.execute(() -> {
            for (AppItem item : itemsToUpdateDb) {
                appItemDao.update(item);
            }
            homeScreenHandler.post(this::forceRefreshLayoutFromDb);
            homeScreenHandler.post(() -> Toast.makeText(getContext(), "Item placed and others displaced.", Toast.LENGTH_SHORT).show());
        });
    }

    private void swapAppItems(AppItem droppedItem, AppItem targetItem, int droppedTargetRow, int droppedTargetCol,
                              int originalDroppedItemRow, int originalDroppedItemCol, int originalDroppedItemPageId) {
        if (!isAdded()) return;

        droppedItem.setPosition(droppedTargetRow, droppedTargetCol);
        droppedItem.page = pageIndex;

        targetItem.setPosition(originalDroppedItemRow, originalDroppedItemCol);
        targetItem.page = originalDroppedItemPageId;

        databaseWriteExecutor.execute(() -> {
            appItemDao.update(droppedItem);
            appItemDao.update(targetItem);
            homeScreenHandler.post(() -> {
                if (isAdded()) {
                    forceRefreshLayoutFromDb();
                }

                if (originalDroppedItemPageId != pageIndex && listener != null && listener.getPagerAdapter() != null) {
                    HomeScreenFragment originalFragment = (HomeScreenFragment) requireActivity().getSupportFragmentManager().findFragmentByTag("f" + listener.getPagerAdapter().getPagePosition(originalDroppedItemPageId));
                    if (originalFragment != null) {
                        originalFragment.forceRefreshLayoutFromDb();
                    }
                }
            });
        });
    }

    @Deprecated
    public void handleItemDropFromCrossPageDrag(AppItem droppedItem) {
        Log.w(TAG, "handleItemDropFromCrossPageDrag is deprecated. Use placeItemOnGridWithDisplacement instead.");
        forceRefreshLayoutFromDb();
    }

    private void updateItemPosition(AppItem item, int newRow, int newCol) {
        if (!isAdded()) return;

        clearCellsFor(item);

        if (isPositionAvailable(newRow, newCol, item.getRowSpan(), item.getColSpan(), item)) {
            item.setPosition(newRow, newCol);
            markCellsOccupied(item, newRow, newCol, item.getRowSpan(), item.getColSpan());

            View itemView = item.getView();
            if (itemView != null) {
                float newX = newCol * cellWidth;
                float newY = newRow * cellHeight;
                itemView.animate()
                        .x(newX)
                        .y(newY)
                        .setDuration(200)
                        .start();
            }

            dbUpdate(item);
        } else {
            markCellsOccupied(item, item.getRow(), item.getCol(), item.getRowSpan(), item.getColSpan());
            Toast.makeText(getContext(), "Cannot place here", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isPositionAvailable(int startRow, int startCol, int rowSpan, int colSpan, AppItem excludingItem) {
        for (int r = startRow; r < startRow + rowSpan && r < ROWS; r++) {
            for (int c = startCol; c < startCol + colSpan && c < COLS; c++) {
                AppItem existingItem = occupiedCells[r][c];
                if (existingItem != null && existingItem != excludingItem) {
                    return false;
                }
            }
        }
        return true;
    }


    public interface PageInteractionListener {
        void onPageEmpty(int pageId);

        AppWidgetHost getAppWidgetHost();


        void requestDisallowInterceptTouchEvent(boolean disallow);

        void onAppInfoRequested(String packageName);

        void onUninstallRequested(String packageName);

        void onHomeScreenLongPressed();

        void onAppRemoved(AppItem appItem);

        void startCrossPageDrag(AppItem item, Bitmap iconBitmap, Rect initialRect,
                                int originalPage, int originalRow, int originalCol,
                                float initialTouchRawX, float initialTouchRawY);

        void removeItemFromPage(AppItem item);

        void onItemDroppedOnPage(AppItem item, int newRow, int newCol, int originalPageId);

        HomeScreenAdapter getPagerAdapter();
    }

    private abstract class ItemTouchListener implements View.OnTouchListener {
        public final long LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
        public final Handler longPressHandler = new Handler(Looper.getMainLooper());
        public final int touchSlop;
        public final Runnable longPressRunnable;
        protected final View view;
        protected final AppItem appItem;
        public float initialTouchX, initialTouchY;
        public boolean isMoving = false;
        public boolean isDragging = false;
        public boolean isLongPressTriggered = false;
        private float lastRawX, lastRawY;
        // Stricter threshold to avoid accidental drags/jitters on newer Android versions
        private final int movementThresholdPx;

        ItemTouchListener(View view, AppItem appItem) {
            this.view = view;
            this.appItem = appItem;
            this.touchSlop = ViewConfiguration.get(view.getContext()).getScaledTouchSlop();
            int minDragDp = 12; // about 12dp feels right to avoid micro jitter
            int minDragPx = (int) (view.getResources().getDisplayMetrics().density * minDragDp + 0.5f);
            this.movementThresholdPx = Math.max(this.touchSlop * 2, minDragPx);
            this.longPressRunnable = () -> {
                isLongPressTriggered = true;
                this.view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                // Do not start drag here. Drag should start only if the user moves after long press
                // (handled in ACTION_MOVE when movement exceeds touchSlop).
            };
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            lastRawX = event.getRawX();
            lastRawY = event.getRawY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    isMoving = false;
                    isDragging = false;
                    isLongPressTriggered = false;
                    longPressHandler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT);
                    onDown(event);
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float currentRawX = event.getRawX();
                    float currentRawY = event.getRawY();
                    float deltaX = Math.abs(currentRawX - initialTouchX);
                    float deltaY = Math.abs(currentRawY - initialTouchY);

                    // Optimized movement detection - use smaller threshold for better responsiveness
                    if (!isMoving && (deltaX > movementThresholdPx || deltaY > movementThresholdPx)) {
                        isMoving = true;
                        // Don't cancel long press timer when movement is detected to allow drag after long press
                    }

                    if (isDragging) {
                        // More responsive drag handling
                        onDrag(event);
                        return true;
                    } else if (isLongPressTriggered && !isDragging && (deltaX > touchSlop || deltaY > touchSlop)) {
                        // Start drag immediately when long press is triggered and movement exceeds touch slop
                        onDragStart(currentRawX, currentRawY);
                        return true;
                    }
                    return false;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    longPressHandler.removeCallbacks(longPressRunnable);

                    // Revert scale and alpha
                    view.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(150).start();

                    if (isDragging) {
                        onDragEnd(event);
                    } else if (isLongPressTriggered && !isMoving) {
                        onLongPressStationary();
                    } else if (!isMoving) {
                        onClick();
                    }

                    isDragging = false;
                    isLongPressTriggered = false;
                    isMoving = false;
                    onUp();
                    return true;
            }
            return false;
        }

        void onDown(MotionEvent event) {
            // Optional override
        }

        void onUp() {
            // Optional override
        }

        abstract void onDragStart(float currentRawX, float currentRawY);

        abstract void onDrag(MotionEvent event);

        abstract void onDragEnd(MotionEvent event);

        abstract void onClick();

        abstract void onLongPressStationary();

        public Bitmap createViewBitmap(View v, AppItem item) {
            if (v == null || v.getWidth() == 0 || v.getHeight() == 0) {
                return null;
            }
            Bitmap bitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            v.draw(canvas);
            return bitmap;
        }
    }

    private class AppIconTouchListener extends ItemTouchListener {
        private MotionEvent pendingMoveEvent;

        AppIconTouchListener(View view, AppItem appItem) {
            super(view, appItem);
        }

        @Override
        void onDown(MotionEvent event) {
            pendingMoveEvent = null;
        }

        @Override
        void onDragStart(float currentRawX, float currentRawY) {
            if (listener != null && !isDragging) {
                // Dismiss any open popup menu before starting drag
                dismissCurrentPopup();

                Bitmap iconBitmap = createViewBitmap(view, appItem);

                int[] viewLocation = new int[2];
                view.getLocationOnScreen(viewLocation);
                Rect initialRect = new Rect(viewLocation[0], viewLocation[1],
                        viewLocation[0] + view.getWidth(), viewLocation[1] + view.getHeight());

                listener.startCrossPageDrag(appItem, iconBitmap, initialRect, pageIndex,
                        appItem.getRow(), appItem.getCol(), currentRawX, currentRawY);

                // Visual feedback
                view.animate().scaleX(1.2f).scaleY(1.2f).alpha(0.7f).setDuration(100).start();
                isDragging = true;
            }
        }

        @Override
        void onDrag(MotionEvent event) {
            // Store move events if drag hasn't started yet
            if (!isDragging && isLongPressTriggered) {
                if (pendingMoveEvent != null) {
                    pendingMoveEvent.recycle();
                }
                pendingMoveEvent = MotionEvent.obtain(event);
            }
        }

        @Override
        void onDragEnd(MotionEvent event) {
            if (pendingMoveEvent != null) {
                pendingMoveEvent.recycle();
                pendingMoveEvent = null;
            }
        }

        @Override
        void onClick() {
            if (appItem.getType() == AppItem.Type.APP) {
                if (appItem.getActivityInfo() != null && isAdded()) {
                    Intent intent = requireContext().getPackageManager().getLaunchIntentForPackage(appItem.getActivityInfo().packageName);
                    if (intent != null) {
                        try {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        } catch (Exception e) {
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Cannot launch app", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            } else {
                handleSpecialItemClick(appItem);
            }
        }

        @Override
        void onLongPressStationary() {
            // Apply popup effect
            view.animate().scaleX(1.1f).scaleY(1.1f).alpha(0.8f).setDuration(150).start();

            // If there's a pending move event, start dragging immediately
            if (pendingMoveEvent != null) {
                onDragStart(pendingMoveEvent.getRawX(), pendingMoveEvent.getRawY());
                pendingMoveEvent.recycle();
                pendingMoveEvent = null;
            } else {
                showAppOptionsMenu(view, appItem);
            }
        }

        public Bitmap createViewBitmap(View v, AppItem item) {
            if (v == null || v.getWidth() == 0 || v.getHeight() == 0) {
                return null;
            }
            Bitmap bitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            v.draw(canvas);
            return bitmap;
        }
    }

    private class WidgetTouchListener implements View.OnTouchListener {
        private final View containerView;
        private final AppItem widgetItem;
        private final AppWidgetHostView hostView;
        private final View touchOverlay; // Added to constructor
        private final long LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
        private final Handler longPressHandler = new Handler(Looper.getMainLooper());
        private final int movementThreshold; // persistent threshold used across MOVE and UP
        private float initialTouchX, initialTouchY;
        private final Runnable longPressRunnable;
        private long downTimestampMs;
        private final int touchSlop; // tougher threshold
        private boolean isMoving = false; // True if moved beyond touchSlop
        private boolean isDragging = false; // True if our drag operation is active
        private boolean isLongPressTriggered = false; // True if longPressRunnable has executed

        // This flag tracks if we have started delegating events to the hostView for normal interaction
        private boolean delegatingToHostView = false;

        // Keep track of the MotionEvent that triggered ACTION_DOWN for the current sequence
        private MotionEvent initialDownEvent; // Stored to potentially dispatch to hostView or for cancel
        private MotionEvent pendingMoveEvent; // Stores pending move event when long press hasn't completed yet

        // Track the most recent touch position
        private float lastRawX, lastRawY;

        WidgetTouchListener(View containerView, AppItem widgetItem, AppWidgetHostView hostView, View touchOverlay) {
            this.containerView = containerView;
            this.widgetItem = widgetItem;
            this.hostView = hostView;
            this.touchOverlay = touchOverlay; // Initialize
            this.touchSlop = ViewConfiguration.get(containerView.getContext()).getScaledTouchSlop() * 2; // tougher threshold
            int minDragDp = 12;
            int minDragPx = (int) (containerView.getResources().getDisplayMetrics().density * minDragDp + 0.5f);
            this.movementThreshold = Math.max(minDragPx, this.touchSlop);
            this.longPressRunnable = () -> {
                isLongPressTriggered = true;
                containerView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);

                // Do not apply any popup/scale here to avoid visual "pop" during drag start

                // If we're already delegating to the hostView, we need to cancel that
                if (delegatingToHostView) {
                    // Cancel any existing touch sequence on the hostView
                    sendCancelEventToHostView(initialDownEvent);
                    delegatingToHostView = false;
                }

                // Prevent ViewPager or parent from intercepting after long press
                if (listener != null) {
                    listener.requestDisallowInterceptTouchEvent(true);
                }

                // Do NOT show the remove dialog here.
                // If the user moves after long press, we'll start drag.
                // If the user lifts finger without moving, we'll show the dialog on ACTION_UP.
                if (isMoving && !isDragging) {
                    Log.d(TAG, "Long press with movement detected. Starting widget drag.");
                    startWidgetDrag(lastRawX, lastRawY);
                }
            };
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int action = event.getActionMasked();

            // Always ensure the touchOverlay is getting the events
            if (v != touchOverlay) {
                Log.e(TAG, "WidgetTouchListener attached to wrong view! Should be on touchOverlay.");
                return false; // Or throw an error
            }

            // Update last touch position on every event
            lastRawX = event.getRawX();
            lastRawY = event.getRawY();

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    isMoving = false;
                    isDragging = false;
                    isLongPressTriggered = false;
                    delegatingToHostView = false;
                    downTimestampMs = System.currentTimeMillis();

                    // Clear any pending callbacks and events
                    longPressHandler.removeCallbacks(longPressRunnable);
                    if (pendingMoveEvent != null) {
                        pendingMoveEvent.recycle();
                        pendingMoveEvent = null;
                    }

                    // Post long press detection
                    longPressHandler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT);

                    // Store initial down event for potential delegation
                    if (initialDownEvent != null) initialDownEvent.recycle();
                    initialDownEvent = MotionEvent.obtain(event);

                    // We don't delegate DOWN events until we determine interaction type
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float deltaX = Math.abs(event.getRawX() - initialTouchX);
                    float deltaY = Math.abs(event.getRawY() - initialTouchY);

                    // If already dragging, just consume the event
                    if (isDragging) {
                        return true;
                    }

                    // If we've already decided to delegate to the widget
                    if (delegatingToHostView) {
                        hostView.dispatchTouchEvent(event);
                        return true;
                    }

                    // Check for significant movement with stricter threshold for Android 15 jitter
                    if (!isMoving && (deltaX > movementThreshold || deltaY > movementThreshold)) {
                        isMoving = true;

                        // CRITICAL CHANGE: We do NOT cancel the long press timer when movement is detected
                        // This allows long press to still trigger even with movement

                        // Store pending move event if long press hasn't triggered yet
                        if (!isLongPressTriggered) {
                            if (pendingMoveEvent != null) pendingMoveEvent.recycle();
                            pendingMoveEvent = MotionEvent.obtain(event);

                            // Do not start drag before long-press triggers; wait for clear movement post-long-press
                        } else {
                            // Long press already triggered, start drag immediately
                            startWidgetDrag(event.getRawX(), event.getRawY());
                            return true;
                        }
                    }

                    // If long press triggered and movement exceeds threshold, start drag
                    if (isLongPressTriggered && !isDragging && (deltaX > movementThreshold || deltaY > movementThreshold)) {
                        startWidgetDrag(event.getRawX(), event.getRawY());
                        return true;
                    }

                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // Cancel any pending long press detection
                    longPressHandler.removeCallbacks(longPressRunnable);

                    // Reset view appearance (no scaling applied earlier)
                    containerView.setScaleX(1.0f);
                    containerView.setScaleY(1.0f);
                    containerView.setAlpha(1.0f);

                    // Handle different end-of-touch scenarios
                    if (isDragging) {
                        // Drag is being handled by the overlay, don't delegate to widget
                        Log.d(TAG, "Drag ended. Not delegating to hostView.");
                    } else if (delegatingToHostView) {
                        // We were already delegating to the widget, continue
                        Log.d(TAG, "Delegating UP/CANCEL to hostView.");
                        hostView.dispatchTouchEvent(event);
                    } else {
                        // Determine stationarity at release to avoid false movement detection on jittery devices
                        float upDeltaX = Math.abs(event.getRawX() - initialTouchX);
                        float upDeltaY = Math.abs(event.getRawY() - initialTouchY);
                        boolean stationary = upDeltaX <= movementThreshold && upDeltaY <= movementThreshold;

                        if (isLongPressTriggered && stationary) {
                            // Stationary long press -> show remove dialog now
                            Log.d(TAG, "Stationary long press - showing remove dialog on UP/CANCEL.");
                            showRemoveItemDialog(widgetItem);
                        } else if (!isMoving) {
                            // This was a short tap - delegate the entire sequence to widget
                            Log.d(TAG, "Short tap detected. Delegating to widget.");
                            if (initialDownEvent != null) {
                                hostView.dispatchTouchEvent(initialDownEvent);
                                initialDownEvent.recycle();
                                initialDownEvent = null;
                            }
                            hostView.dispatchTouchEvent(event);
                        }
                    }

                    // Clean up any pending events
                    if (initialDownEvent != null) {
                        initialDownEvent.recycle();
                        initialDownEvent = null;
                    }
                    if (pendingMoveEvent != null) {
                        pendingMoveEvent.recycle();
                        pendingMoveEvent = null;
                    }

                    // Allow parents to intercept again
                    if (listener != null) {
                        listener.requestDisallowInterceptTouchEvent(false);
                    }

                    // Reset all states for next interaction
                    isDragging = false;
                    isMoving = false;
                    isLongPressTriggered = false;
                    delegatingToHostView = false;
                    return true;
            }
            return false;
        }

        // Helper to send a CANCEL event to the hostView
        private void sendCancelEventToHostView(MotionEvent originalEventToUseForTime) {
            if (originalEventToUseForTime != null) {
                MotionEvent cancelEvent = MotionEvent.obtain(
                        originalEventToUseForTime.getDownTime(),
                        System.currentTimeMillis(),
                        MotionEvent.ACTION_CANCEL,
                        initialTouchX,
                        initialTouchY,
                        0
                );
                hostView.dispatchTouchEvent(cancelEvent);
                cancelEvent.recycle();
                Log.d(TAG, "Sent ACTION_CANCEL to hostView.");
            }
        }

        private void startWidgetDrag(float currentRawX, float currentRawY) {
            if (listener != null && !isDragging) {
                Log.d(TAG, "startWidgetDrag: Initiating drag for " + widgetItem.getName());

                // Cancel any ongoing touch sequence in the widget
                sendCancelEventToHostView(initialDownEvent);

                // Clean up events
                if (initialDownEvent != null) {
                    initialDownEvent.recycle();
                    initialDownEvent = null;
                }
                if (pendingMoveEvent != null) {
                    pendingMoveEvent.recycle();
                    pendingMoveEvent = null;
                }

                // Create visual representation of the widget
                Bitmap iconBitmap = createWidgetBitmap(containerView, widgetItem);

                // Get widget position for drag start
                int[] viewLocation = new int[2];
                containerView.getLocationOnScreen(viewLocation);
                Rect initialRect = new Rect(viewLocation[0], viewLocation[1],
                        viewLocation[0] + containerView.getWidth(), viewLocation[1] + containerView.getHeight());

                // Start cross-page drag operation
                listener.startCrossPageDrag(widgetItem, iconBitmap, initialRect, pageIndex,
                        widgetItem.getRow(), widgetItem.getCol(), currentRawX, currentRawY);

                isDragging = true;
            }
        }

        private Bitmap createWidgetBitmap(View v, AppItem item) {
            if (v == null || v.getWidth() == 0 || v.getHeight() == 0) return null;
            Bitmap bitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            v.draw(canvas);
            return bitmap;
        }
    }
}