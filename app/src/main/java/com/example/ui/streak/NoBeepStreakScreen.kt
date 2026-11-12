package com.example.ui.streak

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.R
import com.example.ShieldApplication
import com.example.ui.components.MotivationalQuoteBannerCard
import com.example.ui.components.MotivationalQuotesRepository
import com.example.ui.i18n.AppStrings.tr
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
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

data class StreakMilestone(
    val days: Int,
    val titleEn: String,
    val titleFa: String,
    val subtitleEn: String,
    val subtitleFa: String,
    val shareMottoEn: String,
    val shareMottoFa: String,
    val badgeColor: Color,
    val secondaryGlowColor: Color
)

val STREAK_MILESTONES = listOf(
    StreakMilestone(
        days = 0,
        titleEn = "Day 0: Chain Starter",
        titleFa = "روز ۰: آغازگر تعهد پاکی",
        subtitleEn = "Commitment pledged — your clean chain has officially begun.",
        subtitleFa = "تعهد شما ثبت شد — زنجیره پاکی شما رسماً آغاز شد.",
        shareMottoEn = "Day 0: Forging an unbreakable chain!",
        shareMottoFa = "روز ۰: آغاز ساختن یک زنجیره شکست‌ناپذیر!",
        badgeColor = Color(0xFF10B981),
        secondaryGlowColor = Color(0xFF059669)
    ),
    StreakMilestone(
        days = 1,
        titleEn = "Day 1: Chain Initiate",
        titleFa = "روز ۱: اولین حلقه زنجیره",
        subtitleEn = "First 24 hours clean — the unbreakable chain begins.",
        subtitleFa = "۲۴ ساعت اول پاکی — اولین حلقه زنجیره ساخته شد.",
        shareMottoEn = "Day 1 forged. Don't break the chain!",
        shareMottoFa = "حلقه اول ساخته شد. زنجیره را نشکن!",
        badgeColor = Color(0xFF38BDF8),
        secondaryGlowColor = Color(0xFF0284C7)
    ),
    StreakMilestone(
        days = 3,
        titleEn = "Day 3: Dopamine Spark",
        titleFa = "روز ۳: جرقه بازسازی ذهن",
        subtitleEn = "Initial urge wave conquered; mental fog starting to lift.",
        subtitleFa = "عبور از موج اول وسوسه و شروع شفافیت ذهن.",
        shareMottoEn = "3 Days Clean. Surfing every urge without breaking the chain.",
        shareMottoFa = "۳ روز پاکی. عبور از موج وسوسه بدون شکستن زنجیره.",
        badgeColor = Color(0xFF818CF8),
        secondaryGlowColor = Color(0xFF4F46E5)
    ),
    StreakMilestone(
        days = 7,
        titleEn = "Day 7: Bronze Shield",
        titleFa = "روز ۷: سپر برنزی (۱ هفته پاک)",
        subtitleEn = "One full week of discipline! Focus & energy rising.",
        subtitleFa = "یک هفته کامل اراده و نظم! افزایش محسوس تمرکز و انرژی.",
        shareMottoEn = "7-Day Bronze Shield Unlocked! Don't break the chain.",
        shareMottoFa = "نشان سپر برنزی ۷ روزه باز شد! زنجیره را نشکن.",
        badgeColor = Color(0xFFD97706),
        secondaryGlowColor = Color(0xFFF59E0B)
    ),
    StreakMilestone(
        days = 14,
        titleEn = "Day 14: Silver Guardian",
        titleFa = "روز ۱۴: محافظ نقره‌ای (۲ هفته پاک)",
        subtitleEn = "Two weeks clean — neural pathways rewiring for self-command.",
        subtitleFa = "دو هفته پاکی — بازسازی مسیرهای عصبی برای تسلط بر نفس.",
        shareMottoEn = "14 Days Clean • Silver Guardian Rank Achieved!",
        shareMottoFa = "۱۴ روز پاکی • دستیابی به رتبه محافظ نقره‌ای!",
        badgeColor = Color(0xFF94A3B8),
        secondaryGlowColor = Color(0xFF38BDF8)
    ),
    StreakMilestone(
        days = 30,
        titleEn = "Day 30: Gold Warrior",
        titleFa = "روز ۳۰: جنگجوی طلایی (۱ ماه پاک)",
        subtitleEn = "One month milestone! Strong emotional & dopamine balance.",
        subtitleFa = "یک ماه کامل پیروزی! تعادل قدرتمند دوپامین و آرامش ذهنی.",
        shareMottoEn = "30 Days Clean • Gold Warrior! Discipline over impulse.",
        shareMottoFa = "۳۰ روز پاکی • جنگجوی طلایی! پیروزی اراده بر هوس.",
        badgeColor = Color(0xFFFBBF24),
        secondaryGlowColor = Color(0xFF10B981)
    ),
    StreakMilestone(
        days = 60,
        titleEn = "Day 60: Platinum Titan",
        titleFa = "روز ۶۰: تایتان پلاتینیوم",
        subtitleEn = "Two months of mastery. Old triggers lose their grip.",
        subtitleFa = "دو ماه تسلط کامل. محرک‌های قدیمی قدرت خود را از دست داده‌اند.",
        shareMottoEn = "60 Days Clean • Platinum Titan! Unbreakable chain.",
        shareMottoFa = "۶۰ روز پاکی • تایتان پلاتینیوم! زنجیره شکست‌ناپذیر.",
        badgeColor = Color(0xFF34D399),
        secondaryGlowColor = Color(0xFF06B6D4)
    ),
    StreakMilestone(
        days = 90,
        titleEn = "Day 90: Diamond Legend",
        titleFa = "روز ۹۰: افسانه الماس (ریبوت کامل)",
        subtitleEn = "Full 90-day reboot complete! Total freedom & self-mastery.",
        subtitleFa = "ریبوت کامل ۹۰ روزه! آزادی، تمرکز و تسلط کامل بر خویشتن.",
        shareMottoEn = "90-Day Diamond Legend Reboot Complete! Master of self.",
        shareMottoFa = "ریبوت ۹۰ روزه افسانه الماس کامل شد! ارباب خویشتن.",
        badgeColor = Color(0xFF10B981),
        secondaryGlowColor = Color(0xFFFBBF24)
    )
)

