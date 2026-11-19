package com.example.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Walks whole keyword-grace episodes through [KeywordGraceLogic], the way the
 * accessibility service, the 1-second ticker and the block screen use it.
 * Named "...FlowTest" on purpose, so it can sit next to any existing
 * KeywordGraceLogic unit test without replacing it.
 */
class KeywordGraceEpisodeFlowTest {

    private val word = "w"

    private fun block(decision: KeywordGraceLogic.MatchDecision): KeywordGraceLogic.MatchDecision.Block {
        assertTrue("expected a Block decision but was $decision", decision is KeywordGraceLogic.MatchDecision.Block)
        return decision as KeywordGraceLogic.MatchDecision.Block
    }

    private fun suppress(decision: KeywordGraceLogic.MatchDecision): KeywordGraceLogic.MatchDecision.Suppress {
        assertTrue("expected a Suppress decision but was $decision", decision is KeywordGraceLogic.MatchDecision.Suppress)
        return decision as KeywordGraceLogic.MatchDecision.Suppress
    }

    /** First sighting of [word] at time 0: the block that offers the button. */
    private fun firstBlock(): KeywordGraceLogic.MatchDecision.Block =
        block(KeywordGraceLogic.onKeywordMatch(KeywordGraceEpisode(), word, nowMs = 0L))


    // ---- the first block of an episode ----

    @Test
    fun firstMatch_blocksWithTheButton() {
        val decision = firstBlock()
        assertTrue(decision.graceAvailable)
        assertEquals(word, decision.episode.word)
        assertTrue(decision.episode.graceOffered)
        assertFalse(decision.episode.graceUsed)
    }

    @Test
    fun waitedOutBlock_nextBlockOfSameEpisodeHasNoButton() {
        val first = firstBlock()
        // The first block simply ran its course; the word is still there.
        val second = block(KeywordGraceLogic.onKeywordMatch(first.episode, word, nowMs = 400_000L))
        assertFalse(second.graceAvailable)
    }

    // ---- tapping the button ----

    @Test
    fun tap_marksGraceUsed_andOpensATenSecondWindow() {
        val tapped = KeywordGraceLogic.onGraceButtonTapped(firstBlock().episode, nowMs = 1_000L)
        assertTrue(tapped.graceUsed)
        assertEquals(1_000L + KeywordGraceLogic.GRACE_MILLIS, tapped.graceUntilMs)
        assertEquals(11_000L, tapped.graceUntilMs)
    }

    @Test
    fun tapTwice_changesNothing() {
        val once = KeywordGraceLogic.onGraceButtonTapped(firstBlock().episode, nowMs = 1_000L)

        val twice = KeywordGraceLogic.onGraceButtonTapped(once, nowMs = 5_000L)
        assertEquals(once, twice)
    }

    // ---- the free window ----

    @Test
    fun matchInsideWindow_isSuppressed_andLeavesTheEpisodeAlone() {
        val tapped = KeywordGraceLogic.onGraceButtonTapped(firstBlock().episode, nowMs = 1_000L)
        val decision = suppress(KeywordGraceLogic.onKeywordMatch(tapped, word, nowMs = 5_000L))
        assertEquals(tapped, decision.episode)
    }

    @Test
    fun windowBoundary_suppressedUntilTheExactEndInstant() {
        val tapped = KeywordGraceLogic.onGraceButtonTapped(firstBlock().episode, nowMs = 1_000L)
        // The window ends at 11_000: one millisecond earlier is still free,
        // the end instant itself is already a block.
        suppress(KeywordGraceLogic.onKeywordMatch(tapped, word, nowMs = 10_999L))
        block(KeywordGraceLogic.onKeywordMatch(tapped, word, nowMs = 11_000L))
    }

    // ---- after the window: the word is still on screen ----

    @Test
    fun matchAfterSpentWindow_blocksWithoutButton_andConsumesTheWindow() {
        val tapped = KeywordGraceLogic.onGraceButtonTapped(firstBlock().episode, nowMs = 1_000L)
        val decision = block(KeywordGraceLogic.onKeywordMatch(tapped, word, nowMs = 11_000L))
        assertFalse("grace was already used for this episode", decision.graceAvailable)
        assertTrue(decision.episode.graceUsed)
        assertEquals("the spent window must not stay pending", 0L, decision.episode.graceUntilMs)
    }


