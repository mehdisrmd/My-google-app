package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.concurrent.TimeUnit

@Entity(tableName = "app_usage_limits")
data class AppUsageLimit(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int, // e.g. 120 for 2 hours
    val isEnabled: Boolean = true,
    val lastModifiedTimestamp: Long = 0L
) {
    fun isCooldownActive(nowMs: Long = System.currentTimeMillis()): Boolean {
        if (lastModifiedTimestamp <= 0L) return false
        return (nowMs - lastModifiedTimestamp) < COOLDOWN_DURATION_MS
    }

    fun getRemainingCooldownMillis(nowMs: Long = System.currentTimeMillis()): Long {
        if (lastModifiedTimestamp <= 0L) return 0L
        return (COOLDOWN_DURATION_MS - (nowMs - lastModifiedTimestamp)).coerceAtLeast(0L)
    }

    fun formatRemainingCooldown(nowMs: Long = System.currentTimeMillis()): String {
        val remainingMs = getRemainingCooldownMillis(nowMs)
        if (remainingMs <= 0L) return "Ready to edit"
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(remainingMs)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        val seconds = (TimeUnit.MILLISECONDS.toSeconds(remainingMs) % 60)
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    companion object {
        const val COOLDOWN_DURATION_MS: Long = 2L * 60L * 60L * 1000L // 2 Hours (7,200,000 ms)
    }
}
