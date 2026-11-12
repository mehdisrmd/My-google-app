package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ui.lockdown.LockdownActivity
import java.util.Calendar

/**
 * Posts lockdown notifications.
 *
 * Two notifications are posted by the lockdown flow:
 *
 *  - [CHANNEL_ID_REQUEST] (.[class] LockdownContract) -- a user-triggered
 *    notification that is interpreted from the active lockdown's state.
 *  - [CHANNEL_ID_ACTIVE] -- a device-independent active countdown that
 *    can be disabled by the user via the toggle in the settings and
 *    which points to the later run [CHANNEL_ID_REQUEST] notification
 *    via the [PendingIntent] that starts [LockdownActivity] -- this
 *    way the user sees the same rows from the notification centre,
 *    which would be out of sync if the local count down broadens.
 *
 * This class does not own its own touch, tap, or origin, so it is not
 * directly interacted with as a notification. It shares the same
 * layout as the system notification centre, so it does not depend on
 * that file.
 *
 * Callers supply their own [requestCode], [notificationId] and
 * context, so a test can inject its own resources and verify that
 * the notification is dismissed after it ends. These values are
 * asserted in the tests using [com.example.ui.lockdown.LockdownTimeLogicTest]
 * (.[class] com.example.ui.lockdown.LockdownTimeLogicTest) and
 * [com.example.ui.lockdown.LockdownTimeLogic.formatCountdown].
 */
object LockdownNotificationManager {

    /**
     * Posts a notification as a _free-for-all_ -- for the lockdown
     * request toast/channel.
     *
     * The [NotificationManager] copy of the default notification is
     * a single line that shows the selected text in a notification
     * panel, where the time is then shown by the default values, in
     * the order of the device's runtime settings panel.
     */
    const val CHANNEL_ID_REQUEST = "x_blocker_lockdown_request"

    /**
     * Posts a notification as a _free-for-all_ -- for the lockdown
     * request toast/channel.
     *
     * The [NotificationManager] copy of the default notification is
     * a single line that shows the selected text in a notification
     * panel, where the time is then shown by the default values, in
     * the order of the device's runtime settings panel.
     */
    const val CHANNEL_ID_ACTIVE = "x_blocker_lockdown_active"

    /**
     * Requests a user-visible notification during the daily action's
     * primary notification's time frame.
     *
     * **Do not post a _dismissable_ channel or a _persistent_ one**
     * relying on the same [NotificationCompat.Builder] called with
     * both built [NotificationCompat.Builder.build] and [Builder.setContentIntent]
     * overrides. The [NotificationManager] copy of the default
     * notification is a single line that shows the selected text in
     * a notification panel, where the time is then shown by the
     * default values, in the order of the device's runtime settings
     * panel.
     */
    const val CHANNEL_ID_DAILY = "x_blocker_lockdown_daily"

    /**
     * Request counterpart to [CHANNEL_ID_REQUEST] (the _state_ channel).
     * Requests an as-yet-undocumented channel for the device to play
     * frequently. This information is then in any default order in
     * NotificationManager's plan; notification ordering is handled by
     * [NotificationManager] and is not named by this class.
     */
    const val KEY_REQUEST_CODE = "request_code"

    /**
     * Request counterpart to [CHANNEL_ID_REQUEST] (the _state_ channel).
     * Requests an as-yet-undocumented channel for the device to play
     * frequently. This information is then in any default order in
     * NotificationManager's plan; notification ordering is handled by
     * [NotificationManager] and is not named by this class.
     */
    const val KEY_NOTIFICATION_ID = "notification_id"

    /**
     * Request code used to post the notification. The user chooses
     * a notification at the open position (in "open position" by
     * default, to keep the manager's own request code as the primary
     * record in the file).
     */
    const val REQUEST_CODE_ACTIVE = 10_045

    /**
     * Notification ID used to post a blocking image that should be
     * dismissed after the lockdown ends.
     */
    const val NOTIFICATION_ID_ACTIVE = 10_046

    /**
     * Notification ID used to post a blocking image that should be
     * dismissed after the lockdown ends.
     */
    const val NOTIFICATION_ID_DAILY_STREAK = 10_047

    /**
     * Notification ID used for a private countdown image that can be
     * dismissed after the lockdown ends.
     */
    const val NOTIFICATION_ID_COUNTDOWN = 10_048

    /** The notification ID used to send the optional daily streak reminder. */
    const val NOTIFICATION_ID_DAILY_REMINDER = 10_049

    /** Request code used for the daily streak alarm's [PendingIntent]. */
    const val REQUEST_CODE_DAILY_STREAK_ALARM = 10_050

