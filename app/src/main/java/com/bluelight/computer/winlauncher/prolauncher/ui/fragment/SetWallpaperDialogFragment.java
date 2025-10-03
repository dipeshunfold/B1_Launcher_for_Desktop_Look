package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

import android.app.Dialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.model.SettingsViewModel;
import com.bluelight.computer.winlauncher.prolauncher.model.prefrance.WallpaperPreferenceHelper;
import com.bluelight.computer.winlauncher.prolauncher.ui.activity.LauncherActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SetWallpaperDialogFragment extends BottomSheetDialogFragment {

    public static final String TAG = "SetWallpaperDialog";
    static final String ARG_WALLPAPER_RES_ID = "wallpaper_res_id";
    static final String ARG_WALLPAPER_URL = "wallpaper_url";

    private int wallpaperResId = -1;
    private String wallpaperUrl;

    private SettingsViewModel settingsViewModel;

    public static SetWallpaperDialogFragment newInstance(int wallpaperResId) {
        SetWallpaperDialogFragment fragment = new SetWallpaperDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_WALLPAPER_RES_ID, wallpaperResId);
        fragment.setArguments(args);
        return fragment;
    }

    public static SetWallpaperDialogFragment newInstance(String wallpaperUrl) {
        SetWallpaperDialogFragment fragment = new SetWallpaperDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_WALLPAPER_URL, wallpaperUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.AppBottomSheetDialogTheme);

        if (getArguments() != null) {
            wallpaperResId = getArguments().getInt(ARG_WALLPAPER_RES_ID, -1);
            wallpaperUrl = getArguments().getString(ARG_WALLPAPER_URL);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_set_wallpaper, container, false);
    }

    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            BottomSheetDialog bsd = (BottomSheetDialog) d;
            View bottomSheet = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                ViewGroup.LayoutParams lp = bottomSheet.getLayoutParams();
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                bottomSheet.setLayoutParams(lp);
                bottomSheet.setBackground(null);

                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setFitToContents(true);
                behavior.setSkipCollapsed(true);
                behavior.setPeekHeight(0, true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        return dialog;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        settingsViewModel = new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);

        ImageView setForApp = view.findViewById(R.id.btnAppBg);
        ImageView setForHome = view.findViewById(R.id.set_for_home);
        ImageView setForLock = view.findViewById(R.id.set_for_lock);
        ImageView setForBoth = view.findViewById(R.id.set_for_both);

        setForApp.setOnClickListener(v -> setForAppBackground());
        setForHome.setOnClickListener(v -> setSystemWallpaper(WallpaperManager.FLAG_SYSTEM));
        setForLock.setOnClickListener(v -> setSystemWallpaper(WallpaperManager.FLAG_LOCK));
        setForBoth.setOnClickListener(v -> setSystemWallpaper(WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK));
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog instanceof BottomSheetDialog) {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialog;
            View bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                // Ensure full width (do not force full height to keep rounded corners visible)
                ViewGroup.LayoutParams lp = bottomSheet.getLayoutParams();
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                bottomSheet.setLayoutParams(lp);
                bottomSheet.setBackground(null);

                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setFitToContents(true);
                behavior.setSkipCollapsed(true);
                behavior.setPeekHeight(0, true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        }
    }

    private void setForAppBackground() {
        if (getContext() == null) return;

        if (wallpaperUrl != null && !wallpaperUrl.isEmpty()) {
            Toast.makeText(getContext(), R.string.setting_wallpaper, Toast.LENGTH_SHORT).show();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(() -> {
                boolean success = false;
                try {
                    // Pre-load the bitmap with Glide to ensure it's downloaded and in cache.
                    Glide.with(requireContext())
                            .asBitmap()
                            .load(wallpaperUrl)
                            .submit()
                            .get();

                    WallpaperPreferenceHelper.setWallpaperUrl(requireContext(), wallpaperUrl);
                    handler.post(() -> {
                        settingsViewModel.setAppWallpaper(wallpaperUrl);
                        // Force immediate update in host activity
                        if (getActivity() instanceof LauncherActivity) {
                            ((LauncherActivity) getActivity()).updateAppBackgroundDisplay();
                        }
                        Toast.makeText(getContext(), R.string.wallpaper_set_success, Toast.LENGTH_SHORT).show();
                        dismiss();
                    });
                    success = true;
                } catch (ExecutionException | InterruptedException e) {
                    Log.e(TAG, "Failed to load bitmap from URL or interrupted for app background", e);
                    handler.post(() -> {
                        Toast.makeText(getContext(), R.string.wallpaper_set_failed, Toast.LENGTH_SHORT).show();
                        dismiss();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Failed to set app wallpaper preferences/ViewModel for URL", e);
                    handler.post(() -> {
                        Toast.makeText(getContext(), R.string.wallpaper_set_failed, Toast.LENGTH_SHORT).show();
                        dismiss();
                    });
                }
            });
        } else if (wallpaperResId != -1) {
            WallpaperPreferenceHelper.setWallpaper(requireContext(), wallpaperResId);
            settingsViewModel.setAppWallpaper(wallpaperResId);
            // Force immediate update in host activity
            if (getActivity() instanceof LauncherActivity) {
                ((LauncherActivity) getActivity()).updateAppBackgroundDisplay();
            }
            Toast.makeText(getContext(), R.string.wallpaper_set_success, Toast.LENGTH_SHORT).show();
            dismiss();
        } else {
            Toast.makeText(getContext(), R.string.wallpaper_set_failed, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No valid wallpaper (URL or ResId) to set for app background.");
            dismiss();
        }
    }

    private void setSystemWallpaper(int flag) {
        if (getContext() == null) return;

        Toast.makeText(getContext(), R.string.setting_wallpaper, Toast.LENGTH_SHORT).show();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            boolean success = false;
            try {
                WallpaperManager wallpaperManager = WallpaperManager.getInstance(requireContext());
                Bitmap bitmap = null;

                if (wallpaperUrl != null && !wallpaperUrl.isEmpty()) {
                    bitmap = Glide.with(requireContext())
                            .asBitmap()
                            .load(wallpaperUrl)
                            .submit()
                            .get();
                } else if (wallpaperResId != -1) {
                    bitmap = getBitmapFromDrawable(requireContext(), wallpaperResId);
                }

                if (bitmap == null)
                    throw new NullPointerException("Bitmap could not be created from resource or URL.");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wallpaperManager.setBitmap(bitmap, null, true, flag);
                } else {
                    wallpaperManager.setBitmap(bitmap);
                }
                success = true;
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Failed to load bitmap from URL or interrupted for system wallpaper", e);
                success = false;
            } catch (Exception e) {
                success = false;
            }

            boolean finalSuccess = success;
            handler.post(() -> {
                if (getContext() == null) return;
                Toast.makeText(getContext(),
                        finalSuccess ? R.string.wallpaper_set_success : R.string.wallpaper_set_failed,
                        Toast.LENGTH_SHORT).show();
                dismiss();
            });
        });
    }

    private Bitmap getBitmapFromDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        if (drawable == null) return null;
        try {
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();
            if (width <= 0 || height <= 0) {
                Log.w(TAG, "Drawable has invalid intrinsic dimensions: " + drawableId);
                return null;
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error creating bitmap from drawable", e);
            return null;
        }
    }
}