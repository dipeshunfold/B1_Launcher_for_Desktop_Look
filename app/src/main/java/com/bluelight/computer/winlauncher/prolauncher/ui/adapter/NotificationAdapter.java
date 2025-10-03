package com.bluelight.computer.winlauncher.prolauncher.ui.adapter;

import android.app.ActivityOptions;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private final Context context;

    private final List<StatusBarNotification> notifications = new ArrayList<>();

    public NotificationAdapter(Context context) {
        this.context = context;
    }

    public static String getNotificationTitle(Bundle extras) {

        CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
        if (!TextUtils.isEmpty(title)) {
            return title.toString();
        }
        CharSequence bigTitle = extras.getCharSequence(Notification.EXTRA_TITLE_BIG);
        if (!TextUtils.isEmpty(bigTitle)) {
            return bigTitle.toString();
        }
        return "No Title";
    }

    public static String getNotificationText(Bundle extras) {

        CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);
        if (!TextUtils.isEmpty(text)) {
            return text.toString();
        }
        CharSequence bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (!TextUtils.isEmpty(bigText)) {
            return bigText.toString();
        }
        CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (lines != null && lines.length > 0) {
            return TextUtils.join("\n", lines);
        }
        return "";
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.notification_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StatusBarNotification sbn = notifications.get(position);

        if (sbn == null) return;

        Notification notification = sbn.getNotification();
        if (notification == null) return;

        Bundle extras = notification.extras;
        String title = getNotificationTitle(extras);
        String text = getNotificationText(extras);

        holder.titleView.setText(title);
        holder.textView.setText(text);


        String packageName = sbn.getPackageName();
        try {
            Drawable appIcon = context.getPackageManager().getApplicationIcon(packageName);
            Glide.with(context)
                    .load(appIcon)
                    .override(100, 100)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.mipmap.ic_launcher)
                    .error(notification.getSmallIcon().loadDrawable(context))
                    .into(holder.iconView);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();

            if (notification.getSmallIcon() != null) {
                Glide.with(context)
                        .load(notification.getSmallIcon().loadDrawable(context))
                        .into(holder.iconView);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            PendingIntent pendingIntent = notification.contentIntent;
            if (pendingIntent != null) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        Bundle options = ActivityOptions.makeBasic().setPendingIntentBackgroundActivityStartMode(
                                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED).toBundle();
                        pendingIntent.send(options);
                    } else {
                        pendingIntent.send();
                    }
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                    Toast.makeText(context, "Could not open notification.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    public void updateNotifications(List<StatusBarNotification> newNotifications) {
        final NotificationDiffCallback diffCallback = new NotificationDiffCallback(this.notifications, newNotifications);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.notifications.clear();
        this.notifications.addAll(newNotifications);
        diffResult.dispatchUpdatesTo(this);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView iconView;
        final TextView titleView;
        final TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.notification_icon);
            titleView = itemView.findViewById(R.id.notification_title);
            textView = itemView.findViewById(R.id.notification_text);
        }
    }


    private static class NotificationDiffCallback extends DiffUtil.Callback {

        private final List<StatusBarNotification> oldList;
        private final List<StatusBarNotification> newList;

        public NotificationDiffCallback(List<StatusBarNotification> oldList, List<StatusBarNotification> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }


        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getKey().equals(newList.get(newItemPosition).getKey());
        }


        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            StatusBarNotification oldSbn = oldList.get(oldItemPosition);
            StatusBarNotification newSbn = newList.get(newItemPosition);

            String oldTitle = getNotificationTitle(oldSbn.getNotification().extras);
            String newTitle = getNotificationTitle(newSbn.getNotification().extras);

            String oldText = getNotificationText(oldSbn.getNotification().extras);
            String newText = getNotificationText(newSbn.getNotification().extras);


            return Objects.equals(oldTitle, newTitle) && Objects.equals(oldText, newText);
        }
    }
}