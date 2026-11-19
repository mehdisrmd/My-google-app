package com.example.util

import org.bardframework.time.LocalDateJalali
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Jalali (Shamsi) calendar rendering for the Persian UI (#34, batch 2 —
 * the batch-1 attempt never took effect and FA dates stayed Gregorian,
 * e.g. "Nov 19").
 *
 * The Gregorian <-> Jalali conversion itself is NOT hand-rolled: it is
 * delegated to the established Bard Framework jalali-date library
 * (org.bardframework:jalali-date, Maven Central — the java.time-style
 * Jalali implementation from github.com/bardframework/jalali-date).
 * This object is only the app's thin facade over it: it keeps the label
 * shapes the UI already uses and the Persian digit/month-name
 * formatting. Timestamps stay Gregorian epoch millis everywhere; only
 * the displayed label changes when the app language is Persian.
 *
 * Anchors pinned by JalaliCalendarTest: 2024-03-20 = 1403-01-01
 * (Nowruz), 2025-03-20 = 1403-12-30 (leap-year Esfand 30),
 * 2026-11-19 = 1405-08-28.
 */
object JalaliCalendar {

    data class JalaliDate(val year: Int, val month: Int, val day: Int)

    val MONTH_NAMES_FA: List<String> = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    /** True when the Jalali year [jy] is a leap year (Esfand has 30 days). */
    fun isLeapYear(jy: Int): Boolean = LocalDateJalali.of(jy, 1, 1).isLeapYear

    /** Converts a Gregorian calendar date to its Jalali date. */
    fun fromGregorian(year: Int, month: Int, day: Int): JalaliDate {
        val jalali = LocalDateJalali.of(LocalDate.of(year, month, day))
        return JalaliDate(jalali.year, jalali.monthValue, jalali.dayOfMonth)
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
