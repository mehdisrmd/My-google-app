package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppUsageLimit
import com.example.data.model.BlockIncident
import com.example.data.preferences.ShieldPreferences
import com.example.ui.i18n.AppStrings.tr
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanTelemetry
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianTextPrimary
import com.example.ui.theme.ObsidianTextSecondary
import com.example.ui.theme.StreakCleanGreen
import com.example.ui.theme.StreakRelapseRed
import com.example.ui.theme.StreakTodayGray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class CalendarDayStatus {
    PRE_INSTALL_GRAY,
    TODAY_IN_PROGRESS,
    CLEAN_STREAK_DAY,
    RELAPSE_DAY
}

data class DailyStreakCalendarNode(
    val daysAgo: Int,
    val dayShortEn: String,
    val dayShortFa: String,
    val dateNumber: String,
    val fullDateLabel: String,
    val status: CalendarDayStatus,
    val isToday: Boolean,
    val urgesDefeated: Int
)

data class DailyActivityMetric(
    val daysAgo: Int,
    val dayShortLabel: String,
    val trackedAppMinutes: Int,
    val status: CalendarDayStatus
)

object WeeklyActivityCalculator {
    fun buildWeeklyMetrics(
        incidents: List<BlockIncident>,
        appLimits: List<AppUsageLimit>,
        preferences: ShieldPreferences?
    ): List<DailyActivityMetric> {
        val dayFmtEn = SimpleDateFormat("EEE", Locale.US)
        val pkgList = appLimits.map { it.packageName }

        return (6 downTo 0).map { daysAgo ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
            val label = if (daysAgo == 0) "Today" else dayFmtEn.format(cal.time)
            val mins = preferences?.getDayAppUsageMinutes(daysAgo, pkgList) ?: 0
            val status = if (preferences == null) {
                if (daysAgo == 0) CalendarDayStatus.TODAY_IN_PROGRESS else CalendarDayStatus.PRE_INSTALL_GRAY
            } else {
                val nowMs = System.currentTimeMillis()
                val todayEpochDay = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(nowMs)
                StreakDayClassifier.classify(
                    dayEpochDay = todayEpochDay - daysAgo,
                    todayEpochDay = todayEpochDay,
                    firstInstallEpochDay = preferences.getFirstInstallEpochDay(),
                    relapseEpochDays = preferences.getRelapseEpochDaysInRange(
                        todayEpochDay - 6,
                        todayEpochDay
                    )
                )
            }
            DailyActivityMetric(
                daysAgo = daysAgo,
                dayShortLabel = label,
                trackedAppMinutes = mins,
                status = status
            )
        }
    }
}

/**
 * Pure day classification for the streak calendar (Feature: Streak Calendar
 * Colors). Mapping, exactly as the owner asked:
 *  - TODAY is always "in progress" (rendered GRAY) — even if a relapse was
 *    logged earlier today; the day is not over yet.
 *  - A past day with a recorded reset/relapse is a RELAPSE day (RED).
 *  - Any other past day since install counts as a passed clean day (GREEN):
 *    the streak clock runs continuously from install/reset, so every day
 *    that ended without a relapse was a day the user passed.
 *  - Days before the app was installed are PRE-INSTALL (muted gray).
 */
object StreakDayClassifier {
    fun classify(
        dayEpochDay: Long,
        todayEpochDay: Long,
        firstInstallEpochDay: Long,
        relapseEpochDays: Set<Long>
    ): CalendarDayStatus {
        return when {
            dayEpochDay > todayEpochDay -> CalendarDayStatus.PRE_INSTALL_GRAY
            dayEpochDay == todayEpochDay -> CalendarDayStatus.TODAY_IN_PROGRESS
            dayEpochDay < firstInstallEpochDay -> CalendarDayStatus.PRE_INSTALL_GRAY
            relapseEpochDays.contains(dayEpochDay) -> CalendarDayStatus.RELAPSE_DAY
            else -> CalendarDayStatus.CLEAN_STREAK_DAY
        }
    }
}

