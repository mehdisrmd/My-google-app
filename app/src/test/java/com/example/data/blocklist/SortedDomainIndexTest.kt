package com.example.data.blocklist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Binary-search membership over the serialized sorted list (#13).
 */
class SortedDomainIndexTest {

    private val domains = listOf(
        "a-site.com",
        "b-site.com",
        "middle.org",
        "pornhub.com",
        "xvideos.com",
        "zzz.net"
    )

    @Test
    fun containsFindsEveryEntry() {
        val index = SortedDomainIndex.fromSortedDomains(domains)
        assertEquals(domains.size, index.size)
        for (domain in domains) {
            assertTrue("expected $domain to be found", index.contains(domain))
        }
    }

    @Test
    fun containsRejectsNonEntries() {
        val index = SortedDomainIndex.fromSortedDomains(domains)
        assertFalse(index.contains("not-there.com"))
        assertFalse(index.contains("pornhub.co"))
        assertFalse(index.contains("pornhub.comm"))
        assertFalse(index.contains("a-site.com.evil.com"))
        assertFalse(index.contains(""))
    }

    @Test
    fun containsWorksAtTheBoundaries() {
        val index = SortedDomainIndex.fromSortedDomains(domains)
        assertTrue(index.contains("a-site.com")) // first line
        assertTrue(index.contains("zzz.net")) // last line
        assertFalse(index.contains("0-before-first.com"))
        assertFalse(index.contains("zzzz-after-last.net"))
    }

    @Test
    fun singleEntryIndex() {
        val index = SortedDomainIndex.fromSortedDomains(listOf("only.com"))
        assertEquals(1, index.size)
        assertTrue(index.contains("only.com"))
        assertFalse(index.contains("other.com"))
    }

    @Test
    fun emptyIndexContainsNothing() {
        val index = SortedDomainIndex.fromBytes(ByteArray(0))
        assertEquals(0, index.size)
        assertFalse(index.contains("anything.com"))
    }

    @Test
    fun fromBytesWithoutTrailingNewlineStillFindsTheLastLine() {
        val bytes = "a.com\nb.com".toByteArray(Charsets.UTF_8)
        val index = SortedDomainIndex.fromBytes(bytes)
        assertEquals(2, index.size)
        assertTrue(index.contains("a.com"))
        assertTrue(index.contains("b.com"))
    }

    @Test
    fun largerGeneratedIndexStaysCorrect() {
        // 5,000 generated domains exercise deeper binary-search paths.
        val generated = (0 until 5_000).map { "site-$it.example.com" }.sorted()
        val index = SortedDomainIndex.fromSortedDomains(generated)
        assertEquals(5_000, index.size)
        assertTrue(index.contains("site-0.example.com"))
        assertTrue(index.contains("site-4999.example.com"))
        assertTrue(index.contains("site-2500.example.com"))
        assertFalse(index.contains("site-5000.example.com"))
        assertFalse(index.contains("site-2500.example.org"))
    }
}
