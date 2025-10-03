package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

import static com.bluelight.computer.winlauncher.prolauncher.ui.activity.LauncherActivity.ACTION_ICON_SIZE_CHANGED;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.model.SettingsViewModel;
import com.bluelight.computer.winlauncher.prolauncher.model.prefrance.PreferenceHelper;


public class PersonalizationFragment extends Fragment {

    private static final String PREFS_NAME = "LauncherPrefs";
    private static final String KEY_HIDE_ICONS = "key_hide_taskbar_icons";

    private SharedPreferences prefs;
    private SettingsViewModel settingsViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_personalization, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        settingsViewModel = new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);


        view.findViewById(R.id.setting_wallpapers).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), WallpaperActivity.class);
                startActivity(intent);
            }
        });
        view.findViewById(R.id.setting_taskbar_color).setOnClickListener(v -> openFragment(ColorPickerFragment.newInstance()));
        view.findViewById(R.id.setting_grid_size).setOnClickListener(v -> showGridOptionsDialog());


        LinearLayout hideIconsLayout = view.findViewById(R.id.setting_hide_icons_layout);
        SwitchCompat hideIconsSwitch = view.findViewById(R.id.setting_hide_icons_switch);


        hideIconsSwitch.setChecked(prefs.getBoolean(KEY_HIDE_ICONS, false));


        hideIconsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_HIDE_ICONS, isChecked).apply();

            settingsViewModel.setHideTaskbarIcons(isChecked);
        });

        hideIconsLayout.setOnClickListener(v -> hideIconsSwitch.toggle());
    }

    private void openFragment(Fragment fragment) {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }


    public void showGridOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_grid_options, null);
        builder.setView(dialogView);
        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();


        final RadioGroup radioGroup = dialogView.findViewById(R.id.radio_group_icon_size);
        int currentSize = PreferenceHelper.getIconSize(getActivity());
        if (currentSize == PreferenceHelper.ICON_SIZE_SMALL) radioGroup.check(R.id.radio_small);
        else if (currentSize == PreferenceHelper.ICON_SIZE_LARGE)
            radioGroup.check(R.id.radio_large);
        else radioGroup.check(R.id.radio_medium);

        dialogView.findViewById(R.id.btn_save).setOnClickListener(v -> {
            int selectedId = radioGroup.getCheckedRadioButtonId();
            int newSize;
            if (selectedId == R.id.radio_small) newSize = PreferenceHelper.ICON_SIZE_SMALL;
            else if (selectedId == R.id.radio_large) newSize = PreferenceHelper.ICON_SIZE_LARGE;
            else newSize = PreferenceHelper.ICON_SIZE_MEDIUM;
            PreferenceHelper.setIconSize(getActivity(), newSize);

            // Update ViewModel immediately
            settingsViewModel.setIconSize(newSize);

            // Send broadcast for fragments already listening
            getActivity().sendBroadcast(new Intent(ACTION_ICON_SIZE_CHANGED));

            // Force the same immediate refresh path as LauncherActivity dialog
            if (getActivity() instanceof com.bluelight.computer.winlauncher.prolauncher.ui.activity.LauncherActivity) {
                ((com.bluelight.computer.winlauncher.prolauncher.ui.activity.LauncherActivity) getActivity()).forceImmediateUIRefresh();
            }

            Toast.makeText(getActivity(), "Icon size updated!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
    }
}