package com.example.ui.panic

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.MotivationalQuoteBannerCard
import com.example.ui.components.TacticalBreathingStudioCard
import com.example.ui.i18n.AppStrings.tr
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
import com.example.ui.theme.ObsidianTextPrimary
import com.example.ui.theme.ObsidianTextSecondary

/**
 * Emergency Panic Mode (SOS) Dialog with Redesigned Tactical Breathing Studio,
 * "Don't Break the Chain" & Famous Quotes, Personal Pledge, Notification Bar SOS sync,
 * and Instant Voluntary Self-Lockdown.
 */
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
    var selectedEmergencyMinutes by remember { mutableIntStateOf(15) }
    var isEditingPledge by remember { mutableStateOf(false) }
    var pledgeDraft by remember(personalPledge) { mutableStateOf(personalPledge) }
    var urgeVictoryFeedback by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
                .testTag("panic_mode_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = ObsidianBg,
            border = BorderStroke(1.5.dp, CrimsonPrimary.copy(alpha = 0.65f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
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
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = tr("Emergency Panic Mode", "حالت اضطراری پنیک (SOS)"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = ObsidianTextPrimary
                            )
                            Text(
                                text = tr(
                                    "Don't break your ${streakDays}-day chain • $urgesDefeatedCount urges defeated",
                                    "زنجیره $streakDays روزه‌ات را نشکن • $urgesDefeatedCount وسوسه شکست‌خورده"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldShield
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_panic_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ObsidianTextSecondary
                        )
                    }
                }

                // Redesigned Breathing Section UI & UX
                TacticalBreathingStudioCard()

                // Motivational "Don't Break the Chain" & Famous Quotes Banner
                MotivationalQuoteBannerCard(streakDays = streakDays)

                // Personal Pledge Card ("Remember Why You Started")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = IndigoAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tr("YOUR PERSONAL PLEDGE", "تعهد شخصی شما به خودتان"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = IndigoAccent
                                )
                            }

                            IconButton(
                                onClick = { isEditingPledge = !isEditingPledge },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("edit_pledge_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Pledge",
                                    tint = ObsidianTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (isEditingPledge) {
                            OutlinedTextField(
                                value = pledgeDraft,
                                onValueChange = { pledgeDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pledge_input_field"),
                                textStyle = MaterialTheme.typography.bodySmall.copy(color = ObsidianTextPrimary),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IndigoAccent,
                                    unfocusedBorderColor = ObsidianBorder
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    onSavePersonalPledge(pledgeDraft)
                                    isEditingPledge = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .testTag("save_pledge_button")
                            ) {
                                Text(
                                    text = tr("Save Pledge", "ذخیره تعهد"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = "\"$personalPledge\"",
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Medium,
                                color = ObsidianTextPrimary
                            )
                        }
                    }
                }

                // Instant Voluntary Emergency Lockdown Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = CrimsonPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = tr(
                                        "Voluntary Emergency Phone Lockdown",
                                        "قفل اضطراری داوطلبانه گوشی"
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ObsidianTextPrimary
                                )
                                Text(
                                    text = tr(
                                        "Lock your screen right now so you can step away & protect your chain:",
                                        "گوشی را همین حالا قفل کنید تا از محیط وسوسه فاصله بگیرید:"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ObsidianTextSecondary
                                )
                            }
                        }

                        val durationPresets = listOf(5, 15, 30, 60)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            durationPresets.forEach { mins ->
                                val selected = mins == selectedEmergencyMinutes
                                Surface(
                                    onClick = { selectedEmergencyMinutes = mins },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .testTag("panic_preset_${mins}m"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) CrimsonPrimary else ObsidianSurface,
                                    border = if (selected) null else BorderStroke(1.dp, ObsidianBorder)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${mins}m",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) Color.White else ObsidianTextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                onUrgeDefeated()
                                onTriggerEmergencyLockdown(selectedEmergencyMinutes)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("trigger_emergency_lockdown_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrimsonPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tr(
                                    "ACTIVATE ${selectedEmergencyMinutes}m EMERGENCY LOCKDOWN",
                                    "فعال‌سازی قفل اضطراری ${selectedEmergencyMinutes} دقیقه‌ای"
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
                if (urgeVictoryFeedback) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldShield.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, EmeraldShield),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("urge_defeated_victory_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldShield,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tr(
                                    "Victory! +1 Urge Defeated recorded ($urgesDefeatedCount total). Don't break the chain!",
                                    "آفرین! +۱ پیروزی بر وسوسه ثبت شد (مجموع: $urgesDefeatedCount). زنجیره را نشکن!"
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldShield
                            )
                        }
                    }
                }

                // Bottom Action Buttons: "I Beat The Urge (+1)" & "Close"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onUrgeDefeated()
                            urgeVictoryFeedback = true
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("urge_defeated_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, EmeraldShield)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldShield,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr("I Beat The Urge (+1)", "بر وسوسه غلبه کردم (+۱)"),
                            color = EmeraldShield,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(0.7f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberWarning.copy(alpha = 0.2f),
                            contentColor = AmberWarning
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = tr("Done", "بستن"),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
