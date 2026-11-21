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

    /**
     * #58 (batch 5): the daily cap is set with a SLIDER, not preset
     * chips. The slider's stops, in megabytes: 0 (off), then 50 MB up
     * to 20 GB (20480 MB) in steps that stay fine-grained at the small
     * end (where a day's budget is usually decided) and coarser at the
     * top. The stored preference stays a plain Int MB, exactly as #43
     * defined it — a cap stored by an older version maps to the
     * nearest stop when the slider is shown.
     */
    val SLIDER_CAPS_MB: List<Int> = listOf(
        0,
        50, 75, 100, 150, 200, 250, 300, 400, 500, 750,
        1024, 1536, 2048, 3072, 4096, 6144, 8192, 10240, 12288, 15360, 20480
    )

    /** The cap (MB) at slider stop [index], clamped to the scale. */
    fun capForSliderIndex(index: Int): Int =
        SLIDER_CAPS_MB[index.coerceIn(0, SLIDER_CAPS_MB.lastIndex)]

    /**
     * The slider stop nearest to a stored cap (MB). Exact matches win;
     * anything between two stops snaps to the closer one (ties go to
     * the lower stop), and anything above the top stop clamps to it.
     */
    fun sliderIndexFor(capMb: Int): Int {
        if (capMb <= 0) return 0
        var best = 0
        var bestDistance = Int.MAX_VALUE
        for ((index, stop) in SLIDER_CAPS_MB.withIndex()) {
            val distance = kotlin.math.abs(stop - capMb)
            if (distance < bestDistance) {
                bestDistance = distance
                best = index
            }
        }
        return best
    }

    /** Fraction of the cap used, 0f..1f; 0 when there is no cap. */
    fun usedFraction(usedBytes: Long, capBytes: Long): Float {
        if (capBytes <= 0L) return 0f
        return (usedBytes.toDouble() / capBytes.toDouble()).toFloat().coerceIn(0f, 1f)
    }

    /** Whole percent of the cap used; 0 when there is no cap. */
    fun usedPercent(usedBytes: Long, capBytes: Long): Int =
        (usedFraction(usedBytes, capBytes) * 100f).toInt()

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
