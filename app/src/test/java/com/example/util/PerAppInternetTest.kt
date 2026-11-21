package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #63 (batch 5): the per-app internet rules codec and the firewall's
 * pure blocked-set policy — including Mehdi's priority rule that the
 * GLOBAL daily cap always wins over per-app limits.
 */
class PerAppInternetTest {

    // ---- codec ----

    @Test
    fun `codec round-trips rules`() {
        val rules = listOf(
            PerAppInternetRule("com.instagram.android", blockOnWifi = true, blockOnMobile = false, dailyCapMb = 500),
            PerAppInternetRule("org.telegram.messenger", blockOnWifi = false, blockOnMobile = true, dailyCapMb = 0),
            PerAppInternetRule("com.example.plain")
        )
        assertEquals(rules, PerAppInternetCodec.decode(PerAppInternetCodec.encode(rules)))
    }

    @Test
    fun `codec tolerates empty and garbage input`() {
        assertEquals(emptyList<PerAppInternetRule>(), PerAppInternetCodec.decode(null))
        assertEquals(emptyList<PerAppInternetRule>(), PerAppInternetCodec.decode(""))
        assertEquals(emptyList<PerAppInternetRule>(), PerAppInternetCodec.decode("   "))
    }

    @Test
    fun `codec clamps a negative cap to zero`() {
        // A corrupted/legacy stored string must never hand back a
        // negative cap (it would read as "always over the cap").
        val decoded = PerAppInternetCodec.decode(
            PerAppInternetCodec.encode(listOf(PerAppInternetRule("a.b.c", dailyCapMb = -50)))
        )
        assertEquals(0, decoded.single().dailyCapMb)
    }

    // ---- policy ----

    private val wifiBlocked = PerAppInternetRule("wifi.app", blockOnWifi = true)
    private val mobileBlocked = PerAppInternetRule("mobile.app", blockOnMobile = true)
    private val capped = PerAppInternetRule("capped.app", dailyCapMb = 100)
    private val plain = PerAppInternetRule("plain.app")

    @Test
    fun `hard blocks apply per transport`() {
        val rules = listOf(wifiBlocked, mobileBlocked, plain)
        assertEquals(
            setOf("wifi.app"),
            PerAppFirewallPolicy.effectiveBlockedPackages(
                rules, UnderlyingTransport.WIFI, emptyMap(), false
            )
        )
        assertEquals(
            setOf("mobile.app"),
            PerAppFirewallPolicy.effectiveBlockedPackages(
                rules, UnderlyingTransport.MOBILE, emptyMap(), false
            )
        )
    }

    @Test
    fun `nothing is blocked on other transports`() {
        val rules = listOf(wifiBlocked, mobileBlocked, capped)
        assertTrue(
            PerAppFirewallPolicy.effectiveBlockedPackages(
                rules, UnderlyingTransport.OTHER, mapOf("capped.app" to Long.MAX_VALUE), true
            ).isEmpty()
        )
    }

    @Test
    fun `per-app cap blocks only at or over the cap`() {
        val capBytes = DataUsageBudget.mbToBytes(100)
        assertTrue(
            PerAppFirewallPolicy.effectiveBlockedPackages(
                listOf(capped), UnderlyingTransport.WIFI,
                mapOf("capped.app" to capBytes - 1), false
            ).isEmpty()
        )
        assertEquals(
            setOf("capped.app"),
            PerAppFirewallPolicy.effectiveBlockedPackages(
                listOf(capped), UnderlyingTransport.WIFI,
                mapOf("capped.app" to capBytes), false
            )
        )
    }

    @Test
    fun `missing usage reads as zero - never blocks on a stats gap`() {
        assertTrue(
            PerAppFirewallPolicy.effectiveBlockedPackages(
                listOf(capped), UnderlyingTransport.MOBILE, emptyMap(), false
            ).isEmpty()
        )
    }

    @Test
    fun `global cap exceeded blocks every ruled app on that transport`() {
        val rules = listOf(wifiBlocked, mobileBlocked, capped, plain)
        // Mehdi's priority rule: the global cap wins outright — even
        // the rule with no blocks and no cap of its own is blocked,
        // because it HAS a rule in the section.
        assertEquals(
            setOf("wifi.app", "mobile.app", "capped.app", "plain.app"),
            PerAppFirewallPolicy.effectiveBlockedPackages(
                rules, UnderlyingTransport.WIFI, emptyMap(), true
            )
        )
    }

    @Test
    fun `global cap on one transport does not leak to the other`() {
        // The policy is always evaluated for ONE transport at a time;
        // a Wi-Fi global excess is passed as `true` only for WIFI.
        val rules = listOf(plain)
        assertTrue(
            PerAppFirewallPolicy.effectiveBlockedPackages(
                rules, UnderlyingTransport.MOBILE, emptyMap(), false
            ).isEmpty()
        )
    }
}
