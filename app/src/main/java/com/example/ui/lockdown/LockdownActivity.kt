package com.example.ui.lockdown

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.MainActivity
import com.example.ShieldApplication
import com.example.ui.theme.MyApplicationTheme

class LockdownActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferences = ShieldApplication.instance.preferences

        // Apply FLAG_SECURE only when Anti-Agent Black Screen Shield is enabled by the user
        if (preferences.isFlagSecureCurrentlyActive()) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // Keep screen on during active lockout and allow showing over keyguard
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())

        setContent {
            MyApplicationTheme(darkTheme = true) {
                LockdownScreen(
                    endTimeEpoch = preferences.getActiveLockEndTime(),
                    totalDurationMinutes = preferences.getActiveLockDurationMinutes(),
                    triggerWord = preferences.getActiveLockTrigger(),
                    sourceApp = preferences.getActiveLockApp(),
                    onLockExpired = {
                        preferences.clearLockdown()
                        com.example.notification.LockdownNotificationManager.cancelLockdownNotification(this@LockdownActivity)
                        val intent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val preferences = ShieldApplication.instance.preferences
        if (preferences.isFlagSecureCurrentlyActive()) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        // If lockdown expired before resuming, finish cleanly
        if (!preferences.isCurrentlyLocked()) {
            preferences.clearLockdown()
        }
    }

    companion object {
        const val EXTRA_STRICT_MODE = "extra_strict_mode"
    }
}