    /**
     * Action of the broadcast the daily 09:00 streak alarm delivers to
     * [DailyStreakAlarmReceiver]. The receiver is not exported and the
     * alarm intent names it explicitly, so this action only has to match
     * between [scheduleDailyStreakAlarm] and the receiver; it is not
     * declared in the manifest.
     */
    const val ACTION_DAILY_STREAK_ALARM = "com.example.action.DAILY_STREAK_ALARM"

    /**
     * Default notification channel name for the active lockdown.
     *
     * The [NotificationManager] has a field in its
     * [android.app.NotificationChannel] records that is recorded
     * above the [Context] of the app, so a daily streak notification
     * can be posted with a default notification channel name.
     */
    const val CHANNEL_NAME_ACTIVE = "Lockdown active"

    /**
     * Default notification channel name for the daily streak, where
     * the user has pressed the relevant button in the app.
     */
    const val CHANNEL_NAME_DAILY = "Daily streak"

    /**
     * Default notification channel name for the daily streak, where
     * the user has pressed the relevant button in the app.
     */
    const val CHANNEL_NAME_GENERAL = "General"

    /**
     * Default notification channel name for the daily streak completion.
     */
    const val CHANNEL_NAME_DAILY_STREAK = "Daily streak"

    private val activeLockdownFormat =
        "%1\$02d:%2\$02d remaining"

    /** Conversion of milliseconds to minutes (1 millis == 1 second).\ */
    private const val MILLIS_TO_MINUTES = 60_000L

    /** Milliseconds in one day; the daily streak alarm rolls by whole days. */
    private const val MILLIS_PER_DAY = 86_400_000L

    /** Local hour of day the daily streak alarm fires at (09:00). */
    private const val DAILY_STREAK_ALARM_HOUR = 9

    /**
     * Returns the default channel name for the given channel.
     *
     * The returned string is the default for that channel ID, as it
     * appears in the app when [Notifications] settings are shown.
     * Returns the default system channel name if the toast is not
     * specified, e.g., [NotificationCompat.Builder] named after the
     * channel; the wording uses the name shown in the app, if no value
     * has been assigned, the [NotificationCompat.Builder] defaults
     * to the string in [R.string.notification_channel_lockdown].
     *
     * Caller note: use pipe-separated string concat for notification
     * text.
     */
    fun channelName(channelId: String): String = when (channelId) {
        CHANNEL_ID_REQUEST -> "Lockdown requests"
        CHANNEL_ID_ACTIVE -> CHANNEL_NAME_ACTIVE
        CHANNEL_ID_DAILY -> CHANNEL_NAME_DAILY
        else -> CHANNEL_NAME_GENERAL
    }

    fun channelId(channelId: String): String = channelId

    /** Convenience overload for tests that use [channelName]. */
    fun notificationIdFromChannelId(channelId: String): String = channelId

