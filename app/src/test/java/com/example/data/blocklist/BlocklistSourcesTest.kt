package com.example.data.blocklist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #38 (batch 2): the adult list has exactly ONE source — OISD NSFW —
 * and the user's own domains merge into it on update.
 */
class BlocklistSourcesTest {

    @Test
    fun `there is exactly one source and it is OISD NSFW`() {
        assertEquals(1, BlocklistSources.SOURCES.size)
        assertEquals("OISD NSFW", BlocklistSources.SOURCE.name)
        assertEquals(BlocklistSources.SOURCE, BlocklistSources.SOURCES.single())
    }

    @Test
    fun `the source urls point at the OISD list and its own mirror`() {
        val urls = BlocklistSources.SOURCE.urls
        assertTrue(urls.isNotEmpty())
        assertTrue(urls.first().startsWith("https://nsfw.oisd.nl/"))
        assertTrue(urls.any { it.contains("githubusercontent.com/sjhgvr/oisd") })
    }

    @Test
    fun `the repository address shown in the app is the OISD repo`() {
        assertEquals("https://github.com/sjhgvr/oisd", BlocklistSources.SOURCE_REPOSITORY_URL)
        assertEquals("https://oisd.nl", BlocklistSources.SOURCE_HOME_URL)
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
    fun `the OISD wildcard format parses to plain domains`() {
        val content = """
            # Title: OISD NSFW
            *.ads.example.com
            plain.example.org
            ||abp.example.net^
        """.trimIndent()
        val parsed = AdultBlocklistParser.parseDomains(content)
        assertTrue(parsed.contains("ads.example.com"))
        assertTrue(parsed.contains("plain.example.org"))
        assertTrue(parsed.contains("abp.example.net"))
    }
}
