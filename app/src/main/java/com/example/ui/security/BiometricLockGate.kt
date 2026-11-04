package com.example.ui.security

import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.preferences.ShieldPreferences
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.CyanTelemetry
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianTextSecondary
import kotlinx.coroutines.delay

object BiometricAuthHelper {

    fun isHardwareBiometricAvailable(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val bm = context.getSystemService(BiometricManager::class.java)
                val res = bm?.canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                res == BiometricManager.BIOMETRIC_SUCCESS
            } else {
                val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                km?.isKeyguardSecure == true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun triggerSystemBiometricPrompt(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val executor = ContextCompat.getMainExecutor(context)
                val builder = BiometricPrompt.Builder(context)
                    .setTitle("ShieldBlock Biometric Lodge")
                    .setSubtitle("Verify your identity to unlock ShieldBlock controls")
                    .setDescription("Use fingerprint, face unlock, or enter your 4-digit Shield PIN below.")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    builder.setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                } else {
                    builder.setNegativeButton(
                        "Use Shield PIN",
                        executor
                    ) { _, _ -> }
                }

                val prompt = builder.build()
                val cancellationSignal = CancellationSignal()
                prompt.authenticate(
                    cancellationSignal,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                            super.onAuthenticationSucceeded(result)
                            onSuccess()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            super.onAuthenticationError(errorCode, errString)
                            onError(errString?.toString() ?: "Use your 4-digit Shield PIN below")
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            onError("Biometric not recognized. Try again or use PIN.")
                        }
                    }
                )
            } catch (e: Exception) {
                onError("Biometric sensor unavailable in this environment. Use your 4-digit PIN below.")
            }
        } else {
            onError("Use your 4-digit Shield PIN to unlock.")
        }
    }
}

