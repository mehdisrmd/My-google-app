package com.example.util

import com.example.util.DataCapEnforcer.OffActor
import com.example.util.DataCapEnforcer.Transport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #46 (batch 3): the data-cap warning state machine — a reached cap
 * warns (never locks), the warning persists while the transport is on,
 * releases on an explicit OFF observation, and is latched per
 * transport for the rest of the day.
 */
class DataCapEnforcerTest {

    private val mb = 1024L * 1024L

    // ---------- exceeded ----------

    @Test
    fun `only the transport that reached its cap is exceeded`() {
        assertEquals(
            setOf(Transport.MOBILE),
            DataCapEnforcer.exceededTransports(
                wifiUsedBytes = 100 * mb, wifiCapMb = 500,
                mobileUsedBytes = 500 * mb, mobileCapMb = 500
            )
        )
        assertEquals(
            setOf(Transport.WIFI),
            DataCapEnforcer.exceededTransports(
                wifiUsedBytes = 600 * mb, wifiCapMb = 500,
                mobileUsedBytes = 100 * mb, mobileCapMb = 500
            )
        )
    }

    @Test
    fun `both transports can exceed together and neither must`() {
        assertEquals(
            setOf(Transport.WIFI, Transport.MOBILE),
            DataCapEnforcer.exceededTransports(
                wifiUsedBytes = 600 * mb, wifiCapMb = 500,
                mobileUsedBytes = 600 * mb, mobileCapMb = 500
            )
        )
        assertTrue(
            DataCapEnforcer.exceededTransports(
                wifiUsedBytes = 10 * mb, wifiCapMb = 500,
                mobileUsedBytes = 10 * mb, mobileCapMb = 500
            ).isEmpty()
        )
    }

    @Test
    fun `a zero cap never exceeds`() {
        assertTrue(
            DataCapEnforcer.exceededTransports(
                wifiUsedBytes = 10_000 * mb, wifiCapMb = 0,
                mobileUsedBytes = 10_000 * mb, mobileCapMb = 0
            ).isEmpty()
        )
    }

    // ---------- warning trigger + latch ----------

    @Test
    fun `an exceeded transport warns once - not while active, never again after release today`() {
        val today = 20_000L
        // First reach: warns.
        assertEquals(
            setOf(Transport.WIFI),
            DataCapEnforcer.newWarnings(
                exceeded = setOf(Transport.WIFI),
                alreadyActive = emptySet(),
                resolvedDayByTransport = emptyMap(),
                today = today
            )
        )
        // Still up: no duplicate warning.
        assertTrue(
            DataCapEnforcer.newWarnings(
                exceeded = setOf(Transport.WIFI),
                alreadyActive = setOf(Transport.WIFI),
                resolvedDayByTransport = emptyMap(),
                today = today
            ).isEmpty()
        )
        // Released today, user turned it back on and exceeded again:
        // the latch stands — no second warning today.
        assertTrue(
            DataCapEnforcer.newWarnings(
                exceeded = setOf(Transport.WIFI),
                alreadyActive = emptySet(),
                resolvedDayByTransport = mapOf(Transport.WIFI to today),
                today = today
            ).isEmpty()
        )
        // Released YESTERDAY: the latch has reset with the counters.
        assertEquals(
            setOf(Transport.WIFI),
            DataCapEnforcer.newWarnings(
                exceeded = setOf(Transport.WIFI),
                alreadyActive = emptySet(),
                resolvedDayByTransport = mapOf(Transport.WIFI to today - 1),
                today = today
            )
        )
    }

    @Test
    fun `the latch is per transport`() {
        val today = 20_000L
        assertEquals(
            setOf(Transport.MOBILE),
            DataCapEnforcer.newWarnings(
                exceeded = setOf(Transport.WIFI, Transport.MOBILE),
                alreadyActive = emptySet(),
                resolvedDayByTransport = mapOf(Transport.WIFI to today),
                today = today
            )
        )
    }

    // ---------- persistence + release ----------

    @Test
    fun `warning persists while on or unknown and releases only on explicit off`() {
        val active = setOf(Transport.WIFI, Transport.MOBILE)
        val states = mapOf<Transport, Boolean?>(
            Transport.WIFI to true,     // still on
            Transport.MOBILE to null    // unknown — must NOT release
        )
        val (stillOn, released) = DataCapEnforcer.partitionByState(active) { states[it] }
        assertEquals(active, stillOn)
        assertTrue(released.isEmpty())
    }

    @Test
    fun `both transports release independently as each is observed off`() {
        val active = setOf(Transport.WIFI, Transport.MOBILE)
        val first = DataCapEnforcer.partitionByState(active) { transport ->
            if (transport == Transport.MOBILE) false else true
        }
        assertEquals(setOf(Transport.WIFI), first.first)
        assertEquals(setOf(Transport.MOBILE), first.second)

        val second = DataCapEnforcer.partitionByState(first.first) { false }
        assertTrue(second.first.isEmpty())
        assertEquals(setOf(Transport.WIFI), second.second)
    }

    // ---------- day math ----------

    @Test
    fun `local day number rolls at local midnight`() {
        val offset = 3L * 60 * 60 * 1000 // +03:00, for the shape of the math
        val justBeforeMidnight = 20_000L * 86_400_000L - offset - 1
        val justAfterMidnight = 20_000L * 86_400_000L - offset
        assertEquals(19_999L, DataCapEnforcer.localDayNumber(justBeforeMidnight, offset))
        assertEquals(20_000L, DataCapEnforcer.localDayNumber(justAfterMidnight, offset))
    }

    // ---------- log wording ----------

    @Test
    fun `trigger words and reasons record who won the race`() {
        assertEquals(
            "Data Cap: Mobile Off By App",
            DataCapEnforcer.triggerWordFor(Transport.MOBILE, OffActor.BY_APP)
        )
        assertEquals(
            "Data Cap: Wi-Fi Off Manual",
            DataCapEnforcer.triggerWordFor(Transport.WIFI, OffActor.MANUAL)
        )
        assertEquals(
            "Mobile data turned off by the app — daily cap reached",
            DataCapEnforcer.reasonFor(Transport.MOBILE, OffActor.BY_APP)
        )
        assertEquals(
            "Wi-Fi turned off manually — daily cap reached",
            DataCapEnforcer.reasonFor(Transport.WIFI, OffActor.MANUAL)
        )
    }
}
