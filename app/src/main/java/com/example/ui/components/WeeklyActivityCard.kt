package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppUsageLimit
import com.example.data.model.BlockIncident
import com.example.data.preferences.ShieldPreferences
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.CyanTelemetry
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianTextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class DayActivityMetric(
    val daysAgo: Int,
    val dayShortLabel: String,
    val fullDateLabel: String,
    val isToday: Boolean,
    val blockedCount: Int,
    val urgesDefeated: Int,
    val screenTimeMinutes: Int,
    val isCleanDay: Boolean
)

object WeeklyActivityCalculator {
    fun buildWeeklyMetrics(
        incidents: List<BlockIncident>,
        appLimits: List<AppUsageLimit>,
        preferences: ShieldPreferences,
        usageRefreshTrigger: Long = 0L
    ): List<DayActivityMetric> {
        val trackedPackages = appLimits.map { it.packageName }.ifEmpty {
            listOf(
                "com.instagram.android",
                "org.telegram.messenger",
                "com.google.android.youtube",
                "com.zhiliaoapp.musically"
            )
        }
        val dayNameFormat = SimpleDateFormat("EEE", Locale.US)
        val fullDateFormat = SimpleDateFormat("EEE, MMM d", Locale.US)
        val nowMillis = System.currentTimeMillis()
        val todayEpochDay = TimeUnit.MILLISECONDS.toDays(nowMillis)
        val streakStartEpochDay = TimeUnit.MILLISECONDS.toDays(preferences.getStreakStartTimestamp())

        // Group incidents by epoch day
        val incidentsByEpochDay = incidents.groupBy {
            TimeUnit.MILLISECONDS.toDays(it.timestamp)
        }

        // Build from 6 days ago (index 0) to Today (index 6)
        return (6 downTo 0).map { daysAgo ->
            val targetEpochDay = todayEpochDay - daysAgo
            val cal = Calendar.getInstance().apply {
                timeInMillis = nowMillis - TimeUnit.DAYS.toMillis(daysAgo.toLong())
            }
            val date = Date(cal.timeInMillis)
            val blocks = incidentsByEpochDay[targetEpochDay]?.size ?: 0
            val urges = preferences.getDayUrgesDefeated(daysAgo)
            val usageMins = preferences.getDayAppUsageMinutes(daysAgo, trackedPackages)
            val isClean = blocks == 0 && targetEpochDay >= streakStartEpochDay

            val shortDay = dayNameFormat.format(date).take(3)
            DayActivityMetric(
                daysAgo = daysAgo,
                dayShortLabel = if (daysAgo == 0) "Today" else shortDay,
                fullDateLabel = if (daysAgo == 0) "Today • ${fullDateFormat.format(date)}" else fullDateFormat.format(date),
                isToday = daysAgo == 0,
                blockedCount = blocks,
                urgesDefeated = urges,
                screenTimeMinutes = usageMins,
                isCleanDay = isClean
            )
        }
    }

    fun calculateDisciplineScore(days: List<DayActivityMetric>): Int {
        if (days.isEmpty()) return 100
        val cleanDays = days.count { it.blockedCount == 0 }
        val totalBlocks = days.sumOf { it.blockedCount }
        val totalUrgesDefeated = days.sumOf { it.urgesDefeated }
        val baseScore = (cleanDays * 100) / days.size
        val urgeBonus = (totalUrgesDefeated * 3).coerceAtMost(15)
        val blockPenalty = (totalBlocks * 6).coerceAtMost(40)
        return (baseScore + urgeBonus - blockPenalty).coerceIn(10, 100)
    }
}

