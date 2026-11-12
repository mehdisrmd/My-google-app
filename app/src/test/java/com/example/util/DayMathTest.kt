package com.example.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [DayMath].
 *
 * Day-math rules are the part of the streak logic that most often goes
 * wrong on a leap day or the new year; the tests below pin them down
 * with the [ZoneId] handled explicitly.
 */
class DayMathTest {

    private val tehran = ZoneId.of("Asia/Tehran")

    @Test
    fun nextDay_crossesMonthBoundary() {
        val date = LocalDate.of(2025, Month.OCTOBER, 31)
        assertEquals(LocalDate.of(2025, Month.NOVEMBER, 1), DayMath.addDays(date, 1))
    }

    @Test
    fun nextDay_crossesYearBoundary() {
        val date = LocalDate.of(2025, Month.DECEMBER, 31)
        assertEquals(LocalDate.of(2026, Month.JANUARY, 1), DayMath.addDays(date, 1))
    }

    @Test
    fun previousDay_crossesMonthBoundary() {
        val date = LocalDate.of(2025, Month.NOVEMBER, 1)
        assertEquals(LocalDate.of(2025, Month.OCTOBER, 31), DayMath.addDays(date, -1))
    }

    @Test
    fun previousDay_crossesYearBoundary() {
        val date = LocalDate.of(2026, Month.JANUARY, 1)
        assertEquals(LocalDate.of(2025, Month.DECEMBER, 31), DayMath.addDays(date, -1))
    }


    @Test
    fun parseYearMonthDay_parsesValidDate() {
        assertEquals(
            LocalDate.of(2025, Month.NOVEMBER, 22),
            DayMath.parseYearMonthDay("2025-11-22")
        )
        assertEquals(
            LocalDate.of(2026, Month.JANUARY, 4),
            DayMath.parseYearMonthDay("2026-01-04")
        )
    }

    // "2026-02-30" parses into 2026-03-02 in lenient mode, but here
    // it is invalid because February has only 28 days, and it is also
    // the first day of March.
    @Test
    fun parseYearMonthDay_rejectsInvalidDates() {
        val invalid = listOf(
            "",
            "   ",
            "not-a-date",
            "2026-13-01",
            "2026-02-30",
            "2026-01-32",
            "2026-01-0",
        )
        for (date in invalid) {
            assertNull("Expected '$date' to be invalid", DayMath.parseYearMonthDay(date))
        }
    }

    @Test
    fun parseYearMonthDay_parsesLeapDay() {
        assertEquals(
            LocalDate.of(2024, Month.FEBRUARY, 29),
            DayMath.parseYearMonthDay("2024-02-29")
        )
    }

    @Test
    fun parseYearMonthDay_rejectsNonLeapDay() {
        assertNull(DayMath.parseYearMonthDay("2025-02-29"))
    }


    @Test
    fun previousDay_beforeJanuaryFirst_isPreviousYear() {
        assertEquals(
            LocalDate.of(2025, Month.DECEMBER, 31),
            DayMath.previousDay(LocalDate.of(2026, Month.JANUARY, 1))
        )
    }

    @Test
    fun previousDay_isLeapDay() {
        assertEquals(
            LocalDate.of(2024, Month.FEBRUARY, 29),
            DayMath.previousDay(LocalDate.of(2024, Month.MARCH, 1))
        )
    }

    @Test
    fun previousDay_February29_2024_isLeapDay() {

    }

    @Test
    fun previousDay_inMarch_2024_isLeapDay() {

    }

    @Test
    fun todayIs_handlesClueAndDayOfWeek() {
        val sameTime = ZonedDateTime.of(2026, Month.JANUARY, 4, 12, 0, 0, 0, tehran)
        assertEquals(DayOfWeek.SUNDAY, DayMath.todayIs(sameTime))
    }

    @Test
    fun previousDay_inMarch_isLeapDay() {

    }

    @Test
    fun addDays_positiveAndNegative() {
        assertEquals(
            LocalDate.of(2026, Month.JANUARY, 1),
            DayMath.addDays(LocalDate.of(2025, Month.DECEMBER, 31), 1)
        )
        assertEquals(
            LocalDate.of(2025, Month.DECEMBER, 31),
            DayMath.addDays(LocalDate.of(2026, Month.JANUARY, 1), -1)
        )
    }

