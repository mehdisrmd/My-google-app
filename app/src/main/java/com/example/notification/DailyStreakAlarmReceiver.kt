package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Entry point for the single daily 9:00 AM streak notification.
 *
 * - The alarm intent (scheduled via
 *   [LockdownNotificationManager.scheduleDailyStreakAlarm]) runs the
 *   coin/streak sync and posts the day's one notification.
 * - BOOT_COMPLETED simply re-arms the alarm for the next 9:00 AM, since
 *   AlarmManager alarms do not survive a reboot.
 */
class DailyStreakAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            LockdownNotificationManager.scheduleDailyStreakAlarm(context)
        } else {
            LockdownNotificationManager.onDailyStreakAlarmFired(context)
        }
    }
}
