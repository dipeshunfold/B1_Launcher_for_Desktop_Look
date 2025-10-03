package com.bluelight.computer.winlauncher.prolauncher.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bluelight.computer.winlauncher.prolauncher.R;

public class GetStartActivity extends AppCompatActivity {

    ImageView btnGetStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_get_start);

        View mainLayout = findViewById(R.id.main);


        ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, windowInsets) -> {

            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());


            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);


            return windowInsets;
        });


        btnGetStart = findViewById(R.id.btnGetStart);

        btnGetStart.setOnClickListener(v -> {
            Intent intent = new Intent(GetStartActivity.this, PermissionActivity.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onBackPressed() {

    }
}