package com.example.data.blocklist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parser / merge coverage for the multi-repo adult blocklist (#13):
 * hosts format, plain lists, wildcard/adblock decorations, dedupe.
 */
class AdultBlocklistParserTest {

    // ---- hosts format (StevenBlack) ----

    @Test
    fun hostsFormatExtractsTheDomainColumn() {
        val content = """
            0.0.0.0 pornhub.com
            0.0.0.0 xvideos.com
            127.0.0.1 xnxx.com
        """.trimIndent()
        assertEquals(
            setOf("pornhub.com", "xvideos.com", "xnxx.com"),
            AdultBlocklistParser.parseDomains(content)
        )
    }

    @Test
    fun hostsHeadersCommentsAndInfraEntriesAreSkipped() {
        val content = """
            # Title: StevenBlack/hosts — porn
            # Number of unique domains: 76,800
            127.0.0.1 localhost
            255.255.255.255 broadcasthost
            ::1 localhost
            0.0.0.0 0.0.0.0
            0.0.0.0 real-site.com
        """.trimIndent()
        assertEquals(
            setOf("real-site.com"),
            AdultBlocklistParser.parseDomains(content)
        )
    }

    // ---- plain format (OISD / Block List Project) ----

    @Test
    fun plainFormatTakesOneDomainPerLine() {
        val content = "example-adult.com\nanother-one.net\n"
        assertEquals(
            setOf("example-adult.com", "another-one.net"),
            AdultBlocklistParser.parseDomains(content)
        )
    }

    @Test
    fun plainFormatSkipsCommentsBlanksAndTrailingComments() {
        val content = """
            # a comment

            kept.com # trailing comment
            also-kept.org
        """.trimIndent()
        assertEquals(
            setOf("kept.com", "also-kept.org"),
            AdultBlocklistParser.parseDomains(content)
        )
    }

    // ---- wildcard / adblock decorations ----

    @Test
    fun wildcardAndAdblockMarkersAreStripped() {
        val content = """
            *.wild-example.com
            ||adblock-example.com^
            ||options-example.com${'$'}third-party
        """.trimIndent()
        assertEquals(
            setOf("wild-example.com", "adblock-example.com", "options-example.com"),
            AdultBlocklistParser.parseDomains(content)
        )
    }

    // ---- normalization ----

    @Test
    fun normalizeLowercasesAndStripsUrlParts() {
        assertEquals("pornhub.com", AdultBlocklistParser.normalizeDomain("PornHub.COM"))
        assertEquals("x.com", AdultBlocklistParser.normalizeDomain("https://x.com/some/path?q=1"))
        assertEquals("x.com", AdultBlocklistParser.normalizeDomain("x.com:8080"))
        assertEquals("x.com", AdultBlocklistParser.normalizeDomain("x.com."))
    }

    @Test
    fun normalizeRejectsNonDomains() {
        assertNull(AdultBlocklistParser.normalizeDomain(""))
        assertNull(AdultBlocklistParser.normalizeDomain("localhost"))
        assertNull(AdultBlocklistParser.normalizeDomain("justaword"))
        assertNull(AdultBlocklistParser.normalizeDomain("foo.123"))
        assertNull(AdultBlocklistParser.normalizeDomain("1.2.3.4"))
        assertNull(AdultBlocklistParser.normalizeDomain("bad_domain!.com"))
        assertNull(AdultBlocklistParser.normalizeDomain("double..dot.com"))
        assertNull(AdultBlocklistParser.normalizeDomain("-leading-hyphen.com"))
        assertNull(AdultBlocklistParser.normalizeDomain("trailing-.com"))
        assertNull(AdultBlocklistParser.normalizeDomain("has space.com"))
    }

    @Test
    fun duplicatesWithinOneSourceCollapse() {
        val content = "dup.com\ndup.com\n0.0.0.0 dup.com\n"
        assertEquals(setOf("dup.com"), AdultBlocklistParser.parseDomains(content))
    }

    // ---- merge + serialize ----

    @Test
    fun mergeDedupesAcrossSourcesAndSorts() {
        val merged = AdultBlocklistParser.mergeDomainSets(
            listOf(
                setOf("b.com", "a.com", "shared.com"),
                setOf("shared.com", "c.com"),
                emptySet()
            )
        )
        assertEquals(listOf("a.com", "b.com", "c.com", "shared.com"), merged)
    }

    @Test
    fun serializeJoinsWithTrailingNewline() {
        assertEquals(
            "a.com\nb.com\n",
            AdultBlocklistParser.serializeDomains(listOf("a.com", "b.com"))
        )
        assertEquals("", AdultBlocklistParser.serializeDomains(emptyList()))
    }

    @Test
    fun fallbackSeedIsCleanAndSortedAfterMerge() {
        // The built-in seed must survive the same normalization pipeline:
        // every entry is a valid domain and the merged form is sorted.
        val merged = AdultBlocklistParser.mergeDomainSets(
            listOf(BlocklistSources.FALLBACK_SEED_DOMAINS)
        )
        assertEquals(BlocklistSources.FALLBACK_SEED_DOMAINS.size, merged.size)
        assertEquals(merged.sorted(), merged)
        assertTrue(merged.contains("pornhub.com"))
    }
}
