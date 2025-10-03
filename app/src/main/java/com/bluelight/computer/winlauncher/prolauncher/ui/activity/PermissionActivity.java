package com.bluelight.computer.winlauncher.prolauncher.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bluelight.computer.winlauncher.prolauncher.R;

import java.util.ArrayList;
import java.util.List;

public class PermissionActivity extends AppCompatActivity {

    private static final int DANGEROUS_PERMISSIONS_CODE = 123;
    private static final int NOTIFICATION_PERMISSION_CODE = 789;

    private final String[] dangerousPermissionsToRequest = {


            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_CONTACTS

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        View mainLayout = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return windowInsets;
        });

        ImageView btnAllowAccess = findViewById(R.id.btnAllowAccess);
        btnAllowAccess.setOnClickListener(v -> checkAndRequestPermissions());
    }

    private void checkAndRequestPermissions() {
        requestDangerousPermissions();
    }

    private void requestDangerousPermissions() {
        List<String> neededPermissions = new ArrayList<>();
        for (String permission : dangerousPermissionsToRequest) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(permission);
            }
        }

        if (!neededPermissions.isEmpty()) {
            showGeneralPermissionsRationale(neededPermissions.toArray(new String[0]));
        } else {
            requestNotificationPermissionIfNeeded();
        }
    }

    private void showGeneralPermissionsRationale(String[] permissionsToRequest) {
        new AlertDialog.Builder(this)
                .setTitle("Additional Permissions Needed")
                .setMessage("To provide features like video calls, caller ID, and location-based widgets, our app needs access to your Camera, Contacts, Phone, and Location. We will not access this data without your direct action.")
                .setPositiveButton("OK", (dialog, which) -> {
                    ActivityCompat.requestPermissions(this, permissionsToRequest, DANGEROUS_PERMISSIONS_CODE);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Toast.makeText(this, "Some features may not be available.", Toast.LENGTH_LONG).show();
                    requestNotificationPermissionIfNeeded();
                })
                .setCancelable(false)
                .show();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                goToNextActivity();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("Enable Notifications?")
                        .setMessage("Allow this app to send you important alerts and updates related to your launcher experience.")
                        .setPositiveButton("Allow", (dialog, which) -> {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
                        })
                        .setNegativeButton("Don't Allow", (dialog, which) -> {
                            Toast.makeText(this, "You won't receive notifications.", Toast.LENGTH_SHORT).show();
                            goToNextActivity();
                        })
                        .setCancelable(false)
                        .show();
            }
        } else {
            goToNextActivity();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == DANGEROUS_PERMISSIONS_CODE) {
            requestNotificationPermissionIfNeeded();
        } else if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            goToNextActivity();
        }
    }

    private void goToNextActivity() {
        startActivity(new Intent(PermissionActivity.this, IntroActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}