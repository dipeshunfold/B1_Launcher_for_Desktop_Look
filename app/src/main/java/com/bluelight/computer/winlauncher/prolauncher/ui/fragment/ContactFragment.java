package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.model.ContactModel;
import com.bluelight.computer.winlauncher.prolauncher.ui.adapter.ContactAdapter;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ContactFragment extends Fragment {
    private static final int REQUEST_READ_CONTACTS = 1;
    private static final int REQUEST_CODE_SPEECH_INPUT = 1001;
    private final List<ContactModel> allContacts = new ArrayList<>();
    private final List<ContactModel> filteredContacts = new ArrayList<>();
    private RecyclerView recyclerView;
    private EditText etSearch;
    private ImageView ivMic;
    private ContactAdapter adapter;

    private LinearLayout permissionLayout;
    private Button btnGrantPermission;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.fragment_contact_list, container, false);
        View rootLayout = view.findViewById(R.id.root_layout);
        applyBlurToContentContainer(view);
        View contentContainer = view.findViewById(R.id.content_container);

        rootLayout.setOnClickListener(v -> closeFragment());
        contentContainer.setOnClickListener(v -> {
        });
        recyclerView = view.findViewById(R.id.recyclerContacts);
        etSearch = view.findViewById(R.id.et_search);
        ivMic = view.findViewById(R.id.iv_mic);
        permissionLayout = view.findViewById(R.id.permission_layout);
        btnGrantPermission = view.findViewById(R.id.btn_grant_permission);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        setupSearchAndMic();

        btnGrantPermission.setOnClickListener(v -> handleGrantPermissionClick());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUiBasedOnPermission();
    }

    private void updateUiBasedOnPermission() {
        if (getContext() == null) return;

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            permissionLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            etSearch.setVisibility(View.VISIBLE);
            ivMic.setVisibility(View.VISIBLE);
            loadAndDisplayContacts();
        } else {
            permissionLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            etSearch.setVisibility(View.GONE);
            ivMic.setVisibility(View.GONE);
        }
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

    private void handleGrantPermissionClick() {
        if (getActivity() == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
                requestContactPermission();
            } else {

                requestContactPermission();
            }
        } else {
            requestContactPermission();
        }
    }

    private void requestContactPermission() {
        if (getActivity() != null) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateUiBasedOnPermission();
            } else {
                Toast.makeText(getContext(), "Permission is required to view contacts.", Toast.LENGTH_LONG).show();

                if (getActivity() != null && !ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.READ_CONTACTS)) {
                    showSettingsRedirectDialog();
                }
            }
        }
    }

    private void showSettingsRedirectDialog() {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Permission Required")
                .setMessage("You have permanently denied the contacts permission. To view contacts, please tap 'Go to Settings' and enable the Contacts permission for this app.")
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", requireActivity().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void setupSearchAndMic() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterContacts(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        ivMic.setOnClickListener(v -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak contact name");
            try {
                startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(getContext(), "Voice search not supported", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void closeFragment() {
        if (isAdded() && getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    private void loadAndDisplayContacts() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            Context context = getContext();
            if (context == null) return;
            final List<ContactModel> loadedContacts = loadContacts(context);
            handler.post(() -> {
                if (!isAdded()) return;
                allContacts.clear();
                allContacts.addAll(loadedContacts);
                filterContacts(etSearch.getText().toString());
            });
        });
    }

    private List<ContactModel> loadContacts(Context context) {
        List<ContactModel> list = new ArrayList<>();
        Set<String> seenNumbers = new HashSet<>();
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = cr.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
                    },
                    null,
                    null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            );
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    String photoUri = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI));
                    if (name != null && number != null) {
                        String cleanNumber = number.replaceAll("[^0-9+]", "");
                        if (!seenNumbers.contains(cleanNumber)) {
                            seenNumbers.add(cleanNumber);
                            list.add(new ContactModel(name, cleanNumber, photoUri));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("ContactFragment", "Error loading contacts: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }

    private void filterContacts(String query) {
        filteredContacts.clear();
        if (query.isEmpty()) {
            filteredContacts.addAll(allContacts);
        } else {
            String lower = query.toLowerCase();
            for (ContactModel contact : allContacts) {
                if (contact.getName().toLowerCase().contains(lower)) {
                    filteredContacts.add(contact);
                }
            }
        }
        if (recyclerView == null) return;
        if (adapter == null) {
            adapter = new ContactAdapter(filteredContacts, number -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Could not open dialer.", Toast.LENGTH_SHORT).show();
                }
            });
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateList(filteredContacts);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                etSearch.setText(results.get(0));
                etSearch.setSelection(etSearch.getText().length());
            }
        }
    }

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
}