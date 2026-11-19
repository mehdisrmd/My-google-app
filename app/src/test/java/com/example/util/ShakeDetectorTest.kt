package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

/**
 * Trace tests for #22's hard-shake detector (PLAN G11): synthetic
 * accelerometer traces are replayed through [ShakeDetector] at 50 Hz.
 * A deliberate hard shake must fire; walking, gentle/moderate shakes,
 * single jolts and slow shakes must NEVER fire — the owner's refinement
 * is that the slightest shake must not open the report box.
 */
class ShakeDetectorTest {

    private val g = ShakeDetector.GRAVITY_MPS2

    /** Replays a signal (t in ms -> x,y,z in m/s²); returns fire timestamps. */
    private fun runTrace(
        durationMs: Long,
        stepMs: Long = 20L,
        detector: ShakeDetector = ShakeDetector(),
        signal: (tMs: Long) -> Triple<Float, Float, Float>
    ): List<Long> {
        val fires = mutableListOf<Long>()
        var t = 0L
        while (t <= durationMs) {
            val (x, y, z) = signal(t)
            if (detector.onSample(t, x, y, z)) fires.add(t)
            t += stepMs
        }
        return fires
    }

    /** Hard back-and-forth shake along X: ±[amplitudeG] g at [hz], gravity on Z. */
    private fun hardShake(amplitudeG: Float, hz: Double): (Long) -> Triple<Float, Float, Float> =
        { t ->
            val x = (amplitudeG * g * sin(2.0 * PI * hz * t / 1000.0)).toFloat()
            Triple(x, 0f, g)
        }

    @Test
    fun hardShake_firesExactlyOncePerGesture() {
        val fires = runTrace(durationMs = 1_200L, signal = hardShake(3.4f, 4.5))
        assertEquals("one gesture = one fire (cooldown swallows the tail)", 1, fires.size)
    }

    @Test
    fun hardShake_onYAxis_alsoFires() {
        val fires = runTrace(durationMs = 1_200L) { t ->
            val y = (3.4f * g * sin(2.0 * PI * 4.5 * t / 1000.0)).toFloat()
            Triple(0f, y, g)
        }
        assertEquals(1, fires.size)
    }

    @Test
    fun hardShake_callbackInvokedOnceAndRearmsAfterCooldown() {
        var callbacks = 0
        val detector = ShakeDetector { callbacks++ }
        val fires = runTrace(durationMs = 5_000L, detector = detector, signal = hardShake(3.4f, 4.5))
        // First gesture fires, the 3 s cooldown passes mid-trace, and the
        // still-ongoing shaking counts as a new gesture and fires again.
        assertEquals(2, fires.size)
        assertEquals(fires.size, callbacks)
        assertTrue(fires[1] - fires[0] >= ShakeDetector.COOLDOWN_MS)
    }

    @Test
    fun walkingTrace_neverFires() {
        // ~1.9 steps/sec, vertical bounce peaking at ~1.45 g, small sway.
        val fires = runTrace(durationMs = 5_000L) { t ->
            val bounce = (0.45 * sin(2.0 * PI * 1.9 * t / 1000.0)).toFloat()
            val sway = (0.15 * sin(2.0 * PI * 0.9 * t / 1000.0)).toFloat()
            Triple(sway * g, 0f, (1f + bounce) * g)
        }
        assertTrue("walking must never fire, got $fires", fires.isEmpty())
    }

    @Test
    fun gentleShake_neverFires() {
        // ±1.9 g along X: total peak ≈ 2.15 g — a light, casual shake.
        val fires = runTrace(durationMs = 2_000L, signal = hardShake(1.9f, 4.0))
        assertTrue("gentle shake must never fire, got $fires", fires.isEmpty())
    }

    @Test
    fun moderateShake_belowThreshold_neverFires() {
        // ±2.4 g along X: firm shake, but total peak ≈ 2.6 g stays under
        // the 2.7 g excursion threshold.
        val fires = runTrace(durationMs = 2_000L, signal = hardShake(2.4f, 4.5))
        assertTrue("sub-threshold shake must never fire, got $fires", fires.isEmpty())
    }

    @Test
    fun singleJolt_neverFires() {
        val fires = runTrace(durationMs = 2_000L) { t ->
            when (t) {
                500L, 520L -> Triple(4.5f * g, 0f, g)   // one hard spike
                1500L, 1520L -> Triple(-4.5f * g, 0f, g) // and one back, 1 s later
                else -> Triple(0f, 0f, g)
            }
        }
        assertTrue("isolated jolts are not a shake pattern, got $fires", fires.isEmpty())
    }

    @Test
    fun slowHardShake_neverFires() {
        // Same 3.4 g amplitude but at 0.8 Hz: reversals land ~625 ms apart,
        // so three never fit inside the 700 ms window.
        val fires = runTrace(durationMs = 4_000L, signal = hardShake(3.4f, 0.8))
        assertTrue("slow shake must never fire, got $fires", fires.isEmpty())
    }

    @Test
    fun shortBurst_twoReversalsOnly_neverFires() {
        val shake = hardShake(3.4f, 4.5)
        val fires = runTrace(durationMs = 1_500L) { t ->
            if (t <= 350L) shake(t) else Triple(0f, 0f, g)
        }
        assertTrue("two reversals are not enough, got $fires", fires.isEmpty())
    }

    @Test
    fun restTrace_neverFires() {
        val fires = runTrace(durationMs = 1_000L) { Triple(0f, 0f, g) }
        assertTrue(fires.isEmpty())
    }

    @Test
    fun reset_clearsPartialPattern() {
        val detector = ShakeDetector()
        val shake = hardShake(3.4f, 4.5)
        // Build most of a pattern, reset, then build the same amount again:
        // neither half alone may fire.
        val first = runTrace(durationMs = 350L, detector = detector, signal = shake)
        detector.reset()
        val second = runTrace(durationMs = 350L, detector = detector, signal = shake)
        assertTrue(first.isEmpty())
        assertTrue(second.isEmpty())
    }
}
