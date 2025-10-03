package com.bluelight.computer.winlauncher.prolauncher.ui.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.bluelight.computer.winlauncher.prolauncher.R;

public class SplashActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "LauncherPrefs";
    public static final String KEY_FIRST_RUN = "isFirstRun";

    private static final int NAVIGATION_DELAY = 7000;
    SplashActivity splash_activity;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_splash);

        splash_activity = this;
        CheckInternet();
    }


    private void checkForFirstRun() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isFirstRun = settings.getBoolean(KEY_FIRST_RUN, true);

        if (isFirstRun) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(KEY_FIRST_RUN, false);
            editor.apply();
            navigateTo(GetStartActivity.class);
        } else {
            navigateTo(LauncherActivity.class);
        }
    }


    private void navigateTo(Class<?> activityClass) {
        Intent intent = new Intent(SplashActivity.this, activityClass);
        startActivity(intent);
        finish();
    }

    public boolean CheckInternet() {
        ConnectivityManager cm = (ConnectivityManager) splash_activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        @SuppressLint("MissingPermission") NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkForFirstRun();
                }
            }, NAVIGATION_DELAY);
            return true;
        } else {
            handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkForFirstRun();
                }
            }, NAVIGATION_DELAY);
            Toast.makeText(splash_activity, "Please check your internet connection !!", Toast.LENGTH_SHORT).show();
        }
        return false;
    }
}