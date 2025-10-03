package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.model.SettingsViewModel;


public class AppsFragment extends Fragment {

    public static final String PREFS_NAME = "LauncherPrefs";
    public static final String KEY_SHOW_RECENT = "key_show_recent_apps";
    private static final String KEY_CREATE_SHORTCUT = "key_create_new_app_shortcut";

    private SharedPreferences prefs;
    private SettingsViewModel settingsViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_apps_static, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        settingsViewModel = new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);

        setupRecentAppsSwitch(
                view.findViewById(R.id.setting_recent_apps_layout),
                view.findViewById(R.id.setting_recent_apps_switch)
        );

        setupSwitch(
                view.findViewById(R.id.setting_new_shortcut_layout),
                view.findViewById(R.id.setting_new_shortcut_switch),
                KEY_CREATE_SHORTCUT
        );
    }

    private void setupRecentAppsSwitch(LinearLayout layout, SwitchCompat switchCompat) {
        switchCompat.setChecked(prefs.getBoolean(KEY_SHOW_RECENT, true));
        switchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_SHOW_RECENT, isChecked).apply();
            settingsViewModel.setRecentsButtonEnabled(isChecked);
        });
        layout.setOnClickListener(v -> switchCompat.toggle());
    }

    private void setupSwitch(LinearLayout layout, SwitchCompat switchCompat, String preferenceKey) {
        switchCompat.setChecked(prefs.getBoolean(preferenceKey, true));
        switchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.edit().putBoolean(preferenceKey, isChecked).apply());
        layout.setOnClickListener(v -> switchCompat.toggle());
    }
}