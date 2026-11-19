package com.example.data.blocklist

/**
 * Lookup structure for the merged adult domain list (#13).
 *
 * The merged list can hold several hundred thousand domains, so it is NOT
 * kept as an in-memory HashSet of strings (100+ MB). Instead the sorted
 * file bytes are held as one [ByteArray] plus an [IntArray] of line-start
 * offsets, and membership is a binary search over the lines — a few MB of
 * RAM and ~19 comparisons per lookup, which the per-event scanner affords.
 *
 * The input MUST be sorted ascending (the output of
 * [AdultBlocklistParser.mergeDomainSets] / [AdultBlocklistParser
 * .serializeDomains]); lookups on unsorted data are meaningless.
 */
class SortedDomainIndex private constructor(
    private val bytes: ByteArray,
    private val lineStarts: IntArray
) {

    /** Number of domains in the index. */
    val size: Int
        get() = lineStarts.size

    /** True when [domain] (already normalized, lowercase) is in the list. */
    fun contains(domain: String): Boolean {
        if (domain.isEmpty() || lineStarts.isEmpty()) return false
        var lo = 0
        var hi = lineStarts.size - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val cmp = compareLine(mid, domain)
            when {
                cmp == 0 -> return true
                cmp < 0 -> lo = mid + 1
                else -> hi = mid - 1
            }
        }
        return false
    }

    /**
     * Compares the line at [lineIndex] with [domain] byte by byte. Domains
     * are lowercase ASCII after normalization, so a raw byte comparison is a
     * correct lexicographic ordering.
     */
    private fun compareLine(lineIndex: Int, domain: String): Int {
        val start = lineStarts[lineIndex]
        var end = start
        while (end < bytes.size && bytes[end] != NEWLINE) end++
        val lineLength = end - start
        val shared = minOf(lineLength, domain.length)
        for (i in 0 until shared) {
            val a = bytes[start + i].toInt() and 0xFF
            val b = domain[i].code
            if (a != b) return a - b
        }
        return lineLength - domain.length
    }

    companion object {
        private val NEWLINE = '\n'.code.toByte()

        /**
         * Builds an index over serialized list bytes (one domain per line).
         * A trailing newline does not create a phantom empty entry.
         */
        fun fromBytes(bytes: ByteArray): SortedDomainIndex {
            if (bytes.isEmpty()) return SortedDomainIndex(bytes, IntArray(0))
            val starts = ArrayList<Int>()
            starts.add(0)
            for (i in bytes.indices) {
                if (bytes[i] == NEWLINE && i + 1 < bytes.size) {
                    starts.add(i + 1)
                }
            }
            return SortedDomainIndex(bytes, starts.toIntArray())
        }

        /** Builds an index directly from an already-sorted domain list. */
        fun fromSortedDomains(sortedDomains: List<String>): SortedDomainIndex {
            return fromBytes(
                AdultBlocklistParser.serializeDomains(sortedDomains)
                    .toByteArray(Charsets.UTF_8)
            )
        }
    }
}
