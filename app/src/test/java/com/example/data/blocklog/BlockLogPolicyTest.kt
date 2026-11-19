package com.example.data.blocklog

import com.example.data.model.BlockIncident
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-logic tests for the block log (#14): capture throttling, screenshot
 * file naming and sizing math, the retention cap, and reason labels.
 */
class BlockLogPolicyTest {

    // ---------- capture throttling ----------

    @Test
    fun `first capture is always allowed`() {
        assertTrue(
            BlockLogPolicy.shouldAttemptCapture(
                nowMs = 100_000L,
                lastCaptureAttemptMs = 0L,
                captureInFlight = false
            )
        )
    }

    @Test
    fun `capture inside the minimum interval is refused`() {
        val last = 100_000L
        assertFalse(
            BlockLogPolicy.shouldAttemptCapture(
                nowMs = last + BlockLogPolicy.MIN_CAPTURE_INTERVAL_MS - 1,
                lastCaptureAttemptMs = last,
                captureInFlight = false
            )
        )
    }

    @Test
    fun `capture at exactly the minimum interval is allowed`() {
        val last = 100_000L
        assertTrue(
            BlockLogPolicy.shouldAttemptCapture(
                nowMs = last + BlockLogPolicy.MIN_CAPTURE_INTERVAL_MS,
                lastCaptureAttemptMs = last,
                captureInFlight = false
            )
        )
    }

    @Test
    fun `an in-flight capture blocks any new attempt`() {
        assertFalse(
            BlockLogPolicy.shouldAttemptCapture(
                nowMs = 10_000_000L,
                lastCaptureAttemptMs = 0L,
                captureInFlight = true
            )
        )
    }

    // ---------- file naming ----------

    @Test
    fun `screenshot file names carry timestamp and sequence`() {
        assertEquals(
            "block_1700000000000_7.jpg",
            BlockLogPolicy.screenshotFileName(1_700_000_000_000L, 7L)
        )
    }

    // ---------- scaling math ----------

    @Test
    fun `portrait frames are capped on the longest side`() {
        assertEquals(
            486 to 1080,
            BlockLogPolicy.scaledDimensions(srcWidth = 1080, srcHeight = 2400)
        )
    }

    @Test
    fun `landscape frames are capped on the longest side`() {
        assertEquals(
            1080 to 540,
            BlockLogPolicy.scaledDimensions(srcWidth = 2000, srcHeight = 1000)
        )
    }

    @Test
    fun `small frames are never upscaled`() {
        assertEquals(
            400 to 300,
            BlockLogPolicy.scaledDimensions(srcWidth = 400, srcHeight = 300)
        )
        assertEquals(
            1080 to 1080,
            BlockLogPolicy.scaledDimensions(srcWidth = 1080, srcHeight = 1080)
        )
    }

    @Test
    fun `degenerate dimensions pass through untouched`() {
        assertEquals(0 to 500, BlockLogPolicy.scaledDimensions(0, 500))
        assertEquals(500 to -1, BlockLogPolicy.scaledDimensions(500, -1))
    }

    @Test
    fun `sample size brings the longest side within the bound`() {
        assertEquals(1, BlockLogPolicy.sampleSizeFor(1000, 800, 1080))
        assertEquals(1, BlockLogPolicy.sampleSizeFor(1080, 1080, 1080))
        assertEquals(2, BlockLogPolicy.sampleSizeFor(2160, 1200, 1080))
        assertEquals(4, BlockLogPolicy.sampleSizeFor(3840, 2160, 1080))
        assertEquals(1, BlockLogPolicy.sampleSizeFor(0, 2160, 1080))
        assertEquals(1, BlockLogPolicy.sampleSizeFor(2160, 2160, 0))
    }

    // ---------- retention cap ----------

    private fun incident(id: Long, timestamp: Long) = BlockIncident(
        id = id,
        timestamp = timestamp,
        matchedWord = "word$id",
        sourceApp = "Chrome",
        lockDurationMinutes = 5,
        screenshotPath = "block_${timestamp}_$id.jpg"
    )

    @Test
    fun `no pruning while the log is within the cap`() {
        val incidents = (1..5).map { incident(it.toLong(), 1_000L - it) }
        assertTrue(BlockLogPolicy.pruneVictims(incidents).isEmpty())
        assertTrue(BlockLogPolicy.pruneVictims(emptyList()).isEmpty())
    }

    @Test
    fun `pruning drops exactly the oldest tail beyond the cap`() {
        // Newest-first, as the DAO returns them.
        val incidents = (1..10).map { incident(it.toLong(), 10_000L - it * 100L) }
        val victims = BlockLogPolicy.pruneVictims(incidents, maxCount = 6)
        assertEquals(listOf(7L, 8L, 9L, 10L), victims.map { it.id })
    }

    @Test
    fun `pruning at the default cap keeps the newest 200`() {
        val incidents = (1..205).map { incident(it.toLong(), 100_000L - it) }
        val victims = BlockLogPolicy.pruneVictims(incidents)
        assertEquals(5, victims.size)
        assertEquals((201L..205L).toList(), victims.map { it.id })
    }

    @Test
    fun `a zero cap prunes everything`() {
        val incidents = (1..3).map { incident(it.toLong(), 1_000L - it) }
        assertEquals(3, BlockLogPolicy.pruneVictims(incidents, maxCount = 0).size)
    }

    // ---------- reason labels ----------

    @Test
    fun `reason labels follow the trigger word conventions`() {
        assertEquals("Adult site", BlockLogPolicy.reasonFor("Adult Site Blocked: example.com"))
        assertEquals("Daily app limit reached", BlockLogPolicy.reasonFor("Daily Limit (30m): TikTok"))
        assertEquals("Blocked app section", BlockLogPolicy.reasonFor("Section Blocked: Instagram Reels"))
        assertEquals("Scheduled focus session", BlockLogPolicy.reasonFor("Smart Focus: Deep Work (09:00 – 11:00)"))
        assertEquals("Tamper attempt", BlockLogPolicy.reasonFor("Prevent Clear Data Guard"))
        assertEquals("Tamper attempt", BlockLogPolicy.reasonFor("Prevent Uninstall Guard"))
        assertEquals("Manual panic lock", BlockLogPolicy.reasonFor("Emergency Panic Shield (15m Voluntary Lock)"))
        assertEquals("Blocked keyword", BlockLogPolicy.reasonFor("some custom word"))
    }
}
