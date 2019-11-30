package com.redinput.notifblock

import android.os.Build
import android.preference.PreferenceManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.*

class BlockerListener : NotificationListenerService() {
    override fun onNotificationPosted(notification: StatusBarNotification) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this@BlockerListener)
        if (preferences.getBoolean(Utils.PREF_ENABLED, false)) {
            val blocked = HashSet(Arrays.asList(*preferences.getString(Utils.PREF_PACKAGES_BLOCKED, "")!!.split(";").toTypedArray()))
            if (blocked.contains(notification.packageName)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    cancelNotification(notification.key)
                } else {
                    cancelNotification(notification.packageName, notification.tag, notification.id)
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}
}