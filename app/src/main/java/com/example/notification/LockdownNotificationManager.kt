package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example. data.preferences.ShieldPreferences
import com.example.ui.lockdown.LockdownActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit

object LockdownNotificationManager {
    const val CHANNEL_ID = "shield_lockdown_channel"
    const val GUARD_CHANNEL_ID = "shield_always_on_panic_channel"

    private const val NOTIFICATION_ID = 1001
    private const val GUARD_NOTIFICATION_ID = 1002

    private var countdownJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. High-priority Lockdown Countdown Channel
            val lockdownChannel = NotificationChannel(
                CHANNEL_ID,
                "Lockdown Countdown",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Displays active countdown timer while phone screen is in lockdown"
                setShowBadge(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(lockdownChannel)

            // 2. Always-On Shield & Panic SOS Quick Action Channel (Low importance so it doesn't chime repeatedly)
            val guardChannel = NotificationChannel(
                GUARD_CHANNEL_ID,
                "Always-On Panic SOS & Shield Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps an emergency Panic SOS button and instant lock action in your notification bar at all times"
                setShowBadge(false)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(guardChannel)
        }
    }

    /**
     * Posts or updates the persistent Always-On notification containing the
     * "🚨 PANIC SOS" button and "🔒 Instant Lock" button so the user always has
     * 1-tap emergency access from any app via the notification shade.
     */
    fun showOrUpdateAlwaysOnPanicNotification(context: Context) {
        try {
            createNotificationChannel(context)
            val prefs = ShieldPreferences(context.applicationContext)
            val isProtected = prefs.getIsProtectionEnabled()
            val streakDays = prefs.getStreakDays()
            val urgesDefeated = prefs.getUrgesDefeatedCount()
            val lockMins = prefs.getLockDurationMinutes()

            // Main content tap -> opens app dashboard
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingOpenApp = PendingIntent.getActivity(
                context,
                200,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Action 1: 🚨 PANIC SOS -> opens MainActivity directly into Emergency Panic Mode Dialog
            val panicIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_OPEN_PANIC_MODE, true)
            }
            val pendingPanic = PendingIntent.getActivity(
                context,
                201,
                panicIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Action 2: 🔒 INSTANT LOCK -> immediately locks the screen for the configured duration
            val quickLockIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_QUICK_EMERGENCY_LOCK, true)
            }
            val pendingQuickLock = PendingIntent.getActivity(
                context,
                202,
                quickLockIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val statusPrefix = if (isProtected) "🛡️ Shield Active" else "⚠️ Shield Paused"
            val title = "$statusPrefix • ${streakDays}d Clean Streak"
            val shortText = "Feeling an urge? Tap 🚨 PANIC SOS below for immediate help."
            val expandedText = "Clean Streak: $streakDays days • Urges Defeated: $urgesDefeated\n" +
                    "Use the buttons below anytime to open Emergency Panic Relief or lock your screen immediately."

            val notification = NotificationCompat.Builder(context, GUARD_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(shortText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
                .setOngoing(true)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setContentIntent(pendingOpenApp)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(
                    R.drawable.ic_launcher_foreground,
                    "🚨 PANIC SOS",
                    pendingPanic
                )
                .addAction(
                    R.drawable.ic_launcher_foreground,
                    "🔒 LOCK (${lockMins}M)",
                    pendingQuickLock
                )
                .build()

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(GUARD_NOTIFICATION_ID, notification)
        } catch (_: Exception) {
            // Ignore in environments where notification service is unavailable
        }
    }

    fun startLockdownNotification(context: Context, endTimeEpoch: Long, triggerWord: String) {
        createNotificationChannel(context)
        // Ensure always-on panic notification is also kept up to date
        showOrUpdateAlwaysOnPanicNotification(context)
        countdownJob?.cancel()

        countdownJob = scope.launch {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Panic SOS PendingIntent also available during lockdown
            val panicIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_OPEN_PANIC_MODE, true)
            }
            val pendingPanic = PendingIntent.getActivity(
                context,
                203,
                panicIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            while (isActive) {
                val now = System.currentTimeMillis()
                val left = (endTimeEpoch - now).coerceAtLeast(0L)

                if (left <= 0L) {
                    val finishIntent = Intent(context, LockdownActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val pendingFinish = PendingIntent.getActivity(
                        context,
                        0,
                        finishIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val finishNotification = NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("🛡️ Focus Period Complete!")
                        .setContentText("You resisted the urge. Tap to return to ShieldBlock.")
                        .setOngoing(false)
                        .setAutoCancel(true)
                        .setContentIntent(pendingFinish)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .addAction(
                            R.drawable.ic_launcher_foreground,
                            "🚨 PANIC SOS",
                            pendingPanic
                        )
                        .build()

                    notificationManager.notify(NOTIFICATION_ID, finishNotification)
                    showOrUpdateAlwaysOnPanicNotification(context)
                    break
                }

                val minutes = TimeUnit.MILLISECONDS.toMinutes(left)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(left) % 60
                val formattedTime = String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)

                val tapIntent = Intent(context, LockdownActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
                val pendingTap = PendingIntent.getActivity(
                    context,
                    0,
                    tapIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("🔒 ShieldBlock Lockdown Active")
                    .setContentText("Remaining: $formattedTime | Trigger: $triggerWord")
                    .setStyle(
                        NotificationCompat.BigTextStyle().bigText(
                            "Lockdown in progress: $formattedTime remaining.\n" +
                                    "Trigger: \"$triggerWord\". Phone is locked to protect your focus."
                        )
                    )
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setContentIntent(pendingTap)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .addAction(
                        R.drawable.ic_launcher_foreground,
                        "🔒 RETURN TO LOCK SCREEN",
                        pendingTap
                    )
                    .build()

                notificationManager.notify(NOTIFICATION_ID, notification)
                delay(1000L)
            }
        }
    }

    fun cancelLockdownNotification(context: Context) {
        countdownJob?.cancel()
        countdownJob = null
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
        // Keep the Always-On Panic SOS notification visible in the notification bar!
        showOrUpdateAlwaysOnPanicNotification(context)
    }
}
