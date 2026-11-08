package com.example.ui.streak

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.i18n.AppStrings.tr
import com.example.ui.info.GithubUpdateChecker
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.CyanTelemetry
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianTextPrimary
import com.example.ui.theme.ObsidianTextSecondary

data class YoutubeRecoveryLesson(
    val id: String,
    val titleEn: String,
    val titleFa: String,
    val channelName: String,
    val durationText: String,
    val youtubeUrl: String,
    val summaryPointEn: String,
    val summaryPointFa: String
)

val NOBEEP_YOUTUBE_RECOVERY_LESSONS = listOf(
    YoutubeRecoveryLesson(
        id = "chaser_effect",
        titleEn = "How to Beat the 'Chaser Effect' After a Slip (Neuroscience)",
        titleFa = "چگونه پس از لغزش، «اثر تعقیب» (Chaser Effect) و وسوسه مجدد را شکست دهیم؟",
        channelName = "NoBeep Recovery & Dopamine Science",
        durationText = "08:42",
        youtubeUrl = "https://www.youtube.com/watch?v=p3JANDkZb24",
        summaryPointEn = "The 48 hours after a slip have the highest dopamine craving ('Chaser Effect'). Recognising this brain trick prevents a single slip from turning into a binge.",
        summaryPointFa = "۴۸ ساعت پس از یک لغزش، مغز بیشترین موج وسوسه («اثر تعقیب») را ایجاد می‌کند. شناخت این تله عصبی مانع از تبدیل یک لغزش به چرخه تکرار می‌شود."
    ),
    YoutubeRecoveryLesson(
        id = "brain_on_dopamine",
        titleEn = "Your Brain on Dopamine & The 90-Day Reboot Process",
        titleFa = "مغز شما، دوپامین و فرآیند علمی ریبوت ۹۰ روزه",
        channelName = "TEDx • Neuroscience of Recovery",
        durationText = "11:18",
        youtubeUrl = "https://www.youtube.com/watch?v=7oFVOJf0TzY",
        summaryPointEn = "Every clean day rewires deltaFosB and D2 dopamine receptors—your previous clean days are never lost.",
        summaryPointFa = "هر روز پاکی، گیرنده‌های دوپامین مغز را بازسازی می‌کند؛ زحمات روزهای پاک قبلی شما هرگز از بین نمی‌رود."
    ),
    YoutubeRecoveryLesson(
        id = "huberman_dopamine",
        titleEn = "Dr. Andrew Huberman: Controlling Dopamine & Overcoming Urges",
        titleFa = "دکتر اندرو هوبرمن: مدیریت دوپامین و غلبه علمی بر موج وسوسه",
        channelName = "Huberman Lab • Stanford Neuroscience",
        durationText = "10:05",
        youtubeUrl = "https://www.youtube.com/watch?v=QmOF0crdyRU",
        summaryPointEn = "Cold water, physical movement, and removing trigger cues immediately reset baseline dopamine.",
        summaryPointFa = "حرکت فیزیکی، تغییر محیط و حذف محرک‌ها بلافاصله سطح پایه دوپامین را متعادل می‌کنند."
    )
)

/**
 * NoBeep-Style Streak Reset Dialog with Interactive YouTube Video Link Preview Gate.
 * The user must tap on the YouTube Video Link Preview card to open & learn from the
 * recovery lesson before the "Reset to Day 0" confirmation button is unlocked.
 */
