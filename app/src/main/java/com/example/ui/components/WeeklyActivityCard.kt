package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppUsageLimit
import com.example.data.model.BlockIncident
import com.example.data.preferences.ShieldPreferences
import com.example.ui.i18n.AppStrings.tr
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.CyanTelemetry
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianTextPrimary
import com.example.ui.theme.ObsidianTextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class DaySafetyStatus {
    CLEAN,
    URGE_DEFEATED,
    BLOCKED_INCIDENT
}

data class DailyActivityMetric(
    val daysAgo: Int,
    val dayShortLabel: String,
    val dateFormatted: String,
    val blocksCount: Int,
    val lockMinutesTotal: Int,
    val urgesDefeatedCount: Int,
    val trackedAppMinutes: Int,
    val status: DaySafetyStatus
)

object WeeklyActivityCalculator {
    fun buildWeeklyMetrics(
        incidents: List<BlockIncident>,
        appLimits: List<AppUsageLimit>,
        preferences: ShieldPreferences?,
        nowMillis: Long = System.currentTimeMillis()
    ): List<DailyActivityMetric> {
        val todayEpochDay = TimeUnit.MILLISECONDS.toDays(nowMillis)
        // Strictly use user-configured appLimits with NO predefined fallback example apps
        val trackedPackages = appLimits.map { it.packageName }

        val dayLabelFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

        return (6 downTo 0).map { daysAgo ->
            val targetEpochDay = todayEpochDay - daysAgo
            val cal = Calendar.getInstance().apply {
                timeInMillis = nowMillis - TimeUnit.DAYS.toMillis(daysAgo.toLong())
            }
            val dayDate = Date(cal.timeInMillis)

            val dayIncidents = incidents.filter { inc ->
                TimeUnit.MILLISECONDS.toDays(inc.timestamp) == targetEpochDay
            }
            val blocksCount = dayIncidents.size
            val lockMinutes = dayIncidents.sumOf { it.lockDurationMinutes }
            val urgesDefeated = preferences?.getDayUrgesDefeated(daysAgo) ?: 0
            val appMinutes = preferences?.getDayAppUsageMinutes(daysAgo, trackedPackages) ?: 0

            val status = when {
                blocksCount > 0 -> DaySafetyStatus.BLOCKED_INCIDENT
                urgesDefeated > 0 -> DaySafetyStatus.URGE_DEFEATED
                else -> DaySafetyStatus.CLEAN
            }

            DailyActivityMetric(
                daysAgo = daysAgo,
                dayShortLabel = if (daysAgo == 0) tr("Today", "امروز") else dayLabelFormat.format(dayDate),
                dateFormatted = dateFormat.format(dayDate),
                blocksCount = blocksCount,
                lockMinutesTotal = lockMinutes,
                urgesDefeatedCount = urgesDefeated,
                trackedAppMinutes = appMinutes,
                status = status
            )
        }
    }
}

