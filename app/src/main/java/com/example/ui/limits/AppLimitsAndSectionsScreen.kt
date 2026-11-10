package com.example.ui.limits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppUsageLimit
import com.example.data.model.FocusScheduleSlot
import com.example.data.model.SectionBlockRule
import com.example.data.preferences.ShieldPreferences
import com.example.ui.focus.AddFocusSlotDialog
import com.example.ui.focus.SmartFocusSlotsList
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
import com.example.util.AppPackageResolver
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun AppLimitsAndSectionsScreen(
    appLimits: List<AppUsageLimit>,
    sectionRules: List<SectionBlockRule>,
    focusSlots: List<FocusScheduleSlot> = emptyList(),
    preferences: ShieldPreferences,
    onToggleAppLimit: (AppUsageLimit) -> Unit,
    onDeleteAppLimit: (String) -> Unit,
    onSaveAppLimit: (AppUsageLimit) -> Unit,
    onToggleSectionRule: (SectionBlockRule) -> Unit,
    onDeleteSectionRule: (Long) -> Unit,
    onAddSectionRule: (SectionBlockRule) -> Unit,
    onToggleFocusSlot: (FocusScheduleSlot) -> Unit = {},
    onDeleteFocusSlot: (Long) -> Unit = {},
    onAddFocusSlot: (FocusScheduleSlot) -> Unit = {},
    onStartFocusSlotNow: (FocusScheduleSlot) -> Unit = {},
    onTriggerUsageLockdown: (appName: String, limitMinutes: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddLimitDialog by remember { mutableStateOf(false) }
    var editingAppLimit by remember { mutableStateOf<AppUsageLimit?>(null) }
    var showAddSectionDialog by remember { mutableStateOf(false) }
    var showAddFocusSlotDialog by remember { mutableStateOf(false) }
    var cooldownNoticeMessage by remember { mutableStateOf<String?>(null) }
    var refreshNoticeMessage by remember { mutableStateOf<String?>(null) }

    val usageRefreshTick by preferences.usageRefreshTrigger.collectAsStateWithLifecycle()
    val isSmartFocusEnabled by preferences.isSmartFocusEnabled.collectAsStateWithLifecycle()
    val isSectionExitOnlyMode by preferences.isSectionExitOnlyMode.collectAsStateWithLifecycle()
    var hasUsageStatsPerm by remember {
        mutableStateOf(ShieldPreferences.hasUsageStatsPermission(context))
    }

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(5000L)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsageStatsPerm = ShieldPreferences.hasUsageStatsPermission(context)
                nowMs = System.currentTimeMillis()
                preferences.refreshLiveUsageStats()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr("App Limits, Sections & Smart Focus", "محدودیت برنامه‌ها، بخش‌ها و تمرکز هوشمند"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianTextPrimary
                    )
                    Text(
                        text = tr(
                            "Daily app limits (2h cooldown), stop Instagram Reels / Telegram Search, or schedule Deep Work",
                            "محدودیت روزانه (با قفل ۲ ساعته)، توقف ریلز اینستاگرام و جستجوی تلگرام، یا زمان‌بندی تمرکز"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = ObsidianTextSecondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                }

                IconButton(
                    onClick = {
                        nowMs = System.currentTimeMillis()
                        preferences.refreshLiveUsageStats()
                        refreshNoticeMessage = tr(
                            "Usage counters & timers refreshed!",
                            "آمار مصرف و تایمرها بروزرسانی شدند!"
                        )
                    },
                    modifier = Modifier.testTag("refresh_all_limits_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Usage",
                        tint = CyanTelemetry
                    )
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = ObsidianSurface,
                contentColor = CrimsonPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.testTag("limits_tab_apps"),
                    text = {
                        Text(
                            text = tr("Limits (${appLimits.size})", "محدودیت (${appLimits.size})"),
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) ObsidianTextPrimary else ObsidianTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.testTag("limits_tab_sections"),
                    text = {
                        Text(
                            text = tr("Sections (${sectionRules.size})", "بخش‌ها (${sectionRules.size})"),
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) ObsidianTextPrimary else ObsidianTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    modifier = Modifier.testTag("limits_tab_focus"),
                    text = {
                        Text(
                            text = tr("Smart Focus (${focusSlots.size})", "تمرکز (${focusSlots.size})"),
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 2) ObsidianTextPrimary else ObsidianTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            refreshNoticeMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldShield.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, EmeraldShield),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .testTag("refresh_notice_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldShield,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { refreshNoticeMessage = null }) {
                            Text(tr("OK", "تایید"), color = EmeraldShield, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            cooldownNoticeMessage?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .testTag("cooldown_warning_banner"),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AmberWarning)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = ObsidianTextPrimary
                            )
                        }
                        TextButton(onClick = { cooldownNoticeMessage = null }) {
                            Text(tr("OK", "باشه"), color = AmberWarning, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    if (!hasUsageStatsPerm) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = AmberWarning,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = tr("Sync OS Screen Time (Optional)", "همگام‌سازی زمان مصرف سیستم (اختیاری)"),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = ObsidianTextPrimary
                                        )
                                        Text(
                                            text = tr(
                                                "Grant Usage Access for system-wide app usage detection alongside Accessibility tracking",
                                                "اعطای دسترسی Usage Access برای تشخیص دقیق زمان استفاده از برنامه‌ها"
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ObsidianTextSecondary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { ShieldPreferences.requestUsageStatsPermission(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(tr("Grant", "اعطا"), color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    AppLimitsList(
                        appLimits = appLimits,
                        preferences = preferences,
                        usageRefreshTick = usageRefreshTick,
                        nowMs = nowMs,
                        onToggle = { limit ->
                            if (limit.isEnabled && limit.isCooldownActive(nowMs)) {
                                cooldownNoticeMessage = tr(
                                    "2-Hour Cooldown Active: You must wait ${limit.formatRemainingCooldown(nowMs)} before disabling ${limit.appName}'s limit.",
                                    "قفل ۲ ساعته فعال است: باید ${limit.formatRemainingCooldown(nowMs)} صبر کنید تا محدودیت ${limit.appName} را غیرفعال کنید."
                                )
                            } else {
                                onToggleAppLimit(limit)
                            }
                        },
                        onEditLimit = { limit ->
                            if (limit.isCooldownActive(nowMs)) {
                                cooldownNoticeMessage = tr(
                                    "2-Hour Cooldown Active: You must wait ${limit.formatRemainingCooldown(nowMs)} before changing ${limit.appName}'s timer.",
                                    "قفل ۲ ساعته فعال است: باید ${limit.formatRemainingCooldown(nowMs)} صبر کنید تا تایمر ${limit.appName} را تغییر دهید."
                                )
                            } else {
                                editingAppLimit = limit
                            }
                        },
                        onDelete = { limit ->
                            if (limit.isCooldownActive(nowMs)) {
                                cooldownNoticeMessage = tr(
                                    "2-Hour Cooldown Active: You must wait ${limit.formatRemainingCooldown(nowMs)} before removing ${limit.appName}'s limit.",
                                    "قفل ۲ ساعته فعال است: باید ${limit.formatRemainingCooldown(nowMs)} صبر کنید تا محدودیت ${limit.appName} را حذف کنید."
                                )
                            } else {
                                onDeleteAppLimit(limit.packageName)
                            }
                        },
                        onRefreshUsage = { limit ->
                            preferences.resetTodayAppUsage(limit.packageName, limit.appName)
                            nowMs = System.currentTimeMillis()
                            refreshNoticeMessage = tr(
                                "${limit.appName} usage timer refreshed & reset to 0m 0s!",
                                "تایمر مصرف ${limit.appName} بروزرسانی و صفر شد (0m 0s)!"
                            )
                        },
                        onTriggerUsageLockdown = onTriggerUsageLockdown
                    )
                }

                1 -> {
                    SectionRulesList(
                        sectionRules = sectionRules,
                        isSectionExitOnlyMode = isSectionExitOnlyMode,
                        onToggleSectionExitMode = { exitOnly ->
                            preferences.setSectionExitOnlyMode(exitOnly)
                        },
                        onToggle = onToggleSectionRule,
                        onDelete = onDeleteSectionRule
                    )
                }

                2 -> {
                    SmartFocusSlotsList(
                        focusSlots = focusSlots,
                        isSmartFocusEnabled = isSmartFocusEnabled,
                        onToggleSmartFocusMaster = { enabled ->
                            preferences.setSmartFocusEnabled(enabled)
                        },
                        onToggleSlot = onToggleFocusSlot,
                        onDeleteSlot = onDeleteFocusSlot,
                        onSaveSlot = onAddFocusSlot,
                        onStartSlotNow = onStartFocusSlotNow,
                        onStartQuickSprint = { sprintMins ->
                            onStartFocusSlotNow(
                                FocusScheduleSlot(
                                    title = "Deep Work Sprint (${sprintMins}m)",
                                    startHour = 9,
                                    startMinute = 0,
                                    endHour = 9 + (sprintMins / 60),
                                    endMinute = sprintMins % 60
                                )
                            )
                        }
                    )
                }
            }
        }

        // FAB to add limit, section rule, or Smart Focus slot
        FloatingActionButton(
            onClick = {
                when (selectedTab) {
                    0 -> showAddLimitDialog = true
                    1 -> showAddSectionDialog = true
                    2 -> showAddFocusSlotDialog = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_limit_or_section_fab"),
            containerColor = CrimsonPrimary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Rule")
        }

        if (showAddLimitDialog || editingAppLimit != null) {
            AddAppLimitDialog(
                initialLimit = editingAppLimit,
                existingLimits = appLimits,
                nowMs = nowMs,
                onDismiss = {
                    showAddLimitDialog = false
                    editingAppLimit = null
                },
                onCooldownBlocked = { blockedMsg ->
                    cooldownNoticeMessage = blockedMsg
                    showAddLimitDialog = false
                    editingAppLimit = null
                },
                onSave = { limit ->
                    onSaveAppLimit(limit)
                    showAddLimitDialog = false
                    editingAppLimit = null
                }
            )
        }

        if (showAddSectionDialog) {
            AddSectionRuleDialog(
                onDismiss = { showAddSectionDialog = false },
                onSave = { rule ->
                    onAddSectionRule(rule)
                    showAddSectionDialog = false
                }
            )
        }

        if (showAddFocusSlotDialog) {
            AddFocusSlotDialog(
                onDismiss = { showAddFocusSlotDialog = false },
                onSave = { slot ->
                    onAddFocusSlot(slot)
                    showAddFocusSlotDialog = false
                }
            )
        }
    }
}

@Composable
fun AppLimitsList(
    appLimits: List<AppUsageLimit>,
    preferences: ShieldPreferences,
    usageRefreshTick: Long,
    nowMs: Long,
    onToggle: (AppUsageLimit) -> Unit,
    onEditLimit: (AppUsageLimit) -> Unit,
    onDelete: (AppUsageLimit) -> Unit,
    onRefreshUsage: (AppUsageLimit) -> Unit,
    onTriggerUsageLockdown: (appName: String, limitMinutes: Int) -> Unit
) {
    val context = LocalContext.current

    if (appLimits.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ObsidianBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassBottom,
                        contentDescription = null,
                        tint = CyanTelemetry,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = tr("No App Limits Added Yet", "هنوز هیچ محدودیتی برای برنامه‌ها اضافه نشده است"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianTextPrimary
                    )
                    Text(
                        text = tr(
                            "Tap the + button below to add Instagram, Telegram, YouTube, Chrome, or any installed app.",
                            "روی دکمه + در پایین صفحه بزنید تا اینستاگرام، تلگرام، یوتیوب یا هر برنامه نصب‌شده را اضافه کنید."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = ObsidianTextSecondary
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(appLimits, key = { it.packageName }) { limit ->
                val usedSeconds = remember(usageRefreshTick, nowMs, limit.packageName, limit.appName) {
                    preferences.getTodayAppUsageSeconds(limit.packageName, limit.appName)
                }
                val isInstalledOnDevice = remember(context, limit.packageName, limit.appName) {
                    AppPackageResolver.isAppLimitInstalledOnDevice(context, limit.packageName, limit.appName)
                }
                val usedMinutes = usedSeconds / 60
                val progress = if (limit.dailyLimitMinutes > 0) {
                    (usedSeconds.toFloat() / (limit.dailyLimitMinutes * 60f)).coerceIn(0f, 1f)
                } else 0f
                val isLimitExceeded = usedMinutes >= limit.dailyLimitMinutes && limit.isEnabled
                val isOnCooldown = limit.isCooldownActive(nowMs)
                val cooldownLabel = limit.formatRemainingCooldown(nowMs)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_limit_card_${limit.packageName}"),
                    colors = CardDefaults.cardColors(
                        containerColor = ObsidianSurface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isLimitExceeded) CrimsonPrimary else ObsidianBorder
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
                                            if (isLimitExceeded) CrimsonPrimary.copy(alpha = 0.2f)
                                            else ObsidianCard
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HourglassBottom,
                                        contentDescription = null,
                                        tint = if (isLimitExceeded) CrimsonPrimary else CyanTelemetry,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = limit.appName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = ObsidianTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isInstalledOnDevice) {
                                                EmeraldShield.copy(alpha = 0.16f)
                                            } else {
                                                IndigoAccent.copy(alpha = 0.16f)
                                            }
                                        ) {
                                            Text(
                                                text = if (isInstalledOnDevice) tr("INSTALLED", "نصب‌شده") else tr("PRE-BLOCK", "پیش‌مسدود"),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isInstalledOnDevice) EmeraldShield else CyanTelemetry,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = tr(
                                            "Limit: ${limit.dailyLimitMinutes / 60}h ${limit.dailyLimitMinutes % 60}m/day • ${limit.packageName}",
                                            "محدودیت: ${limit.dailyLimitMinutes / 60} ساعت و ${limit.dailyLimitMinutes % 60} دقیقه • ${limit.packageName}"
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ObsidianTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onEditLimit(limit) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("edit_limit_${limit.packageName}")
                                ) {
                                    Icon(
                                        imageVector = if (isOnCooldown) Icons.Default.Lock else Icons.Default.Edit,
                                        contentDescription = "Edit Timer",
                                        tint = if (isOnCooldown) AmberWarning else CyanTelemetry,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onDelete(limit) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("delete_limit_${limit.packageName}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = if (isOnCooldown) ObsidianTextSecondary.copy(alpha = 0.4f) else CrimsonPrimary.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Switch(
                                    checked = limit.isEnabled,
                                    onCheckedChange = { onToggle(limit) },
                                    modifier = Modifier.testTag("toggle_limit_${limit.packageName}"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = CrimsonPrimary
                                    )
                                )
                            }
                        }

                        // 2-Hour Change Cooldown Status Badge
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isOnCooldown) AmberWarning.copy(alpha = 0.14f) else EmeraldShield.copy(alpha = 0.12f),
                            border = BorderStroke(
                                1.dp,
                                if (isOnCooldown) AmberWarning.copy(alpha = 0.45f) else EmeraldShield.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cooldown_badge_${limit.packageName}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (isOnCooldown) AmberWarning else EmeraldShield,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isOnCooldown) {
                                            tr(
                                                "Timer Locked (2h Cooldown): Wait $cooldownLabel to change",
                                                "تایمر قفل است (قفل ۲ ساعته): $cooldownLabel صبر کنید"
                                            )
                                        } else {
                                            tr(
                                                "Timer Unlocked: Editable (Locks for 2h after change)",
                                                "تایمر باز است: قابل ویرایش (پس از تغییر ۲ ساعت قفل می‌شود)"
                                            )
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = if (isOnCooldown) AmberWarning else EmeraldShield
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress Bar for Today's Usage
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = tr(
                                    "Today's used: ${usedMinutes}m ${usedSeconds % 60}s / ${limit.dailyLimitMinutes}m",
                                    "مصرف امروز: ${usedMinutes}m ${usedSeconds % 60}s از ${limit.dailyLimitMinutes}m"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isLimitExceeded) CrimsonPrimary else ObsidianTextSecondary
                            )
                            Text(
                                text = if (isLimitExceeded) tr("TIME LIMIT EXCEEDED — BLOCKED UNTIL MIDNIGHT", "پایان زمان — تا نیمه‌شب مسدود است") else "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isLimitExceeded) CrimsonPrimary else EmeraldShield
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isLimitExceeded) CrimsonPrimary else EmeraldShield,
                            trackColor = ObsidianCard
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action Controls: Refresh / Reset Usage Button (Always Enabled & Working) + Open App
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onRefreshUsage(limit) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("reset_usage_${limit.packageName}"),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                border = BorderStroke(1.dp, EmeraldShield.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Usage",
                                    tint = EmeraldShield,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tr("Refresh / Reset Timer", "بروزرسانی / صفر کردن تایمر"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ObsidianTextPrimary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            if (isInstalledOnDevice) {
                                OutlinedButton(
                                    onClick = {
                                        val currentMins = preferences.getTodayAppUsageMinutes(limit.packageName, limit.appName)
                                        if (limit.isEnabled && currentMins >= limit.dailyLimitMinutes) {
                                            onTriggerUsageLockdown(limit.appName, limit.dailyLimitMinutes)
                                        } else {
                                            preferences.addAppUsageSeconds(limit.packageName, 15)
                                            AppPackageResolver.launchLimitedApp(context, limit.packageName, limit.appName)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("open_limited_app_${limit.packageName}"),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    border = BorderStroke(1.dp, CyanTelemetry.copy(alpha = 0.5f))
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Launch,
                                        contentDescription = null,
                                        tint = CyanTelemetry,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = tr("Open App", "باز کردن برنامه"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyanTelemetry,
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
    }
}

@Composable
fun SectionRulesList(
    sectionRules: List<SectionBlockRule>,
    isSectionExitOnlyMode: Boolean = true,
    onToggleSectionExitMode: (Boolean) -> Unit = {},
    onToggle: (SectionBlockRule) -> Unit,
    onDelete: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("section_exit_mode_card"),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, EmeraldShield.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 10.dp)
                    ) {
                        Text(
                            text = tr("Stop Section Only (Do Not Block Whole App)", "توقف فقط همان بخش (بدون بستن کل برنامه)"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianTextPrimary
                        )
                        Text(
                            text = tr(
                                "When ON, entering Instagram Reels or Telegram Search Box immediately backs out of that section while letting you use the rest of Instagram or Telegram normally.",
                                "وقتی روشن باشد، فقط هنگام ورود به ریلز اینستاگرام یا باکس جستجوی تلگرام از آن بخش خارج می‌شود و کل برنامه را مسدود نمی‌کند."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = ObsidianTextSecondary
                        )
                    }

                    Switch(
                        checked = isSectionExitOnlyMode,
                        onCheckedChange = onToggleSectionExitMode,
                        modifier = Modifier.testTag("section_exit_only_mode_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = EmeraldShield
                        )
                    )
                }
            }
        }

        items(sectionRules, key = { it.id }) { rule ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ObsidianBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (rule.isEnabled) CrimsonPrimary.copy(alpha = 0.15f)
                                        else ObsidianCard
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = if (rule.isEnabled) CrimsonPrimary else ObsidianTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = rule.sectionName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (rule.isEnabled) ObsidianTextPrimary else ObsidianTextSecondary
                                )
                                Text(
                                    text = "Package: ${rule.targetPackage}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ObsidianTextSecondary
                                )
                                Text(
                                    text = tr(
                                        "Stops only this section when opened • Pattern: \"${rule.triggerPattern}\"",
                                        "فقط همین بخش را متوقف می‌کند • الگو: \"${rule.triggerPattern}\""
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AmberWarning
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onDelete(rule.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = CrimsonPrimary.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Switch(
                                checked = rule.isEnabled,
                                onCheckedChange = { onToggle(rule) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = CrimsonPrimary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddAppLimitDialog(
    initialLimit: AppUsageLimit? = null,
    existingLimits: List<AppUsageLimit> = emptyList(),
    nowMs: Long = System.currentTimeMillis(),
    onDismiss: () -> Unit,
    onCooldownBlocked: (String) -> Unit = {},
    onSave: (AppUsageLimit) -> Unit
) {
    val context = LocalContext.current
    val installedApps = remember(context) { AppPackageResolver.queryInstalledApps(context) }
    val popularApps = remember(context) { AppPackageResolver.getPopularPresetApps(context) }

    val defaultInitialApp = remember(installedApps, popularApps, initialLimit) {
        if (initialLimit != null) {
            Pair(initialLimit.appName, initialLimit.packageName)
        } else {
            val firstInstalled = installedApps.firstOrNull {
                it.packageName.contains("chrome") || it.packageName.contains("youtube") || it.packageName.contains("settings")
            } ?: installedApps.firstOrNull()
            if (firstInstalled != null) {
                Pair(firstInstalled.appName, firstInstalled.packageName)
            } else {
                Pair("Instagram", "com.instagram.android")
            }
        }
    }

    var selectedAppName by remember { mutableStateOf(defaultInitialApp.first) }
    var selectedPackage by remember { mutableStateOf(defaultInitialApp.second) }
    var searchQuery by remember { mutableStateOf("") }

    var limitHours by remember {
        mutableFloatStateOf(
            if (initialLimit != null) (initialLimit.dailyLimitMinutes / 60f).coerceIn(0.25f, 8f)
            else 1.5f
        )
    }
    var showCustomInput by remember { mutableStateOf(false) }

    val filteredInstalledApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter {
            it.appName.contains(searchQuery.trim(), ignoreCase = true) ||
                it.packageName.contains(searchQuery.trim(), ignoreCase = true)
        }
    }

    val filteredPopularApps = remember(popularApps, searchQuery) {
        if (searchQuery.isBlank()) popularApps
        else popularApps.filter {
            it.appName.contains(searchQuery.trim(), ignoreCase = true) ||
                it.packageName.contains(searchQuery.trim(), ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Text(
                text = if (initialLimit != null) tr("Edit App Time Limit", "ویرایش محدودیت زمانی برنامه") else tr("Add App to Daily Limitations", "افزودن برنامه به محدودیت روزانه"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ObsidianTextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AmberWarning.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr(
                                "Strict 2-Hour Rule: Once saved, you must wait 2 hours before changing or disabling this app's timer.",
                                "قانون قفل ۲ ساعته: پس از ذخیره، باید حداقل ۲ ساعت صبر کنید تا بتوانید تایمر این برنامه را تغییر دهید."
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberWarning
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (initialLimit == null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianCard,
                        border = BorderStroke(1.dp, CyanTelemetry.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                text = tr("Selected: ${selectedAppName.ifBlank { "None" }}", "انتخاب شده: ${selectedAppName.ifBlank { "هیچکدام" }}"),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianTextPrimary
                            )
                            Text(
                                text = "Package: ${selectedPackage.ifBlank { "Auto-resolved on save" }}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = CyanTelemetry
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text(tr("Search installed or popular apps...", "جستجوی برنامه‌های نصب‌شده یا محبوب...")) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_app_limit_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanTelemetry,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedTextColor = ObsidianTextPrimary,
                            unfocusedTextColor = ObsidianTextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (filteredInstalledApps.isNotEmpty()) {
                        Text(
                            text = tr("Installed on This Device (${filteredInstalledApps.size}):", "نصب‌شده روی این دستگاه (${filteredInstalledApps.size}):"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldShield
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            filteredInstalledApps.forEach { appInfo ->
                                val isSelected = selectedPackage == appInfo.packageName && !showCustomInput
                                Surface(
                                    onClick = {
                                        showCustomInput = false
                                        selectedAppName = appInfo.appName
                                        selectedPackage = appInfo.packageName
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) CrimsonPrimary else ObsidianCard,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) CrimsonPrimary else EmeraldShield.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.testTag("app_chip_${appInfo.packageName}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else EmeraldShield,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = appInfo.appName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) Color.White else ObsidianTextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Text(
                        text = tr("Popular Social & Media Presets:", "برنامه‌های محبوب:"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filteredPopularApps.forEach { appInfo ->
                            val isSelected = selectedPackage == appInfo.packageName &&
                                selectedAppName == appInfo.appName &&
                                !showCustomInput
                            Surface(
                                onClick = {
                                    showCustomInput = false
                                    selectedAppName = appInfo.appName
                                    selectedPackage = appInfo.packageName
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) CrimsonPrimary else ObsidianCard,
                                border = if (isSelected) null else BorderStroke(1.dp, ObsidianBorder),
                                modifier = Modifier.testTag("app_chip_${appInfo.packageName}")
                            ) {
                                Text(
                                    text = appInfo.appName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.White else ObsidianTextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                val nextState = !showCustomInput
                                showCustomInput = nextState
                                if (nextState) {
                                    selectedAppName = ""
                                    selectedPackage = ""
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (showCustomInput) IndigoAccent else ObsidianCard,
                            border = BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.6f)),
                            modifier = Modifier.testTag("custom_app_toggle_chip")
                        ) {
                            Text(
                                text = tr("+ Custom App", "+ برنامه دلخواه"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (showCustomInput) Color.White else ObsidianTextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (showCustomInput) {
                        OutlinedTextField(
                            value = selectedAppName,
                            onValueChange = { typedName ->
                                selectedAppName = typedName
                                selectedPackage = AppPackageResolver.resolvePackageForCustomInput(
                                    appNameInput = typedName,
                                    packageInput = "",
                                    installedApps = installedApps
                                )
                            },
                            label = { Text(tr("App Name (e.g. Discord, Chrome)", "نام برنامه")) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_app_name_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = ObsidianTextPrimary,
                                unfocusedTextColor = ObsidianTextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = selectedPackage,
                            onValueChange = { selectedPackage = it },
                            label = { Text(tr("Package Name (auto-filled or custom)", "نام پکیج")) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_app_package_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = ObsidianTextPrimary,
                                unfocusedTextColor = ObsidianTextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                } else {
                    Text(
                        text = "App: ${initialLimit.appName} (${initialLimit.packageName})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianTextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                val totalMinutes = (limitHours * 60).roundToInt().coerceAtLeast(15)
                Text(
                    text = tr(
                        "Daily Limit: ${totalMinutes / 60}h ${totalMinutes % 60}m ($totalMinutes mins)",
                        "محدودیت روزانه: ${totalMinutes / 60} ساعت و ${totalMinutes % 60} دقیقه"
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianTextPrimary
                )

                Slider(
                    value = limitHours,
                    onValueChange = { limitHours = it },
                    valueRange = 0.25f..8f,
                    steps = 30,
                    modifier = Modifier.testTag("app_limit_hours_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = CrimsonPrimary,
                        activeTrackColor = CrimsonPrimary
                    )
                )

                // Quick presets for the daily budget (Feature 5)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(15, 30, 60, 120, 240).forEach { presetMinutes ->
                        val isPresetSelected = (limitHours * 60).toInt() == presetMinutes
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isPresetSelected) CrimsonPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { limitHours = presetMinutes / 60f }
                                .testTag("app_limit_preset_$presetMinutes")
                        ) {
                            Text(
                                text = if (presetMinutes < 60) "${presetMinutes}m" else "${presetMinutes / 60}h",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isPresetSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isPresetSelected) Color.White else ObsidianTextPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = selectedAppName.trim().ifEmpty { "Custom App" }
                    val cleanPkg = AppPackageResolver.resolvePackageForCustomInput(
                        appNameInput = cleanName,
                        packageInput = selectedPackage,
                        installedApps = installedApps
                    )
                    val existing = existingLimits.find { it.packageName == cleanPkg }
                    if (existing != null && existing.isCooldownActive(nowMs)) {
                        onCooldownBlocked(
                            tr(
                                "2-Hour Cooldown Active: Wait ${existing.formatRemainingCooldown(nowMs)} before modifying ${existing.appName}'s limit.",
                                "قفل ۲ ساعته فعال است: ${existing.formatRemainingCooldown(nowMs)} صبر کنید."
                            )
                        )
                    } else {
                        val totalMinutes = (limitHours * 60).roundToInt().coerceAtLeast(15)
                        onSave(
                            AppUsageLimit(
                                packageName = cleanPkg,
                                appName = cleanName,
                                dailyLimitMinutes = totalMinutes,
                                isEnabled = true,
                                lastModifiedTimestamp = System.currentTimeMillis()
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                modifier = Modifier.testTag("save_app_limit_button")
            ) {
                Text(tr("Lock Limit (2h Cooldown)", "ذخیره محدودیت (قفل ۲ ساعته)"), color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Cancel", "انصراف"), color = ObsidianTextSecondary)
            }
        }
    )
}

@Composable
fun AddSectionRuleDialog(
    onDismiss: () -> Unit,
    onSave: (SectionBlockRule) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var pkg by remember { mutableStateOf("com.instagram.android") }
    var pattern by remember { mutableStateOf("reels") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Text(
                text = tr("Add Section Blocker Rule", "افزودن قانون مسدودسازی بخش"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ObsidianTextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = tr(
                        "Stop specific tabs or sections inside apps (e.g. Reels in Instagram or Search Box in Telegram):",
                        "توقف بخش‌های خاص داخل برنامه‌ها (مثل ریلز اینستاگرام یا باکس جستجوی تلگرام):"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianTextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(tr("Section Name (e.g. Instagram Reels)", "نام بخش")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ObsidianTextPrimary,
                        unfocusedTextColor = ObsidianTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = pkg,
                    onValueChange = { pkg = it },
                    label = { Text(tr("App Package Name", "نام پکیج برنامه")) },
                    placeholder = { Text("e.g. org.telegram.messenger") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ObsidianTextPrimary,
                        unfocusedTextColor = ObsidianTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text(tr("Trigger text or section keyword", "کلمه کلیدی یا شناسه بخش")) },
                    placeholder = { Text("e.g. reels or search") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ObsidianTextPrimary,
                        unfocusedTextColor = ObsidianTextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && pkg.isNotBlank() && pattern.isNotBlank()) {
                        onSave(
                            SectionBlockRule(
                                targetPackage = pkg.trim(),
                                sectionName = name.trim(),
                                triggerPattern = pattern.trim(),
                                isEnabled = true
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
            ) {
                Text(tr("Add Section", "افزودن بخش"), color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Cancel", "انصراف"), color = ObsidianTextSecondary)
            }
        }
    )
}