@Composable
fun NoBeepYoutubeResetStreakDialog(
    bestChainDays: Int,
    onConfirmResetStreak: (triggerNote: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var selectedLessonIndex by remember { mutableIntStateOf(0) }
    var hasTappedYoutubePreview by remember { mutableStateOf(false) }
    var triggerNote by remember { mutableStateOf("") }

    val currentLesson = NOBEEP_YOUTUBE_RECOVERY_LESSONS[selectedLessonIndex]
    val youtubeRed = Color(0xFFFF0000)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        modifier = Modifier.testTag("nobeep_youtube_reset_streak_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(youtubeRed.copy(alpha = 0.18f))
                        .border(1.dp, youtubeRed.copy(alpha = 0.7f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartDisplay,
                        contentDescription = null,
                        tint = youtubeRed,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr(
                            "Reset Streak • Learn First (NoBeep)",
                            "ریست روزهای پاک • آموزش علمی (نوبیپ)"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = ObsidianTextPrimary
                    )
                    Text(
                        text = tr(
                            "Best Chain (${bestChainDays}d) is kept safe forever",
                            "بهترین زنجیره شما ($bestChainDays روز) برای همیشه حفظ می‌شود"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldShield,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Step 1 Instruction Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (hasTappedYoutubePreview) {
                        EmeraldShield.copy(alpha = 0.15f)
                    } else {
                        AmberWarning.copy(alpha = 0.15f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (hasTappedYoutubePreview) EmeraldShield else AmberWarning
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (hasTappedYoutubePreview) {
                                Icons.Default.CheckCircle
                            } else {
                                Icons.Default.Psychology
                            },
                            contentDescription = null,
                            tint = if (hasTappedYoutubePreview) EmeraldShield else AmberWarning,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasTappedYoutubePreview) {
                                tr(
                                    "✓ YouTube recovery video opened! You learned how to beat the Chaser Effect. You can now reset your streak below.",
                                    "✓ ویدیوی آموزشی یوتیوب باز شد! اکنون می‌توانید زنجیره خود را از روز ۰ با قدرت بیشتر آغاز کنید."
                                )
                            } else {
                                tr(
                                    "Tap the YouTube video link preview below to watch & learn how to stop a slip from repeating before resetting.",
                                    "قبل از ریست کردن روزهای پاک، روی پیش‌نمایش لینک ویدیوی یوتیوب زیر ضربه بزنید تا با روش علمی جلوگیری از تکرار لغزش آشنا شوید."
                                )
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (hasTappedYoutubePreview) EmeraldShield else AmberWarning
                        )
                    }
                }

                // Lesson Selector Chips (3 Curated NoBeep Recovery Videos)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NOBEEP_YOUTUBE_RECOVERY_LESSONS.forEachIndexed { idx, lesson ->
                        val selected = idx == selectedLessonIndex
                        Surface(
                            onClick = { selectedLessonIndex = idx },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) IndigoAccent.copy(alpha = 0.25f) else ObsidianCard,
                            border = BorderStroke(
                                1.dp,
                                if (selected) CyanTelemetry else ObsidianBorder
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = tr("Lesson ${idx + 1}", "ویدیو ${idx + 1}"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) CyanTelemetry else ObsidianTextSecondary,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Rich Interactive YouTube Video Link Preview Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianCard,
                    border = BorderStroke(
                        1.5.dp,
                        if (hasTappedYoutubePreview) EmeraldShield else youtubeRed.copy(alpha = 0.85f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            hasTappedYoutubePreview = true
                            GithubUpdateChecker.openUrlSafely(context, currentLesson.youtubeUrl)
                        }
                        .testTag("youtube_video_preview_card")
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // 16:9 YouTube Thumbnail Preview Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(138.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF0F172A),
                                            Color(0xFF1E1B4B),
                                            Color(0xFF3B0764)
                                        )
                                    )
                                )
                                .padding(12.dp)
                        ) {
                            // Top-left YouTube Badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.Black.copy(alpha = 0.72f),
                                modifier = Modifier.align(Alignment.TopStart)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SmartDisplay,
                                        contentDescription = null,
                                        tint = youtubeRed,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "YouTube • NoBeep Lesson",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            // Center YouTube Play Button Overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(width = 64.dp, height = 44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(youtubeRed)
                                    .border(1.5.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play YouTube Recovery Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            // Bottom-right Duration Badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.Black.copy(alpha = 0.82f),
                                modifier = Modifier.align(Alignment.BottomEnd)
                            ) {
                                Text(
                                    text = currentLesson.durationText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            // Bottom-left Tap to Watch Callout
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (hasTappedYoutubePreview) {
                                    EmeraldShield.copy(alpha = 0.9f)
                                } else {
                                    Color.White.copy(alpha = 0.16f)
                                },
                                modifier = Modifier.align(Alignment.BottomStart)
                            ) {
                                Text(
                                    text = if (hasTappedYoutubePreview) {
                                        tr("✓ WATCHED / OPENED", "✓ باز شد")
                                    } else {
                                        tr("TAP TO WATCH & UNLOCK", "برای تماشا و باز شدن قفل بزنید")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp,
                                    color = if (hasTappedYoutubePreview) Color.Black else Color.White,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // YouTube Link Preview Metadata Footer
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = tr(currentLesson.titleEn, currentLesson.titleFa),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianTextPrimary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentLesson.channelName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanTelemetry,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Launch,
                                    contentDescription = "Open YouTube Link",
                                    tint = CyanTelemetry,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            Text(
                                text = currentLesson.youtubeUrl,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = ObsidianTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ObsidianSurface,
                                border = BorderStroke(1.dp, ObsidianBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = tr(currentLesson.summaryPointEn, currentLesson.summaryPointFa),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = ObsidianTextSecondary,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }

                // Optional Trigger Note Input
                OutlinedTextField(
                    value = triggerNote,
                    onValueChange = { triggerNote = it },
                    label = {
                        Text(tr("What triggered the slip? (optional)", "علت یا موقعیت لغزش چه بود؟ (اختیاری)"))
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_streak_trigger_note_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CrimsonPrimary,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = ObsidianTextPrimary,
                        unfocusedTextColor = ObsidianTextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (hasTappedYoutubePreview) {
                        onConfirmResetStreak(triggerNote.trim())
                        onDismiss()
                    }
                },
                enabled = hasTappedYoutubePreview,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrimsonPrimary,
                    contentColor = Color.White,
                    disabledContainerColor = ObsidianCard,
                    disabledContentColor = ObsidianTextSecondary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("confirm_reset_streak_button")
            ) {
                Icon(
                    imageVector = if (hasTappedYoutubePreview) Icons.Default.Refresh else Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (hasTappedYoutubePreview) {
                        tr("Reset Streak to Day 0", "ریست به روز ۰")
                    } else {
                        tr("Tap YouTube Preview First", "ابتدا روی ویدیوی یوتیوب بزنید")
                    },
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = tr("Cancel", "انصراف"),
                    color = ObsidianTextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}
