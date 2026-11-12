package com.example.ui.lockdown

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import android.telecom.TelecomManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ShieldApplication
import com.example.ui.components.MotivationalQuoteBannerCard
import com.example.ui.components.TacticalBreathingStudioCard
import com.example.ui.i18n.AppStrings.tr
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.CyanTelemetry
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianTextPrimary
import com.example.ui.theme.ObsidianTextSecondary
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@Composable
fun LockdownScreen(
    endTimeEpoch: Long,
    totalDurationMinutes: Int,
    triggerWord: String,
    sourceApp: String,
    onLockExpired: () -> Unit,
    graceAvailable: Boolean = false,
    onGraceFreeRequested: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Disable back button while locked
    BackHandler(enabled = true) {
        // Intentionally no-op to prevent bypass during lockdown
    }

    var remainingMillis by remember(endTimeEpoch) {
        mutableLongStateOf(
            LockdownTimeLogic.remainingMs(endTimeEpoch, System.currentTimeMillis())
        )
    }

    // Breathing exercise is hidden by default on the lock screen and only opens when the user taps the button
    var showBreathingExercise by remember { mutableStateOf(false) }

    val totalDurationMillis = remember(totalDurationMinutes) {
        TimeUnit.MINUTES.toMillis(totalDurationMinutes.coerceAtLeast(1).toLong())

    }

    // The last wait is shortened to the time actually left, so the loop wakes
    // up exactly when the timer reaches zero instead of up to half a second
    // later.
    LaunchedEffect(endTimeEpoch) {
        while (true) {
            val left = LockdownTimeLogic.remainingMs(endTimeEpoch, System.currentTimeMillis())
            remainingMillis = left
            if (left <= 0L) {
                break
            }
            delay(LockdownTimeLogic.nextDelayMs(left))
        }
    }

    val isFinished = remainingMillis <= 0L

    // Expiry is FINAL for a timed block: the moment the countdown reaches
    // zero, the block lifts and this screen dismisses itself (clearing the
    // stored lock). It must not linger on an "UNLOCKED" page — a lingering
    // block screen can be mistaken for a live block, and (v0.4.9 bug) its
    // own printed trigger word used to re-arm the lock from underneath it.
    LaunchedEffect(isFinished) {
        if (isFinished) {
            onLockExpired()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // The mid-gradient tint comes from the theme tokens, so it follows the
    // selected season and the light/dark mode. A fixed dark-crimson literal
    // here looked wrong on every light palette.
    val gradientMid = CrimsonPrimary.copy(alpha = 0.07f).compositeOver(ObsidianBg)

    val triggerText = if (triggerWord.isEmpty()) {
        tr("Protected Content", "محتوای محافظت‌شده")
    } else {
        triggerWord
    }
    val sourceText = if (sourceApp.isEmpty()) tr("System", "سیستم") else sourceApp

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("lockdown_screen"),
        color = ObsidianBg
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ObsidianBg,
                            gradientMid,
                            ObsidianBg
                        )
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header badge
                Box(
                    modifier = Modifier
                        .scale(if (isFinished) 1f else pulseScale)
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFinished) EmeraldShield.copy(alpha = 0.2f)
                            else CrimsonPrimary.copy(alpha = 0.2f)
                        )
                        .border(
                            2.dp,
                            if (isFinished) EmeraldShield else CrimsonPrimary,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFinished) Icons.Default.CheckCircle else Icons.Default.Shield,
                        contentDescription = tr("Shield status", "وضعیت محافظ"),
                        tint = if (isFinished) EmeraldShield else CrimsonPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                val isPanicLock = triggerWord.contains("Panic", ignoreCase = true)

                Text(
                    text = when {
                        isFinished -> tr("Focus Period Complete", "پایان زمان قفل محافظتی")
                        isPanicLock -> tr("Emergency Panic Shield Active", "قفل اضطراری پنیک فعال است")
                        else -> tr("Screen Locked by X Blocker", "صفحه توسط ایکس بلاکر قفل شد")
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianTextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = when {
                        isFinished -> tr(
                            "You successfully resisted the impulse. Don't break the chain!",
                            "شما با موفقیت بر وسوسه غلبه کردید. زنجیره را نشکن!"
                        )
                        isPanicLock -> tr(
                            "Voluntary Panic Mode lock active. Only Calls & Texts are allowed.",
                            "قفل اضطراری فعال است. فقط تماس تلفنی و پیامک مجاز است."
                        )
                        else -> tr(
                            "All other apps are blocked until the timer ends. Only Calls & Texts are accessible.",
                            "تمام برنامه‌های دیگر تا پایان تایمر مسدودند. فقط تماس و پیامک در دسترس است."
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // Countdown Timer Gauge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(200.dp)
                ) {
                    val progress = if (totalDurationMillis > 0) {
                        (remainingMillis.toFloat() / totalDurationMillis).coerceIn(0f, 1f)
                    } else 0f

                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(200.dp),
                        color = ObsidianBorder,
                        strokeWidth = 14.dp
                    )

                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(200.dp),
                        color = if (isFinished) EmeraldShield else CrimsonPrimary,
                        strokeWidth = 14.dp
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isFinished) {
                            Text(
                                text = tr("UNLOCKED", "باز شد"),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = EmeraldShield
                            )
                        } else {
                            // Rounded UP to the next whole second, so "00:00"
                            // only ever means the block is over.
                            val timeStr = LockdownTimeLogic.formatCountdown(remainingMillis)

                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = ObsidianTextPrimary
                            )

                            Text(
                                text = tr("LOCKED", "در حال شمارش"),
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 2.sp,
                                color = ObsidianTextSecondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Emergency Phone Calls & SMS Texting Access Buttons (Item 8)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, EmeraldShield.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = tr(
                                "Allowed During Lock: Phone Calls & SMS Only",
                                "دسترسی مجاز هنگام قفل: فقط تماس تلفنی و پیامک"
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldShield
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    ShieldApplication.instance.preferences
                                        .grantCallOrSmsAccessForCurrentLock()
                                    // Open the device's DEFAULT dialer,
                                    // targeted by package: a bare ACTION_DIAL
                                    // can silently fail on some builds.
                                    // Fallbacks: untargeted ACTION_DIAL, then
                                    // the dialer package's launch intent.
                                    val dialerPkg = runCatching {
                                        val tm = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                                        tm?.defaultDialerPackage
                                    }.getOrNull()
                                    val opened = runCatching {
                                        val targeted = Intent(Intent.ACTION_DIAL).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            if (!dialerPkg.isNullOrBlank()) setPackage(dialerPkg)
                                        }
                                        context.startActivity(targeted)
                                        true
                                    }.getOrDefault(false)
                                    if (!opened) {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Intent.ACTION_DIAL).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                            )
                                        }.onFailure {
                                            if (!dialerPkg.isNullOrBlank()) {
                                                runCatching {
                                                    val launch = context.packageManager
                                                        .getLaunchIntentForPackage(dialerPkg)?.apply {
                                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                        }
                                                    if (launch != null) context.startActivity(launch)
                                                }
                                            }
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = EmeraldShield.copy(alpha = 0.18f),
                                    contentColor = EmeraldShield
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("lockdown_phone_call_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = tr("Phone call", "تماس تلفنی"),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tr("Phone Call", "تماس تلفنی"),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            FilledTonalButton(
                                onClick = {
                                    ShieldApplication.instance.preferences
                                        .grantCallOrSmsAccessForCurrentLock()
                                    // Open the DEFAULT SMS app by launching
                                    // its package directly: the old
                                    // ACTION_VIEW "sms:" intent has no
                                    // handler on several Samsung builds and
                                    // silently did nothing when tapped.
                                    // Fallbacks: the messaging-category
                                    // intent, then a SENDTO composer.
                                    val smsPkg = runCatching {
                                        Telephony.Sms.getDefaultSmsPackage(context)
                                    }.getOrNull()
                                    var opened = false
                                    if (!smsPkg.isNullOrBlank()) {
                                        opened = runCatching {
                                            val launch = context.packageManager
                                                .getLaunchIntentForPackage(smsPkg)?.apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                ?: throw IllegalStateException("No launch intent for SMS app")
                                            context.startActivity(launch)
                                            true
                                        }.getOrDefault(false)
                                    }
                                    if (!opened) {
                                        opened = runCatching {
                                            val categoryIntent = Intent(Intent.ACTION_MAIN).apply {
                                                addCategory(Intent.CATEGORY_APP_MESSAGING)
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(categoryIntent)
                                            true
                                        }.getOrDefault(false)
                                    }
                                    if (!opened) {
                                        runCatching {
                                            val sendTo = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(sendTo)
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = IndigoAccent.copy(alpha = 0.2f),
                                    contentColor = CyanTelemetry
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("lockdown_sms_text_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = tr("Text message", "پیامک"),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tr("Text / SMS", "ارسال پیامک"),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // One-Time 10-Second Grace (keyword blocks only, once per
                // continuous trigger episode). After the 10 seconds the
                // shield re-checks: word still on screen -> block returns
                // WITHOUT this button; word gone -> no block.
                if (graceAvailable && !isFinished) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.55f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = tr(
                                    "One-time chance: peek for 10 seconds — if the trigger word is still there, the shield comes back without this button.",
                                    "فرصت یک‌باره: ۱۰ ثانیه آزاد — اگر کلمه ممنوعه هنوز روی صفحه باشد، قفل بدون این دکمه برمی‌گردد."
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = ObsidianTextSecondary
                            )
                            Button(
                                onClick = onGraceFreeRequested,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AmberWarning,
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("lockdown_grace_10s_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tr("10 Seconds Free", "۱۰ ثانیه آزاد"),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Detection trigger details card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ObsidianBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tr("Strict Lock Activated", "قفل سخت‌گیرانه فعال شد"),
                                style = MaterialTheme.typography.titleSmall,
                                color = ObsidianTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Label and value share the row by weight and the value
                        // may wrap, so a long trigger such as "Daily Limit (30m):
                        // <app name>" no longer squeezes its own label. Start/End
                        // alignment mirrors correctly in the Persian (RTL) layout.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = tr("Trigger reason:", "دلیل فعال‌سازی:"),
                                style = MaterialTheme.typography.bodySmall,
                                color = ObsidianTextSecondary,
                                modifier = Modifier.weight(0.42f)
                            )
                            Text(
                                text = triggerText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = CrimsonPrimary,
                                textAlign = TextAlign.End,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(0.58f)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = tr("Source / Duration:", "منبع / مدت قفل:"),
                                style = MaterialTheme.typography.bodySmall,
                                color = ObsidianTextSecondary,
                                modifier = Modifier.weight(0.42f)
                            )
                            Text(
                                text = "$sourceText • $totalDurationMinutes ${tr("min", "دقیقه")}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = ObsidianTextPrimary,
                                textAlign = TextAlign.End,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(0.58f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Daily Motivational Quote Card (Non-tappable, 1 new quote per day)
                MotivationalQuoteBannerCard()

                Spacer(modifier = Modifier.height(12.dp))

                // Button to Open / Hide Breathing Exercise
                OutlinedButton(
                    onClick = { showBreathingExercise = !showBreathingExercise },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, CyanTelemetry),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CyanTelemetry.copy(alpha = 0.10f),
                        contentColor = CyanTelemetry
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("toggle_lockdown_breathing_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Air,
                        contentDescription = null,
                        tint = CyanTelemetry,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (showBreathingExercise) {
                            tr("Hide Breathing Exercise", "بستن تمرین تنفس")
                        } else {
                            tr("Open Breathing Exercise (4-7-8 / Box)", "باز کردن تمرین تنفس و آرام‌سازی")
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = CyanTelemetry
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (showBreathingExercise) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = CyanTelemetry
                    )
                }

                AnimatedVisibility(
                    visible = showBreathingExercise,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        TacticalBreathingStudioCard()
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (isFinished) {
                    Button(
                        onClick = onLockExpired,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("dismiss_lock_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldShield,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = tr("Return to X Blocker", "بازگشت به ایکس بلاکر"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                } else {
                    Text(
                        text = tr(
                            "Leaving to any other app is blocked while the timer is counting down.",
                            "خروج به هر برنامه دیگری در حین شمارش تایمر مسدود است."
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = ObsidianTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
