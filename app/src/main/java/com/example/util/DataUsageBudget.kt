package com.example.util

import java.util.Locale

/**
 * #43 (batch 2): pure math for the daily data-usage caps — separate
 * budgets for Wi-Fi and mobile data. A cap of 0 MB means "no cap".
 * Enforcement (#46, batch 3): reaching a cap raises a warning page for
 * that transport and the app races the user to switch it off — see
 * DataCapEnforcer and ShieldAccessibilityService step 3b.
 */
object DataUsageBudget {

    /** The cap choices offered in the UI, in megabytes; 0 = off. */
    val PRESET_CAPS_MB: List<Int> = listOf(0, 250, 500, 1024, 2048, 5120, 10240)

    fun mbToBytes(capMb: Int): Long {
        if (capMb <= 0) return 0L
        return capMb.toLong() * 1024L * 1024L
    }

    /** True when a real cap is set and today's usage has reached it. */
    fun isExceeded(usedBytes: Long, capBytes: Long): Boolean {
        return capBytes > 0L && usedBytes >= capBytes
    }

    fun remainingBytes(usedBytes: Long, capBytes: Long): Long {
        if (capBytes <= 0L) return Long.MAX_VALUE
        return (capBytes - usedBytes).coerceAtLeast(0L)
    }

    /** Compact human label: "512 B", "2 KB", "850 MB", "1.50 GB". */
    fun formatBytes(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return String.format(Locale.US, "%.0f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024.0) return String.format(Locale.US, "%.0f MB", mb)
        val gb = mb / 1024.0
        return String.format(Locale.US, "%.2f GB", gb)
    }
}
