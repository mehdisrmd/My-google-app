package com.example

import android.app.Application
import com.example.data.local.ShieldDatabase
import com.example.data.preferences.ShieldPreferences
import com.example.data.repository.ShieldRepository
import com.example.notification.LockdownNotificationManager

class ShieldApplication : Application() {
    lateinit var database: ShieldDatabase
        private set

    lateinit var preferences: ShieldPreferences
        private set

    lateinit var repository: ShieldRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = ShieldDatabase.getDatabase(this)
        preferences = ShieldPreferences(this)
        repository = ShieldRepository(database.shieldDao(), preferences)

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
