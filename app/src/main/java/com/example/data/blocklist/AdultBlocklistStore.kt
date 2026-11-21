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
     * Replaces the stored list with [sortedDomains] in one atomic rename.
     *
     * #53 (batch 4): the lookup index is rebuilt HERE, from the bytes
     * just serialized, instead of invalidating the cache — previously
     * the first accessibility event after every update re-read and
     * re-parsed the whole file (hundreds of thousands of domains) on
     * the main thread. The stamp is taken after the write, so a later
     * external change still triggers a reload in [currentIndex].
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
            cachedIndex = SortedDomainIndex.fromBytes(serialized.toByteArray(Charsets.UTF_8))
            cachedStamp = stampOf()
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

    // ------------------------------------------------------------------
    // #61 (batch 5): per-source domain sets.
    //
    // Each repository's last successfully downloaded set is stored on
    // its own — filesDir/blocklist/sources/<key>.txt, one sorted domain
    // per line, keyed by BlocklistSources.keyFor. The merged
    // adult_domains.txt stays the ONE list the matcher reads; the
    // updater rebuilds it from these sets (+ the user's own domains).
    // Per-source sets are what make per-repo updates exact: updating
    // one repository replaces only its contribution, and removing a
    // repository removes exactly its domains from the merged list.
    // ------------------------------------------------------------------

    private val sourcesDirectory = File(directory, SOURCES_DIR_NAME)

    private fun sourceFile(key: String): File = File(sourcesDirectory, "$key.txt")

    /**
     * True once per-source mode exists: the sources directory is
     * created by the first per-source write and never removed, so its
     * existence is the marker that the merged list is (re)built from
     * per-source sets — as opposed to a legacy merged file whose
     * domains cannot be attributed to any repository.
     */
    fun hasAnySourceSets(): Boolean {
        return try {
            sourcesDirectory.isDirectory
        } catch (_: Exception) {
            false
        }
    }

    /** Stores one repository's downloaded set (sorted, one per line). */
    fun writeSourceDomains(key: String, sortedDomains: List<String>) {
        sourcesDirectory.mkdirs()
        val file = sourceFile(key)
        val temp = File(sourcesDirectory, "$key.txt.tmp")
        val serialized = AdultBlocklistParser.serializeDomains(sortedDomains)
        temp.writeText(serialized, Charsets.UTF_8)
        if (!temp.renameTo(file)) {
            file.writeText(serialized, Charsets.UTF_8)
            temp.delete()
        }
    }

    /** One repository's stored set, or null when it was never downloaded. */
    fun readSourceDomains(key: String): List<String>? {
        val file = sourceFile(key)
        if (!file.isFile) return null
        return try {
            file.readLines(Charsets.UTF_8)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    /** Drops one repository's stored set (the repository was removed). */
    fun deleteSourceDomains(key: String) {
        try {
            sourceFile(key).delete()
        } catch (_: Exception) {
            // A set that cannot be deleted is stale data, not a crash:
            // the rebuild only reads sets of CURRENT sources, so an
            // orphaned file is never merged back in.
        }
    }

    private fun stampOf(): Long {
        return listFile.lastModified() * 31L + listFile.length()
    }

    companion object {
        const val BLOCKLIST_DIR_NAME = "blocklist"
        const val LIST_FILE_NAME = "adult_domains.txt"
        const val TEMP_FILE_NAME = "adult_domains.txt.tmp"
        const val SOURCES_DIR_NAME = "sources"
    }
}
