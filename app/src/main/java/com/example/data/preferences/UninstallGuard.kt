package com.example.data.preferences

import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Pure logic for the 5-hour uninstall delay (Feature: Uninstall Guard).
 *
 * Honest Android model: without device-admin there is no way to hard-block a
 * system uninstall. Instead, requesting an uninstall starts a persisted
 * 5-hour countdown (a wall-clock deadline in SharedPreferences, so it
 * survives reboot). While no request exists or the countdown is still
 * running, the master protection switch cannot be turned OFF. Only after the
 * deadline passes may protection be disabled — which is what then allows a
 * normal uninstall from system Settings.
 */
object UninstallGuard {

    /** Mandatory wait between an uninstall request and protection switch-off. */
    val WAIT_MILLIS: Long = TimeUnit.HOURS.toMillis(5)

    /** Deadline created by a fresh uninstall request made at [nowMs]. */
    fun deadlineForRequest(nowMs: Long): Long = nowMs + WAIT_MILLIS

    /** True once an uninstall request has been recorded. */
    fun isRequested(deadlineMs: Long): Boolean = deadlineMs > 0L

    /** Milliseconds left until [deadlineMs]; 0 when no request or already past. */
    fun remainingMillis(deadlineMs: Long, nowMs: Long): Long =
        (deadlineMs - nowMs).coerceAtLeast(0L)

    /** True when a request exists and its 5-hour wait has fully elapsed. */
    fun isUnlocked(deadlineMs: Long, nowMs: Long): Boolean =
        isRequested(deadlineMs) && nowMs >= deadlineMs

    /**
     * The master protection switch may only be turned OFF once the uninstall
     * wait has elapsed. Not requested yet -> false. Still counting -> false.
     */
    fun canDisableProtection(deadlineMs: Long, nowMs: Long): Boolean =
        isUnlocked(deadlineMs, nowMs)

    /** Formats a remaining duration as H:MM:SS (e.g. "4:59:32"). */
    fun formatRemainingHms(remainingMs: Long): String {
        val safe = remainingMs.coerceAtLeast(0L)
        val hours = TimeUnit.MILLISECONDS.toHours(safe)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(safe) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(safe) % 60
        return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    }
}
