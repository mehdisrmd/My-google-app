package com.example.util

/**
 * Pure character-level diff between what the user typed and a target sentence.
 *
 * Used by the type-to-confirm gate (#9) to show WHERE the typed text diverges
 * from the sentence that must be reproduced exactly: the differing part of the
 * user's text gets an orange underline, updated live while typing.
 *
 * Comparison is case-insensitive, mirroring the gate's confirm check
 * (`equals(target, ignoreCase = true)`), so correctly typed text is never
 * flagged just for letter case. No trimming is applied here — the caller
 * decides which strings to compare.
 */
object TextDiff {

    data class Result(
        /**
         * Index of the first position where the typed text diverges from the
         * target, or -1 when everything typed so far matches (the typed text
         * is a prefix of the target, or equal to it).
         */
        val firstMismatchIndex: Int,
        /**
         * Range inside the TYPED text that differs and should be underlined,
         * or null when there is nothing to highlight.
         */
        val underlineRange: IntRange?,
        /** True when the typed text equals the target (case-insensitive). */
        val isExactMatch: Boolean
    )

    fun diff(typed: String, target: String): Result {
        val prefix = commonPrefixLength(typed, target)

        // Everything typed so far matches the target.
        if (prefix == typed.length) {
            return if (prefix == target.length) {
                Result(firstMismatchIndex = -1, underlineRange = null, isExactMatch = true)
            } else {
                // Strict prefix: still typing, nothing wrong yet.
                Result(firstMismatchIndex = -1, underlineRange = null, isExactMatch = false)
            }
        }

        val suffix = commonSuffixLength(typed, target, prefix)
        val spanEndExclusive = typed.length - suffix
        val underlineRange = if (spanEndExclusive > prefix) {
            // A concrete differing span exists inside the typed text
            // (substitution, insertion, or extra trailing characters).
            prefix until spanEndExclusive
        } else {
            // Pure deletion in the middle: the missing chunk is not present in
            // the typed text, so the divergent span is empty. Highlight from
            // the mismatch point to the end of the typed text instead — from
            // that point on, the typed text is positionally shifted against
            // the target, which is exactly what the user needs to see.
            prefix until typed.length
        }
        return Result(
            firstMismatchIndex = prefix,
            underlineRange = underlineRange,
            isExactMatch = false
        )
    }

    private fun commonPrefixLength(a: String, b: String): Int {
        val limit = minOf(a.length, b.length)
        var i = 0
        while (i < limit && a[i].equals(b[i], ignoreCase = true)) i++
        return i
    }

    /**
     * Length of the common suffix of [a] and [b], never overlapping the
     * already-matched [prefix] region of either string.
     */
    private fun commonSuffixLength(a: String, b: String, prefix: Int): Int {
        val maxSuffix = minOf(a.length - prefix, b.length - prefix)
        var s = 0
        while (s < maxSuffix &&
            a[a.length - 1 - s].equals(b[b.length - 1 - s], ignoreCase = true)
        ) {
            s++
        }
        return s
    }
}
