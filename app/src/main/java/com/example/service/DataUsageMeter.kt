package com.example.service

import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.TrafficStats
import com.example.data.preferences.ShieldPreferences
import java.util.Calendar

/**
 * #43 (batch 2): today's data usage, split by transport (Wi-Fi vs
 * mobile), for the daily data caps.
 *
 * Primary source: [NetworkStatsManager.querySummaryForDevice] from
 * local midnight to now — the app already holds PACKAGE_USAGE_STATS
 * for the per-app limits. Fallback: [TrafficStats] since-boot counters
 * (mobile counters are separate there; Wi-Fi = total − mobile) made
 * "daily" with per-day baselines persisted in preferences. The
 * fallback underestimates after a mid-day reboot (counters restart at
 * zero while the baseline stays) — acceptable for a best-effort path;
 * the primary path is exact.
 */
object DataUsageMeter {

    data class Usage(val wifiBytes: Long, val mobileBytes: Long)

    fun todayUsageBytes(context: Context, preferences: ShieldPreferences): Usage {
        return try {
            networkStatsUsage(context) ?: trafficStatsUsage(preferences)
        } catch (_: Exception) {
            trafficStatsUsage(preferences)
        }
    }

    /**
     * #63 (batch 5): today's usage for ONE app (by UID), split by
     * transport — the per-app volume caps' meter. NetworkStatsManager
     * only (there is no per-UID TrafficStats); any failure reads as
     * zero usage, so a stats hiccup never blocks an app by itself.
     */
    fun todayUsageBytesForUid(context: Context, uid: Int): Usage {
        return try {
            val manager = context.getSystemService(Context.NETWORK_STATS_SERVICE)
                as? NetworkStatsManager ?: return Usage(0L, 0L)
            val start = startOfTodayMs()
            val end = System.currentTimeMillis()
            Usage(
                wifiBytes = sumUidBytes(manager, ConnectivityManager.TYPE_WIFI, start, end, uid),
                mobileBytes = sumUidBytes(manager, ConnectivityManager.TYPE_MOBILE, start, end, uid)
            )
        } catch (_: Exception) {
            Usage(0L, 0L)
        }
    }

    @Suppress("DEPRECATION")
    private fun sumUidBytes(
        manager: NetworkStatsManager,
        networkType: Int,
        start: Long,
        end: Long,
        uid: Int
    ): Long {
        val stats = manager.queryDetailsForUid(networkType, null, start, end, uid)
        var total = 0L
        try {
            val bucket = android.app.usage.NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                total += bucket.rxBytes + bucket.txBytes
            }
        } finally {
            stats.close()
        }
        return total
    }

    private fun networkStatsUsage(context: Context): Usage? {
        val manager = context.getSystemService(Context.NETWORK_STATS_SERVICE)
            as? NetworkStatsManager ?: return null
        val start = startOfTodayMs()
        val end = System.currentTimeMillis()
        @Suppress("DEPRECATION")
        val wifiBucket = manager.querySummaryForDevice(ConnectivityManager.TYPE_WIFI, null, start, end)
        @Suppress("DEPRECATION")
        val mobileBucket = manager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE, null, start, end)
        return Usage(
            wifiBytes = wifiBucket.rxBytes + wifiBucket.txBytes,
            mobileBytes = mobileBucket.rxBytes + mobileBucket.txBytes
        )
    }

    private fun trafficStatsUsage(preferences: ShieldPreferences): Usage {
        val totalRx = TrafficStats.getTotalRxBytes()
        val totalTx = TrafficStats.getTotalTxBytes()
        val mobileRx = TrafficStats.getMobileRxBytes()
        val mobileTx = TrafficStats.getMobileTxBytes()
        // TrafficStats.UNSUPPORTED is -1 on every counter.
        if (totalRx < 0 || totalTx < 0 || mobileRx < 0 || mobileTx < 0) {
            return Usage(0L, 0L)
        }
        val mobileNow = mobileRx + mobileTx
        val wifiNow = (totalRx + totalTx - mobileNow).coerceAtLeast(0L)
        val day = localDayKey(System.currentTimeMillis())
        if (preferences.getDataBaselineDay() != day) {
            // First read of a new day: anchor the baselines, usage is 0.
            preferences.setDataBaselines(day, wifiNow, mobileNow)
            return Usage(0L, 0L)
        }
        return Usage(
            wifiBytes = (wifiNow - preferences.getDataBaselineWifi()).coerceAtLeast(0L),
            mobileBytes = (mobileNow - preferences.getDataBaselineMobile()).coerceAtLeast(0L)
        )
    }

    private fun startOfTodayMs(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun localDayKey(nowMs: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = nowMs
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}-${cal.get(Calendar.DAY_OF_MONTH)}"
    }
}
