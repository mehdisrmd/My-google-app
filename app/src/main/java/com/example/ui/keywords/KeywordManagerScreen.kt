package com.example.ui.keywords

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BlockedCategory
import com.example.data.model.BlockedKeyword
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianTextSecondary

@Composable
fun KeywordManagerScreen(
    keywords: List<BlockedKeyword>,
    onToggleKeyword: (BlockedKeyword) -> Unit,
    onDeleteKeyword: (Long) -> Unit,
    onAddKeyword: (text: String, isDomain: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredList = remember(keywords, selectedTab, searchQuery) {
        val tabFiltered = when (selectedTab) {
            0 -> keywords.filter {
                it.category == BlockedCategory.ADULT_EXPLICIT || it.category == BlockedCategory.ADULT_DOMAIN
            }
            else -> keywords.filter {
                it.category == BlockedCategory.CUSTOM_KEYWORD || it.category == BlockedCategory.CUSTOM_DOMAIN
            }
        }

        if (searchQuery.isBlank()) {
            tabFiltered
        } else {
            tabFiltered.filter { it.text.contains(searchQuery.trim(), ignoreCase = true) }
        }
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
                text = "Manage words & porn website domains that trigger immediate screen lockdown",
                style = MaterialTheme.typography.bodySmall,
                color = ObsidianTextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

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

            // Tabs: Adult Preset vs Custom Blacklist
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = ObsidianSurface,
                contentColor = CrimsonPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Adult Presets (${keywords.count { it.category == BlockedCategory.ADULT_EXPLICIT || it.category == BlockedCategory.ADULT_DOMAIN }})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) Color.White else ObsidianTextSecondary
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Custom Blacklist (${keywords.count { it.category == BlockedCategory.CUSTOM_KEYWORD || it.category == BlockedCategory.CUSTOM_DOMAIN }})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) Color.White else ObsidianTextSecondary
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                            else if (selectedTab == 1) "No custom rules added yet"
                            else "No preset keywords",
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
                        KeywordItemCard(
                            keyword = item,
                            onToggle = { onToggleKeyword(item) },
                            onDelete = if (selectedTab == 1) {
                                { onDeleteKeyword(item.id) }
                            } else null
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
                    selectedTab = 1
                }
            )
        }
    }
}

@Composable
fun KeywordItemCard(
    keyword: BlockedKeyword,
    onToggle: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val isDomain = keyword.category == BlockedCategory.ADULT_DOMAIN ||
            keyword.category == BlockedCategory.CUSTOM_DOMAIN

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
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
