package com.example.ui.security

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.AppStrings.tr
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianTextPrimary
import com.example.ui.theme.ObsidianTextSecondary

/**
 * Restricted-settings guidance (#25).
 *
 * Since Android 13, an app installed from OUTSIDE Google Play (a
 * sideloaded APK — exactly how X Blocker ships) cannot have its
 * accessibility service enabled until the user first opens App info →
 * ⋮ → "Allow restricted settings". That gate is enforced by Android
 * itself: no app can bypass, pre-approve, or even detect it directly.
 * The only honest thing the app can do is notice that the service is
 * still off and walk the user through the system steps — which is what
 * this dialog does, in both app languages.
 *
 * The step text lives in [RestrictedSettingsGuideContent] as pure data
 * so it is unit-testable without Compose.
 */
object RestrictedSettingsGuideContent {

    val stepsEn: List<String> = listOf(
        "Open Android Settings → Apps → usafe.",
        "Tap the ⋮ menu in the top corner and choose “Allow restricted settings”. Android will ask for your phone PIN or fingerprint.",
        "Go back, then open Settings → Accessibility → Installed apps → usafe.",
        "Turn the service ON and confirm the system warning dialog."
    )

    val stepsFa: List<String> = listOf(
        "تنظیمات اندروید ← برنامه‌ها ← usafe را باز کنید.",
        "روی منوی ⋮ در گوشهٔ بالا بزنید و «Allow restricted settings» (اجازه به تنظیمات محدودشده) را انتخاب کنید. اندروید پین یا اثر انگشت گوشی را می‌خواهد.",
        "برگردید و تنظیمات ← دسترسی‌پذیری ← برنامه‌های نصب‌شده ← usafe را باز کنید.",
        "سرویس را روشن کنید و پیام هشدار سیستم را تأیید کنید."
    )

    fun steps(isFa: Boolean): List<String> = if (isFa) stepsFa else stepsEn

    /**
     * The guide is only useful while the accessibility service is OFF —
     * that is the one moment the restricted-settings wall can be the
     * reason. Once the service runs, the guide has nothing to fix.
     */
    fun shouldOfferGuide(isAccessibilityEnabled: Boolean): Boolean = !isAccessibilityEnabled
}

@Composable
fun RestrictedSettingsGuideDialog(onDismiss: () -> Unit) {
    val steps = RestrictedSettingsGuideContent.steps(AppStrings.isFa)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        modifier = Modifier.testTag("restricted_settings_guide_dialog"),
        title = {
            Text(
                text = tr("Android is blocking the switch", "اندروید جلوی روشن‌کردن را گرفته"),
                color = ObsidianTextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = tr(
                        "Because usafe was installed from outside Google Play, Android 13+ hides the accessibility switch until you allow “restricted settings” once. usafe cannot skip this step — no app can. It takes about 20 seconds:",
                        "چون usafe خارج از گوگل‌پلی نصب شده، اندروید ۱۳ به بالا کلید دسترسی‌پذیری را قفل می‌کند تا یک‌بار «تنظیمات محدودشده» را اجازه دهید. usafe نمی‌تواند این مرحله را رد کند — هیچ برنامه‌ای نمی‌تواند. حدود ۲۰ ثانیه طول می‌کشد:"
                    ),
                    color = ObsidianTextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(10.dp))
                steps.forEachIndexed { index, step ->
                    Text(
                        text = "${index + 1}. $step",
                        color = ObsidianTextPrimary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (index < steps.lastIndex) {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                modifier = Modifier.testTag("restricted_settings_guide_ok")
            ) {
                Text(text = tr("Got it", "فهمیدم"), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = tr("Later", "بعداً"), color = ObsidianTextSecondary)
            }
        }
    )
}
