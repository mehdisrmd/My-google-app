package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureTextInputFilterTest {

    private val sentence =
        "I am the master of my mind and I refuse to trade my clean streak chain for a temporary urge"

    // ---- normal typing is always accepted ----

    @Test
    fun filter_singleCharacterTyped_accepted() {
        assertEquals("I", SecureTextInputFilter.filter("", "I"))
        assertEquals("I a", SecureTextInputFilter.filter("I ", "I a"))
    }

    @Test
    fun filter_singleCharacterAtCursor_accepted() {
        // Inserting one character in the middle of existing text.
        assertEquals("abcd", SecureTextInputFilter.filter("acd", "abcd"))
    }

    @Test
    fun filter_deletion_accepted() {
        assertEquals("I a", SecureTextInputFilter.filter("I am", "I a"))
        assertEquals("", SecureTextInputFilter.filter("I am", ""))
    }

    @Test
    fun filter_noChange_accepted() {
        assertTrue(SecureTextInputFilter.isAcceptableChange("abc", "abc"))
        assertEquals("abc", SecureTextInputFilter.filter("abc", "abc"))
    }

    @Test
    fun filter_selectAllAndRetypeOneCharacter_accepted() {
        // Replacing a long selection with a single keystroke is typing, not paste.
        assertEquals("x", SecureTextInputFilter.filter(sentence, "x"))
    }

    @Test
    fun filter_wordReplacementOfSimilarLength_accepted() {
        // IME word swaps replace a span with a comparable span.
        assertTrue(SecureTextInputFilter.isAcceptableChange("I choose", "I chosee"))
        assertTrue(SecureTextInputFilter.isAcceptableChange("abc def", "abc xyz"))
    }

    // ---- bulk insertions are rejected (paste / dictation / glide) ----

    @Test
    fun filter_pasteWholeSentenceIntoEmptyField_rejected() {
        assertFalse(SecureTextInputFilter.isAcceptableChange("", sentence))
        assertEquals("", SecureTextInputFilter.filter("", sentence))
    }

    @Test
    fun filter_pasteAppendedToTypedPrefix_rejected() {
        val typedPrefix = "I am the master"
        assertEquals(
            typedPrefix,
            SecureTextInputFilter.filter(typedPrefix, sentence)
        )
    }

    @Test
    fun filter_dictationStyleMultiWordCommit_rejected() {
        // A voice-typing commit lands as one multi-character insertion.
        assertEquals(
            "I am",
            SecureTextInputFilter.filter("I am", "I am the master of my mind")
        )
    }

    @Test
    fun filter_glideTypedWord_rejected() {
        assertEquals(
            "I am the ",
            SecureTextInputFilter.filter("I am the ", "I am the master")
        )
    }

    @Test
    fun filter_pasteOverShortSelection_rejected() {
        // Selecting a few characters and pasting a long text over them.
        assertEquals(
            "abc",
            SecureTextInputFilter.filter("abc", sentence)
        )
    }

    @Test
    fun filter_twoCharactersAtOnce_rejected() {
        // Even a two-character pure insertion is not a single keystroke.
        assertFalse(SecureTextInputFilter.isAcceptableChange("ab", "abcd"))
        assertEquals("ab", SecureTextInputFilter.filter("ab", "abcd"))
    }
}
