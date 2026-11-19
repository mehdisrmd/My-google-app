package com.example.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #51 (batch 4): the foreign-VPN knockdown decisions — what counts as
 * a foreign VPN, the own-session guard, and the knock cooldown.
 */
class VpnKnockdownPolicyTest {

    @Test
    fun vpnTransportWithoutOwnSession_isForeign() {
        assertTrue(VpnKnockdownPolicy.isForeignVpn(hasVpnTransport = true, ownSessionActive = false))
    }

    @Test
    fun vpnTransportDuringOwnSession_isNotForeign() {
        assertFalse(VpnKnockdownPolicy.isForeignVpn(hasVpnTransport = true, ownSessionActive = true))
    }

    @Test
    fun noVpnTransport_isNotForeign() {
        assertFalse(VpnKnockdownPolicy.isForeignVpn(hasVpnTransport = false, ownSessionActive = false))
    }

    @Test
    fun ownSessionGuard_coversOnlyTheGuardWindow() {
        assertFalse(VpnKnockdownPolicy.isOwnSessionActive(nowMs = 10_000L, knockStartedMs = 0L))
        assertTrue(VpnKnockdownPolicy.isOwnSessionActive(nowMs = 10_000L, knockStartedMs = 9_000L))
        assertTrue(
            VpnKnockdownPolicy.isOwnSessionActive(
                nowMs = 10_000L,
                knockStartedMs = 10_000L - VpnKnockdownPolicy.OWN_SESSION_GUARD_MS + 1
            )
        )
        assertFalse(
            VpnKnockdownPolicy.isOwnSessionActive(
                nowMs = 10_000L,
                knockStartedMs = 10_000L - VpnKnockdownPolicy.OWN_SESSION_GUARD_MS
            )
        )
    }

    @Test
    fun firstKnock_isAlwaysAllowed() {
        assertTrue(VpnKnockdownPolicy.shouldKnock(nowMs = 5_000L, lastKnockMs = 0L))
    }

    @Test
    fun knockDuringCooldown_isSuppressed() {
        assertFalse(
            VpnKnockdownPolicy.shouldKnock(
                nowMs = 10_000L,
                lastKnockMs = 10_000L - VpnKnockdownPolicy.COOLDOWN_MS + 1
            )
        )
    }

    @Test
    fun knockAfterCooldown_isAllowed() {
        assertTrue(
            VpnKnockdownPolicy.shouldKnock(
                nowMs = 10_000L,
                lastKnockMs = 10_000L - VpnKnockdownPolicy.COOLDOWN_MS
            )
        )
    }
}
