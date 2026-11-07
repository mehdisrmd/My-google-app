package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.i18n.AppStrings.tr
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanTelemetry
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianTextPrimary
import com.example.ui.theme.ObsidianTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class BreathingPhaseSpec(
    val labelEn: String,
    val labelFa: String,
    val instructionEn: String,
    val instructionFa: String,
    val durationSeconds: Int,
    val targetScale: Float,
    val color: Color
)

enum class BreathingPatternMode(
    val titleEn: String,
    val titleFa: String,
    val subtitleEn: String,
    val subtitleFa: String,
    val phases: List<BreathingPhaseSpec>
) {
    RELAX_478(
        titleEn = "4-7-8 Calm",
        titleFa = "تنفس ۴-۷-۸",
        subtitleEn = "Deep vagus nerve reset to stop acute urges",
        subtitleFa = "آرام‌سازی عمیق سیستم عصبی و توقف وسوسه",
        phases = listOf(
            BreathingPhaseSpec(
                labelEn = "INHALE",
                labelFa = "دم عمیق",
                instructionEn = "Breathe in quietly through your nose",
                instructionFa = "آرام از راه بینی نفس عمیق بکشید",
                durationSeconds = 4,
                targetScale = 1.0f,
                color = CyanTelemetry
            ),
            BreathingPhaseSpec(
                labelEn = "HOLD",
                labelFa = "نگه دارید",
                instructionEn = "Hold your breath gently and let tension fade",
                instructionFa = "نفس را نگه دارید و به ضربان قلب توجه کنید",
                durationSeconds = 7,
                targetScale = 1.0f,
                color = IndigoAccent
            ),
            BreathingPhaseSpec(
                labelEn = "EXHALE",
                labelFa = "بازدم آرام",
                instructionEn = "Exhale slowly through your mouth — release the urge",
                instructionFa = "به آرامی از دهان بازدم کنید — وسوسه را رها سازید",
                durationSeconds = 8,
                targetScale = 0.56f,
                color = EmeraldShield
            )
        )
    ),
    BOX_4444(
        titleEn = "Box 4-4-4-4",
        titleFa = "تنفس مربعی ۴-۴",
        subtitleEn = "Tactical Navy SEAL focus & mental control",
        subtitleFa = "تکنیک تمرکز تاکتیکی برای تسلط کامل بر ذهن",
        phases = listOf(
            BreathingPhaseSpec(
                labelEn = "INHALE",
                labelFa = "دم",
                instructionEn = "Inhale steadily for 4 seconds",
                instructionFa = "به مدت ۴ ثانیه نفس بکشید",
                durationSeconds = 4,
                targetScale = 1.0f,
                color = CyanTelemetry
            ),
            BreathingPhaseSpec(
                labelEn = "HOLD FULL",
                labelFa = "مکث پر",
                instructionEn = "Hold with lungs full for 4 seconds",
                instructionFa = "۴ ثانیه نفس را در سینه نگه دارید",
                durationSeconds = 4,
                targetScale = 1.0f,
                color = AmberWarning
            ),
            BreathingPhaseSpec(
                labelEn = "EXHALE",
                labelFa = "بازدم",
                instructionEn = "Release smoothly for 4 seconds",
                instructionFa = "به مدت ۴ ثانیه آرام بازدم کنید",
                durationSeconds = 4,
                targetScale = 0.56f,
                color = EmeraldShield
            ),
            BreathingPhaseSpec(
                labelEn = "HOLD EMPTY",
                labelFa = "مکث خالی",
                instructionEn = "Rest calm & empty for 4 seconds",
                instructionFa = "۴ ثانیه در آرامش مکث کنید",
                durationSeconds = 4,
                targetScale = 0.56f,
                color = IndigoAccent
            )
        )
    ),
    QUICK_SIGH_426(
        titleEn = "Sigh 4-2-6",
        titleFa = "آرامش سریع ۴-۲-۶",
        subtitleEn = "Fast physiological sigh to lower heart rate",
        subtitleFa = "کاهش سریع استرس و ضربان قلب در ۱۲ ثانیه",
        phases = listOf(
            BreathingPhaseSpec(
                labelEn = "DOUBLE INHALE",
                labelFa = "دم دو مرحله‌ای",
                instructionEn = "Deep inhale + top-up sniff at the top",
                instructionFa = "یک دم عمیق و یک دم کوتاه تکمیلی",
                durationSeconds = 4,
                targetScale = 1.0f,
                color = CyanTelemetry
            ),
            BreathingPhaseSpec(
                labelEn = "PAUSE",
                labelFa = "مکث کوتاه",
                instructionEn = "Brief pause at peak lung expansion",
                instructionFa = "مکث کوتاه در اوج دم",
                durationSeconds = 2,
                targetScale = 1.0f,
                color = IndigoAccent
            ),
            BreathingPhaseSpec(
                labelEn = "LONG EXHALE",
                labelFa = "بازدم طولانی",
                instructionEn = "Long, slow exhale until completely calm",
                instructionFa = "بازدم طولانی و آرام تا تخلیه کامل تنش",
                durationSeconds = 6,
                targetScale = 0.56f,
                color = EmeraldShield
            )
        )
    )
}