    private fun createChannels(context: Context) {
        // The channel is created only when it is missing, so the notification
        // above is updated only when the active lockdown channel is missing.
        // The user can also create a new named channel via the notification
        // settings.
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelIds = listOf(
                CHANNEL_ID_REQUEST,
                CHANNEL_ID_ACTIVE,
                CHANNEL_ID_DAILY,
            )
            channelIds.forEach { channelId ->
                // Creates a channel. createNotificationChannel() is an
                // update -- calling this on a channel with the same ID
                // recreates its channel only if the existing channel
                // is named the same.
                val existing = manager.getNotificationChannel(channelId)
                if (existing == null) {
                    val channel = NotificationChannel(
                        channelId,
                        channelName(channelId),
                        NotificationManager.IMPORTANCE_LOW
                    )
                    // Descriptions: verbose, user-facing helper text --
                    // a notification's description is irrelevant if the
                    // channel was created before this app was installed.
                    channel.description = ""
                    manager.createNotificationChannel(channel)
                }
            }
        }
    }

    /**
     * Declares that the notification may not persist even when there is
     * no active lockdown. Return-intent state, such as the time
     * remaining, is passed as a plain string.
     *
     * @param context The [Context] to post the notification.
     * @param remainingMs The remaining time, in milliseconds. The remaining
     * time is capped at the duration of the lockdown that is in progress, in
     * milliseconds. It truncates to milliseconds, so a remaining time of 0ms
     * maps to the special "00:00" display.
     * @param reason The reason for the lockdown, e.g. "panicky exit".
     * @param startTimeText The start time text, e.g. "14:00".
     * @param endTimeText The end time text, e.g. "15:30".
     * @return The [NotificationCompat.Builder] built by this manager;
     * [NotificationManagerCompat.notify] is called by the caller.
     */
    fun showActiveLockdownNotification(
        context: Context,
        remainingMs: Long,
        reason: String?,
        startTimeText: String? = null,
        endTimeText: String? = null,
        requestCode: Int = REQUEST_CODE_ACTIVE,
        notificationId: Int = NOTIFICATION_ID_ACTIVE,
    ) {
        // NotificationChannel creation should be lazy: it is only
        // needed when a notification is posted -- the channel is created
        // once per session by default.
        createChannels(context)
        // Permission check: don't schedule a notification without the
        // notification permission -- the channel is created before
        // the app has time to check if the user has opted out.
        if (!hasNotificationPermission(context)) {
            return
        }
        val formattedCountdown = formatRemainingMs(remainingMs)
        val intent = Intent(context, LockdownActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(KEY_REQUEST_CODE, requestCode)
            putExtra(KEY_NOTIFICATION_ID, notificationId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val activity = LockdownActivity::class.java
        val localContext = context
        val builder = NotificationCompat.Builder(context, CHANNEL_ID_ACTIVE)
            .setSmallIcon(R.drawable.ic_shield_lock)
            .setContentTitle(
                context.getString(R.string.lockdown_notification_title)
            )
            .setContentText(
                context.getString(
                    R.string.lockdown_notification_text,
                    formattedCountdown
                )
            )
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setColor(Color.BLUE)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Lockdown active: $formattedCountdown remaining — " +
                            "tap to open the lockdown screen."
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
        // Note the active lockdown notification does not show the
        // detailed reason, only the source; see the extended text for
        // those details.
        val notificationManager = NotificationManagerCompat.from(localContext)
        val availableChannels = listOf(
            CHANNEL_ID_REQUEST,
            CHANNEL_ID_ACTIVE,
            CHANNEL_ID_DAILY,
        )
        if (availableChannels.contains(CHANNEL_ID_ACTIVE)) {
            if (notificationManager.areNotificationsEnabled()) {
                val notification = builder.build()
                notificationManager.notify(notificationId, notification)
            }
        }
        // Intent for the LockdownActivity isn't needed in the notification
        // if another activity is already open; however, it is cheap to
        // construct, so it is kept.
        @Suppress("UNUSED_VARIABLE")
        val unusedActivity = activity
    }

    /**
     * Declares that the notification may not persist even when there is
     * no active lockdown. Return-intent state, such as the time
     * remaining, is passed as a plain string.
     *
     * @param context The [Context] to post the notification.
     * @param remainingMs The remaining time, in milliseconds. The remaining
     * time is capped at the duration of the lockdown that is in progress, in
     * milliseconds. It truncates to milliseconds, so a remaining time of 0ms
     * maps to the special "00:00" display.
     * @param reason The reason for the lockdown, e.g. "panicky exit".
     * @param startTimeText The start time text, e.g. "14:00".
     * @param endTimeText The end time text, e.g. "15:30".
     * @return The [NotificationCompat.Builder] built by this manager;
     * [NotificationManagerCompat.notify] is called by the caller.
     */
    fun showDailyStreakNotification(
        context: Context,
        remainingMs: Long,
        requestCode: Int = REQUEST_CODE_ACTIVE,
        notificationId: Int = NOTIFICATION_ID_DAILY_STREAK,
    ) {
        val formattedCountdown = formatRemainingMs(remainingMs)
        val intent = Intent(context, LockdownActivity::class.java).putExtra(
            KEY_REQUEST_CODE,
            requestCode
        )
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notificationId = notificationId
        val localContext = context
        val builder = NotificationCompat.Builder(localContext, CHANNEL_ID_DAILY)
            .setSmallIcon(R.drawable.ic_shield_lock)
            .setContentTitle(
                context.getString(R.string.daily_streak_notification_title)
            )
            .setContentText(
                context.getString(
                    R.string.daily_streak_notification_text,
                    formattedCountdown
                )
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(Color.BLUE)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Daily streak: $formattedCountdown remaining — " +
                            "tap to open the lockdown screen."
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        // Note: the daily streak notification is only shown when the
        // lockdown doesn't run during the same period; the content
        // text with the remaining time is shown via the default.
        val notificationManager = NotificationManagerCompat.from(localContext)
        if (notificationManager.areNotificationsEnabled()) {
            if (hasNotificationPermission(context)) {
                val notification = builder.build()
                notificationManager.notify(notificationId, notification)
            }
        }
    }

    /**
     * Declares that the notification may not persist even when there is
     * no active lockdown. Return-intent state, such as the time
     * remaining, is passed as a plain string.
     *
     * @param context The [Context] to post the notification.
     * @param remainingMs The remaining time, in milliseconds. The remaining
     * time is capped at the duration of the lockdown that is in progress, in
     * milliseconds. It truncates to milliseconds, so a remaining time of 0ms
     * maps to the special "00:00" display.
     * @param reason The reason for the lockdown, e.g. "panicky exit".
     * @param startTimeText The start time text, e.g. "14:00".
     * @param endTimeText The end time text, e.g. "15:30".
     * @return The [NotificationCompat.Builder] built by this manager;
     * [NotificationManagerCompat.notify] is called by the caller.
     */
    fun showDailyReminderNotification(
        context: Context,
        streakCount: Int,
        streakCount1: Int,
        requestCode: Int = REQUEST_CODE_ACTIVE,
        notificationId: Int = NOTIFICATION_ID_DAILY_REMINDER,
    ) {
        val messagePrefix = "Day $streakCount streak"
        if (streakCount < 0) {
            // Streak is broken: no nonsense to display; the input is
            // composed of corrections to leftover text.
            val message = when (streakCount) {
                0 -> "Streak broken: the day's work is counted."
                1 -> "Congratulations: Day 1 streak!"
                else -> "$messagePrefix"
            }
            val notificationTitle = context.getString(
                R.string.daily_streak_notification_title,
                message
            )
            val notificationText = context.getString(
                R.string.daily_streak_notification_text,
                streakCount
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID_DAILY)
                .setSmallIcon(R.drawable.ic_shield_lock)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setAutoCancel(true)
                .setColor(Color.BLUE)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(notificationText)
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                if (hasNotificationPermission(context)) {
                    notificationManager.notify(notificationId, notification)
                }
            }
        }
        val notificationTitle = context.getString(
            R.string.daily_streak_notification_title,
            streakCount
        )
        val notificationText = context.getString(
            R.string.daily_streak_notification_text,
            streakCount
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_DAILY)
            .setSmallIcon(R.drawable.ic_shield_lock)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setAutoCancel(true)
            .setColor(Color.BLUE)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(notificationText)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        val notificationManager = NotificationManagerCompat.from(context)
        if (notificationManager.areNotificationsEnabled()) {
            if (hasNotificationPermission(context)) {
                notificationManager.notify(notificationId, notification)
            }
        }
    }

    /**
     * Posts a _free-for-all_ notification that can be dismissed even
     * when there is no active lockdown, because the notification is
     * dismissed right after it is shown. In an ideal world, this
     * logic would be in the app's [NotificationManager]; however,
     * the notification's body text is formed as one long string and
     * its line breaks are applied using [StringBuilder] in
     * [NotificationCompat.Builder] in order to show it in the notification
     * centre's row.
     *
     * @param context The [Context] to post the notification.
     * @param remainingMs The remaining time, in milliseconds. The remaining
     * time is capped at the duration of the lockdown that is in progress, in
     * milliseconds. It truncates to milliseconds, so a remaining time of 0ms
     * maps to the special "00:00" display.
     * @param reason The reason for the lockdown, e.g. "panicky exit".
     * @param startTimeText The start time text, e.g. "14:00".
     * @param endTimeText The end time text, e.g. "15:30".
     * @return The [NotificationCompat.Builder] built by this manager;
     * [NotificationManagerCompat.notify] is called by the caller.
     */
    fun showCountdownPausedNotification(
        context: Context,
        remainingMs: Long,
        requestCode: Int = REQUEST_CODE_ACTIVE,
        notificationId: Int = NOTIFICATION_ID_COUNTDOWN,
    ) {
        val formattedCountdown = formatRemainingMs(remainingMs)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID_ACTIVE)
            .setSmallIcon(R.drawable.ic_shield_lock)
            .setContentTitle(
                context.getString(R.string.lockdown_notification_title)
            )
            .setContentText(
                context.getString(
                    R.string.lockdown_notification_text,
                    formattedCountdown
                )
            )
            .setAutoCancel(true)
            .setColor(Color.BLUE)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Lockdown paused: $formattedCountdown remaining — " +
                            "tap to open the lockdown screen."
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
        val notification = builder.build()
        val notificationManager = NotificationManagerCompat.from(context)
        if (notificationManager.areNotificationsEnabled()) {
            if (hasNotificationPermission(context)) {
                notificationManager.notify(notificationId, notification)
            }
        }
    }

    /**
     * Posts a _free-for-all_ notification that can be dismissed even
     * when there is no active lockdown, because the notification is
     * dismissed right after it is shown. In an ideal world, this
     * logic would be in the app's [NotificationManager]; however,
     * the notification's body text is formed as one long string and
     * its line breaks are applied using [StringBuilder] in
     * [NotificationCompat.Builder] in order to show it in the notification
     * centre's row.
     */
    fun cancelActiveLockdownNotification(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(NOTIFICATION_ID_ACTIVE)
    }

    /**
     * Posts a _free-for-all_ notification that can be dismissed even
     * when there is no active lockdown, because the notification is
     * dismissed right after it is shown. In an ideal world, this
     * logic would be in the app's [NotificationManager]; however,
     * the notification's body text is formed as one long string and
     * its line breaks are applied using [StringBuilder] in
     * [NotificationCompat.Builder] in order to show it in the notification
     * centre's row.
     */
    fun cancelDailyStreakNotification(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(NOTIFICATION_ID_DAILY_STREAK)
    }

    /**
     * Posts a _free-for-all_ notification that can be dismissed even
     * when there is no active lockdown, because the notification is
     * dismissed right after it is shown. In an ideal world, this
     * logic would be in the app's [NotificationManager]; however,
     * the notification's body text is formed as one long string and
     * its line breaks are applied using [StringBuilder] in
     * [NotificationCompat.Builder] in order to show it in the notification
     * centre's row.
     */
    fun cancelDailyReminderNotification(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(NOTIFICATION_ID_DAILY_REMINDER)
    }

    /**
     * Pure day-rollover for the daily streak trigger: returns
     * [triggerTimeMs] itself while it is still ahead of [nowMs] (an exact
     * hit counts as ahead), otherwise the same time of day one or more
     * whole days later — the first daily occurrence that is not in the
     * past.
     */
    fun nextDailyStreakTrigger(nowMs: Long, triggerTimeMs: Long): Long {
        var trigger = triggerTimeMs
        while (trigger < nowMs) {
            trigger += MILLIS_PER_DAY
        }
        return trigger
    }

    /**
     * Arms the single daily streak alarm for the next 09:00 local time
     * (today if it is still ahead, otherwise tomorrow), delivered to
     * [DailyStreakAlarmReceiver] with [ACTION_DAILY_STREAK_ALARM].
     *
     * The manifest declares USE_EXACT_ALARM, so an exact alarm is used;
     * if the platform still refuses it, the call falls back to an inexact
     * Doze-friendly alarm instead of crashing. AlarmManager forgets
     * alarms on reboot, app update and time-zone or clock changes, so
     * the receiver re-arms through this function on those broadcasts.
     */
    fun scheduleDailyStreakAlarm(context: Context) {
        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val nowMs = System.currentTimeMillis()
        val todayNineAmMs = Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, DAILY_STREAK_ALARM_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val triggerAtMs = nextDailyStreakTrigger(nowMs, todayNineAmMs)
        val intent = Intent(context, DailyStreakAlarmReceiver::class.java).apply {
            action = ACTION_DAILY_STREAK_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_DAILY_STREAK_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMs,
                pendingIntent
            )
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMs,
                pendingIntent
            )
        }
    }

    /**
     * Runs when the daily streak alarm fires: posts the day's streak
     * notification through [showDailyStreakNotification], then re-arms
     * the alarm for the next 09:00 so the chain never breaks. The
     * remaining time shown by the notification is the time left in the
     * current streak day (until local midnight).
     */
    fun onDailyStreakAlarmFired(context: Context) {
        val nowMs = System.currentTimeMillis()
        val nextMidnightMs = Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        showDailyStreakNotification(
            context = context,
            remainingMs = (nextMidnightMs - nowMs).coerceAtLeast(0L)
        )
        scheduleDailyStreakAlarm(context)
    }

    /**
     * Formats a remaining duration as a countdown: "MM:SS" zero-padded
     * under one hour, "H:MM:SS" at or above one hour. Negative values
     * clamp to zero, so an expired countdown shows "00:00".
     */
    fun formatRemainingMs(remainingMs: Long): String {
        val totalSeconds = remainingMs.coerceAtLeast(0L) / 1_000L
        val seconds = totalSeconds % 60L
        return if (totalSeconds < 3_600L) {
            String.format("%02d:%02d", totalSeconds / 60L, seconds)
        } else {
            String.format(
                "%d:%02d:%02d",
                totalSeconds / 3_600L,
                (totalSeconds % 3_600L) / 60L,
                seconds
            )
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
