package com.example.data.blocklist

/**
 * Matches what the accessibility service sees on screen against the adult
 * domain index (#12).
 *
 * Two steps, both pure and unit-tested:
 *  1. [extractHostCandidates] pulls domain-shaped tokens out of the screen
 *     texts (the browser URL bar, link targets, page text).
 *  2. [matchesHost] checks a candidate host AND every parent suffix of it,
 *     so `www.pornhub.com` and `m.pornhub.com` match a `pornhub.com` entry
 *     while `notpornhub.com` and `pornhub.com.evil.com` never do.
 */
object AdultDomainMatcher {

    private val DOMAIN_TOKEN_REGEX =
        Regex("""(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z]{2,}""")

    /** All domain-shaped tokens found in [texts], lowercased, deduplicated. */
    fun extractHostCandidates(texts: List<String>): Set<String> {
        val result = LinkedHashSet<String>()
        for (text in texts) {
            if (text.isBlank()) continue
            val lower = text.lowercase()
            for (match in DOMAIN_TOKEN_REGEX.findAll(lower)) {
                result.add(match.value.trimEnd('.'))
            }
        }
        return result
    }

    /**
     * True when [host] or any of its parent domains is in [index]
     * (e.g. host `a.b.example.com` also checks `b.example.com`,
     * `example.com`).
     */
    fun matchesHost(index: SortedDomainIndex, host: String): Boolean {
        var candidate = host.trim().lowercase().trimEnd('.')
        if (candidate.isEmpty()) return false
        while (true) {
            if (index.contains(candidate)) return true
            val dot = candidate.indexOf('.')
            if (dot < 0) return false
            candidate = candidate.substring(dot + 1)
            if (candidate.isEmpty()) return false
        }
    }

    /**
     * Returns the first screen host that the blocklist covers, or null when
     * nothing on screen is an adult domain.
     */
    fun findBlockedHost(texts: List<String>, index: SortedDomainIndex): String? {
        if (index.size == 0) return null
        for (host in extractHostCandidates(texts)) {
            if (matchesHost(index, host)) return host
        }
        return null
    }
}
