package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.database.AppDatabase;
import com.bluelight.computer.winlauncher.prolauncher.database.AppDatabase1;
import com.bluelight.computer.winlauncher.prolauncher.database.AppItem;
import com.bluelight.computer.winlauncher.prolauncher.database.GroupedApp;
import com.bluelight.computer.winlauncher.prolauncher.databinding.FragmentAppListBinding;
import com.bluelight.computer.winlauncher.prolauncher.databinding.ItemGroupLayoutBinding;
import com.bluelight.computer.winlauncher.prolauncher.model.AppInfo;
import com.bluelight.computer.winlauncher.prolauncher.model.AppUtils;
import com.bluelight.computer.winlauncher.prolauncher.service.DeviceAdmin;
import com.bluelight.computer.winlauncher.prolauncher.ui.adapter.AllAppsAdapter;
import com.bluelight.computer.winlauncher.prolauncher.ui.adapter.GroupedAppsAdapter;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppListFragment extends Fragment implements AllAppsAdapter.AppInteractionListener {
    public static final String KEY_PROFILE_SETUP_COMPLETE = "key_profile_setup_complete";
    public static final String KEY_PROFILE_IMAGE_PATH = "key_profile_image_path";
    public static final String KEY_PROFILE_NAME = "key_profile_name";
    private static final String PREFS_NAME = "LauncherPrefs";

    private static final String ALL_DEFAULT_GROUPS_ADDED_KEY = "AllDefaultGroupsAdded_v1";
    private static final int VOICE_INPUT_REQUEST_CODE = 1001;
    ImageView iv_mic;
    private SharedPreferences prefs;
    private FragmentAppListBinding binding;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> pickProfileImageLauncher;
    private List<AppInfo> allAppsList;
    private AppDatabase1 db;
    private AppDatabase appItemDb;
    private ExecutorService executor;
    private ImageView dialogProfileImageView;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName compName;
    private Context applicationContext;

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        if (enter && nextAnim == R.anim.slide_up) {
            Animation rootFadeIn = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in_background);
            Animation contentSlideUp = AnimationUtils.loadAnimation(getContext(), R.anim.slide_up);
            if (getView() != null) {
                View content = getView().findViewById(R.id.content_container);
                content.startAnimation(contentSlideUp);
            }
            return rootFadeIn;
        } else if (!enter && nextAnim == R.anim.slide_down) {
            Animation rootFadeOut = AnimationUtils.loadAnimation(getContext(), R.anim.fade_out_background);
            Animation contentSlideDown = AnimationUtils.loadAnimation(getContext(), R.anim.slide_down);

            if (getView() != null) {
                View content = getView().findViewById(R.id.content_container);
                content.startAnimation(contentSlideDown);
            }
            return rootFadeOut;
        } else {
            return super.onCreateAnimation(transit, enter, nextAnim);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAppListBinding.inflate(inflater, container, false);
        // Removed the !isAdded() check here, as requireContext() is safe in onCreateView
        db = AppDatabase1.getDatabase(requireContext());
        appItemDb = AppDatabase.getDatabase(requireContext());
        executor = Executors.newSingleThreadExecutor();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Ensure fragment is added before using requireActivity() and requireContext()
        if (!isAdded()) {
            Log.w("AppListFragment", "Fragment not attached in onViewCreated, skipping initialization.");
            return;
        }
        devicePolicyManager = (DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
        compName = new ComponentName(requireActivity(), DeviceAdmin.class);
        applyBlurToContentContainer();

        binding.contentContainer.post(() -> {
            // No direct requireContext() here
        });
        binding.getRoot().setOnClickListener(v -> closeFragment());
        binding.contentContainer.setOnClickListener(v -> {
        });
        addDefaultAppsToGroupsIfNeeded();


        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String imageUriString = prefs.getString(KEY_PROFILE_IMAGE_PATH, null);
        String savedName = prefs.getString(KEY_PROFILE_NAME, "User");

        binding.txtUserName.setText(savedName);
        if (imageUriString != null) {
            try {
                Uri imageUri = Uri.parse(imageUriString);

                binding.ivProfile.setImageURI(imageUri);
            } catch (Exception e) {
                e.printStackTrace();

                binding.ivProfile.setImageResource(R.drawable.ic_user);
            }
        }
        setupAllAppsList();
        setupGroupedApps();
        setupClickListeners();


        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                launchGalleryForProfile();
            } else {
                if (isAdded()) { // Guard Toast
                    Toast.makeText(getContext(), "Permission denied. Cannot select image.", Toast.LENGTH_SHORT).show();
                }
            }
        });


        pickProfileImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri imageUri = result.getData().getData();
                if (imageUri != null) {
                    prefs.edit().putString(KEY_PROFILE_IMAGE_PATH, imageUri.toString()).apply();
                    if (dialogProfileImageView != null) {
                        dialogProfileImageView.setImageURI(imageUri);
                    }
                }
            }
        });
        iv_mic = view.findViewById(R.id.iv_mic);


        iv_mic.setOnClickListener(v -> {
            if (!isAdded()) { // Guard click listener execution
                Log.w("AppListFragment", "Fragment not attached, skipping iv_mic click.");
                return;
            }
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search...");

            try {
                startActivityForResult(intent, VOICE_INPUT_REQUEST_CODE);
            } catch (ActivityNotFoundException e) {
                if (isAdded()) { // Guard Toast
                    Toast.makeText(getContext(), "Voice input not supported on this device", Toast.LENGTH_SHORT).show();
                }
            }
        });
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (binding == null || !isAdded()) return; // Guard against fragment being detached

                if (binding.rvAllApps.getAdapter() instanceof AllAppsAdapter) {
                    ((AllAppsAdapter) binding.rvAllApps.getAdapter()).getFilter().filter(s);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.ivLock.setOnClickListener(v -> {
            if (!isAdded()) { // Guard click listener execution
                Log.w("AppListFragment", "Fragment not attached, skipping ivLock click.");
                return;
            }
            boolean active = devicePolicyManager.isAdminActive(compName);
            if (active) {
                devicePolicyManager.lockNow();
            } else {
                new AlertDialog.Builder(requireContext()) // Safe because guarded by isAdded()
                        .setTitle("Permission Required")
                        .setMessage("To use the screen lock feature, this app needs to be activated as a 'Device Administrator'. On the next screen, please accept the permission. Your phone might ask for a verification code for security.")
                        .setPositiveButton("Continue", (dialog, which) -> {
                            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName);
                            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "This permission is needed to allow the app to lock the screen.");
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
    }


    private void closeFragment() {
        if (isAdded() && getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    private void applyBlurToContentContainer() {
        if (binding == null || !isAdded()) return; // Initial check

        CardView contentContainer = binding.contentContainer;
        ShapeableImageView blurBackground = binding.blurBackground;

        contentContainer.post(() -> {
            if (!isAdded()) { // Crucial check within post()
                Log.w("BlurDebug", "Fragment not added during contentContainer.post for blur.");
                return;
            }
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

            // requireContext() here is safe due to the isAdded() check above
            Bitmap blurredBitmap = blurBitmap(scaledBitmap, requireContext(), 15f);
            Bitmap finalBitmap = Bitmap.createScaledBitmap(blurredBitmap, width, height, true);
            if (scaledBitmap != blurredBitmap) blurredBitmap.recycle();

            requireActivity().runOnUiThread(() -> {
                if (binding != null && isAdded()) { // Final check before UI update
                    blurBackground.setImageBitmap(finalBitmap);
                    blurBackground.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private Bitmap blurBitmap(Bitmap bitmap, Context context, float radius) {
        // Context here is passed, so no requireContext() issue unless the passed context is null
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
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (!isAdded()) { // Guard entry point
            Log.w("AppListFragment", "Fragment not attached, skipping onActivityResult.");
            return;
        }

        if (requestCode == VOICE_INPUT_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                String spokenText = result.get(0);
                binding.etSearch.setText(spokenText);
                binding.etSearch.setSelection(spokenText.length());
            }
        }
    }

    private void addDefaultAppsToGroupsIfNeeded() {
        if (!isAdded()) { // Guard entry point
            Log.w("AppListFragment", "Fragment not attached, skipping addDefaultAppsToGroupsIfNeeded.");
            return;
        }

        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(ALL_DEFAULT_GROUPS_ADDED_KEY, false)) {
            return;
        }

        // Capture application context for background task
        final Context applicationContext = requireContext().getApplicationContext();

        executor.execute(() -> {
            Map<String, Map<String, String[]>> defaultGroups = new LinkedHashMap<>();
            Map<String, String[]> lifeApps = new LinkedHashMap<>();
            lifeApps.put("Calendar", new String[]{"com.google.android.calendar", "com.samsung.android.calendar"});
            lifeApps.put("Gmail", new String[]{"com.google.android.gm"});
            lifeApps.put("Camera", new String[]{"com.google.android.camera.Camera", "com.android.camera2", "com.sec.android.app.camera"});
            lifeApps.put("Settings", new String[]{"com.android.settings"});
            lifeApps.put("File Manager", new String[]{"com.google.android.apps.nbu.files", "com.sec.android.app.myfiles", "com.android.documentsui"});
            lifeApps.put("GPay", new String[]{"com.google.android.apps.wallet"});
            lifeApps.put("Paytm", new String[]{"net.one97.paytm"});
            lifeApps.put("PhonePe", new String[]{"com.phonepe.app"});
            lifeApps.put("Messages", new String[]{"com.google.android.apps.messaging", "com.samsung.android.messaging"});
            lifeApps.put("Phone", new String[]{"com.google.android.dialer", "com.samsung.android.app.telephony"});
            lifeApps.put("Gallery", new String[]{"com.google.android.apps.photos", "com.sec.android.gallery3d", "com.android.gallery3d"});
            lifeApps.put("Calculator", new String[]{"com.google.android.calculator", "com.sec.android.app.popupcalculator"});
            lifeApps.put("WhatsApp", new String[]{"com.whatsapp"});
            lifeApps.put("Facebook", new String[]{"com.facebook.katana"});
            lifeApps.put("Instagram", new String[]{"com.instagram.android"});
            lifeApps.put("Telegram", new String[]{"org.telegram.messenger"});
            lifeApps.put("Snapchat", new String[]{"com.snapchat.android"});
            lifeApps.put("TikTok", new String[]{"com.zhiliaoapp.musically", "com.ss.android.ugc.trill"});
            lifeApps.put("Teams", new String[]{"com.microsoft.teams"});
            lifeApps.put("Skype", new String[]{"com.skype.raider"});
            defaultGroups.put("life_at_a_glance", lifeApps);
            Map<String, String[]> playApps = new LinkedHashMap<>();
            playApps.put("YouTube", new String[]{"com.google.android.youtube"});
            playApps.put("MX Player", new String[]{"com.mxtech.videoplayer.ad"});
            playApps.put("Netflix", new String[]{"com.netflix.mediaclient"});
            playApps.put("Prime Video", new String[]{"com.amazon.avod.thirdpartyclient"});
            playApps.put("Flipkart", new String[]{"com.flipkart.android"});
            playApps.put("Amazon Shopping", new String[]{"com.amazon.mShop.android.shopping"});
            playApps.put("Facebook Messenger", new String[]{"com.facebook.orca"});
            playApps.put("YouTube Music", new String[]{"com.google.android.apps.youtube.music"});
            playApps.put("Spotify", new String[]{"com.spotify.music"});
            playApps.put("Gaana", new String[]{"com.gaana"});
            playApps.put("VLC", new String[]{"org.videolan.vlc"});
            defaultGroups.put("play_and_explore", playApps);
            Map<String, String[]> utilityApps = new LinkedHashMap<>();
            utilityApps.put("Play Store", new String[]{"com.android.vending"});
            utilityApps.put("Chrome", new String[]{"com.android.chrome"});
            utilityApps.put("Google", new String[]{"com.google.android.googlequicksearchbox"});
            utilityApps.put("Maps", new String[]{"com.google.android.apps.maps"});
            defaultGroups.put("apps", utilityApps);

            // Use the captured applicationContext for PackageManager
            PackageManager pm = applicationContext.getPackageManager();

            for (Map.Entry<String, Map<String, String[]>> groupEntry : defaultGroups.entrySet()) {
                String groupKey = groupEntry.getKey();
                Map<String, String[]> appsInGroup = groupEntry.getValue();

                for (Map.Entry<String, String[]> appEntry : appsInGroup.entrySet()) {
                    for (String packageName : appEntry.getValue()) {
                        // Pass applicationContext to isAppInstalled
                        if (isAppInstalled(packageName, applicationContext)) {
                            if (db.groupedAppDao().isAppInGroup(packageName, groupKey) == 0) { // db is now guaranteed not null
                                try {
                                    // Use applicationContext for getApplicationLabel
                                    CharSequence label = pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0));
                                    GroupedApp newGroupedApp = new GroupedApp();
                                    newGroupedApp.appName = label.toString();
                                    newGroupedApp.packageName = packageName;
                                    newGroupedApp.groupName = groupKey;
                                    db.groupedAppDao().insert(newGroupedApp);
                                    break;
                                } catch (PackageManager.NameNotFoundException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                break;
                            }
                        }
                    }
                }
            }
            if (getActivity() != null) { // Check for activity before posting to UI thread
                getActivity().runOnUiThread(() -> {
                    // Check if the fragment is still added before updating UI
                    if (binding != null && isAdded()) {
                        prefs.edit().putBoolean(ALL_DEFAULT_GROUPS_ADDED_KEY, true).apply();
                        loadAppsForGroup(binding.groupLifeAtAGlance, "life_at_a_glance");
                        loadAppsForGroup(binding.groupPlayAndExplore, "play_and_explore");
                        loadAppsForGroup(binding.groupApps, "apps");
                    } else {
                        Log.w("AppListFragment", "Fragment not attached or binding null, skipping UI update after default groups added.");
                    }
                });
            }
        });
    }

    // Modified isAppInstalled to accept Context
    private boolean isAppInstalled(String packageName, Context context) {
        PackageManager pm = context.getPackageManager(); // Use the provided context
        try {
            pm.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }


    private void saveProfileImageUri(String uriString) {
        if (!isAdded()) { // Guard entry point
            Log.w("AppListFragment", "Fragment not attached, skipping saveProfileImageUri.");
            return;
        }
        SharedPreferences.Editor editor = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(KEY_PROFILE_IMAGE_PATH, uriString);
        editor.apply();
    }


    private void setupAllAppsList() {
        if (binding == null || !isAdded()) return; // Initial check

        binding.rvAllApps.setHasFixedSize(true);
        binding.rvAllApps.setItemViewCacheSize(20);
        binding.rvAllApps.setDrawingCacheEnabled(true);
        binding.rvAllApps.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        binding.rvAllApps.setItemAnimator(null);

        binding.rvAllApps.setLayoutManager(new LinearLayoutManager(getContext())); // getContext() fine here
        binding.rvAllApps.setAdapter(new AllAppsAdapter(getContext(), new ArrayList<>(), this)); // getContext() fine here

        if (AppUtils.cachedApps != null && !AppUtils.cachedApps.isEmpty()) {
            allAppsList = new ArrayList<>(AppUtils.cachedApps);
            AllAppsAdapter adapter = new AllAppsAdapter(getContext(), allAppsList, this); // getContext() fine here
            binding.rvAllApps.setAdapter(adapter);
        }

        // Capture application context for background task
        final Context applicationContext = requireContext().getApplicationContext();

        executor.execute(() -> {
            List<AppInfo> freshApps = AppUtils.getInstalledApps(applicationContext); // Use captured context
            FragmentActivity activity = getActivity();
            if (activity == null || !isAdded()) { // Check isAdded before proceeding to UI thread
                Log.w("AppListFragment", "Fragment not attached, skipping setupAllAppsList UI update.");
                return;
            }

            activity.runOnUiThread(() -> {
                if (binding == null || !isAdded()) { // Final check before UI update
                    Log.w("AppListFragment", "Fragment not attached or binding null, skipping setupAllAppsList UI update.");
                    return;
                }

                boolean listHasChanged = AppUtils.cachedApps == null || AppUtils.cachedApps.size() != freshApps.size();
                if (listHasChanged) {
                    AppUtils.cachedApps = new ArrayList<>(freshApps);
                    allAppsList = new ArrayList<>(freshApps);
                    AllAppsAdapter adapter = new AllAppsAdapter(getContext(), allAppsList, this); // getContext() fine here
                    binding.rvAllApps.setAdapter(adapter);
                }
            });
        });
    }

    private void setupGroupedApps() {
        if (!isAdded()) return; // Guard entry point
        setupSingleGroup(binding.groupLifeAtAGlance, "Life at a glance", "life_at_a_glance");
        setupSingleGroup(binding.groupPlayAndExplore, "Play and explore", "play_and_explore");
        setupSingleGroup(binding.groupApps, "Apps", "apps");
    }

    private void setupSingleGroup(ItemGroupLayoutBinding groupBinding, String title, String groupKey) {
        if (!isAdded()) return; // Guard entry point
        groupBinding.tvGroupTitle.setText(title);
        groupBinding.rvGroupedApps.setHasFixedSize(true);
        groupBinding.rvGroupedApps.setItemViewCacheSize(20);
        groupBinding.rvGroupedApps.setDrawingCacheEnabled(true);
        groupBinding.rvGroupedApps.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        groupBinding.rvGroupedApps.setItemAnimator(null);
        groupBinding.rvGroupedApps.setLayoutManager(new GridLayoutManager(getContext(), 2)); // getContext() fine here
        loadAppsForGroup(groupBinding, groupKey);
        groupBinding.ivAddToGroup.setOnClickListener(v -> {
            if (!isAdded()) { // Guard click listener execution
                Log.w("AppListFragment", "Fragment not attached, skipping ivAddToGroup click.");
                return;
            }
            if (allAppsList != null && !allAppsList.isEmpty()) {
                showAddAppDialog(groupKey, groupBinding);
            } else {
                Toast.makeText(getContext(), "App list not loaded yet. Please wait.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAppsForGroup(ItemGroupLayoutBinding groupBinding, String groupKey) {
        // Capture application context for background task
        final Context applicationContext = requireContext().getApplicationContext();

        executor.execute(() -> {
            List<GroupedApp> dbApps = db.groupedAppDao().getAppsForGroup(groupKey);
            List<AppInfo> appInfoList = new ArrayList<>();
            // Use captured applicationContext
            PackageManager pm = applicationContext.getPackageManager();
            for (GroupedApp dbApp : dbApps) {
                try {
                    AppInfo info = new AppInfo();
                    info.packageName = dbApp.packageName;
                    info.label = pm.getApplicationLabel(pm.getApplicationInfo(dbApp.packageName, 0));
                    info.icon = pm.getApplicationIcon(dbApp.packageName);
                    appInfoList.add(info);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
            if (getActivity() != null) { // Check for activity before posting to UI thread
                getActivity().runOnUiThread(() -> {
                    if (binding == null || !isAdded()) { // Final check before UI update
                        Log.w("AppListFragment", "Fragment not attached or binding null, skipping loadAppsForGroup UI update.");
                        return;
                    }
                    if (groupBinding.getRoot().isAttachedToWindow()) {
                        GroupedAppsAdapter adapter = new GroupedAppsAdapter(getContext(), appInfoList, groupKey); // getContext() fine here
                        groupBinding.rvGroupedApps.setAdapter(adapter);
                    }
                });
            }
        });
    }


    private void showAddAppDialog(String groupKey, ItemGroupLayoutBinding groupBinding) {
        if (!isAdded()) { // Guard entry point
            Log.w("AppListFragment", "Fragment not attached, skipping showAddAppDialog.");
            return;
        }

        final Dialog dialog = new Dialog(requireContext()); // Safe because guarded by isAdded()
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);


        dialog.setCanceledOnTouchOutside(true);

        dialog.setContentView(R.layout.dialog_add_app);


        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(lp);
        }


        TextView dialogTitle = dialog.findViewById(R.id.dialog_title);
        EditText searchField = dialog.findViewById(R.id.et_search_apps);
        RecyclerView recyclerView = dialog.findViewById(R.id.rv_apps);
        Button cancelButton = dialog.findViewById(R.id.button_cancel);

        dialogTitle.setText("Add App to " + groupBinding.tvGroupTitle.getText());

        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3)); // Safe

        AppSelectionAdapter adapter = new AppSelectionAdapter(allAppsList, selectedApp -> {
            if (selectedApp != null) {
                if (isAdded()) { // Guard execution of addAppToGroupInDb
                    addAppToGroupInDb(selectedApp, groupKey, groupBinding);
                } else {
                    Log.w("AppListFragment", "Fragment not attached, skipping addAppToGroupInDb from dialog callback.");
                }
            }
            dialog.dismiss();
        });
        recyclerView.setAdapter(adapter);

        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());


        dialog.show();
    }

    private void addAppToGroupInDb(AppInfo appInfo, String groupKey, ItemGroupLayoutBinding groupBinding) {
        // Capture application context for background task
        final Context applicationContext = requireContext().getApplicationContext();

        executor.execute(() -> {
            int existingCount = db.groupedAppDao().isAppInGroup(appInfo.packageName.toString(), groupKey);
            if (getActivity() != null) { // Check for activity before posting to UI thread
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) { // Crucial check before any UI interaction
                        Log.w("AppListFragment", "Fragment not attached, skipping addAppToGroupInDb UI update (outer).");
                        return;
                    }
                    if (existingCount > 0) {
                        Toast.makeText(getContext(), appInfo.label + " is already in this group.", Toast.LENGTH_SHORT).show();
                    } else {
                        executor.execute(() -> {
                            GroupedApp newGroupedApp = new GroupedApp();
                            newGroupedApp.appName = appInfo.label.toString();
                            newGroupedApp.packageName = appInfo.packageName.toString();
                            newGroupedApp.groupName = groupKey;
                            db.groupedAppDao().insert(newGroupedApp);

                            if (getActivity() != null) { // Check for activity before posting to UI thread
                                getActivity().runOnUiThread(() -> {
                                    if (!isAdded()) { // Crucial check before any UI interaction
                                        Log.w("AppListFragment", "Fragment not attached, skipping addAppToGroupInDb UI update (inner).");
                                        return;
                                    }
                                    Toast.makeText(getContext(), appInfo.label + " added.", Toast.LENGTH_SHORT).show();
                                    loadAppsForGroup(groupBinding, groupKey);
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    private void setupClickListeners() {
        if (!isAdded()) return; // Guard entry point

        binding.lnrUser.setOnClickListener(
                v -> {
                    if (!isAdded()) { // Guard click listener execution
                        Log.w("AppListFragment", "Fragment not attached, skipping lnrUser click.");
                        return;
                    }
                    checkAndShowProfileDialog();
                });

        binding.ivPower.setOnClickListener(v -> {
            if (!isAdded()) { // Guard click listener execution
                Log.w("AppListFragment", "Fragment not attached, skipping ivPower click.");
                return;
            }

            final Dialog powerMenuDialog = new Dialog(requireContext()); // Safe
            powerMenuDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            powerMenuDialog.setContentView(R.layout.dialog_power_menu);

            LinearLayout closeOption = powerMenuDialog.findViewById(R.id.dialog_option_close);
            LinearLayout resetOption = powerMenuDialog.findViewById(R.id.dialog_option_reset);

            closeOption.setOnClickListener(closeView -> {
                powerMenuDialog.dismiss();
                // requireActivity() is safe here as it's a direct user interaction from an attached fragment
                requireActivity().finishAffinity();
            });

            resetOption.setOnClickListener(resetView -> {
                powerMenuDialog.dismiss();

                final Dialog confirmationDialog = new Dialog(requireContext()); // Safe
                confirmationDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                confirmationDialog.setContentView(R.layout.dialog_reset_confirmation);

                TextView cancelButton = confirmationDialog.findViewById(R.id.button_cancel);
                TextView confirmResetButton = confirmationDialog.findViewById(R.id.button_confirm_reset);


                cancelButton.setOnClickListener(cancelView -> confirmationDialog.dismiss());


                confirmResetButton.setOnClickListener(confirmView -> {
                    confirmationDialog.dismiss();

                    // Capture application context for background task
                    final Context applicationContextForReset = requireContext().getApplicationContext(); // Safe

                    executor.execute(() -> {
                        SharedPreferences prefs = applicationContextForReset.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        prefs.edit().clear().commit();

                        AppDatabase db = AppDatabase.getDatabase(applicationContextForReset);
                        db.appItemDao().deleteAllItems();


                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (!isAdded()) { // Crucial check before UI operations
                                Log.w("AppListFragment", "Fragment not attached, skipping restart after reset.");
                                return;
                            }

                            // Use the captured applicationContextForReset for package manager
                            PackageManager packageManager = applicationContextForReset.getPackageManager();
                            Intent restartIntent = packageManager.getLaunchIntentForPackage(applicationContextForReset.getPackageName());

                            if (restartIntent == null) {
                                // Fallback toast should also be guarded with isAdded() if using getContext()
                                Toast.makeText(applicationContextForReset, "Error: Could not find launch intent to restart.", Toast.LENGTH_LONG).show();
                                return;
                            }
                            restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            applicationContextForReset.startActivity(restartIntent);
                            System.exit(0);
                        });
                    });
                    if (isAdded()) { // Guard Toast
                        Toast.makeText(getContext(), "Resetting data...", Toast.LENGTH_SHORT).show();
                    }
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


        binding.ivSettings.setOnClickListener(v -> {
            if (!isAdded()) { // Guard click listener execution
                Log.w("AppListFragment", "Fragment not attached, skipping ivSettings click.");
                return;
            }
            SettingsFragment settingsFragment = new SettingsFragment();
            FragmentTransaction ft = requireActivity().getSupportFragmentManager().beginTransaction(); // Safe
            ft.setCustomAnimations(R.anim.slide_up, R.anim.fade_out, R.anim.fade_in, R.anim.slide_down);
            ft.replace(R.id.fragment_container, settingsFragment);
            ft.addToBackStack(null);
            ft.commit();
        });

        binding.ivFileManager.setOnClickListener(v -> {
            if (!isAdded()) { // Guard click listener execution
                Log.w("AppListFragment", "Fragment not attached, skipping ivFileManager click.");
                return;
            }
            HomeFileFragment homeFileFragment = new HomeFileFragment();
            FragmentTransaction ft = requireActivity().getSupportFragmentManager().beginTransaction(); // Safe
            ft.setCustomAnimations(R.anim.slide_up, R.anim.fade_out, R.anim.fade_in, R.anim.slide_down);
            ft.replace(R.id.fragment_container, homeFileFragment);
            ft.addToBackStack(null);
            ft.commit();
        });
    }

    private void checkAndShowProfileDialog() {
        if (!isAdded()) { // Crucial check at method entry point
            Log.w("AppListFragment", "Fragment not attached, cannot show profile dialog.");
            return;
        }

        Context context = requireContext(); // Safe because guarded by isAdded()
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_set_profile, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView).setCancelable(false);
        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialogProfileImageView = dialogView.findViewById(R.id.iv_profile_image);
        EditText etProfileName = dialogView.findViewById(R.id.et_profile_name);
        Button btnSaveProfile = dialogView.findViewById(R.id.btn_save_profile);

        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE); // Safe

        String savedName = prefs.getString(KEY_PROFILE_NAME, "");
        etProfileName.setText(savedName);
        String savedImagePath = prefs.getString(KEY_PROFILE_IMAGE_PATH, null);
        if (savedImagePath != null) {
            try {
                Uri imageUri = Uri.parse(savedImagePath);
                dialogProfileImageView.setImageURI(imageUri);
            } catch (Exception e) {
                dialogProfileImageView.setImageResource(R.drawable.ic_user);
            }
        }
        dialogProfileImageView.setOnClickListener(v -> {
            if (!isAdded()) { // Guard click listener execution
                Log.w("AppListFragment", "Fragment not attached, skipping handleImagePick from dialog.");
                return;
            }
            handleImagePick();
        });

        btnSaveProfile.setOnClickListener(v -> {
            if (!isAdded()) { // Guard click listener execution


                Log.w("AppListFragment", "Fragment not attached, skipping save profile from dialog.");
                return;
            }
            String name = etProfileName.getText().toString().trim();
            binding.txtUserName.setText(name);
            if (name.isEmpty()) {
                etProfileName.setError("Name is required");
                return;
            }
            prefs.edit()
                    .putString(KEY_PROFILE_NAME, name)
                    .putBoolean(KEY_PROFILE_SETUP_COMPLETE, true)
                    .apply();
            Toast.makeText(context, "Profile Saved!", Toast.LENGTH_SHORT).show(); // `context` from dialog creation is usually fine
            dialog.dismiss();
        });
        dialog.show();
    }

    private void handleImagePick() {
        if (!isAdded()) { // Crucial check at method entry point
            Log.w("AppListFragment", "Fragment not attached, skipping handleImagePick.");
            return;
        }
        String permission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) { // Safe
            launchGalleryForProfile();
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    private void launchGalleryForProfile() {
        if (!isAdded()) { // Guard entry point
            Log.w("AppListFragment", "Fragment not attached, skipping launchGalleryForProfile.");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickProfileImageLauncher.launch(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    @Override
    public void onAppLongPressed(View anchorView, AppInfo appInfo) {
        if (!isAdded()) { // Guard entry point
            Log.w("AppListFragment", "Fragment not attached, skipping onAppLongPressed.");
            return;
        }
        showAppOptionsMenu(anchorView, appInfo);
    }

    private void showAppOptionsMenu(View anchorView, AppInfo appInfo) {
        if (!isAdded()) { // Crucial check at method entry point
            Log.w("AppListFragment", "Fragment not attached, skipping showAppOptionsMenu.");
            return;
        }

        LayoutInflater inflater = (LayoutInflater) requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE); // Safe
        View customMenuView = inflater.inflate(R.layout.popup_custom_menu, null);

        PopupWindow popupWindow = new PopupWindow(
                customMenuView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(20);

        LinearLayout menuItemsContainer = customMenuView.findViewById(R.id.menu_items_container);

        addMenuItem(menuItemsContainer, R.drawable.ic_infos, "App Info", () -> {
            if (!isAdded()) { // Guard action execution
                Log.w("AppListFragment", "Fragment not attached, skipping 'App Info' action.");
                return;
            }
            try {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + appInfo.packageName));
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                if (isAdded()) { // Guard Toast
                    Toast.makeText(getContext(), "Could not open app info.", Toast.LENGTH_SHORT).show();
                }
            }
            popupWindow.dismiss();
        });

        addMenuItem(menuItemsContainer, R.drawable.ic_uninstall, "Uninstall", () -> {
            if (!isAdded()) { // Guard action execution
                Log.w("AppListFragment", "Fragment not attached, skipping 'Uninstall' action.");
                return;
            }
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:" + appInfo.packageName));
            startActivity(intent);
            popupWindow.dismiss();
        });

        executor.execute(() -> {
            AppItem existingItem = appItemDb.appItemDao().findItemByPackageNameNow(appInfo.packageName.toString());
            boolean isPinned = existingItem != null && existingItem.isPinned;

            if (getActivity() != null) { // Check for activity before posting to UI thread
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) { // Crucial check before adding menu item
                        Log.w("AppListFragment", "Fragment not attached, skipping pin/unpin menu item creation.");
                        return;
                    }
                    String pinTitle = isPinned ? "Unpin from Taskbar" : "Pin to Taskbar";
                    int pinIcon = isPinned ? R.drawable.ic_unpin : R.drawable.ic_pin;
                    addMenuItem(menuItemsContainer, pinIcon, pinTitle, () -> {
                        if (!isAdded()) { // Guard action execution
                            Log.w("AppListFragment", "Fragment not attached, skipping 'Pin/Unpin' action.");
                            return;
                        }
                        togglePinStatus(appInfo);
                        popupWindow.dismiss();
                    });
                });
            }
        });

        popupWindow.showAsDropDown(anchorView);
    }

    private void addMenuItem(LinearLayout container, int iconResId, String title, Runnable action) {
        if (!isAdded()) { // Guard entry point
            Log.w("AppListFragment", "Fragment not attached, skipping addMenuItem.");
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(getContext()); // Safe
        View itemView = inflater.inflate(R.layout.item_custom_menu, container, false);

        ImageView icon = itemView.findViewById(R.id.menu_item_icon);
        TextView titleView = itemView.findViewById(R.id.menu_item_title);

        icon.setImageResource(iconResId);
        titleView.setText(title);

        // The 'action' itself needs its own isAdded() check if it performs UI operations
        itemView.setOnClickListener(v -> action.run());

        container.addView(itemView);
    }

    private void togglePinStatus(AppInfo appInfo) {
        if (!isAdded()) { // Guard entry point
            Log.w("AppListFragment", "Fragment not attached, skipping togglePinStatus.");
            return;
        }
        // Capture application context for background execution
        final Context applicationContext = requireContext().getApplicationContext(); // Safe

        executor.execute(() -> {
            AppItem existingItem = appItemDb.appItemDao().findItemByPackageNameNow(appInfo.packageName.toString());
            final String message;

            if (existingItem != null) {
                existingItem.isPinned = !existingItem.isPinned;
                appItemDb.appItemDao().update(existingItem);
                message = existingItem.isPinned ? "'" + appInfo.label + "' pinned to taskbar" : "'" + appInfo.label + "' unpinned from taskbar";
            } else {
                AppItem newItem = new AppItem();
                newItem.packageName = appInfo.packageName.toString();
                newItem.originalName = appInfo.label.toString();
                newItem.name = appInfo.label.toString();
                newItem.isPinned = true;
                newItem.page = -1;
                newItem.row = -1;
                newItem.col = -1;
                newItem.type = AppItem.Type.APP;
                appItemDb.appItemDao().insert(newItem);
                message = "'" + appInfo.label + "' pinned to taskbar";
            }

            if (getActivity() != null) { // Check for activity before posting to UI thread
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) { // Crucial check before Toast
                        Log.w("AppListFragment", "Fragment not attached, skipping togglePinStatus UI update.");
                        return;
                    }
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}