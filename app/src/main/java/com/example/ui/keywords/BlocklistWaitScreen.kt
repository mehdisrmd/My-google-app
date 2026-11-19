package com.example.ui.keywords

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.i18n.AppStrings.tr
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianTextPrimary
import com.example.ui.theme.ObsidianTextSecondary
import kotlinx.coroutines.delay

/**
 * First-launch gate for the adult domain list (#13): while no list file
 * exists yet, the app shows this "please wait — the site list is being
 * downloaded from the internet" screen instead of the main UI.
 *
 * Failure is NOT a hard lockout (Mehdi's spec asks for the wait, not a
 * brick): when the download fails, the screen offers Retry, or continuing
 * with the small built-in starter list so the app stays usable offline
 * until a real update lands (the weekly worker and the Rules tab's
 * "Update lists now" button keep trying afterwards).
 *
 * #29 (batch 2): while waiting, a «رد شدن» / Skip option appears after
 * 5 seconds. Skipping (like continuing with the starter seed) does NOT
 * count as an update: the app then stays not-fully-activated — see
 * [BlocklistActivationBlockedScreen] — until a real download succeeds.
 */
@Composable
fun BlocklistWaitScreen(
    isFailed: Boolean,
    onRetry: () -> Unit,
    onContinueWithSeed: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    // #29: the Skip option only appears after 5 seconds of waiting, and
    // only in the waiting state (the failed state has its own choices).
    var showSkip by remember { mutableStateOf(false) }
    LaunchedEffect(isFailed) {
        if (!isFailed) {
            delay(SKIP_DELAY_MS)
            showSkip = true
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("blocklist_wait_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 36.dp)
        ) {
            Icon(
                imageVector = if (isFailed) Icons.Default.Warning else Icons.Default.Block,
                contentDescription = null,
                tint = if (isFailed) AmberWarning else CrimsonPrimary,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = tr(
                    "Preparing the site block list",
                    "در حال آماده‌سازی فهرست مسدودی سایت‌ها"
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ObsidianTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (!isFailed) {
                Text(
                    text = tr(
                        "Please wait — the adult site list is being downloaded from the internet.",
                        "لطفاً صبر کنید — فهرست سایت‌های بزرگسال از اینترنت دانلود می‌شود."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ObsidianTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                CircularProgressIndicator(
                    color = CrimsonPrimary,
                    modifier = Modifier.size(40.dp)
                )

                // #29: after 5 seconds of waiting the user may skip — but
                // the list then counts as NOT updated (see the activation
                // screen), so this is a deferral, not a free pass.
                if (showSkip) {
                    Spacer(modifier = Modifier.height(20.dp))

                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.testTag("blocklist_wait_skip_button")
                    ) {
                        Text(
                            text = tr("Skip", "رد شدن"),
                            color = ObsidianTextSecondary
                        )
                    }
                }
            } else {
                Text(
                    text = tr(
                        "The list could not be downloaded. Check your internet connection and try again, or continue with the small built-in starter list for now.",
                        "دانلود فهرست انجام نشد. اتصال اینترنت را بررسی و دوباره تلاش کنید، یا فعلاً با فهرست اولیهٔ داخلی ادامه دهید."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ObsidianTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonPrimary,
                        contentColor = ObsidianTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("blocklist_wait_retry_button")
                ) {
                    Text(
                        text = tr("Retry", "تلاش دوباره"),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onContinueWithSeed,
                    modifier = Modifier.testTag("blocklist_wait_continue_button")
                ) {
                    Text(
                        text = tr(
                            "Continue with the starter list",
                            "ادامه با فهرست اولیه"
                        ),
                        color = ObsidianTextSecondary
                    )
                }
            }
        }
    }
}

/** How long the first-launch wait runs before Skip appears (#29). */
private const val SKIP_DELAY_MS = 5_000L

/**
 * #29 (batch 2): shown instead of the main UI when the first-launch list
 * download was skipped (or replaced by the starter seed). The list
 * counts as NOT updated, so the app is not fully activated: the only way
 * forward is a successful list update, and the button routes straight
 * back into that update flow.
 */
@Composable
fun BlocklistActivationBlockedScreen(
    onUpdateNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("blocklist_activation_blocked_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = AmberWarning,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = tr(
                    "The site list still needs an update",
                    "فهرست سایت‌ها هنوز به به‌روزرسانی نیاز دارد"
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ObsidianTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = tr(
                    "The first download was skipped, so X Blocker is not fully activated yet. Update the site list to finish activation.",
                    "دانلود اول رد شد، پس ایکس بلاکر هنوز کامل فعال نشده است. برای کامل شدن فعال‌سازی، فهرست سایت‌ها را به‌روزرسانی کنید."
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = ObsidianTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onUpdateNow,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrimsonPrimary,
                    contentColor = ObsidianTextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("blocklist_activation_update_button")
            ) {
                Text(
                    text = tr("Update the list now", "همین حالا فهرست را به‌روزرسانی کن"),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
