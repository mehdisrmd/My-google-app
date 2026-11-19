package com.example.util

import com.example.data.model.BlockedCategory

/**
 * #47 (batch 3): pure escalation policy for block presentations.
 *
 * Two independent scopes, deliberately kept apart so each can be tuned
 * without touching the other:
 *
 * SCOPE A — per-app daily time limits. Batch 1's #26 sent an over-limit
 * app straight to the block page with the until-midnight timer.
 * Mehdi's escalation: the first TWO opens of an over-limit app each
 * day only show a WARNING page and close the app; from the THIRD open
 * on, the timer block starts as before. There is NO 10-second grace
 * in this scope — the keyword grace (KeywordGraceLogic) belongs to
 * the keyword flow only and is never consulted here.
 *
 * SCOPE B — the other block sections (keyword/web blocks, adult
 * sites, Custom Block). The first block of a section each day presents
 * the WARNING variant of the block page (it blocks exactly like a
 * block — same lock, same timer); from the second block on that day,
 * the standard block page comes directly.
 *
 * The daily data-cap warning (#46) is NOT part of either scope; it has
 * its own design in [DataCapEnforcer].
 *
 * All decisions here are pure functions of "how many already today";
 * the day-keyed counters live in ShieldPreferences next to the usage
 * counters, so every count resets at the same local midnight.
 */
object BlockEscalationPolicy {

    // ---------------- Scope A: per-app daily time limits ----------------

    /** Opens of an over-limit app per day that earn a warning before the timer block. */
    const val APP_LIMIT_WARNING_OPENS: Int = 2

    /** What an over-limit open of an app gets. */
    enum class AppLimitAction { WARNING, BLOCK }

    /**
     * Decides an over-limit open from the number of opens ALREADY
     * counted today for the app: opens 1 and 2 (prior count 0 and 1)
     * warn; open 3 and beyond (prior count >= 2) start the timer
     * block.
     */
    fun appLimitAction(opensAlreadyToday: Int): AppLimitAction =
        if (opensAlreadyToday < APP_LIMIT_WARNING_OPENS) {
            AppLimitAction.WARNING
        } else {
            AppLimitAction.BLOCK
        }

    /** 1-based number of the warning an open earns (1 or 2 while it warns). */
    fun appLimitWarningNumber(opensAlreadyToday: Int): Int = opensAlreadyToday + 1

    // ---------------- Scope B: keyword / adult / custom blocks ----------------

    /** The block sections whose first daily block is a warning. */
    enum class Section { KEYWORD, ADULT, CUSTOM }

    /**
     * Which section a keyword-list match belongs to: the user's own
     * entries (the Custom Block lists) are CUSTOM; the shipped trigger
     * words and domains are KEYWORD. The blocklist-based adult path
     * (#12) does not pass through here — it is ADULT by construction.
     */
    fun sectionForKeywordCategory(category: BlockedCategory): Section = when (category) {
        BlockedCategory.CUSTOM_KEYWORD, BlockedCategory.CUSTOM_DOMAIN -> Section.CUSTOM
        BlockedCategory.ADULT_EXPLICIT, BlockedCategory.ADULT_DOMAIN -> Section.KEYWORD
    }

    /** True only for the first block of the day in a section. */
    fun showsWarningPresentation(blocksAlreadyToday: Int): Boolean = blocksAlreadyToday <= 0
}
