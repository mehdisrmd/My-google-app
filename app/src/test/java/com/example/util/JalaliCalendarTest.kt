package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * #34 (batch 2): the Jalali conversion must be exact — including leap
 * years — because every FA date in the app (streak calendar, block log,
 * incident details) renders through it. Anchors are independently known
 * calendar facts (Nowruz dates and leap-year Esfand lengths), and the
 * algorithm was transliteration-verified against the same anchors.
 */
class JalaliCalendarTest {

    private fun assertConversion(gy: Int, gm: Int, gd: Int, jy: Int, jm: Int, jd: Int) {
        assertEquals(
            "Gregorian $gy-$gm-$gd",
            JalaliCalendar.JalaliDate(jy, jm, jd),
            JalaliCalendar.fromGregorian(gy, gm, gd)
        )
    }

    @Test
    fun `nowruz anchors`() {
        assertConversion(2023, 3, 21, 1402, 1, 1)
        assertConversion(2024, 3, 20, 1403, 1, 1)
        assertConversion(2025, 3, 21, 1404, 1, 1)
        assertConversion(2026, 3, 21, 1405, 1, 1)
        assertConversion(2030, 3, 21, 1409, 1, 1)
    }

    @Test
    fun `esfand length follows leap years`() {
        // 1402 is not leap: its last day is Esfand 29 = 2024-03-19.
        assertConversion(2024, 3, 19, 1402, 12, 29)
        // 1403 IS leap: Esfand 30 exists = 2025-03-20.
        assertConversion(2025, 3, 20, 1403, 12, 30)
        // 1404 is not leap again.
        assertConversion(2026, 3, 20, 1404, 12, 29)
    }

    @Test
    fun `leap year detection`() {
        assertTrue(JalaliCalendar.isLeapYear(1399))
        assertTrue(JalaliCalendar.isLeapYear(1403))
        assertTrue(JalaliCalendar.isLeapYear(1408))
        assertFalse(JalaliCalendar.isLeapYear(1400))
        assertFalse(JalaliCalendar.isLeapYear(1402))
        assertFalse(JalaliCalendar.isLeapYear(1404))
        assertFalse(JalaliCalendar.isLeapYear(1405))
    }

    @Test
    fun `known dates including the batch-2 report date`() {
        // The day Mehdi reported "Nov 19" still showing in FA.
        assertConversion(2026, 11, 19, 1405, 8, 28)
        assertConversion(2000, 1, 1, 1378, 10, 11)
        assertConversion(1979, 2, 11, 1357, 11, 22)
        assertConversion(2024, 2, 29, 1402, 12, 10)
    }

    @Test
    fun `month boundaries`() {
        // First 6 months have 31 days, months 7-11 have 30.
        assertConversion(2026, 4, 20, 1405, 1, 31)
        assertConversion(2026, 4, 21, 1405, 2, 1)
        assertConversion(2026, 9, 22, 1405, 6, 31)
        assertConversion(2026, 9, 23, 1405, 7, 1)
        assertConversion(2026, 10, 22, 1405, 7, 30)
        assertConversion(2026, 10, 23, 1405, 8, 1)
    }

    @Test
    fun `epoch millis conversion and labels`() {
        val utc = TimeZone.getTimeZone("UTC")
        val calendar = Calendar.getInstance(utc)
        calendar.set(2026, Calendar.NOVEMBER, 19, 12, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val millis = calendar.timeInMillis

        assertEquals(
            JalaliCalendar.JalaliDate(1405, 8, 28),
            JalaliCalendar.fromEpochMillis(millis, utc)
        )
        assertEquals("۲۸", JalaliCalendar.dayNumberLabel(millis, isPersian = true, utc))
        assertEquals("19", JalaliCalendar.dayNumberLabel(millis, isPersian = false, utc))
        assertEquals("۲۸ آبان", JalaliCalendar.shortDateLabel(millis, isPersian = true, utc))
        assertEquals("Nov 19", JalaliCalendar.shortDateLabel(millis, isPersian = false, utc))
        assertEquals("۲۸ آبان ۱۴۰۵", JalaliCalendar.fullDateLabel(millis, isPersian = true, utc))
        assertEquals("Nov 19, 2026", JalaliCalendar.fullDateLabel(millis, isPersian = false, utc))
    }

    @Test
    fun `persian digits conversion`() {
        assertEquals("۱۴۰۵", JalaliCalendar.toPersianDigits("1405"))
        assertEquals("۲۸ آبان", JalaliCalendar.toPersianDigits("28 آبان"))
        assertEquals("", JalaliCalendar.toPersianDigits(""))
    }
}