/**
 * Redesigned Tactical Breathing Studio with Multi-Ring Visualizer, Mode Selector,
 * Phase Timeline Pills, Cycle Counter, and Play/Pause/Reset controls.
 */
@Composable
fun TacticalBreathingStudioCard(
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableStateOf(BreathingPatternMode.RELAX_478) }
    var isRunning by remember { mutableStateOf(true) }
    var phaseIndex by remember { mutableIntStateOf(0) }
    var completedCycles by remember { mutableIntStateOf(0) }

    val phases = selectedMode.phases
    val safePhaseIndex = phaseIndex.coerceIn(0, phases.lastIndex)
    val currentPhase = phases[safePhaseIndex]

    var secondsRemaining by remember(selectedMode, safePhaseIndex) {
        mutableIntStateOf(currentPhase.durationSeconds)
    }

    val orbScale = remember { Animatable(0.62f) }
    val ringProgress = remember { Animatable(0f) }

    val infiniteShimmer = rememberInfiniteTransition(label = "breathing_halo")
    val haloAlpha by infiniteShimmer.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_alpha"
    )

    LaunchedEffect(selectedMode, isRunning) {
        if (!isRunning) return@LaunchedEffect
        phaseIndex = 0
        while (isRunning) {
            val activePhases = selectedMode.phases
            for (idx in activePhases.indices) {
                if (!isRunning) break
                phaseIndex = idx
                val spec = activePhases[idx]
                secondsRemaining = spec.durationSeconds

                launch {
                    orbScale.animateTo(
                        targetValue = spec.targetScale,
                        animationSpec = tween(
                            durationMillis = spec.durationSeconds * 1000,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
                launch {
                    ringProgress.snapTo(0f)
                    ringProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = spec.durationSeconds * 1000,
                            easing = LinearEasing
                        )
                    )
                }

                for (sec in spec.durationSeconds downTo 1) {
                    if (!isRunning) break
                    secondsRemaining = sec
                    delay(1000L)
                }
            }
            if (isRunning) {
                completedCycles += 1
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tactical_breathing_studio_card"),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.5.dp, currentPhase.color.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            currentPhase.color.copy(alpha = 0.12f),
                            ObsidianSurface,
                            ObsidianCard.copy(alpha = 0.7f)
                        )
                    )
                )
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Row: Header + Cycles Completed Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(currentPhase.color.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Air,
                            contentDescription = null,
                            tint = currentPhase.color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = tr("Mindful Breathing Studio", "استودیو تنفس و آرام‌سازی ذهن"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = ObsidianTextPrimary
                        )
                        Text(
                            text = tr(selectedMode.subtitleEn, selectedMode.subtitleFa),
                            style = MaterialTheme.typography.labelSmall,
                            color = ObsidianTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldShield.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, EmeraldShield.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = tr("Cycles: $completedCycles", "دورها: $completedCycles"),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldShield,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Mode Selector Pills (4-7-8 Calm | Box 4-4-4-4 | Sigh 4-2-6)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BreathingPatternMode.entries.forEach { mode ->
                    val selected = mode == selectedMode
                    Surface(
                        onClick = {
                            selectedMode = mode
                            phaseIndex = 0
                            isRunning = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) currentPhase.color.copy(alpha = 0.22f) else ObsidianCard,
                        border = BorderStroke(
                            1.dp,
                            if (selected) currentPhase.color else ObsidianBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("breathing_mode_${mode.name.lowercase()}")
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tr(mode.titleEn, mode.titleFa),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (selected) ObsidianTextPrimary else ObsidianTextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Multi-Ring Animated Breathing Orb Visualizer
            Box(
                modifier = Modifier
                    .size(196.dp)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                val activeColor = currentPhase.color

                Canvas(modifier = Modifier.size(184.dp)) {
                    val strokeWidth = 10.dp.toPx()
                    // Outer track ring
                    drawCircle(
                        color = activeColor.copy(alpha = 0.14f),
                        style = Stroke(width = strokeWidth)
                    )
                    // Animated sweep arc for current phase progress
                    drawArc(
                        color = activeColor,
                        startAngle = -90f,
                        sweepAngle = 360f * ringProgress.value,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Pulsing middle aura
                Box(
                    modifier = Modifier
                        .size(152.dp)
                        .scale(orbScale.value)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    activeColor.copy(alpha = haloAlpha),
                                    activeColor.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(1.5.dp, activeColor.copy(alpha = 0.55f), CircleShape)
                )

                // Inner core with countdown & phase label
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(ObsidianCard.copy(alpha = 0.92f))
                        .border(2.dp, activeColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = tr(currentPhase.labelEn, currentPhase.labelFa),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = activeColor,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${secondsRemaining}s",
                            style = MaterialTheme.typography.headlineMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Black,
                            color = ObsidianTextPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.SelfImprovement,
                            contentDescription = null,
                            tint = activeColor.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Live Coaching Instruction
            Text(
                text = tr(currentPhase.instructionEn, currentPhase.instructionFa),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = ObsidianTextPrimary,
                textAlign = TextAlign.Center
            )

            // Step-by-Step Phase Timeline Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                phases.forEachIndexed { idx, spec ->
                    val isCurrent = idx == safePhaseIndex
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isCurrent) spec.color.copy(alpha = 0.22f) else ObsidianCard,
                        border = BorderStroke(
                            1.dp,
                            if (isCurrent) spec.color else ObsidianBorder
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = tr(spec.labelEn, spec.labelFa),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) spec.color else ObsidianTextSecondary,
                                maxLines = 1
                            )
                            Text(
                                text = "${spec.durationSeconds}s",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isCurrent) ObsidianTextPrimary else ObsidianTextSecondary
                            )
                        }
                    }
                }
            }

            // Play / Pause & Reset Cycle Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = { isRunning = !isRunning },
                    shape = RoundedCornerShape(10.dp),
                    color = currentPhase.color.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, currentPhase.color),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("breathing_play_pause_button")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = currentPhase.color,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRunning) tr("Pause Guide", "توقف موقت") else tr("Resume Guide", "ادامه تمرین"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianTextPrimary
                        )
                    }
                }

                Surface(
                    onClick = {
                        phaseIndex = 0
                        completedCycles = 0
                        isRunning = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.testTag("breathing_reset_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Breathing",
                            tint = ObsidianTextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tr("Reset", "شروع مجدد"),
                            style = MaterialTheme.typography.labelMedium,
                            color = ObsidianTextSecondary
                        )
                    }
                }
            }
        }
    }
}