@Composable
fun WeeklyActivityCard(
    incidents: List<BlockIncident>,
    appLimits: List<AppUsageLimit>,
    preferences: ShieldPreferences,
    usageRefreshTrigger: Long = 0L,
    modifier: Modifier = Modifier
) {
    val weeklyDays = remember(incidents, appLimits, usageRefreshTrigger) {
        WeeklyActivityCalculator.buildWeeklyMetrics(
            incidents = incidents,
            appLimits = appLimits,
            preferences = preferences,
            usageRefreshTrigger = usageRefreshTrigger
        )
    }

    val disciplineScore = remember(weeklyDays) {
        WeeklyActivityCalculator.calculateDisciplineScore(weeklyDays)
    }

    val cleanDaysCount = remember(weeklyDays) { weeklyDays.count { it.blockedCount == 0 } }
    val totalWeeklyBlocks = remember(weeklyDays) { weeklyDays.sumOf { it.blockedCount } }
    val totalWeeklyUrges = remember(weeklyDays) { weeklyDays.sumOf { it.urgesDefeated } }
    val totalWeeklyMinutes = remember(weeklyDays) { weeklyDays.sumOf { it.screenTimeMinutes } }

    // 0 = Shield & Clean Status View, 1 = App Screen Time View
    var chartMode by remember { mutableIntStateOf(0) }
    var selectedDayIndex by remember { mutableIntStateOf(weeklyDays.lastIndex.coerceAtLeast(0)) }
    val selectedDay = weeklyDays.getOrNull(selectedDayIndex) ?: weeklyDays.last()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_activity_card"),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            CyanTelemetry.copy(alpha = 0.08f),
                            IndigoAccent.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Header Row: Title (weighted) + Weekly Score Badge (never wraps vertically)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            CyanTelemetry.copy(alpha = 0.25f),
                                            IndigoAccent.copy(alpha = 0.25f)
                                        )
                                    )
                                )
                                .border(1.dp, CyanTelemetry.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = CyanTelemetry,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Weekly Activity Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "7-day discipline & screen time",
                                style = MaterialTheme.typography.bodySmall,
                                color = ObsidianTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Discipline Score Badge
                    val scoreColor = if (disciplineScore >= 80) EmeraldShield else AmberWarning
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = scoreColor.copy(alpha = 0.16f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, scoreColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = scoreColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$disciplineScore% Score",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = scoreColor,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Mode Switcher Pills (Protection Activity vs Screen Time)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianCard)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ModeTabButton(
                        title = "Shield & Streak",
                        icon = Icons.Default.BarChart,
                        isSelected = chartMode == 0,
                        accentColor = EmeraldShield,
                        onClick = { chartMode = 0 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("weekly_tab_shield")
                    )
                    ModeTabButton(
                        title = "App Screen Time",
                        icon = Icons.Default.Schedule,
                        isSelected = chartMode == 1,
                        accentColor = CyanTelemetry,
                        onClick = { chartMode = 1 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("weekly_tab_screentime")
                    )
                }

                // 2x2 Bento Summary Grid (Spacious, never wraps or clips on compact screens)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WeeklyMiniStat(
                            label = "Clean Days",
                            value = "$cleanDaysCount / 7",
                            color = EmeraldShield,
                            modifier = Modifier.weight(1f)
                        )
                        WeeklyMiniStat(
                            label = "Blocked Triggers",
                            value = "$totalWeeklyBlocks",
                            color = if (totalWeeklyBlocks == 0) EmeraldShield else CrimsonPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WeeklyMiniStat(
                            label = "Urges Defeated",
                            value = "$totalWeeklyUrges",
                            color = IndigoAccent,
                            modifier = Modifier.weight(1f)
                        )
                        WeeklyMiniStat(
                            label = "7-Day Screen Time",
                            value = if (totalWeeklyMinutes >= 60) {
                                "${totalWeeklyMinutes / 60}h ${totalWeeklyMinutes % 60}m"
                            } else {
                                "${totalWeeklyMinutes}m"
                            },
                            color = CyanTelemetry,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Interactive 7-Day Bar Chart + Legend
                val maxUsageMins = remember(weeklyDays) {
                    weeklyDays.maxOfOrNull { it.screenTimeMinutes }?.coerceAtLeast(60) ?: 60
                }
                val maxActivityEvents = remember(weeklyDays) {
                    weeklyDays.maxOfOrNull { it.blockedCount + it.urgesDefeated }?.coerceAtLeast(3) ?: 3
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ObsidianCard.copy(alpha = 0.7f))
                        .border(1.dp, ObsidianBorder.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(142.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        weeklyDays.forEachIndexed { idx, day ->
                            val isSelected = idx == selectedDayIndex

                            val rawFraction = if (chartMode == 0) {
                                if (day.blockedCount == 0 && day.urgesDefeated == 0) {
                                    0.72f
                                } else {
                                    ((day.blockedCount + day.urgesDefeated).toFloat() / maxActivityEvents.toFloat())
                                        .coerceIn(0.32f, 1f)
                                }
                            } else {
                                if (day.screenTimeMinutes == 0) 0.14f
                                else (day.screenTimeMinutes.toFloat() / maxUsageMins.toFloat()).coerceIn(0.22f, 1f)
                            }

                            val animatedFraction by animateFloatAsState(
                                targetValue = rawFraction,
                                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                label = "bar_height_$idx"
                            )

                            val barColor by animateColorAsState(
                                targetValue = when {
                                    chartMode == 1 -> if (day.screenTimeMinutes > 120) CrimsonPrimary else CyanTelemetry
                                    day.blockedCount > 0 -> CrimsonPrimary
                                    day.urgesDefeated > 0 -> IndigoAccent
                                    else -> EmeraldShield
                                },
                                label = "bar_color_$idx"
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) ObsidianElevated.copy(alpha = 0.95f)
                                        else Color.Transparent
                                    )
                                    .border(
                                        width = if (isSelected) 1.dp else 0.dp,
                                        color = if (isSelected) barColor.copy(alpha = 0.6f) else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedDayIndex = idx }
                                    .padding(vertical = 6.dp, horizontal = 2.dp)
                                    .testTag("weekly_day_bar_$idx"),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                // Top value badge (never wraps)
                                Text(
                                    text = if (chartMode == 0) {
                                        when {
                                            day.blockedCount > 0 -> "${day.blockedCount}!"
                                            day.urgesDefeated > 0 -> "+${day.urgesDefeated}"
                                            else -> "✓"
                                        }
                                    } else {
                                        if (day.screenTimeMinutes >= 60) {
                                            "${day.screenTimeMinutes / 60}h"
                                        } else {
                                            "${day.screenTimeMinutes}m"
                                        }
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else barColor,
                                    maxLines = 1,
                                    softWrap = false
                                )

                                Spacer(modifier = Modifier.height(5.dp))

                                // Bar track + animated fill
                                Box(
                                    modifier = Modifier
                                        .width(20.dp)
                                        .weight(1f),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(20.dp)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(ObsidianSurface.copy(alpha = 0.85f))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(20.dp)
                                            .fillMaxHeight(animatedFraction)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        barColor,
                                                        barColor.copy(alpha = 0.5f)
                                                    )
                                                )
                                            )
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Day label (shows full "Today" or 3-letter weekday, never truncates to "Tod")
                                Text(
                                    text = day.dayShortLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (day.isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 10.sp,
                                    color = when {
                                        isSelected -> Color.White
                                        day.isToday -> CyanTelemetry
                                        else -> ObsidianTextSecondary
                                    },
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Legend Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (chartMode == 0) {
                            LegendDotItem(color = EmeraldShield, label = "Clean Day")
                            Spacer(modifier = Modifier.width(14.dp))
                            LegendDotItem(color = IndigoAccent, label = "Urge Beat")
                            Spacer(modifier = Modifier.width(14.dp))
                            LegendDotItem(color = CrimsonPrimary, label = "Blocked")
                        } else {
                            LegendDotItem(color = CyanTelemetry, label = "Within Limit")
                            Spacer(modifier = Modifier.width(16.dp))
                            LegendDotItem(color = CrimsonPrimary, label = "High Screen Time (>2h)")
                        }
                    }
                }

                // Selected Day Detailed Telemetry Strip (weighted left column so right badge never squishes)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ObsidianCard,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selectedDay.blockedCount > 0) CrimsonPrimary.copy(alpha = 0.5f)
                        else EmeraldShield.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selectedDay.blockedCount > 0) CrimsonPrimary.copy(alpha = 0.2f)
                                        else EmeraldShield.copy(alpha = 0.2f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when {
                                        selectedDay.blockedCount > 0 -> Icons.Default.Warning
                                        selectedDay.urgesDefeated > 0 -> Icons.Default.Verified
                                        else -> Icons.Default.CheckCircle
                                    },
                                    contentDescription = null,
                                    tint = when {
                                        selectedDay.blockedCount > 0 -> CrimsonPrimary
                                        selectedDay.urgesDefeated > 0 -> IndigoAccent
                                        else -> EmeraldShield
                                    },
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedDay.fullDateLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (selectedDay.blockedCount == 0)
                                        "100% Clean Shield • ${selectedDay.urgesDefeated} urges beat"
                                    else
                                        "${selectedDay.blockedCount} blocked • ${selectedDay.urgesDefeated} urges beat",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ObsidianTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ObsidianElevated,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                CyanTelemetry.copy(alpha = 0.35f)
                            )
                        ) {
                            Text(
                                text = "${selectedDay.screenTimeMinutes}m used",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = CyanTelemetry,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDotItem(
    color: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ObsidianTextSecondary,
            fontSize = 10.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun ModeTabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 40.dp),
        shape = RoundedCornerShape(9.dp),
        color = if (isSelected) ObsidianElevated else Color.Transparent,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(
            1.dp,
            accentColor.copy(alpha = 0.6f)
        ) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) accentColor else ObsidianTextSecondary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else ObsidianTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WeeklyMiniStat(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = ObsidianCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = ObsidianTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
