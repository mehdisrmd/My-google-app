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

    // ---- #58 (batch 5): the slider scale, 50 MB to 20 GB ----

    @Test
    fun `slider scale spans off, 50 MB and 20 GB, strictly growing`() {
        val scale = DataUsageBudget.SLIDER_CAPS_MB
        assertEquals(0, scale.first())
        assertEquals(50, scale[1])
        assertEquals(20480, scale.last())
        assertEquals(scale.sorted(), scale)
        assertEquals(scale.distinct(), scale)
    }

    @Test
    fun `slider index round-trips every stop`() {
        for ((index, cap) in DataUsageBudget.SLIDER_CAPS_MB.withIndex()) {
            assertEquals(cap, DataUsageBudget.capForSliderIndex(index))
            assertEquals(index, DataUsageBudget.sliderIndexFor(cap))
        }
    }

    @Test
    fun `slider index snaps stored values to the nearest stop`() {
        assertEquals(0, DataUsageBudget.sliderIndexFor(0))
        assertEquals(0, DataUsageBudget.sliderIndexFor(-10))
        // 5000 MB sits between 4096 and 6144 — closer to 4096.
        assertEquals(
            DataUsageBudget.SLIDER_CAPS_MB.indexOf(4096),
            DataUsageBudget.sliderIndexFor(5000)
        )
        // Above the top stop clamps to it.
        assertEquals(
            DataUsageBudget.SLIDER_CAPS_MB.lastIndex,
            DataUsageBudget.sliderIndexFor(999_999)
        )
        // Index clamping mirrors on the way back out.
        assertEquals(0, DataUsageBudget.capForSliderIndex(-3))
        assertEquals(20480, DataUsageBudget.capForSliderIndex(10_000))
    }

    // ---- #60 (batch 5): chart fraction / percent ----

    @Test
    fun `used fraction and percent are bounded`() {
        val cap = DataUsageBudget.mbToBytes(1024)
        assertEquals(0f, DataUsageBudget.usedFraction(0L, cap), 0.0001f)
        assertEquals(0.5f, DataUsageBudget.usedFraction(cap / 2, cap), 0.0001f)
        assertEquals(1f, DataUsageBudget.usedFraction(cap * 3, cap), 0.0001f)
        assertEquals(0f, DataUsageBudget.usedFraction(123L, 0L), 0.0001f)
        assertEquals(50, DataUsageBudget.usedPercent(cap / 2, cap))
        assertEquals(100, DataUsageBudget.usedPercent(cap * 3, cap))
        assertEquals(0, DataUsageBudget.usedPercent(123L, 0L))
    }
}
