package com.example.service

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService
import com.example.data.preferences.ShieldPreferences
import com.example.util.DataUsageBudget
import com.example.util.PerAppFirewallPolicy
import com.example.util.UnderlyingTransport

/**
 * #63 (batch 5): coordination point for the per-app internet
 * firewall.
 *
 * The firewall is usafe's own [ShieldVpnService] running a
 * persistent, drop-everything TUN that ONLY the currently blocked
 * apps are routed into (addAllowedApplication) — NetGuard-style, no
 * root and no Device Owner needed. It exists only while the Block
 * VPN guard (#42/#51) is ON (Mehdi's gating rule): guard off ⇒ no
 * firewall, section inactive.
 *
 * Priority (Mehdi's rule): the global daily cap (#43/#46) wins over
 * per-app limits — see [PerAppFirewallPolicy].
 *
 * The accessibility service re-evaluates on its data cadence (and
 * the VPN service re-requests evaluation when it is revoked); all
 * evaluation entry points run on background threads because they
 * read NetworkStatsManager.
 */
object PerAppFirewall {

    /** True while OUR firewall TUN is established. Read by the
     *  accessibility service so the #51 knockdown never mistakes our
     *  own firewall for a foreign VPN. */
    @Volatile
    var isActive: Boolean = false

    /** The package set the live TUN was built with. */
    @Volatile
    var appliedBlocked: Set<String> = emptySet()

    /** The most recent evaluation's desired set (diagnostics and the
     *  VPN service's knockdown-vs-firewall decision). */
    @Volatile
    var desiredBlocked: Set<String> = emptySet()

    fun currentTransport(context: Context): UnderlyingTransport {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return UnderlyingTransport.OTHER
        val network = cm.activeNetwork ?: return UnderlyingTransport.OTHER
        val caps = cm.getNetworkCapabilities(network) ?: return UnderlyingTransport.OTHER
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> UnderlyingTransport.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> UnderlyingTransport.MOBILE
            else -> UnderlyingTransport.OTHER
        }
    }

    /**
     * Recompute the desired blocked set from the live rules, the
     * current transport, per-app usage and the global caps, and
     * start/stop/rebuild the VPN service to match. No-op start when
     * the live TUN already matches.
     */
    fun evaluateAndApply(context: Context, preferences: ShieldPreferences) {
        val appContext = context.applicationContext
        if (!preferences.getIsVpnConfigBlockEnabled()) {
            desiredBlocked = emptySet()
            if (isActive) requestStop(appContext)
            return
        }
        val rules = preferences.getPerAppInternetRules()
        if (rules.isEmpty()) {
            desiredBlocked = emptySet()
            if (isActive) requestStop(appContext)
            return
        }
        // Without the one-time VPN consent (asked when the guard was
        // enabled) the TUN cannot be established; stay silent and let
        // the next evaluation retry.
        if (VpnService.prepare(appContext) != null) {
            desiredBlocked = emptySet()
            return
        }
        val transport = currentTransport(appContext)
        val deviceUsage = DataUsageMeter.todayUsageBytes(appContext, preferences)
        val globalExceeded = when (transport) {
            UnderlyingTransport.WIFI -> DataUsageBudget.isExceeded(
                deviceUsage.wifiBytes,
                DataUsageBudget.mbToBytes(preferences.getDataLimitWifiMb())
            )
            UnderlyingTransport.MOBILE -> DataUsageBudget.isExceeded(
                deviceUsage.mobileBytes,
                DataUsageBudget.mbToBytes(preferences.getDataLimitMobileMb())
            )
            UnderlyingTransport.OTHER -> false
        }
        val perAppUsage: Map<String, Long> =
            if (transport != UnderlyingTransport.OTHER && rules.any { it.dailyCapMb > 0 }) {
                rules.filter { it.dailyCapMb > 0 }.associate { rule ->
                    val uid = try {
                        appContext.packageManager.getApplicationInfo(rule.packageName, 0).uid
                    } catch (_: Exception) {
                        -1
                    }
                    val usage = if (uid >= 0) {
                        DataUsageMeter.todayUsageBytesForUid(appContext, uid)
                    } else {
                        DataUsageMeter.Usage(0L, 0L)
                    }
                    rule.packageName to when (transport) {
                        UnderlyingTransport.WIFI -> usage.wifiBytes
                        UnderlyingTransport.MOBILE -> usage.mobileBytes
                        UnderlyingTransport.OTHER -> 0L
                    }
                }
            } else {
                emptyMap()
            }
        val blocked = PerAppFirewallPolicy.effectiveBlockedPackages(
            rules = rules,
            transport = transport,
            usageBytesOnTransport = perAppUsage,
            globalCapExceededOnTransport = globalExceeded
        )
        desiredBlocked = blocked
        if (blocked.isEmpty()) {
            if (isActive) requestStop(appContext)
        } else if (!isActive || blocked != appliedBlocked) {
            requestApply(appContext, blocked)
        }
    }

    fun requestApply(context: Context, blocked: Set<String>) {
        try {
            val intent = Intent(context, ShieldVpnService::class.java)
                .setAction(ShieldVpnService.ACTION_FIREWALL)
                .putExtra(
                    ShieldVpnService.EXTRA_BLOCKED_PACKAGES,
                    blocked.toTypedArray()
                )
            context.startService(intent)
        } catch (_: Exception) {
            // Background-start restrictions etc.: the next evaluation
            // retries; never crash the caller over the firewall.
        }
    }

    fun requestStop(context: Context) {
        try {
            val intent = Intent(context, ShieldVpnService::class.java)
                .setAction(ShieldVpnService.ACTION_FIREWALL)
                .putExtra(ShieldVpnService.EXTRA_BLOCKED_PACKAGES, emptyArray<String>())
            context.startService(intent)
        } catch (_: Exception) {
        }
    }
}