    @Test
    fun afterTheEventPathConsumedTheWindow_theTickerRecheckHasNothingLeftToFire() {
        val tapped = KeywordGraceLogic.onGraceButtonTapped(firstBlock().episode, nowMs = 1_000L)
        val reBlock = block(KeywordGraceLogic.onKeywordMatch(tapped, word, nowMs = 11_000L))
        // The service's re-check only runs while graceUsed && graceUntilMs > 0.
        val tickerWouldRecheck = reBlock.episode.graceUsed && reBlock.episode.graceUntilMs > 0L
        assertFalse(tickerWouldRecheck)
    }

    @Test
    fun everyLaterBlockOfTheEpisodeStaysButtonFree() {
        val tapped = KeywordGraceLogic.onGraceButtonTapped(firstBlock().episode, nowMs = 1_000L)
        var episode = block(KeywordGraceLogic.onKeywordMatch(tapped, word, nowMs = 11_000L)).episode
        for (now in listOf(12_000L, 60_000L, 600_000L)) {
            val decision = block(KeywordGraceLogic.onKeywordMatch(episode, word, nowMs = now))
            assertFalse("block at $now must not offer the button", decision.graceAvailable)
            episode = decision.episode
        }
    }

    // ---- the ticker's post-grace re-check ----

    @Test
    fun recheckConsumed_clearsOnlyThePendingWindow() {
        val tapped = KeywordGraceLogic.onGraceButtonTapped(firstBlock().episode, nowMs = 1_000L)
        val consumed = KeywordGraceLogic.onGraceRecheckConsumed(tapped)
        assertEquals(0L, consumed.graceUntilMs)
        assertTrue(consumed.graceUsed)
        assertEquals(word, consumed.word)
    }


    @Test
    fun recheckConsumed_thenWordStillThere_blocksWithoutButton() {
        val tapped = KeywordGraceLogic.onGraceButtonTapped(firstBlock().episode, nowMs = 1_000L)
        val consumed = KeywordGraceLogic.onGraceRecheckConsumed(tapped)
        val decision = block(KeywordGraceLogic.onKeywordMatch(consumed, word, nowMs = 12_000L))
        assertFalse(decision.graceAvailable)
    }

    // ---- the word goes away ----

    @Test
    fun absenceInsideTheWindow_keepsTheEpisode() {
        val tapped = KeywordGraceLogic.onGraceButtonTapped(firstBlock().episode, nowMs = 1_000L)
        // The word may just be briefly off-screen while the user is peeking.
        assertEquals(tapped, KeywordGraceLogic.onKeywordAbsent(tapped, nowMs = 5_000L))
    }

    @Test
    fun absenceAtOrAfterTheWindowEnd_clearsTheEpisode() {
        val tapped = KeywordGraceLogic.onGraceButtonTapped(firstBlock().episode, nowMs = 1_000L)
        assertEquals(KeywordGraceEpisode(), KeywordGraceLogic.onKeywordAbsent(tapped, nowMs = 11_000L))
        assertEquals(KeywordGraceEpisode(), KeywordGraceLogic.onKeywordAbsent(tapped, nowMs = 20_000L))
    }

    @Test
    fun absenceAfterRecheckConsumed_clearsTheEpisode() {
        val tapped = KeywordGraceLogic.onGraceButtonTapped(firstBlock().episode, nowMs = 1_000L)
        val consumed = KeywordGraceLogic.onGraceRecheckConsumed(tapped)
        assertEquals(KeywordGraceEpisode(), KeywordGraceLogic.onKeywordAbsent(consumed, nowMs = 12_000L))
    }

    @Test

    fun absenceOnAnEmptyEpisode_isANoOp() {
        val empty = KeywordGraceEpisode()
        assertEquals(empty, KeywordGraceLogic.onKeywordAbsent(empty, nowMs = 5_000L))
    }

    @Test
    fun newAppearanceAfterTheWordWasGone_getsTheButtonBack() {
        val tapped = KeywordGraceLogic.onGraceButtonTapped(firstBlock().episode, nowMs = 1_000L)
        val cleared = KeywordGraceLogic.onKeywordAbsent(tapped, nowMs = 20_000L)
        val decision = block(KeywordGraceLogic.onKeywordMatch(cleared, word, nowMs = 21_000L))
        assertTrue(decision.graceAvailable)
    }

