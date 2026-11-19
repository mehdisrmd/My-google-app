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
import com.example.ui.keywords.KeywordSelection
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
                    text = "Weekly Status & Activity Log",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "7-day discipline telemetry and blocked attempt timeline",
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
                        contentDescription = "Clear history",
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
                            contentDescription = "Cancel selection",
                            tint = ObsidianTextSecondary
                        )
                    }
                    Text(
                        text = "${selectedIds.size} selected",
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
                        Text(text = "Select all", color = CrimsonPrimary)
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
                            contentDescription = "Delete selected",
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
                        text = "Blocked Attempt Timeline (${incidents.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldShield.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "$streakDays Days Clean",
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
                                text = "100% Clean Timeline",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Zero explicit content or restricted sections triggered.",
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
                title = { Text("Clear Incident History", color = Color.White) },
                text = {
                    Text(
                        "Are you sure you want to clear all blocked attempt records and their screenshots? Your clean streak days will remain intact.",
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
                        Text("Clear", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("Cancel", color = ObsidianTextSecondary)
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
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val formattedDate = remember(incident.timestamp) { dateFormat.format(Date(incident.timestamp)) }

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
                    contentDescription = "Screenshot of the blocked page",
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
                        text = "Triggered: \"${incident.matchedWord}\"",
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
                            text = "${incident.lockDurationMinutes}m lock",
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
                        text = "Source: ${incident.sourceApp.substringAfterLast('.')}",
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
                if (!incident.blockReason.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Reason: ${incident.blockReason}",
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
    val fullDateFormat = remember {
        SimpleDateFormat("MMM d, yyyy · h:mm:ss a", Locale.getDefault())
    }
    val formattedFullDate = remember(incident.timestamp) {
        fullDateFormat.format(Date(incident.timestamp))
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
                        text = "Block Details",
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
                            contentDescription = "Close",
                            tint = ObsidianTextSecondary
                        )
                    }
                }

                if (screenshot != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Image(
                        bitmap = screenshot,
                        contentDescription = "Screenshot of the blocked page",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("incident_detail_screenshot")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                IncidentDetailRow(label = "Date & time", value = formattedFullDate)
                IncidentDetailRow(
                    label = "Reason",
                    value = incident.blockReason?.takeIf { it.isNotBlank() }
                        ?: incident.matchedWord
                )
                IncidentDetailRow(label = "Trigger", value = incident.matchedWord)
                IncidentDetailRow(label = "Source app", value = incident.sourceApp)
                if (!incident.blockedUrl.isNullOrBlank()) {
                    IncidentDetailRow(label = "Blocked address", value = incident.blockedUrl)
                }
                IncidentDetailRow(
                    label = "Lock duration",
                    value = "${incident.lockDurationMinutes} min"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", color = Color.White)
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
