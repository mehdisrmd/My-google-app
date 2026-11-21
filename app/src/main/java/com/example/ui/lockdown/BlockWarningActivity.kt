package com.example.ui.lockdown

import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import com.example.service.OverlayDisplayGate
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SeasonalTheme

/**
 * #47 (batch 3) Scope B chose WHICH locks get the warning presentation:
 * the first block of a section (keyword / adult / custom) on a day.
 * #59 (batch 5) retired the separate warning LOOK: Mehdi asked why the
 * site-block page differed from the regular block page — it must be
 * the SAME page, with the 10-second free button present for these
 * site blocks.
 *
 * So this activity now renders the very same [LockdownScreen] as
 * [LockdownActivity], fed from the same stored lock. What remains
 * distinct is only the selection: the accessibility service launches
 * this activity while the stored lock carries a warning section, and
 * the section's first-block-of-day counting is untouched. Adult-site
 * locks are started with graceAvailable = true (see the service's
 * adult path), so the 10-second free button on the unified page is
 * live for site blocks exactly as #59 ordered; keyword/custom locks
 * carry the grace their episode decision granted.
 *
 * The duplicated label from the old warning page ("Blocked: Adult
 * Site Blocked: sex.com" — the page prefixed "Blocked:" onto a trigger
 * that already says it) disappears with the old layout: the unified
 * page shows the trigger once, as the block reason.
 *
 * State discipline mirrors LockdownActivity: everything is read from
 * the stored lock, the page stands down only for the lock screen
 * (#18; #57 — never for a call), pulls itself back when the user tries
 * to leave mid-lock, and clears the lock when its timer ends. A light
 * poll additionally stands this page down if the stored lock stops
 * being a warning lock (the service then shows the right page).
 */
class BlockWarningActivity : ComponentActivity() {

    private var lockEndTimeState by mutableLongStateOf(0L)
    private var lockDurationState by mutableIntStateOf(5)
    private var lockTriggerState by mutableStateOf("")
    private var lockSourceAppState by mutableStateOf("")
    private var warningSectionState by mutableStateOf("")
    private var lockGraceAvailableState by mutableStateOf(false)

    // Polls while resumed: refreshes the state from the stored lock,
    // finishes when the lock ends or stops being a warning lock, and
    // stands down for the lock screen.
    private val pollHandler = Handler(Looper.getMainLooper())
    private val pollRunnable = object : Runnable {
        override fun run() {
            if (shouldYieldToSystemSurface()) {
                finish()
                return
            }
            refreshStateFromPrefs()
            val prefs = ShieldApplication.instance.preferences
            if (!prefs.isCurrentlyLocked()) {
                // The stored lock is the source of truth: if it says
                // the lock is over, this page is done. Clear it the
                // way LockdownActivity's expiry path does.
                prefs.clearLockdown()
                finish()
                return
            }
            if (warningSectionState.isBlank()) {
                // Not a warning lock after all (e.g. a stale launch):
                // stand down; the service shows the right page.
                finish()
                return
            }
            pollHandler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    private fun shouldYieldToSystemSurface(): Boolean {
        val keyguardLocked = try {
            (getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isKeyguardLocked == true
        } catch (_: Exception) {
            false
        }
        // #57 (batch 5): this page is a presentation of the block
        // screen, so it follows the block-screen table — it stands
        // down only for the lock screen (#18), never for a call.
        return OverlayDisplayGate.decideBlockScreen(
            keyguardLocked = keyguardLocked
        ) != OverlayDisplayGate.Decision.SHOW
    }

    private fun refreshStateFromPrefs() {
        val prefs = ShieldApplication.instance.preferences
        lockEndTimeState = prefs.getActiveLockEndTime()
        lockDurationState = prefs.getActiveLockDurationMinutes()
        lockTriggerState = prefs.getActiveLockTrigger()
        lockSourceAppState = prefs.getActiveLockApp()
        warningSectionState = prefs.getActiveLockWarningSection()
        lockGraceAvailableState = prefs.getActiveLockGraceAvailable()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLockscreenFlags()
        refreshStateFromPrefs()

        val prefs = ShieldApplication.instance.preferences
        setContent {
            val isDarkTheme = prefs.isDarkTheme.collectAsStateWithLifecycle().value
            val isPersian = prefs.isPersianLanguage.collectAsStateWithLifecycle().value
            val seasonalThemeName = prefs.seasonalThemeName.collectAsStateWithLifecycle().value
            // #56 (batch 5): call & SMS options only under Strict mode.
            val strictModeEnabled = prefs.strictModeEnabled.collectAsStateWithLifecycle().value

            MyApplicationTheme(
                darkTheme = isDarkTheme,
                isPersianRtl = isPersian,
                seasonalTheme = SeasonalTheme.fromName(seasonalThemeName)
            ) {
                // #59: the unified block page — the same composable
                // LockdownActivity renders, including the 10-second
                // free button whenever the stored lock grants grace.
                LockdownScreen(
                    endTimeEpoch = lockEndTimeState,
                    totalDurationMinutes = lockDurationState,
                    triggerWord = lockTriggerState,
                    sourceApp = lockSourceAppState,
                    graceAvailable = lockGraceAvailableState,
                    strictModeEnabled = strictModeEnabled,
                    onGraceFreeRequested = {
                        // Mirrors LockdownActivity: open the 10-second
                        // free window (ends this lock); leave only when
                        // the lock is really over.
                        if (lockGraceAvailableState) {
                            prefs.beginKeywordGracePeriod()
                        }
                        if (prefs.isCurrentlyLocked()) {
                            refreshStateFromPrefs()
                        } else {
                            finish()
                        }
                    },
                    onLockExpired = {
                        // Mirrors LockdownActivity: the stored lock is
                        // the source of truth — refresh a stale copy
                        // instead of clearing a live lock.
                        if (prefs.isCurrentlyLocked()) {
                            refreshStateFromPrefs()
                        } else {
                            prefs.clearLockdown()
                            finish()
                        }
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStateFromPrefs()
        val prefs = ShieldApplication.instance.preferences
        if (!prefs.isCurrentlyLocked() || warningSectionState.isBlank()) {
            finish()
            return
        }
        pollHandler.removeCallbacks(pollRunnable)
        pollHandler.post(pollRunnable)
    }

    override fun onPause() {
        pollHandler.removeCallbacks(pollRunnable)
        super.onPause()
        // Pull back when the user tries to leave mid-lock — the same
        // rule (and the same Call/SMS exception) as LockdownActivity.
        val prefs = ShieldApplication.instance.preferences
        if (prefs.isCurrentlyLocked() && !prefs.hasActiveEmergencyCommWindow()) {
            try {
                val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                am?.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME)
            } catch (e: Exception) {
                Log.w(TAG, "moveTaskToFront failed", e)
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val prefs = ShieldApplication.instance.preferences
        if (prefs.isCurrentlyLocked() && !prefs.hasActiveEmergencyCommWindow()) {
            val relaunch = Intent(this, BlockWarningActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
            }
            try {
                startActivity(relaunch)
            } catch (e: Exception) {
                Log.w(TAG, "relaunch failed", e)
            }
        }
    }

    private fun configureLockscreenFlags() {
        val prefs = runCatching { ShieldApplication.instance.preferences }.getOrNull()
        val applySecure = prefs?.isFlagSecureCurrentlyActive() == true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            // #18: never draw over the lock screen; the service
            // relaunches after unlock while the lock still applies.
            setShowWhenLocked(false)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
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
        private const val TAG = "BlockWarning"
        private const val POLL_INTERVAL_MS = 1000L
    }
}
