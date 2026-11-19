package com.example.data.blocklist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #38 (batch 2, corrected after measuring the actual lists): the adult
 * list merges exactly TWO famous porn-only sources — StevenBlack's porn
 * alternate and HaGeZi NSFW. The Block List Project and OISD full lists
 * were dropped: measured, they carry tens of thousands of *.blogspot.com
 * / *.tumblr.com entries and block whole blogging platforms.
 */
class BlocklistSourcesTest {

    @Test
    fun `there are exactly two sources - StevenBlack porn and HaGeZi NSFW`() {
        assertEquals(2, BlocklistSources.SOURCES.size)
        assertEquals(
            listOf("StevenBlack hosts (porn)", "HaGeZi NSFW"),
            BlocklistSources.SOURCES.map { it.name }
        )
    }

    @Test
    fun `no source is the Block List Project or OISD`() {
        val allUrls = BlocklistSources.SOURCES.flatMap { it.urls } +
            BlocklistSources.SOURCES.map { it.repositoryUrl }
        assertTrue(allUrls.none { it.contains("blocklistproject", ignoreCase = true) })
        assertTrue(allUrls.none { it.contains("oisd", ignoreCase = true) })
    }

    @Test
    fun `the source urls point at the porn-only lists and their mirrors`() {
        val stevenBlack = BlocklistSources.SOURCES[0]
        assertTrue(stevenBlack.urls.first().contains("StevenBlack/hosts"))
        assertTrue(stevenBlack.urls.first().endsWith("alternates/porn/hosts"))
        val haGeZi = BlocklistSources.SOURCES[1]
        assertTrue(haGeZi.urls.first().contains("hagezi/dns-blocklists"))
        assertTrue(haGeZi.urls.first().endsWith("nsfw-onlydomains.txt"))
        // Every source carries a CDN mirror of the same repository.
        assertTrue(BlocklistSources.SOURCES.all { src -> src.urls.any { it.contains("cdn.jsdelivr.net") } })
    }

    @Test
    fun `the repository addresses shown in the app are the two repos`() {
        assertEquals(
            "https://github.com/StevenBlack/hosts",
            BlocklistSources.SOURCES[0].repositoryUrl
        )
        assertEquals(
            "https://github.com/hagezi/dns-blocklists",
            BlocklistSources.SOURCES[1].repositoryUrl
        )
    }

    @Test
    fun `user domains merge into the downloaded set`() {
        val downloaded = setOf("blocked.example", "other.example")
        val user = setOf("mine.example", "blocked.example")
        val merged = AdultBlocklistParser.mergeDomainSets(listOf(downloaded, user))
        assertEquals(
            listOf("blocked.example", "mine.example", "other.example"),
            merged
        )
    }

    @Test
    fun `hosts and wildcard formats parse to plain domains`() {
        val content = """
            # Title: StevenBlack hosts (porn)
            0.0.0.0 blocked.example.com
            *.ads.example.com
            plain.example.org
            ||abp.example.net^
        """.trimIndent()
        val parsed = AdultBlocklistParser.parseDomains(content)
        assertTrue(parsed.contains("blocked.example.com"))
        assertTrue(parsed.contains("ads.example.com"))
        assertTrue(parsed.contains("plain.example.org"))
        assertTrue(parsed.contains("abp.example.net"))
    }

    @Test
    fun `an inline comment URL is never injected as a blocked domain`() {
        // The exact v0.5 bug: this StevenBlack-style line put github.com
        // itself into the blocklist ("Adult Site Blocked: github.com").
        val content = """
            0.0.0.0 docs.pipenv.org # https://github.com/StevenBlack/hosts/issues/1635
        """.trimIndent()
        val parsed = AdultBlocklistParser.parseDomains(content)
        assertTrue(parsed.contains("docs.pipenv.org"))
        assertFalse(parsed.contains("github.com"))
    }

    @Test
    fun `never-block apexes are dropped even as plain entries`() {
        val content = """
            github.com
            en.wikipedia.org
            blocked.example.com
        """.trimIndent()
        val parsed = AdultBlocklistParser.parseDomains(content)
        assertFalse(parsed.contains("github.com"))
        assertFalse(parsed.contains("en.wikipedia.org"))
        assertTrue(parsed.contains("blocked.example.com"))
    }

    // ---- #49 (batch 3): the persisted, user-managed source list ----

    @Test
    fun `unseeded storage resolves to the built-in defaults`() {
        assertEquals(null, BlocklistSources.decodeSources(null))
        assertEquals(BlocklistSources.SOURCES, BlocklistSources.resolveSources(null))
    }

    @Test
    fun `a stored empty list is a real state - every repository removed`() {
        assertEquals("", BlocklistSources.encodeSources(emptyList()))
        assertEquals(emptyList<BlocklistSource>(), BlocklistSources.decodeSources(""))
        assertEquals(emptyList<BlocklistSource>(), BlocklistSources.resolveSources(""))
    }

    @Test
    fun `the defaults survive an encode decode round trip`() {
        val encoded = BlocklistSources.encodeSources(BlocklistSources.SOURCES)
        assertEquals(BlocklistSources.SOURCES, BlocklistSources.decodeSources(encoded))
    }

    @Test
    fun `removing one default persists - only the other remains`() {
        val remaining = BlocklistSources.SOURCES - BlocklistSources.SOURCES[0]
        val stored = BlocklistSources.encodeSources(remaining)
        assertEquals(remaining, BlocklistSources.resolveSources(stored))
        assertEquals(listOf("HaGeZi NSFW"), BlocklistSources.resolveSources(stored).map { it.name })
    }

    @Test
    fun `a user added source round trips alongside the defaults`() {
        val custom = BlocklistSources.sourceFromUserInput("My List", "https://lists.example.org/adult.txt")
        assertTrue(custom != null)
        val combined = BlocklistSources.SOURCES + custom!!
        assertEquals(combined, BlocklistSources.decodeSources(BlocklistSources.encodeSources(combined)))
    }

    @Test
    fun `malformed stored records are skipped, valid ones survive`() {
        val custom = BlocklistSources.sourceFromUserInput("", "https://ok.example.net/list.txt")!!
        // \u001E is the codec's record separator (see BlocklistSources).
        val stored = "garbage-without-fields\u001E" + BlocklistSources.encodeSources(listOf(custom))
        assertEquals(listOf(custom), BlocklistSources.decodeSources(stored))
    }

    @Test
    fun `sourceFromUserInput falls back to the host when the name is blank`() {
        val source = BlocklistSources.sourceFromUserInput("  ", "https://lists.example.org/adult.txt")
        assertTrue(source != null)
        assertEquals("lists.example.org", source!!.name)
        assertEquals("https://lists.example.org/adult.txt", source.repositoryUrl)
        assertEquals(listOf("https://lists.example.org/adult.txt"), source.urls)
    }

    @Test
    fun `sourceFromUserInput rejects addresses that are not full http urls`() {
        assertEquals(null, BlocklistSources.sourceFromUserInput("x", "example.com/list.txt"))
        assertEquals(null, BlocklistSources.sourceFromUserInput("x", "ftp://example.com/list.txt"))
        assertEquals(null, BlocklistSources.sourceFromUserInput("x", ""))
        assertEquals(null, BlocklistSources.sourceFromUserInput("x", "not a url"))
    }
}
