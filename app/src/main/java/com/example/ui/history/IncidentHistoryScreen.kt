package com.example.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.blocklog.ScreenshotStore
import com.example.data.model.AppUsageLimit
import com.example.data.model.BlockIncident
import com.example.data.preferences.ShieldPreferences
import com.example.ui.components.WeeklyActivityCard
import com.example.ui.i18n.AppStrings.tr
import com.example.ui.keywords.KeywordSelection
import com.example.ui.theme.AppThemeState
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianTextSecondary
import com.example.util.JalaliCalendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * #33 (batch 2): block reasons are recorded in English at block time
 * (see BlockLogPolicy); the log screen translates the known set for
 * display in Persian. Unknown/future reasons pass through unchanged.
 */
private fun localizedBlockReason(reason: String?, isPersian: Boolean): String? {
    if (reason.isNullOrBlank() || !isPersian) return reason
    return when (reason) {
        "Adult site" -> "سایت بزرگسال"
        "Daily app limit reached" -> "رسیدن به سقف روزانه برنامه"
        "Daily data limit reached" -> "رسیدن به سقف روزانه اینترنت"
        // #46 (batch 3): data-cap warning releases — who turned the
        // transport off, the app or the user by hand.
        "Mobile data turned off by the app — daily cap reached" -> "داده همراه خاموش شد — سقف روزانه پر شد"
        "Mobile data turned off manually — daily cap reached" -> "داده همراه دستی خاموش شد — سقف روزانه پر شد"
        "Wi-Fi turned off by the app — daily cap reached" -> "وای‌فای قطع شد — سقف روزانه پر شد"
        "Wi-Fi turned off manually — daily cap reached" -> "وای‌فای دستی قطع شد — سقف روزانه پر شد"
        "Blocked app section" -> "بخش مسدود برنامه"
        "Scheduled focus session" -> "جلسه تمرکز زمان‌بندی‌شده"
        "Tamper attempt" -> "تلاش برای دست‌کاری"
        "Force stop attempt blocked" -> "تلاش برای توقف اجباری"
        "Manual panic lock" -> "قفل اضطراری دستی"
        "Blocked keyword" -> "کلمه کلیدی مسدود"
        else -> reason
    }
}

