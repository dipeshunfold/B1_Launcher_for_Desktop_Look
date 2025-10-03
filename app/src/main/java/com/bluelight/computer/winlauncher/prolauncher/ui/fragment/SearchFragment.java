package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.apicalling.GoogleSuggestApi;
import com.bluelight.computer.winlauncher.prolauncher.apicalling.RetrofitClient;
import com.bluelight.computer.winlauncher.prolauncher.ui.adapter.SearchAdapter;
import com.google.android.material.imageview.ShapeableImageView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment {

    private static final int VOICE_INPUT_REQUEST_CODE = 1001;

    private SearchAdapter searchAdapter;
    private RecyclerView recyclerView;
    private EditText etSearch;
    private ImageView ivMic;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        applyBlurToContentContainer();
        View rootLayout = view.findViewById(R.id.root_layout);
        View contentContainer = view.findViewById(R.id.content_container);


        rootLayout.setOnClickListener(v -> closeFragment());
        contentContainer.setOnClickListener(v -> {
        });

        recyclerView = view.findViewById(R.id.recycler_search_results);
        etSearch = view.findViewById(R.id.et_search);
        ivMic = view.findViewById(R.id.iv_mic);


        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        searchAdapter = new SearchAdapter(new ArrayList<>(), suggestion -> {
            String url = "https://www.google.com/search?q=" + Uri.encode(suggestion);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });
        recyclerView.setAdapter(searchAdapter);

        etSearch.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String query = etSearch.getText().toString().trim();
                if (!query.isEmpty()) {
                    String url = "https://www.google.com/search?q=" + Uri.encode(query);
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                }
                return true;
            }
            return false;
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    fetchSuggestions(s.toString());
                } else {
                    searchAdapter.updateList(new ArrayList<>());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        ivMic.setOnClickListener(v -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search...");

            try {
                startActivityForResult(intent, VOICE_INPUT_REQUEST_CODE);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(getContext(), "Voice input not supported on this device", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyBlurToContentContainer() {


        CardView contentContainer = getView().findViewById(R.id.content_container);
        ShapeableImageView blurBackground = getView().findViewById(R.id.blur_background);

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


    private void closeFragment() {
        if (isAdded() && getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VOICE_INPUT_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                String spokenText = result.get(0);
                etSearch.setText(spokenText);
                etSearch.setSelection(spokenText.length());
                String url = "https://www.google.com/search?q=" + Uri.encode(spokenText);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
        }
    }

    private void fetchSuggestions(String query) {
        GoogleSuggestApi api = RetrofitClient.getClient().create(GoogleSuggestApi.class);
        Call<String> call = api.getSuggestions(query);

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray jsonArray = new JSONArray(response.body());
                        JSONArray suggestions = jsonArray.getJSONArray(1);
                        List<String> suggestionList = new ArrayList<>();
                        for (int i = 0; i < suggestions.length(); i++) {
                            suggestionList.add(suggestions.getString(i));
                        }
                        searchAdapter.updateList(suggestionList);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Log.e("SearchFragment", "API call failed: " + t.getMessage());
            }
        });
    }
}
