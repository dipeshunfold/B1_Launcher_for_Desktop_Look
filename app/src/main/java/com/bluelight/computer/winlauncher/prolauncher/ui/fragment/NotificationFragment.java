package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.service.NotificationListener;
import com.bluelight.computer.winlauncher.prolauncher.ui.adapter.NotificationAdapter;
import com.google.android.material.imageview.ShapeableImageView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


public class NotificationFragment extends Fragment {


    private static final String TAG = "NotificationCenter";
    private static final String WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";
    private static final String EXTRA_WIFI_AP_STATE = "wifi_state";
    private static final int WIFI_AP_STATE_ENABLED = 13;
    private static final int WIFI_AP_STATE_FAILED = 14;
    private final List<StatusBarNotification> activeNotifications = new ArrayList<>();
    private View btnAirplane, btnWifi, btnBluetooth, btnFlashlight, btnAutoBrightness, btnData, btnRotate, btnHotspot, btnLocation, btnSettings, btnSound, btnWallpapers;
    private SwitchCompat toggleNotification;
    private SeekBar seekbarVolume, seekbarBrightness;
    private RecyclerView notificationRecyclerView;
    private NotificationAdapter notificationAdapter;
    private TextView btnClearAll;
    private TextView tvNoNotifications;
    private final BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            Log.d(TAG, "Notification broadcast received: " + action);
            if (NotificationListener.ACTION_NOTIFICATION_POSTED.equals(action)) {
                if (intent.hasExtra(NotificationListener.EXTRA_ALL_NOTIFICATIONS)) {
                    ArrayList<Parcelable> parcelableList = intent.getParcelableArrayListExtra(NotificationListener.EXTRA_ALL_NOTIFICATIONS);
                    activeNotifications.clear();
                    if (parcelableList != null) {
                        for (Parcelable p : parcelableList) {
                            if (p instanceof StatusBarNotification) {
                                activeNotifications.add((StatusBarNotification) p);
                            }
                        }
                    }
                } else if (intent.hasExtra(NotificationListener.EXTRA_NOTIFICATION)) {
                    StatusBarNotification sbn = intent.getParcelableExtra(NotificationListener.EXTRA_NOTIFICATION);
                    if (sbn != null) {
                        activeNotifications.removeIf(n -> n.getKey().equals(sbn.getKey()));
                        activeNotifications.add(0, sbn);
                    }
                }
            } else if (NotificationListener.ACTION_NOTIFICATION_REMOVED.equals(action)) {
                StatusBarNotification sbn = intent.getParcelableExtra(NotificationListener.EXTRA_NOTIFICATION);
                if (sbn != null) {
                    activeNotifications.removeIf(notification -> notification.getKey().equals(sbn.getKey()));
                }
            }
            updateNotificationUi();
        }
    };
    private AudioManager audioManager;
    private WifiManager wifiManager;
    private BluetoothAdapter bluetoothAdapter;
    private CameraManager cameraManager;
    private String cameraId;
    private boolean isFlashlightOn = false;
    private boolean isHotspotActive = false;
    private boolean isLaunchingWriteSettings = false;
    private boolean isUpdatingToggleProgrammatically = false;
    private final BroadcastReceiver systemStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action == null) return;


            if (WIFI_AP_STATE_CHANGED_ACTION.equals(action)) {
                int apState = intent.getIntExtra(EXTRA_WIFI_AP_STATE, WIFI_AP_STATE_FAILED);
                isHotspotActive = (apState == WIFI_AP_STATE_ENABLED);
                Log.d(TAG, "Hotspot state changed via broadcast. Is now active: " + isHotspotActive);
            }

            updateAllButtonStates();
        }
    };

    private final ActivityResultLauncher<Intent> bluetoothEnableRequestLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

                updateAllButtonStates();
            });


    public static boolean isMobileDataEnabled(Context context) {
        boolean mobileDataEnabled = false;
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            Class<?> cmClass = Class.forName(cm.getClass().getName());
            Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
            method.setAccessible(true);
            mobileDataEnabled = (Boolean) method.invoke(cm);
        } catch (Exception e) {
            Log.e(TAG, "Reflection for mobile data failed.", e);
        }
        return mobileDataEnabled;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FrameLayout rootLayout = view.findViewById(R.id.root_layout);
        CardView contentContainer = view.findViewById(R.id.content_container);


        applyBlurToContentContainer(view);
        applyBlurToContentContainer1(view);
        rootLayout.setOnClickListener(v -> closeFragment());

        contentContainer.setOnClickListener(v -> {
        });

        initializeServices();
        bindViews(view);
        setupClickListeners();
        setupSliders();
        setupNotificationPanel(view);
    }

    private void applyBlurToContentContainer1(View view) {

        CardView contentContainer1 = view.findViewById(R.id.content_container1);
        ShapeableImageView blurBackground1 = view.findViewById(R.id.blur_background1);
        contentContainer1.post(() -> {
            View rootView = requireActivity().getWindow().getDecorView().findViewById(android.R.id.content);
            Bitmap fullBitmap = Bitmap.createBitmap(rootView.getWidth(), rootView.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(fullBitmap);
            rootView.draw(canvas);

            int[] location = new int[2];
            contentContainer1.getLocationOnScreen(location);
            int x = location[0];
            int y = location[1];
            int width = contentContainer1.getWidth();
            int height = contentContainer1.getHeight();

            // Ensure coordinates are within bitmap bounds
            x = Math.max(0, Math.min(x, fullBitmap.getWidth() - 1));
            y = Math.max(0, Math.min(y, fullBitmap.getHeight() - 1));
            width = Math.min(width, fullBitmap.getWidth() - x);
            height = Math.min(height, fullBitmap.getHeight() - y);

            Log.d("BlurDebug", "Position: x=" + x + ", y=" + y + ", width=" + width + ", height=" + height);

            // Only create bitmap if dimensions are valid
            Bitmap croppedBitmap;
            if (width > 0 && height > 0) {
                croppedBitmap = Bitmap.createBitmap(fullBitmap, x, y, width, height);
            } else {
                croppedBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                Log.e("BlurDebug", "Invalid bitmap dimensions, using fallback");
            }
            fullBitmap.recycle();

            float scale = 0.5f;
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap,
                    (int) (croppedBitmap.getWidth() * scale),
                    (int) (croppedBitmap.getHeight() * scale),
                    true);
            croppedBitmap.recycle();

            Bitmap blurredBitmap = blurBitmap(scaledBitmap, requireContext(), 15f);
            Bitmap finalBitmap = Bitmap.createScaledBitmap(blurredBitmap, width, height, true);
            if (scaledBitmap != blurredBitmap) blurredBitmap.recycle();

            requireActivity().runOnUiThread(() -> {
                blurBackground1.setImageBitmap(finalBitmap);
                blurBackground1.setVisibility(View.VISIBLE);
            });
        });
    }

    private void applyBlurToContentContainer(View view) {


        CardView contentContainer = view.findViewById(R.id.content_container);
        ShapeableImageView blurBackground = view.findViewById(R.id.blur_background);

        contentContainer.post(() -> {
            View rootView = requireActivity().getWindow().getDecorView().findViewById(android.R.id.content);
            Bitmap fullBitmap = Bitmap.createBitmap(rootView.getWidth(), rootView.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(fullBitmap);
            rootView.draw(canvas);

            int[] location = new int[2];
            contentContainer.getLocationOnScreen(location);
            int x = location[0];
            int y = location[1];
            int width = contentContainer.getWidth();
            int height = contentContainer.getHeight();

            // Ensure coordinates are within bitmap bounds
            x = Math.max(0, Math.min(x, fullBitmap.getWidth() - 1));
            y = Math.max(0, Math.min(y, fullBitmap.getHeight() - 1));
            width = Math.min(width, fullBitmap.getWidth() - x);
            height = Math.min(height, fullBitmap.getHeight() - y);

            Log.d("BlurDebug", "Position: x=" + x + ", y=" + y + ", width=" + width + ", height=" + height);

            // Only create bitmap if dimensions are valid
            Bitmap croppedBitmap;
            if (width > 0 && height > 0) {
                croppedBitmap = Bitmap.createBitmap(fullBitmap, x, y, width, height);
            } else {
                croppedBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                Log.e("BlurDebug", "Invalid bitmap dimensions, using fallback");
            }
            fullBitmap.recycle();

            float scale = 0.5f;
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap,
                    (int) (croppedBitmap.getWidth() * scale),
                    (int) (croppedBitmap.getHeight() * scale),
                    true);
            croppedBitmap.recycle();

            Bitmap blurredBitmap = blurBitmap(scaledBitmap, requireContext(), 15f);
            Bitmap finalBitmap = Bitmap.createScaledBitmap(blurredBitmap, width, height, true);
            if (scaledBitmap != blurredBitmap) blurredBitmap.recycle();

            requireActivity().runOnUiThread(() -> {
                blurBackground.setImageBitmap(finalBitmap);
                blurBackground.setVisibility(View.VISIBLE);
            });
        });
    }

    private Bitmap blurBitmap(Bitmap bitmap, Context context, float radius) {
        RenderScript renderScript = RenderScript.create(context);
        Allocation input = Allocation.createFromBitmap(renderScript, bitmap);
        Allocation output = Allocation.createTyped(renderScript, input.getType());

        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
        blurScript.setInput(input);
        blurScript.setRadius(Math.min(radius, 25f));
        blurScript.forEach(output);
        output.copyTo(bitmap);

        input.destroy();
        output.destroy();
        blurScript.destroy();
        renderScript.destroy();

        return bitmap;
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        if (enter && nextAnim == R.anim.slide_in_from_right) {
            Animation rootFadeIn = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in_background);
            Animation contentSlideUp = AnimationUtils.loadAnimation(getContext(), R.anim.slide_in_from_right);
            if (getView() != null) {
                View content = getView().findViewById(R.id.content_container);
                content.startAnimation(contentSlideUp);
            }
            return rootFadeIn;
        } else if (!enter && nextAnim == R.anim.slide_out_to_right) {
            Animation rootFadeOut = AnimationUtils.loadAnimation(getContext(), R.anim.fade_out_background);
            Animation contentSlideDown = AnimationUtils.loadAnimation(getContext(), R.anim.slide_out_to_right);

            if (getView() != null) {
                View content = getView().findViewById(R.id.content_container);
                content.startAnimation(contentSlideDown);
            }
            return rootFadeOut;
        } else {
            return super.onCreateAnimation(transit, enter, nextAnim);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification_center, container, false);
    }

    private void closeFragment() {
        if (isAdded() && getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        isHotspotActive = isHotspotEnabled();
        updateAllButtonStates();
        registerReceivers();
        if (getContext() != null) {
            NotificationListener.requestCurrentNotifications(getContext());
        }
        // Allow WRITE_SETTINGS prompt again after returning from Settings
        isLaunchingWriteSettings = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceivers();
    }

    private void initializeServices() {
        Context context = requireContext();
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                cameraId = cameraManager.getCameraIdList()[0];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void bindViews(View view) {

        btnAirplane = view.findViewById(R.id.btn_airplane);
        btnWifi = view.findViewById(R.id.btn_wifi);
        btnBluetooth = view.findViewById(R.id.btn_bluetooth);
        btnFlashlight = view.findViewById(R.id.btn_flashlight);
        btnAutoBrightness = view.findViewById(R.id.btn_auto_brightness);
        btnData = view.findViewById(R.id.btn_data);
        btnRotate = view.findViewById(R.id.btn_rotate);
        btnHotspot = view.findViewById(R.id.btn_hotspot);
        btnLocation = view.findViewById(R.id.btn_location);
        btnSettings = view.findViewById(R.id.btn_settings);
        btnSound = view.findViewById(R.id.btn_sound);
        btnWallpapers = view.findViewById(R.id.btn_wallpapers);
        toggleNotification = view.findViewById(R.id.toggle_notification);
        seekbarVolume = view.findViewById(R.id.seekbar_volume);
        seekbarBrightness = view.findViewById(R.id.seekbar_brightness);
        btnClearAll = view.findViewById(R.id.btn_clear_all);
        tvNoNotifications = view.findViewById(R.id.tv_no_notifications);

        setupButtonUI(btnAirplane, R.drawable.ic_airplane, "Airplane");
        setupButtonUI(btnWifi, R.drawable.ic_wifi, "Wifi");
        setupButtonUI(btnBluetooth, R.drawable.ic_bluetooth, "Bluetooth");
        setupButtonUI(btnFlashlight, R.drawable.ic_flashlight, "Flashlight");
        setupButtonUI(btnAutoBrightness, R.drawable.ic_auto_brightness, "Manual");
        setupButtonUI(btnData, R.drawable.ic_data, "Data");
        setupButtonUI(btnRotate, R.drawable.ic_rotate, "Rotate");
        setupButtonUI(btnHotspot, R.drawable.ic_hotspot, "Hotspot");
        setupButtonUI(btnLocation, R.drawable.ic_location, "Location");
        setupButtonUI(btnSettings, R.drawable.ic_settings_l, "Settings");
        setupButtonUI(btnSound, R.drawable.ic_sound, "Sound");
        setupButtonUI(btnWallpapers, R.drawable.ic_wallpaper, "Wallpapers");
    }    private final ActivityResultLauncher<String> requestBluetoothConnectPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    toggleBluetoothState();
                } else {
                    Toast.makeText(getContext(), "Bluetooth permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    private void setupNotificationPanel(View view) {
        notificationRecyclerView = view.findViewById(R.id.notifications_recycler_view);
        notificationAdapter = new NotificationAdapter(getContext());
        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        notificationRecyclerView.setAdapter(notificationAdapter);
        btnClearAll.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), NotificationListener.class);
            intent.setAction("CLEAR_ALL_NOTIFICATIONS");
            requireContext().startService(intent);
            activeNotifications.clear();
            updateNotificationUi();
        });
    }

    private void setupClickListeners() {
        toggleNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingToggleProgrammatically) return;
            if (isChecked && !isNotificationAccessGranted()) {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            }
        });
        btnWifi.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startActivity(new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY));
            } else if (wifiManager != null) {
                wifiManager.setWifiEnabled(!wifiManager.isWifiEnabled());
            }
        });
        btnBluetooth.setOnClickListener(v -> {
            toggleBluetoothState();
        });
        btnWallpapers.setOnClickListener(v -> methodeWallpapers());
        btnFlashlight.setOnClickListener(v -> toggleFlashlight());
        btnData.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_DATA_USAGE_SETTINGS));
            } catch (android.content.ActivityNotFoundException e) {
                // Handle the case where the Data Usage Settings activity is not found
                Toast.makeText(getContext(), "Data Usage settings not found on this device.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Data Usage Settings not found: " + e.getMessage());
                // Optionally, open the general settings page as a fallback
                // startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        });
        btnAirplane.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)));
        btnLocation.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)));
        btnSound.setOnClickListener(v -> cycleRingerMode());
        btnSettings.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_SETTINGS)));
        btnAutoBrightness.setOnClickListener(v -> toggleAutoBrightness());
        btnRotate.setOnClickListener(v -> toggleAutoRotation());
        btnHotspot.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                startActivity(new Intent("android.settings.TETHER_SETTINGS"));
            }
        });
    }

    private void methodeWallpapers() {

        Intent intent = new Intent(getActivity(), WallpaperActivity.class);
        startActivity(intent);
    }

    private void setupSliders() {
        if (audioManager != null) {
            seekbarVolume.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            seekbarVolume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
            seekbarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar s, int p, boolean u) {
                    if (u) audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, p, 0);
                }

                public void onStartTrackingTouch(SeekBar s) {
                }

                public void onStopTrackingTouch(SeekBar s) {
                }
            });
        }
        seekbarBrightness.setMax(255);
        try {
            seekbarBrightness.setProgress(Settings.System.getInt(requireContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS));
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        seekbarBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                if (u && checkWriteSettingsPermission()) {
                    Settings.System.putInt(requireContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, p);
                }
            }

            public void onStartTrackingTouch(SeekBar s) {
                checkWriteSettingsPermission();
            }

            public void onStopTrackingTouch(SeekBar s) {
            }
        });
    }

    private void setupButtonUI(View buttonView, int iconRes, String label) {
        if (buttonView == null) return;
        ((ImageView) buttonView.findViewById(R.id.icon)).setImageResource(iconRes);
        ((TextView) buttonView.findViewById(R.id.label)).setText(label);
    }

    private void toggleBluetoothState() {
        if (bluetoothAdapter == null) {
            Toast.makeText(getContext(), "Bluetooth not supported on this device", Toast.LENGTH_SHORT).show();
            return;
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestBluetoothConnectPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
                return;
            }
        }

        if (bluetoothAdapter.isEnabled()) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {


                Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(intent);
            } else {

                bluetoothAdapter.disable();
            }
        } else {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                bluetoothEnableRequestLauncher.launch(intent);
            } else {

                bluetoothAdapter.enable();
            }
        }
    }

    private void toggleFlashlight() {
        if (cameraId == null || cameraManager == null) return;
        try {
            isFlashlightOn = !isFlashlightOn;
            cameraManager.setTorchMode(cameraId, isFlashlightOn);
            setButtonState(btnFlashlight, isFlashlightOn);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private boolean checkWriteSettingsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(requireContext())) {
            if (!isLaunchingWriteSettings) {
                isLaunchingWriteSettings = true;
                Toast.makeText(getContext(), "Please grant 'Modify system settings' permission", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + requireContext().getPackageName()));
                startActivity(intent);
            }
            return false;
        }
        return true;
    }

    private void toggleAutoBrightness() {
        if (!checkWriteSettingsPermission()) return;
        try {
            int mode = Settings.System.getInt(getContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);
            Settings.System.putInt(getContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC ? Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL : Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
            updateAutoBrightnessState();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toggleAutoRotation() {
        if (!checkWriteSettingsPermission()) return;
        try {
            int status = Settings.System.getInt(getContext().getContentResolver(), Settings.System.ACCELEROMETER_ROTATION);
            Settings.System.putInt(getContext().getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, status == 1 ? 0 : 1);
            updateRotationState();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setButtonState(View button, boolean isActive) {
        if (getContext() == null || button == null) return;
        button.setActivated(isActive);
        int color = ContextCompat.getColor(getContext(), isActive ? R.color.white : R.color.quick_settings_icon_tint);
        ((ImageView) button.findViewById(R.id.icon)).setColorFilter(color);
        ((TextView) button.findViewById(R.id.label)).setTextColor(color);
    }

    private void updateAllButtonStates() {
        if (!isAdded()) return;

        setButtonState(btnAirplane, isAirplaneModeOn());
        setButtonState(btnWifi, wifiManager != null && wifiManager.isWifiEnabled());
        setButtonState(btnBluetooth, bluetoothAdapter != null && bluetoothAdapter.isEnabled());
        setButtonState(btnFlashlight, isFlashlightOn);
        setButtonState(btnData, isMobileDataEnabled(requireContext()));
        setButtonState(btnHotspot, isHotspotActive);

        LocationManager lm = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        setButtonState(btnLocation, lm != null && (lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)));

        updateRotationState();
        updateAutoBrightnessState();
        updateSoundButtonState();

        if (toggleNotification != null) {
            isUpdatingToggleProgrammatically = true;
            toggleNotification.setChecked(isNotificationAccessGranted());
            isUpdatingToggleProgrammatically = false;
        }
    }

    private boolean isAirplaneModeOn() {
        if (getContext() == null) return false;
        return Settings.Global.getInt(requireContext().getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    private boolean isHotspotEnabled() {
        if (wifiManager == null) return false;
        try {
            Method method = wifiManager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(wifiManager);
        } catch (Exception e) {
            Log.e(TAG, "Cannot reflectively check hotspot state", e);
            return false;
        }
    }

    private void cycleRingerMode() {
        if (audioManager == null) return;
        int currentMode = audioManager.getRingerMode();
        switch (currentMode) {
            case AudioManager.RINGER_MODE_NORMAL:
                audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                break;
            case AudioManager.RINGER_MODE_SILENT:
                audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                break;
        }
        updateSoundButtonState();
    }

    private void updateSoundButtonState() {
        if (audioManager == null || !isAdded() || btnSound == null) return;
        int ringerMode = audioManager.getRingerMode();
        ImageView iconView = btnSound.findViewById(R.id.icon);
        TextView labelView = btnSound.findViewById(R.id.label);
        switch (ringerMode) {
            case AudioManager.RINGER_MODE_NORMAL:
                setButtonState(btnSound, true);
                iconView.setImageResource(R.drawable.ic_sound);
                labelView.setText("Sound");
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                setButtonState(btnSound, true);
                iconView.setImageResource(R.drawable.ic_vibrate);
                labelView.setText("Vibrate");
                break;
            case AudioManager.RINGER_MODE_SILENT:
                setButtonState(btnSound, false);
                iconView.setImageResource(R.drawable.ic_mute);
                labelView.setText("Mute");
                break;
        }
    }

    private void updateRotationState() {
        if (!isAdded()) return;
        try {
            setButtonState(btnRotate, Settings.System.getInt(getContext().getContentResolver(), Settings.System.ACCELEROMETER_ROTATION) == 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateAutoBrightnessState() {
        if (!isAdded()) return;
        try {
            boolean isAuto = Settings.System.getInt(getContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
            setButtonState(btnAutoBrightness, isAuto);
            ((TextView) btnAutoBrightness.findViewById(R.id.label)).setText(isAuto ? "Auto" : "Manual");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isNotificationAccessGranted() {
        if (getContext() == null) return false;
        return NotificationManagerCompat.getEnabledListenerPackages(requireContext()).contains(requireContext().getPackageName());
    }

    private void registerReceivers() {
        IntentFilter systemFilter = new IntentFilter();
        systemFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        systemFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        systemFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        systemFilter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
        systemFilter.addAction("android.media.VOLUME_CHANGED_ACTION");
        systemFilter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
        systemFilter.addAction(WIFI_AP_STATE_CHANGED_ACTION);
        requireContext().registerReceiver(systemStateReceiver, systemFilter);

        IntentFilter notificationFilter = new IntentFilter();
        notificationFilter.addAction(NotificationListener.ACTION_NOTIFICATION_POSTED);
        notificationFilter.addAction(NotificationListener.ACTION_NOTIFICATION_REMOVED);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(notificationReceiver, notificationFilter);
    }

    private void unregisterReceivers() {
        if (getContext() != null) {
            try {
                getContext().unregisterReceiver(systemStateReceiver);
                LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(notificationReceiver);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateNotificationUi() {
        if (notificationAdapter != null) {
            notificationAdapter.updateNotifications(activeNotifications);
        }
        boolean hasNotifications = !activeNotifications.isEmpty();
        if (notificationRecyclerView != null)
            notificationRecyclerView.setVisibility(hasNotifications ? View.VISIBLE : View.GONE);
        if (tvNoNotifications != null)
            tvNoNotifications.setVisibility(hasNotifications ? View.GONE : View.VISIBLE);
        if (btnClearAll != null)
            btnClearAll.setVisibility(hasNotifications ? View.VISIBLE : View.GONE);
    }




}