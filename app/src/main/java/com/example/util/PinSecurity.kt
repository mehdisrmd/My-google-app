package com.example.util

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Pure cryptography / policy helpers for the app-entry PIN gate (#7).
 *
 * The PIN itself is never stored anywhere: only a per-install salted
 * SHA-256 hash is kept (see ShieldPreferences). The security-question
 * answer gets the same treatment, hashed over its NORMALIZED form so
 * " Fluffy ", "fluffy" and "FLUFFY" all match the same answer.
 *
 * Everything here is Android-free so it can be unit-tested directly.
 */
object PinSecurity {

    const val MIN_PIN_LENGTH = 4
    const val MAX_PIN_LENGTH = 6

    private const val SALT_BYTES = 16

    /** Consecutive wrong PINs tolerated before a timed lockout starts. */
    private const val FAILURES_BEFORE_BACKOFF = 5
    private const val BASE_BACKOFF_MS = 30_000L
    private const val MAX_BACKOFF_MS = 300_000L

    /** Random per-credential salt, hex-encoded. */
    fun generateSalt(random: SecureRandom = SecureRandom()): String {
        val bytes = ByteArray(SALT_BYTES)
        random.nextBytes(bytes)
        return bytes.toHex()
    }

    /** A gate PIN is 4–6 ASCII digits, matching the on-screen keypad. */
    fun isValidPin(pin: String): Boolean =
        pin.length in MIN_PIN_LENGTH..MAX_PIN_LENGTH && pin.all { it in '0'..'9' }

    /** Salted SHA-256 of the PIN, hex-encoded. */
    fun hashPin(pin: String, saltHex: String): String =
        sha256Hex("$saltHex:$pin")

    /**
     * Canonical form of a security-question answer: trimmed, lowercased
     * (invariant locale rules) and with internal whitespace runs collapsed
     * to a single space, so trivial typing differences don't lock the user
     * out of their own recovery.
     */
    fun normalizeSecurityAnswer(answer: String): String =
        answer.trim().lowercase().replace(Regex("\\s+"), " ")

    /** Salted SHA-256 of the NORMALIZED answer, hex-encoded. */
    fun hashSecurityAnswer(answer: String, saltHex: String): String =
        sha256Hex("$saltHex:${normalizeSecurityAnswer(answer)}")

    /** Constant-time comparison of two hash strings. */
    fun hashesEqual(a: String, b: String): Boolean =
        MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))

    /**
     * Setup-required state: a missing/blank stored hash means NO PIN was
     * ever set. There is deliberately no default PIN — an unset PIN must
     * route the user into the setup flow, never silently accept a code.
     */
    fun requiresPinSetup(storedPinHash: String?): Boolean =
        storedPinHash.isNullOrBlank()

    /**
     * Brute-force backoff for the PIN pad: no delay for the first few
     * mistakes, then a doubling lockout (30 s, 60 s, 120 s, …) capped at
     * 5 minutes.
     */
    fun backoffMillis(consecutiveFailures: Int): Long {
        if (consecutiveFailures < FAILURES_BEFORE_BACKOFF) return 0L
        val steps = (consecutiveFailures - FAILURES_BEFORE_BACKOFF).coerceAtMost(4)
        return (BASE_BACKOFF_MS shl steps).coerceAtMost(MAX_BACKOFF_MS)
    }

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .toHex()

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it.toInt() and 0xFF) }
}
