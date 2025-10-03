package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.model.SettingsViewModel;


public class AdvanceFeaturesFragment extends Fragment {

    public static final String PREFS_NAME = "LauncherPrefs";
    public static final String KEY_CORTANA = "key_enable_cortana";
    public static final String KEY_TRANSPARENT_TASKBAR = "key_transparent_taskbar";
    public static final String KEY_SHOW_CONTACTS = "key_show_contacts_icon";
    public static final String KEY_SHOW_TIME = "key_show_taskbar_time";

    private SharedPreferences prefs;
    private SettingsViewModel settingsViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_advanced_static, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        settingsViewModel = new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);

        setupSwitch(view.findViewById(R.id.setting_cortana_layout), view.findViewById(R.id.setting_cortana_switch), KEY_CORTANA, true, isChecked -> settingsViewModel.setCortanaEnabled(isChecked));
        setupSwitch(view.findViewById(R.id.setting_transparent_taskbar_layout), view.findViewById(R.id.setting_transparent_taskbar_switch), KEY_TRANSPARENT_TASKBAR, false, isChecked -> settingsViewModel.setTaskbarTransparent(isChecked));
        setupSwitch(view.findViewById(R.id.setting_show_contacts_layout), view.findViewById(R.id.setting_show_contacts_switch), KEY_SHOW_CONTACTS, true, isChecked -> settingsViewModel.setContactsEnabled(isChecked));
        setupSwitch(view.findViewById(R.id.setting_show_time_layout), view.findViewById(R.id.setting_show_time_switch), KEY_SHOW_TIME, true, isChecked -> settingsViewModel.setTimeEnabled(isChecked));
    }

    private void setupSwitch(LinearLayout layout, SwitchCompat switchCompat, String preferenceKey, boolean defaultValue, OnSwitchChangedListener listener) {
        switchCompat.setChecked(prefs.getBoolean(preferenceKey, defaultValue));
        switchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(preferenceKey, isChecked).apply();

            listener.onSwitchChanged(isChecked);
        });
        layout.setOnClickListener(v -> switchCompat.toggle());
    }

    private interface OnSwitchChangedListener {
        void onSwitchChanged(boolean isChecked);
    }
}