package com.example.data.blocklist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Host extraction + suffix matching for the adult blocker (#12): subdomains
 * of a listed domain match, lookalike domains never do.
 */
class AdultDomainMatcherTest {

    private val index = SortedDomainIndex.fromSortedDomains(
        listOf("example.com", "pornhub.com", "xvideos.com")
    )

    // ---- matchesHost ----

    @Test
    fun exactHostMatches() {
        assertTrue(AdultDomainMatcher.matchesHost(index, "pornhub.com"))
    }

    @Test
    fun subdomainsMatchTheirListedParent() {
        assertTrue(AdultDomainMatcher.matchesHost(index, "www.pornhub.com"))
        assertTrue(AdultDomainMatcher.matchesHost(index, "m.xvideos.com"))
        assertTrue(AdultDomainMatcher.matchesHost(index, "deep.sub.pornhub.com"))
    }

    @Test
    fun lookalikeDomainsDoNotMatch() {
        assertFalse(AdultDomainMatcher.matchesHost(index, "notpornhub.com"))
        assertFalse(AdultDomainMatcher.matchesHost(index, "fake-pornhub.com"))
        assertFalse(AdultDomainMatcher.matchesHost(index, "pornhub.com.evil.com"))
        assertFalse(AdultDomainMatcher.matchesHost(index, "pornhub.co"))
    }

    @Test
    fun matchingIsCaseInsensitiveAndToleratesTrailingDot() {
        assertTrue(AdultDomainMatcher.matchesHost(index, "WWW.PornHub.COM"))
        assertTrue(AdultDomainMatcher.matchesHost(index, "pornhub.com."))
    }

    @Test
    fun unlistedHostsDoNotMatch() {
        assertFalse(AdultDomainMatcher.matchesHost(index, "google.com"))
        assertFalse(AdultDomainMatcher.matchesHost(index, ""))
    }

    // ---- extractHostCandidates ----

    @Test
    fun extractsHostsFromUrlsAndBareText() {
        val candidates = AdultDomainMatcher.extractHostCandidates(
            listOf(
                "https://www.pornhub.com/video/123",
                "Address: xvideos.com",
                "just some words without hosts"
            )
        )
        assertTrue(candidates.contains("www.pornhub.com"))
        assertTrue(candidates.contains("xvideos.com"))
    }

    @Test
    fun extractionIgnoresPlainWordsAndNumbers() {
        val candidates = AdultDomainMatcher.extractHostCandidates(
            listOf("Version 2.5 released at 10.30 today, see the notes")
        )
        assertTrue(candidates.isEmpty())
    }

    // ---- findBlockedHost ----

    @Test
    fun findBlockedHostReturnsTheMatchedScreenHost() {
        val blocked = AdultDomainMatcher.findBlockedHost(
            listOf("Welcome", "https://m.pornhub.com/home", "Sign in"),
            index
        )
        assertEquals("m.pornhub.com", blocked)
    }

    @Test
    fun findBlockedHostReturnsNullForCleanScreens() {
        assertNull(
            AdultDomainMatcher.findBlockedHost(
                listOf("https://www.google.com", "Kotlin tutorials"),
                index
            )
        )
        assertNull(
            AdultDomainMatcher.findBlockedHost(
                listOf("pornhub.com mentioned in a chat about notpornhub.com fans"),
                SortedDomainIndex.fromSortedDomains(listOf("other.com"))
            )
        )
    }

    @Test
    fun findBlockedHostWithEmptyIndexNeverMatches() {
        val empty = SortedDomainIndex.fromBytes(ByteArray(0))
        assertNull(
            AdultDomainMatcher.findBlockedHost(listOf("pornhub.com"), empty)
        )
    }
}
