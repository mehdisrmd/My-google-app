package com.example.util

import com.example.data.model.BlockedCategory
import com.example.util.BlockEscalationPolicy.AppLimitAction
import com.example.util.BlockEscalationPolicy.Section
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #47 (batch 3): the escalating block flow.
 *
 * Scope A — per-app daily limits: opens 1–2 of an over-limit app warn
 * (warning page + close), open 3+ starts the timer block; the count is
 * per app and per day. The escalation decision is a pure function of
 * the open count — there is deliberately no grace input, pinning the
 * "no 10-second grace in this section" rule: KeywordGraceLogic is a
 * keyword-flow machine and the daily-limit path never consults it.
 *
 * Scope B — keyword / adult / custom sections: only the first block
 * of a section each day presents the warning page; sections count
 * independently and reset with the day.
 */
class BlockEscalationPolicyTest {

    // ---------- Scope A: the escalation ladder ----------

    @Test
    fun `first two opens of an over-limit app warn, numbered 1 and 2`() {
        assertEquals(AppLimitAction.WARNING, BlockEscalationPolicy.appLimitAction(0))
        assertEquals(AppLimitAction.WARNING, BlockEscalationPolicy.appLimitAction(1))
        assertEquals(1, BlockEscalationPolicy.appLimitWarningNumber(0))
        assertEquals(2, BlockEscalationPolicy.appLimitWarningNumber(1))
    }

    @Test
    fun `third open and beyond start the timer block`() {
        assertEquals(AppLimitAction.BLOCK, BlockEscalationPolicy.appLimitAction(2))
        assertEquals(AppLimitAction.BLOCK, BlockEscalationPolicy.appLimitAction(3))
        assertEquals(AppLimitAction.BLOCK, BlockEscalationPolicy.appLimitAction(25))
    }

    @Test
    fun `escalation counts per app and resets with the day`() {
        // A day-keyed fake of the preferences counters: (day, package)
        // -> opens already counted today.
        val counters = mutableMapOf<Pair<Long, String>, Int>()
        fun open(day: Long, pkg: String): AppLimitAction {
            val already = counters[day to pkg] ?: 0
            val action = BlockEscalationPolicy.appLimitAction(already)
            counters[day to pkg] = already + 1
            return action
        }

        val day1 = 20_000L
        val day2 = day1 + 1

        assertEquals(AppLimitAction.WARNING, open(day1, "com.a"))
        assertEquals(AppLimitAction.WARNING, open(day1, "com.a"))
        // A different app the same day starts its own ladder at zero.
        assertEquals(AppLimitAction.WARNING, open(day1, "com.b"))
        // The third open of com.a blocks.
        assertEquals(AppLimitAction.BLOCK, open(day1, "com.a"))
        // Tomorrow, com.a starts over with warnings.
        assertEquals(AppLimitAction.WARNING, open(day2, "com.a"))
        assertEquals(AppLimitAction.WARNING, open(day2, "com.a"))
        assertEquals(AppLimitAction.BLOCK, open(day2, "com.a"))
    }

    // ---------- Scope B: first block of a section warns ----------

    @Test
    fun `only the first block of a section today is a warning`() {
        assertTrue(BlockEscalationPolicy.showsWarningPresentation(0))
        assertFalse(BlockEscalationPolicy.showsWarningPresentation(1))
        assertFalse(BlockEscalationPolicy.showsWarningPresentation(2))
        assertFalse(BlockEscalationPolicy.showsWarningPresentation(9))
    }

    @Test
    fun `sections count independently and reset with the day`() {
        // A day-keyed fake of the preferences counters: (day, section)
        // -> blocks already counted today; returns whether the block
        // presents the warning page.
        val counters = mutableMapOf<Pair<Long, Section>, Int>()
        fun block(day: Long, section: Section): Boolean {
            val already = counters[day to section] ?: 0
            val warning = BlockEscalationPolicy.showsWarningPresentation(already)
            counters[day to section] = already + 1
            return warning
        }

        val day1 = 20_000L
        val day2 = day1 + 1

        assertTrue(block(day1, Section.KEYWORD))
        assertFalse(block(day1, Section.KEYWORD))
        // ADULT and CUSTOM still have their own first warning today.
        assertTrue(block(day1, Section.ADULT))
        assertTrue(block(day1, Section.CUSTOM))
        assertFalse(block(day1, Section.ADULT))
        assertFalse(block(day1, Section.CUSTOM))
        // Tomorrow every section warns once again.
        assertTrue(block(day2, Section.KEYWORD))
        assertTrue(block(day2, Section.ADULT))
        assertTrue(block(day2, Section.CUSTOM))
    }

    @Test
    fun `keyword categories map to sections`() {
        assertEquals(
            Section.KEYWORD,
            BlockEscalationPolicy.sectionForKeywordCategory(BlockedCategory.ADULT_EXPLICIT)
        )
        assertEquals(
            Section.KEYWORD,
            BlockEscalationPolicy.sectionForKeywordCategory(BlockedCategory.ADULT_DOMAIN)
        )
        assertEquals(
            Section.CUSTOM,
            BlockEscalationPolicy.sectionForKeywordCategory(BlockedCategory.CUSTOM_KEYWORD)
        )
        assertEquals(
            Section.CUSTOM,
            BlockEscalationPolicy.sectionForKeywordCategory(BlockedCategory.CUSTOM_DOMAIN)
        )
    }
}
