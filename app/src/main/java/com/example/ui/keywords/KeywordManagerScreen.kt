package com.example.ui.keywords

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.blocklist.AdultBlocklistParser
import com.example.data.blocklist.BlocklistSource
import com.example.data.blocklist.BlocklistSources
import com.example.data.model.AppUsageLimit
import com.example.data.model.BlockIncident
import com.example.data.model.BlockedCategory
import com.example.data.model.BlockedKeyword
import com.example.data.preferences.ShieldPreferences
import com.example.ui.history.IncidentHistoryScreen
import com.example.ui.i18n.AppStrings.tr
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianTextSecondary
import java.text.DateFormat
import java.util.Date

/**
 * #61 (batch 5): one repository's update state in the Adult Block
 * card — IDLE normally, UPDATING while its own download runs (the row
 * shows a spinner), FAILED after a failed update (the row turns red
 * until a later update succeeds).
 */
enum class SourceUpdateUiState { IDLE, UPDATING, FAILED }

@Composable
fun KeywordManagerScreen(
    keywords: List<BlockedKeyword>,
    onToggleKeyword: (BlockedKeyword) -> Unit,
    onDeleteKeyword: (Long) -> Unit,
    onAddKeyword: (text: String, isDomain: Boolean) -> Unit,
    adultBlockEnabled: Boolean,
    onToggleAdultBlock: (Boolean) -> Unit,
    blocklistDomainCount: Int,
    blocklistLastUpdatedMs: Long,
    isBlocklistUpdating: Boolean,
    onUpdateBlocklistNow: () -> Unit,
    userBlocklistDomains: List<String> = emptyList(),
    onAddUserBlocklistDomain: (String) -> Unit = {},
    onRemoveUserBlocklistDomain: (String) -> Unit = {},
    blocklistSources: List<BlocklistSource> = BlocklistSources.SOURCES,
    onRemoveBlocklistSource: (BlocklistSource) -> Unit = {},
    onAddBlocklistSource: (BlocklistSource) -> Unit = {},
    onRestoreDefaultBlocklistSources: () -> Unit = {},
    // #61 (batch 5): per-repo update controls + merged-list search.
    sourceUpdateStates: Map<String, SourceUpdateUiState> = emptyMap(),
    onUpdateBlocklistSource: (BlocklistSource) -> Unit = {},
    onSearchBlocklistDomains: suspend (String) -> List<String> = { emptyList() },
    incidents: List<BlockIncident> = emptyList(),
    incidentStreakDays: Int = 0,
    incidentAppLimits: List<AppUsageLimit> = emptyList(),
    incidentPreferences: ShieldPreferences? = null,
    incidentUsageRefreshTrigger: Long = 0L,
    onDeleteIncidents: (List<BlockIncident>) -> Unit = {},
    onClearIncidentHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    // #14: the Block Log lives in the slot the Adult Preset used to own —
    // its entry card sits directly under the adult-block card below, and
    // tapping it swaps this screen's content for the log itself.
    var showBlockLog by remember { mutableStateOf(false) }

    // Multi-select state for the Custom Blacklist (#15). Selection is a
    // set of keyword ids; all transitions go through KeywordSelection so the
    // math stays unit-testable.
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    fun exitSelectionMode() {
        isSelectionMode = false
        selectedIds = emptySet()
    }

    // Drop ids whose rows disappeared (deleted elsewhere / list reloaded) so
    // the toolbar count never points at ghosts.
    LaunchedEffect(keywords) {
        selectedIds = KeywordSelection.retainExisting(selectedIds, keywords.map { it.id })
    }

    // #12: the Adult Preset tab is gone. Only the user's own custom rules
    // live in this list now; adult sites are covered by the toggle card
    // above and its downloadable domain list.
    val filteredList = remember(keywords, searchQuery) {
        val customOnly = keywords.filter {
            it.category == BlockedCategory.CUSTOM_KEYWORD ||
                it.category == BlockedCategory.CUSTOM_DOMAIN
        }

        if (searchQuery.isBlank()) {
            customOnly
        } else {
            customOnly.filter { it.text.contains(searchQuery.trim(), ignoreCase = true) }
        }
    }

    // #14: while the Block Log is open it replaces this screen's content
    // (same tab, no extra navigation destination).
    if (showBlockLog) {
        BlockLogView(
            incidents = incidents,
            streakDays = incidentStreakDays,
            appLimits = incidentAppLimits,
            preferences = incidentPreferences,
            usageRefreshTrigger = incidentUsageRefreshTrigger,
            onBack = { showBlockLog = false },
            onDeleteIncidents = onDeleteIncidents,
            onClearHistory = onClearIncidentHistory,
            modifier = modifier
        )
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Blocked Content Rules",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Manage your custom words & domains that trigger immediate screen lockdown",
                style = MaterialTheme.typography.bodySmall,
                color = ObsidianTextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // Adult Site Blocking card (#12/#13): the single toggle that
            // replaced the Adult Preset, with the downloadable list's
            // status and a manual update action. The list also refreshes
            // itself weekly in the background.
            AdultBlockCard(
                adultBlockEnabled = adultBlockEnabled,
                onToggleAdultBlock = onToggleAdultBlock,
                blocklistDomainCount = blocklistDomainCount,
                blocklistLastUpdatedMs = blocklistLastUpdatedMs,
                isBlocklistUpdating = isBlocklistUpdating,
                onUpdateBlocklistNow = onUpdateBlocklistNow,
                userBlocklistDomains = userBlocklistDomains,
                onAddUserBlocklistDomain = onAddUserBlocklistDomain,
                onRemoveUserBlocklistDomain = onRemoveUserBlocklistDomain,
                blocklistSources = blocklistSources,
                onRemoveBlocklistSource = onRemoveBlocklistSource,
                onAddBlocklistSource = onAddBlocklistSource,
                onRestoreDefaultBlocklistSources = onRestoreDefaultBlocklistSources,
                sourceUpdateStates = sourceUpdateStates,
                onUpdateBlocklistSource = onUpdateBlocklistSource,
                onSearchBlocklistDomains = onSearchBlocklistDomains
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Block Log entry (#14): every blocked page with its
            // screenshot and reason, in the freed Adult Preset slot.
            BlockLogCard(
                incidentCount = incidents.size,
                onOpen = { showBlockLog = true }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search rules...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = ObsidianTextSecondary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = ObsidianTextSecondary
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rules_search_field"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CrimsonPrimary,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Multi-select toolbar (#15): visible only while selecting
            // custom rules. Delete applies to every selected id at once.
            if (isSelectionMode) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_selection_toolbar"),
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
                            modifier = Modifier.testTag("custom_selection_cancel_button")
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
                                    filteredList.map { it.id }
                                )
                            },
                            modifier = Modifier.testTag("custom_selection_select_all_button")
                        ) {
                            Text(text = "Select all", color = CrimsonPrimary)
                        }
                        IconButton(
                            onClick = {
                                val idsToDelete = selectedIds
                                exitSelectionMode()
                                idsToDelete.forEach { id -> onDeleteKeyword(id) }
                            },
                            enabled = selectedIds.isNotEmpty(),
                            modifier = Modifier.testTag("custom_selection_delete_button")
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

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = ObsidianTextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching rules found"
                            else "No custom rules added yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ObsidianTextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        CustomKeywordRow(
                            keyword = item,
                            isSelectionMode = isSelectionMode,
                            isSelected = item.id in selectedIds,
                            onToggle = { onToggleKeyword(item) },
                            onDelete = { onDeleteKeyword(item.id) },
                            onRowClick = {
                                if (isSelectionMode) {
                                    selectedIds = KeywordSelection.toggle(selectedIds, item.id)
                                }
                            },
                            onRowLongClick = {
                                if (!isSelectionMode) isSelectionMode = true
                                selectedIds = KeywordSelection.toggle(selectedIds, item.id)
                            }
                        )
                    }
                }
            }
        }

        // FAB to add custom keywords/domains
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_custom_rule_fab"),
            containerColor = CrimsonPrimary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add custom rule")
        }

        if (showAddDialog) {
            AddCustomRuleDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { text, isDomain ->
                    onAddKeyword(text, isDomain)
                    showAddDialog = false
                }
            )
        }
    }
}

