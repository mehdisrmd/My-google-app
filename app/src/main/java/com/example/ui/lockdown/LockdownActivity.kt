package com.example.ui.lockdown

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SeasonalTheme

class LockdownActivity : ComponentActivity() {

    private var lockEndTimeState by mutableLongStateOf(0L)
    private var lockDurationState by mutableIntStateOf(5)
    private var lockTriggerState by mutableStateOf("Protected Content")
    private var lockSourceAppState by mutableStateOf("System")
    private var lockGraceAvailableState by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLockscreenFlags()
        refreshStateFromPrefsAndIntent(intent)

        val prefs = ShieldApplication.instance.preferences

        setContent {
            val isDarkTheme = prefs.isDarkTheme.collectAsStateWithLifecycle().value
            val isPersian = prefs.isPersianLanguage.collectAsStateWithLifecycle().value
            val seasonalThemeName = prefs.seasonalThemeName.collectAsStateWithLifecycle().value

            MyApplicationTheme(
                darkTheme = isDarkTheme,
                isPersianRtl = isPersian,
                seasonalTheme = SeasonalTheme.fromName(seasonalThemeName)
            ) {
                LockdownScreen(
                    endTimeEpoch = lockEndTimeState,
                    totalDurationMinutes = lockDurationState,
                    triggerWord = lockTriggerState,
                    sourceApp = lockSourceAppState,
                    graceAvailable = lockGraceAvailableState,
                    onGraceFreeRequested = {
                        // Opens the 10-second free window and ends this lock;
                        // the accessibility service re-blocks without the
                        // button if the trigger word is still on screen.
                        // A stale tap (this screen no longer shows the button,
                        // or the lock already ended) is ignored by the
                        // preferences too, so check what actually happened:
                        // only leave the screen when the lock is really over.
                        if (lockGraceAvailableState) {

                            prefs.beginKeywordGracePeriod()
                        }
                        if (prefs.isCurrentlyLocked()) {
                            refreshStateFromPrefsAndIntent(intent)
                        } else {
                            finish()
                        }
                    },
                    onLockExpired = {
                        // The screen's own copy of the end time reached zero.
                        // The STORED lock is the source of truth: if it says a
                        // lock is still running, this copy was stale, so
                        // refresh it instead of clearing a live lock.
                        if (prefs.isCurrentlyLocked()) {
                            refreshStateFromPrefsAndIntent(intent)
                        } else {
                            prefs.clearLockdown()
                            finish()
                        }
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
        // The end time comes ONLY from the stored lock. The launch intent also
        // carries EXTRA_END_TIME_EPOCH, but that value can be stale (an
        // activity recreated from its original intent, or a relaunch queued
        // before the lock was replaced or cleared) and used to keep this
        // screen alive past the real end. The service writes the lock to
        // preferences before it launches this screen, so the stored value is
        // always already there.
        lockEndTimeState = prefs.getActiveLockEndTime()
        lockDurationState = prefs.getActiveLockDurationMinutes()
        lockTriggerState = incoming?.getStringExtra(EXTRA_TRIGGER_WORD)
            ?.takeIf { it.isNotBlank() }
            ?: prefs.getActiveLockTrigger()
        lockSourceAppState = incoming?.getStringExtra(EXTRA_SOURCE_APP)
            ?.takeIf { it.isNotBlank() }
            ?: prefs.getActiveLockApp()
        lockGraceAvailableState = prefs.getActiveLockGraceAvailable()
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
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        val prefs = ShieldApplication.instance.preferences
        // If user did not tap Phone Call / SMS and tries to leave while timer is counting down, pull back!
        // (The comm window — not the package allowlist — marks a deliberate
        // trip to the dialer/messages; dialer/SMS packages themselves are
        // always allowed during a block, see ShieldPreferences.)
        if (prefs.isCurrentlyLocked() && !prefs.hasActiveEmergencyCommWindow()) {
            try {
                val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                am?.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME)
            } catch (e: Exception) {
                // moveTaskToFront needs the REORDER_TASKS permission, which
                // this app does not declare, so this pull-back is a no-op and
                // the accessibility service does the real enforcement. Log it
                // instead of hiding it, so the gap is visible in logcat.
                Log.w(TAG, "moveTaskToFront failed", e)
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()

        val prefs = ShieldApplication.instance.preferences
        if (prefs.isCurrentlyLocked() && !prefs.hasActiveEmergencyCommWindow()) {
            val relaunch = Intent(this, LockdownActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    // This relaunch is not a user action; without the flag it
                    // can raise another onUserLeaveHint and chain relaunches.
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
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
        private const val TAG = "LockdownActivity"

        const val EXTRA_STRICT_MODE = "extra_strict_mode"
        const val EXTRA_TRIGGER_WORD = "extra_trigger_word"
        const val EXTRA_SOURCE_APP = "extra_source_app"
        const val EXTRA_END_TIME_EPOCH = "extra_end_time_epoch"
    }
}