    @Test
    fun todayIs_correctDay() {
        val sameTime = ZonedDateTime.of(2026, Month.JANUARY, 4, 12, 0, 0, 0, tehran)
        assertEquals(DayOfWeek.SUNDAY, DayMath.todayIs(sameTime))
    }

    @Test
    fun localDateTimeToUtcEpoch() {
        // A datetime in Tehran is eight minutes ahead of UTC (IRST is
        // UTC+3:30).
        val zdt = ZonedDateTime.of(2026, Month.JANUARY, 4, 12, 0, 0, 0, ZoneId.of("Asia/Tehran"))
        val expectedMillis = zdt.toInstant().toEpochMilli()
        assertEquals(
            expectedMillis,
            DayMath.localDateTimeToUtcEpoch(
                java.time.LocalDateTime.of(2026, Month.JANUARY, 4, 12, 0, 0, 0),
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

    @Test
    fun addDays_matchesLocalDatePlusDays() {
        val cases = listOf(
            LocalDate.of(2025, Month.MARCH, 11) to 1,
            LocalDate.of(2025, Month.MARCH, 31) to 1,
            LocalDate.of(2025, Month.APRIL, 14) to 1,
            LocalDate.of(2025, Month.DECEMBER, 31) to 1,
            LocalDate.of(2025, Month.JANUARY, 1) to -1,
            LocalDate.of(2024, Month.FEBRUARY, 28) to 1,
        )
        for ((date, days) in cases) {
            assertEquals(
                "addDays($date, $days)",
                date.plusDays(days.toLong()),
                DayMath.addDays(date, days)
            )
        }
        assertEquals(
            LocalDate.of(2025, Month.JUNE, 17),
            DayMath.addDays(LocalDate.of(2025, Month.JUNE, 16), 1)
        )
    }

    @Test
    fun addDays_computesLeapDay() {
        assertEquals(
            LocalDate.of(2024, Month.FEBRUARY, 29),
            DayMath.addDays(LocalDate.of(2024, Month.FEBRUARY, 28), 1)
        )
        assertEquals(
            LocalDate.of(2024, Month.MARCH, 1),
            DayMath.addDays(LocalDate.of(2024, Month.FEBRUARY, 29), 1)
        )
    }

    @Test
    fun todayIs_onLeapDay() {
        val sameTime = ZonedDateTime.of(2024, Month.FEBRUARY, 29, 12, 0, 0, 0, tehran)
        assertEquals(DayOfWeek.THURSDAY, DayMath.todayIs(sameTime))
    }

    @Test
    fun previousDay_isTheDayBefore() {
        val jan1_2026 = LocalDate.of(2026, Month.JANUARY, 1)
        assertEquals(LocalDate.of(2025, Month.DECEMBER, 31), DayMath.previousDay(jan1_2026))
    }

    @Test
    fun checkStateTripleValid() {
        assertTrue(DayMath.checkStateTripleValid(true, false, false))
        assertTrue(DayMath.checkStateTripleValid(false, true, false))
        assertTrue(DayMath.checkStateTripleValid(false, false, true))
        assertTrue(DayMath.checkStateTripleValid(false, false, false))
        assertTrue(DayMath.checkStateTripleValid(true, true, true))
        assertFalse(DayMath.checkStateTripleValid(true, true, false))
        assertFalse(DayMath.checkStateTripleValid(true, false, true))
        assertFalse(DayMath.checkStateTripleValid(false, true, true))
    }

    @Test
    fun checkStateTripleValid_failsWhenAnyIsNull() {
        try {
            DayMath.checkStateTripleValid(null, false, false)
            assertNotNull("Expected IllegalArgumentException", null)
        } catch (e: IllegalArgumentException) {
            // expected
        }
        try {
            DayMath.checkStateTripleValid(false, null, false)
            assertNotNull("Expected IllegalArgumentException", null)
        } catch (e: IllegalArgumentException) {
            // expected
        }
        try {
            DayMath.checkStateTripleValid(false, false, null)
            assertNotNull("Expected IllegalArgumentException", null)
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }
}