@Composable
fun BiometricLockOverlay(
    preferences: ShieldPreferences,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var statusMessage by remember {
        mutableStateOf("Verify with Biometrics or enter your 4-digit Shield PIN")
    }
    var isError by remember { mutableStateOf(false) }

    // Attempt system biometric prompt automatically when overlay appears if hardware is enrolled
    LaunchedEffect(Unit) {
        if (BiometricAuthHelper.isHardwareBiometricAvailable(context)) {
            BiometricAuthHelper.triggerSystemBiometricPrompt(
                context = context,
                onSuccess = onUnlocked,
                onError = { msg ->
                    statusMessage = msg
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(24.dp)
            .testTag("biometric_lock_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.5.dp, EmeraldShield.copy(alpha = 0.55f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    EmeraldShield.copy(alpha = 0.3f),
                                    CyanTelemetry.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .border(2.dp, EmeraldShield, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometric Lodge",
                        tint = EmeraldShield,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Biometric Lodge Active",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isError) CrimsonPrimary else ObsidianTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // PIN Dots Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val targetLength = preferences.getSecurityPinCode().length.coerceAtLeast(4)
                    repeat(targetLength) { index ->
                        val filled = index < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    if (filled) EmeraldShield else ObsidianCard
                                )
                                .border(
                                    1.5.dp,
                                    if (filled) EmeraldShield else ObsidianBorder,
                                    CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Numeric Keypad
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("CLR", "0", "DEL")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    rows.forEach { rowKeys ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowKeys.forEach { key ->
                                Surface(
                                    onClick = {
                                        isError = false
                                        when (key) {
                                            "CLR" -> enteredPin = ""
                                            "DEL" -> {
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                }
                                            }
                                            else -> {
                                                val maxLen = preferences.getSecurityPinCode().length.coerceAtLeast(4)
                                                if (enteredPin.length < maxLen) {
                                                    enteredPin += key
                                                    if (enteredPin.length == maxLen) {
                                                        if (preferences.verifySecurityPin(enteredPin)) {
                                                            onUnlocked()
                                                        } else {
                                                            isError = true
                                                            statusMessage = "Incorrect PIN. Default is 0000 unless changed."
                                                            enteredPin = ""
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(width = 76.dp, height = 52.dp)
                                        .testTag("biometric_pin_key_$key"),
                                    shape = RoundedCornerShape(14.dp),
                                    color = ObsidianCard,
                                    border = BorderStroke(1.dp, ObsidianBorder)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = if (key == "CLR" || key == "DEL") AmberWarning else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        BiometricAuthHelper.triggerSystemBiometricPrompt(
                            context = context,
                            onSuccess = onUnlocked,
                            onError = { msg ->
                                statusMessage = msg
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trigger_biometric_prompt_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldShield,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Scan Biometrics (Fingerprint / Face)",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BiometricAndPrivacyDashboardSection(
    preferences: ShieldPreferences,
    isBiometricLockEnabled: Boolean,
    isBlackoutPrivacyShieldEnabled: Boolean,
    screenshotTempUnlockUntil: Long,
    onToggleBiometricLock: (Boolean) -> Unit,
    onLockAppNow: () -> Unit,
    onToggleBlackoutShield: (Boolean) -> Unit,
    onAllowScreenshot60s: () -> Unit
) {
    var showChangePinDialog by remember { mutableStateOf(false) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(screenshotTempUnlockUntil) {
        while (screenshotTempUnlockUntil > System.currentTimeMillis()) {
            nowMs = System.currentTimeMillis()
            delay(1000L)
        }
        nowMs = System.currentTimeMillis()
    }

    val isTempScreenshotWindowOpen = screenshotTempUnlockUntil > nowMs
    val remainingScreenshotSec = ((screenshotTempUnlockUntil - nowMs) / 1000L).coerceAtLeast(0L)
    val isFlagSecureActive = isBlackoutPrivacyShieldEnabled && !isTempScreenshotWindowOpen

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Card 1: Biometric Lodge (App & Settings Lock)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("biometric_lodge_card"),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(
                1.dp,
                if (isBiometricLockEnabled) EmeraldShield.copy(alpha = 0.5f) else ObsidianBorder
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isBiometricLockEnabled) EmeraldShield.copy(alpha = 0.2f)
                                    else IndigoAccent.copy(alpha = 0.18f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Biometric Lodge",
                                tint = if (isBiometricLockEnabled) EmeraldShield else IndigoAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Biometric Lodge",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isBiometricLockEnabled) EmeraldShield.copy(alpha = 0.18f)
                                    else ObsidianCard
                                ) {
                                    Text(
                                        text = if (isBiometricLockEnabled) "PROTECTED" else "OFF",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = if (isBiometricLockEnabled) EmeraldShield else ObsidianTextSecondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Require Fingerprint / Face or Shield PIN to enter app & edit limits",
                                style = MaterialTheme.typography.bodySmall,
                                color = ObsidianTextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = isBiometricLockEnabled,
                        onCheckedChange = onToggleBiometricLock,
                        modifier = Modifier.testTag("biometric_lock_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = EmeraldShield
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showChangePinDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("configure_biometric_pin_button"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = CyanTelemetry,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Set PIN (${preferences.getSecurityPinCode().map { '*' }.joinToString("")})",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = onLockAppNow,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("lock_app_now_button"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, EmeraldShield.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = EmeraldShield,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Lock Lodge Now",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldShield,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Card 2: Anti-Agent Blackout Shield (FLAG_SECURE) & Personal Screenshot Control
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("blackout_privacy_shield_card"),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(
                1.dp,
                if (isFlagSecureActive) CyanTelemetry.copy(alpha = 0.5f) else AmberWarning.copy(alpha = 0.6f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isFlagSecureActive) CyanTelemetry.copy(alpha = 0.18f)
                                    else AmberWarning.copy(alpha = 0.18f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isFlagSecureActive) Icons.Default.VisibilityOff else Icons.Default.CameraAlt,
                                contentDescription = "Anti-Agent Blackout Shield",
                                tint = if (isFlagSecureActive) CyanTelemetry else AmberWarning,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Anti-Agent Black Screen Shield",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = if (isFlagSecureActive) {
                                    "BLACKOUT ACTIVE (FLAG_SECURE): External agents, AI screen readers & recorders see a 100% BLACK SCREEN."
                                } else if (isTempScreenshotWindowOpen) {
                                    "Personal Screenshot Window Open (${remainingScreenshotSec}s left) — Auto-relocks to Blackout Shield."
                                } else {
                                    "Personal Screenshot Mode ON — Background curtain still blacks out Recent Apps."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isFlagSecureActive) ObsidianTextSecondary else AmberWarning
                            )
                        }
                    }

                    Switch(
                        checked = isBlackoutPrivacyShieldEnabled,
                        onCheckedChange = onToggleBlackoutShield,
                        modifier = Modifier.testTag("blackout_privacy_shield_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = CyanTelemetry
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Status & Personal Screenshot Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ObsidianCard,
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = if (isFlagSecureActive) EmeraldShield else AmberWarning,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFlagSecureActive) {
                                    "Agents see: PURE BLACK"
                                } else {
                                    "Screenshots: ALLOWED"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = if (isFlagSecureActive) EmeraldShield else AmberWarning,
                                maxLines = 1
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onAllowScreenshot60s,
                        modifier = Modifier.testTag("allow_screenshot_60s_button"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isTempScreenshotWindowOpen) AmberWarning else CyanTelemetry.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = if (isTempScreenshotWindowOpen) AmberWarning else CyanTelemetry,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isTempScreenshotWindowOpen) {
                                "Screenshot (${remainingScreenshotSec}s)"
                            } else {
                                "Take Screenshot (60s)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    if (showChangePinDialog) {
        ChangeSecurityPinDialog(
            currentPin = preferences.getSecurityPinCode(),
            onDismiss = { showChangePinDialog = false },
            onSavePin = { newPin ->
                preferences.setSecurityPinCode(newPin)
                showChangePinDialog = false
            }
        )
    }
}

@Composable
private fun ChangeSecurityPinDialog(
    currentPin: String,
    onDismiss: () -> Unit,
    onSavePin: (String) -> Unit
) {
    var newPin by remember { mutableStateOf(currentPin) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Text(
                text = "Configure Biometric Lodge PIN",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Set a 4-digit security PIN used alongside Fingerprint/Face authentication to unlock ShieldBlock:",
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianTextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { input ->
                        newPin = input.filter { it.isDigit() }.take(4)
                    },
                    label = { Text("4-Digit PIN Code") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("security_pin_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldShield,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPin.length == 4) {
                        onSavePin(newPin)
                    }
                },
                enabled = newPin.length == 4,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldShield,
                    contentColor = Color.Black
                ),
                modifier = Modifier.testTag("save_security_pin_button")
            ) {
                Text("Save PIN", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ObsidianTextSecondary)
            }
        }
    )
}

@Composable
fun BlackoutPrivacyCurtain() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("blackout_privacy_curtain"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Privacy Blackout Shield",
                tint = EmeraldShield.copy(alpha = 0.4f),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "SHIELDBLOCK PRIVACY BLACKOUT",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}
