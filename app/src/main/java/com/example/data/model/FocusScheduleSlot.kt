package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar
import java.util.Locale

@Entity(tableName = "focus_schedule_slots")
data class FocusScheduleSlot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val daysOfWeekCsv: String = "1,2,3,4,5,6,7", // 1=Mon .. 7=Sun
    val isEnabled: Boolean = true,
    val autoTriggerLockdown: Boolean = true
) {
    fun getActiveDaysSet(): Set<Int> {
        return daysOfWeekCsv.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .toSet()
    }

    fun formattedTimeRange(): String {
        return String.format(
            Locale.US,
            "%02d:%02d – %02d:%02d",
            startHour,
            startMinute,
            endHour,
            endMinute
        )
    }

    fun getDurationMinutes(): Int {
        val startTotal = startHour * 60 + startMinute
        val endTotal = endHour * 60 + endMinute
        val diff = if (endTotal > startTotal) {
            endTotal - startTotal
        } else if (endTotal < startTotal) {
            (24 * 60 - startTotal) + endTotal
        } else {
            60
        }
        return diff.coerceIn(1, 720)
    }

    fun isTimeWithinSlot(
        currentHour: Int,
        currentMinute: Int,
        isoDayOfWeek: Int
    ): Boolean {
        if (!isEnabled) return false
        val activeDays = getActiveDaysSet()
        if (isoDayOfWeek !in activeDays) return false

        val nowMins = currentHour * 60 + currentMinute
        val startMins = startHour * 60 + startMinute
        val endMins = endHour * 60 + endMinute

        return if (endMins > startMins) {
            nowMins in startMins until endMins
        } else if (endMins < startMins) {
            // Overnight slot (e.g., 23:00 - 01:00)
            nowMins >= startMins || nowMins < endMins
        } else {
            false
        }
    }

    fun getRemainingSlotMinutes(currentHour: Int, currentMinute: Int): Int {
        val nowMins = currentHour * 60 + currentMinute
        val startMins = startHour * 60 + startMinute
        val endMins = endHour * 60 + endMinute

        val remaining = if (endMins > startMins) {
            (endMins - nowMins).coerceAtLeast(1)
        } else {
            if (nowMins >= startMins) {
                (24 * 60 - nowMins) + endMins
            } else {
                (endMins - nowMins).coerceAtLeast(1)
            }
        }
        return remaining.coerceIn(1, 720)
    }

    fun formattedDaysSummary(): String {
        val days = getActiveDaysSet()
        if (days.size == 7) return "Every day"
        if (days == setOf(1, 2, 3, 4, 5)) return "Weekdays (Mon–Fri)"
        if (days == setOf(6, 7)) return "Weekends (Sat–Sun)"
        val labels = mapOf(
            1 to "Mon",
            2 to "Tue",
            3 to "Wed",
            4 to "Thu",
            5 to "Fri",
            6 to "Sat",
            7 to "Sun"
        )
        return days.sorted().mapNotNull { labels[it] }.joinToString(", ")
    }

    companion object {
        fun calendarDayToIsoDay(calendarDayOfWeek: Int): Int {
            return when (calendarDayOfWeek) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 1
            }
        }
    }
}
