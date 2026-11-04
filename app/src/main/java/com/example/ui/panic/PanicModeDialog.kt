package com.example.ui.panic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianTextSecondary
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun PanicModeDialog(
    streakDays: Int,
    urgesDefeatedCount: Int,
    personalPledge: String,
    onSavePersonalPledge: (String) -> Unit,
    onUrgeDefeated: () -> Unit,
    onTriggerEmergencyLockdown: (durationMinutes: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var urgeWaveSecondsLeft by remember { mutableIntStateOf(180) } // 3-minute urge surf timer
    var breathingPhase by remember { mutableStateOf("Inhale Slowly (4s)") }
    var breathingInstruction by remember { mutableStateOf("Fill your lungs deeply through your nose") }
    val circleScale = remember { Animatable(0.72f) }

    var isEditingPledge by remember { mutableStateOf(false) }
    var draftPledge by remember(personalPledge) { mutableStateOf(personalPledge) }
    var selectedEmergencyMinutes by remember { mutableIntStateOf(15) }

    val physicalChallenges = remember {
        listOf(
            "Stand up & step away from your bed or chair right now",
            "Do 20 pushups or squats to redirect blood flow",
            "Splash cold water on your face or drink a full glass of water",
            "Take 5 slow 4-7-8 breaths before touching any browser"
        )
    }
    val completedChallenges = remember { mutableStateListOf<Int>() }

    // 3-minute urge surfing countdown
    LaunchedEffect(Unit) {
        while (urgeWaveSecondsLeft > 0) {
            delay(1000L)
            urgeWaveSecondsLeft--
        }
    }

    // 4-7-8 Nervous System Regulation Breathing Loop
    LaunchedEffect(Unit) {
        while (true) {
            breathingPhase = "Inhale Deeply (4s)"
            breathingInstruction = "Breathe in quietly through your nose"
            circleScale.animateTo(1.22f, animationSpec = tween(4000, easing = LinearEasing))

            breathingPhase = "Hold Breath (7s)"
            breathingInstruction = "Let oxygen calm your heart rate & prefrontal cortex"
            delay(7000L)

            breathingPhase = "Exhale Completely (8s)"
            breathingInstruction = "Release the tension slowly through your mouth"
            circleScale.animateTo(0.72f, animationSpec = tween(8000, easing = LinearEasing))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("panic_mode_screen"),
            color = ObsidianBg
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1F0A10),
                                ObsidianBg,
                                Color(0xFF091915)
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
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(CrimsonPrimary.copy(alpha = 0.2f))
                                    .border(1.5.dp, CrimsonPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Emergency,
                                    contentDescription = null,
                                    tint = CrimsonPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "PANIC MODE ACTIVE",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Protecting your $streakDays-Day Clean Streak • $urgesDefeatedCount Urges Defeated",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldShield
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_panic_mode_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Panic Mode",
                                tint = ObsidianTextSecondary
                            )
                        }
                    }

                    // 1. Instant Voluntary Phone Lockdown Card (Primary Emergency Action)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF241115)),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, CrimsonPrimary)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = CrimsonPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Emergency Self-Lockdown",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Text(
                                text = "Feeling overwhelmed? Lock your entire phone right now so you physically cannot act on the urge. Your clean streak stays 100% intact.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
                            )

                            // Duration Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(5, 15, 30, 60).forEach { mins ->
                                    val isSelected = selectedEmergencyMinutes == mins
                                    Surface(
                                        onClick = { selectedEmergencyMinutes = mins },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .testTag("panic_lock_preset_${mins}m"),
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) CrimsonPrimary else ObsidianCard,
                                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            ObsidianBorder
                                        )
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${mins}m",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else ObsidianTextSecondary
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    onUrgeDefeated()
                                    onTriggerEmergencyLockdown(selectedEmergencyMinutes)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("trigger_panic_lockdown_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CrimsonPrimary,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "LOCK MY PHONE FOR $selectedEmergencyMinutes MIN NOW",
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    // 2. 4-7-8 Breathing & 3-Minute Urge Surf Timer
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "4-7-8 Tactical Breathing",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldShield
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ObsidianCard,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                                ) {
                                    val mins = urgeWaveSecondsLeft / 60
                                    val secs = urgeWaveSecondsLeft % 60
                                    Text(
                                        text = String.format(Locale.ROOT, "Urge Wave: %02d:%02d", mins, secs),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberWarning,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Box(
                                modifier = Modifier
                                    .size(136.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldShield.copy(alpha = 0.12f))
                                    .border(2.dp, EmeraldShield.copy(alpha = 0.7f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(92.dp)
                                        .scale(circleScale.value)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    EmeraldShield.copy(alpha = 0.65f),
                                                    EmeraldShield.copy(alpha = 0.2f)
                                                )
                                            )
                                        )
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.SelfImprovement,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = breathingPhase,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = breathingInstruction,
                                style = MaterialTheme.typography.bodySmall,
                                color = ObsidianTextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // 3. Personal Commitment Vault ("Why I Started")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Remember Why You Started",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF818CF8)
                                )
                                IconButton(
                                    onClick = { isEditingPledge = !isEditingPledge },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Pledge",
                                        tint = ObsidianTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            if (isEditingPledge) {
                                OutlinedTextField(
                                    value = draftPledge,
                                    onValueChange = { draftPledge = it },
                                    label = { Text("Your personal commitment") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("panic_pledge_input"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = IndigoAccent,
                                        unfocusedBorderColor = ObsidianBorder,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        onSavePersonalPledge(draftPledge)
                                        isEditingPledge = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Save Commitment", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text(
                                    text = "\"$personalPledge\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // 4. Physical Dopamine Interrupt Checklist
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = AmberWarning,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Physical Pattern Interrupt (${completedChallenges.size}/${physicalChallenges.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { completedChallenges.size.toFloat() / physicalChallenges.size },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = EmeraldShield,
                                trackColor = ObsidianCard
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            physicalChallenges.forEachIndexed { index, challenge ->
                                val isDone = completedChallenges.contains(index)
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            if (isDone) completedChallenges.remove(index)
                                            else completedChallenges.add(index)
                                        },
                                    color = if (isDone) EmeraldShield.copy(alpha = 0.15f) else ObsidianCard,
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isDone) EmeraldShield else ObsidianBorder
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (isDone) EmeraldShield else ObsidianTextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = challenge,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isDone) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isDone) Color.White else ObsidianTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. "I Defeated This Urge" Victory Button
                    OutlinedButton(
                        onClick = {
                            onUrgeDefeated()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("urge_defeated_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = EmeraldShield.copy(alpha = 0.15f),
                            contentColor = EmeraldShield
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldShield),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "I SURVIVED THIS URGE (+1 VICTORY)",
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}
