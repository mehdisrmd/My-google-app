package com.example.notification

import android.Manifest
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
import com.example.ui.lockdown.LockdownActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Manages:
 * 1. Short, efficient Always-On Quick Panic SOS Notification Bar control.
 * 2. Concise live countdown notification during an active X Blocker lockdown.
 * 3. Celebratory motivational notification whenever the user reaches Day 1, Day 2, Day 3... Clean (+1 Coin).
 */
object LockdownNotificationManager {
    private const val CHANNEL_ID_LOCKDOWN = "x_blocker_lockdown_channel_v2"
    private const val CHANNEL_ID_ALWAYS_ON_PANIC = "x_blocker_panic_sos_channel_v2"
    private const val CHANNEL_ID_MILESTONE = "x_blocker_streak_milestone_channel_v1"

    private const val NOTIFICATION_ID_LOCKDOWN = 4001
    private const val NOTIFICATION_ID_ALWAYS_ON_PANIC = 4002
    private const val NOTIFICATION_ID_MILESTONE = 4003

    const val ACTION_OPEN_MAIN_DASHBOARD = "com.example.action.OPEN_MAIN_DASHBOARD"
    const val ACTION_OPEN_PANIC_MODE = "com.example.action.OPEN_PANIC_MODE"
    const val ACTION_QUICK_EMERGENCY_LOCK = "com.example.action.QUICK_EMERGENCY_LOCK"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var countdownJob: Job? = null

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

            val lockdownChannel = NotificationChannel(
                CHANNEL_ID_LOCKDOWN,
                "X Blocker Lockdown Timer",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Active lockdown countdown"
                setShowBadge(true)
                setSound(null, null)
                enableVibration(false)
            }

