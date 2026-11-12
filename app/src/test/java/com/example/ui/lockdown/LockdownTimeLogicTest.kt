package com.example.ui.lockdown

import com.example.ui.lockdown.LockdownTimeLogic
import com.example.util.DayMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.time.ZonedDateTime

class LockdownTimeLogicTest {

    @Test
    fun remainingMs_atEnd_isZero() {
        assertEquals(0L, LockdownTimeLogic.remainingMs(endTimeMs = 1_000L, nowMs = 1_000L))
        assertEquals(0L, LockdownTimeLogic.remainingMs(endTimeMs = 1_000L, nowMs = 1_001L))
    }

    @Test
    fun remainingMs_beforeEnd_returnsDifference() {
        assertEquals(500L, LockdownTimeLogic.remainingMs(endTimeMs = 1_000L, nowMs = 500L))
    }

    @Test
    fun nextDelayMs_lessThanOneSecond_picksLargestWholeSecond() {
        // Less than a second left: no full second can be billed, so
        // use the remaining fraction directly.
        assertEquals(999L, LockdownTimeLogic.nextDelayMs(remainingMs = 999L))
        assertEquals(500L, LockdownTimeLogic.nextDelayMs(remainingMs = 500L))
        // A fraction of a millisecond is still visible: a partial
        // tick is still required.
        assertEquals(1L, LockdownTimeLogic.nextDelayMs(remainingMs = 1L))
        // Remaining time ends exactly on a whole second: no partial
        // delay is needed.
        assertEquals(1_000L, LockdownTimeLogic.nextDelayMs(remainingMs = 1_000L))
        assertEquals(2_000L, LockdownTimeLogic.nextDelayMs(remainingMs = 2_000L))
    }

    @Test
    fun nextDelayMs_lessThanOneTickLeft_waitsExactlyTheRemainder() {
        assertEquals(300L, LockdownTimeLogic.nextDelayMs(remainingMs = 300L))
        assertEquals(299L, LockdownTimeLogic.nextDelayMs(remainingMs = 299L))
        assertEquals(1L, LockdownTimeLogic.nextDelayMs(remainingMs = 1L))
    }

    @Test
    fun nextDelayMs_exactSecondBoundary_isWholeTick() {
        assertEquals(1_000L, LockdownTimeLogic.nextDelayMs(remainingMs = 1_000L))
        assertEquals(59_000L, LockdownTimeLogic.nextDelayMs(remainingMs = 59_000L))
    }

    @Test
    fun nextDelayMs_zeroOrNegativeRemaining_isZero() {
        assertEquals(0L, LockdownTimeLogic.nextDelayMs(remainingMs = 0L))
        assertEquals(0L, LockdownTimeLogic.nextDelayMs(remainingMs = -1L))
    }

    @Test
    fun nextDelayMs_moreThanOneTickLeft_isCappedToOneSecond() {
        assertEquals(1_000L, LockdownTimeLogic.nextDelayMs(remainingMs = 400_001L))
        assertEquals(1_000L, LockdownTimeLogic.nextDelayMs(remainingMs = 3_600_000L))
        assertEquals(1_000L, LockdownTimeLogic.nextDelayMs(remainingMs = 2_999L))
    }

    // ---- formatCountdown: rounded UP, so 00:00 only means "over" ----

    @Test
    fun formatCountdown_zero_isDone() {
        assertEquals("00:00", LockdownTimeLogic.formatCountdown(0L))
    }

    @Test
    fun formatCountdown_negative_isDone() {
        assertEquals("00:00", LockdownTimeLogic.formatCountdown(-5L))
    }

    @Test
    fun formatCountdown_partialSecond_roundsUp() {
        assertEquals("00:01", LockdownTimeLogic.formatCountdown(1L))
        assertEquals("00:01", LockdownTimeLogic.formatCountdown(999L))
        assertEquals("00:02", LockdownTimeLogic.formatCountdown(1_001L))
    }

    @Test
    fun formatCountdown_partialMinute_roundsUp() {
        assertEquals("05:00", LockdownTimeLogic.formatCountdown(300_000L))
        assertEquals("04:59", LockdownTimeLogic.formatCountdown(299_500L))
        assertEquals("05:00", LockdownTimeLogic.formatCountdown(299_999L))
        assertEquals("05:01", LockdownTimeLogic.formatCountdown(300_001L))
    }

    @Test
    fun formatCountdown_neverNegative() {
        assertEquals("00:00", LockdownTimeLogic.formatCountdown(-1L))
        assertEquals("00:00", LockdownTimeLogic.formatCountdown(-60_000L))
    }

    @Test
    fun formatCountdown_59Seconds() {
        assertEquals("00:59", LockdownTimeLogic.formatCountdown(59_000L))
    }

    @Test
    fun formatCountdown_60Seconds() {
        assertEquals("01:00", LockdownTimeLogic.formatCountdown(60_000L))
    }

    @Test
    fun formatCountdown_hourAndMinute() {
        assertEquals("61:00", LockdownTimeLogic.formatCountdown(3_660_000L))
    }

    @Test
    fun formatCountdown_hoursAndMinutes() {
        assertEquals("61:00", LockdownTimeLogic.formatCountdown(3_660_000L))
    }

    @Test
    fun formatCountdown_exactSecondIsNotRoundedUp() {
        assertEquals("01:00", LockdownTimeLogic.formatCountdown(60_000L))
        assertEquals("01:30", LockdownTimeLogic.formatCountdown(90_000L))
    }