@Composable
fun NoBeepStreakScreen(
    streakStartTimestamp: Long,
    longestStreakDays: Int,
    slipsCount: Int,
    urgesDefeatedCount: Int,
    onResetStreak: (reason: String) -> Unit,
    onOpenPanicMode: () -> Unit = {},
    onOpenAddictionQuiz: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { runCatching { ShieldApplication.instance.preferences }.getOrNull() }
    val scrollState = rememberScrollState()
    var showRelapseDialog by remember { mutableStateOf(false) }
    var inspectedBadgeMilestone by remember { mutableStateOf<StreakMilestone?>(null) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(streakStartTimestamp) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val elapsedMillis = (nowMillis - streakStartTimestamp).coerceAtLeast(0L)
    val days = TimeUnit.MILLISECONDS.toDays(elapsedMillis).toInt()
    val hours = (TimeUnit.MILLISECONDS.toHours(elapsedMillis) % 24).toInt()
    val minutes = (TimeUnit.MILLISECONDS.toMinutes(elapsedMillis) % 60).toInt()
    val seconds = (TimeUnit.MILLISECONDS.toSeconds(elapsedMillis) % 60).toInt()

    val bestChainDays = maxOf(days, longestStreakDays, prefs?.getLongestStreakDays() ?: 0)

    val nextMilestone = STREAK_MILESTONES.firstOrNull { it.days > days } ?: STREAK_MILESTONES.last()
    val prevMilestoneDays = STREAK_MILESTONES.lastOrNull { it.days <= days }?.days ?: 0

    val defaultMilestoneIndex = remember(days) {
        val unlockedIdx = STREAK_MILESTONES.indexOfLast { days >= it.days }
        if (unlockedIdx >= 0) unlockedIdx else 0
    }
    var selectedShareMilestoneIndex by remember(defaultMilestoneIndex) {
        mutableIntStateOf(defaultMilestoneIndex)
    }
    val selectedShareMilestone = STREAK_MILESTONES[selectedShareMilestoneIndex]

    val progressToNext = remember(elapsedMillis, nextMilestone, prevMilestoneDays) {
        val spanDays = (nextMilestone.days - prevMilestoneDays).coerceAtLeast(1)
        val spanMillis = TimeUnit.DAYS.toMillis(spanDays.toLong()).toFloat()
        val elapsedSincePrev = (elapsedMillis - TimeUnit.DAYS.toMillis(prevMilestoneDays.toLong())).coerceAtLeast(0L)
        (elapsedSincePrev / spanMillis).coerceIn(0f, 1f)
    }

    val currentBadgeTitle = when {
        days >= 90 -> tr("Diamond Legend", "افسانه الماس")
        days >= 60 -> tr("Platinum Titan", "تایتان پلاتینیوم")
        days >= 30 -> tr("Gold Warrior", "جنگجوی طلایی")
        days >= 14 -> tr("Silver Guardian", "محافظ نقره‌ای")
        days >= 7 -> tr("Bronze Shield", "سپر برنزی")
        days >= 3 -> tr("Dopamine Spark", "جرقه اراده")
        days >= 1 -> tr("Chain Initiate", "آغازگر زنجیره")
        else -> tr("Chain Starter", "شروع‌کننده مسیر")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tr("Clean Streak & Chain", "زنجیره روزهای پاک"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = ObsidianTextPrimary
                )
                Text(
                    text = tr(
                        "Best Chain: ${bestChainDays}d",
                        "بهترین زنجیره: $bestChainDays روز"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianTextSecondary
                )
            }

            // Current Rank Badge
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldShield.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, EmeraldShield.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = null,
                            tint = EmeraldShield,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentBadgeTitle,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldShield
                        )
                    }
                }
            }
        }

        // Main Hero Live Counter Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("streak_hero_card"),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, EmeraldShield.copy(alpha = 0.45f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                EmeraldShield.copy(alpha = 0.14f),
                                ObsidianSurface,
                                ObsidianSurface
                            )
                        )
                    )
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(170.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.size(170.dp),
                            color = ObsidianCard,
                            strokeWidth = 12.dp
                        )

                        CircularProgressIndicator(
                            progress = { progressToNext },
                            modifier = Modifier.size(170.dp),
                            color = EmeraldShield,
                            strokeWidth = 12.dp
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = EmeraldShield,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$days",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Black,
                                color = ObsidianTextPrimary
                            )
                            Text(
                                text = tr("DAYS CLEAN", "روز پاک"),
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldShield
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live HH:MM:SS Ticker
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ObsidianCard,
                        border = BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TimeUnitColumn(value = String.format(Locale.US, "%02d", hours), label = tr("HRS", "ساعت"))
                            Text(":", color = ObsidianTextSecondary, fontWeight = FontWeight.Bold)
                            TimeUnitColumn(value = String.format(Locale.US, "%02d", minutes), label = tr("MIN", "دقیقه"))
                            Text(":", color = ObsidianTextSecondary, fontWeight = FontWeight.Bold)
                            TimeUnitColumn(value = String.format(Locale.US, "%02d", seconds), label = tr("SEC", "ثانیه"))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (days < 90) {
                            tr(
                                "${(progressToNext * 100).toInt()}% toward ${nextMilestone.titleEn}",
                                "${(progressToNext * 100).toInt()}٪ تا رسیدن به ${nextMilestone.titleFa}"
                            )
                        } else {
                            tr(
                                "90-Day Reboot Achieved! Keep guarding your chain.",
                                "ریبوت ۹۰ روزه تکمیل شد! به محافظت از زنجیره ادامه دهید."
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = ObsidianTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Minimal & Cute Share Streak Card (Items 12 & 13: No "Instagram/X" text, shows @mehdisrmd in small font, opens Android system share sheet with app icon)
        MinimalCuteStreakShareCard(
            currentStreakDays = days,
            bestChainDays = bestChainDays,
            urgesDefeatedCount = urgesDefeatedCount,
            selectedMilestone = selectedShareMilestone,
            onSelectMilestoneIndex = { selectedShareMilestoneIndex = it },
            selectedMilestoneIndex = selectedShareMilestoneIndex,
            context = context
        )

        // Daily Motivational Quote Card (Item 10: Non-tappable, 1 new quote per day)
        MotivationalQuoteBannerCard(streakDays = days)

        // Emergency Panic Button & Scientific Quiz Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onOpenPanicMode,
                modifier = Modifier
                    .weight(1.3f)
                    .height(52.dp)
                    .testTag("streak_panic_sos_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrimsonPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Emergency,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = tr("PANIC SOS", "دکمه اضطراری پنیک"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black
                )
            }

            FilledTonalButton(
                onClick = onOpenAddictionQuiz,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("streak_retake_quiz_button"),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = IndigoAccent.copy(alpha = 0.2f),
                    contentColor = CyanTelemetry
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = tr("Dependency Quiz", "آزمون وابستگی"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 3-Column Stats Row (Best Chain, Urges Beat, Resets)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatMiniCard(
                modifier = Modifier.weight(1f),
                title = tr("BEST CHAIN", "بهترین رکورد"),
                value = "${bestChainDays}d",
                accentColor = AmberWarning,
                icon = Icons.Default.EmojiEvents
            )
            StatMiniCard(
                modifier = Modifier.weight(1f),
                title = tr("URGES BEAT", "مهار وسوسه"),
                value = "$urgesDefeatedCount",
                accentColor = EmeraldShield,
                icon = Icons.Default.Shield
            )
            StatMiniCard(
                modifier = Modifier.weight(1f),
                title = tr("RESETS", "ریست‌ها"),
                value = "$slipsCount",
                accentColor = CrimsonPrimary,
                icon = Icons.Default.Refresh
            )
        }

        // Brain Reboot Progress Bar (90-Day Goal)
        val brainRebootPercent = ((days.toFloat() / 90f) * 100f).coerceIn(0f, 100f)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, ObsidianBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("90-Day Dopamine Reboot Progress", "پیشرفت ریبوت ۹۰ روزه مغز و دوپامین"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianTextPrimary
                    )
                    Text(
                        text = "${brainRebootPercent.toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldShield
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { brainRebootPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = EmeraldShield,
                    trackColor = ObsidianCard
                )
            }
        }

        // Interactive Milestone Badges Section (Item 15: Fixed & Interactive with Progress Bars + Badge Trophy Dialog)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tr(
                    "Milestone Badges (Tap any badge to inspect & share)",
                    "نشان‌های افتخار (برای مشاهده و اشتراک‌گذاری روی هر نشان بزنید)"
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ObsidianTextPrimary
            )
            val unlockedCount = STREAK_MILESTONES.count { days >= it.days }
            Text(
                text = "$unlockedCount / ${STREAK_MILESTONES.size}",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Black,
                color = EmeraldShield
            )
        }

        STREAK_MILESTONES.forEachIndexed { idx, milestone ->
            val unlocked = days >= milestone.days
            val badgeProgress = if (milestone.days <= 0) {
                1f
            } else {
                val targetMs = TimeUnit.DAYS.toMillis(milestone.days.toLong()).toFloat()
                (elapsedMillis.toFloat() / targetMs).coerceIn(0f, 1f)
            }
            MilestoneRowCard(
                milestone = milestone,
                unlocked = unlocked,
                progressFraction = badgeProgress,
                isSelectedForShare = idx == selectedShareMilestoneIndex,
                onClick = {
                    selectedShareMilestoneIndex = idx
                    inspectedBadgeMilestone = milestone
                }
            )
        }

        // Relapse / Reset Streak Action
        OutlinedButton(
            onClick = { showRelapseDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("reset_streak_button"),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.6f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonPrimary)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = tr("I Slipped — Reset My Clean Streak", "لغزش داشتم — شروع مجدد روزهای پاک"),
                fontWeight = FontWeight.Bold
            )
        }
    }

    // Interactive Badge Trophy Dialog when user taps any badge in the Badge Section (Item 15)
    inspectedBadgeMilestone?.let { badge ->
        val isUnlocked = days >= badge.days
        val targetMs = TimeUnit.DAYS.toMillis(badge.days.coerceAtLeast(1).toLong())
        val progressPct = if (badge.days <= 0 || isUnlocked) {
            100
        } else {
            ((elapsedMillis.toFloat() / targetMs.toFloat()) * 100f).toInt().coerceIn(0, 99)
        }
        val remainingHours = if (isUnlocked) {
            0L
        } else {
            TimeUnit.MILLISECONDS.toHours((targetMs - elapsedMillis).coerceAtLeast(0L))
        }
        val quoteForBadge = MotivationalQuotesRepository.getDailyQuoteForToday()

        AlertDialog(
            onDismissRequest = { inspectedBadgeMilestone = null },
            containerColor = ObsidianSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(badge.badgeColor.copy(alpha = 0.2f))
                            .border(1.5.dp, badge.badgeColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isUnlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                            contentDescription = null,
                            tint = badge.badgeColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = tr(badge.titleEn, badge.titleFa),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = ObsidianTextPrimary
                        )
                        Text(
                            text = if (isUnlocked) {
                                tr("✓ UNLOCKED BADGE", "✓ نشان باز شده")
                            } else {
                                tr("LOCKED • $progressPct% Complete ($remainingHours hrs left)", "قفل • $progressPct٪ انجام شده ($remainingHours ساعت باقی‌مانده)")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) EmeraldShield else AmberWarning
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = tr(badge.subtitleEn, badge.subtitleFa),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ObsidianTextPrimary
                    )
                    LinearProgressIndicator(
                        progress = { progressPct / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = badge.badgeColor,
                        trackColor = ObsidianCard
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ObsidianCard,
                        border = BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tr("Clean days reached:", "روزهای پاک این مرحله:"),
                                style = MaterialTheme.typography.labelMedium,
                                color = ObsidianTextSecondary
                            )
                            Text(
                                text = "${badge.days.coerceAtLeast(1)}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,
                                color = AmberWarning
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        StreakShareImageGenerator.shareMilestoneViaSystemSheet(
                            context = context,
                            milestone = badge,
                            currentStreakDays = days,
                            bestChainDays = bestChainDays,
                            urgesDefeatedCount = urgesDefeatedCount,
                            quoteText = tr(quoteForBadge.quoteEn, quoteForBadge.quoteFa)
                        )
                        inspectedBadgeMilestone = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = badge.badgeColor,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tr("Share Badge", "اشتراک‌گذاری نشان"),
                        fontWeight = FontWeight.Black
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { inspectedBadgeMilestone = null }) {
                    Text(tr("Close", "بستن"), color = ObsidianTextSecondary)
                }
            }
        )
    }

    if (showRelapseDialog) {
        NoBeepYoutubeResetStreakDialog(
            bestChainDays = bestChainDays,
            onConfirmResetStreak = { note ->
                onResetStreak(note)
            },
            onDismiss = { showRelapseDialog = false }
        )
    }
}

