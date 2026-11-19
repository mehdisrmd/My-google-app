package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextDiffTest {

    private val target =
        "I am the master of my mind and I refuse to trade my clean streak chain for a temporary urge"

    // ---- exact / prefix states: nothing is highlighted ----

    @Test
    fun diff_exactMatch_reportsExactAndNoUnderline() {
        val result = TextDiff.diff(target, target)
        assertTrue(result.isExactMatch)
        assertEquals(-1, result.firstMismatchIndex)
        assertNull(result.underlineRange)
    }

    @Test
    fun diff_exactMatchIgnoringCase_reportsExact() {
        val result = TextDiff.diff(target.lowercase(), target)
        assertTrue(result.isExactMatch)
        assertNull(result.underlineRange)
    }

    @Test
    fun diff_emptyTyped_noUnderline() {
        val result = TextDiff.diff("", target)
        assertFalse(result.isExactMatch)
        assertEquals(-1, result.firstMismatchIndex)
        assertNull(result.underlineRange)
    }

    @Test
    fun diff_correctPrefixWhileTyping_noUnderline() {
        val result = TextDiff.diff("I am the master of my", target)
        assertFalse(result.isExactMatch)
        assertEquals(-1, result.firstMismatchIndex)
        assertNull(result.underlineRange)
    }

    // ---- the reported #9 case: the word "chain" was omitted ----

    @Test
    fun diff_omittedWord_pointsAtOmissionAndUnderlinesTail() {
        val typed =
            "I am the master of my mind and I refuse to trade my clean streak for a temporary urge"
        val result = TextDiff.diff(typed, target)

        assertFalse(result.isExactMatch)
        // The mismatch starts exactly where "chain" should have been typed.
        assertEquals(target.indexOf("chain"), result.firstMismatchIndex)
        // A mid-text deletion leaves an empty divergent span in the typed
        // text, so the highlight runs from the omission point to the end.
        assertEquals(
            target.indexOf("chain") until typed.length,
            result.underlineRange
        )
    }

    // ---- substitutions / insertions / extra text ----

    @Test
    fun diff_wrongWord_underlinesOnlyTheWrongSpan() {
        val typed = target.replace("master", "servant")
        val result = TextDiff.diff(typed, target)

        val start = target.indexOf("master")
        assertEquals(start, result.firstMismatchIndex)
        assertEquals(start until start + "servant".length, result.underlineRange)
    }

    @Test
    fun diff_singleWrongCharacter_underlinesThatCharacter() {
        val typed = target.replace("refuse", "refuze")
        val result = TextDiff.diff(typed, target)

        val index = target.indexOf('s', target.indexOf("refuse"))
        assertEquals(index, result.firstMismatchIndex)
        assertEquals(index..index, result.underlineRange)
    }

    @Test
    fun diff_insertedWord_underlinesTheInsertedSpan() {
        val typed = target.replace("streak chain", "streak long chain")
        val result = TextDiff.diff(typed, target)

        val start = target.indexOf("chain")
        assertEquals(start, result.firstMismatchIndex)
        assertEquals(start until start + "long ".length, result.underlineRange)
    }

    @Test
    fun diff_extraTrailingText_underlinesTheExtraTail() {
        val typed = target + " please"
        val result = TextDiff.diff(typed, target)

        assertFalse(result.isExactMatch)
        assertEquals(target.length, result.firstMismatchIndex)
        assertEquals(target.length until typed.length, result.underlineRange)
    }

    @Test
    fun diff_completelyDifferentText_underlinesEverything() {
        val typed = "something else entirely"
        val result = TextDiff.diff(typed, target)

        assertEquals(0, result.firstMismatchIndex)
        assertEquals(0 until typed.length, result.underlineRange)
    }

    @Test
    fun diff_caseDifferenceOnly_isNotFlagged() {
        val typed = target.replace("I am", "i am")
        val result = TextDiff.diff(typed, target)
        assertTrue(result.isExactMatch)
        assertNull(result.underlineRange)
    }

    // ---- RTL / Persian text ----

    @Test
    fun diff_persianText_wrongCharacterIsUnderlined() {
        val faTarget = "من صاحب ذهن خودم هستم"
        val faTyped = "من صاحب ذهن خودم هستن"
        val result = TextDiff.diff(faTyped, faTarget)

        val index = faTarget.lastIndexOf('م')
        assertEquals(index, result.firstMismatchIndex)
        assertEquals(index..index, result.underlineRange)
    }

    @Test
    fun diff_persianText_correctPrefix_noUnderline() {
        val faTarget = "من صاحب ذهن خودم هستم"
        val result = TextDiff.diff("من صاحب ذهن", faTarget)
        assertEquals(-1, result.firstMismatchIndex)
        assertNull(result.underlineRange)
    }
}
