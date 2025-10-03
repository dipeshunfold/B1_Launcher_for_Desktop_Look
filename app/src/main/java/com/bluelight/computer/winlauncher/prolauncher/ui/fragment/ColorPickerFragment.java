package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.ui.adapter.BackgroundColorAdapter;
import com.bluelight.computer.winlauncher.prolauncher.ui.adapter.TextColorAdapter;

import java.util.Arrays;
import java.util.List;

public class ColorPickerFragment extends Fragment {
    public int currentBgColor;
    private OnColorSelectedListener listener;
    private SharedPreferences prefs;
    private int currentTextColor;


    public static ColorPickerFragment newInstance() {
        return new ColorPickerFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnColorSelectedListener) {
            listener = (OnColorSelectedListener) context;
        } else {
            throw new RuntimeException(context + " must implement OnColorSelectedListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_color_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = requireActivity().getSharedPreferences("LauncherPrefs", Context.MODE_PRIVATE);
        currentBgColor = prefs.getInt("taskbar_bg_color", ContextCompat.getColor(requireContext(), R.color.white));
        currentTextColor = prefs.getInt("taskbar_text_color", ContextCompat.getColor(requireContext(), R.color.black));
        view.findViewById(R.id.back_button).setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        view.findViewById(R.id.set_default_button).setOnClickListener(v -> {
            if (listener != null) {
                listener.onSetDefault();
            }
            requireActivity().getSupportFragmentManager().beginTransaction().detach(this).attach(this).commit();
        });

        setupTextColorPicker(view);
        setupBackgroundColorPicker(view);


        FrameLayout rootLayout = view.findViewById(R.id.root_layout);
        LinearLayout contentContainer = view.findViewById(R.id.content_container);
        rootLayout.setOnClickListener(v -> closeFragment());
        contentContainer.setOnClickListener(v -> {
        });
    }

    private void closeFragment() {
        if (isAdded() && getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }


    private void setupTextColorPicker(View view) {
        RecyclerView textColorRecyclerView = view.findViewById(R.id.text_color_recycler_view);
        textColorRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        TextColorAdapter textColorAdapter = new TextColorAdapter(currentTextColor, color -> {
            currentTextColor = color;
            if (listener != null) {
                listener.onColorSelected(currentBgColor, currentTextColor);
            }
        });
        textColorRecyclerView.setAdapter(textColorAdapter);
    }

    private void setupBackgroundColorPicker(View view) {
        RecyclerView bgColorRecyclerView = view.findViewById(R.id.background_color_recycler_view);
        bgColorRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 5));
        BackgroundColorAdapter bgColorAdapter = new BackgroundColorAdapter(getContext(), getBackgroundColors(), currentBgColor, color -> {
            currentBgColor = color;
            if (listener != null) {
                listener.onColorSelected(currentBgColor, currentTextColor);
            }
        });
        bgColorRecyclerView.setAdapter(bgColorAdapter);
    }


