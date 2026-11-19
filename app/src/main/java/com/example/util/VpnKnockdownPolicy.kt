package com.example.util

/**
 * #51 (batch 4): pure decisions for the foreign-VPN knockdown — Mehdi's
 * design for closing the VPN-app gap in the #42 "Block VPN Setup"
 * toggle. Android allows only ONE active VPN at a time, so when a
 * foreign VPN is detected, X Blocker starts its own VpnService for a
 * split second: establishing our (empty, route-less) TUN makes the
 * system disconnect the foreign VPN, and we tear ours down immediately
 * — we never stay connected and never carry traffic.
 *
 * This object holds only the side-effect-free decisions (is what we see
 * a foreign VPN? is a knock allowed yet?) so they are unit-testable;
 * the Android-facing pieces live in ShieldVpnService and in
 * ShieldAccessibilityService.
 */
object VpnKnockdownPolicy {

    /**
     * Minimum gap between two knocks. An auto-reconnecting VPN app is
     * knocked again and again, but never in a tight establish/teardown
     * loop that would churn the system's VPN state every second.
     */
    const val COOLDOWN_MS = 10_000L

    /**
     * After we start a knock, our own brief TUN session must not be
     * mistaken for a foreign VPN. Our session lasts milliseconds; this
     * guard window covers the establish/teardown plus the system's
     * capability-callback lag.
     */
    const val OWN_SESSION_GUARD_MS = 3_000L

    /**
     * True when the system's active network has the VPN transport AND
     * that VPN is not the tail of our own knock.
     */
    fun isForeignVpn(hasVpnTransport: Boolean, ownSessionActive: Boolean): Boolean {
        return hasVpnTransport && !ownSessionActive
    }

    /** True while [nowMs] is still inside the guard window of a knock that started at [knockStartedMs]. */
    fun isOwnSessionActive(
        nowMs: Long,
        knockStartedMs: Long,
        guardMs: Long = OWN_SESSION_GUARD_MS
    ): Boolean {
        if (knockStartedMs <= 0L) return false
        return nowMs - knockStartedMs in 0 until guardMs
    }

    /**
     * True when a knock may fire: none has fired yet, or the cooldown
     * since the last one has fully elapsed.
     */
    fun shouldKnock(
        nowMs: Long,
        lastKnockMs: Long,
        cooldownMs: Long = COOLDOWN_MS
    ): Boolean {
        if (lastKnockMs <= 0L) return true
        return nowMs - lastKnockMs >= cooldownMs
    }
}
