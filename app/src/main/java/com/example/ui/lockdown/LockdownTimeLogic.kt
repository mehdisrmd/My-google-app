package com.example.ui.lockdown

import java.util.Locale

/**
 * Pure countdown rules for the block screen, kept free of Android classes so
 * they can be unit-tested without a device clock.
 *
 * The rule that matters most: a timed block is over at the exact instant its
 * end time is reached (`now >= end`). This matches ShieldPreferences
 * .isCurrentlyLocked(), which is locked only while `end > now`, so the screen
 * and the stored lock can never disagree by a millisecond.
 */
object LockdownTimeLogic {

    /** Longest wait between countdown refreshes. */
    const val TICK_MS: Long = 500L

    /** Milliseconds left until [endTimeMs], never negative. */
    fun remainingMs(endTimeMs: Long, nowMs: Long): Long =
        (endTimeMs - nowMs).coerceAtLeast(0L)

    /** True at the exact end instant and at every later time. */
    fun isExpired(endTimeMs: Long, nowMs: Long): Boolean = nowMs >= endTimeMs

    /**
     * How long to wait before the next refresh. Never longer than one tick, and
     * never longer than the time left, so the final wait lands exactly on the
     * end time instead of overshooting it by up to a tick. Always at least 1 ms
     * so a loop can never spin without yielding.
     */
    fun nextDelayMs(remainingMs: Long): Long =

        minOf(TICK_MS, remainingMs).coerceAtLeast(1L)

    /**
     * MM:SS text, rounded UP to the next whole second. That way "00:00" only
     * ever means the block is over, never "less than a second left".
     */
    fun formatCountdown(remainingMs: Long): String {
        val totalSeconds = (remainingMs.coerceAtLeast(0L) + 999L) / 1000L
        return String.format(Locale.ROOT, "%02d:%02d", totalSeconds / 60L, totalSeconds % 60L)
    }
}