    @Test
    fun waitedOutBlock_thenWordGone_thenBackAgain_offersTheButtonAgain() {
        val first = firstBlock() // button offered but never tapped
        val cleared = KeywordGraceLogic.onKeywordAbsent(first.episode, nowMs = 400_000L)
        val again = block(KeywordGraceLogic.onKeywordMatch(cleared, word, nowMs = 410_000L))
        assertTrue(again.graceAvailable)
    }

    // ---- a different word ----

    @Test
    fun differentWord_startsAFreshEpisodeWithItsOwnButton() {
        val tapped = KeywordGraceLogic.onGraceButtonTapped(firstBlock().episode, nowMs = 1_000L)
        val decision = block(KeywordGraceLogic.onKeywordMatch(tapped, "other", nowMs = 2_000L))
        assertTrue(decision.graceAvailable)
        assertEquals("other", decision.episode.word)
        assertNotEquals(tapped.word, decision.episode.word)
    }


    // ---- one complete episode, start to finish ----

    @Test
    fun fullEpisode_buttonOnce_thenNoButtonUntilTheWordIsGone() {
        // t=0: the word appears. Blocked, with the one-time button.
        val e1 = firstBlock()
        assertTrue(e1.graceAvailable)

        // t=2s: the user taps "10 seconds free".
        val e2 = KeywordGraceLogic.onGraceButtonTapped(e1.episode, nowMs = 2_000L)
        assertEquals(12_000L, e2.graceUntilMs)

        // t=5s: still inside the free window, so nothing blocks.
        val e3 = suppress(KeywordGraceLogic.onKeywordMatch(e2, word, nowMs = 5_000L))
        assertEquals(e2, e3.episode)

        // t=12s: the window is over and the word is still there. Blocked, no button.
        val e4 = block(KeywordGraceLogic.onKeywordMatch(e3.episode, word, nowMs = 12_000L))
        assertFalse(e4.graceAvailable)
        assertEquals(0L, e4.episode.graceUntilMs)

        // t=13s: still there. Still blocked, still no button.
        val e5 = block(KeywordGraceLogic.onKeywordMatch(e4.episode, word, nowMs = 13_000L))
        assertFalse(e5.graceAvailable)

        // t=20s: the word is gone, so the episode closes.
        val closed = KeywordGraceLogic.onKeywordAbsent(e5.episode, nowMs = 20_000L)
        assertEquals(KeywordGraceEpisode(), closed)

        // t=21s: it comes back. A brand-new episode, so the button is back.
        val e6 = block(KeywordGraceLogic.onKeywordMatch(closed, word, nowMs = 21_000L))
        assertTrue(e6.graceAvailable)

    }

    // ---- the ticker re-check pending state (the #1 fix) ----
    //
    // The bug being fixed: the ticker consumed its one-shot re-check
    // BEFORE scanning, and on a "word not found" verdict also cleared the
    // episode — but its scan reads only the extracted window tree, which
    // can miss a word the event path originally matched from an event's
    // own text payload. One missed scan spent the re-check forever, and
    // on a static page (no new events) the block never came back.
    // The fixed contract, exercised below: an absent re-check verdict
    // neither consumes nor clears; consumption happens on a found word
    // (re-block) or after MAX_RECHECK_ABSENT_SCANS absent verdicts, and
    // even then the episode is not cleared by the ticker.

    @Test
    fun recheckPending_onlyAfterAUsedWindowEnds() {
        val offered = firstBlock().episode
        // Button offered but never tapped: no window, no re-check owed.
        assertFalse(KeywordGraceLogic.isGraceRecheckPending(offered, nowMs = 400_000L))

        val tapped = KeywordGraceLogic.onGraceButtonTapped(offered, nowMs = 1_000L)
        // Inside the window the re-check is not due yet.
        assertFalse(KeywordGraceLogic.isGraceRecheckPending(tapped, nowMs = 10_999L))
        // At the exact end instant it becomes due, and stays due.
        assertTrue(KeywordGraceLogic.isGraceRecheckPending(tapped, nowMs = 11_000L))
        assertTrue(KeywordGraceLogic.isGraceRecheckPending(tapped, nowMs = 60_000L))
    }

