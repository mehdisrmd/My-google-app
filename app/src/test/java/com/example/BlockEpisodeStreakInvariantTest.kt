package com.example

import android.content.Context
import com.example.data.preferences.ShieldPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Batch-2 regression tests for the streak and PIN semantics:
 *
 * #32 — a block episode (a lockdown starting and ending) must NOT reset
 * the clean streak; only the explicit Reset / "I Slipped" action may.
 * The automatic first-run wipe must not zero an existing streak either.
 *
 * #30 — when a lockdown ends, the PIN-relock flag is raised so the next
 * entry into the app UI passes the PIN gate again.
 */
@RunWith(RobolectricTestRunner::class)
class BlockEpisodeStreakInvariantTest {

    private val threeDaysMs = 3L * 24L * 60L * 60L * 1000L

    private fun rawPrefs() = RuntimeEnvironment.getApplication()
        .getSharedPreferences("shield_block_prefs", Context.MODE_PRIVATE)

    /** Anchors a streak that started exactly 3 days ago; returns its start. */
    private fun anchorThreeDayStreak(): Long {
        // Construct once so the first-run initialization has happened.
        ShieldPreferences(RuntimeEnvironment.getApplication())
        val start = System.currentTimeMillis() - threeDaysMs
        rawPrefs().edit().putLong("key_streak_start_time", start).apply()
        return start
    }

    @Test
    fun `a block episode does not reset the streak`() {
        val start = anchorThreeDayStreak()
        val prefs = ShieldPreferences(RuntimeEnvironment.getApplication())
        assertEquals(3, prefs.getStreakDays())

        // Exactly what a block episode does in preferences terms.
        prefs.startLockdown(
            durationMinutes = 5,
            triggerWord = "Adult Site Blocked: example",
            sourceApp = "Browser"
        )
        prefs.clearLockdown()

        assertEquals(start, prefs.streakStartTimestamp.value)
        assertEquals(3, prefs.getStreakDays())
    }

    @Test
    fun `the first-run wipe preserves an existing streak`() {
        val start = anchorThreeDayStreak()
        // Force the automatic first-run wipe to run again.
        rawPrefs().edit().putBoolean("key_fresh_scratch_v4_init", false).apply()
        val reinitialized = ShieldPreferences(RuntimeEnvironment.getApplication())
        assertEquals(start, reinitialized.streakStartTimestamp.value)
        assertEquals(3, reinitialized.getStreakDays())
    }

    @Test
    fun `ending a lockdown raises the pin relock flag`() {
        anchorThreeDayStreak()
        val prefs = ShieldPreferences(RuntimeEnvironment.getApplication())
        prefs.setPinRelockRequired(false)
        assertFalse(prefs.isPinRelockRequired())

        prefs.startLockdown(
            durationMinutes = 5,
            triggerWord = "Adult Site Blocked: example",
            sourceApp = "Browser"
        )
        prefs.clearLockdown()

        assertTrue(prefs.isPinRelockRequired())
        // And the flag survives a fresh preferences instance (process death).
        val reloaded = ShieldPreferences(RuntimeEnvironment.getApplication())
        assertTrue(reloaded.isPinRelockRequired())
    }
}
