package com.bluelight.computer.winlauncher.prolauncher.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.ui.adapter.IntroPagerAdapter;
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;

public class IntroActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private TextView btnNext;
    private IntroPagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        viewPager = findViewById(R.id.viewPager);
        btnNext = findViewById(R.id.btnNext);

        DotsIndicator dotsIndicator = findViewById(R.id.dotsIndicator);

        adapter = new IntroPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Attach dots indicator to viewpager
        dotsIndicator.attachTo(viewPager);

        // Initially hide both buttons


        // Next button click listener
        btnNext.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() < adapter.getItemCount() - 1) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            }
        });

        // Start button click listener
        btnNext.setOnClickListener(v -> {
            startActivity(new Intent(this, StartWallPaperActivity.class));
            finish();
        });

        // Update UI based on current page
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position == adapter.getItemCount() - 1) {
                    // Last page - show Start button and hide Next
                    btnNext.setVisibility(View.VISIBLE);
                } else {
                    // Not last page - show Next button and hide Start
                    btnNext.setVisibility(View.INVISIBLE);
                }
            }


        });
    }
}
