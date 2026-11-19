package com.example.ui.lockdown

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The hours-aware countdown format used by the daily-limit block page
 * (#26c), whose timer runs until the midnight reset.
 */
class LockdownTimeLogicTest {

    @Test
    fun hhMmSsFallsBackToMmSsUnderOneHour() {
        assertEquals("00:00", LockdownTimeLogic.formatCountdownHhMmSs(0L))
        assertEquals("00:59", LockdownTimeLogic.formatCountdownHhMmSs(59_000L))
        assertEquals("01:00", LockdownTimeLogic.formatCountdownHhMmSs(60_000L))
        assertEquals("59:59", LockdownTimeLogic.formatCountdownHhMmSs(3_599_000L))
    }

    @Test
    fun hhMmSsShowsHoursAtAndPastOneHour() {
        assertEquals("01:00:00", LockdownTimeLogic.formatCountdownHhMmSs(3_600_000L))
        assertEquals("01:01:01", LockdownTimeLogic.formatCountdownHhMmSs(3_661_000L))
        assertEquals("12:00:00", LockdownTimeLogic.formatCountdownHhMmSs(43_200_000L))
        assertEquals("23:59:59", LockdownTimeLogic.formatCountdownHhMmSs(86_399_000L))
    }

    @Test
    fun hhMmSsRoundsUpAndNeverGoesNegative() {
        // 59.5s left must read 01:00, not 00:59: "00:00" means the block is over.
        assertEquals("01:00", LockdownTimeLogic.formatCountdownHhMmSs(59_500L))
        assertEquals("00:00", LockdownTimeLogic.formatCountdownHhMmSs(-5_000L))
    }
}
