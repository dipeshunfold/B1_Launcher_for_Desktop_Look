package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.model.SettingsViewModel;
import com.bluelight.computer.winlauncher.prolauncher.ui.adapter.DateFormatAdapter;
import com.github.dhaval2404.colorpicker.ColorPickerDialog;
import com.github.dhaval2404.colorpicker.model.ColorShape;

import java.util.Arrays;

public class GeneralFragment extends Fragment {

    public static final String PREFS_NAME = "LauncherPrefs";
    public static final String KEY_DESKTOP_TEXT_COLOR = "key_desktop_text_color";
    public static final String KEY_DATE_FORMAT = "key_date_format";

    private SharedPreferences prefs;
    private SettingsViewModel settingsViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_general_static, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        settingsViewModel = new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);

        view.findViewById(R.id.setting_text_color).setOnClickListener(v -> showColorPickerDialog());
        view.findViewById(R.id.setting_date_format).setOnClickListener(v -> showDateFormatDialog());
        view.findViewById(R.id.setting_device_name).setOnClickListener(v -> Toast.makeText(getContext(), "Opening device name editor...", Toast.LENGTH_SHORT).show());
    }

    private void showColorPickerDialog() {
        int initialColor = prefs.getInt(KEY_DESKTOP_TEXT_COLOR, Color.WHITE);
        new ColorPickerDialog.Builder(requireActivity())
                .setTitle("Choose desktop item color")
                .setColorShape(ColorShape.SQAURE)
                .setDefaultColor(initialColor)
                .setColorListener((color, colorHex) -> {
                    prefs.edit().putInt(KEY_DESKTOP_TEXT_COLOR, color).apply();

                    settingsViewModel.setDesktopTextColor(color);
                })
                .setPositiveButton("OK")
                .setNegativeButton("Cancel")
                .show();
    }

    private void showDateFormatDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_date_format, null);
        builder.setView(dialogView);
        final AlertDialog dialog = builder.create();

        final String[] dateFormats = {"dd/MM/yyyy", "dd/MM/yy", "d/M/yyyy", "d/M/yy", "yy/MM/dd", "yyyy/MM/dd", "yyyy-MM-dd", "dd-MMM-yy", "MM/dd/yyyy"};
        String currentFormat = prefs.getString(KEY_DATE_FORMAT, dateFormats[0]);

        RecyclerView recyclerView = dialogView.findViewById(R.id.date_formats_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        DateFormatAdapter adapter = new DateFormatAdapter(Arrays.asList(dateFormats), currentFormat, format -> {
            prefs.edit().putString(KEY_DATE_FORMAT, format).apply();

            settingsViewModel.setDateFormat(format);
            dialog.dismiss();
        });
        recyclerView.setAdapter(adapter);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();
    }
}