@Composable
fun IncidentHistoryScreen(
    incidents: List<BlockIncident>,
    streakDays: Int,
    appLimits: List<AppUsageLimit> = emptyList(),
    preferences: ShieldPreferences? = null,
    usageRefreshTrigger: Long = 0L,
    onClearHistory: () -> Unit,
    onDeleteIncidents: (List<BlockIncident>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val effectivePrefs = remember(context, preferences) {
        preferences ?: ShieldPreferences(context)
    }
    var showClearDialog by remember { mutableStateOf(false) }

    // Block Log (#14): multi-select state for deleting records (rows +
    // their screenshot files). The set math is the same pure helper the
    // Custom Blacklist uses (#15).
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    // The record whose full detail (with screenshot) is open, if any.
    var detailIncident by remember { mutableStateOf<BlockIncident?>(null) }

    fun exitSelectionMode() {
        isSelectionMode = false
        selectedIds = emptySet()
    }

    // Drop ids whose records disappeared (deleted / pruned) so the
    // toolbar count never points at ghosts; an emptied log leaves
    // selection mode behind entirely.
    LaunchedEffect(incidents) {
        selectedIds = KeywordSelection.retainExisting(selectedIds, incidents.map { it.id })
        if (incidents.isEmpty()) exitSelectionMode()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp)
            ) {
                Text(
                    text = tr("Weekly Status & Activity Log", "وضعیت هفتگی و گزارش بلاک‌ها"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = tr(
                        "7-day discipline telemetry and blocked attempt timeline",
                        "نمای ۷ روزه و خط زمانی تلاش‌های مسدودشده"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianTextSecondary
                )
            }

            if (incidents.isNotEmpty()) {
                IconButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.testTag("clear_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = tr("Clear history", "پاک کردن تاریخچه"),
                        tint = ObsidianTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Multi-select toolbar (#14): visible only while selecting block
        // records. Delete removes every selected record together with its
        // screenshot file.
        if (isSelectionMode) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("incident_selection_toolbar"),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonPrimary)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { exitSelectionMode() },
                        modifier = Modifier.testTag("incident_selection_cancel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = tr("Cancel selection", "لغو انتخاب"),
                            tint = ObsidianTextSecondary
                        )
                    }
                    Text(
                        text = tr(
                            "${selectedIds.size} selected",
                            "${JalaliCalendar.toPersianDigits(selectedIds.size.toString())} انتخاب شده"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            selectedIds = KeywordSelection.selectAll(
                                selectedIds,
                                incidents.map { it.id }
                            )
                        },
                        modifier = Modifier.testTag("incident_selection_select_all_button")
                    ) {
                        Text(text = tr("Select all", "انتخاب همه"), color = CrimsonPrimary)
                    }
                    IconButton(
                        onClick = {
                            val toDelete = incidents.filter { it.id in selectedIds }
                            exitSelectionMode()
                            onDeleteIncidents(toDelete)
                        },
                        enabled = selectedIds.isNotEmpty(),
                        modifier = Modifier.testTag("incident_selection_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = tr("Delete selected", "حذف انتخاب‌شده‌ها"),
                            tint = if (selectedIds.isNotEmpty()) CrimsonPrimary
                            else ObsidianTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                WeeklyActivityCard(
                    incidents = incidents,
                    appLimits = appLimits,
                    preferences = effectivePrefs,
                    usageRefreshTrigger = usageRefreshTrigger
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr(
                            "Blocked Attempt Timeline (${incidents.size})",
                            "خط زمانی تلاش‌های مسدودشده (${JalaliCalendar.toPersianDigits(incidents.size.toString())})"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldShield.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = tr(
                                "$streakDays Days Clean",
                                "${JalaliCalendar.toPersianDigits(streakDays.toString())} روز پاک"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldShield,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (incidents.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldShield.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = EmeraldShield,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = tr("100% Clean Timeline", "خط زمانی کاملاً پاک"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = tr(
                                    "Zero explicit content or restricted sections triggered.",
                                    "هیچ محتوای صریح یا بخش ممنوعه‌ای فعال نشده است."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = ObsidianTextSecondary
                            )
                        }
                    }
                }
            } else {
                items(incidents, key = { it.id }) { item ->
                    IncidentItemCard(
                        incident = item,
                        isSelectionMode = isSelectionMode,
                        isSelected = item.id in selectedIds,
                        onClick = {
                            if (isSelectionMode) {
                                selectedIds = KeywordSelection.toggle(selectedIds, item.id)
                            } else {
                                detailIncident = item
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) isSelectionMode = true
                            selectedIds = KeywordSelection.toggle(selectedIds, item.id)
                        }
                    )
                }
            }
        }

        // Full per-record detail (#14): date, reason, trigger, source and
        // the screenshot of the blocked page when one was captured.
        detailIncident?.let { incident ->
            IncidentDetailDialog(
                incident = incident,
                onDismiss = { detailIncident = null }
            )
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                containerColor = ObsidianSurface,
                title = {
                    Text(
                        tr("Clear Incident History", "پاک کردن تاریخچه بلاک‌ها"),
                        color = Color.White
                    )
                },
                text = {
                    Text(
                        tr(
                            "Are you sure you want to clear all blocked attempt records and their screenshots? Your clean streak days will remain intact.",
                            "مطمئنید که می‌خواهید همه رکوردهای تلاش‌های مسدودشده و اسکرین‌شات‌هایشان پاک شود؟ روزهای پاک شما دست‌نخورده می‌ماند."
                        ),
                        color = ObsidianTextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onClearHistory()
                            showClearDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                    ) {
                        Text(tr("Clear", "پاک کردن"), color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text(tr("Cancel", "انصراف"), color = ObsidianTextSecondary)
                    }
                }
            )
        }
    }
}

/**
 * Loads a block-log screenshot from app-private storage, decoded off the
 * main thread and sampled down to [maxDimensionPx]. Returns null while
 * loading, when the record has no screenshot, or when the file is gone.
 */
@Composable
private fun rememberBlockScreenshot(
    fileName: String?,
    maxDimensionPx: Int
): ImageBitmap? {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(
        initialValue = null,
        fileName,
        maxDimensionPx
    ) {
        value = if (fileName.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                ScreenshotStore(context.applicationContext).loadBitmap(fileName, maxDimensionPx)
            }
        }
    }
    return bitmap?.asImageBitmap()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IncidentItemCard(
    incident: BlockIncident,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    // #33/#34 (batch 2): the row date renders Jalali in Persian.
    val isPersian = AppThemeState.isPersianLanguage
    val formattedDate = remember(incident.timestamp, isPersian) {
        JalaliCalendar.dateTimeLabel(incident.timestamp, isPersian)
    }

    // #14: the row leads with a thumbnail of the blocked page when a
    // screenshot was captured for this record.
    val thumbnail = rememberBlockScreenshot(incident.screenshotPath, maxDimensionPx = 256)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("incident_row_${incident.id}")
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) CrimsonPrimary.copy(alpha = 0.18f) else ObsidianSurface
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, CrimsonPrimary)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = CrimsonPrimary,
                        uncheckedColor = ObsidianTextSecondary,
                        checkmarkColor = Color.White
                    ),
                    modifier = Modifier.testTag("incident_select_checkbox_${incident.id}")
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail,
                    contentDescription = tr("Screenshot of the blocked page", "اسکرین‌شات صفحه مسدودشده"),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CrimsonPrimary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr(
                            "Triggered: \"${incident.matchedWord}\"",
                            "محرک: «${incident.matchedWord}»"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CrimsonPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = tr(
                                "${incident.lockDurationMinutes}m lock",
                                "${JalaliCalendar.toPersianDigits(incident.lockDurationMinutes.toString())} دقیقه قفل"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = CrimsonPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = tr(
                            "Source: ${incident.sourceApp.substringAfterLast('.')}",
                            "منبع: ${incident.sourceApp.substringAfterLast('.')}"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = ObsidianTextSecondary
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = ObsidianTextSecondary
                    )
                }

                // #14: the recorded reason, when this block carried one.
                // #33: known reasons are translated for display in FA.
                if (!incident.blockReason.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tr(
                            "Reason: ${incident.blockReason}",
                            "دلیل: ${localizedBlockReason(incident.blockReason, isPersian = true)}"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = CrimsonPrimary
                    )
                }
            }
        }
    }
}

