package com.example

import android.app.Application
import com.example.data.blocklist.AdultBlocklistStore
import com.example.data.blocklog.ScreenshotStore
import com.example.data.blocklist.AdultBlocklistUpdater
import com.example.data.local.ShieldDatabase
import com.example.data.preferences.ShieldPreferences
import com.example.data.repository.ShieldRepository
import com.example.deviceowner.DeviceOwnerManager
import com.example.notification.LockdownNotificationManager
import com.example.worker.BlocklistUpdateScheduler

class ShieldApplication : Application() {
    lateinit var database: ShieldDatabase
        private set

    lateinit var preferences: ShieldPreferences
        private set

    lateinit var repository: ShieldRepository
        private set

    lateinit var adultBlocklistStore: AdultBlocklistStore
        private set

    lateinit var adultBlocklistUpdater: AdultBlocklistUpdater
        private set

    lateinit var screenshotStore: ScreenshotStore
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = ShieldDatabase.getDatabase(this)
        preferences = ShieldPreferences(this)
        // Block-log screenshots (#14) live in app-private storage; the
        // store is created before the repository so record deletion and
        // retention pruning can remove the files together with the rows.
        screenshotStore = ScreenshotStore(this)

        // Adult site blocking (#12/#13): the on-disk domain list, its
        // updater, and the weekly self-refresh schedule. The store is
        // created before the repository so a backup restore (#62) can
        // rebuild the merged list file.
        adultBlocklistStore = AdultBlocklistStore(this)
        adultBlocklistUpdater = AdultBlocklistUpdater(adultBlocklistStore, preferences)
        repository = ShieldRepository(
            database.shieldDao(),
            preferences,
            screenshotStore,
            adultBlocklistStore
        )
        BlocklistUpdateScheduler.enqueueWeekly(this)

        // Device Owner protection (#11): re-apply (or clear) the DPM
        // restrictions from the stored toggle choice, so the applied
        // state survives reboots and process death. No-ops unless the
        // app actually is the device owner.
        DeviceOwnerManager.reconcile(this)

        // Notifications are minimal by design: create the streak channel and
        // make sure the single daily 9:00 AM streak alarm is scheduled.
        LockdownNotificationManager.createNotificationChannel(this)
        LockdownNotificationManager.scheduleDailyStreakAlarm(this)
    }

    companion object {
        lateinit var instance: ShieldApplication
            private set
    }
}