            val panicChannel = NotificationChannel(
                CHANNEL_ID_ALWAYS_ON_PANIC,
                "X Blocker Quick SOS Bar",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Quick Panic SOS & Lock shortcut"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }

            val milestoneChannel = NotificationChannel(
                CHANNEL_ID_MILESTONE,
                "X Blocker Clean Day Milestones",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Motivational notifications for Day 1, Day 2, Day 3+ Clean streaks"
                setShowBadge(true)
                enableVibration(true)
            }

            nm.createNotificationChannel(lockdownChannel)
            nm.createNotificationChannel(panicChannel)
            nm.createNotificationChannel(milestoneChannel)
        }
    }

    /**
     * Checks if the user has reached a new Clean Day (Day 1, Day 2, Day 3...) and awards +1 Coin
     * while posting a motivational milestone notification.
     */
    fun checkAndNotifyCleanDayMilestone(context: Context) {
        val app = runCatching { ShieldApplication.instance }.getOrNull() ?: return
        val prefs = app.preferences
        val newlyReachedDay = prefs.syncCleanDaysCoinsAndCheckMilestone()
        if (newlyReachedDay >= 1) {
            showCleanDayMilestoneNotification(
                context = context,
                streakDays = newlyReachedDay,
                coinsBalance = prefs.getCoinsBalance(),
                isPersian = prefs.getIsPersianLanguage()
            )
        }
    }

    fun showCleanDayMilestoneNotification(
        context: Context,
        streakDays: Int,
        coinsBalance: Int,
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
                "روز $streakDays پاک! +۱ سکه جایزه"
            } else {
                "Day $streakDays Clean! +1 Coin Earned"
            }

            val body = if (isPersian) {
                "زنجیره را نشکن! مجموع سکه‌های شما: $coinsBalance سکه."
            } else {
                "Don't break the chain! Keep going strong ($coinsBalance coins)."
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

    /**
     * Posts or refreshes the short, efficient Always-On Emergency Panic SOS notification.
     */
    fun showOrUpdateAlwaysOnPanicNotification(context: Context): Boolean {
        createNotificationChannel(context)
        checkAndNotifyCleanDayMilestone(context)
        if (!hasNotificationPermission(context)) {
            return false
        }

        return try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return false
            val app = runCatching { ShieldApplication.instance }.getOrNull()
            val prefs = app?.preferences
            val isProtectionOn = prefs?.getIsProtectionEnabled() ?: true
            val streakDays = prefs?.getStreakDays() ?: 0
            val coins = prefs?.getCoinsBalance() ?: 0
            val lockMins = prefs?.getLockDurationMinutes() ?: 5
            val isPersian = prefs?.getIsPersianLanguage() ?: false

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_OPEN_MAIN_DASHBOARD
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                201,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val panicSosIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_OPEN_PANIC_MODE
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_OPEN_PANIC_MODE, true)
            }
            val panicSosPendingIntent = PendingIntent.getActivity(
                context,
                202,
                panicSosIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val quickLockIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_QUICK_EMERGENCY_LOCK
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_QUICK_EMERGENCY_LOCK, true)
            }
            val quickLockPendingIntent = PendingIntent.getActivity(
                context,
                203,
                quickLockIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = if (isPersian) {
                val state = if (isProtectionOn) "فعال" else "متوقف"
                "X Blocker ($state) • ${streakDays} روز پاک • $coins سکه"
            } else {
                val state = if (isProtectionOn) "On" else "Paused"
                "X Blocker ($state) • ${streakDays}d Clean • ${coins}🪙"
            }

            val shortDesc = if (isPersian) {
                "دکمه PANIC SOS یا قفل فوری (${lockMins}m)"
            } else {
                "Tap PANIC SOS or LOCK NOW (${lockMins}m)"
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALWAYS_ON_PANIC)
                .setSmallIcon(R.drawable.ic_stat_x_shield)
                .setContentTitle(title)
                .setContentText(shortDesc)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(openAppPendingIntent)
                .addAction(
                    R.drawable.ic_stat_x_shield,
                    "PANIC SOS",
                    panicSosPendingIntent
                )
                .addAction(
                    R.drawable.ic_stat_x_shield,
                    "LOCK (${lockMins}m)",
                    quickLockPendingIntent
                )
                .build()

            nm.notify(NOTIFICATION_ID_ALWAYS_ON_PANIC, notification)
            true
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    fun startLockdownNotification(
        context: Context,
        endTimeEpoch: Long,
        triggerWord: String
    ) {
        createNotificationChannel(context)
        countdownJob?.cancel()

        val appContext = context.applicationContext
        countdownJob = scope.launch {
            val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return@launch

            val tapIntent = Intent(appContext, LockdownActivity::class.java).apply {
                action = "com.example.action.OPEN_ACTIVE_LOCKDOWN"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                appContext,
                204,
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            while (true) {
                val remaining = (endTimeEpoch - System.currentTimeMillis()).coerceAtLeast(0L)
                if (remaining <= 0L) {
                    nm.cancel(NOTIFICATION_ID_LOCKDOWN)
                    showOrUpdateAlwaysOnPanicNotification(appContext)
                    break
                }

                if (hasNotificationPermission(appContext)) {
                    val mins = TimeUnit.MILLISECONDS.toMinutes(remaining)
                    val secs = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60
                    val timeText = String.format(Locale.ROOT, "%02d:%02d", mins, secs)

                    try {
                        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID_LOCKDOWN)
                            .setSmallIcon(R.drawable.ic_stat_x_shield)
                            .setContentTitle("X Blocker • $timeText left")
                            .setContentText("Blocked: $triggerWord")
                            .setOngoing(true)
                            .setOnlyAlertOnce(true)
                            .setSilent(true)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setCategory(NotificationCompat.CATEGORY_ALARM)
                            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                            .setContentIntent(pendingIntent)
                            .build()

                        nm.notify(NOTIFICATION_ID_LOCKDOWN, notification)
                    } catch (_: Exception) {
                    }
                }

                delay(1000L)
            }
        }
    }

    fun cancelNotification(context: Context) {
        countdownJob?.cancel()
        countdownJob = null
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(NOTIFICATION_ID_LOCKDOWN)
        showOrUpdateAlwaysOnPanicNotification(context)
    }
}
