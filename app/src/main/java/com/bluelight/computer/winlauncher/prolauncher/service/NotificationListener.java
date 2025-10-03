package com.bluelight.computer.winlauncher.prolauncher.service;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;


public class NotificationListener extends NotificationListenerService {


    public static final String ACTION_NOTIFICATION_POSTED = "com.protheme.launcher.NOTIFICATION_POSTED";
    public static final String ACTION_NOTIFICATION_REMOVED = "com.protheme.launcher.NOTIFICATION_REMOVED";

    public static final String EXTRA_NOTIFICATION = "notification_data";
    public static final String EXTRA_ALL_NOTIFICATIONS = "all_notifications_data";
    private static final String TAG = "NotificationListener";


    public static void requestCurrentNotifications(Context context) {
        Intent intent = new Intent(context, NotificationListener.class);
        intent.setAction("REQUEST_CURRENT_NOTIFICATIONS");
        context.startService(intent);
    }


    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;
        Log.d(TAG, "onNotificationPosted: " + sbn.getPackageName());
        if (sbn.isOngoing() && !isMediaNotification(sbn)) {
            Log.d(TAG, "Ignoring ongoing notification: " + sbn.getPackageName());
            return;
        }
        broadcastNotificationChange(ACTION_NOTIFICATION_POSTED, sbn);
    }


    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn == null) return;
        Log.d(TAG, "onNotificationRemoved: " + sbn.getPackageName());
        broadcastNotificationChange(ACTION_NOTIFICATION_REMOVED, sbn);
    }


    private void broadcastNotificationChange(String action, StatusBarNotification sbn) {
        Intent intent = new Intent(action);
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_NOTIFICATION, sbn);
        intent.putExtras(bundle);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    private boolean isMediaNotification(StatusBarNotification sbn) {
        return sbn.getNotification().extras.containsKey(Notification.EXTRA_MEDIA_SESSION);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && intent.getAction() != null) {


            switch (intent.getAction()) {

                case "REQUEST_CURRENT_NOTIFICATIONS":
                    Log.d(TAG, "Received request for all current notifications.");
                    StatusBarNotification[] activeNotifications = getActiveNotifications();
                    ArrayList<Parcelable> notificationList = new ArrayList<>();
                    for (StatusBarNotification sbn : activeNotifications) {
                        if (!sbn.isOngoing() || isMediaNotification(sbn)) {
                            notificationList.add(sbn);
                        }
                    }
                    Intent broadcastIntent = new Intent(ACTION_NOTIFICATION_POSTED);
                    broadcastIntent.putParcelableArrayListExtra(EXTRA_ALL_NOTIFICATIONS, notificationList);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
                    break;


                case "CLEAR_ALL_NOTIFICATIONS":
                    Log.d(TAG, "Received request to clear all notifications.");


                    cancelAllNotifications();
                    break;
            }
        }
        return START_STICKY;
    }
}