@Composable
fun WeeklyActivityCard(
    incidents: List<BlockIncident>,
    appLimits: List<AppUsageLimit>,
    preferences: ShieldPreferences,
    usageRefreshTrigger: Long,
    modifier: Modifier = Modifier
) {
    val currentStreakDays = preferences.getStreakDays()
    val bestChainDays = preferences.getLongestStreakDays()
    val coinsBalance = preferences.getCoinsBalance()

    val calendarNodes = remember(incidents, appLimits, usageRefreshTrigger, currentStreakDays) {
        val dayFmtEn = SimpleDateFormat("EEE", Locale.US)
        val dateNumFmt = SimpleDateFormat("d", Locale.US)
        val fullFmt = SimpleDateFormat("MMM d", Locale.US)

        (6 downTo 0).map { daysAgo ->
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }
            val dayShortEn = dayFmtEn.format(cal.time)
            val dayShortFa = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SATURDAY -> "ش"
                Calendar.SUNDAY -> "ی"
                Calendar.MONDAY -> "د"
                Calendar.TUESDAY -> "س"
                Calendar.WEDNESDAY -> "چ"
                Calendar.THURSDAY -> "پ"
                Calendar.FRIDAY -> "ج"
                else -> dayShortEn
            }
            // Classification (Streak Calendar Colors): TODAY = gray,
            // passed clean day = green, relapse day = red, pre-install = muted.
            val todayEpochDay = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(
                System.currentTimeMillis()
            )
            val status = StreakDayClassifier.classify(
                dayEpochDay = todayEpochDay - daysAgo,
                todayEpochDay = todayEpochDay,
                firstInstallEpochDay = preferences.getFirstInstallEpochDay(),
                relapseEpochDays = preferences.getRelapseEpochDaysInRange(
                    todayEpochDay - 6,
                    todayEpochDay
                )
            )
            val urges = preferences.getDayUrgesDefeated(daysAgo)

            DailyStreakCalendarNode(
                daysAgo = daysAgo,
                dayShortEn = dayShortEn,
                dayShortFa = dayShortFa,
                dateNumber = dateNumFmt.format(cal.time),
                fullDateLabel = fullFmt.format(cal.time),
                status = status,
                isToday = daysAgo == 0,
                urgesDefeated = urges
            )
        }
    }

    var selectedIndex by remember { mutableIntStateOf(6) } // Default to Today
    val selectedNode = calendarNodes.getOrElse(selectedIndex) { calendarNodes.last() }

    val cleanDaysInWeek = calendarNodes.count { it.status == CalendarDayStatus.CLEAN_STREAK_DAY }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_activity_card"),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.5.dp, AmberWarning.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Duolingo-style Streak Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        AmberWarning.copy(alpha = 0.30f),
                                        EmeraldShield.copy(alpha = 0.12f)
                                    )
                                )
                            )
                            .border(1.5.dp, AmberWarning, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak Calendar",
                            tint = AmberWarning,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = tr("Clean Streak Calendar", "تقویم زنجیره پاکی (سبک دولینگو)"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = ObsidianTextPrimary
                        )
                        Text(
                            text = if (cleanDaysInWeek == 0) {
                                tr(
                                    "0 clean days • Starting from Day 0 (Pre-install gray)",
                                    "۰ روز پاک • شروع از روز صفر (روزهای قبل خاکستری)"
                                )
                            } else {
                                tr(
                                    "$cleanDaysInWeek clean day(s) this week • Pre-install days are gray",
                                    "$cleanDaysInWeek روز پاک در این هفته • روزهای قبل از نصب خاکستری هستند"
                                )
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = ObsidianTextSecondary
                        )
                    }
                }

                // Coins + Best Chain Pill
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AmberWarning.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$coinsBalance",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Black,
                                color = AmberWarning
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EmeraldShield.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, EmeraldShield.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = EmeraldShield,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tr("Best: ${bestChainDays}d", "رکورد: ${bestChainDays} روز"),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Black,
                                color = EmeraldShield
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Duolingo-Style 7-Day Connected Streak Calendar Strip
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder)
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        calendarNodes.forEachIndexed { index, node ->
                            val isSelected = index == selectedIndex
                            val isPreInstall = node.status == CalendarDayStatus.PRE_INSTALL_GRAY

                            val circleBgColor = when (node.status) {
                                CalendarDayStatus.PRE_INSTALL_GRAY -> ObsidianBorder.copy(alpha = 0.45f)
                                CalendarDayStatus.TODAY_IN_PROGRESS -> StreakTodayGray.copy(alpha = 0.20f)
                                CalendarDayStatus.CLEAN_STREAK_DAY -> StreakCleanGreen
                                CalendarDayStatus.RELAPSE_DAY -> StreakRelapseRed.copy(alpha = 0.22f)
                            }

                            val circleBorderColor = when {
                                node.status == CalendarDayStatus.TODAY_IN_PROGRESS -> StreakTodayGray
                                isSelected -> CyanTelemetry
                                node.status == CalendarDayStatus.PRE_INSTALL_GRAY -> ObsidianBorder
                                node.status == CalendarDayStatus.CLEAN_STREAK_DAY -> StreakCleanGreen
                                else -> StreakRelapseRed
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedIndex = index }
                                    .padding(vertical = 4.dp)
                                    .testTag("weekly_day_bar_$index"),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Day of Week Label (M, T, W...)
                                Text(
                                    text = if (node.isToday) {
                                        tr("TODAY", "امروز")
                                    } else {
                                        tr(node.dayShortEn.take(3).uppercase(), node.dayShortFa)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = if (node.isToday || isSelected) FontWeight.Black else FontWeight.SemiBold,
                                    color = when {
                                        node.isToday -> StreakTodayGray
                                        isPreInstall -> ObsidianTextSecondary.copy(alpha = 0.55f)
                                        else -> ObsidianTextSecondary
                                    },
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Duolingo-style Streak Circle
                                Box(
                                    modifier = Modifier
                                        .size(if (node.isToday) 40.dp else 36.dp)
                                        .clip(CircleShape)
                                        .background(circleBgColor)
                                        .border(
                                            width = if (node.isToday || isSelected) 2.5.dp else 1.5.dp,
                                            color = circleBorderColor,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (node.status) {
                                        CalendarDayStatus.CLEAN_STREAK_DAY -> {
                                            Icon(
                                                imageVector = if (node.isToday) {
                                                    Icons.Default.LocalFireDepartment
                                                } else {
                                                    Icons.Default.Check
                                                },
                                                contentDescription = "Clean Day",
                                                tint = Color.Black,
                                                modifier = Modifier.size(if (node.isToday) 22.dp else 19.dp)
                                            )
                                        }

                                        CalendarDayStatus.TODAY_IN_PROGRESS -> {
                                            Text(
                                                text = "${currentStreakDays}d",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp,
                                                color = StreakTodayGray
                                            )
                                        }

                                        CalendarDayStatus.PRE_INSTALL_GRAY -> {
                                            Icon(
                                                imageVector = Icons.Default.Remove,
                                                contentDescription = "Before Install",
                                                tint = ObsidianTextSecondary.copy(alpha = 0.55f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        CalendarDayStatus.RELAPSE_DAY -> {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Streak Reset",
                                                tint = StreakRelapseRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(5.dp))

                                // Calendar Date Number
                                Text(
                                    text = node.dateNumber,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                    color = if (isPreInstall) {
                                        ObsidianTextSecondary.copy(alpha = 0.5f)
                                    } else {
                                        ObsidianTextPrimary
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Selected Day Summary Strip
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = ObsidianSurface,
                        border = BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (selectedNode.status) {
                                                CalendarDayStatus.CLEAN_STREAK_DAY -> StreakCleanGreen
                                                CalendarDayStatus.TODAY_IN_PROGRESS -> StreakTodayGray
                                                CalendarDayStatus.PRE_INSTALL_GRAY -> ObsidianTextSecondary.copy(alpha = 0.5f)
                                                CalendarDayStatus.RELAPSE_DAY -> StreakRelapseRed
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (selectedNode.isToday) {
                                        tr("Today (${selectedNode.fullDateLabel})", "امروز (${selectedNode.fullDateLabel})")
                                    } else {
                                        "${selectedNode.dayShortEn} (${selectedNode.fullDateLabel})"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ObsidianTextPrimary
                                )
                            }

                            val statusText = when (selectedNode.status) {
                                CalendarDayStatus.CLEAN_STREAK_DAY -> tr(
                                    "✓ Clean Day (+1 Coin)",
                                    "✓ روز پاک (+۱ سکه)"
                                )
                                CalendarDayStatus.TODAY_IN_PROGRESS -> tr(
                                    "Today • In Progress (Day $currentStreakDays)",
                                    "امروز • در حال انجام (روز $currentStreakDays)"
                                )
                                CalendarDayStatus.PRE_INSTALL_GRAY -> tr(
                                    "Before App Install (Gray)",
                                    "قبل از نصب برنامه (خاکستری)"
                                )
                                CalendarDayStatus.RELAPSE_DAY -> tr(
                                    "Streak Reset Logged",
                                    "ثبت شروع مجدد زنجیره"
                                )
                            }

                            val statusColor = when (selectedNode.status) {
                                CalendarDayStatus.CLEAN_STREAK_DAY -> StreakCleanGreen
                                CalendarDayStatus.TODAY_IN_PROGRESS -> StreakTodayGray
                                CalendarDayStatus.PRE_INSTALL_GRAY -> ObsidianTextSecondary
                                CalendarDayStatus.RELAPSE_DAY -> StreakRelapseRed
                            }

                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = statusColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDotItem(
                    color = StreakCleanGreen,
                    label = tr("Clean Day (+1🪙)", "روز پاک (+۱ سکه)")
                )
                LegendDotItem(
                    color = StreakRelapseRed,
                    label = tr("Relapse Day", "روز لغزش")
                )
                LegendDotItem(
                    color = StreakTodayGray,
                    label = tr("Today", "امروز")
                )
                LegendDotItem(
                    color = ObsidianTextSecondary.copy(alpha = 0.5f),
                    label = tr("Before Install", "قبل از نصب")
                )
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
            fontSize = 10.sp,
            color = ObsidianTextSecondary
        )
    }
}
