package com.bluelight.computer.winlauncher.prolauncher.ui.adapter;

import android.graphics.Color;

public class ColorGenerator {


    public static final int[] MATERIAL_COLORS = {
            Color.parseColor("#0F81D2"),
            Color.parseColor("#FF4B77"),
            Color.parseColor("#C551FF"),
            Color.parseColor("#FF8233"),
            Color.parseColor("#1DBE33"),

    };


    public static int getColor(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return MATERIAL_COLORS[0];
        }


        return MATERIAL_COLORS[Math.abs(identifier.hashCode()) % MATERIAL_COLORS.length];
    }
}