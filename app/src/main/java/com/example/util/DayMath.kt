package com.example.util

import java.util.TimeZone

/**
 * Local-calendar day numbering for "today" counters.
 *
 * `TimeUnit.MILLISECONDS.toDays(now)` counts UTC days, so counters keyed by it
 * roll over at UTC midnight (03:30 in Iran, 19:00 to 20:00 in the US East).
 * That disagreed with [com.example.data.model.AppLimitBudget], which ends a
 * daily-limit block at LOCAL midnight, and let a block chain into a second
 * ~24 hour block. A day number computed from the local offset flips at the
 * same instant the budget resets.
 *
 * Kept free of Android classes so it can be unit-tested with fixed offsets.
 */
object DayMath {

    private const val DAY_MS: Long = 86_400_000L

    /**
     * Whole days since 1970-01-01 in the zone with offset [zoneOffsetMs] from
     * UTC at the instant [nowMs]. [Math.floorDiv] keeps instants before the
     * epoch correct.
     */
    fun localEpochDay(nowMs: Long, zoneOffsetMs: Long): Long =
        Math.floorDiv(nowMs + zoneOffsetMs, DAY_MS)

    /** Local epoch day in the device's current time zone, DST included. */
    fun localEpochDay(nowMs: Long = System.currentTimeMillis()): Long =
        localEpochDay(nowMs, TimeZone.getDefault().getOffset(nowMs).toLong())
}
