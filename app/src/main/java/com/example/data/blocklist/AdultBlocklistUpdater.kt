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
 * Every source in [BlocklistSources] is tried (canonical URL first, jsDelivr
 * mirror as fallback). The update succeeds when AT LEAST ONE source
 * downloads and parses — partial coverage beats no coverage — and the
 * merged, deduplicated set is swapped in atomically by the store. When every
 * source fails, the previously stored list (if any) is left untouched.
 *
 * Networking uses plain [HttpURLConnection]: OkHttp is commented out in
 * build.gradle.kts, and restoring a whole HTTP stack for one simple GET
 * job is not justified — the updater needs nothing beyond redirects,
 * timeouts and a body read.
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
        val sourcesTotal: Int
    )

    /** Runs one full update cycle. Safe to call from any thread. */
    suspend fun updateNow(): UpdateResult = updateMutex.withLock {
        withContext(Dispatchers.IO) { runUpdate() }
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
            listOf(BlocklistSources.FALLBACK_SEED_DOMAINS)
        )
        store.writeDomainsAtomically(seed)
        preferences.setAdultBlocklistDomainCount(seed.size)
    }

    private fun runUpdate(): UpdateResult {
        val parsedSets = mutableListOf<Set<String>>()
        var succeeded = 0
        for (source in BlocklistSources.SOURCES) {
            val text = downloadFirstAvailable(source.urls) ?: continue
            val domains = AdultBlocklistParser.parseDomains(text)
            if (domains.isNotEmpty()) {
                parsedSets.add(domains)
                succeeded++
            }
        }

        if (parsedSets.isEmpty()) {
            return UpdateResult(
                success = false,
                domainCount = store.currentIndex()?.size ?: 0,
                sourcesSucceeded = 0,
                sourcesTotal = BlocklistSources.SOURCES.size
            )
        }

        val merged = AdultBlocklistParser.mergeDomainSets(parsedSets)
        store.writeDomainsAtomically(merged)
        preferences.saveAdultBlocklistUpdate(
            lastUpdatedMs = System.currentTimeMillis(),
            domainCount = merged.size
        )
        return UpdateResult(
            success = true,
            domainCount = merged.size,
            sourcesSucceeded = succeeded,
            sourcesTotal = BlocklistSources.SOURCES.size
        )
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
