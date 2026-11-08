package com.example.ui.lockdown

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun LockdownScreen(
    endTimeEpoch: Long,
    totalDurationMinutes: Int,
    triggerWord: String,
    sourceApp: String,
    onLockExpired: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Disable back button while locked
    BackHandler(enabled = true) {
        // Intentionally no-op to prevent bypass during lockdown
    }

    var remainingMillis by remember(endTimeEpoch) {
        mutableLongStateOf(
            (endTimeEpoch - System.currentTimeMillis()).coerceAtLeast(0L)
        )
    }

    // Breathing exercise is hidden by default on the lock screen and only opens when the user taps the button
    var showBreathingExercise by remember { mutableStateOf(false) }

    val totalDurationMillis = remember(totalDurationMinutes) {
        TimeUnit.MINUTES.toMillis(totalDurationMinutes.coerceAtLeast(1).toLong())
    }

    LaunchedEffect(endTimeEpoch) {
        while (true) {
            val now = System.currentTimeMillis()
            val left = (endTimeEpoch - now).coerceAtLeast(0L)
            remainingMillis = left
            if (left <= 0L) {
                break
            }
            delay(500L)
        }
    }

    val isFinished = remainingMillis <= 0L

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
                            Color(0xFF160B12),
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
                        contentDescription = "Shield Status",
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
                            val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis)
                            val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis) % 60
                            val timeStr = String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)

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
                                    runCatching {
                                        ShieldApplication.instance.preferences.grantTemporaryCallOrSmsAccess(120)
                                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(dialIntent)
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
                                    contentDescription = "Phone Call",
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
                                    runCatching {
                                        ShieldApplication.instance.preferences.grantTemporaryCallOrSmsAccess(120)
                                        val smsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:")).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(smsIntent)
                                    }.onFailure {
                                        runCatching {
                                            val fallback = Intent(Intent.ACTION_MAIN).apply {
                                                addCategory(Intent.CATEGORY_APP_MESSAGING)
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(fallback)
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
                                    contentDescription = "Text Message",
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

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = tr("Trigger reason:", "دلیل فعال‌سازی:"),
                                style = MaterialTheme.typography.bodySmall,
                                color = ObsidianTextSecondary
                            )
                            Text(
                                text = triggerWord.ifEmpty { "Protected Content" },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = CrimsonPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = tr("Source / Duration:", "منبع / مدت قفل:"),
                                style = MaterialTheme.typography.bodySmall,
                                color = ObsidianTextSecondary
                            )
                            Text(
                                text = "${sourceApp.ifEmpty { "System" }} • $totalDurationMinutes min",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = ObsidianTextPrimary
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
