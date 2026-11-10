package com.example.data.model

import java.util.concurrent.TimeUnit

/**
 * Pure budget / reset math for per-app daily usage limits
 * (Feature: Per-App Access Limits).
 *
 * A per-app limit is a daily-minutes budget stored as an [AppUsageLimit]
 * (Room) and tracked by day-keyed usage counters in ShieldPreferences, so
 * "today" always means the current local day and the budget resets at
 * local midnight. When the budget is spent, enforcement blocks the app
 * until that midnight reset — the block duration is therefore "minutes
 * until next midnight", computed here so it can be unit-tested without a
 * device clock or timezone database: callers pass the current time and
 * the local zone's offset from UTC for that instant (DST transitions on
 * the boundary night can shift the result by the DST delta, which is
 * acceptable for a daily budget).
 */
object AppLimitBudget {

    private val DAY_MS: Long = TimeUnit.DAYS.toMillis(1)
    private val MINUTE_MS: Long = TimeUnit.MINUTES.toMillis(1)

    /** True when [usedMinutesToday] has reached an enabled, positive limit. */
    fun isExceeded(
        usedMinutesToday: Int,
        dailyLimitMinutes: Int,
        isEnabled: Boolean
    ): Boolean {
        if (!isEnabled) return false
        if (dailyLimitMinutes <= 0) return false
        return usedMinutesToday >= dailyLimitMinutes
    }

    /** Minutes of budget left today; 0 once the limit is reached. */
    fun remainingMinutes(
        usedMinutesToday: Int,
        dailyLimitMinutes: Int
    ): Int {
        if (dailyLimitMinutes <= 0) return 0
        return (dailyLimitMinutes - usedMinutesToday).coerceAtLeast(0)
    }

    /**
     * Milliseconds from [nowMs] until the next local midnight, given the
     * local zone offset ([zoneOffsetMs], e.g. TimeZone.getOffset(now)).
     * Always in (0, DAY_MS]: at exactly midnight the next reset is a full
     * day away, never 0 — a block minted at midnight must not be born
     * already expired.
     */
    fun millisUntilNextMidnight(nowMs: Long, zoneOffsetMs: Long): Long {
        val localMs = nowMs + zoneOffsetMs
        val intoDay = Math.floorMod(localMs, DAY_MS)
        return if (intoDay == 0L) DAY_MS else DAY_MS - intoDay
    }

    /**
     * Whole minutes until the next local midnight, rounded UP so a block
     * never ends before the daily counters actually reset, and coerced to
     * 1..1440 for use as a lockdown duration.
     */
    fun minutesUntilNextMidnight(nowMs: Long, zoneOffsetMs: Long): Int {
        val millis = millisUntilNextMidnight(nowMs, zoneOffsetMs)
        val minutes = (millis + MINUTE_MS - 1L) / MINUTE_MS
        return minutes.coerceIn(1L, 1440L).toInt()
    }
}
