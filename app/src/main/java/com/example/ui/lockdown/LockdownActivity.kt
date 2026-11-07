package com.example.ui.lockdown

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ShieldApplication
import com.example.notification.LockdownNotificationManager
import com.example.ui.theme.MyApplicationTheme

class LockdownActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLockscreenFlags()

        val prefs = ShieldApplication.instance.preferences
        val endTime = prefs.getActiveLockEndTime()
        val duration = prefs.getActiveLockDurationMinutes()
        val trigger = prefs.getActiveLockTrigger()
        val sourceApp = prefs.getActiveLockApp()

        setContent {
            val isDarkTheme = prefs.isDarkTheme.collectAsStateWithLifecycle().value
            val isPersian = prefs.isPersianLanguage.collectAsStateWithLifecycle().value

            MyApplicationTheme(
                darkTheme = isDarkTheme,
                isPersianRtl = isPersian
            ) {
                LockdownScreen(
                    endTimeEpoch = endTime,
                    totalDurationMinutes = duration,
                    triggerWord = trigger,
                    sourceApp = sourceApp,
                    onLockExpired = {
                        prefs.clearLockdown()
                        LockdownNotificationManager.cancelNotification(this@LockdownActivity)
                        finish()
                    }
                )
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Block DEL and CTRL keys on lock screen to prevent keyboard shortcut bypasses
        if (event.keyCode == KeyEvent.KEYCODE_DEL ||
            event.keyCode == KeyEvent.KEYCODE_FORWARD_DEL ||
            event.keyCode == KeyEvent.KEYCODE_CTRL_LEFT ||
            event.keyCode == KeyEvent.KEYCODE_CTRL_RIGHT ||
            event.isCtrlPressed
        ) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onResume() {
        super.onResume()
        val prefs = ShieldApplication.instance.preferences
        if (!prefs.isCurrentlyLocked()) {
            LockdownNotificationManager.cancelNotification(this)
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        val prefs = ShieldApplication.instance.preferences
        if (prefs.isCurrentlyLocked() && prefs.getStrictModeEnabled()) {
            try {
                val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                am?.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME)
            } catch (_: Exception) {
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val prefs = ShieldApplication.instance.preferences
        if (prefs.isCurrentlyLocked() && prefs.getStrictModeEnabled()) {
            val relaunch = Intent(this, LockdownActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(relaunch)
        }
    }

    private fun configureLockscreenFlags() {
        val prefs = runCatching { ShieldApplication.instance.preferences }.getOrNull()
        val applySecure = prefs?.isFlagSecureCurrentlyActive() == true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (applySecure) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    companion object {
        const val EXTRA_STRICT_MODE = "extra_strict_mode"
    }
}
