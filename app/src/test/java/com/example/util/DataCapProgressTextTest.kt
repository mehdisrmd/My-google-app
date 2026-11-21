package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** #60 (batch 5): the data-cap progress readout lines. */
class DataCapProgressTextTest {

    @Test
    fun `no caps means no lines`() {
        assertTrue(
            DataCapProgressText.lines(
                wifiUsedBytes = 123_000_000L,
                wifiCapMb = 0,
                mobileUsedBytes = 456_000_000L,
                mobileCapMb = 0,
                isPersian = false
            ).isEmpty()
        )
    }

    @Test
    fun `one line per capped transport with used, left and percent`() {
        val lines = DataCapProgressText.lines(
            wifiUsedBytes = DataUsageBudget.mbToBytes(1024),
            wifiCapMb = 2048,
            mobileUsedBytes = 0L,
            mobileCapMb = 0,
            isPersian = false
        )
        assertEquals(1, lines.size)
        assertEquals("Wi-Fi: 1.00 GB of 2.00 GB used · 1.00 GB left (50%)", lines[0])
    }

    @Test
    fun `both transports produce two lines, wifi first`() {
        val lines = DataCapProgressText.lines(
            wifiUsedBytes = 0L,
            wifiCapMb = 100,
            mobileUsedBytes = DataUsageBudget.mbToBytes(250),
            mobileCapMb = 250,
            isPersian = false
        )
        assertEquals(2, lines.size)
        assertTrue(lines[0].startsWith("Wi-Fi:"))
        assertTrue(lines[1].startsWith("Mobile data:"))
        assertTrue(lines[1].contains("(100%)"))
    }

    @Test
    fun `persian lines use the persian labels`() {
        val lines = DataCapProgressText.lines(
            wifiUsedBytes = 0L,
            wifiCapMb = 100,
            mobileUsedBytes = 0L,
            mobileCapMb = 0,
            isPersian = true
        )
        assertEquals(1, lines.size)
        assertTrue(lines[0].startsWith("وای‌فای:"))
    }
}
