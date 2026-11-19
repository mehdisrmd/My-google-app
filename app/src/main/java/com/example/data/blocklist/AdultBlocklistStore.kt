package com.example.data.blocklist

import android.content.Context
import java.io.File

/**
 * On-disk home of the merged adult domain list (#13).
 *
 * The canonical file is `filesDir/blocklist/adult_domains.txt` — app-private
 * storage, one sorted domain per line (the [AdultBlocklistParser] output
 * form). Updates replace it ATOMICALLY (write to a temp file in the same
 * directory, then rename), so a crashed or interrupted update can never
 * leave a half-written list behind.
 *
 * [currentIndex] caches the parsed [SortedDomainIndex] and reloads it only
 * when the file's (mtime, length) stamp changes, which keeps the
 * accessibility service's per-event lookups allocation-free.
 */
class AdultBlocklistStore(context: Context) {

    private val directory = File(context.filesDir, BLOCKLIST_DIR_NAME)
    private val listFile = File(directory, LIST_FILE_NAME)
    private val tempFile = File(directory, TEMP_FILE_NAME)

    @Volatile
    private var cachedIndex: SortedDomainIndex? = null

    @Volatile
    private var cachedStamp: Long = Long.MIN_VALUE

    /** True when a usable list file (real download or fallback seed) exists. */
    fun hasList(): Boolean {
        return try {
            listFile.isFile && listFile.length() > 0L
        } catch (_: Exception) {
            false
        }
    }

    /** The current lookup index, or null when no list has been stored yet. */
    fun currentIndex(): SortedDomainIndex? {
        if (!hasList()) {
            cachedIndex = null
            cachedStamp = Long.MIN_VALUE
            return null
        }
        val stamp = stampOf()
        val cached = cachedIndex
        if (cached != null && stamp == cachedStamp) return cached
        return synchronized(this) {
            val again = cachedIndex
            if (again != null && stamp == cachedStamp) {
                again
            } else {
                val loaded = try {
                    SortedDomainIndex.fromBytes(listFile.readBytes())
                } catch (_: Exception) {
                    null
                }
                cachedIndex = loaded
                cachedStamp = if (loaded != null) stamp else Long.MIN_VALUE
                loaded
            }
        }
    }

    /**
     * Replaces the stored list with [sortedDomains] in one atomic rename and
     * invalidates the cached index so the next lookup sees the new list.
     */
    fun writeDomainsAtomically(sortedDomains: List<String>) {        directory.mkdirs()
        val serialized = AdultBlocklistParser.serializeDomains(sortedDomains)
        tempFile.writeText(serialized, Charsets.UTF_8)
        if (!tempFile.renameTo(listFile)) {
            // Rename can refuse across mount quirks on some OEM builds; fall
            // back to a direct write (still better than losing the update).
            listFile.writeText(serialized, Charsets.UTF_8)
            tempFile.delete()
        }
        synchronized(this) {
            cachedIndex = null
            cachedStamp = Long.MIN_VALUE
        }
    }

    /** The currently stored domains (the file's lines), or empty. */
    fun readDomains(): List<String> {
        if (!hasList()) return emptyList()
        return try {
            listFile.readLines(Charsets.UTF_8)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * #38 (batch 2): re-merge the user's own added domains into the
     * stored list after they added/removed entries. The base list is the
     * current file minus the PREVIOUS user set (so removals really
     * disappear), plus the NEW user set. Downloaded updates merge the
     * user set themselves (see AdultBlocklistUpdater), so user entries
     * survive weekly refreshes.
     */
    fun rebuildWithUserDomains(userDomains: Set<String>, previousUserDomains: Set<String>) {
        val base = readDomains().filter { it !in previousUserDomains }
        val merged = AdultBlocklistParser.mergeDomainSets(listOf(base, userDomains))
        writeDomainsAtomically(merged)
    }

    private fun stampOf(): Long {
        return listFile.lastModified() * 31L + listFile.length()
    }

    companion object {
        const val BLOCKLIST_DIR_NAME = "blocklist"
        const val LIST_FILE_NAME = "adult_domains.txt"
        const val TEMP_FILE_NAME = "adult_domains.txt.tmp"
    }
}
