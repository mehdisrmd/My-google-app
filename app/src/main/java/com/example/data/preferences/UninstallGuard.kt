package com.example.data.preferences

import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Pure logic for the uninstall delay (Feature: Uninstall Guard).
 *
 * Honest Android model: without device-admin there is no way to hard-block a
 * system uninstall. Instead, requesting an uninstall starts a persisted
 * countdown (a wall-clock deadline in SharedPreferences, so it survives
 * reboot). The wait length is user-chosen during first setup (#41); the
 * built-in default is 1 hour (#40, batch 2 — previously 5 hours). While no
 * request exists or the countdown is still running, the uninstall guard
 * stays armed; only after the deadline passes does the guard stand down,
 * which is what then allows a normal uninstall from system Settings.
 */
object UninstallGuard {

    /** Default wait between an uninstall request and the guard standing down. */
    val WAIT_MILLIS: Long = TimeUnit.HOURS.toMillis(1)

    /** Deadline created by a fresh uninstall request made at [nowMs]. */
    fun deadlineForRequest(nowMs: Long): Long = nowMs + WAIT_MILLIS

    /**
     * Deadline for a fresh request with a caller-chosen wait (#41: the
     * length picked during first setup). A non-positive wait means the
     * deadline is simply [nowMs] itself.
     */
    fun deadlineForRequest(nowMs: Long, waitMillis: Long): Long =
        nowMs + waitMillis.coerceAtLeast(0L)

    /** True once an uninstall request has been recorded. */
    fun isRequested(deadlineMs: Long): Boolean = deadlineMs > 0L

    /** Milliseconds left until [deadlineMs]; 0 when no request or already past. */
    fun remainingMillis(deadlineMs: Long, nowMs: Long): Long =
        (deadlineMs - nowMs).coerceAtLeast(0L)

    /** True when a request exists and its wait has fully elapsed. */
    fun isUnlocked(deadlineMs: Long, nowMs: Long): Boolean =
        isRequested(deadlineMs) && nowMs >= deadlineMs

    /**
     * The uninstall guard stands down only once the uninstall wait has
     * elapsed. Not requested yet -> false. Still counting -> false.
     * (Since batch 2 #35 the master switch no longer consults this; it
     * describes the guard's own arming window.)
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