    private List<Integer> getBackgroundColors() {
        return Arrays.asList(
                ContextCompat.getColor(requireContext(), R.color.picker_img_yellow),
                ContextCompat.getColor(requireContext(), R.color.picker_img_light_orange),
                ContextCompat.getColor(requireContext(), R.color.picker_img_orange),
                ContextCompat.getColor(requireContext(), R.color.picker_img_burnt_orange),
                ContextCompat.getColor(requireContext(), R.color.picker_img_red_orange),
                ContextCompat.getColor(requireContext(), R.color.picker_img_salmon),
                ContextCompat.getColor(requireContext(), R.color.picker_img_red),
                ContextCompat.getColor(requireContext(), R.color.picker_img_bright_red),
                ContextCompat.getColor(requireContext(), R.color.picker_img_cherry_red),
                ContextCompat.getColor(requireContext(), R.color.picker_img_deep_red),
                ContextCompat.getColor(requireContext(), R.color.picker_img_hot_pink),
                ContextCompat.getColor(requireContext(), R.color.picker_img_magenta),
                ContextCompat.getColor(requireContext(), R.color.picker_img_bright_pink),
                ContextCompat.getColor(requireContext(), R.color.picker_img_dark_pink),
                ContextCompat.getColor(requireContext(), R.color.picker_img_purple_pink),
                ContextCompat.getColor(requireContext(), R.color.picker_img_purple),
                ContextCompat.getColor(requireContext(), R.color.picker_img_bright_blue),
                ContextCompat.getColor(requireContext(), R.color.picker_img_periwinkle),
                ContextCompat.getColor(requireContext(), R.color.picker_img_light_purple),
                ContextCompat.getColor(requireContext(), R.color.picker_img_deep_purple_blue),
                ContextCompat.getColor(requireContext(), R.color.picker_img_dark_purple),
                ContextCompat.getColor(requireContext(), R.color.picker_img_indigo),
                ContextCompat.getColor(requireContext(), R.color.picker_img_deep_purple),
                ContextCompat.getColor(requireContext(), R.color.picker_img_blue_violet),
                ContextCompat.getColor(requireContext(), R.color.picker_img_dark_blue),
                ContextCompat.getColor(requireContext(), R.color.picker_img_teal),
                ContextCompat.getColor(requireContext(), R.color.picker_img_dark_teal),
                ContextCompat.getColor(requireContext(), R.color.picker_img_cyan_teal),
                ContextCompat.getColor(requireContext(), R.color.picker_img_dark_green),
                ContextCompat.getColor(requireContext(), R.color.picker_img_green),
                ContextCompat.getColor(requireContext(), R.color.picker_img_bright_green),
                ContextCompat.getColor(requireContext(), R.color.picker_img_gray),
                ContextCompat.getColor(requireContext(), R.color.picker_img_dark_gray),
                ContextCompat.getColor(requireContext(), R.color.picker_img_olive),
                ContextCompat.getColor(requireContext(), R.color.picker_img_deep_olive),


                ContextCompat.getColor(requireContext(), R.color.picker_new_slate_blue),
                ContextCompat.getColor(requireContext(), R.color.picker_new_muted_green_1),
                ContextCompat.getColor(requireContext(), R.color.picker_new_muted_green_2),
                ContextCompat.getColor(requireContext(), R.color.picker_new_olive_green),
                ContextCompat.getColor(requireContext(), R.color.picker_new_forest_green),
                ContextCompat.getColor(requireContext(), R.color.picker_new_charcoal),
                ContextCompat.getColor(requireContext(), R.color.picker_new_dark_olive),
                ContextCompat.getColor(requireContext(), R.color.picker_new_deep_olive),
                ContextCompat.getColor(requireContext(), R.color.picker_new_brown_olive),
                ContextCompat.getColor(requireContext(), R.color.picker_new_black),
                ContextCompat.getColor(requireContext(), R.color.picker_new_dark_gray),
                ContextCompat.getColor(requireContext(), R.color.picker_new_olive_gray),
                ContextCompat.getColor(requireContext(), R.color.picker_new_deep_charcoal),
                ContextCompat.getColor(requireContext(), R.color.picker_new_muted_olive),
                ContextCompat.getColor(requireContext(), R.color.picker_new_dark_charcoal),
                ContextCompat.getColor(requireContext(), R.color.picker_new_gray),
                ContextCompat.getColor(requireContext(), R.color.picker_new_silver),
                ContextCompat.getColor(requireContext(), R.color.picker_new_light_gray),
                ContextCompat.getColor(requireContext(), R.color.picker_new_off_white),
                ContextCompat.getColor(requireContext(), R.color.picker_new_white)


        );
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }


    public interface OnColorSelectedListener {
        void onColorSelected(int backgroundColor, int textColor);

        void onSetDefault();
    }
}