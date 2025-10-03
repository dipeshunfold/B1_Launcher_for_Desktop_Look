package com.bluelight.computer.winlauncher.prolauncher.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.ui.fragment.JavaIntroSlideFragment;

public class IntroPagerAdapter extends FragmentStateAdapter {
    
    private static final int[] IMAGE_RES_IDS = {
            R.drawable.img_intro1,
            R.drawable.img_intro2,
            R.drawable.img_intro3
    };
    
    private static final int[] TITLES = {
            R.string.intro_title_1,
            R.string.intro_title_2,
            R.string.intro_title_3
    };
    
    private static final int[] DESCRIPTIONS = {
            R.string.intro_desc_1,
            R.string.intro_desc_2,
            R.string.intro_desc_3
    };

    public IntroPagerAdapter(FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return JavaIntroSlideFragment.newInstance(
                IMAGE_RES_IDS[position],
                TITLES[position],
                DESCRIPTIONS[position]
        );
    }

    @Override
    public int getItemCount() {
        return IMAGE_RES_IDS.length;
    }
}
