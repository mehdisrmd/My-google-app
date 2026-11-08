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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ShieldApplication
import com.example.notification.LockdownNotificationManager
import com.example.ui.theme.MyApplicationTheme

class LockdownActivity : ComponentActivity() {

    private var lockEndTimeState by mutableLongStateOf(0L)
    private var lockDurationState by mutableIntStateOf(5)
    private var lockTriggerState by mutableStateOf("Protected Content")
    private var lockSourceAppState by mutableStateOf("System")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLockscreenFlags()
        refreshStateFromPrefsAndIntent(intent)

        val prefs = ShieldApplication.instance.preferences

        setContent {
            val isDarkTheme = prefs.isDarkTheme.collectAsStateWithLifecycle().value
            val isPersian = prefs.isPersianLanguage.collectAsStateWithLifecycle().value

            MyApplicationTheme(
                darkTheme = isDarkTheme,
                isPersianRtl = isPersian
            ) {
                LockdownScreen(
                    endTimeEpoch = lockEndTimeState,
                    totalDurationMinutes = lockDurationState,
                    triggerWord = lockTriggerState,
                    sourceApp = lockSourceAppState,
                    onLockExpired = {
                        prefs.clearLockdown()
                        LockdownNotificationManager.cancelNotification(this@LockdownActivity)
                        finish()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        refreshStateFromPrefsAndIntent(intent)
    }

    private fun refreshStateFromPrefsAndIntent(incoming: Intent?) {
        val prefs = ShieldApplication.instance.preferences
        val prefEnd = prefs.getActiveLockEndTime()
        val intentEnd = incoming?.getLongExtra(EXTRA_END_TIME_EPOCH, 0L) ?: 0L
        lockEndTimeState = maxOf(prefEnd, intentEnd)
        lockDurationState = prefs.getActiveLockDurationMinutes()
        lockTriggerState = incoming?.getStringExtra(EXTRA_TRIGGER_WORD)
            ?.takeIf { it.isNotBlank() }
            ?: prefs.getActiveLockTrigger()
        lockSourceAppState = incoming?.getStringExtra(EXTRA_SOURCE_APP)
            ?.takeIf { it.isNotBlank() }
            ?: prefs.getActiveLockApp()
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
        refreshStateFromPrefsAndIntent(intent)
        if (!prefs.isCurrentlyLocked()) {
            LockdownNotificationManager.cancelNotification(this)
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        val prefs = ShieldApplication.instance.preferences
        // If user did not tap Phone Call / SMS and tries to leave while timer is counting down, pull back!
        if (prefs.isCurrentlyLocked() && !prefs.isPackageAllowedDuringLockdown("com.android.dialer")) {
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
        if (prefs.isCurrentlyLocked() && !prefs.isPackageAllowedDuringLockdown("com.android.dialer")) {
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
        const val EXTRA_TRIGGER_WORD = "extra_trigger_word"
        const val EXTRA_SOURCE_APP = "extra_source_app"
        const val EXTRA_END_TIME_EPOCH = "extra_end_time_epoch"
    }
}
