package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Budget math for per-app daily limits, including the #26 one-minute-left
 * warning window. All functions under test are pure: the caller supplies
 * today's usage, so no device clock is involved.
 */
class AppLimitBudgetTest {

    // ---- remainingSeconds ----

    @Test
    fun remainingSecondsCountsDownToZero() {
        assertEquals(3600, AppLimitBudget.remainingSeconds(0, 60))
        assertEquals(1800, AppLimitBudget.remainingSeconds(1800, 60))
        assertEquals(1, AppLimitBudget.remainingSeconds(3599, 60))
        assertEquals(0, AppLimitBudget.remainingSeconds(3600, 60))
        assertEquals(0, AppLimitBudget.remainingSeconds(7200, 60))
    }

    @Test
    fun remainingSecondsIsZeroForNonPositiveLimit() {
        assertEquals(0, AppLimitBudget.remainingSeconds(0, 0))
        assertEquals(0, AppLimitBudget.remainingSeconds(100, -5))
    }

    // ---- final-minute warning window ----

    @Test
    fun warningWindowIsExactlyTheLastMinute() {
        // 61 seconds left: not yet.
        assertFalse(AppLimitBudget.isInFinalMinuteWarningWindow(3539, 60, isEnabled = true))
        // 60 seconds left: window opens.
        assertTrue(AppLimitBudget.isInFinalMinuteWarningWindow(3540, 60, isEnabled = true))
        // 1 second left: still in the window.
        assertTrue(AppLimitBudget.isInFinalMinuteWarningWindow(3599, 60, isEnabled = true))
        // 0 seconds left: budget is spent, the block (not the warning) applies.
        assertFalse(AppLimitBudget.isInFinalMinuteWarningWindow(3600, 60, isEnabled = true))
        assertFalse(AppLimitBudget.isInFinalMinuteWarningWindow(4000, 60, isEnabled = true))
    }

    @Test
    fun warningWindowRequiresEnabledPositiveLimit() {
        assertFalse(AppLimitBudget.isInFinalMinuteWarningWindow(3540, 60, isEnabled = false))
        assertFalse(AppLimitBudget.isInFinalMinuteWarningWindow(0, 0, isEnabled = true))
    }

    @Test
    fun oneMinuteWarningFiresOnlyOncePerDay() {
        assertTrue(
            AppLimitBudget.shouldShowOneMinuteWarning(
                usedSecondsToday = 3540,
                dailyLimitMinutes = 60,
                isEnabled = true,
                alreadyWarnedToday = false
            )
        )
        assertFalse(
            AppLimitBudget.shouldShowOneMinuteWarning(
                usedSecondsToday = 3540,
                dailyLimitMinutes = 60,
                isEnabled = true,
                alreadyWarnedToday = true
            )
        )
        // Outside the window it must not fire even if never warned.
        assertFalse(
            AppLimitBudget.shouldShowOneMinuteWarning(
                usedSecondsToday = 100,
                dailyLimitMinutes = 60,
                isEnabled = true,
                alreadyWarnedToday = false
            )
        )
    }

    // ---- pre-existing budget rules stay pinned ----

    @Test
    fun isExceededOnlyAtOrPastAnEnabledPositiveLimit() {
        assertFalse(AppLimitBudget.isExceeded(59, 60, isEnabled = true))
        assertTrue(AppLimitBudget.isExceeded(60, 60, isEnabled = true))
        assertTrue(AppLimitBudget.isExceeded(61, 60, isEnabled = true))
        assertFalse(AppLimitBudget.isExceeded(600, 60, isEnabled = false))
        assertFalse(AppLimitBudget.isExceeded(600, 0, isEnabled = true))
    }

    @Test
    fun minutesUntilNextMidnightIsAlwaysWithinOneDay() {
        val utc = 0L
        // Exactly midnight UTC: the NEXT reset is a full day away, never 0.
        assertEquals(1440, AppLimitBudget.minutesUntilNextMidnight(0L, utc))
        // One minute before midnight: 1 minute left.
        assertEquals(1, AppLimitBudget.minutesUntilNextMidnight(86_340_000L, utc))
        // Noon UTC: 12 hours left.
        assertEquals(720, AppLimitBudget.minutesUntilNextMidnight(43_200_000L, utc))
    }
}
