package com.example.util

/**
 * #46 (batch 3): pure decision logic for the daily data-cap warning.
 *
 * Batch 2's #43 enforced a reached cap with a lockdown until midnight.
 * Mehdi redesigned it: when a transport's daily cap is reached, a
 * dedicated WARNING page goes up for that transport (not a lockdown)
 * and stays up while the transport is still ON, while the app races
 * the user to switch it off (Device Owner global setting → WifiManager
 * → Settings-page automation). Whichever reaches OFF first — the app
 * or the user — releases the warning, and the release is latched for
 * the rest of the day so the warning never nags twice.
 *
 * This object owns the testable decisions; ShieldAccessibilityService
 * performs the radio switching, the warning overlay and the logging.
 */
object DataCapEnforcer {

    /** A network transport a daily cap can govern. */
    enum class Transport { WIFI, MOBILE }

    /** Who won the race to switch a transport off. */
    enum class OffActor { BY_APP, MANUAL }

    /**
     * The local calendar day containing [nowMs], as a day number (days
     * since the epoch in local time). Two timestamps on the same local
     * day share a number; the once-per-day latch compares against it.
     */
    fun localDayNumber(nowMs: Long, zoneOffsetMs: Long): Long =
        Math.floorDiv(nowMs + zoneOffsetMs, 86_400_000L)

    /**
     * The transports whose cap is set (> 0 MB) and whose usage today has
     * reached it. A cap of 0 MB means "no cap" and can never exceed.
     */
    fun exceededTransports(
        wifiUsedBytes: Long,
        wifiCapMb: Int,
        mobileUsedBytes: Long,
        mobileCapMb: Int
    ): Set<Transport> {
        val exceeded = mutableSetOf<Transport>()
        if (DataUsageBudget.isExceeded(wifiUsedBytes, DataUsageBudget.mbToBytes(wifiCapMb))) {
            exceeded.add(Transport.WIFI)
        }
        if (DataUsageBudget.isExceeded(mobileUsedBytes, DataUsageBudget.mbToBytes(mobileCapMb))) {
            exceeded.add(Transport.MOBILE)
        }
        return exceeded
    }

    /**
     * Of the [exceeded] transports, the ones that need a NEW warning:
     * not already warned ([alreadyActive]) and not already released
     * today ([resolvedDayByTransport] holds the local day number of the
     * release). A transport released today is never warned again today,
     * even if the user turns it back on and keeps using it — the latch
     * resets at midnight together with the usage counters.
     */
    fun newWarnings(
        exceeded: Set<Transport>,
        alreadyActive: Set<Transport>,
        resolvedDayByTransport: Map<Transport, Long>,
        today: Long
    ): Set<Transport> = exceeded.filter { transport ->
        transport !in alreadyActive && resolvedDayByTransport[transport] != today
    }.toSet()

    /**
     * Splits the warned transports by their freshly observed state:
     * returns (stillOn, released). Only an explicit OFF observation
     * releases a transport — an unknown state (null) keeps the warning
     * up, because the page must not dismiss while the transport might
     * still be on.
     */
    fun partitionByState(
        active: Set<Transport>,
        isOn: (Transport) -> Boolean?
    ): Pair<Set<Transport>, Set<Transport>> {
        val released = active.filter { transport -> isOn(transport) == false }.toSet()
        return Pair(active - released, released)
    }

    /** Trigger word recorded in the block log when a warning releases. */
    fun triggerWordFor(transport: Transport, actor: OffActor): String = when (transport) {
        Transport.MOBILE -> if (actor == OffActor.BY_APP) "Data Cap: Mobile Off By App" else "Data Cap: Mobile Off Manual"
        Transport.WIFI -> if (actor == OffActor.BY_APP) "Data Cap: Wi-Fi Off By App" else "Data Cap: Wi-Fi Off Manual"
    }

    /** Precise block-log reason when a warning releases. */
    fun reasonFor(transport: Transport, actor: OffActor): String = when (transport) {
        Transport.MOBILE ->
            if (actor == OffActor.BY_APP) "Mobile data turned off by the app — daily cap reached"
            else "Mobile data turned off manually — daily cap reached"
        Transport.WIFI ->
            if (actor == OffActor.BY_APP) "Wi-Fi turned off by the app — daily cap reached"
            else "Wi-Fi turned off manually — daily cap reached"
    }
}
