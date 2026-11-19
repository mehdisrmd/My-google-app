package com.example.data.blocklist

import java.util.TreeSet

/**
 * Pure parsing / merging logic for the adult domain blocklist (#13).
 *
 * The upstream repositories publish three shapes of file:
 *  - hosts format:        `0.0.0.0 pornhub.com` (StevenBlack)
 *  - plain domain lists:  one domain per line (OISD, Block List Project)
 *  - wildcard / adblock:  `*.example.com`, `||example.com^`
 *
 * One tolerant parser covers all of them: every whitespace-separated token
 * on a non-comment line is normalized, and only tokens that survive
 * [normalizeDomain] as real domains are kept. That makes the parser immune
 * to header comments, IP columns, trailing comments and format drift.
 *
 * Everything here is Android-free so it is fully unit-testable.
 */
object AdultBlocklistParser {

    private val IPV4_REGEX = Regex("""\d{1,3}(?:\.\d{1,3}){3}""")
    private val LABEL_REGEX = Regex("""[a-z0-9](?:[a-z0-9-]*[a-z0-9])?""")
    private val WHITESPACE_REGEX = Regex("""\s+""")

    /** Parses one downloaded list (any supported format) into a domain set. */
    fun parseDomains(content: String): Set<String> {
        val result = LinkedHashSet<String>()
        for (rawLine in content.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            // Comment / header / adblock section lines.
            if (line.startsWith("#") || line.startsWith("!") || line.startsWith("[")) continue
            for (token in line.split(WHITESPACE_REGEX)) {
                if (token.isEmpty()) continue
                if (IPV4_REGEX.matches(token)) continue
                val domain = normalizeDomain(token) ?: continue
                result.add(domain)
            }
        }
        return result
    }

    /**
     * Normalizes one raw token into a canonical domain (lowercase, no
     * scheme / path / port / wildcard markers), or returns null when the
     * token is not a plausible public domain name.
     */
    fun normalizeDomain(raw: String): String? {
        var candidate = raw.trim().lowercase()
        if (candidate.isEmpty()) return null

        // A full URL slipped into the list: keep only its host part.
        val schemeEnd = candidate.indexOf("://")
        if (schemeEnd >= 0) candidate = candidate.substring(schemeEnd + 3)

        // Wildcard / adblock decorations.
        if (candidate.startsWith("||")) candidate = candidate.substring(2)
        if (candidate.startsWith("*.")) candidate = candidate.substring(2)

        // Cut path, port, query, fragment and adblock option suffixes.
        val cut = candidate.indexOfAny(charArrayOf('/', ':', '?', '#', '^', '$', '|'))
        if (cut >= 0) candidate = candidate.substring(0, cut)

        candidate = candidate.trim().trimEnd('.')
        if (candidate.isEmpty() || candidate.length > 253) return null
        if (!candidate.contains('.')) return null
        if (candidate.startsWith(".") || candidate.contains("..")) return null

        val labels = candidate.split('.')
        for (label in labels) {
            if (label.isEmpty() || label.length > 63) return null
            if (!LABEL_REGEX.matches(label)) return null
        }
        // A purely numeric final label means this was an IP address, not a
        // domain (the dotted-quad column of a hosts file, version strings…).
        val tld = labels.last()
        if (tld.length < 2 || tld.all { it.isDigit() }) return null

        return candidate
    }

    /**
     * Merges any number of parsed source sets into ONE sorted, deduplicated
     * list — the canonical on-disk form consumed by [SortedDomainIndex].
     */
    fun mergeDomainSets(sets: Collection<Collection<String>>): List<String> {
        val merged = TreeSet<String>()
        for (set in sets) {
            merged.addAll(set)
        }
        return merged.toList()
    }

    /** Serializes the sorted list: one domain per line, trailing newline. */
    fun serializeDomains(sortedDomains: List<String>): String {
        if (sortedDomains.isEmpty()) return ""
        return sortedDomains.joinToString(separator = "\n", postfix = "\n")
    }
}
