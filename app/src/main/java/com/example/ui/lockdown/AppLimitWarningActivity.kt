package com.example.ui.lockdown

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
 * #47 (batch 3) Scope A: the daily app-limit WARNING page.
 *
 * Shown for the first two opens of an app whose daily budget is spent
 * (see BlockEscalationPolicy): the accessibility service has already
 * closed the app (BACK + HOME) and puts this page over the launcher
 * instead of starting a lockdown. It is a plain warning — no lock
 * state, no timer — and the user dismisses it with the button or
 * Back. From the third open on, the service starts the until-midnight
 * timer block and this page never appears for that app that day.
 *
 * All of its state rides in the launch intent: app name + which
 * warning this is (1 or 2 of 2).
 */
class AppLimitWarningActivity : ComponentActivity() {

    private var appNameState by mutableStateOf("")
    private var warningNumberState by mutableIntStateOf(1)

    // While resumed, stand down if a call takes over or the phone
    // locks (same yield rule as the other overlay pages). Unlike the
    // block screen there is nothing to re-show afterwards: the warning
    // was delivered, the app is closed, the open was counted.
    private val yieldCheckHandler = Handler(Looper.getMainLooper())
    private val yieldCheckRunnable = object : Runnable {
        override fun run() {
            if (shouldYieldToSystemSurface()) {
                finish()
                return
            }
            yieldCheckHandler.postDelayed(this, YIELD_CHECK_INTERVAL_MS)
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

    private fun refreshStateFromIntent(incoming: Intent?) {
        appNameState = incoming?.getStringExtra(EXTRA_APP_NAME)?.takeIf { it.isNotBlank() }
            ?: appNameState
        val number = incoming?.getIntExtra(EXTRA_WARNING_NUMBER, 0) ?: 0
        if (number > 0) warningNumberState = number
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLockscreenFlags()
        refreshStateFromIntent(intent)

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
                AppLimitWarningScreen(
                    appName = appNameState,
                    warningNumber = warningNumberState,
                    onDismiss = { finish() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        refreshStateFromIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        yieldCheckHandler.removeCallbacks(yieldCheckRunnable)
        yieldCheckHandler.postDelayed(yieldCheckRunnable, YIELD_CHECK_INTERVAL_MS)
    }

    override fun onPause() {
        yieldCheckHandler.removeCallbacks(yieldCheckRunnable)
        super.onPause()
    }

    private fun configureLockscreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            // Like the block screen (#18): never draw over the lock
            // screen.
            setShowWhenLocked(false)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        private const val YIELD_CHECK_INTERVAL_MS = 1000L

        const val EXTRA_APP_NAME = "extra_app_limit_warning_app_name"
        const val EXTRA_WARNING_NUMBER = "extra_app_limit_warning_number"
    }
}

@Composable
private fun AppLimitWarningScreen(
    appName: String,
    warningNumber: Int,
    onDismiss: () -> Unit
) {
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
                text = tr("Daily Limit Reached", "سقف امروز این برنامه پر شده"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = ObsidianTextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = tr(
                    "You've reached today's limit for $appName. This is warning " +
                        "$warningNumber of ${BlockEscalationPolicy.APP_LIMIT_WARNING_OPENS} — " +
                        "next time, the block timer starts.",
                    "به سقف امروز $appName رسیدی. این هشدار $warningNumber از " +
                        "${BlockEscalationPolicy.APP_LIMIT_WARNING_OPENS} است؛ دفعه‌ی بعد تایمر مسدودی شروع می‌شود."
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = ObsidianTextSecondary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = tr("Got it", "فهمیدم"))
            }
        }
    }
}