@Composable
fun WeeklyActivityCard(
    incidents: List<BlockIncident>,
    appLimits: List<AppUsageLimit>,
    preferences: ShieldPreferences?,
    usageRefreshTrigger: Long = 0L,
    modifier: Modifier = Modifier
) {
    val metrics = remember(incidents, appLimits, usageRefreshTrigger, com.example.ui.theme.AppThemeState.isPersianLanguage) {
        WeeklyActivityCalculator.buildWeeklyMetrics(
            incidents = incidents,
            appLimits = appLimits,
            preferences = preferences
        )
    }

    var selectedIndex by remember { mutableIntStateOf(metrics.lastIndex.coerceAtLeast(0)) }
    val selectedDay = metrics.getOrNull(selectedIndex) ?: metrics.last()

    val cleanDaysCount = metrics.count { it.blocksCount == 0 }
    val weeklyBlocksTotal = metrics.sumOf { it.blocksCount }
    val weeklyUrgesTotal = metrics.sumOf { it.urgesDefeatedCount }
    val weeklyAppUsageMins = metrics.sumOf { it.trackedAppMinutes }

    val maxBarUnits = remember(metrics) {
        metrics.maxOfOrNull { day ->
            (day.blocksCount * 25) + (day.urgesDefeatedCount * 15) + (day.trackedAppMinutes / 2)
        }?.coerceAtLeast(30) ?: 30
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_activity_card"),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(CyanTelemetry.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = CyanTelemetry,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = tr("Weekly Activity Status", "وضعیت فعالیت هفتگی"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianTextPrimary
                        )
                        Text(
                            text = tr("Real-time 7-day activity (no preset examples)", "آمار واقعی ۷ روز گذشته (بدون داده‌های پیش‌فرض)"),
                            style = MaterialTheme.typography.labelSmall,
                            color = ObsidianTextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (cleanDaysCount >= 5) EmeraldShield.copy(alpha = 0.16f)
                    else AmberWarning.copy(alpha = 0.16f),
                    border = BorderStroke(
                        1.dp,
                        if (cleanDaysCount >= 5) EmeraldShield.copy(alpha = 0.45f)
                        else AmberWarning.copy(alpha = 0.45f)
                    )
                ) {
                    Text(
                        text = tr("$cleanDaysCount/7 CLEAN", "$cleanDaysCount/7 روز پاک"),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = if (cleanDaysCount >= 5) EmeraldShield else AmberWarning,
                        fontSize = 10.sp,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Summary KPI Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WeeklyKpiPill(
                    label = tr("CLEAN DAYS", "روزهای پاک"),
                    value = "$cleanDaysCount / 7",
                    accent = EmeraldShield,
                    modifier = Modifier.weight(1f)
                )
                WeeklyKpiPill(
                    label = tr("7D BLOCKS", "مسدودسازی ۷ روز"),
                    value = "$weeklyBlocksTotal",
                    accent = if (weeklyBlocksTotal == 0) EmeraldShield else CrimsonPrimary,
                    modifier = Modifier.weight(1f)
                )
                WeeklyKpiPill(
                    label = tr("URGES WON", "غلبه بر وسوسه"),
                    value = "$weeklyUrgesTotal",
                    accent = CyanTelemetry,
                    modifier = Modifier.weight(1f)
                )
                WeeklyKpiPill(
                    label = tr("LIMITED APPS", "اپ‌های محدود"),
                    value = if (weeklyAppUsageMins >= 60) "${weeklyAppUsageMins / 60}h ${weeklyAppUsageMins % 60}m" else "${weeklyAppUsageMins}m",
                    accent = AmberWarning,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 7-Day Interactive Bar Chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianCard.copy(alpha = 0.7f))
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                metrics.forEachIndexed { index, metric ->
                    val isSelected = index == selectedIndex
                    val rawUnits = (metric.blocksCount * 25) +
                            (metric.urgesDefeatedCount * 15) +
                            (metric.trackedAppMinutes / 2)
                    val fillRatio = if (rawUnits == 0) {
                        0.15f
                    } else {
                        (rawUnits.toFloat() / maxBarUnits.toFloat()).coerceIn(0.22f, 1f)
                    }

                    val barColors = when (metric.status) {
                        DaySafetyStatus.BLOCKED_INCIDENT -> listOf(CrimsonPrimary, CrimsonPrimary.copy(alpha = 0.55f))
                        DaySafetyStatus.URGE_DEFEATED -> listOf(CyanTelemetry, EmeraldShield)
                        DaySafetyStatus.CLEAN -> listOf(EmeraldShield, EmeraldShield.copy(alpha = 0.45f))
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) ObsidianBorder.copy(alpha = 0.55f) else Color.Transparent
                            )
                            .clickable { selectedIndex = index }
                            .padding(vertical = 4.dp)
                            .testTag("weekly_day_bar_$index"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = when {
                                metric.blocksCount > 0 -> "${metric.blocksCount}!"
                                metric.urgesDefeatedCount > 0 -> "+${metric.urgesDefeatedCount}"
                                else -> "✓"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (metric.status) {
                                DaySafetyStatus.BLOCKED_INCIDENT -> CrimsonPrimary
                                DaySafetyStatus.URGE_DEFEATED -> CyanTelemetry
                                DaySafetyStatus.CLEAN -> EmeraldShield
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .weight(1f),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(fillRatio)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                    .background(Brush.verticalGradient(barColors))
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = metric.dayShortLabel.take(3),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            fontSize = 10.sp,
                            color = if (isSelected) ObsidianTextPrimary else ObsidianTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Selected Day Breakdown Strip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("weekly_selected_day_details")
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
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = when (selectedDay.status) {
                                DaySafetyStatus.BLOCKED_INCIDENT -> Icons.Default.Warning
                                DaySafetyStatus.URGE_DEFEATED -> Icons.Default.Shield
                                DaySafetyStatus.CLEAN -> Icons.Default.CheckCircle
                            },
                            contentDescription = null,
                            tint = when (selectedDay.status) {
                                DaySafetyStatus.BLOCKED_INCIDENT -> CrimsonPrimary
                                DaySafetyStatus.URGE_DEFEATED -> CyanTelemetry
                                DaySafetyStatus.CLEAN -> EmeraldShield
                            },
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "${selectedDay.dayShortLabel} (${selectedDay.dateFormatted})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianTextPrimary
                            )
                            Text(
                                text = when {
                                    selectedDay.blocksCount > 0 ->
                                        tr(
                                            "${selectedDay.blocksCount} blocked trigger(s) • ${selectedDay.lockMinutesTotal}m lockdown",
                                            "${selectedDay.blocksCount} مورد مسدود شده • ${selectedDay.lockMinutesTotal} دقیقه قفل"
                                        )
                                    selectedDay.urgesDefeatedCount > 0 ->
                                        tr(
                                            "100% Clean • ${selectedDay.urgesDefeatedCount} urge(s) defeated",
                                            "کاملاً پاک • غلبه بر ${selectedDay.urgesDefeatedCount} وسوسه"
                                        )
                                    else ->
                                        tr("100% Clean Day • Zero triggers", "روز کاملاً پاک • بدون هیچ تخلف")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = ObsidianTextSecondary
                            )
                        }
                    }

                    Text(
                        text = tr("${selectedDay.trackedAppMinutes}m apps", "${selectedDay.trackedAppMinutes} دقیقه اپ"),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = AmberWarning
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyKpiPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianCard)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 8.sp,
            color = ObsidianTextSecondary,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = ObsidianTextPrimary,
                maxLines = 1
            )
        }
    }
}
