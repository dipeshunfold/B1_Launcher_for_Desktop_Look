package com.bluelight.computer.winlauncher.prolauncher.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bluelight.computer.winlauncher.prolauncher.R;

public class JavaIntroSlideFragment extends Fragment {
    private static final String ARG_IMAGE_RES_ID = "image_res_id";
    private static final String ARG_TITLE = "title";
    private static final String ARG_DESCRIPTION = "description";

    public static JavaIntroSlideFragment newInstance(int imageResId, int titleResId, int descriptionResId) {
        JavaIntroSlideFragment fragment = new JavaIntroSlideFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_IMAGE_RES_ID, imageResId);
        args.putInt(ARG_TITLE, titleResId);
        args.putInt(ARG_DESCRIPTION, descriptionResId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_intro_slide, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (getArguments() != null) {
            int imageResId = getArguments().getInt(ARG_IMAGE_RES_ID);
            int titleResId = getArguments().getInt(ARG_TITLE);
            int descriptionResId = getArguments().getInt(ARG_DESCRIPTION);

            ImageView imageView = view.findViewById(R.id.imageView);
            TextView titleText = view.findViewById(R.id.titleText);
            TextView descriptionText = view.findViewById(R.id.descriptionText);

            imageView.setImageResource(imageResId);
            titleText.setText(titleResId);
            descriptionText.setText(descriptionResId);
        }
    }
}
