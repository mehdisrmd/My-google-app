package com.example.ui.security

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ShieldApplication
import com.example.service.InstallGuardLogic
import com.example.service.OverlayDisplayGate
import com.example.ui.i18n.AppStrings.tr
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.CyanTelemetry
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianTextPrimary
import com.example.ui.theme.ObsidianTextSecondary
import com.example.ui.theme.SeasonalTheme
import com.example.util.PinSecurity
import kotlinx.coroutines.delay

/**
 * Install Guard PIN pad (#27).
 *
 * Launched by the accessibility service when an app install is attempted
 * while the Install Guard is ON. The user has 10 seconds (the deadline
 * lives in preferences — see [InstallGuardLogic]) to prove the app PIN:
 *  - correct PIN in time → a one-shot allowance is stored and this
 *    screen closes with a hint to re-tap Install within 2 minutes;
 *  - timeout / leaving / wrong handling → nothing is stored, so the
 *    install stays blocked.
 *
 * Like the block screen (G6), this overlay respects [OverlayDisplayGate]:
 * it never shows over the lock screen and it finishes itself if a call
 * takes over or the phone locks while it is up — in those cases no
 * allowance is granted (fail-closed).
 */
class InstallGuardActivity : ComponentActivity() {

    // #17/#18 yield check, same pattern as LockdownActivity: poll while
    // resumed and finish if a call takes over or the phone locks.
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
        val prefs = ShieldApplication.instance.preferences
        if (prefs.isCurrentlyLocked()) return true
        val keyguardLocked = try {
            (getSystemService(KEYGUARD_SERVICE) as? android.app.KeyguardManager)
                ?.isKeyguardLocked == true
        } catch (_: Exception) {
            false
        }
        val audioMode = try {
            (getSystemService(AUDIO_SERVICE) as? android.media.AudioManager)?.mode
                ?: android.media.AudioManager.MODE_NORMAL
        } catch (_: Exception) {
            android.media.AudioManager.MODE_NORMAL
        }
        return OverlayDisplayGate.decide(
            keyguardLocked = keyguardLocked,
            callActive = OverlayDisplayGate.isCallAudioMode(audioMode)
        ) != OverlayDisplayGate.Decision.SHOW
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            // #18: never draw over the lock screen.
            setShowWhenLocked(false)
            setTurnScreenOn(true)
        }

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
                InstallGuardPrompt(
                    preferences = prefs,
                    onConfirmed = {
                        Toast.makeText(
                            this@InstallGuardActivity,
                            if (prefs.getIsPersianLanguage()) {
                                "✓ تأیید شد — ظرف ۲ دقیقه دوباره روی «نصب» بزنید"
                            } else {
                                "✓ Confirmed — tap Install again within 2 minutes"
                            },
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    },
                    onClosed = { finish() }
                )
            }
        }
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

    private companion object {
        const val YIELD_CHECK_INTERVAL_MS = 1000L
    }
}

@Composable
private fun InstallGuardPrompt(
    preferences: com.example.data.preferences.ShieldPreferences,
    onConfirmed: () -> Unit,
    onClosed: () -> Unit
) {
    val pinIsSet = remember { preferences.isPinSet() }
    var pinInput by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var remainingMs by remember {
        mutableLongStateOf(
            InstallGuardLogic.promptRemainingMs(
                preferences.getInstallGuardState(),
                System.currentTimeMillis()
            )
        )
    }

    // Countdown: when the 10-second window lapses unconfirmed, clear the
    // prompt state and close — the install stays blocked.
    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            val state = preferences.getInstallGuardState()
            remainingMs = InstallGuardLogic.promptRemainingMs(state, now)
            if (remainingMs <= 0L) {
                preferences.saveInstallGuardState(
                    InstallGuardLogic.onPromptClosed(state, now)
                )
                onClosed()
                return@LaunchedEffect
            }
            delay(200L)
        }
    }

    val remainingSec = (remainingMs + 999L) / 1000L

    fun confirm() {
        val now = System.currentTimeMillis()
        val state = preferences.getInstallGuardState()
        if (!preferences.verifySecurityPin(pinInput)) {
            errorText = tr("Wrong PIN — try again", "پین اشتباه است — دوباره تلاش کنید")
            pinInput = ""
            return
        }
        val (newState, granted) = InstallGuardLogic.onPinConfirmed(state, now)
        preferences.saveInstallGuardState(newState)
        if (granted) {
            onConfirmed()
        } else {
            // The window expired while typing: a correct-but-late PIN
            // grants nothing.
            onClosed()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = tr("Install Confirmation", "تأیید نصب برنامه"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = ObsidianTextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = tr(
                    "Install Guard is ON: a new app is trying to install. " +
                        "Enter your PIN within $remainingSec seconds to allow this one install.",
                    "محافظ نصب فعال است: یک برنامهٔ جدید می‌خواهد نصب شود. " +
                        "برای اجازه دادن به این یک نصب، پین را ظرف $remainingSec ثانیه وارد کنید."
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = ObsidianTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("install_guard_countdown")
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (!pinIsSet) {
                Text(
                    text = tr(
                        "No PIN is set yet. Open usafe and set a PIN first — installs stay blocked until then.",
                        "هنوز پینی تنظیم نشده است. اول در usafe پین تنظیم کنید — تا آن موقع نصب‌ها مسدود می‌مانند."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CrimsonPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onClosed,
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    modifier = Modifier.testTag("install_guard_close_button")
                ) {
                    Text(tr("Close", "بستن"))
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ObsidianCard,
                    border = BorderStroke(
                        1.dp,
                        if (errorText != null) CrimsonPrimary else CyanTelemetry.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (pinInput.isEmpty()) {
                                tr("Tap numbers below (PIN)", "اعداد پین را لمس کنید")
                            } else {
                                "● ".repeat(pinInput.length).trim()
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (pinInput.isEmpty()) ObsidianTextSecondary else CyanTelemetry
                        )
                    }
                }
                if (errorText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = CrimsonPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("install_guard_error")
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                CleanNumericKeypad(
                    onDigitClick = { digit ->
                        if (pinInput.length < PinSecurity.MAX_PIN_LENGTH) {
                            pinInput += digit
                            errorText = null
                        }
                    },
                    onClearAll = { pinInput = "" },
                    onConfirm = { confirm() }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = tr(
                        "No confirmation = the install stays blocked.",
                        "بدون تأیید = نصب مسدود می‌ماند."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldShield,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
