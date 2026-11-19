package com.example.util

/**
 * Bulk-insert guard for secure type-to-confirm fields (#5 paste block, #8
 * voice-typing block).
 *
 * The anti-cheat rule of those fields is "type it key by key". Normal typing
 * arrives as single-character insertions; paste, dictation commits, glide
 * typing and clipboard/autocomplete suggestions all arrive as a single edit
 * that inserts many characters at once. This filter rejects such bulk edits
 * while always letting single-key typing, deletions and ordinary IME
 * word-replacement edits through.
 *
 * Best-effort by design (see PLAN.md #5/#8): Compose/Android exposes no API
 * that truly disables paste or voice input for a field, so this guard — plus
 * hiding the field's text toolbar in [com.example.ui.components.SecureTypeField] —
 * is the enforcement layer. A determined IME can still inject text as if it
 * were keystrokes; nothing at the app level can prevent that.
 */
object SecureTextInputFilter {

    /**
     * Returns [newValue] when the edit from [oldValue] to [newValue] is
     * acceptable for a secure field, or [oldValue] when the edit looks like a
     * bulk insertion and must be rejected.
     */
    fun filter(oldValue: String, newValue: String): String =
        if (isAcceptableChange(oldValue, newValue)) newValue else oldValue

    fun isAcceptableChange(oldValue: String, newValue: String): Boolean {
        if (oldValue == newValue) return true

        val prefix = commonPrefixLength(oldValue, newValue)
        val suffix = commonSuffixLength(oldValue, newValue, prefix)
        val inserted = newValue.length - prefix - suffix
        val removed = oldValue.length - prefix - suffix

        // Single-character insertion (or a pure deletion / shrink): normal
        // typing, backspace, or replacing a selection with one keystroke.
        if (inserted <= 1) return true

        // Pure bulk insertion with nothing replaced: paste into the field,
        // a dictation commit, a glide-typed word, a clipboard suggestion.
        if (removed == 0) return false

        // Replacement edits (e.g. an IME swapping the word being composed):
        // accept only when the inserted text is not much larger than what it
        // replaced. A paste-over-selection swaps a short selection for a long
        // pasted text and is rejected here.
        return inserted <= removed + 1
    }

    private fun commonPrefixLength(a: String, b: String): Int {
        val limit = minOf(a.length, b.length)
        var i = 0
        while (i < limit && a[i] == b[i]) i++
        return i
    }

    private fun commonSuffixLength(a: String, b: String, prefix: Int): Int {
        val maxSuffix = minOf(a.length - prefix, b.length - prefix)
        var s = 0
        while (s < maxSuffix && a[a.length - 1 - s] == b[b.length - 1 - s]) s++
        return s
    }
}
