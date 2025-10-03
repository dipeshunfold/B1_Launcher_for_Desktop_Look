package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout; // Import FrameLayout
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.ui.activity.LauncherActivity;
import com.google.android.material.imageview.ShapeableImageView;

public class LauncherMenuDialogFragment extends DialogFragment {

    private static final String ARG_BOTTOM_OFFSET = "arg_bottom_offset";
    private int bottomOffset = 0;

    public static LauncherMenuDialogFragment newInstance(int bottomOffset) {
        LauncherMenuDialogFragment fragment = new LauncherMenuDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_BOTTOM_OFFSET, bottomOffset);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_launcher_menu, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.LauncherMenuDialogTheme);

        if (getArguments() != null) {
            bottomOffset = getArguments().getInt(ARG_BOTTOM_OFFSET, 0);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        applyBlurToContentContainer(view);

        // Find the root layout and set a click listener on it
        FrameLayout rootLayout = view.findViewById(R.id.root_layout);
        if (rootLayout != null) {
            rootLayout.setOnClickListener(v -> {
                // This listener will be triggered if a click occurs on the root_layout
                // itself, but not on any of its children that consume the click (like menu items).
                // This effectively dismisses the dialog when clicking on the transparent
                // areas of the root_layout, outside the actual menu content.
                dismiss();
            });
        }

        setupMenuItem(view.findViewById(R.id.menu_wallpapers), v -> {
            // Assuming WallpaperActivity exists
            Intent intent = new Intent(getActivity(), WallpaperActivity.class);
            startActivity(intent);
            dismiss();
        });
        setupMenuItem(view.findViewById(R.id.menu_gallery), v -> {
            startActivity(new Intent(Intent.ACTION_VIEW, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
            dismiss();
        });
        setupMenuItem(view.findViewById(R.id.menu_system_widget), v -> {
            ((LauncherActivity) requireActivity()).showWidgetList();
            dismiss();
        });
        setupMenuItem(view.findViewById(R.id.menu_launcher_app), v -> {
            ((LauncherActivity) requireActivity()).showAppPicker();
            dismiss();
        });
        setupMenuItem(view.findViewById(R.id.menu_add_page), v -> {
            Log.d("LauncherMenu", "Add page menu item clicked");
            if (getActivity() instanceof LauncherActivity) {
                ((LauncherActivity) getActivity()).addNewPage();
            } else {
                Log.e("LauncherMenu", "Activity is not LauncherActivity");
            }
            dismiss(); // Added for consistency
        });

        setupMenuItem(view.findViewById(R.id.menu_remove_page), v -> {
            ((LauncherActivity) requireActivity()).removeCurrentPage();
            dismiss();
        });
        setupMenuItem(view.findViewById(R.id.menu_desktop_icons), v -> {
            ((LauncherActivity) requireActivity()).showGridOptionsDialog();
            dismiss();
        });
        setupMenuItem(view.findViewById(R.id.menu_refresh_desktop), v -> {
            ((LauncherActivity) requireActivity()).showResetConfirmationDialog();
            dismiss();
        });
        setupMenuItem(view.findViewById(R.id.menu_settings), v -> {
            // Assuming SettingsFragment exists
            ((LauncherActivity) requireActivity()).toggleFragment(SettingsFragment.class, R.anim.slide_up, R.anim.slide_down);
            dismiss();
        });
    }

    private void applyBlurToContentContainer(View view) {
        LinearLayout contentContainer = view.findViewById(R.id.content_container);
        ShapeableImageView blurBackground = view.findViewById(R.id.blur_background);

        contentContainer.post(() -> {
            if (!isAdded() || requireActivity().isFinishing()) {
                return;
            }

            View rootView = requireActivity().getWindow().getDecorView().findViewById(android.R.id.content);
            if (rootView == null || rootView.getWidth() <= 0 || rootView.getHeight() <= 0) {
                Log.e("BlurDebug", "Root view for blur is not ready or has zero dimensions.");
                return;
            }

            Bitmap fullBitmap = Bitmap.createBitmap(rootView.getWidth(), rootView.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(fullBitmap);
            rootView.draw(canvas);

            int[] location = new int[2];
            contentContainer.getLocationOnScreen(location);
            int x = location[0];
            int y = location[1];
            int width = contentContainer.getWidth();
            int height = contentContainer.getHeight();

            if (width <= 0 || height <= 0) {
                Log.e("BlurDebug", "Content container for blur has zero dimensions.");
                fullBitmap.recycle();
                return;
            }
            Log.d("BlurDebug", "Position: x=" + x + ", y=" + y + ", width=" + width + ", height=" + height);

            x = Math.max(0, x);
            y = Math.max(0, y);
            width = Math.min(width, fullBitmap.getWidth() - x);
            height = Math.min(height, fullBitmap.getHeight() - y);

            if (width <= 0 || height <= 0) {
                Log.e("BlurDebug", "Cropped blur region has zero dimensions.");
                fullBitmap.recycle();
                return;
            }

            Bitmap croppedBitmap = Bitmap.createBitmap(fullBitmap, x, y, width, height);
            fullBitmap.recycle();

            float scale = 0.5f;
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap,
                    (int) (croppedBitmap.getWidth() * scale),
                    (int) (croppedBitmap.getHeight() * scale),
                    true);
            croppedBitmap.recycle();

            Bitmap blurredBitmap = blurBitmap(scaledBitmap, requireContext(), 15f);
            Bitmap finalBitmap = Bitmap.createScaledBitmap(blurredBitmap, width, height, true);

            requireActivity().runOnUiThread(() -> {
                if (isAdded() && blurBackground != null) {
                    blurBackground.setImageBitmap(finalBitmap);
                    blurBackground.setVisibility(View.VISIBLE);
                } else {
                    finalBitmap.recycle();
                }
            });
        });
    }

    private Bitmap blurBitmap(Bitmap bitmap, Context context, float radius) {
        RenderScript renderScript = null;
        Allocation input = null;
        Allocation output = null;
        ScriptIntrinsicBlur blurScript = null;
        try {
            renderScript = RenderScript.create(context);
            input = Allocation.createFromBitmap(renderScript, bitmap);
            output = Allocation.createTyped(renderScript, input.getType());

            blurScript = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
            blurScript.setInput(input);
            blurScript.setRadius(Math.min(radius, 25f));
            blurScript.forEach(output);
            output.copyTo(bitmap);
        } catch (Exception e) {
            Log.e("BlurBitmap", "Error during blur: " + e.getMessage());
        } finally {
            if (input != null) input.destroy();
            if (output != null) output.destroy();
            if (blurScript != null) blurScript.destroy();
            if (renderScript != null) renderScript.destroy();
        }
        return bitmap;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                WindowManager.LayoutParams layoutParams = window.getAttributes();
                boolean isLandscape = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
                layoutParams.width = isLandscape ? WindowManager.LayoutParams.WRAP_CONTENT : WindowManager.LayoutParams.MATCH_PARENT;
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
                layoutParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;

                layoutParams.y = bottomOffset;

                window.setAttributes(layoutParams);
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.getAttributes().windowAnimations = R.style.DialogAnimation;

                WindowCompat.setDecorFitsSystemWindows(window, false);
                window.setStatusBarColor(Color.TRANSPARENT);
                window.setNavigationBarColor(Color.TRANSPARENT);

                // This ensures that touching outside the dialog's window will dismiss it.
                // In conjunction with the rootLayout click listener, it provides comprehensive dismissal.
                dialog.setCanceledOnTouchOutside(true);
                dialog.setCancelable(true); // Ensure the dialog is cancelable
            }
        }
    }

    private void setupMenuItem(View view, View.OnClickListener listener) {
        if (view != null) {
            view.setOnClickListener(v -> {
                Log.d("LauncherMenu", "Menu item clicked: " + v.getId());
                if (listener != null) {
                    listener.onClick(v);
                }
                // Dismiss is handled by the listener already, and if not,
                // the default behavior would be to not dismiss.
                // Since most listeners already call dismiss(), this is fine.
            });
        }
    }
}