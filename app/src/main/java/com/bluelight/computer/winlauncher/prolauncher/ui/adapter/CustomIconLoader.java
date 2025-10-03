package com.bluelight.computer.winlauncher.prolauncher.ui.adapter;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomIconLoader {


    private static final float ADAPTIVE_ICON_SCALE_FACTOR = 1.4f;


    private static final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private static final Handler handler = new Handler(Looper.getMainLooper());


    public static void loadIcon(ResolveInfo resolveInfo, PackageManager packageManager, ImageView imageView) {
        final String packageName = resolveInfo.activityInfo.packageName;


        imageView.setTag(packageName);

        imageView.setImageBitmap(null);

        executor.execute(() -> {


            final Drawable drawable = resolveInfo.loadIcon(packageManager);


            final Bitmap bitmap = createIconBitmap(drawable);


            handler.post(() -> {


                if (imageView.getTag() != null && imageView.getTag().equals(packageName)) {
                    imageView.setImageBitmap(bitmap);
                }
            });
        });
    }


    private static Bitmap createIconBitmap(Drawable drawable) {
        if (drawable == null) return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable adaptiveIcon = (AdaptiveIconDrawable) drawable;

            int width = adaptiveIcon.getIntrinsicWidth();
            int height = adaptiveIcon.getIntrinsicHeight();

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            Drawable backgroundDrawable = adaptiveIcon.getBackground();
            if (backgroundDrawable != null) {
                backgroundDrawable.setBounds(0, 0, width, height);
                backgroundDrawable.draw(canvas);
            }

            Drawable foregroundDrawable = adaptiveIcon.getForeground();
            if (foregroundDrawable != null) {
                int scaledSize = (int) (width * ADAPTIVE_ICON_SCALE_FACTOR);
                int offset = (width - scaledSize) / 2;
                foregroundDrawable.setBounds(offset, offset, scaledSize + offset, scaledSize + offset);
                foregroundDrawable.draw(canvas);
            }
            return bitmap;
        } else {
            if (drawable instanceof BitmapDrawable) {
                return ((BitmapDrawable) drawable).getBitmap();
            }

            int width = drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : 100;
            int height = drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : 100;

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        }
    }
}