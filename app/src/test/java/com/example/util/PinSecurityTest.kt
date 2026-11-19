package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinSecurityTest {

    // ---- PIN hashing / verification ----

    @Test
    fun hashPin_samePinAndSalt_isDeterministic() {
        val a = PinSecurity.hashPin("1234", "salt-a")
        val b = PinSecurity.hashPin("1234", "salt-a")
        assertEquals(a, b)
        assertEquals(64, a.length) // SHA-256 hex
    }

    @Test
    fun hashPin_differentSalt_changesHash() {
        val a = PinSecurity.hashPin("1234", "salt-a")
        val b = PinSecurity.hashPin("1234", "salt-b")
        assertNotEquals(a, b)
    }

    @Test
    fun hashPin_differentPin_changesHash() {
        val a = PinSecurity.hashPin("1234", "salt-a")
        val b = PinSecurity.hashPin("1235", "salt-a")
        assertNotEquals(a, b)
    }

    @Test
    fun hashPin_neverContainsThePin() {
        val hash = PinSecurity.hashPin("975310", "pepper")
        assertFalse(hash.contains("975310"))
    }

    @Test
    fun hashesEqual_matchesOnlyIdenticalHashes() {
        val stored = PinSecurity.hashPin("2468", "salt")
        assertTrue(PinSecurity.hashesEqual(PinSecurity.hashPin("2468", "salt"), stored))
        assertFalse(PinSecurity.hashesEqual(PinSecurity.hashPin("2469", "salt"), stored))
        assertFalse(PinSecurity.hashesEqual("abcd", stored))
    }

    @Test
    fun generateSalt_produces32HexCharsAndVaries() {
        val a = PinSecurity.generateSalt()
        val b = PinSecurity.generateSalt()
        assertEquals(32, a.length) // 16 bytes hex-encoded
        assertTrue(a.all { it in '0'..'9' || it in 'a'..'f' })
        assertNotEquals(a, b)
    }

    // ---- PIN validity ----

    @Test
    fun isValidPin_accepts4To6Digits() {
        assertTrue(PinSecurity.isValidPin("1234"))
        assertTrue(PinSecurity.isValidPin("12345"))
        assertTrue(PinSecurity.isValidPin("123456"))
        assertTrue(PinSecurity.isValidPin("0000"))
    }

    @Test
    fun isValidPin_rejectsBadShapes() {
        assertFalse(PinSecurity.isValidPin(""))
        assertFalse(PinSecurity.isValidPin("123"))
        assertFalse(PinSecurity.isValidPin("1234567"))
        assertFalse(PinSecurity.isValidPin("12a4"))
        assertFalse(PinSecurity.isValidPin(" 1234"))
        assertFalse(PinSecurity.isValidPin("１２３４")) // full-width digits are not keypad digits
    }

    // ---- setup-required state ----

    @Test
    fun requiresPinSetup_onlyWhenHashMissingOrBlank() {
        assertTrue(PinSecurity.requiresPinSetup(null))
        assertTrue(PinSecurity.requiresPinSetup(""))
        assertTrue(PinSecurity.requiresPinSetup("   "))
        assertFalse(PinSecurity.requiresPinSetup(PinSecurity.hashPin("1234", "s")))
    }

    // ---- security-answer normalization ----

    @Test
    fun normalizeSecurityAnswer_trimsAndLowercases() {
        assertEquals("fluffy", PinSecurity.normalizeSecurityAnswer("  Fluffy  "))
        assertEquals("fluffy", PinSecurity.normalizeSecurityAnswer("FLUFFY"))
    }

    @Test
    fun normalizeSecurityAnswer_collapsesInternalWhitespace() {
        assertEquals(
            "mary ann smith",
            PinSecurity.normalizeSecurityAnswer("Mary   Ann\t Smith")
        )
    }

    @Test
    fun normalizeSecurityAnswer_keepsPersianTextIntact() {
        assertEquals("تهران", PinSecurity.normalizeSecurityAnswer("  تهران "))
    }

    @Test
    fun hashSecurityAnswer_typingVariantsShareOneHash() {
        val stored = PinSecurity.hashSecurityAnswer("Fluffy", "salt")
        assertEquals(stored, PinSecurity.hashSecurityAnswer("  fluffy ", "salt"))
        assertEquals(stored, PinSecurity.hashSecurityAnswer("FLUFFY", "salt"))
    }

    @Test
    fun hashSecurityAnswer_wrongAnswerOrSalt_differs() {
        val stored = PinSecurity.hashSecurityAnswer("Fluffy", "salt")
        assertNotEquals(stored, PinSecurity.hashSecurityAnswer("Rex", "salt"))
        assertNotEquals(stored, PinSecurity.hashSecurityAnswer("Fluffy", "other-salt"))
    }

    // ---- brute-force backoff ----

    @Test
    fun backoffMillis_noDelayBelowThreshold() {
        assertEquals(0L, PinSecurity.backoffMillis(0))
        assertEquals(0L, PinSecurity.backoffMillis(1))
        assertEquals(0L, PinSecurity.backoffMillis(4))
    }

    @Test
    fun backoffMillis_doublesFromThirtySeconds() {
        assertEquals(30_000L, PinSecurity.backoffMillis(5))
        assertEquals(60_000L, PinSecurity.backoffMillis(6))
        assertEquals(120_000L, PinSecurity.backoffMillis(7))
        assertEquals(240_000L, PinSecurity.backoffMillis(8))
    }

    @Test
    fun backoffMillis_cappedAtFiveMinutes() {
        assertEquals(300_000L, PinSecurity.backoffMillis(9))
        assertEquals(300_000L, PinSecurity.backoffMillis(50))
    }
}