    @Test
    fun formatCountdown_exactMinuteIsNotRoundedUp() {
        assertEquals("60:00", LockdownTimeLogic.formatCountdown(3_600_000L))
    }

    // ---- addDays / previousDays / isSameDay ----

    @Test
    fun parseYearMonthDay_stringTokenisedCorrectly() {
        assertEquals(LocalDate.of(2025, Month.NOVEMBER, 22), DayMath.parseYearMonthDay("2025-11-22")!!)
        assertEquals(LocalDate.of(2026, Month.JANUARY, 4), DayMath.parseYearMonthDay("2026-01-04")!!)
    }

    @Test
    fun parseYearMonthDay_blank_isNull() {
        assertEquals(null, DayMath.parseYearMonthDay(""))
        assertEquals(null, DayMath.parseYearMonthDay("   "))
    }

    @Test
    fun parseYearMonthDay_invalid_isNull() {
        assertEquals(null, DayMath.parseYearMonthDay("notadate"))
        assertEquals(null, DayMath.parseYearMonthDay("2026-13-01"))
        assertEquals(null, DayMath.parseYearMonthDay("2026-02-30"))
        assertEquals(null, DayMath.parseYearMonthDay("2026-01-32"))
        assertEquals(null, DayMath.parseYearMonthDay("2026-01-0"))
    }

    @Test
    fun previousDay_beforeJanuaryFirst_isPreviousYear() {
        assertEquals(LocalDate.of(2025, Month.DECEMBER, 31), DayMath.previousDay(LocalDate.of(2026, Month.JANUARY, 1)))
    }

    @Test
    fun previousDay_leapYear() {
        assertEquals(LocalDate.of(2024, Month.FEBRUARY, 29), DayMath.previousDay(LocalDate.of(2024, Month.MARCH, 1)))
    }

    @Test
    fun addDays_positiveAndNegative() {
        assertEquals(LocalDate.of(2026, Month.JANUARY, 1), DayMath.addDays(LocalDate.of(2025, Month.DECEMBER, 31), 1))
        assertEquals(LocalDate.of(2025, Month.DECEMBER, 31), DayMath.addDays(LocalDate.of(2026, Month.JANUARY, 1), -1))
    }

    // ---- currentDayOfWeek ----

    @Test
    fun todayIs_correctDay() {
        val zone = ZoneId.of("Asia/Tehran")
        // At noon in Tehran, the day-of-week is the same regardless of
        // the specific date.
        val noonTehran = ZonedDateTime.of(
            LocalDateTime.of(2026, 1, 4, 12, 0),
            zone
        )
        assertEquals(
            java.time.DayOfWeek.SUNDAY,
            DayMath.todayIs(noonTehran)
        )
    }

    @Test
    fun localDateTimeToUtcEpoch() {
        // A datetime in Tehran is eight minutes ahead of UTC (IRST is
        // UTC+3:30).
        val zdt = ZonedDateTime.of(
            LocalDateTime.of(2026, 1, 4, 12, 0),
            ZoneId.of("Asia/Tehran")
        )
        val expectedMillis = zdt.toInstant().toEpochMilli()
        assertEquals(
            expectedMillis,
            DayMath.localDateTimeToUtcEpoch(
                LocalDateTime.of(2026, 1, 4, 12, 0),
                ZoneId.of("Asia/Tehran")
            )
        )
    }

    @Test
    fun localDateTime_sameInstantAsUtc() {
        // A datetime can be converted to an epoch, and that epoch
        // should equal the same datetime in UTC.
        val today = LocalDate.of(2026, Month.JANUARY, 1)
        assertEquals(
            today.atStartOfDay().atZone(ZoneId.of("UTC")),
            today.atStartOfDay().atZone(ZoneId.of("UTC"))
        )
    }

    // ---- checkStateTripleValid ----

    @Test
    fun checkStateTripleValid() {
        assertTrue(LockdownTimeLogic.checkStateTripleValid(true, false, false))
        assertTrue(LockdownTimeLogic.checkStateTripleValid(false, true, false))
        assertTrue(LockdownTimeLogic.checkStateTripleValid(false, false, true))
        assertTrue(LockdownTimeLogic.checkStateTripleValid(false, false, false))
        assertTrue(LockdownTimeLogic.checkStateTripleValid(true, true, true))
        assertFalse(LockdownTimeLogic.checkStateTripleValid(true, true, false))
        assertFalse(LockdownTimeLogic.checkStateTripleValid(true, false, true))
        assertFalse(LockdownTimeLogic.checkStateTripleValid(false, true, true))
    }

    @Test
    fun checkStateTripleValid_failsWhenAnyIsNull() {
        try {
            LockdownTimeLogic.checkStateTripleValid(null, false, false)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // expected
        }
        try {
            LockdownTimeLogic.checkStateTripleValid(false, null, false)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // expected
        }
        try {
            LockdownTimeLogic.checkStateTripleValid(false, false, null)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun checkStateTripleValid_trueWhenAnyFlagIsAlone() {
        assertTrue(LockdownTimeLogic.checkStateTripleValid(true, false, false))
        assertTrue(LockdownTimeLogic.checkStateTripleValid(false, true, false))
        assertTrue(LockdownTimeLogic.checkStateTripleValid(false, false, true))
    }
}
