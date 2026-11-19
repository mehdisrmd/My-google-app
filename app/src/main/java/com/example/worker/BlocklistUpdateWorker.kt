package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.ShieldApplication
import java.util.concurrent.TimeUnit

/**
 * Weekly refresh of the adult domain blocklist (#13): downloads every
 * source in [com.example.data.blocklist.BlocklistSources], merges and
 * deduplicates them, and atomically swaps the stored list. Scheduled once
 * from [ShieldApplication] with a connected-network constraint; a failed
 * cycle asks WorkManager for a retry instead of giving up until next week.
 */
class BlocklistUpdateWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? ShieldApplication ?: return Result.failure()
        return try {
            val result = app.adultBlocklistUpdater.updateNow()
            if (result.success) Result.success() else Result.retry()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

/** Scheduling entry points for [BlocklistUpdateWorker]. */
object BlocklistUpdateScheduler {

    private const val UNIQUE_WEEKLY_WORK_NAME = "adult_blocklist_weekly_update"

    /**
     * Enqueues the 7-day periodic update exactly once. KEEP policy: later
     * app starts never reset the weekly cadence of an existing schedule.
     */
    fun enqueueWeekly(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<BlocklistUpdateWorker>(7, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()
        try {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WEEKLY_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        } catch (_: IllegalStateException) {
            // WorkManager is not initialized in this process — e.g. under
            // Robolectric, where the library's InitializationProvider does
            // not run. On a real device the default initializer runs before
            // Application.onCreate, so this is a no-op safeguard: skip the
            // schedule instead of crashing app startup.
        }
    }
}
