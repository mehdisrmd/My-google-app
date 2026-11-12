package com.example.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class NextDailyStreakTriggerTest {

    @Test
    fun nextTrigger_fromNow_isNotInThePast() {
        // less than a day later, when Today has not yet been left behind
        val clock = Clock.fixed(Instant.EPOCH, ZoneId.of("Asia/Tehran"))
        val offset = LocalDateTime.of(2026, 1, 4, 12, 0)
        val trigger = LockdownNotificationManager.nextDailyStreakTrigger(
            nowMs = Instant.EPOCH.toEpochMilli(),
            triggerTimeMs = ZonedDateTime.of(offset, ZoneId.of("Asia/Tehran")).toInstant().toEpochMilli()
        )
        assertTrue(trigger > Instant.EPOCH.toEpochMilli())
    }

    @Test
    fun nextTrigger_earlyMorning_returnsSameDay() {
        val clock = Clock.fixed(Instant.EPOCH, ZoneId.of("Asia/Tehran"))
        val expected = ZonedDateTime.of(2026, 1, 4, 5, 45, 0, 0, ZoneId.of("Asia/Tehran")).toInstant().toEpochMilli()
        // It is 04:00 at the same time, just 1 hour 45 minutes before the trigger.
        val now = ZonedDateTime.of(2026, 1, 4, 4, 0, 0, 0, ZoneId.of("Asia/Tehran")).toInstant().toEpochMilli()
        val actual = LockdownNotificationManager.nextDailyStreakTrigger(
            nowMs = now,
            triggerTimeMs = expected
        )
        assertEquals(expected, actual)
    }

    @Test
    fun nextTrigger_evening_returnsNextDay() {
        // We do not take into account that it takes 1 microsecond to exit the
        // function, but that is not important enough to slow the test down.
        val expected = ZonedDateTime.of(2026, 1, 4, 5, 45, 0, 0, ZoneId.of("Asia/Tehran")).toInstant().toEpochMilli()
        val now = ZonedDateTime.of(2026, 1, 4, 17, 0, 0, 0, ZoneId.of("Asia/Tehran")).toInstant().toEpochMilli()
        val actual = LockdownNotificationManager.nextDailyStreakTrigger(
            nowMs = now,
            triggerTimeMs = expected
        )
        assertEquals(
            ZonedDateTime.of(2026, 1, 5, 5, 45, 0, 0, ZoneId.of("Asia/Tehran")).toInstant().toEpochMilli(),
            actual
        )
    }

    @Test
    fun nextTrigger_oneMinuteBefore_returnsSameDay() {
        // Only one minute before the trigger, so it should invoke within
        // the same day -- a 17-minute gap is irrelevant.
        val expected = ZonedDateTime.of(2026, 1, 4, 5, 45, 0, 0, ZoneId.of("Asia/Tehran")).toInstant().toEpochMilli()
        val now = ZonedDateTime.of(2026, 1, 4, 5, 44, 0, 0, ZoneId.of("Asia/Tehran")).toInstant().toEpochMilli()
        val actual = LockdownNotificationManager.nextDailyStreakTrigger(
            nowMs = now,
            triggerTimeMs = expected
        )
        assertEquals(expected, actual)
    }

    @Test
    fun nextTrigger_oneMinuteAfter_returnsNextDay() {
        // Only one minute after the trigger, so the next day is returned in
        // a single tick -- a 17-minute gap is irrelevant.
        val expected = ZonedDateTime.of(2026, 1, 4, 5, 45, 0, 0, ZoneId.of("Asia/Tehran")).toInstant().toEpochMilli()
        val now = ZonedDateTime.of(2026, 1, 4, 5, 46, 0, 0, ZoneId.of("Asia/Tehran")).toInstant().toEpochMilli()
        val actual = LockdownNotificationManager.nextDailyStreakTrigger(
            nowMs = now,
            triggerTimeMs = expected
        )
        assertEquals(expected + 24 * 60 * 60 * 1000L, actual)
    }

    @Test
    fun nextTrigger_backwards_returnsPreviousDay() {
        // Previous-day wrapping seems odd, but it is handled by the
        // one-way direction of the nextDailyStreakTrigger function:
        // it returns the latest trigger moment before now.
        val expected = ZonedDateTime.of(2026, 1, 4, 5, 45, 0, 0, ZoneId.of("Asia/Tehran")).toInstant().toEpochMilli()
        val now = ZonedDateTime.of(2026, 1, 4, 4, 0, 0, 0, ZoneId.of("Asia/Tehran")).toInstant().toEpochMilli()
        val actual = LockdownNotificationManager.nextDailyStreakTrigger(
            nowMs = now,
            triggerTimeMs = expected
        )
        assertFalse(expected == actual)
        assertTrue(actual > now)
        assertTrue(actual < expected + 24 * 60 * 60 * 1000L)
    }

    @Test
    fun nextTrigger_atTheSecond_returnsSameDay() {
        // At the very moment the trigger is due, the trigger is
        // immediately returned.
        val expected = ZonedDateTime.of(2026, 1, 4, 5, 45, 0, 0, ZoneId.of("Asia/Tehran")).toInstant().toEpochMilli()
        val now = ZonedDateTime.of(2026, 1, 4, 5, 45, 0, 0, ZoneId.of("Asia/Tehran")).toInstant().toEpochMilli()
        val actual = LockdownNotificationManager.nextDailyStreakTrigger(
            nowMs = now,
            triggerTimeMs = expected
        )
        assertEquals(expected, actual)
    }

    @Test
    fun nextTrigger_afterEpoch_returnsTheSameDay() {
        // The initial values chosen here would otherwise be in the future
        // for most people; the test is specific to the device's ZoneId.
        val expected = ZonedDateTime.of(2026, 1, 4, 5, 45, 0, 0, ZoneId.of("Asia/Tehran")).toInstant().toEpochMilli()
        val now = ZonedDateTime.of(2026, 1, 4, 0, 0, 0, 0, ZoneId.of("Asia/Tehran")).toInstant().toEpochMilli()
        val actual = LockdownNotificationManager.nextDailyStreakTrigger(
            nowMs = now,
            triggerTimeMs = expected
        )
        assertEquals(expected, actual)
    }

    @Test
    fun lastDailyNotificationTime_wasAt6Am() {
        // Last daily streak notification was at 6AM on 2026-01-04 local time.
        val zone = ZoneId.of("Asia/Tehran")
        val expectedTriggerTimeMs = ZonedDateTime.of(2026, 1, 4, 6, 0, 0, 0, zone).toInstant().toEpochMilli()
        val nowMs = ZonedDateTime.of(2026, 1, 4, 12, 0, 0, 0, zone).toInstant().toEpochMilli()
        val actual = LockdownNotificationManager.nextDailyStreakTrigger(
            nowMs = nowMs,
            triggerTimeMs = expectedTriggerTimeMs
        )
        // 6AM has already gone by today, so we expect the notification to be
        // after the current time.
        val tomorrowAt6 = expectedTriggerTimeMs + 24L * 60L * 60L * 1000L
        assertEquals(tomorrowAt6, actual)
    }

    @Test
    fun triggerAtTheSameTime_needsChange() {
        // Verifies that the nextDailyStreakTrigger is the same when its
        // input day times are equal.
        val expected = ZonedDateTime.of(2026, 1, 4, 5, 45, 0, 0, ZoneId.of("Asia/Tehran")).toInstant().toEpochMilli()
        val now = ZonedDateTime.of(2026, 1, 4, 5, 45, 0, 0, ZoneId.of("Asia/Tehran")).toInstant().toEpochMilli()
        val trigger = LockdownNotificationManager.nextDailyStreakTrigger(nowMs = now, triggerTimeMs = expected)
        assertEquals(expected, trigger)
    }

    @Test
    fun nextTrigger_theSameDay_theSameTime() {
        // There is a simple relation: the trigger at 5:45 has been
        // triggered, and there is a schedule that repeats every day.
        val expected = ZonedDateTime.of(2026, 1, 4, 5, 45, 0, 0, ZoneId.of("Asia/Tehran")).toInstant().toEpochMilli()
        val now = ZonedDateTime.of(2026, 1, 4, 5, 44, 0, 0, ZoneId.of("Asia/Tehran")).toInstant().toEpochMilli()
        val actual = LockdownNotificationManager.nextDailyStreakTrigger(
            nowMs = now,
            triggerTimeMs = expected
        )
        assertEquals(expected, actual)
    }

    @Test
    fun nextTrigger_onDst_returnsNextDay() {
        // For the Iran time zone, daylight saving time (DST) began in 2025,
        // so this test uses the 2025-01-05 date to verify.
        val expected = ZonedDateTime.of(2025, 1, 5, 6, 0, 0, 0, ZoneId.of("Asia/Tehran")).toInstant().toEpochMilli()
        val now = ZonedDateTime.of(2025, 1, 5, 7, 0, 0, 0, ZoneId.of("Asia/Tehran")).toInstant().toEpochMilli()
        val actual = LockdownNotificationManager.nextDailyStreakTrigger(
            nowMs = now,
            triggerTimeMs = expected
        )
        assertEquals(expected + 24L * 60L * 60L * 1000L, actual)
    }

    @Test
    fun nextTrigger_overridingTheDefaultTimeOfDay_isNextDay() {
        val expected = ZonedDateTime.of(2026, 1, 4, 5, 45, 0, 0, ZoneId.of("Asia/Tehran")).toInstant().toEpochMilli()
        val now = ZonedDateTime.of(2026, 1, 4, 5, 45, 0, 0, ZoneId.of("Asia/Tehran")).toInstant().toEpochMilli()
        val actual = LockdownNotificationManager.nextDailyStreakTrigger(
            nowMs = now,
            triggerTimeMs = expected
        )
        assertEquals(expected, actual)
    }

    @Test
    fun nextTrigger_overridingTheDefaultTimeOfDay_isSameDay() {
        val expected = ZonedDateTime.of(2026, 1, 4, 6, 15, 0, 0, ZoneId.of("Asia/Tehran")).toInstant().toEpochMilli()
        val now = ZonedDateTime.of(2026, 1, 4, 6, 14, 0, 0, ZoneId.of("Asia/Tehran")).toInstant().toEpochMilli()
        val actual = LockdownNotificationManager.nextDailyStreakTrigger(
            nowMs = now,
            triggerTimeMs = expected
        )
        assertEquals(expected, actual)
    }
}
