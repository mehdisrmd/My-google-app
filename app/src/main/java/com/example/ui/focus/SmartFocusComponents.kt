package com.example.ui.focus

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FocusScheduleSlot
import com.example.data.preferences.ShieldPreferences
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.CyanTelemetry
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianTextSecondary
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

object SmartFocusEngine {
    /**
     * Checks if any enabled FocusScheduleSlot matches the current time and day of week
     * and has not yet auto-triggered a lock session today.
     */
    fun checkAndFindActiveSlotToTrigger(
        slots: List<FocusScheduleSlot>,
        preferences: ShieldPreferences,
        calendar: Calendar = Calendar.getInstance()
    ): Pair<FocusScheduleSlot, Int>? {
        if (!preferences.getIsSmartFocusEnabled()) return null
        if (preferences.isCurrentlyLocked()) return null

        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val isoDay = FocusScheduleSlot.calendarDayToIsoDay(calendar.get(Calendar.DAY_OF_WEEK))
        val nowMs = calendar.timeInMillis

        for (slot in slots) {
            if (slot.isEnabled &&
                slot.autoTriggerLockdown &&
                slot.isTimeWithinSlot(currentHour, currentMinute, isoDay) &&
                !preferences.wasFocusSlotTriggeredToday(slot.id, nowMs)
            ) {
                val remainingMinutes = slot.getRemainingSlotMinutes(currentHour, currentMinute)
                return Pair(slot, remainingMinutes)
            }
        }
        return null
    }
}

