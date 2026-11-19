package com.example.data.blocklist

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #50 (batch 3): the blocklist is demanded ONLY at activation. The gate
 * condition is exactly "no real list update has ever landed" — the
 * persisted update timestamp is 0 while the on-disk list is still only
 * the built-in fallback seed (the seed records its domain count but
 * leaves the timestamp at 0), and any successful download stamps it.
 */
class BlocklistActivationGateTest {

    @Test
    fun `activation is blocked while no update has ever landed`() {
        assertTrue(BlocklistActivationGate.isActivationBlocked(0L))
    }

    @Test
    fun `a non-positive timestamp still counts as never updated`() {
        assertTrue(BlocklistActivationGate.isActivationBlocked(-1L))
    }

    @Test
    fun `any real update timestamp opens the gate`() {
        assertFalse(BlocklistActivationGate.isActivationBlocked(1L))
        assertFalse(BlocklistActivationGate.isActivationBlocked(1_763_000_000_000L))
    }
}
