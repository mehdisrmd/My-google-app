package com.example.ui.streak

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.CyanTelemetry
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianTextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class Milestone(
    val day: Int,
    val title: String,
    val description: String,
    val badgeColor: Color
)

val MILESTONES = listOf(
    Milestone(1, "Novice", "Taking the first pledge", Color(0xFF94A3B8)),
    Milestone(3, "Spark", "72h Dopamine Reset", AmberWarning),
    Milestone(7, "Bronze Shield", "1 Week Milestone", Color(0xFFCD7F32)),
    Milestone(14, "Silver Guardian", "2 Weeks of Self-Mastery", Color(0xFFC0C0C0)),
    Milestone(30, "Gold Warrior", "1 Month Reboot", Color(0xFFFFD700)),
    Milestone(60, "Platinum Veteran", "60 Days Brain Rewiring", Color(0xFFE5E4E2)),
    Milestone(90, "Diamond Master", "90-Day Complete Reboot", Color(0xFF38BDF8))
)

@Composable
fun NoBeepStreakScreen(
    streakStartTimestamp: Long,
    longestStreakDays: Int,
    slipsCount: Int,
    urgesDefeatedCount: Int = 0,
    onResetStreak: (reason: String) -> Unit,
    onOpenPanicMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val elapsedMillis = (currentTime - streakStartTimestamp).coerceAtLeast(0L)
    val days = TimeUnit.MILLISECONDS.toDays(elapsedMillis).toInt()
    val hours = (TimeUnit.MILLISECONDS.toHours(elapsedMillis) % 24).toInt()
    val minutes = (TimeUnit.MILLISECONDS.toMinutes(elapsedMillis) % 60).toInt()
    val seconds = (TimeUnit.MILLISECONDS.toSeconds(elapsedMillis) % 60).toInt()

    val currentMilestone = remember(days) {
        MILESTONES.lastOrNull { days >= it.day } ?: MILESTONES.first()
    }
    val nextMilestone = remember(days) {
        MILESTONES.firstOrNull { it.day > days }
    }

    val progressToNext = remember(days, currentMilestone, nextMilestone) {
        if (nextMilestone == null) 1f
        else {
            val prevDay = currentMilestone.day
            val targetDay = nextMilestone.day
            if (targetDay == prevDay) 1f
            else ((days - prevDay).toFloat() / (targetDay - prevDay)).coerceIn(0f, 1f)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = "Clean Streak",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Inspired by NoBeep clean time tracking",
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianTextSecondary
                )
            }

            // Emergency SOS Panic Button
            Button(
                onClick = onOpenPanicMode,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrimsonPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("sos_panic_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Emergency,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PANIC SOS",
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Live Clock Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                currentMilestone.badgeColor.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(currentMilestone.badgeColor.copy(alpha = 0.2f))
                            .border(2.dp, currentMilestone.badgeColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = currentMilestone.badgeColor,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "$days",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 58.sp
                    )

                    Text(
                        text = "DAYS CLEAN",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                        color = currentMilestone.badgeColor
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Digital Clock Timer (HH : MM : SS)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ObsidianCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanTelemetry.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = String.format(Locale.ROOT, "%02dh : %02dm : %02ds", hours, minutes, seconds),
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = CyanTelemetry
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Milestone badge label
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = currentMilestone.badgeColor.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, currentMilestone.badgeColor)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MilitaryTech,
                                contentDescription = null,
                                tint = currentMilestone.badgeColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentMilestone.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = currentMilestone.badgeColor
                            )
                        }
                    }
                }
            }
        }

        // 7-Day Weekly Streak Consistency Strip
        WeeklyStreakConsistencyCard(
            streakStartTimestamp = streakStartTimestamp,
            currentTimeMillis = currentTime
        )

        // Milestone Progress Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Next Rank: ${nextMilestone?.title ?: "Max Rank"}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (nextMilestone != null) "Day ${nextMilestone.day} (${nextMilestone.day - days} left)" else "Completed",
                        style = MaterialTheme.typography.labelSmall,
                        color = ObsidianTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progressToNext },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = currentMilestone.badgeColor,
                    trackColor = ObsidianCard
                )
            }
        }

        // Stats Row (Best Streak, Urges Defeated, Total Slips)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Best Record",
                        style = MaterialTheme.typography.labelSmall,
                        color = ObsidianTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$longestStreakDays d",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldShield
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Urges Beat",
                        style = MaterialTheme.typography.labelSmall,
                        color = ObsidianTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$urgesDefeatedCount",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IndigoAccent
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Resets",
                        style = MaterialTheme.typography.labelSmall,
                        color = ObsidianTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$slipsCount",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AmberWarning
                    )
                }
            }
        }

        // Milestones Journey Horizontal List
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Reboot Milestones Journey",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(MILESTONES) { milestone ->
                    val isUnlocked = days >= milestone.day
                    Card(
                        modifier = Modifier.width(130.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUnlocked) ObsidianCard else Color(0xFF0F1420)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isUnlocked) milestone.badgeColor.copy(alpha = 0.5f) else ObsidianBorder
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (isUnlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isUnlocked) milestone.badgeColor else ObsidianTextSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Day ${milestone.day}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isUnlocked) Color.White else ObsidianTextSecondary
                            )
                            Text(
                                text = milestone.title,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                color = if (isUnlocked) milestone.badgeColor else ObsidianTextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Reset Streak Button (with reflection dialog)
        OutlinedButton(
            onClick = { showResetDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("reset_streak_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonPrimary),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.6f))
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Reset Streak (Relapse)", fontWeight = FontWeight.SemiBold)
        }
    }

    if (showResetDialog) {
        ResetStreakDialog(
            onDismiss = { showResetDialog = false },
            onConfirm = { reason ->
                onResetStreak(reason)
                showResetDialog = false
            }
        )
    }
}

@Composable
fun WeeklyStreakConsistencyCard(
    streakStartTimestamp: Long,
    currentTimeMillis: Long
) {
    val dayFmt = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val todayEpochDay = TimeUnit.MILLISECONDS.toDays(currentTimeMillis)
    val streakStartEpochDay = TimeUnit.MILLISECONDS.toDays(streakStartTimestamp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weekly_streak_consistency_card"),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "7-Day Weekly Consistency",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Daily Check-in",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldShield
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                (6 downTo 0).forEach { daysAgo ->
                    val targetDay = todayEpochDay - daysAgo
                    val isClean = targetDay >= streakStartEpochDay
                    val date = Date(currentTimeMillis - TimeUnit.DAYS.toMillis(daysAgo.toLong()))
                    val label = if (daysAgo == 0) "Today" else dayFmt.format(date).take(3)

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isClean) EmeraldShield.copy(alpha = 0.2f)
                                    else ObsidianCard
                                )
                                .border(
                                    1.5.dp,
                                    if (isClean) EmeraldShield else ObsidianBorder,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isClean) Icons.Default.CheckCircle else Icons.Default.Whatshot,
                                contentDescription = null,
                                tint = if (isClean) EmeraldShield else ObsidianTextSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (daysAgo == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (daysAgo == 0) CyanTelemetry else ObsidianTextSecondary,
                            fontSize = 10.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ResetStreakDialog(
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    var reasonText by remember { mutableStateOf("") }
    val commonTriggers = listOf("Boredom", "Stress / Fatigue", "Social Media Triggers", "Late Night Screen")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Text(
                text = "Log Slip & Reset Streak",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "A slip is not a failure, but a lesson. Be honest with yourself to build stronger resilience:",
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianTextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "What was the trigger?",
                    style = MaterialTheme.typography.labelSmall,
                    color = ObsidianTextSecondary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    commonTriggers.take(2).forEach { t ->
                        Surface(
                            onClick = { reasonText = t },
                            shape = RoundedCornerShape(8.dp),
                            color = if (reasonText == t) CrimsonPrimary.copy(alpha = 0.2f) else ObsidianCard,
                            border = if (reasonText == t) androidx.compose.foundation.BorderStroke(1.dp, CrimsonPrimary) else null
                        ) {
                            Text(
                                text = t,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (reasonText == t) Color.White else ObsidianTextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = reasonText,
                    onValueChange = { reasonText = it },
                    placeholder = { Text("Describe what happened...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("slip_reason_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CrimsonPrimary,
                        unfocusedBorderColor = ObsidianCard,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reasonText) },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Reset Streak", color = Color.White, fontWeight = FontWeight.Bold)
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
fun SosPanicDialog(onDismiss: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val scale = remember { Animatable(0.8f) }

    LaunchedEffect(step) {
        if (step == 0) {
            while (true) {
                scale.animateTo(1.2f, animationSpec = tween(4000, easing = LinearEasing))
                delay(4000L)
                scale.animateTo(0.8f, animationSpec = tween(4000, easing = LinearEasing))
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CrimsonPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Emergency,
                        contentDescription = null,
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Emergency Urge Relief (SOS)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (step == 0) {
                    Text(
                        text = "1. Regulate Your Nervous System",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldShield
                    )
                    Text(
                        text = "Follow the circle: Inhale (4s) -> Hold (4s) -> Exhale (4s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = ObsidianTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(EmeraldShield.copy(alpha = 0.2f))
                            .border(2.dp, EmeraldShield, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .scale(scale.value)
                                .clip(CircleShape)
                                .background(EmeraldShield.copy(alpha = 0.5f))
                        )
                        Icon(
                            imageVector = Icons.Default.SelfImprovement,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else if (step == 1) {
                    Text(
                        text = "2. 5-4-3-2-1 Sensory Grounding",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = IndigoAccent
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "• Acknowledge 5 things you can see around you.\n" +
                                "• Touch 4 physical textures (shirt, table, wall).\n" +
                                "• Listen to 3 distant sounds.\n" +
                                "• Notice 2 distinct smells.\n" +
                                "• Take 1 deep breath.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "3. Physical Shock & Distance",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AmberWarning
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "• Splash cold water on your face.\n" +
                                "• Do 15 quick pushups or jumping jacks.\n" +
                                "• Put the phone in another room for 10 minutes.\n" +
                                "• Remember: The urge is only temporary neurochemistry.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (step < 2) step++ else onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldShield),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (step < 2) "Next Technique" else "Urge Passed", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ObsidianTextSecondary)
            }
        }
    )
}
