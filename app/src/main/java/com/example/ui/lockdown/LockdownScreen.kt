package com.example.ui.lockdown

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonDark
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
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
    // Disable back button while locked
    BackHandler(enabled = true) {
        // Intentionally no-op to prevent bypass during lockdown
    }

    var remainingMillis by remember {
        mutableLongStateOf(
            (endTimeEpoch - System.currentTimeMillis()).coerceAtLeast(0L)
        )
    }

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

    val quotes = remember {
        listOf(
            "The craving is a wave. Surf it without acting on it, and it will subside.",
            "Discipline is choosing between what you want now and what you want most.",
            "Every minute you resist rewires your dopamine pathways toward freedom.",
            "You are stronger than a temporary impulse. Breathe through it.",
            "Your future self will thank you for standing firm in this moment."
        )
    }
    var currentQuoteIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(6000L)
            currentQuoteIndex = (currentQuoteIndex + 1) % quotes.size
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
                        .size(88.dp)
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
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                val isPanicLock = triggerWord.contains("Panic", ignoreCase = true)

                Text(
                    text = when {
                        isFinished -> "Focus Period Complete"
                        isPanicLock -> "Emergency Panic Shield Active"
                        else -> "Screen Locked by ShieldBlock"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = when {
                        isFinished -> "You successfully resisted the impulse. Well done!"
                        isPanicLock -> "Voluntary Panic Mode lock active to protect your clean streak. Breathe through the wave."
                        else -> "Blocked keyword or restriction detected. Take a break to reset your focus."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = ObsidianTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                )

                // Countdown Timer Gauge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(240.dp)
                ) {
                    val progress = if (totalDurationMillis > 0) {
                        (remainingMillis.toFloat() / totalDurationMillis).coerceIn(0f, 1f)
                    } else 0f

                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(240.dp),
                        color = Color(0xFF1E293B),
                        strokeWidth = 14.dp
                    )

                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(240.dp),
                        color = if (isFinished) EmeraldShield else CrimsonPrimary,
                        strokeWidth = 14.dp
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isFinished) {
                            Text(
                                text = "UNLOCKED",
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
                                color = Color.White
                            )

                            Text(
                                text = "REMAINING",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 2.sp,
                                color = ObsidianTextSecondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Detection trigger details card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                text = "Strict Lock Activated",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Trigger keyword:",
                                style = MaterialTheme.typography.bodySmall,
                                color = ObsidianTextSecondary
                            )
                            Text(
                                text = triggerWord.ifEmpty { "Adult Content" },
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
                                text = "Lock duration:",
                                style = MaterialTheme.typography.bodySmall,
                                color = ObsidianTextSecondary
                            )
                            Text(
                                text = "$totalDurationMinutes min",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Rotating Mindful Recovery Quote
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelfImprovement,
                            contentDescription = null,
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = quotes[currentQuoteIndex],
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mindful Breathing / Urge Surfing Exercise
                MindfulBreathingCard(isFinished = isFinished)

                Spacer(modifier = Modifier.height(24.dp))

                if (isFinished) {
                    Button(
                        onClick = onLockExpired,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("dismiss_lock_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldShield,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Return to ShieldBlock",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "Phone is locked to prevent relapse. Wait for timer to finish.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun MindfulBreathingCard(isFinished: Boolean) {
    var phase by remember { mutableStateOf("Breathe In") }
    val scale = remember { Animatable(0.7f) }

    LaunchedEffect(isFinished) {
        if (isFinished) return@LaunchedEffect
        while (true) {
            phase = "Inhale Deeply (4s)"
            scale.animateTo(1.2f, animationSpec = tween(4000, easing = LinearEasing))
            phase = "Hold (4s)"
            delay(4000L)
            phase = "Exhale Slowly (4s)"
            scale.animateTo(0.7f, animationSpec = tween(4000, easing = LinearEasing))
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF24304D)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .scale(scale.value)
                        .clip(CircleShape)
                        .background(EmeraldShield.copy(alpha = 0.6f))
                )
                Icon(
                    imageVector = Icons.Default.SelfImprovement,
                    contentDescription = "Breathing guide",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Urge Surfing & Breathing",
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldShield,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isFinished) "Mind cleared & urge conquered" else phase,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = "Cravings typically peak and subside within 3 to 5 minutes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianTextSecondary
                )
            }
        }
    }
}
