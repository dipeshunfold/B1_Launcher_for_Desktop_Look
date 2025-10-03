package com.bluelight.computer.winlauncher.prolauncher.model;

import android.appwidget.AppWidgetHost;
import android.content.Context;
import android.util.Log;

public class SafeAppWidgetHost extends AppWidgetHost {

    public SafeAppWidgetHost(Context context, int hostId) {
        super(context, hostId);
    }

    @Override
    public void stopListening() {
        try {
            super.stopListening();
        } catch (Exception e) {

            Log.e("SafeAppWidgetHost", "stopListening() failed safely", e);
        }
    }

    @Override
    public void startListening() {
        try {
            super.startListening();
        } catch (Exception e) {
            Log.e("SafeAppWidgetHost", "startListening() failed safely", e);
        }
    }
}