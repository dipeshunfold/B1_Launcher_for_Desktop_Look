package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.model.SettingsViewModel;


public class SystemFragment extends Fragment {


    public static final String PREFS_NAME = "LauncherPrefs";
    public static final String KEY_SYSTEM_BARS_MODE = "key_system_bars_mode";

    public static final int MODE_HIDE_ALL = 0;

    public static final int MODE_SHOW_NAV_ONLY = 1;


    public static final int MODE_SHOW_STATUS_ONLY = 2;

    public static final int MODE_SHOW_ALL = 3;
    private static final String KEY_ALERTS = "key_notification_alerts";
    private static final String KEY_STARTUP = "key_launch_at_startup";
    private SharedPreferences prefs;
    private SettingsViewModel settingsViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_system_static, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        settingsViewModel = new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);


        view.findViewById(R.id.setting_set_default).setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_HOME_SETTINGS)));
        view.findViewById(R.id.setting_system_settings).setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_SETTINGS)));
        view.findViewById(R.id.setting_notification_settings).setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        view.findViewById(R.id.setting_status_bar_layout).setOnClickListener(v -> showSystemBarsDialog());


        setupSwitch(view.findViewById(R.id.setting_alerts_layout), view.findViewById(R.id.setting_alerts_switch), KEY_ALERTS, true);
        setupSwitch(view.findViewById(R.id.setting_startup_layout), view.findViewById(R.id.setting_startup_switch), KEY_STARTUP, false);
    }


    private void showSystemBarsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_system_bars, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        RadioGroup radioGroup = dialogView.findViewById(R.id.radio_group_system_bars);


        int currentMode = prefs.getInt(KEY_SYSTEM_BARS_MODE, MODE_SHOW_STATUS_ONLY);


        switch (currentMode) {
            case MODE_SHOW_ALL:
                radioGroup.check(R.id.radio_show_all);
                break;
            case MODE_HIDE_ALL:
                radioGroup.check(R.id.radio_hide_all);
                break;
            case MODE_SHOW_NAV_ONLY:
                radioGroup.check(R.id.radio_show_nav);
                break;
            case MODE_SHOW_STATUS_ONLY:
            default:
                radioGroup.check(R.id.radio_show_status);
                break;
        }


        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int newMode;


            if (checkedId == R.id.radio_show_all) {
                newMode = MODE_SHOW_ALL;
            } else if (checkedId == R.id.radio_hide_all) {
                newMode = MODE_HIDE_ALL;
            } else if (checkedId == R.id.radio_show_nav) {
                newMode = MODE_SHOW_NAV_ONLY;
            } else {
                newMode = MODE_SHOW_STATUS_ONLY;
            }


            prefs.edit().putInt(KEY_SYSTEM_BARS_MODE, newMode).apply();


            settingsViewModel.setSystemBarsMode(newMode);


            dialog.dismiss();
        });

        dialog.show();
    }


    private void setupSwitch(LinearLayout layout, SwitchCompat switchCompat, String key, boolean defaultValue) {

        switchCompat.setChecked(prefs.getBoolean(key, defaultValue));

        switchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.edit().putBoolean(key, isChecked).apply());

        layout.setOnClickListener(v -> switchCompat.toggle());
    }
}