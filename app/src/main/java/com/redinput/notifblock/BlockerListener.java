package com.redinput.notifblock;

import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.Arrays;
import java.util.HashSet;

public class BlockerListener extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification notification) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(BlockerListener.this);

        if (preferences.getBoolean(Utils.PREF_ENABLED, false)) {
            HashSet<String> blocked = new HashSet<>(Arrays.asList(preferences.getString(Utils.PREF_PACKAGES_BLOCKED, "").split(";")));

            if (blocked.contains(notification.getPackageName())) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    cancelNotification(notification.getKey());
                } else {
                    //noinspection deprecation
                    cancelNotification(notification.getPackageName(), notification.getTag(), notification.getId());
                }
            }
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
    }
}
