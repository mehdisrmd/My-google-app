package com.example.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.ShieldApplication
import com.example.data.preferences.ShieldPreferences
import java.util.Calendar

/**
 * X Blocker notifications are intentionally minimal (owner's request):
 * exactly ONE motivational streak notification per day, posted at 9:00 AM
 * local time by an AlarmManager-scheduled broadcast
 * (see [DailyStreakAlarmReceiver]).
 *
 * Removed completely:
 * - The always-on persistent PANIC SOS notification bar.
 * - The per-second lockdown countdown notification.
 * - Opportunistic Clean Day milestone popups (previously posted from
 *   whichever screen/service happened to run the coin sync).
 *
 * The in-app Panic Mode and Lockdown features themselves are unchanged —
 * they simply no longer post notifications.
 */
object LockdownNotificationManager {
    private const val CHANNEL_ID_MILESTONE = "x_blocker_streak_milestone_channel_v1"

    private const val NOTIFICATION_ID_MILESTONE = 4003
    private const val REQUEST_CODE_DAILY_STREAK_ALARM = 9001

    const val ACTION_OPEN_MAIN_DASHBOARD = "com.example.action.OPEN_MAIN_DASHBOARD"
    const val ACTION_OPEN_PANIC_MODE = "com.example.action.OPEN_PANIC_MODE"
    const val ACTION_QUICK_EMERGENCY_LOCK = "com.example.action.QUICK_EMERGENCY_LOCK"
    const val ACTION_DAILY_STREAK_ALARM = "com.example.action.DAILY_STREAK_9AM"

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val milestoneChannel = NotificationChannel(
                CHANNEL_ID_MILESTONE,
                "X Blocker Clean Day Milestones",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "One motivational notification at 9:00 AM for your clean streak"
                setShowBadge(true)
                enableVibration(true)
            }

            nm.createNotificationChannel(milestoneChannel)
        }
    }

    /**
     * Epoch millis of the next 9:00 AM local time strictly after [nowMs]
     * (today at 9:00 if that is still ahead, otherwise tomorrow at 9:00).
     */
    fun nextDailyStreakTriggerMillis(nowMs: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= nowMs) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return cal.timeInMillis
    }

    /**
     * (Re)schedules the single daily 9:00 AM streak alarm. Called on app
     * start (ShieldApplication / MainActivity), after BOOT_COMPLETED, and
     * by the receiver itself after each firing so the chain never breaks.
     * Uses an exact alarm where permitted, falling back to an inexact
     * Doze-friendly alarm if exact alarms are not allowed.
     */
    fun scheduleDailyStreakAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return
        val intent = Intent(context, DailyStreakAlarmReceiver::class.java).apply {
            action = ACTION_DAILY_STREAK_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_DAILY_STREAK_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAtMillis = nextDailyStreakTriggerMillis()
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (_: Exception) {
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Runs when the daily 9:00 AM alarm fires: chains tomorrow's alarm,
     * syncs clean-day coins / best chain, then posts the day's single
     * streak notification — but only when the streak is at least 1 day.
     * A streak of 0 stays completely silent.
     */
    fun onDailyStreakAlarmFired(context: Context) {
        scheduleDailyStreakAlarm(context)

        val appContext = context.applicationContext
        createNotificationChannel(appContext)
        if (!hasNotificationPermission(appContext)) return

        val prefs = runCatching { ShieldApplication.instance.preferences }
            .getOrElse { ShieldPreferences(appContext) }
        prefs.syncCleanDaysCoinsAndCheckMilestone()

        val streakDays = prefs.getStreakDays()
        if (streakDays >= 1) {
            showCleanDayMilestoneNotification(
                context = appContext,
                streakDays = streakDays,
                isPersian = prefs.getIsPersianLanguage()
            )
        }
    }

    fun showCleanDayMilestoneNotification(
        context: Context,
        streakDays: Int,
        isPersian: Boolean = false
    ): Boolean {
        createNotificationChannel(context)
        if (!hasNotificationPermission(context)) return false

        return try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return false

            val openIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_OPEN_MAIN_DASHBOARD
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val openPendingIntent = PendingIntent.getActivity(
                context,
                210,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = if (isPersian) {
                "روز $streakDays پاک — آفرین!"
            } else {
                "Day $streakDays Clean — Well Done!"
            }

            val body = if (isPersian) {
                "زنجیره را نشکن! ادامه بده"
            } else {
                "Don't break the chain — keep going!"
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID_MILESTONE)
                .setSmallIcon(R.drawable.ic_stat_x_shield)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(openPendingIntent)
                .build()

            nm.notify(NOTIFICATION_ID_MILESTONE, notification)
            true
        } catch (_: Exception) {
            false
        }
    }
}
