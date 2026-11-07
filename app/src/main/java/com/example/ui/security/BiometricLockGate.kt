package com.example.ui.security

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.ShieldPreferences
import com.example.ui.i18n.AppStrings.tr
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.CyanTelemetry
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianTextPrimary
import com.example.ui.theme.ObsidianTextSecondary

/**
 * Full-screen Biometric Lodge & PIN Security Gate that blocks access to X Blocker
 * until the user authenticates via OS Biometric / Device Credential or their Security PIN.
 * Uses a custom built-in PIN keypad without any DEL or CTRL buttons.
 */
@Composable
fun BiometricLockOverlay(
    preferences: ShieldPreferences,
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val deviceCredentialLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            onUnlocked()
        } else {
            errorText = tr(
                "Biometric / Device authentication cancelled. Use PIN pad below or retry.",
                "احراز هویت لغو شد. رمز پین را وارد کنید یا دوباره تلاش کنید."
            )
        }
    }

    val triggerBiometricOrDeviceCredential = {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        @Suppress("DEPRECATION")
        val intent: Intent? = keyguardManager?.createConfirmDeviceCredentialIntent(
            tr("X Blocker Biometric Lock", "قفل بیومتریک ایکس بلاکر"),
            tr("Authenticate with Fingerprint, Face, or Device PIN to unlock X Blocker", "برای باز کردن ایکس بلاکر با اثر انگشت یا رمز دستگاه احراز هویت کنید")
        )
        if (intent != null) {
            deviceCredentialLauncher.launch(intent)
        } else {
            onUnlocked()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(20.dp)
            .testTag("biometric_lodge_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, CyanTelemetry.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(CyanTelemetry.copy(alpha = 0.15f))
                        .border(2.dp, CyanTelemetry, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometric Lock",
                        tint = CyanTelemetry,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = tr("Biometric Security Gate", "درگاه امنیتی بیومتریک"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = ObsidianTextPrimary
                )

                Text(
                    text = tr(
                        "Authenticate with Fingerprint / Biometrics or enter your Security PIN (Default: 0000).",
                        "با اثر انگشت یا وارد کردن رمز پین (پیش‌فرض: 0000) قفل برنامه را باز کنید."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianTextSecondary,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = triggerBiometricOrDeviceCredential,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanTelemetry,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("biometric_authenticate_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tr("Unlock with Biometrics / Device Auth", "بازگشایی با اثر انگشت / قفل گوشی"),
                        fontWeight = FontWeight.Bold
                    )
                }

                // Masked PIN Display Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, if (errorText != null) CrimsonPrimary else CyanTelemetry.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("biometric_pin_input")
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val masked = if (pinInput.isEmpty()) {
                            tr("Tap numbers below (PIN)", "اعداد پین را لمس کنید")
                        } else {
                            "● ".repeat(pinInput.length).trim()
                        }
                        Text(
                            text = masked,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (pinInput.isEmpty()) ObsidianTextSecondary else CyanTelemetry
                        )
                    }
                }

                // Clean In-App Numeric Keypad (Digits 0-9 + Clear + OK — NO DEL and NO CTRL buttons)
                CleanNumericKeypad(
                    onDigitClick = { digit ->
                        if (pinInput.length < 6) {
                            pinInput += digit
                            errorText = null
                        }
                    },
                    onClearAll = {
                        pinInput = ""
                        errorText = null
                    },
                    onConfirm = {
                        if (preferences.verifySecurityPin(pinInput)) {
                            onUnlocked()
                        } else {
                            errorText = tr("Incorrect Security PIN. Try again.", "رمز پین اشتباه است. دوباره تلاش کنید.")
                        }
                    }
                )

                errorText?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.labelSmall,
                        color = CrimsonPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        if (preferences.verifySecurityPin(pinInput)) {
                            onUnlocked()
                        } else {
                            errorText = tr("Incorrect Security PIN. Try again.", "رمز پین اشتباه است. دوباره تلاش کنید.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldShield,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("biometric_pin_unlock_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tr("Unlock with Security PIN", "بازگشایی با رمز پین"), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Custom In-App Keyboard / Keypad without any DEL or CTRL buttons.
 */
@Composable
fun CleanNumericKeypad(
    onDigitClick: (String) -> Unit,
    onClearAll: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("CLR", "0", "OK")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("clean_numeric_keypad"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowKeys.forEach { key ->
                    val isAction = key == "CLR" || key == "OK"
                    Surface(
                        onClick = {
                            when (key) {
                                "CLR" -> onClearAll()
                                "OK" -> onConfirm()
                                else -> onDigitClick(key)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = when (key) {
                            "OK" -> EmeraldShield.copy(alpha = 0.22f)
                            "CLR" -> IndigoAccent.copy(alpha = 0.18f)
                            else -> ObsidianCard
                        },
                        border = BorderStroke(
                            1.dp,
                            when (key) {
                                "OK" -> EmeraldShield
                                "CLR" -> IndigoAccent
                                else -> ObsidianBorder
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("keypad_btn_${key.lowercase()}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = when (key) {
                                    "CLR" -> tr("CLEAR", "پاک")
                                    "OK" -> tr("OK", "تایید")
                                    else -> key
                                },
                                style = if (isAction) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Black,
                                color = when (key) {
                                    "OK" -> EmeraldShield
                                    "CLR" -> IndigoAccent
                                    else -> ObsidianTextPrimary
                                },
                                fontSize = if (isAction) 11.sp else 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full-screen solid black curtain rendered when the app enters background / Recents
 * so no external screen-reader or AI agent can inspect the preview frame.
 */
@Composable
fun BlackoutPrivacyCurtain() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("anti_agent_blackout_curtain"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.VisibilityOff,
                contentDescription = "Black Screen Privacy Active",
                tint = Color(0xFF1E293B),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "X BLOCKER • BLACKOUT PRIVACY SHIELD",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMonoFontFamily,
                color = Color(0xFF1E293B)
            )
        }
    }
}

/**
 * Dashboard Card for Biometric Lodge & Simplified Anti-Agent Black Screen Shield (FLAG_SECURE).
 */
@Composable
fun BiometricAndPrivacyDashboardSection(
    preferences: ShieldPreferences,
    isBiometricLockEnabled: Boolean,
    isBlackoutPrivacyShieldEnabled: Boolean,
    onToggleBiometricLock: (Boolean) -> Unit,
    onLockAppNow: () -> Unit,
    onToggleBlackoutShield: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showChangePinDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Simplified Anti-Agent Screenshot Block (Single ON/OFF Button + Clear Explanation)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("blackout_privacy_shield_card"),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(
                1.dp,
                if (isBlackoutPrivacyShieldEnabled) EmeraldShield.copy(alpha = 0.5f) else ObsidianBorder
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isBlackoutPrivacyShieldEnabled) EmeraldShield.copy(alpha = 0.18f)
                                else ObsidianCard
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = if (isBlackoutPrivacyShieldEnabled) EmeraldShield else ObsidianTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = tr("Anti-Agent & Screenshot Blocker", "ضد اسکرین‌شات و مخفی‌ساز صفحه"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tr(
                                "Turn ON so AI agents, screen recorders, and screenshots only see a black screen (FLAG_SECURE). Turn OFF to allow normal screen viewing and screenshots.",
                                "با روشن کردن این گزینه، هوش مصنوعی، ضبط‌کننده‌های صفحه و اسکرین‌شات فقط یک صفحه سیاه می‌بینند. برای مشاهده عادی و گرفتن اسکرین‌شات آن را خاموش بگذارید."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = ObsidianTextSecondary
                        )
                    }
                }

                Switch(
                    checked = isBlackoutPrivacyShieldEnabled,
                    onCheckedChange = onToggleBlackoutShield,
                    modifier = Modifier.testTag("blackout_privacy_shield_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = EmeraldShield
                    )
                )
            }
        }

        // 2. Biometric Lodge (App Access Gate) Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("biometric_lodge_card"),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(
                1.dp,
                if (isBiometricLockEnabled) CyanTelemetry.copy(alpha = 0.5f) else ObsidianBorder
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyanTelemetry.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = CyanTelemetry,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = tr("Biometric Lock Gate", "قفل بیومتریک برنامه"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianTextPrimary
                            )
                            Text(
                                text = tr(
                                    "Require Fingerprint / Biometrics or PIN to open X Blocker",
                                    "نیاز به اثر انگشت یا رمز پین برای ورود به ایکس بلاکر"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = ObsidianTextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = isBiometricLockEnabled,
                        onCheckedChange = onToggleBiometricLock,
                        modifier = Modifier.testTag("biometric_lodge_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = CyanTelemetry
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showChangePinDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("biometric_change_pin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = CyanTelemetry,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr("Set PIN (${preferences.getSecurityPinCode()})", "تنظیم پین (${preferences.getSecurityPinCode()})"),
                            style = MaterialTheme.typography.labelMedium,
                            color = ObsidianTextPrimary
                        )
                    }

                    Button(
                        onClick = onLockAppNow,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IndigoAccent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("biometric_lock_now_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr("Lock App Now", "قفل فوری برنامه"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showChangePinDialog) {
        var newPin by remember { mutableStateOf(preferences.getSecurityPinCode()) }
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            containerColor = ObsidianSurface,
            title = {
                Text(
                    text = tr("Update Biometric PIN", "تغییر رمز پین بیومتریک"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = tr("Use the keypad below to set a 4 to 6 digit Security PIN:", "یک رمز پین ۴ تا ۶ رقمی با صفحه کلید زیر تعیین کنید:"),
                        style = MaterialTheme.typography.bodySmall,
                        color = ObsidianTextSecondary
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianCard,
                        border = BorderStroke(1.dp, CyanTelemetry),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = newPin.ifEmpty { "----" },
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Black,
                            color = CyanTelemetry,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    CleanNumericKeypad(
                        onDigitClick = { d ->
                            if (newPin.length < 6) newPin += d
                        },
                        onClearAll = { newPin = "" },
                        onConfirm = {
                            if (newPin.length >= 4) {
                                preferences.setSecurityPinCode(newPin)
                                showChangePinDialog = false
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPin.length >= 4) {
                            preferences.setSecurityPinCode(newPin)
                            showChangePinDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldShield)
                ) {
                    Text(tr("Save PIN", "ذخیره پین"), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text(tr("Cancel", "انصراف"), color = ObsidianTextSecondary)
                }
            }
        )
    }
}