@Composable
private fun StatMiniCard(
    title: String,
    value: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                color = ObsidianTextPrimary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = ObsidianTextSecondary,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

/**
 * Minimal & Cute Share Streak Card (Items 12 & 13):
 * - Removes "Instagram / X" from the title.
 * - Minimal, cute floating animation with the new Material Design 3 App Icon.
 * - Shows ONLY `@mehdisrmd` in small font at the bottom (instead of the app name).
 * - Tapping the card or Share button opens the Android system share chooser (`Intent.createChooser`)
 *   so the user can pick any app with native app icons.
 */
@Composable
fun MinimalCuteStreakShareCard(
    currentStreakDays: Int,
    bestChainDays: Int,
    urgesDefeatedCount: Int,
    selectedMilestone: StreakMilestone,
    selectedMilestoneIndex: Int,
    onSelectMilestoneIndex: (Int) -> Unit,
    context: Context,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "minimal_cute_share_anim")
    val gentleFloatY by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gentle_float_y"
    )
    val softGlowScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "soft_glow_scale"
    )

    val displayedDays = maxOf(currentStreakDays, selectedMilestone.days)
    val quoteForCard = remember {
        MotivationalQuotesRepository.getDailyQuoteForToday()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("streak_milestone_share_studio_card"),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.5.dp, selectedMilestone.badgeColor.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with Material 3 App Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_stop_sign_x_blocker_1794123976741),
                        contentDescription = "App Icon",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, selectedMilestone.badgeColor, RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = tr(
                                "Share Clean Streak Card",
                                "اشتراک‌گذاری کارت زنجیره پاکی"
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = ObsidianTextPrimary
                        )
                        Text(
                            text = tr(
                                "Minimal animated card • Tap to share to any app",
                                "کارت مینیمال با انیمیشن لطیف • اشتراک در هر برنامه"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = ObsidianTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = selectedMilestone.badgeColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Milestone Tier Selector Pills (0d, 1d, 3d, 7d, 14d, 30d, 60d, 90d)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                STREAK_MILESTONES.forEachIndexed { idx, ms ->
                    val isSelected = idx == selectedMilestoneIndex
                    val isUnlocked = currentStreakDays >= ms.days
                    Surface(
                        onClick = { onSelectMilestoneIndex(idx) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) ms.badgeColor.copy(alpha = 0.25f) else ObsidianCard,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) ms.badgeColor else ObsidianBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_milestone_pill_${ms.days}d")
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${ms.days}d",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = if (isSelected || isUnlocked) FontWeight.Black else FontWeight.Medium,
                                color = if (isSelected) ms.badgeColor else ObsidianTextSecondary,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            // Minimal & Cute Visual Share Card Preview (Tap opens Android System Share Sheet!)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(215.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0F172A),
                                Color(0xFF111827),
                                Color(0xFF0B132B)
                            )
                        )
                    )
                    .border(1.5.dp, selectedMilestone.badgeColor.copy(alpha = 0.8f), RoundedCornerShape(22.dp))
                    .clickable {
                        StreakShareImageGenerator.shareMilestoneViaSystemSheet(
                            context = context,
                            milestone = selectedMilestone,
                            currentStreakDays = currentStreakDays,
                            bestChainDays = bestChainDays,
                            urgesDefeatedCount = urgesDefeatedCount,
                            quoteText = tr(quoteForCard.quoteEn, quoteForCard.quoteFa)
                        )
                    }
                    .testTag("streak_visual_poster_preview"),
                contentAlignment = Alignment.Center
            ) {
                // Minimal cute soft glowing pastel circles in background
                val primaryGlow = selectedMilestone.badgeColor
                val secondaryGlow = selectedMilestone.secondaryGlowColor
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f - 8.dp.toPx())
                    drawCircle(
                        color = primaryGlow.copy(alpha = 0.12f),
                        radius = 74.dp.toPx() * softGlowScale,
                        center = center
                    )
                    drawCircle(
                        color = secondaryGlow.copy(alpha = 0.16f),
                        radius = 52.dp.toPx() / softGlowScale,
                        center = center
                    )
                    // Minimal cute sparkle dots
                    drawCircle(
                        color = primaryGlow.copy(alpha = 0.65f),
                        radius = 3.5.dp.toPx(),
                        center = Offset(center.x - 85.dp.toPx(), center.y - 34.dp.toPx())
                    )
                    drawCircle(
                        color = AmberWarning.copy(alpha = 0.7f),
                        radius = 3.dp.toPx(),
                        center = Offset(center.x + 88.dp.toPx(), center.y - 26.dp.toPx())
                    )
                    drawCircle(
                        color = EmeraldShield.copy(alpha = 0.65f),
                        radius = 3.dp.toPx(),
                        center = Offset(center.x - 74.dp.toPx(), center.y + 32.dp.toPx())
                    )
                    drawCircle(
                        color = primaryGlow.copy(alpha = 0.65f),
                        radius = 3.5.dp.toPx(),
                        center = Offset(center.x + 78.dp.toPx(), center.y + 30.dp.toPx())
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Minimal Pill with M3 App Icon + Milestone Title
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, selectedMilestone.badgeColor.copy(alpha = 0.65f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_stop_sign_x_blocker_1794123976741),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tr(selectedMilestone.titleEn, selectedMilestone.titleFa),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Center Cute Floating Streak Emblem
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .offset(y = gentleFloatY.dp)
                            .scale(softGlowScale)
                    ) {
                        Text(
                            text = "${displayedDays}d Clean ✨",
                            style = MaterialTheme.typography.headlineLarge,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tr(selectedMilestone.shareMottoEn, selectedMilestone.shareMottoFa),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = selectedMilestone.badgeColor,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Bottom Footer: ONLY `@mehdisrmd` in small font (instead of the app name!)
                    Text(
                        text = "@mehdisrmd",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Single Sleek Share Button with M3 App Icon -> Opens Native Android System Share Sheet
            Button(
                onClick = {
                    StreakShareImageGenerator.shareMilestoneViaSystemSheet(
                        context = context,
                        milestone = selectedMilestone,
                        currentStreakDays = currentStreakDays,
                        bestChainDays = bestChainDays,
                        urgesDefeatedCount = urgesDefeatedCount,
                        quoteText = tr(quoteForCard.quoteEn, quoteForCard.quoteFa)
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = selectedMilestone.badgeColor,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("share_streak_instagram_button")
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_stop_sign_x_blocker_1794123976741),
                    contentDescription = null,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = tr(
                        "Share Clean Streak Card (@mehdisrmd)",
                        "اشتراک‌گذاری کارت پاکی (@mehdisrmd)"
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

object StreakShareImageGenerator {
    /**
     * Renders the minimal & cute streak card PNG with `@mehdisrmd` in small font at the bottom
     * (without the app name) and opens Android's native system share sheet (`Intent.createChooser`)
     * so all installed app icons are shown.
     */
    fun shareMilestoneViaSystemSheet(
        context: Context,
        milestone: StreakMilestone,
        currentStreakDays: Int,
        bestChainDays: Int,
        urgesDefeatedCount: Int,
        quoteText: String
    ) {
        val shownDays = maxOf(currentStreakDays, milestone.days)
        val shareCaption = buildString {
            append("✨ ${shownDays}d Clean • ${milestone.titleEn}\n")
            append("🔗 ${milestone.shareMottoEn} (Best Chain: ${maxOf(bestChainDays, shownDays)}d)\n")
            append("💬 \"$quoteText\"\n")
            append("@mehdisrmd")
        }

        try {
            val bitmap = renderMinimalSharePosterBitmap(
                context = context,
                milestone = milestone,
                currentStreakDays = currentStreakDays,
                bestChainDays = bestChainDays,
                quoteText = quoteText
            )
            val sharedDir = File(context.cacheDir, "shared_streaks").apply { mkdirs() }
            val imageFile = File(sharedDir, "streak_card_${shownDays}d.png")
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, shareCaption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(sendIntent, "Share Clean Streak").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
            try {
                val textIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareCaption)
                }
                context.startActivity(
                    Intent.createChooser(textIntent, "Share Clean Streak").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            } catch (_: Exception) {
            }
        }
    }

    private fun renderMinimalSharePosterBitmap(
        context: Context,
        milestone: StreakMilestone,
        currentStreakDays: Int,
        bestChainDays: Int,
        quoteText: String
    ): Bitmap {
        val width = 1080
        val height = 1080
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Minimal deep slate gradient background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                0f,
                height.toFloat(),
                intArrayOf(0xFF0F172A.toInt(), 0xFF111827.toInt(), 0xFF0B132B.toInt()),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Soft glowing aura circles
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = milestone.badgeColor.copy(alpha = 0.16f).toArgb()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(width / 2f, height / 2f - 40f, 290f, glowPaint)

        // Rounded minimal frame
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = milestone.badgeColor.toArgb()
        }
        canvas.drawRoundRect(RectF(48f, 48f, width - 48f, height - 48f), 56f, 56f, borderPaint)

        // Draw M3 App Icon at top center
        val iconBitmap = runCatching {
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_stop_sign_x_blocker_1794123976741)
        }.getOrNull()
        if (iconBitmap != null) {
            val dst = RectF(width / 2f - 64f, 110f, width / 2f + 64f, 238f)
            canvas.drawBitmap(iconBitmap, null, dst, null)
        }

        // Milestone Rank Title
        val rankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = milestone.badgeColor.toArgb()
            textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(milestone.titleEn, width / 2f, 310f, rankPaint)

        // Center Streak Days
        val shownDays = maxOf(currentStreakDays, milestone.days)
        val daysPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 148f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${shownDays}d Clean", width / 2f, 530f, daysPaint)

        // Motto
        val mottoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = milestone.badgeColor.toArgb()
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(milestone.shareMottoEn, width / 2f, 620f, mottoPaint)

        // Best Chain Subtitle
        val chainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFCBD5E1.toInt()
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "Don't break the chain • Best Chain: ${maxOf(bestChainDays, shownDays)}d",
            width / 2f,
            690f,
            chainPaint
        )

        // Quote
        val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE2E8F0.toInt()
            textSize = 28f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        val trimmedQuote = if (quoteText.length > 72) quoteText.take(69) + "..." else quoteText
        canvas.drawText("\"$trimmedQuote\"", width / 2f, 830f, quotePaint)

        // Small font @mehdisrmd signature at bottom (instead of app name)
        val signaturePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF94A3B8.toInt()
            textSize = 26f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("@mehdisrmd", width / 2f, 975f, signaturePaint)

        return bitmap
    }
}

@Composable
private fun TimeUnitColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = ObsidianTextPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ObsidianTextSecondary,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun MilestoneRowCard(
    milestone: StreakMilestone,
    unlocked: Boolean,
    progressFraction: Float,
    isSelectedForShare: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("milestone_row_${milestone.days}d"),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) ObsidianSurface else ObsidianCard.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (isSelectedForShare) 1.5.dp else 1.dp,
            color = when {
                isSelectedForShare -> milestone.badgeColor
                unlocked -> milestone.badgeColor.copy(alpha = 0.55f)
                else -> ObsidianBorder
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (unlocked) milestone.badgeColor.copy(alpha = 0.22f)
                            else ObsidianBorder.copy(alpha = 0.4f)
                        )
                        .border(
                            1.5.dp,
                            if (unlocked) milestone.badgeColor else ObsidianBorder,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (unlocked) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Unlocked",
                            tint = milestone.badgeColor,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = ObsidianTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr(milestone.titleEn, milestone.titleFa),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (unlocked) ObsidianTextPrimary else ObsidianTextSecondary
                    )
                    Text(
                        text = tr(milestone.subtitleEn, milestone.subtitleFa),
                        style = MaterialTheme.typography.bodySmall,
                        color = ObsidianTextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (unlocked) {
                        milestone.badgeColor.copy(alpha = 0.2f)
                    } else {
                        ObsidianSurface
                    },
                    border = BorderStroke(
                        1.dp,
                        if (unlocked) milestone.badgeColor else ObsidianBorder
                    )
                ) {
                    Text(
                        text = if (unlocked) {
                            tr("UNLOCKED", "باز شده")
                        } else {
                            "${(progressFraction * 100).toInt()}%"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = if (unlocked) milestone.badgeColor else ObsidianTextSecondary,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { if (unlocked) 1f else progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (unlocked) milestone.badgeColor else CyanTelemetry,
                trackColor = ObsidianBorder
            )
        }
    }
}
