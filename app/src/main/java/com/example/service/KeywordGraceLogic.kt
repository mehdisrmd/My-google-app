package com.example.service

/**
 * Pure state machine for the trigger-word 10-second grace button
 * (Feature: Keyword Grace).
 *
 * One "episode" = one continuous presence of the same trigger word on
 * screen. Rules:
 *  - The first match of an episode blocks WITH the "10 seconds free" button.
 *    The button is offered once per episode: if that block is simply waited
 *    out (button never tapped), the next block of the same episode already
 *    comes WITHOUT the button.
 *  - Tapping the button marks the episode's grace as used and opens a
 *    10-second free window ([GRACE_MILLIS]) during which matches are
 *    suppressed (the user is looking at the content, unblocked).
 *  - When the window ends and the word is STILL on screen, the next match
 *    blocks again WITHOUT the button (grace already used for this episode).
 *  - Once the word disappears from the screen, the episode is cleared, so a
 *    genuinely new appearance starts a fresh episode with the button back.
 */
data class KeywordGraceEpisode(
    val word: String = "",
    val graceOffered: Boolean = false,
    val graceUsed: Boolean = false,
    val graceUntilMs: Long = 0L
)

object KeywordGraceLogic {

    const val GRACE_MILLIS: Long = 10_000L

    sealed interface MatchDecision {
        val episode: KeywordGraceEpisode

        /** Show the blocker. [graceAvailable] = show the 10s-free button. */
        data class Block(
            override val episode: KeywordGraceEpisode,
            val graceAvailable: Boolean
        ) : MatchDecision

        /** Do not block (the user is inside the 10-second free window). */
        data class Suppress(
            override val episode: KeywordGraceEpisode
        ) : MatchDecision
    }

    /**
     * A scan found [word] on screen at [nowMs]. Returns the decision plus the
     * (possibly updated) episode to persist.
     */
    fun onKeywordMatch(
        episode: KeywordGraceEpisode,
        word: String,
        nowMs: Long
    ): MatchDecision {
        val current = if (episode.word == word) episode else KeywordGraceEpisode(word = word)
        return if (nowMs < current.graceUntilMs) {
            MatchDecision.Suppress(current)
        } else {
            val available = !current.graceUsed && !current.graceOffered
            MatchDecision.Block(
                episode = current.copy(graceOffered = true),
                graceAvailable = available
            )
        }
    }

    /**
     * A scan completed and the episode's word was NOT on screen. Clears the
     * episode once no grace window is active, so the next appearance counts
     * as a brand-new episode. While a grace window is still running the
     * episode is kept: the word may just be momentarily off-screen, and the
     * post-grace re-check still needs to know the episode was used.
     */
    fun onKeywordAbsent(
        episode: KeywordGraceEpisode,
        nowMs: Long
    ): KeywordGraceEpisode {
        return if (episode.word.isNotEmpty() && nowMs >= episode.graceUntilMs) {
            KeywordGraceEpisode()
        } else {
            episode
        }
    }

    /**
     * The user tapped "10 seconds free" on the block screen. Opens the free
     * window exactly once per episode; further taps change nothing.
     */
    fun onGraceButtonTapped(
        episode: KeywordGraceEpisode,
        nowMs: Long
    ): KeywordGraceEpisode {
        return if (episode.graceUsed) {
            episode
        } else {
            episode.copy(
                graceUsed = true,
                graceUntilMs = nowMs + GRACE_MILLIS
            )
        }
    }
}
