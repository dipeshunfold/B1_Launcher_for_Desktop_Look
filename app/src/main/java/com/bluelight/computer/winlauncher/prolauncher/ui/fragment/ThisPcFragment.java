package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.my_interface.FragmentActionListener;

import java.io.File;

public class ThisPcFragment extends Fragment {
    private static final String TAG = "ThisPcFragment"; // Define TAG
    private FragmentActionListener actionListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // It's generally safer to get the listener from the Activity if possible,
        // or ensure the parent fragment always implements it.
        // For now, keeping your existing logic but highlighting this as a potential point.
        Fragment parent = getParentFragment();
        if (parent instanceof FragmentActionListener) {
            actionListener = (FragmentActionListener) parent;
        } else {
            // Check if activity implements it as a fallback
            if (getActivity() instanceof FragmentActionListener) {
                actionListener = (FragmentActionListener) getActivity();
            } else {
                throw new RuntimeException("Parent fragment or hosting activity must implement FragmentActionListener");
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_this_pc, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupFolderShortcuts(view);
        setupStorageInfo(view);
    }

    private void setupFolderShortcuts(View view) {
        view.findViewById(R.id.btnDocument).setOnClickListener(v -> {
            if (actionListener != null)
                actionListener.requestLoadFragment(new DocumentFragment(), "Documents");
        });
        view.findViewById(R.id.btnDownloads).setOnClickListener(v -> {
            if (actionListener != null)
                actionListener.requestLoadFragment(new DownloadFragment(), "Downloads");
        });
        view.findViewById(R.id.btnPictures).setOnClickListener(v -> {
            if (actionListener != null)
                actionListener.requestLoadFragment(new PhotoFragment(), "Pictures");
        });
        view.findViewById(R.id.btnVideos).setOnClickListener(v -> {
            if (actionListener != null)
                actionListener.requestLoadFragment(new VideoFragment(), "Videos");
        });
    }

    private void setupStorageInfo(View view) {
        // Internal Storage
        File internalStoragePath = Environment.getExternalStorageDirectory(); // This is typically the primary external storage
        updateStorageInfo(view, R.id.progress_internal, R.id.text_internal_space, internalStoragePath, "Local Disk (C:)");
        view.findViewById(R.id.internal_storage_layout).setOnClickListener(v -> {
            if (actionListener != null)
                actionListener.requestLoadFragment(new PhoneStorageFragment(), "Local Disk (C:)");
        });


        // SD Card / Removable Storage
        LinearLayout sdCardLayout = view.findViewById(R.id.sd_card_layout);
        File sdCardDir = getRemovableStorageDirectory();

        if (sdCardDir != null) {
            sdCardLayout.setVisibility(View.VISIBLE);
            updateStorageInfo(view, R.id.progress_sd, R.id.text_sd_space, sdCardDir, "SD Card (D:)");
            sdCardLayout.setOnClickListener(v -> {
                if (actionListener != null)
                    actionListener.requestLoadFragment(new SdcardFragment(), "SD Card (D:)");
            });
        } else {
            sdCardLayout.setVisibility(View.GONE);
        }
    }

    /**
     * Attempts to find a removable storage directory that is suitable for StatFs.
     * This method looks for a non-primary external storage directory.
     * On newer Android, these are often app-specific. You might need broader storage permissions
     * (MANAGE_EXTERNAL_STORAGE) to access the actual root of an SD card if your app is a file manager.
     */
    private File getRemovableStorageDirectory() {
        if (getContext() == null) return null;

        File[] externalStorageDirs = ContextCompat.getExternalFilesDirs(getContext(), null);

        // externalStorageDirs[0] is usually the primary internal storage.
        // Subsequent entries might be removable storage.
        if (externalStorageDirs != null) {
            for (int i = 1; i < externalStorageDirs.length; i++) {
                File dir = externalStorageDirs[i];
                if (dir != null) {
                    // Check if it's considered removable and mounted
                    if (Environment.isExternalStorageRemovable(dir) && isStorageMounted(dir)) {
                        // This path is usually app-specific, e.g., /storage/XXXX-XXXX/Android/data/your.package/files
                        // If you need access to the root of the SD card (/storage/XXXX-XXXX),
                        // you will need MANAGE_EXTERNAL_STORAGE permission on Android 11+.
                        // For a typical file browser, simply using this accessible directory for StatFs is fine,
                        // but navigating "up" to the root may still hit permission issues.
                        return dir; // Return the first removable storage found
                    }
                }
            }
        }
        return null; // No suitable removable storage found
    }

    private boolean isStorageMounted(File path) {
        String state = Environment.getExternalStorageState(path);
        return Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }


    private void updateStorageInfo(View view, int progressBarId, int textViewId, File path, String label) {
        ProgressBar progressBar = view.findViewById(progressBarId);
        TextView textView = view.findViewById(textViewId);

        if (path == null || !path.exists()) {
            Log.e(TAG, "Storage path does not exist or is null: " + (path != null ? path.getPath() : "null"));
            progressBar.setVisibility(View.GONE);
            textView.setText(label + ": Not available");
            return;
        }

        try {
            StatFs stat = new StatFs(path.getPath());
            long totalSize = stat.getTotalBytes();
            long freeSize = stat.getAvailableBytes();
            long usedSize = totalSize - freeSize;

            if (totalSize > 0) {
                progressBar.setProgress((int) (usedSize * 100 / totalSize));
            } else {
                progressBar.setProgress(0); // Handle division by zero
            }

            textView.setText(Formatter.formatFileSize(getContext(), freeSize) + " free of " + Formatter.formatFileSize(getContext(), totalSize));
            progressBar.setVisibility(View.VISIBLE);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid path for StatFs: " + path.getPath(), e);
            progressBar.setVisibility(View.GONE);
            textView.setText(label + ": Error accessing storage");
            if (isAdded()) { // Only show toast if fragment is currently added to an activity
                Toast.makeText(getContext(), "Error accessing " + label + " storage. It might not be available or permission denied.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) { // Catch any other unexpected exceptions
            Log.e(TAG, "Unexpected error in updateStorageInfo for path: " + path.getPath(), e);
            progressBar.setVisibility(View.GONE);
            textView.setText(label + ": Error accessing storage");
            if (isAdded()) {
                Toast.makeText(getContext(), "Unexpected error for " + label + " storage.", Toast.LENGTH_LONG).show();
            }
        }
    }
}