/**
 * The "Block all adult sites" card (#12) with the blocklist status and
 * manual update action (#13). It sits exactly where the Adult Preset tab
 * used to live: one toggle instead of preset lists to manage.
 */
@Composable
private fun AdultBlockCard(
    adultBlockEnabled: Boolean,
    onToggleAdultBlock: (Boolean) -> Unit,
    blocklistDomainCount: Int,
    blocklistLastUpdatedMs: Long,
    isBlocklistUpdating: Boolean,
    onUpdateBlocklistNow: () -> Unit,
    userBlocklistDomains: List<String> = emptyList(),
    onAddUserBlocklistDomain: (String) -> Unit = {},
    onRemoveUserBlocklistDomain: (String) -> Unit = {},
    blocklistSources: List<BlocklistSource> = BlocklistSources.SOURCES,
    onRemoveBlocklistSource: (BlocklistSource) -> Unit = {},
    onAddBlocklistSource: (BlocklistSource) -> Unit = {},
    onRestoreDefaultBlocklistSources: () -> Unit = {},
    // #61 (batch 5): per-repo update controls + merged-list search.
    sourceUpdateStates: Map<String, SourceUpdateUiState> = emptyMap(),
    onUpdateBlocklistSource: (BlocklistSource) -> Unit = {},
    onSearchBlocklistDomains: suspend (String) -> List<String> = { emptyList() }
) {
    val lastUpdatedText = remember(blocklistLastUpdatedMs) {
        if (blocklistLastUpdatedMs > 0L) {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(blocklistLastUpdatedMs))
        } else {
            null
        }
    }

    val statusText = when {
        isBlocklistUpdating -> tr(
            "Updating the site list…",
            "در حال به‌روزرسانی فهرست سایت‌ها…"
        )
        blocklistDomainCount <= 0 -> tr(
            "The domain list has not been downloaded yet.",
            "فهرست دامنه‌ها هنوز دانلود نشده است."
        )
        lastUpdatedText != null -> tr(
            "$blocklistDomainCount domains · Updated $lastUpdatedText",
            "$blocklistDomainCount دامنه · به‌روزرسانی: $lastUpdatedText"
        )
        else -> tr(
            "$blocklistDomainCount domains (built-in starter list)",
            "$blocklistDomainCount دامنه (فهرست اولیهٔ داخلی)"
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("adult_block_card"),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (adultBlockEnabled) CrimsonPrimary.copy(alpha = 0.55f) else ObsidianBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CrimsonPrimary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = tr("Block All Adult Sites", "مسدود کردن همهٔ سایت‌های بزرگسال"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = tr(
                                "Any adult site is blocked and the browser is sent to the Google homepage.",
                                "هر سایت بزرگسالی مسدود می‌شود و مرورگر به صفحهٔ اصلی گوگل فرستاده می‌شود."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = ObsidianTextSecondary
                        )
                    }
                }

                Switch(
                    checked = adultBlockEnabled,
                    onCheckedChange = { onToggleAdultBlock(it) },
                    modifier = Modifier.testTag("adult_block_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = CrimsonPrimary,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = ObsidianBorder
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianTextSecondary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                TextButton(
                    onClick = onUpdateBlocklistNow,
                    enabled = !isBlocklistUpdating,
                    modifier = Modifier.testTag("adult_blocklist_update_button")
                ) {
                    if (isBlocklistUpdating) {
                        CircularProgressIndicator(
                            color = CrimsonPrimary,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        // #61 (batch 5): this is now the UPDATE ALL
                        // action — each repository also has its own
                        // update button below.
                        text = tr("Update all", "به‌روزرسانی همه"),
                        color = CrimsonPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // #38 (batch 2, corrected): the list sources, shown openly.
            // #49 (batch 3): they are also MANAGED here — each repository
            // can be removed, new ones added by list URL, and the two
            // built-in defaults restored. The list lives in preferences;
            // changes take effect on the next update.
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = tr("List repositories", "مخازن فهرست"),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (blocklistSources.isEmpty()) {
                Text(
                    text = tr(
                        "No repositories configured. The list now relies on your own sites below and the built-in starter list — add or restore a repository to download a full list again.",
                        "هیچ مخزنی تنظیم نشده است. فهرست حالا به سایت‌های خودتان در پایین و فهرست اولیهٔ داخلی تکیه دارد — برای دانلود دوبارهٔ فهرست کامل، یک مخزن اضافه یا بازیابی کنید."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianTextSecondary,
                    modifier = Modifier.testTag("blocklist_sources_empty_note")
                )
            } else {
                blocklistSources.forEachIndexed { index, source ->
                    // #61 (batch 5): each repository carries its own
                    // update state — spinner while ITS download runs,
                    // red after a failed update until one succeeds.
                    val sourceState = sourceUpdateStates[BlocklistSources.keyFor(source)]
                        ?: SourceUpdateUiState.IDLE
                    val failed = sourceState == SourceUpdateUiState.FAILED
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = source.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (failed) CrimsonPrimary else Color.White
                            )
                            Text(
                                text = if (failed) {
                                    tr(
                                        "Update failed — the previous list from this repository is still active.",
                                        "به‌روزرسانی ناموفق بود — فهرست قبلی این مخزن همچنان فعال است."
                                    )
                                } else {
                                    source.repositoryUrl
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (failed) CrimsonPrimary else ObsidianTextSecondary
                            )
                        }
                        IconButton(
                            onClick = { onUpdateBlocklistSource(source) },
                            enabled = sourceState != SourceUpdateUiState.UPDATING && !isBlocklistUpdating,
                            modifier = Modifier.testTag("blocklist_source_update_$index")
                        ) {
                            if (sourceState == SourceUpdateUiState.UPDATING) {
                                CircularProgressIndicator(
                                    color = CrimsonPrimary,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = tr("Update this repository", "به‌روزرسانی این مخزن"),
                                    tint = if (failed) CrimsonPrimary else EmeraldShield,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { onRemoveBlocklistSource(source) },
                            modifier = Modifier.testTag("blocklist_source_remove_$index")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = tr("Remove repository", "حذف مخزن"),
                                tint = CrimsonPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            if (blocklistSources != BlocklistSources.SOURCES) {
                TextButton(
                    onClick = onRestoreDefaultBlocklistSources,
                    modifier = Modifier.testTag("blocklist_sources_restore_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tr("Restore default repositories", "بازیابی مخازن پیش‌فرض"),
                        color = CrimsonPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            var newSourceName by remember { mutableStateOf("") }
            var newSourceUrl by remember { mutableStateOf("") }
            var sourceUrlError by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = newSourceName,
                onValueChange = { newSourceName = it },
                placeholder = {
                    Text(
                        tr("Repository name (optional)", "نام مخزن (اختیاری)"),
                        color = ObsidianTextSecondary
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CrimsonPrimary,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = CrimsonPrimary,
                    focusedContainerColor = ObsidianSurface,
                    unfocusedContainerColor = ObsidianSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("blocklist_source_name_input")
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newSourceUrl,
                    onValueChange = {
                        newSourceUrl = it
                        sourceUrlError = false
                    },
                    placeholder = {
                        Text(
                            tr("https://example.com/list.txt", "https://example.com/list.txt"),
                            color = ObsidianTextSecondary
                        )
                    },
                    singleLine = true,
                    isError = sourceUrlError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CrimsonPrimary,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = CrimsonPrimary,
                        focusedContainerColor = ObsidianSurface,
                        unfocusedContainerColor = ObsidianSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("blocklist_source_url_input")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val source = BlocklistSources.sourceFromUserInput(newSourceName, newSourceUrl)
                        if (source == null) {
                            sourceUrlError = true
                        } else {
                            onAddBlocklistSource(source)
                            newSourceName = ""
                            newSourceUrl = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("blocklist_source_add_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(tr("Add", "افزودن"), fontWeight = FontWeight.Bold)
                }
            }
            if (sourceUrlError) {
                Text(
                    text = tr(
                        "That does not look like a list URL — type the full https:// address of a domain list.",
                        "این شبیه آدرس فهرست نیست — آدرس کامل https:// یک فهرست دامنه را بنویسید."
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = CrimsonPrimary
                )
            }

            // #61 (batch 5): the TOTAL number of filtered domains in
            // the merged list, and a search across the whole merged
            // list (every repository's domains together).
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = tr(
                    "Total filtered domains: $blocklistDomainCount",
                    "مجموع دامنه‌های فیلترشده: $blocklistDomainCount"
                ),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.testTag("blocklist_total_domains_count")
            )
            var blocklistSearchQuery by remember { mutableStateOf("") }
            var blocklistSearchResults by remember { mutableStateOf<List<String>>(emptyList()) }
            var blocklistSearchDone by remember { mutableStateOf(false) }
            LaunchedEffect(blocklistSearchQuery) {
                val query = blocklistSearchQuery.trim()
                if (query.length < 2) {
                    blocklistSearchResults = emptyList()
                    blocklistSearchDone = false
                } else {
                    // Small debounce: the merged list is ~224k lines;
                    // the search itself runs off the main thread in
                    // the supplied callback.
                    kotlinx.coroutines.delay(300)
                    blocklistSearchResults = onSearchBlocklistDomains(query)
                    blocklistSearchDone = true
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = blocklistSearchQuery,
                onValueChange = { blocklistSearchQuery = it },
                placeholder = {
                    Text(
                        tr("Search all filtered domains…", "جستجو در همه دامنه‌های فیلترشده…"),
                        color = ObsidianTextSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = ObsidianTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CrimsonPrimary,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = CrimsonPrimary,
                    focusedContainerColor = ObsidianSurface,
                    unfocusedContainerColor = ObsidianSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("blocklist_search_input")
            )
            if (blocklistSearchDone) {
                if (blocklistSearchResults.isEmpty()) {
                    Text(
                        text = tr(
                            "No matching domain in the filtered list.",
                            "دامنه‌ای مطابق جستجو در فهرست فیلترشده نیست."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = ObsidianTextSecondary,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .padding(top = 6.dp)
                            .verticalScroll(rememberScrollState())
                            .testTag("blocklist_search_results")
                    ) {
                        blocklistSearchResults.forEach { domain ->
                            Text(
                                text = domain,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // #38 (batch 2): the user's own additions to the list. They
            // are merged into the on-disk index immediately, survive
            // updates, and can be removed again here.
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = tr("Sites you added yourself", "سایت‌هایی که خودتان اضافه کردید"),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (userBlocklistDomains.isEmpty()) {
                Text(
                    text = tr(
                        "None yet. Add any site below and it will be blocked like the downloaded list.",
                        "هنوز هیچی نیست. هر سایتی را پایین اضافه کنید، مثل فهرست دانلودشده مسدود می‌شود."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianTextSecondary
                )
            } else {
                userBlocklistDomains.sorted().forEach { domain ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = domain,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                        IconButton(
                            onClick = { onRemoveUserBlocklistDomain(domain) },
                            modifier = Modifier.testTag("user_blocklist_remove_$domain")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = tr("Remove", "حذف"),
                                tint = CrimsonPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            var newUserDomain by remember { mutableStateOf("") }
            var userDomainError by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newUserDomain,
                    onValueChange = {
                        newUserDomain = it
                        userDomainError = false
                    },
                    placeholder = {
                        Text(
                            tr("example.com", "example.com"),
                            color = ObsidianTextSecondary
                        )
                    },
                    singleLine = true,
                    isError = userDomainError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CrimsonPrimary,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = CrimsonPrimary,
                        focusedContainerColor = ObsidianSurface,
                        unfocusedContainerColor = ObsidianSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("user_blocklist_input")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val normalized = AdultBlocklistParser.normalizeDomain(newUserDomain)
                        if (normalized == null) {
                            userDomainError = true
                        } else {
                            onAddUserBlocklistDomain(normalized)
                            newUserDomain = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("user_blocklist_add_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(tr("Add", "افزودن"), fontWeight = FontWeight.Bold)
                }
            }
            if (userDomainError) {
                Text(
                    text = tr(
                        "That does not look like a domain — type something like example.com.",
                        "این شبیه دامنه نیست — چیزی مثل example.com بنویسید."
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = CrimsonPrimary
                )
            }
        }
    }
}

/**
 * Block Log entry card (#14). Sits directly under the adult-block card —
 * the slot the Adult Preset used to own — and opens the log of blocked
 * pages (screenshot + reason + full details per record).
 */
@Composable
private fun BlockLogCard(
    incidentCount: Int,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("block_log_card")
            .clickable { onOpen() },
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CrimsonPrimary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = CrimsonPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tr("Block Log", "گزارش مسدودی‌ها"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = tr(
                        "Every blocked page, with its screenshot and the reason it was blocked.",
                        "همهٔ صفحه‌های مسدودشده، همراه با اسکرین‌شات و دلیل مسدود شدن."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianTextSecondary
                )
            }
            if (incidentCount > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CrimsonPrimary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = incidentCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = com.example.ui.theme.JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = ObsidianTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * The Block Log itself (#14): a back row over the incident history, whose
 * rows carry the block screenshots and open full per-record details.
 */
@Composable
private fun BlockLogView(
    incidents: List<BlockIncident>,
    streakDays: Int,
    appLimits: List<AppUsageLimit>,
    preferences: ShieldPreferences?,
    usageRefreshTrigger: Long,
    onBack: () -> Unit,
    onDeleteIncidents: (List<BlockIncident>) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("block_log_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = tr("Block Log", "گزارش مسدودی‌ها"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        IncidentHistoryScreen(
            incidents = incidents,
            streakDays = streakDays,
            appLimits = appLimits,
            preferences = preferences,
            usageRefreshTrigger = usageRefreshTrigger,
            onClearHistory = onClearHistory,
            onDeleteIncidents = onDeleteIncidents,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * One row of the Custom Blacklist (#15): the regular keyword card wrapped
 * in a swipe-to-dismiss shell. Swiping the row left or right deletes the rule
 * (red delete background is revealed behind the card). While multi-select
 * mode is active the swipe gesture is disabled so taps and long-presses
 * drive selection instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomKeywordRow(
    keyword: BlockedKeyword,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onRowClick: () -> Unit,
    onRowLongClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value -> value != SwipeToDismissBoxValue.Settled }
    )

    // The dismiss only "settles" visually here; the actual removal happens
    // through onDelete, which takes the row out of the list entirely.
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            onDelete()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !isSelectionMode,
        enableDismissFromEndToStart = !isSelectionMode,
        modifier = Modifier.testTag("custom_keyword_row_${keyword.id}"),
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CrimsonPrimary)
                    .padding(horizontal = 20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    ) {
        KeywordItemCard(
            keyword = keyword,
            onToggle = onToggle,
            onDelete = onDelete,
            isSelectionMode = isSelectionMode,
            isSelected = isSelected,
            onRowClick = onRowClick,
            onRowLongClick = onRowLongClick
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeywordItemCard(
    keyword: BlockedKeyword,
    onToggle: () -> Unit,
    onDelete: (() -> Unit)?,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onRowClick: (() -> Unit)? = null,
    onRowLongClick: (() -> Unit)? = null
) {
    val isDomain = keyword.category == BlockedCategory.ADULT_DOMAIN ||
            keyword.category == BlockedCategory.CUSTOM_DOMAIN

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onRowClick != null || onRowLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = { onRowClick?.invoke() },
                        onLongClick = { onRowLongClick?.invoke() }
                    )
                } else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) CrimsonPrimary.copy(alpha = 0.18f) else ObsidianSurface
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, CrimsonPrimary)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onRowClick?.invoke() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = CrimsonPrimary,
                            uncheckedColor = ObsidianTextSecondary,
                            checkmarkColor = Color.White
                        ),
                        modifier = Modifier.testTag("keyword_select_checkbox_${keyword.id}")
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isDomain) Color(0xFF1E293B)
                            else CrimsonPrimary.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDomain) Icons.Default.Language else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isDomain) Color(0xFF94A3B8) else CrimsonPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = keyword.text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (keyword.isEnabled) Color.White else ObsidianTextSecondary
                    )
                    Text(
                        text = when (keyword.category) {
                            BlockedCategory.ADULT_EXPLICIT -> "Adult Word"
                            BlockedCategory.ADULT_DOMAIN -> "Adult Domain"
                            BlockedCategory.CUSTOM_KEYWORD -> "Custom Word"
                            BlockedCategory.CUSTOM_DOMAIN -> "Custom Domain"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = ObsidianTextSecondary
                    )
                }
            }

            // In selection mode the per-item actions step aside: the toolbar
            // owns deletion and row taps toggle selection (#15).
            if (!isSelectionMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = CrimsonPrimary.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Switch(
                        checked = keyword.isEnabled,
                        onCheckedChange = { onToggle() },
                        modifier = Modifier.testTag("toggle_${keyword.id}"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CrimsonPrimary,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = ObsidianBorder
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun AddCustomRuleDialog(
    onDismiss: () -> Unit,
    onAdd: (text: String, isDomain: Boolean) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var isDomain by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Text(
                text = "Add Block Rules (Single or Multi-Word)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            val detectedWordsCount = remember(inputText) {
                inputText.split(Regex("[,\\n\\r]+"))
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .size
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Add single or multiple words in English or Persian (فارسی). Separate multiple words by comma (,) or newlines:",
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianTextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = {
                        inputText = it
                        errorMessage = null
                    },
                    label = { Text(if (isDomain) "Website Domains" else "Words / Phrases") },
                    placeholder = {
                        Text(
                            if (isDomain) "e.g. badsite.com, adultpage.net"
                            else "e.g. trigger1, trigger2, پورن, سکسی"
                        )
                    },
                    minLines = 3,
                    maxLines = 6,
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(errorMessage ?: "", color = CrimsonPrimary)
                        } else if (detectedWordsCount > 0) {
                            Text(
                                "$detectedWordsCount item(s) will be added",
                                color = EmeraldShield
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_rule_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CrimsonPrimary,
                        unfocusedBorderColor = ObsidianCard,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Rule Type Radio Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = !isDomain,
                        onClick = { isDomain = false },
                        colors = RadioButtonDefaults.colors(selectedColor = CrimsonPrimary)
                    )
                    Text(
                        text = "Keywords / Phrases",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        modifier = Modifier.padding(start = 4.dp, end = 12.dp)
                    )

                    RadioButton(
                        selected = isDomain,
                        onClick = { isDomain = true },
                        colors = RadioButtonDefaults.colors(selectedColor = CrimsonPrimary)
                    )
                    Text(
                        text = "Website Domains",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = inputText.trim()
                    if (trimmed.isEmpty()) {
                        errorMessage = "Please enter one or more words or domains"
                        return@Button
                    }
                    onAdd(trimmed, isDomain)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrimsonPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_custom_rule_button")
            ) {
                Text(text = "Add to Blocklist", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = ObsidianTextSecondary)
            }
        }
    )
}
