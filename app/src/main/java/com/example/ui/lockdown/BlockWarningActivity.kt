package com.example.ui.lockdown

import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ShieldApplication
import com.example.service.OverlayDisplayGate
import com.example.ui.i18n.AppStrings.tr
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianTextPrimary
import com.example.ui.theme.ObsidianTextSecondary
import com.example.ui.theme.SeasonalTheme
import com.example.util.BlockEscalationPolicy

/**
 * #47 (batch 3) Scope B: the WARNING presentation of the block page.
 *
 * When the first block of a section (keyword / adult / custom) lands
 * on a day, the lock is a real lock — same duration, same countdown,
 * same enforcement — but the page shown is this warning variant
 * instead of [LockdownActivity]: it tells the user this first one is
 * a warning and that from the next block on, the regular block page
 * appears directly. The choice is stored WITH the lock
 * (ShieldPreferences.getActiveLockWarningSection), so the
 * accessibility service's relaunches keep presenting this page for
 * the lock's whole life; later blocks that day store no section and
 * get LockdownActivity as before.
 *
 * State discipline mirrors LockdownActivity: everything is read from
 * the stored lock, the page yields to calls / the lock screen (the
 * service re-shows it afterwards), pulls itself back when the user
 * tries to leave mid-lock, and clears the lock when its timer ends.
 */
class BlockWarningActivity : ComponentActivity() {

    private var lockEndTimeState by mutableLongStateOf(0L)
    private var lockTriggerState by mutableStateOf("")
    private var lockSourceAppState by mutableStateOf("")
    private var warningSectionState by mutableStateOf("")
    private var remainingTextState by mutableStateOf("")

    // Polls while resumed: refreshes the countdown, finishes when the
    // stored lock ends (clearing it, like the block screen's own
    // expiry), and stands down for a call / the lock screen.
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
            val remaining = LockdownTimeLogic.remainingMs(
                lockEndTimeState,
                System.currentTimeMillis()
            )
            remainingTextState = LockdownTimeLogic.formatCountdownHhMmSs(remaining)
            pollHandler.postDelayed(this, LockdownTimeLogic.nextDelayMs(remaining))
        }
    }

    private fun shouldYieldToSystemSurface(): Boolean {
        val keyguardLocked = try {
            (getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isKeyguardLocked == true
        } catch (_: Exception) {
            false
        }
        val audioMode = try {
            (getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.mode
                ?: AudioManager.MODE_NORMAL
        } catch (_: Exception) {
            AudioManager.MODE_NORMAL
        }
        return OverlayDisplayGate.decide(
            keyguardLocked = keyguardLocked,
            callActive = OverlayDisplayGate.isCallAudioMode(audioMode)
        ) != OverlayDisplayGate.Decision.SHOW
    }

    private fun refreshStateFromPrefs() {
        val prefs = ShieldApplication.instance.preferences
        lockEndTimeState = prefs.getActiveLockEndTime()
        lockTriggerState = prefs.getActiveLockTrigger()
        lockSourceAppState = prefs.getActiveLockApp()
        warningSectionState = prefs.getActiveLockWarningSection()
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

            MyApplicationTheme(
                darkTheme = isDarkTheme,
                isPersianRtl = isPersian,
                seasonalTheme = SeasonalTheme.fromName(seasonalThemeName)
            ) {
                BlockWarningScreen(
                    sectionName = warningSectionState,
                    triggerWord = lockTriggerState,
                    sourceApp = lockSourceAppState,
                    remainingText = remainingTextState
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
    }
}

@Composable
private fun BlockWarningScreen(
    sectionName: String,
    triggerWord: String,
    sourceApp: String,
    remainingText: String
) {
    // A block presentation: Back does not dismiss it.
    BackHandler { }

    val section = try {
        BlockEscalationPolicy.Section.valueOf(sectionName)
    } catch (e: IllegalArgumentException) {
        null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = when (section) {
                    BlockEscalationPolicy.Section.KEYWORD ->
                        tr("Warning: Blocked Word", "هشدار: کلمه مسدود")
                    BlockEscalationPolicy.Section.ADULT ->
                        tr("Warning: Adult Site", "هشدار: سایت بزرگسال")
                    BlockEscalationPolicy.Section.CUSTOM ->
                        tr("Warning: Custom Block", "هشدار: مسدودی دستی")
                    null -> tr("Warning", "هشدار")
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = CrimsonPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = tr(
                    "This is the first block in this section today, so you get a warning " +
                        "first. From the next block on, the regular block page will " +
                        "appear right away.",
                    "این اولین مسدودی امروز در این بخش است، پس اول هشدار می‌بینی. " +
                        "از مسدودی بعدی به بعد، صفحه‌ی معمولی مسدودی مستقیم نمایش داده می‌شود."
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = ObsidianTextPrimary
            )
            if (triggerWord.isNotBlank()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = tr("Blocked: $triggerWord", "مسدود شد: $triggerWord"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianTextPrimary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (sourceApp.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = sourceApp,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ObsidianTextSecondary
                )
            }
            if (remainingText.isNotBlank()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = tr("Time left: $remainingText", "زمان باقی‌مانده: $remainingText"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianTextPrimary
                )
            }
        }
    }
}
