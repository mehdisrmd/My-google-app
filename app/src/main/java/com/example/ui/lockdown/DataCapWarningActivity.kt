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
import android.provider.Settings
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
import androidx.compose.foundation.layout.Row
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
import com.example.util.DataCapEnforcer
import com.example.util.TransportStateReader

/**
 * #46 (batch 3): the daily data-cap warning page.
 *
 * This is NOT a lockdown: it has no timer and no lock state. Its state
 * is the data-cap warning set in preferences — the transports whose
 * daily cap was reached and that are still on. The page persists while
 * any of them is on (the accessibility service relaunches it, and this
 * activity pulls itself back), and it finishes itself the moment the
 * set empties, which happens as the service observes each transport
 * switch off — by the app's own background attempts or by the user.
 *
 * The one deliberate exit is the trip to the system Settings: the
 * buttons below open the transport's Settings screen (recording a
 * short trip window in preferences) so the user can win the race by
 * hand without the page fighting them.
 */
class DataCapWarningActivity : ComponentActivity() {

    private var warningTransportsState by mutableStateOf<Set<DataCapEnforcer.Transport>>(emptySet())
    private var transportStatesState by mutableStateOf<Map<DataCapEnforcer.Transport, Boolean?>>(emptyMap())

    // Polls once a second while resumed: yields to a call / the lock
    // screen like the block screen does, refreshes the rows' on/off
    // states, and finishes when the warning set has emptied.
    private val pollHandler = Handler(Looper.getMainLooper())
    private val pollRunnable = object : Runnable {
        override fun run() {
            if (shouldYieldToSystemSurface()) {
                finish()
                return
            }
            refreshState()
            if (warningTransportsState.isEmpty()) {
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

    private fun refreshState() {
        val prefs = ShieldApplication.instance.preferences
        val transports = prefs.getDataCapWarningTransports().mapNotNull { name ->
            try {
                DataCapEnforcer.Transport.valueOf(name)
            } catch (e: IllegalArgumentException) {
                null
            }
        }.toSet()
        warningTransportsState = transports
        transportStatesState = transports.associateWith { transport ->
            TransportStateReader.isOn(this, transport)
        }
    }

    private fun hasActiveSettingsTrip(): Boolean {
        val prefs = ShieldApplication.instance.preferences
        return prefs.getDataCapSettingsTripUntil() > System.currentTimeMillis()
    }

    private fun openTransportSettings(transport: DataCapEnforcer.Transport) {
        // Record the trip first: while it is active, neither this
        // activity's pull-back nor the service's relaunch may fight
        // the user for the Settings screen.
        val prefs = ShieldApplication.instance.preferences
        prefs.setDataCapSettingsTripUntil(
            System.currentTimeMillis() + SETTINGS_TRIP_WINDOW_MS
        )
        val action = when (transport) {
            DataCapEnforcer.Transport.WIFI -> Settings.ACTION_WIFI_SETTINGS
            DataCapEnforcer.Transport.MOBILE -> Settings.ACTION_DATA_ROAMING_SETTINGS
        }
        try {
            startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Log.w(TAG, "Could not open Settings for $transport", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLockscreenFlags()
        refreshState()

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
                DataCapWarningScreen(
                    transports = warningTransportsState,
                    states = transportStatesState,
                    onOpenSettings = { transport -> openTransportSettings(transport) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshState()
        if (warningTransportsState.isEmpty()) {
            finish()
            return
        }
        pollHandler.removeCallbacks(pollRunnable)
        pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS)
    }

    override fun onPause() {
        pollHandler.removeCallbacks(pollRunnable)
        super.onPause()
        // Pull back when the user tries to leave mid-warning — except
        // on a deliberate Settings trip, which is the page's own exit.
        // (As in LockdownActivity, this is best-effort: the real
        // enforcement is the accessibility service's relaunch.)
        if (warningTransportsState.isNotEmpty() && !hasActiveSettingsTrip()) {
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
        if (warningTransportsState.isNotEmpty() && !hasActiveSettingsTrip()) {
            val relaunch = Intent(this, DataCapWarningActivity::class.java).apply {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            // Like the block screen (#18): never draw over the lock
            // screen; the service relaunches after unlock.
            setShowWhenLocked(false)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        private const val TAG = "DataCapWarning"
        private const val POLL_INTERVAL_MS = 1000L
        private const val SETTINGS_TRIP_WINDOW_MS = 90_000L
    }
}

@Composable
private fun DataCapWarningScreen(
    transports: Set<DataCapEnforcer.Transport>,
    states: Map<DataCapEnforcer.Transport, Boolean?>,
    onOpenSettings: (DataCapEnforcer.Transport) -> Unit
) {
    // The warning does not dismiss while a warned transport is on.
    BackHandler { }

    // Stable display order: mobile data first, then Wi-Fi.
    val ordered = listOf(DataCapEnforcer.Transport.MOBILE, DataCapEnforcer.Transport.WIFI)
        .filter { it in transports }

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
                text = tr("Daily Data Limit Reached", "سقف داده روزانه پر شد"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = ObsidianTextPrimary
            )
            Spacer(modifier = Modifier.height(20.dp))
            for (transport in ordered) {
                val isOn = states[transport]
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = when (transport) {
                            DataCapEnforcer.Transport.MOBILE -> tr("Mobile data", "داده همراه")
                            DataCapEnforcer.Transport.WIFI -> tr("Wi-Fi", "وای‌فای")
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (isOn == false) tr("Off", "خاموش") else tr("On", "روشن"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isOn == false) ObsidianTextSecondary else CrimsonPrimary
                    )
                }
                if (isOn != false) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (transport) {
                            DataCapEnforcer.Transport.MOBILE -> tr(
                                "Daily data limit reached — Mobile data is still on. Turn off mobile data to continue.",
                                "سقف داده روزانه پر شد — داده همراه هنوز روشنه. برای ادامه، داده همراه رو خاموش کن."
                            )
                            DataCapEnforcer.Transport.WIFI -> tr(
                                "Daily data limit reached — Wi-Fi is still on. Turn off Wi-Fi to continue.",
                                "سقف داده روزانه پر شد — وای‌فای هنوز روشنه. برای ادامه، وای‌فای رو خاموش کن."
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = ObsidianTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onOpenSettings(transport) },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = when (transport) {
                                DataCapEnforcer.Transport.MOBILE ->
                                    tr("Open mobile data settings", "باز کردن تنظیمات داده همراه")
                                DataCapEnforcer.Transport.WIFI ->
                                    tr("Open Wi-Fi settings", "باز کردن تنظیمات وای‌فای")
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