@Composable
fun SmartFocusDashboardCard(
    focusSlots: List<FocusScheduleSlot>,
    isSmartFocusEnabled: Boolean,
    onToggleSmartFocusMaster: (Boolean) -> Unit,
    onToggleFocusSlot: (FocusScheduleSlot) -> Unit,
    onAddFocusSlot: (FocusScheduleSlot) -> Unit,
    onStartFocusSessionNow: (title: String, durationMinutes: Int) -> Unit
) {
    var showAddSlotDialog by remember { mutableStateOf(false) }
    var editingSlot by remember { mutableStateOf<FocusScheduleSlot?>(null) }
    val enabledCount = focusSlots.count { it.isEnabled }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("smart_focus_dashboard_card"),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.5.dp,
            if (isSmartFocusEnabled) IndigoAccent.copy(alpha = 0.6f) else ObsidianBorder
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            IndigoAccent.copy(alpha = 0.16f),
                            CyanTelemetry.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(IndigoAccent.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Smart Focus",
                                tint = IndigoAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Smart Focus",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSmartFocusEnabled) EmeraldShield.copy(alpha = 0.18f)
                                    else ObsidianCard
                                ) {
                                    Text(
                                        text = if (isSmartFocusEnabled) "$enabledCount ACTIVE" else "PAUSED",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = if (isSmartFocusEnabled) EmeraldShield else ObsidianTextSecondary,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Auto-triggers Deep Work lock sessions on schedule",
                                style = MaterialTheme.typography.bodySmall,
                                color = ObsidianTextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Switch(
                        checked = isSmartFocusEnabled,
                        onCheckedChange = onToggleSmartFocusMaster,
                        modifier = Modifier.testTag("smart_focus_master_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = IndigoAccent
                        )
                    )
                }

                // Preview of scheduled Deep Work time slots
                if (focusSlots.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        focusSlots.take(3).forEach { slot ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ObsidianCard,
                                border = BorderStroke(
                                    1.dp,
                                    if (slot.isEnabled && isSmartFocusEnabled) IndigoAccent.copy(alpha = 0.45f)
                                    else ObsidianBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = slot.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (slot.isEnabled) Color.White else ObsidianTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${slot.formattedTimeRange()} • ${slot.getDurationMinutes()}m • ${slot.formattedDaysSummary()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 11.sp,
                                            color = if (slot.isEnabled) CyanTelemetry else ObsidianTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { editingSlot = slot },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Slot",
                                                tint = CyanTelemetry,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Switch(
                                            checked = slot.isEnabled,
                                            onCheckedChange = { onToggleFocusSlot(slot) },
                                            modifier = Modifier.testTag("dashboard_focus_slot_switch_${slot.id}"),
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = IndigoAccent
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Action Row: Add Deep Work Slot or Start Instant Deep Work Session
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showAddSlotDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_deep_work_slot_button"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        border = BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = IndigoAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add Time Slot",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Button(
                        onClick = {
                            onStartFocusSessionNow("Smart Focus Deep Work Sprint", 25)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("start_instant_deep_work_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IndigoAccent,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Start 25m Focus",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }

    if (showAddSlotDialog || editingSlot != null) {
        AddFocusSlotDialog(
            initialSlot = editingSlot,
            onDismiss = {
                showAddSlotDialog = false
                editingSlot = null
            },
            onSave = { slot ->
                onAddFocusSlot(slot)
                showAddSlotDialog = false
                editingSlot = null
            }
        )
    }
}

@Composable
fun SmartFocusSlotsList(
    focusSlots: List<FocusScheduleSlot>,
    isSmartFocusEnabled: Boolean = true,
    onToggleSmartFocusMaster: (Boolean) -> Unit = {},
    onToggleSlot: (FocusScheduleSlot) -> Unit,
    onDeleteSlot: (Long) -> Unit,
    onSaveSlot: (FocusScheduleSlot) -> Unit = {},
    onStartSlotNow: (FocusScheduleSlot) -> Unit,
    onStartQuickSprint: (durationMinutes: Int) -> Unit = {}
) {
    var editingSlot by remember { mutableStateOf<FocusScheduleSlot?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Control & Quick Focus Sprint Header Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("smart_focus_tab_header_card"),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    if (isSmartFocusEnabled) IndigoAccent.copy(alpha = 0.6f) else ObsidianBorder
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                text = "Auto-Schedule Engine",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isSmartFocusEnabled) {
                                    "Enabled slots automatically lock your screen during Deep Work hours"
                                } else {
                                    "Auto-triggers paused — toggle on to activate scheduled Deep Work locks"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = ObsidianTextSecondary
                            )
                        }

                        Switch(
                            checked = isSmartFocusEnabled,
                            onCheckedChange = onToggleSmartFocusMaster,
                            modifier = Modifier.testTag("smart_focus_tab_master_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = IndigoAccent
                            )
                        )
                    }

                    // Instant Focus Sprint Bar
                    Text(
                        text = "Instant Deep Work Sprint (Lock Now):",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = CyanTelemetry
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 25, 45, 60).forEach { mins ->
                            Surface(
                                onClick = { onStartQuickSprint(mins) },
                                shape = RoundedCornerShape(10.dp),
                                color = IndigoAccent.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_focus_sprint_${mins}m")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = EmeraldShield,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${mins}m",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (focusSlots.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ObsidianBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Deep Work Time Slots Scheduled",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap below or + to add a daily or weekday Deep Work focus window.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ObsidianTextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create First Focus Slot", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(focusSlots, key = { it.id }) { slot ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("focus_slot_card_${slot.id}"),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (slot.isEnabled) IndigoAccent.copy(alpha = 0.55f) else ObsidianBorder
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (slot.isEnabled) IndigoAccent.copy(alpha = 0.2f)
                                            else ObsidianCard
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = if (slot.isEnabled) IndigoAccent else ObsidianTextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = slot.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${slot.formattedTimeRange()} (${slot.getDurationMinutes()}m)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = CyanTelemetry,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = slot.formattedDaysSummary(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ObsidianTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { editingSlot = slot },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("edit_focus_slot_${slot.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Slot",
                                        tint = CyanTelemetry,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteSlot(slot.id) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("delete_focus_slot_${slot.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Slot",
                                        tint = CrimsonPrimary.copy(alpha = 0.85f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Switch(
                                    checked = slot.isEnabled,
                                    onCheckedChange = { onToggleSlot(slot) },
                                    modifier = Modifier.testTag("toggle_focus_slot_${slot.id}"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = IndigoAccent
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { onStartSlotNow(slot) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("start_focus_slot_now_${slot.id}"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            border = BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = EmeraldShield,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Start Focus Lock Now (${slot.getDurationMinutes()}m)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    if (editingSlot != null || showCreateDialog) {
        AddFocusSlotDialog(
            initialSlot = editingSlot,
            onDismiss = {
                editingSlot = null
                showCreateDialog = false
            },
            onSave = { updated ->
                onSaveSlot(updated)
                editingSlot = null
                showCreateDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddFocusSlotDialog(
    initialSlot: FocusScheduleSlot? = null,
    onDismiss: () -> Unit,
    onSave: (FocusScheduleSlot) -> Unit
) {
    var title by remember(initialSlot) {
        mutableStateOf(initialSlot?.title ?: "Deep Work Session")
    }
    var startHour by remember(initialSlot) {
        mutableIntStateOf(initialSlot?.startHour ?: 9)
    }
    var startMinute by remember(initialSlot) {
        mutableIntStateOf(initialSlot?.startMinute ?: 0)
    }
    var durationMinutes by remember(initialSlot) {
        mutableFloatStateOf((initialSlot?.getDurationMinutes() ?: 60).toFloat().coerceIn(15f, 240f))
    }
    var selectedDays by remember(initialSlot) {
        mutableStateOf(initialSlot?.getActiveDaysSet()?.ifEmpty { setOf(1, 2, 3, 4, 5, 6, 7) } ?: setOf(1, 2, 3, 4, 5, 6, 7))
    }

    val totalStartMins = startHour * 60 + startMinute
    val totalEndMins = (totalStartMins + durationMinutes.roundToInt()) % (24 * 60)
    val endHour = totalEndMins / 60
    val endMinute = totalEndMins % 60

    val dayChips = listOf(
        1 to "M",
        2 to "T",
        3 to "W",
        4 to "T",
        5 to "F",
        6 to "S",
        7 to "S"
    )

    val quickStartTimes = listOf(
        Pair("08:00", 8 to 0),
        Pair("09:00", 9 to 0),
        Pair("12:00", 12 to 0),
        Pair("14:00", 14 to 0),
        Pair("18:00", 18 to 0),
        Pair("21:00", 21 to 0)
    )

    val minuteOptions = listOf(0, 15, 30, 45)
    val durationPresets = listOf(25, 45, 60, 90, 120)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Text(
                text = if (initialSlot != null) "Edit Smart Focus Slot" else "Schedule Smart Focus Deep Work",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Define a Deep Work time window. ShieldBlock automatically triggers a lock session during this slot:",
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianTextSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Live Preview Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = IndigoAccent.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format(
                                Locale.US,
                                "%02d:%02d – %02d:%02d",
                                startHour,
                                startMinute,
                                endHour,
                                endMinute
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = CyanTelemetry
                        )
                        Text(
                            text = "${durationMinutes.roundToInt()}m Lock",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldShield
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Focus Slot Label") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("focus_slot_title_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IndigoAccent,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Quick Start Time Presets:",
                    style = MaterialTheme.typography.labelSmall,
                    color = ObsidianTextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickStartTimes.forEach { (label, hrMin) ->
                        val isSelected = startHour == hrMin.first && startMinute == hrMin.second
                        Surface(
                            onClick = {
                                startHour = hrMin.first
                                startMinute = hrMin.second
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) IndigoAccent else ObsidianCard,
                            border = if (isSelected) null else BorderStroke(1.dp, ObsidianBorder)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else ObsidianTextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(Locale.US, "Start Hour: %02d:00", startHour),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        minuteOptions.forEach { minOpt ->
                            val selectedMin = startMinute == minOpt
                            Surface(
                                onClick = { startMinute = minOpt },
                                shape = RoundedCornerShape(6.dp),
                                color = if (selectedMin) CyanTelemetry.copy(alpha = 0.25f) else ObsidianCard,
                                border = BorderStroke(
                                    1.dp,
                                    if (selectedMin) CyanTelemetry else ObsidianBorder
                                )
                            ) {
                                Text(
                                    text = String.format(Locale.US, ":%02d", minOpt),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = if (selectedMin) CyanTelemetry else ObsidianTextSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Slider(
                    value = startHour.toFloat(),
                    onValueChange = { startHour = it.roundToInt().coerceIn(0, 23) },
                    valueRange = 0f..23f,
                    steps = 22,
                    colors = SliderDefaults.colors(
                        thumbColor = IndigoAccent,
                        activeTrackColor = IndigoAccent
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Duration: ${durationMinutes.roundToInt()} mins",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    durationPresets.forEach { presetMins ->
                        val isDurSelected = durationMinutes.roundToInt() == presetMins
                        Surface(
                            onClick = { durationMinutes = presetMins.toFloat() },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDurSelected) EmeraldShield else ObsidianCard,
                            border = if (isDurSelected) null else BorderStroke(1.dp, ObsidianBorder)
                        ) {
                            Text(
                                text = "${presetMins}m",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = if (isDurSelected) Color.Black else ObsidianTextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Slider(
                    value = durationMinutes,
                    onValueChange = { durationMinutes = it },
                    valueRange = 15f..240f,
                    steps = 14,
                    colors = SliderDefaults.colors(
                        thumbColor = EmeraldShield,
                        activeTrackColor = EmeraldShield
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Days:",
                        style = MaterialTheme.typography.labelSmall,
                        color = ObsidianTextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            onClick = { selectedDays = setOf(1, 2, 3, 4, 5) },
                            shape = RoundedCornerShape(6.dp),
                            color = ObsidianCard,
                            border = BorderStroke(1.dp, ObsidianBorder)
                        ) {
                            Text(
                                text = "Weekdays",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = CyanTelemetry,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            onClick = { selectedDays = setOf(1, 2, 3, 4, 5, 6, 7) },
                            shape = RoundedCornerShape(6.dp),
                            color = ObsidianCard,
                            border = BorderStroke(1.dp, ObsidianBorder)
                        ) {
                            Text(
                                text = "Every Day",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = EmeraldShield,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    dayChips.forEach { (dayNum, label) ->
                        val isSelected = dayNum in selectedDays
                        Surface(
                            onClick = {
                                selectedDays = if (isSelected && selectedDays.size > 1) {
                                    selectedDays - dayNum
                                } else {
                                    selectedDays + dayNum
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) EmeraldShield else ObsidianCard,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    color = if (isSelected) Color.Black else ObsidianTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanTitle = title.trim().ifEmpty { "Deep Work Slot" }
                    onSave(
                        FocusScheduleSlot(
                            id = initialSlot?.id ?: 0L,
                            title = cleanTitle,
                            startHour = startHour,
                            startMinute = startMinute,
                            endHour = endHour,
                            endMinute = endMinute,
                            daysOfWeekCsv = selectedDays.sorted().joinToString(","),
                            isEnabled = initialSlot?.isEnabled ?: true,
                            autoTriggerLockdown = true
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent),
                modifier = Modifier.testTag("save_focus_slot_button")
            ) {
                Text(
                    text = if (initialSlot != null) "Update Slot" else "Save Focus Slot",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ObsidianTextSecondary)
            }
        }
    )
}