    @Test
    fun recheckPending_clearsOnceConsumed_andNeedsWordAndUsedGrace() {
        val tapped = KeywordGraceLogic.onGraceButtonTapped(firstBlock().episode, nowMs = 1_000L)
        val consumed = KeywordGraceLogic.onGraceRecheckConsumed(tapped)
        assertFalse(KeywordGraceLogic.isGraceRecheckPending(consumed, nowMs = 60_000L))

        assertFalse(KeywordGraceLogic.isGraceRecheckPending(KeywordGraceEpisode(), nowMs = 60_000L))
        val unusedWindow = KeywordGraceEpisode(
            word = word, graceOffered = true, graceUsed = false, graceUntilMs = 5_000L
        )
        assertFalse(KeywordGraceLogic.isGraceRecheckPending(unusedWindow, nowMs = 60_000L))
    }

    @Test
    fun absentRecheckScans_consumeOnlyAtTheCap() {
        assertFalse(KeywordGraceLogic.shouldConsumeGraceRecheck(0))
        assertFalse(
            KeywordGraceLogic.shouldConsumeGraceRecheck(KeywordGraceLogic.MAX_RECHECK_ABSENT_SCANS - 1)
        )
        assertTrue(
            KeywordGraceLogic.shouldConsumeGraceRecheck(KeywordGraceLogic.MAX_RECHECK_ABSENT_SCANS)
        )
        assertTrue(
            KeywordGraceLogic.shouldConsumeGraceRecheck(KeywordGraceLogic.MAX_RECHECK_ABSENT_SCANS + 5)
        )
    }

    @Test
    fun missedRecheckScans_keepTheRecheckPending_andALaterMatchReblocksWithoutButton() {
        // t=0 block with button; t=2s the user taps; window ends at t=12s.
        val tapped = KeywordGraceLogic.onGraceButtonTapped(firstBlock().episode, nowMs = 2_000L)

        // The ticker's extracted-text scans at/after expiry keep missing
        // the still-present word. Below the absent cap nothing consumes
        // the re-check, so seconds later it is STILL pending against the
        // untouched episode.
        assertTrue(KeywordGraceLogic.isGraceRecheckPending(tapped, nowMs = 12_000L))
        assertTrue(KeywordGraceLogic.isGraceRecheckPending(tapped, nowMs = 15_000L))

        // When a later real content scan (the event path) finds the word,
        // the block comes back — WITHOUT the button.
        val reblock = block(KeywordGraceLogic.onKeywordMatch(tapped, word, nowMs = 30_000L))
        assertFalse(reblock.graceAvailable)
        assertTrue(reblock.episode.graceUsed)
    }

    @Test
    fun absentCap_consumesTheWindow_butTheEpisodeStillClosesOnRealAbsence() {
        val tapped = KeywordGraceLogic.onGraceButtonTapped(firstBlock().episode, nowMs = 1_000L)

        // The ticker gives up retrying at the cap and consumes the window…
        assertTrue(
            KeywordGraceLogic.shouldConsumeGraceRecheck(KeywordGraceLogic.MAX_RECHECK_ABSENT_SCANS)
        )
        val consumed = KeywordGraceLogic.onGraceRecheckConsumed(tapped)
        assertFalse(KeywordGraceLogic.isGraceRecheckPending(consumed, nowMs = 20_000L))

        // …but it does NOT clear the episode: a later match still
        // re-blocks without the button…
        val reblock = block(KeywordGraceLogic.onKeywordMatch(consumed, word, nowMs = 21_000L))
        assertFalse(reblock.graceAvailable)

        // …and a later genuine absence (event path) still closes the
        // episode, so the next appearance starts fresh, button included.
        val closed = KeywordGraceLogic.onKeywordAbsent(reblock.episode, nowMs = 22_000L)
        assertEquals(KeywordGraceEpisode(), closed)
        val fresh = block(KeywordGraceLogic.onKeywordMatch(closed, word, nowMs = 23_000L))
        assertTrue(fresh.graceAvailable)
    }
}