/**
 * Full detail for one block record (#14): the screenshot of the blocked
 * page (from app-private storage — it never reaches the gallery) above
 * every recorded field. Records captured before screenshots existed, on
 * API < 30, or on secure windows simply show no image.
 */
@Composable
fun IncidentDetailDialog(
    incident: BlockIncident,
    onDismiss: () -> Unit
) {
    // #33/#34 (batch 2): full date renders Jalali in Persian.
    val isPersian = AppThemeState.isPersianLanguage
    val fullDateFormat = remember {
        SimpleDateFormat("MMM d, yyyy · h:mm:ss a", Locale.US)
    }
    val formattedFullDate = remember(incident.timestamp, isPersian) {
        if (isPersian) {
            "${JalaliCalendar.fullDateLabel(incident.timestamp, isPersian = true)} · " +
                JalaliCalendar.timeLabel(incident.timestamp, isPersian = true)
        } else {
            fullDateFormat.format(Date(incident.timestamp))
        }
    }
    val screenshot = rememberBlockScreenshot(incident.screenshotPath, maxDimensionPx = 1080)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("incident_detail_dialog"),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("Block Details", "جزئیات بلاک"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("incident_detail_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = tr("Close", "بستن"),
                            tint = ObsidianTextSecondary
                        )
                    }
                }

                if (screenshot != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Image(
                        bitmap = screenshot,
                        contentDescription = tr("Screenshot of the blocked page", "اسکرین‌شات صفحه مسدودشده"),
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("incident_detail_screenshot")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                IncidentDetailRow(
                    label = tr("Date & time", "تاریخ و ساعت"),
                    value = formattedFullDate
                )
                IncidentDetailRow(
                    label = tr("Reason", "دلیل"),
                    value = localizedBlockReason(
                        incident.blockReason?.takeIf { it.isNotBlank() }
                            ?: incident.matchedWord,
                        isPersian
                    ) ?: incident.matchedWord
                )
                IncidentDetailRow(label = tr("Trigger", "محرک"), value = incident.matchedWord)
                IncidentDetailRow(label = tr("Source app", "برنامه منبع"), value = incident.sourceApp)
                if (!incident.blockedUrl.isNullOrBlank()) {
                    IncidentDetailRow(
                        label = tr("Blocked address", "آدرس مسدودشده"),
                        value = incident.blockedUrl
                    )
                }
                IncidentDetailRow(
                    label = tr("Lock duration", "مدت قفل"),
                    value = tr(
                        "${incident.lockDurationMinutes} min",
                        "${JalaliCalendar.toPersianDigits(incident.lockDurationMinutes.toString())} دقیقه"
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(tr("Close", "بستن"), color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun IncidentDetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = ObsidianTextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
        )
    }
}
