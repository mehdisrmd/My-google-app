package com.example.data.blocklist

import com.example.data.preferences.ShieldPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads, merges and stores the adult domain list (#13).
 *
 * Every source in the persisted, user-managed source list (#49 — see
 * [ShieldPreferences.getBlocklistSources], seeded from [BlocklistSources]
 * defaults) is tried (canonical URL first, jsDelivr mirror as fallback).
 * A full update succeeds when AT LEAST ONE source downloads and parses —
 * partial coverage beats no coverage. When every source fails, the
 * previously stored list (if any) is left untouched.
 *
 * #61 (batch 5): each repository's downloaded set is stored SEPARATELY
 * (see AdultBlocklistStore's per-source sets) and the merged list the
 * matcher reads is rebuilt from those sets plus the user's own domains.
 * That makes per-repository updates exact — updating one repository
 * replaces only its contribution — and makes removing a repository
 * remove exactly its domains from the merged list. A repository whose
 * update FAILS keeps its previously stored set: a failed refresh never
 * shrinks the list.
 *
 * Networking uses plain [HttpURLConnection]: OkHttp is commented out in
 * build.gradle.kts, and restoring a whole HTTP stack for one simple GET
 * job is not justified — the updater needs nothing beyond redirects,
 * timeouts and a body read.
 *
 * Play Protect hygiene (#25): this updater downloads DATA only — plain
 * text domain lists that are parsed as strings. Nothing it fetches is
 * ever executed, loaded as code, or written anywhere code could run
 * from. Keep it that way.
 *
 * All entry points are serialized on one shared [Mutex] so the first-launch
 * download, the manual "update now" button and the weekly worker can never
 * interleave writes.
 */
class AdultBlocklistUpdater(
    private val store: AdultBlocklistStore,
    private val preferences: ShieldPreferences
) {

    data class UpdateResult(
        val success: Boolean,
        val domainCount: Int,
        val sourcesSucceeded: Int,
        val sourcesTotal: Int,
        // #61: per-repository outcome of this cycle, keyed by
        // BlocklistSources.keyFor — the UI turns a failed repo red.
        val perSourceResults: Map<String, Boolean> = emptyMap()
    )

    /**
     * Runs one full update cycle over every configured repository, in
     * list order. [onSourceUpdated] fires as each repository finishes
     * (on the IO thread) so the UI can spin/stop each repo's own
     * indicator in turn. Safe to call from any thread.
     */
    suspend fun updateNow(
        onSourceUpdated: ((BlocklistSource, Boolean) -> Unit)? = null
    ): UpdateResult = updateMutex.withLock {
        withContext(Dispatchers.IO) { runUpdateAll(onSourceUpdated) }
    }

    /**
     * #61: updates ONE repository and rebuilds the merged list. When
     * some configured repository has no stored set yet (fresh install
     * or upgrade from before per-source sets existed), a single-repo
     * update first completes a full cycle — otherwise the rebuild
     * would silently drop the never-downloaded repositories' domains.
     * Returns the requested repository's own outcome.
     */
    suspend fun updateSourceNow(source: BlocklistSource): Boolean =
        updateMutex.withLock {
            withContext(Dispatchers.IO) { runUpdateSource(source) }
        }

    /**
     * #61: a repository was removed from the list — drop its stored
     * set and rebuild the merged list without it, so its domains stop
     * being blocked (they were that repository's contribution).
     */
    suspend fun onSourceRemoved(source: BlocklistSource) {
        updateMutex.withLock {
            withContext(Dispatchers.IO) {
                store.deleteSourceDomains(BlocklistSources.keyFor(source))
                rebuildMergedFromStoredSets()
            }
        }
    }

    /**
     * Writes the tiny built-in fallback list (see [BlocklistSources
     * .FALLBACK_SEED_DOMAINS]). Called when the first-ever download failed
     * and the user chose to continue anyway: the toggle stays meaningful
     * offline, the recorded domain count reflects the seed, and the
     * last-updated timestamp stays 0 ("never updated from the internet")
     * so the UI can keep nudging toward a real update.
     */
    fun writeFallbackSeed() {
        val seed = AdultBlocklistParser.mergeDomainSets(
            listOf(
                BlocklistSources.FALLBACK_SEED_DOMAINS,
                preferences.getUserBlocklistDomains()
            )
        )
        store.writeDomainsAtomically(seed)
        preferences.setAdultBlocklistDomainCount(seed.size)
    }

    private fun runUpdateAll(
        onSourceUpdated: ((BlocklistSource, Boolean) -> Unit)?
    ): UpdateResult {
        // #49: iterate the persisted source list, not the hardcoded
        // defaults — the user may have removed or added repositories.
        val sources = preferences.getBlocklistSources()
        val perSource = LinkedHashMap<String, Boolean>()
        var succeeded = 0
        for (source in sources) {
            val ok = downloadAndStore(source)
            perSource[BlocklistSources.keyFor(source)] = ok
            if (ok) succeeded++
            onSourceUpdated?.invoke(source, ok)
        }

        if (succeeded == 0) {
            return UpdateResult(
                success = false,
                domainCount = store.currentIndex()?.size ?: 0,
                sourcesSucceeded = 0,
                sourcesTotal = sources.size,
                perSourceResults = perSource
            )
        }

        val mergedCount = rebuildMergedFromStoredSets()
        preferences.saveAdultBlocklistUpdate(
            lastUpdatedMs = System.currentTimeMillis(),
            domainCount = mergedCount
        )
        // #50 (batch 3): a real download landed — its timestamp is what
        // opens the activation gate (see BlocklistActivationGate); there
        // is no separate pending flag anymore.
        return UpdateResult(
            success = true,
            domainCount = mergedCount,
            sourcesSucceeded = succeeded,
            sourcesTotal = sources.size,
            perSourceResults = perSource
        )
    }

    private fun runUpdateSource(source: BlocklistSource): Boolean {
        val sources = preferences.getBlocklistSources()
        val anySetMissing = sources.any {
            store.readSourceDomains(BlocklistSources.keyFor(it)) == null
        }
        if (anySetMissing) {
            // Complete the per-source sets first (see updateSourceNow):
            // the requested repo's outcome is its own result in the
            // full cycle.
            val result = runUpdateAll(null)
            return result.perSourceResults[BlocklistSources.keyFor(source)] == true
        }
        val ok = downloadAndStore(source)
        if (ok) {
            val mergedCount = rebuildMergedFromStoredSets()
            preferences.saveAdultBlocklistUpdate(
                lastUpdatedMs = System.currentTimeMillis(),
                domainCount = mergedCount
            )
        }
        return ok
    }

    /** Downloads and parses one source; on success stores its set. */
    private fun downloadAndStore(source: BlocklistSource): Boolean {
        val text = downloadFirstAvailable(source.urls) ?: return false
        val domains = AdultBlocklistParser.parseDomains(text)
        if (domains.isEmpty()) return false
        store.writeSourceDomains(
            BlocklistSources.keyFor(source),
            AdultBlocklistParser.mergeDomainSets(listOf(domains))
        )
        return true
    }

    /**
     * Rebuilds the merged list from the stored per-source sets of the
     * CURRENTLY configured repositories, plus the user's own domains,
     * and returns the merged count.
     *
     * Before per-source mode exists (no source set was ever stored —
     * a legacy install whose merged file came from an older version,
     * or the fallback seed), the merged file is left exactly as it is:
     * its domains cannot be attributed to repositories, and dropping
     * them would silently unblock sites. Once per-source mode exists,
     * the rebuild is exact; if it would produce an empty list (every
     * repository removed), the built-in starter seed takes over so the
     * toggle never guards an empty list — the same promise the card's
     * empty-state text makes.
     */
    internal fun rebuildMergedFromStoredSets(): Int {
        if (!store.hasAnySourceSets()) {
            return store.currentIndex()?.size ?: 0
        }
        val sets = preferences.getBlocklistSources().mapNotNull { source ->
            store.readSourceDomains(BlocklistSources.keyFor(source))
        }
        var merged = AdultBlocklistParser.mergeDomainSets(
            sets + listOf(preferences.getUserBlocklistDomains())
        )
        if (merged.isEmpty()) {
            merged = AdultBlocklistParser.mergeDomainSets(
                listOf(
                    BlocklistSources.FALLBACK_SEED_DOMAINS,
                    preferences.getUserBlocklistDomains()
                )
            )
        }
        store.writeDomainsAtomically(merged)
        preferences.setAdultBlocklistDomainCount(merged.size)
        return merged.size
    }

    private fun downloadFirstAvailable(urls: List<String>): String? {
        for (url in urls) {
            val text = download(url)
            if (!text.isNullOrBlank()) return text
        }
        return null
    }

    private fun download(urlString: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "X-Blocker/0.5 (Android; blocklist updater)")
            }
            if (connection.responseCode !in 200..299) {
                null
            } else {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000

        // Shared across every updater instance (Application, worker,
        // activity) so update cycles never overlap.
        private val updateMutex = Mutex()
    }
}
