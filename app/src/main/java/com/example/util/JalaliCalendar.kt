package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Real Jalali (Shamsi) calendar conversion for the Persian UI (#34,
 * batch 2 — the batch-1 attempt never took effect and FA dates stayed
 * Gregorian, e.g. "Nov 19").
 *
 * The conversion is the well-known arithmetic algorithm (the 33-year
 * cycle "breaks" table used by jalaali-js / Birashk), implemented with
 * plain integer math so it is fully unit-testable on the JVM. It is
 * render-only: timestamps stay Gregorian epoch millis everywhere; only
 * the displayed label changes when the app language is Persian.
 *
 * Verified against known anchors (transliteration-tested):
 * 2024-03-20 = 1403-01-01 (Nowruz), 2025-03-20 = 1403-12-30 (leap-year
 * Esfand 30), 2026-11-19 = 1405-08-28.
 */
object JalaliCalendar {

    data class JalaliDate(val year: Int, val month: Int, val day: Int)

    val MONTH_NAMES_FA: List<String> = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    private val BREAKS = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210,
        1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
    )

    private data class JalCalResult(val leap: Int, val gregorianYear: Int, val march: Int)

    /**
     * Core of the algorithm: for Jalali year [jy], returns how many years
     * have passed since the last leap year (0 = [jy] itself is leap), the
     * matching Gregorian year, and the March day on which Farvardin 1
     * falls in that Gregorian year.
     */
    private fun jalCal(jy: Int): JalCalResult {
        val gy = jy + 621
        var leapJ = -14
        var jp = BREAKS[0]
        var jump = 0
        for (i in 1 until BREAKS.size) {
            val jm = BREAKS[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += (jump / 33) * 8 + ((jump % 33) / 4)
            jp = jm
        }
        var n = jy - jp
        leapJ += (n / 33) * 8 + (((n % 33) + 3) / 4)
        if ((jump % 33) == 4 && jump - n == 4) {
            leapJ += 1
        }
        val leapG = (gy / 4) - (((gy / 100) + 1) * 3 / 4) - 150
        val march = 20 + leapJ - leapG
        if (jump - n < 6) {
            n = n - jump + (((jump + 4) / 33) * 33)
        }
        var leap = (((n + 1) % 33) - 1) % 4
        if (leap == -1) leap = 4
        return JalCalResult(leap = leap, gregorianYear = gy, march = march)
    }

    /** True when the Jalali year [jy] is a leap year (Esfand has 30 days). */
    fun isLeapYear(jy: Int): Boolean = jalCal(jy).leap == 0

    /** Gregorian date -> Julian day number. */
    private fun gregorianToJdn(gy: Int, gm: Int, gd: Int): Int {
        var d = ((gy + ((gm - 8) / 6) + 100100) * 1461) / 4 +
            (153 * ((gm + 9) % 12) + 2) / 5 + gd - 34840408
        d = d - (((gy + 100100 + ((gm - 8) / 6)) / 100) * 3) / 4 + 752
        return d
    }

    /** Julian day number -> Gregorian (year, month, day). */
    private fun jdnToGregorian(jdn: Int): Triple<Int, Int, Int> {
        var j = 4 * jdn + 139361631
        j += (((4 * jdn + 183187720) / 146097) * 3 / 4) * 4 - 3908
        val i = ((j % 1461) / 4) * 5 + 308
        val gd = ((i % 153) / 5) + 1
        val gm = ((i / 153) % 12) + 1
        val gy = (j / 1461) - 100100 + ((8 - gm) / 6)
        return Triple(gy, gm, gd)
    }

    /** Julian day number -> Jalali date. */
    private fun jdnToJalali(jdn: Int): JalaliDate {
        val (gy, _, _) = jdnToGregorian(jdn)
        var jy = gy - 621
        val cal = jalCal(jy)
        val jdn1Farvardin = gregorianToJdn(gy, 3, cal.march)
        var k = jdn - jdn1Farvardin
        if (k >= 0) {
            if (k <= 185) {
                // The first 6 months have 31 days each.
                return JalaliDate(jy, 1 + (k / 31), (k % 31) + 1)
            }
            k -= 186
        } else {
            // The date belongs to the previous Jalali year; its Esfand
            // has 30 days exactly when that year was leap, which in
            // jalCal(jy) terms is leap == 1.
            jy -= 1
            k += 179
            if (cal.leap == 1) {
                k += 1
            }
        }
        return JalaliDate(jy, 7 + (k / 30), (k % 30) + 1)
    }

    /** Converts a Gregorian calendar date to its Jalali date. */
    fun fromGregorian(year: Int, month: Int, day: Int): JalaliDate {
        return jdnToJalali(gregorianToJdn(year, month, day))
    }

    /** Converts an epoch-millis instant (in [timeZone]) to its Jalali date. */
    fun fromEpochMillis(epochMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): JalaliDate {
        val calendar = Calendar.getInstance(timeZone)
        calendar.timeInMillis = epochMillis
        return fromGregorian(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    /** Latin digits -> Persian digits (۰۱۲۳۴۵۶۷۸۹); other chars pass through. */
    fun toPersianDigits(input: String): String {
        val persianDigits = "۰۱۲۳۴۵۶۷۸۹"
        val builder = StringBuilder(input.length)
        for (ch in input) {
            builder.append(if (ch in '0'..'9') persianDigits[ch - '0'] else ch)
        }
        return builder.toString()
    }

    /**
     * Day-of-month number for calendar strips: the Gregorian number in
     * English (unchanged), the Jalali day in Persian digits when FA.
     */
    fun dayNumberLabel(
        epochMillis: Long,
        isPersian: Boolean,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        return if (isPersian) {
            toPersianDigits(fromEpochMillis(epochMillis, timeZone).day.toString())
        } else {
            val calendar = Calendar.getInstance(timeZone)
            calendar.timeInMillis = epochMillis
            calendar.get(Calendar.DAY_OF_MONTH).toString()
        }
    }

    /** "Nov 19" in English; "۲۸ آبان" in Persian. */
    fun shortDateLabel(
        epochMillis: Long,
        isPersian: Boolean,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        return if (isPersian) {
            val jalali = fromEpochMillis(epochMillis, timeZone)
            "${toPersianDigits(jalali.day.toString())} ${MONTH_NAMES_FA[jalali.month - 1]}"
        } else {
            val format = SimpleDateFormat("MMM d", Locale.US)
            format.timeZone = timeZone
            format.format(java.util.Date(epochMillis))
        }
    }

    /** "Nov 19, 2026" in English; "۲۸ آبان ۱۴۰۵" in Persian. */
    fun fullDateLabel(
        epochMillis: Long,
        isPersian: Boolean,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        return if (isPersian) {
            val jalali = fromEpochMillis(epochMillis, timeZone)
            "${toPersianDigits(jalali.day.toString())} ${MONTH_NAMES_FA[jalali.month - 1]} " +
                toPersianDigits(jalali.year.toString())
        } else {
            val format = SimpleDateFormat("MMM d, yyyy", Locale.US)
            format.timeZone = timeZone
            format.format(java.util.Date(epochMillis))
        }
    }

    /** "Nov 19, 6:37 PM" in English; "۲۸ آبان، ۱۸:۳۷" (24h) in Persian. */
    fun dateTimeLabel(
        epochMillis: Long,
        isPersian: Boolean,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        return if (isPersian) {
            val calendar = Calendar.getInstance(timeZone)
            calendar.timeInMillis = epochMillis
            val time = "%02d:%02d".format(
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE)
            )
            "${shortDateLabel(epochMillis, true, timeZone)}، ${toPersianDigits(time)}"
        } else {
            val format = SimpleDateFormat("MMM d, h:mm a", Locale.US)
            format.timeZone = timeZone
            format.format(java.util.Date(epochMillis))
        }
    }

    /** "6:37:05 PM" in English; "۱۸:۳۷:۰۵" (24h) in Persian. */
    fun timeLabel(
        epochMillis: Long,
        isPersian: Boolean,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        return if (isPersian) {
            val calendar = Calendar.getInstance(timeZone)
            calendar.timeInMillis = epochMillis
            val time = "%02d:%02d:%02d".format(
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND)
            )
            toPersianDigits(time)
        } else {
            val format = SimpleDateFormat("h:mm:ss a", Locale.US)
            format.timeZone = timeZone
            format.format(java.util.Date(epochMillis))
        }
    }
}
