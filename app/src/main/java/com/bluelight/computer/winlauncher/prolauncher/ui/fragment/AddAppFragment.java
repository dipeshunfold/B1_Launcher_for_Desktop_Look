package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.model.prefrance.PreferenceHelper;
import com.bluelight.computer.winlauncher.prolauncher.ui.adapter.AppListAdapter;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AddAppFragment extends Fragment {

    private static final String ARG_PRESENT_PACKAGES = "present_packages";
    private Set<String> presentOnHomePackages = new HashSet<>();
    private Set<String> quickAccessPackages = new HashSet<>();
    private AppListAdapter adapter;
    private PackageManager packageManager;
    private OnAppPickedListener listener;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnAppPickedListener) {
            listener = (OnAppPickedListener) context;
        } else {
            throw new RuntimeException(context + " must implement OnAppPickedListener");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_app_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        RecyclerView recyclerView = view.findViewById(R.id.apps_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        packageManager = requireContext().getPackageManager();

        if (getArguments() != null) {
            Serializable serializable = getArguments().getSerializable(ARG_PRESENT_PACKAGES);
            if (serializable instanceof Set) {
                presentOnHomePackages = (Set<String>) serializable;
            }
        }

        quickAccessPackages = PreferenceHelper.getAllowedPackages(requireContext());
        if (quickAccessPackages == null) {
            quickAccessPackages = new HashSet<>();
        }

        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> allApps = packageManager.queryIntentActivities(mainIntent, 0);
        Collections.sort(allApps, new ResolveInfo.DisplayNameComparator(packageManager));

        adapter = new AppListAdapter(allApps, packageManager, presentOnHomePackages, quickAccessPackages, new AppListAdapter.OnAppActionListener() {
            @Override
            public void onAppAction(ResolveInfo appInfo, AppListAdapter.AppAction action) {
                if (listener != null) {
                    listener.onAppPicked(appInfo, action);
                }
            }

            @Override
            public void onAppLongClick(ResolveInfo appInfo, boolean isCurrentlyInQuickAccess) {
                showQuickAccessDialog(appInfo, isCurrentlyInQuickAccess);
            }
        });


        recyclerView.setAdapter(adapter);
    }


    private void showQuickAccessDialog(ResolveInfo appInfo, boolean isCurrentlyInQuickAccess) {
        String appName = appInfo.loadLabel(packageManager).toString();
        String message = isCurrentlyInQuickAccess
                ? "Remove '" + appName + "' from Quick Access?"
                : "Add '" + appName + "' to Quick Access?";
        String buttonText = isCurrentlyInQuickAccess ? "Remove" : "Add";

        new AlertDialog.Builder(requireContext())
                .setTitle("Quick Access")
                .setMessage(message)
                .setPositiveButton(buttonText, (dialog, which) -> {
                    String packageName = appInfo.activityInfo.packageName;
                    if (isCurrentlyInQuickAccess) {
                        PreferenceHelper.removePackage(requireContext(), packageName);
                        quickAccessPackages.remove(packageName);
                    } else {
                        PreferenceHelper.addPackage(requireContext(), packageName);
                        quickAccessPackages.add(packageName);
                    }
                    adapter.notifyDataSetChanged();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public interface OnAppPickedListener {
        void onAppPicked(ResolveInfo appInfo, AppListAdapter.AppAction action);
    }
}
