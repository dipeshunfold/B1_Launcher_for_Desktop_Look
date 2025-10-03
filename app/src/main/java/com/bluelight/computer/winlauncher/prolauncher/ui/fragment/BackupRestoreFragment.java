package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.database.AppDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackupRestoreFragment extends Fragment {
    private static final String PREFS_NAME = "LauncherPrefs";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_backup_restore_static, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.setting_backup_data).setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Backup Data")
                    .setMessage("This will back up your launcher settings and layout. Continue?")
                    .setPositiveButton("Backup", (dialog, which) -> {
                        Toast.makeText(getContext(), "Backing up data...", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        view.findViewById(R.id.setting_restore_data).setOnClickListener(v -> {

            final Dialog powerMenuDialog = new Dialog(requireContext());
            powerMenuDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            powerMenuDialog.setContentView(R.layout.dialog_power_menu);


            LinearLayout closeOption = powerMenuDialog.findViewById(R.id.dialog_option_close);
            LinearLayout resetOption = powerMenuDialog.findViewById(R.id.dialog_option_reset);

            closeOption.setOnClickListener(closeView -> {
                powerMenuDialog.dismiss();
                requireActivity().finishAffinity();
            });

            resetOption.setOnClickListener(resetView -> {

                powerMenuDialog.dismiss();


                final Dialog confirmationDialog = new Dialog(requireContext());
                confirmationDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                confirmationDialog.setContentView(R.layout.dialog_reset_confirmation);

                TextView cancelButton = confirmationDialog.findViewById(R.id.button_cancel);
                TextView confirmResetButton = confirmationDialog.findViewById(R.id.button_confirm_reset);


                cancelButton.setOnClickListener(cancelView -> confirmationDialog.dismiss());


                confirmResetButton.setOnClickListener(confirmView -> {

                    confirmationDialog.dismiss();


                    Toast.makeText(getContext(), "Resetting data...", Toast.LENGTH_SHORT).show();


                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    Handler handler = new Handler(Looper.getMainLooper());

                    executor.execute(() -> {

                        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        prefs.edit().clear().commit();

                        AppDatabase db = AppDatabase.getDatabase(requireContext());
                        db.appItemDao().deleteAllItems();


                        handler.post(() -> {

                            Context context = requireContext().getApplicationContext();
                            PackageManager packageManager = context.getPackageManager();
                            Intent restartIntent = packageManager.getLaunchIntentForPackage(context.getPackageName());

                            if (restartIntent == null) {
                                Toast.makeText(context, "Error: Could not find launch intent to restart.", Toast.LENGTH_LONG).show();
                                return;
                            }
                            restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            context.startActivity(restartIntent);
                            System.exit(0);
                        });
                    });
                });


                if (confirmationDialog.getWindow() != null) {
                    confirmationDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                    confirmationDialog.getWindow().setLayout(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
                }


                confirmationDialog.show();
            });


            if (powerMenuDialog.getWindow() != null) {
                powerMenuDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                powerMenuDialog.getWindow().setLayout(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            }


            powerMenuDialog.show();
        });
    }
}