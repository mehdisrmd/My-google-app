package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** #43 (batch 2): pure math behind the daily Wi-Fi / mobile data caps. */
class DataUsageBudgetTest {

    @Test
    fun `zero or negative cap means no cap`() {
        assertEquals(0L, DataUsageBudget.mbToBytes(0))
        assertEquals(0L, DataUsageBudget.mbToBytes(-5))
        assertFalse(DataUsageBudget.isExceeded(usedBytes = 10_000_000_000L, capBytes = 0L))
    }

    @Test
    fun `megabytes convert to binary bytes`() {
        assertEquals(250L * 1024 * 1024, DataUsageBudget.mbToBytes(250))
        assertEquals(1024L * 1024 * 1024, DataUsageBudget.mbToBytes(1024))
    }

    @Test
    fun `exceeded at exactly the cap, not one byte before`() {
        val cap = DataUsageBudget.mbToBytes(500)
        assertFalse(DataUsageBudget.isExceeded(cap - 1, cap))
        assertTrue(DataUsageBudget.isExceeded(cap, cap))
        assertTrue(DataUsageBudget.isExceeded(cap + 1, cap))
    }

    @Test
    fun `remaining never goes negative and is unbounded without a cap`() {
        val cap = DataUsageBudget.mbToBytes(100)
        assertEquals(cap - 40L, DataUsageBudget.remainingBytes(40L, cap))
        assertEquals(0L, DataUsageBudget.remainingBytes(cap + 1, cap))
        assertEquals(Long.MAX_VALUE, DataUsageBudget.remainingBytes(40L, 0L))
    }

    @Test
    fun `byte formatting is compact`() {
        assertEquals("0 B", DataUsageBudget.formatBytes(0))
        assertEquals("512 B", DataUsageBudget.formatBytes(512))
        assertEquals("2 KB", DataUsageBudget.formatBytes(2048))
        assertEquals("5 MB", DataUsageBudget.formatBytes(5L * 1024 * 1024))
        assertEquals("850 MB", DataUsageBudget.formatBytes(850L * 1024 * 1024))
        assertEquals("1.50 GB", DataUsageBudget.formatBytes(1536L * 1024 * 1024))
    }

    @Test
    fun `presets start with off and grow`() {
        assertEquals(0, DataUsageBudget.PRESET_CAPS_MB.first())
        assertEquals(
            DataUsageBudget.PRESET_CAPS_MB.sorted(),
            DataUsageBudget.PRESET_CAPS_MB
        )
    }
}
