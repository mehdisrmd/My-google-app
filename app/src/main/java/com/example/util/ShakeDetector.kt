package com.example.util

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Hard-shake detection for the shake-to-report feature (#22).
 *
 * The owner's hard requirement: the SLIGHTEST shake must never trigger the
 * report box — only a deliberate, violent shake may. A single acceleration
 * threshold cannot express that (a sharp tap or a caught phone can spike
 * high once), so this detector requires a full shake PATTERN:
 *
 *  1. An "excursion" is a burst of samples whose total acceleration reaches
 *     at least [SHAKE_THRESHOLD_G] g. Ordinary handling stays far below
 *     this: standing/walking peaks around 1.2–1.7 g, a gentle shake around
 *     2 g. A hard deliberate shake crosses 3 g repeatedly.
 *  2. Each excursion has a direction: the signed dominant axis at the
 *     moment the threshold is crossed (e.g. +X or −Z).
 *  3. A "reversal" is a new excursion whose direction differs from the
 *     previous excursion's — the phone was flung back the other way.
 *  4. The detector fires only after [REQUIRED_REVERSALS] reversals inside
 *     a sliding [REVERSAL_WINDOW_MS] window — i.e. four hard flings,
 *     back-and-forth, in well under a second.
 *  5. After firing, a [COOLDOWN_MS] cooldown swallows all samples, so one
 *     gesture opens the report box exactly once.
 *
 * Pure logic on purpose: the Android side (MainActivity) only forwards
 * accelerometer samples as (timestampMs, x, y, z) in m/s², and the whole
 * pattern math is unit-tested against synthetic traces (hard shake fires;
 * walking, gentle shakes, single jolts and slow shakes never do).
 */
class ShakeDetector(
    private val onShakeDetected: () -> Unit = {}
) {

    /** Signed dominant axis of the current/last excursion: ±1=X, ±2=Y, ±3=Z; 0 = none yet. */
    private var lastExcursionDirection = 0

    /** True while the current burst of samples is still above the threshold. */
    private var inExcursion = false

    /** Timestamps of the reversals still inside the sliding window. */
    private val reversalTimesMs = ArrayDeque<Long>()

    /** Samples are ignored until this timestamp (post-fire cooldown). */
    private var cooldownUntilMs = Long.MIN_VALUE

    /**
     * Feeds one accelerometer sample. Returns true exactly once per detected
     * hard shake (the same moment [onShakeDetected] is invoked).
     */
    fun onSample(timestampMs: Long, x: Float, y: Float, z: Float): Boolean {
        if (timestampMs < cooldownUntilMs) return false

        val gForce = sqrt(x * x + y * y + z * z) / GRAVITY_MPS2
        if (gForce < SHAKE_THRESHOLD_G) {
            // Below the line the current excursion (if any) is over; the
            // next crossing starts a fresh excursion with a fresh direction.
            inExcursion = false
            return false
        }
        if (inExcursion) return false
        inExcursion = true

        val direction = dominantDirection(x, y, z)
        if (lastExcursionDirection == 0) {
            lastExcursionDirection = direction
            return false
        }
        if (direction == lastExcursionDirection) return false

        // A fling in a new direction. Debounce: reversals implausibly close
        // together are one noisy excursion, not two flings — don't count
        // them, but do adopt the new direction so the pattern stays sane.
        val lastReversalMs = reversalTimesMs.lastOrNull()
        if (lastReversalMs != null && timestampMs - lastReversalMs < MIN_REVERSAL_INTERVAL_MS) {
            lastExcursionDirection = direction
            return false
        }
        lastExcursionDirection = direction

        reversalTimesMs.addLast(timestampMs)
        while (reversalTimesMs.isNotEmpty() &&
            timestampMs - reversalTimesMs.first() > REVERSAL_WINDOW_MS
        ) {
            reversalTimesMs.removeFirst()
        }

        if (reversalTimesMs.size < REQUIRED_REVERSALS) return false

        // Hard shake confirmed. Reset the pattern and cool down so the
        // tail of the same gesture cannot fire twice.
        reversalTimesMs.clear()
        lastExcursionDirection = 0
        inExcursion = false
        cooldownUntilMs = timestampMs + COOLDOWN_MS
        onShakeDetected()
        return true
    }

    /** Drops all pattern state (e.g. when the listener is re-registered). */
    fun reset() {
        lastExcursionDirection = 0
        inExcursion = false
        reversalTimesMs.clear()
        cooldownUntilMs = Long.MIN_VALUE
    }

    private fun dominantDirection(x: Float, y: Float, z: Float): Int {
        val ax = abs(x)
        val ay = abs(y)
        val az = abs(z)
        return when {
            ax >= ay && ax >= az -> if (x >= 0f) 1 else -1
            ay >= az -> if (y >= 0f) 2 else -2
            else -> if (z >= 0f) 3 else -3
        }
    }

    companion object {
        /** Standard gravity, for converting m/s² samples to g units. */
        const val GRAVITY_MPS2 = 9.80665f

        /**
         * Excursion threshold in g. Deliberately HIGH (owner's refinement):
         * walking peaks ≈1.7 g and gentle shakes ≈2.2 g stay under it; only
         * a hard fling of the phone crosses 2.7 g.
         */
        const val SHAKE_THRESHOLD_G = 2.7f

        /** Direction changes required inside the window before firing. */
        const val REQUIRED_REVERSALS = 3

        /** Sliding window the reversals must fit into. */
        const val REVERSAL_WINDOW_MS = 700L

        /** Reversals closer together than this are sensor noise, not flings. */
        const val MIN_REVERSAL_INTERVAL_MS = 50L

        /** After firing, samples are ignored for this long. */
        const val COOLDOWN_MS = 3_000L
    }
}
