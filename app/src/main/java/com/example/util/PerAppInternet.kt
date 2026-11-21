package com.example.util

import com.example.data.DataUsageBudget

/**
 * #63 (batch 5): one app's internet rule for the Per-App Internet
 * section.
 *
 *  - [blockOnWifi] / [blockOnMobile]: hard-block this app's traffic
 *    on that transport while the Block VPN guard is on.
 *  - [dailyCapMb]: per-app daily volume cap, measured per transport
 *    (NetworkStatsManager per-UID); once the app passes it on the
 *    transport in use, it is blocked on that transport until the day
 *    resets. 0 = no cap. Stored on the same discrete scale as the
 *    global caps ([DataUsageBudget.SLIDER_CAPS_MB]).
 */
data class PerAppInternetRule(
    val packageName: String,
    val blockOnWifi: Boolean = false,
    val blockOnMobile: Boolean = false,
    val dailyCapMb: Int = 0
)

/**
 * #63: codec for the rules list, stored in one preference string.
 * Same conventions as [com.example.data.blocklist.BlocklistSources]:
 * records separated by U+001E, fields by U+001F — characters that
 * cannot appear in a package name. Pure and unit-testable.
 */
object PerAppInternetCodec {

    private const val RECORD_SEPARATOR = '\u001E'
    private const val FIELD_SEPARATOR = '\u001F'

    fun encode(rules: List<PerAppInternetRule>): String =
        rules.joinToString(RECORD_SEPARATOR.toString()) { rule ->
            listOf(
                rule.packageName,
                if (rule.blockOnWifi) "1" else "0",
                if (rule.blockOnMobile) "1" else "0",
                rule.dailyCapMb.toString()
            ).joinToString(FIELD_SEPARATOR.toString())
        }

    fun decode(encoded: String?): List<PerAppInternetRule> {
        if (encoded.isNullOrBlank()) return emptyList()
        return encoded.split(RECORD_SEPARATOR).mapNotNull { record ->
            val fields = record.split(FIELD_SEPARATOR)
            val pkg = fields.getOrNull(0)?.trim().orEmpty()
            if (pkg.isEmpty()) return@mapNotNull null
            PerAppInternetRule(
                packageName = pkg,
                blockOnWifi = fields.getOrNull(1) == "1",
                blockOnMobile = fields.getOrNull(2) == "1",
                dailyCapMb = fields.getOrNull(3)?.toIntOrNull()?.coerceAtLeast(0) ?: 0
            )
        }
    }
}

/** #63: which underlying transport the phone is currently using. */
enum class UnderlyingTransport { WIFI, MOBILE, OTHER }

/**
 * #63: the pure decision for the per-app firewall — which packages
 * must be inside the drop-everything TUN right now.
 *
 * Mehdi's priority rule (2026-11-21): the GLOBAL daily cap (#43/#46)
 * always wins — when it is exceeded on the transport in use, EVERY
 * ruled app is blocked on that transport regardless of its own rule;
 * per-app limits only operate within what the global cap leaves.
 */
object PerAppFirewallPolicy {

    fun effectiveBlockedPackages(
        rules: List<PerAppInternetRule>,
        transport: UnderlyingTransport,
        usageBytesOnTransport: Map<String, Long>,
        globalCapExceededOnTransport: Boolean
    ): Set<String> {
        // On any other transport (ethernet, no network, …) there is
        // no Wi-Fi/mobile distinction to enforce against.
        if (transport == UnderlyingTransport.OTHER) return emptySet()
        val blocked = LinkedHashSet<String>()
        for (rule in rules) {
            val hardBlocked = when (transport) {
                UnderlyingTransport.WIFI -> rule.blockOnWifi
                UnderlyingTransport.MOBILE -> rule.blockOnMobile
                UnderlyingTransport.OTHER -> false
            }
            val capBytes = DataUsageBudget.mbToBytes(rule.dailyCapMb)
            val capExceeded = rule.dailyCapMb > 0 &&
                (usageBytesOnTransport[rule.packageName] ?: 0L) >= capBytes
            if (hardBlocked || capExceeded || globalCapExceededOnTransport) {
                blocked += rule.packageName
            }
        }
        return blocked
    }
}
