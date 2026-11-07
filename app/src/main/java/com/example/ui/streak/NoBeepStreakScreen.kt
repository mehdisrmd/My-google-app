package com.example.ui.streak

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.cos
import kotlin.math.sin

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
        days = 1,
        titleEn = "Day 1: Chain Initiate",
        titleFa = "روز ۱: آغازگر زنجیره پاکی",
        subtitleEn = "First 24 hours clean — the unbreakable chain begins.",
        subtitleFa = "۲۴ ساعت اول پاکی — آغاز زنجیره شکست‌ناپذیر.",
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
        titleFa = "روز ۷: سپر برنزی (۱ هفته)",
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
        titleFa = "روز ۱۴: محافظ نقره‌ای (۲ هفته)",
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
        titleFa = "روز ۳۰: جنگجوی طلایی (۱ ماه)",
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showRelapseDialog by remember { mutableStateOf(false) }
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

    val nextMilestone = STREAK_MILESTONES.firstOrNull { it.days > days } ?: STREAK_MILESTONES.last()
    val prevMilestoneDays = STREAK_MILESTONES.lastOrNull { it.days <= days }?.days ?: 0

    // Default selected milestone for the Shareable Card is the highest unlocked milestone (or Day 1)
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
        else -> tr("Day 0 — Start Your Chain", "روز ۰ — شروع زنجیره پاکی")
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
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = ObsidianTextPrimary
                )
                Text(
                    text = tr(
                        "Don't break the chain • Share your milestone victories",
                        "زنجیره را نشکن • دستاوردهای خود را به اشتراک بگذارید"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianTextSecondary
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = EmeraldShield.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, EmeraldShield.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = null,
                        tint = EmeraldShield,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentBadgeTitle,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldShield
                    )
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
                    .padding(22.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(180.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.size(180.dp),
                            color = ObsidianCard,
                            strokeWidth = 12.dp
                        )

                        CircularProgressIndicator(
                            progress = { progressToNext },
                            modifier = Modifier.size(180.dp),
                            color = EmeraldShield,
                            strokeWidth = 12.dp
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = EmeraldShield,
                                modifier = Modifier.size(26.dp)
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

                    Spacer(modifier = Modifier.height(14.dp))

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

                    Spacer(modifier = Modifier.height(12.dp))

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

        // Predefined Image & Animation Social Share Studio (Instagram & X / Twitter)
        PredefinedStreakMilestoneShareCard(
            currentStreakDays = days,
            urgesDefeatedCount = urgesDefeatedCount,
            selectedMilestone = selectedShareMilestone,
            onSelectMilestoneIndex = { selectedShareMilestoneIndex = it },
            selectedMilestoneIndex = selectedShareMilestoneIndex,
            context = context
        )

        // Motivational "Don't Break the Chain" & Famous Quotes Card
        MotivationalQuoteBannerCard(streakDays = days)

        // Emergency Panic Button for Urges
        Button(
            onClick = onOpenPanicMode,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("streak_panic_sos_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = CrimsonPrimary,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Emergency,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = tr(
                    "FEELING AN URGE? OPEN PANIC SHIELD",
                    "احساس وسوسه دارید؟ باز کردن سپر اضطراری پنیک"
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black
            )
        }

        // 3-Column Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ObsidianBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tr("BEST CHAIN", "بهترین رکورد"),
                            style = MaterialTheme.typography.labelSmall,
                            color = ObsidianTextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${maxOf(days, longestStreakDays)}d",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = ObsidianTextPrimary
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, EmeraldShield.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = EmeraldShield,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tr("URGES BEAT", "وسوسه مهارشده"),
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldShield,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$urgesDefeatedCount",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = ObsidianTextPrimary
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ObsidianBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tr("RESETS", "دفعات ریست"),
                            style = MaterialTheme.typography.labelSmall,
                            color = ObsidianTextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$slipsCount",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = ObsidianTextPrimary
                    )
                }
            }
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

        // Milestones List (Tap any milestone to preview its Predefined Share Card & Animation!)
        Text(
            text = tr(
                "Milestone Badges (Tap any badge to preview its Share Card)",
                "نشان‌های افتخار (روی هر نشان بزنید تا کارت اشتراک‌گذاری آن را ببینید)"
            ),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = ObsidianTextPrimary
        )

        STREAK_MILESTONES.forEachIndexed { idx, milestone ->
            val unlocked = days >= milestone.days
            MilestoneRowCard(
                milestone = milestone,
                unlocked = unlocked,
                isSelectedForShare = idx == selectedShareMilestoneIndex,
                onClick = { selectedShareMilestoneIndex = idx }
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

    if (showRelapseDialog) {
        var triggerNote by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showRelapseDialog = false },
            containerColor = ObsidianSurface,
            title = {
                Text(
                    text = tr("Reset Clean Streak?", "ریست کردن زنجیره روزهای پاک؟"),
                    color = ObsidianTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = tr(
                            "Recovery is a journey. Log what triggered the slip so you can block it and forge an even stronger chain:",
                            "مسیر پاکی نیازمند استقامت است. عامل لغزش را بنویسید تا زنجیره جدید را قوی‌تر بسازید:"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = ObsidianTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = triggerNote,
                        onValueChange = { triggerNote = it },
                        label = { Text(tr("Trigger note (optional)", "علت یا زمان لغزش (اختیاری)")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
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
                        onResetStreak(triggerNote.trim())
                        showRelapseDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    modifier = Modifier.testTag("confirm_reset_streak_button")
                ) {
                    Text(tr("Reset to Day 0", "ریست به روز ۰"), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRelapseDialog = false }) {
                    Text(tr("Cancel", "انصراف"), color = ObsidianTextSecondary)
                }
            }
        )
    }
}

/**
 * Predefined Shareable Visual Card & Live Animation for each Clean Streak Milestone,
 * with direct one-tap sharing to Instagram and X (Twitter).
 */
@Composable
fun PredefinedStreakMilestoneShareCard(
    currentStreakDays: Int,
    urgesDefeatedCount: Int,
    selectedMilestone: StreakMilestone,
    selectedMilestoneIndex: Int,
    onSelectMilestoneIndex: (Int) -> Unit,
    context: Context,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "milestone_share_anim")
    val haloRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo_rotation"
    )
    val badgePulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badge_pulse"
    )

    val displayedDays = maxOf(currentStreakDays, selectedMilestone.days)
    val quoteForCard = remember(selectedMilestoneIndex) {
        MotivationalQuotesRepository.getQuoteByIndex(selectedMilestoneIndex)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("streak_milestone_share_studio_card"),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.5.dp, selectedMilestone.badgeColor.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = selectedMilestone.badgeColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = tr(
                                "Share Clean Streak Card (Instagram / X)",
                                "کارت اشتراک‌گذاری رکورد پاکی (اینستاگرام و X)"
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = ObsidianTextPrimary
                        )
                        Text(
                            text = tr(
                                "Predefined artwork & live animation for every streak milestone",
                                "تصویر و انیمیشن اختصاصی برای هر مرحله از زنجیره پاکی"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = ObsidianTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Milestone Tier Selector Pills (1d, 3d, 7d, 14d, 30d, 60d, 90d)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                            modifier = Modifier.padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${ms.days}d",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = if (isSelected || isUnlocked) FontWeight.Black else FontWeight.Medium,
                                color = if (isSelected) ms.badgeColor else ObsidianTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Animated Visual Share Poster Preview (with generated img_streak_share_bg_1793985592091 artwork)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(2.dp, selectedMilestone.badgeColor, RoundedCornerShape(20.dp))
                    .testTag("streak_visual_poster_preview"),
                contentAlignment = Alignment.Center
            ) {
                // Background generated broken-chain shield crest artwork
                Image(
                    painter = painterResource(id = R.drawable.img_streak_share_bg_1793985592091),
                    contentDescription = "Streak Milestone Trophy Emblem",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Dark gradient overlay for crisp text legibility + milestone color aura
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xCC060911),
                                    selectedMilestone.secondaryGlowColor.copy(alpha = 0.35f),
                                    Color(0xEE060911)
                                )
                            )
                        )
                )

                // Animated Rotating Orbiting Particles & Energy Ring Canvas
                val primaryGlow = selectedMilestone.badgeColor
                val secondaryGlow = selectedMilestone.secondaryGlowColor
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f - 10.dp.toPx())
                    val radius = 78.dp.toPx()

                    drawCircle(
                        color = primaryGlow.copy(alpha = 0.22f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    drawArc(
                        color = primaryGlow,
                        startAngle = haloRotation,
                        sweepAngle = 110f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )

                    drawArc(
                        color = secondaryGlow,
                        startAngle = haloRotation + 180f,
                        sweepAngle = 110f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Orbiting stars
                    for (i in 0 until 6) {
                        val angleRad = Math.toRadians((haloRotation + i * 60f).toDouble())
                        val px = center.x + (radius + 12.dp.toPx()) * cos(angleRad).toFloat()
                        val py = center.y + (radius + 12.dp.toPx()) * sin(angleRad).toFloat()
                        drawCircle(
                            color = if (i % 2 == 0) primaryGlow else AmberWarning,
                            radius = 3.5.dp.toPx(),
                            center = Offset(px, py)
                        )
                    }
                }

                // Poster Foreground Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Banner Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xCC0F172A),
                        border = BorderStroke(1.dp, selectedMilestone.badgeColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = selectedMilestone.badgeColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tr(selectedMilestone.titleEn, selectedMilestone.titleFa),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }

                    // Center Pulsing Streak Badge
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.scale(badgePulse)
                    ) {
                        Text(
                            text = if (currentStreakDays >= selectedMilestone.days) {
                                "${currentStreakDays}D CLEAN"
                            } else {
                                "${selectedMilestone.days}D GOAL"
                            },
                            style = MaterialTheme.typography.headlineLarge,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = tr(selectedMilestone.shareMottoEn, selectedMilestone.shareMottoFa),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = selectedMilestone.badgeColor,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Bottom Quote & Branding Footer
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "\"${tr(quoteForCard.quoteEn, quoteForCard.quoteFa).take(95)}\"",
                            style = MaterialTheme.typography.labelSmall,
                            fontStyle = FontStyle.Italic,
                            color = Color(0xFFE2E8F0),
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "X BLOCKER • DON'T BREAK THE CHAIN • $displayedDays DAYS • $urgesDefeatedCount URGES DEFEATED",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberWarning
                        )
                    }
                }
            }

            // Share Buttons for Instagram and X (Twitter)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        StreakShareImageGenerator.shareMilestoneToSocial(
                            context = context,
                            milestone = selectedMilestone,
                            currentStreakDays = currentStreakDays,
                            urgesDefeatedCount = urgesDefeatedCount,
                            quoteText = tr(quoteForCard.quoteEn, quoteForCard.quoteFa),
                            quoteAuthor = tr(quoteForCard.authorEn, quoteForCard.authorFa),
                            targetPlatform = SocialSharePlatform.INSTAGRAM
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE1306C),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("share_streak_instagram_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tr("Share on Instagram", "اشتراک در اینستاگرام"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = {
                        StreakShareImageGenerator.shareMilestoneToSocial(
                            context = context,
                            milestone = selectedMilestone,
                            currentStreakDays = currentStreakDays,
                            urgesDefeatedCount = urgesDefeatedCount,
                            quoteText = tr(quoteForCard.quoteEn, quoteForCard.quoteFa),
                            quoteAuthor = tr(quoteForCard.authorEn, quoteForCard.authorFa),
                            targetPlatform = SocialSharePlatform.TWITTER_X
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanTelemetry,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("share_streak_twitter_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tr("Share on X / Twitter", "اشتراک در X / توییتر"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

enum class SocialSharePlatform(val packageName: String) {
    INSTAGRAM("com.instagram.android"),
    TWITTER_X("com.twitter.android")
}

object StreakShareImageGenerator {
    fun shareMilestoneToSocial(
        context: Context,
        milestone: StreakMilestone,
        currentStreakDays: Int,
        urgesDefeatedCount: Int,
        quoteText: String,
        quoteAuthor: String,
        targetPlatform: SocialSharePlatform
    ) {
        val shareCaption = buildString {
            append("🛡️ X Blocker • ${milestone.titleEn}\n")
            append("🔥 Clean Streak: $currentStreakDays Days (${milestone.shareMottoEn})\n")
            append("⚡ Urges Defeated: $urgesDefeatedCount\n")
            append("💬 \"$quoteText\" — $quoteAuthor\n")
            append("#DontBreakTheChain #XBlocker #SelfDiscipline")
        }

        try {
            val bitmap = renderSharePosterBitmap(
                context = context,
                milestone = milestone,
                currentStreakDays = currentStreakDays,
                urgesDefeatedCount = urgesDefeatedCount,
                quoteText = quoteText,
                quoteAuthor = quoteAuthor
            )
            val sharedDir = File(context.cacheDir, "shared_streaks").apply { mkdirs() }
            val imageFile = File(sharedDir, "x_blocker_streak_${milestone.days}d.png")
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )

            val targetedIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, shareCaption)
                setPackage(targetPlatform.packageName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val pm = context.packageManager
            if (targetedIntent.resolveActivity(pm) != null) {
                context.startActivity(targetedIntent)
            } else {
                val chooserIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_TEXT, shareCaption)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(
                    Intent.createChooser(chooserIntent, "Share Clean Streak Result").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
        } catch (_: Exception) {
            // Fallback to text share if bitmap file provider fails
            try {
                val textIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareCaption)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(
                    Intent.createChooser(textIntent, "Share Clean Streak Result").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            } catch (_: Exception) {
            }
        }
    }

    private fun renderSharePosterBitmap(
        context: Context,
        milestone: StreakMilestone,
        currentStreakDays: Int,
        urgesDefeatedCount: Int,
        quoteText: String,
        quoteAuthor: String
    ): Bitmap {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw background image if available
        val bgBitmap = runCatching {
            BitmapFactory.decodeResource(context.resources, R.drawable.img_streak_share_bg_1793985592091)
        }.getOrNull()

        if (bgBitmap != null) {
            val srcRect = Rect(0, 0, bgBitmap.width, bgBitmap.height)
            val dstRect = Rect(0, 0, width, height)
            canvas.drawBitmap(bgBitmap, srcRect, dstRect, null)
        }

        // Gradient overlay
        val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                0f,
                height.toFloat(),
                intArrayOf(0xDD060911.toInt(), 0x880B132B.toInt(), 0xEE060911.toInt()),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

        // Outer neon frame
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 10f
            color = milestone.badgeColor.toArgb()
        }
        canvas.drawRoundRect(RectF(40f, 40f, width - 40f, height - 40f), 48f, 48f, borderPaint)

        // Title Text
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 54f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("X BLOCKER • DON'T BREAK THE CHAIN", width / 2f, 150f, titlePaint)

        // Milestone Rank
        val rankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = milestone.badgeColor.toArgb()
            textSize = 64f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(milestone.titleEn.uppercase(Locale.US), width / 2f, 250f, rankPaint)

        // Center Streak Number
        val daysPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 150f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val shownDays = maxOf(currentStreakDays, milestone.days)
        canvas.drawText("${shownDays}D CLEAN", width / 2f, 700f, daysPaint)

        // Motto
        val mottoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = milestone.badgeColor.toArgb()
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(milestone.shareMottoEn, width / 2f, 790f, mottoPaint)

        // Quote
        val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE2E8F0.toInt()
            textSize = 34f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        val trimmedQuote = if (quoteText.length > 75) quoteText.take(72) + "..." else quoteText
        canvas.drawText("\"$trimmedQuote\"", width / 2f, 1080f, quotePaint)

        val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF59E0B.toInt()
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("— $quoteAuthor • Urges Defeated: $urgesDefeatedCount", width / 2f, 1145f, authorPaint)

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
    isSelectedForShare: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("milestone_row_${milestone.days}d"),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) ObsidianSurface else ObsidianCard.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            when {
                isSelectedForShare -> milestone.badgeColor
                unlocked -> milestone.badgeColor.copy(alpha = 0.5f)
                else -> ObsidianBorder
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (unlocked) milestone.badgeColor.copy(alpha = 0.2f)
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

            Spacer(modifier = Modifier.width(14.dp))

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

            if (isSelectedForShare) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = milestone.badgeColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = tr("PREVIEWING", "پیش‌نمایش"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = milestone.badgeColor,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else if (unlocked) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = milestone.badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = tr("UNLOCKED", "باز شده"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = milestone.badgeColor,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
