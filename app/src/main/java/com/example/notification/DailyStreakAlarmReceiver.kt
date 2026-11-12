package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives the daily streak reminder alarm and the Boot / TZ / time-change
 * broadcasts the receiver is subscribed to, then delegates to
 * [LockdownNotificationManager].
 *
 * The receiver's own [Context] receiver [BroadcastReceiver] is disabled in
 * the manifest -- see AndroidManifest.xml -- so the receiver only wakes up
 * when it is explicitly invoked by the alarm, not passively.
 */
class DailyStreakAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED -> {
                // The time and/or date changed, or the app was (re)installed:
                // schedule the alarm again for the next day.
                LockdownNotificationManager.scheduleDailyStreakAlarm(context)
            }

            LockdownNotificationManager.ACTION_DAILY_STREAK_ALARM -> {
                LockdownNotificationManager.onDailyStreakAlarmFired(context)
            }

            else -> Unit
        }
    }
}
