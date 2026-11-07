package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notification.LockdownNotificationManager
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.focus.SmartFocusEngine
import com.example.ui.i18n.AppStrings.tr
import com.example.ui.info.GithubUpdateChecker
import com.example.ui.info.GithubUpdateStatus
import com.example.ui.info.InfoAndUpdateScreen
import com.example.ui.keywords.KeywordManagerScreen
import com.example.ui.limits.AppLimitsAndSectionsScreen
import com.example.ui.lockdown.LockdownActivity
import com.example.ui.panic.PanicModeDialog
import com.example.ui.security.BiometricLockOverlay
import com.example.ui.security.BlackoutPrivacyCurtain
import com.example.ui.streak.NoBeepStreakScreen
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.CyanTelemetry
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianTextPrimary
import com.example.ui.theme.ObsidianTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ScreenTab {
    DASHBOARD,
    STREAK,
    LIMITS,
    KEYWORDS,
    INFO
}

class MainActivity : ComponentActivity() {

    private var openPanicModeRequest by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        syncWindowSecureFlag()

        // Initialize persistent lockdown & always-on Panic SOS notifications
        LockdownNotificationManager.createNotificationChannel(this)
        LockdownNotificationManager.showOrUpdateAlwaysOnPanicNotification(this)

        handleNotificationIntent(intent)

        setContent {
            val app = ShieldApplication.instance
            val isDarkTheme by app.preferences.isDarkTheme.collectAsStateWithLifecycle()
            val isPersianLanguage by app.preferences.isPersianLanguage.collectAsStateWithLifecycle()

            MyApplicationTheme(
                darkTheme = isDarkTheme,
                isPersianRtl = isPersianLanguage
            ) {
                MainContent(
                    openPanicModeExternal = openPanicModeRequest,
                    onConsumePanicModeExternal = { openPanicModeRequest = false },
                    onSyncWindowSecureFlag = { syncWindowSecureFlag() },
                    onTriggerLockdown = { triggerWord, minutes ->
                        triggerLockdown(triggerWord, minutes)
                    }
                )
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Block CTRL key shortcuts
        if (event.keyCode == KeyEvent.KEYCODE_CTRL_LEFT ||
            event.keyCode == KeyEvent.KEYCODE_CTRL_RIGHT ||
            event.isCtrlPressed
        ) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    fun syncWindowSecureFlag() {
        val app = runCatching { ShieldApplication.instance }.getOrNull() ?: return
        val shouldApplySecure = app.preferences.isFlagSecureCurrentlyActive()
        if (shouldApplySecure) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        val isQuickLock = intent.action == LockdownNotificationManager.ACTION_QUICK_EMERGENCY_LOCK ||
            intent.getBooleanExtra(EXTRA_QUICK_EMERGENCY_LOCK, false)
        if (isQuickLock) {
            intent.removeExtra(EXTRA_QUICK_EMERGENCY_LOCK)
            intent.action = null
            val app = runCatching { ShieldApplication.instance }.getOrNull()
            app?.preferences?.incrementUrgesDefeated()
            val lockMins = app?.preferences?.getLockDurationMinutes() ?: 15
            triggerLockdown("Notification Quick Panic Lock (${lockMins}m)", lockMins)
            return
        }

        val isOpenPanic = intent.action == LockdownNotificationManager.ACTION_OPEN_PANIC_MODE ||
            intent.getBooleanExtra(EXTRA_OPEN_PANIC_MODE, false)
        if (isOpenPanic) {
            intent.removeExtra(EXTRA_OPEN_PANIC_MODE)
            intent.action = null
            openPanicModeRequest = true
        }
    }

    override fun onResume() {
        super.onResume()
        syncWindowSecureFlag()
        LockdownNotificationManager.showOrUpdateAlwaysOnPanicNotification(this)
        checkActiveLockdown()
    }

    private fun checkActiveLockdown() {
        val app = runCatching { ShieldApplication.instance }.getOrNull() ?: return
        if (app.preferences.isCurrentlyLocked()) {
            val intent = Intent(this, LockdownActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(intent)
        }
    }

    private fun triggerLockdown(triggerWord: String, minutes: Int) {
        val app = ShieldApplication.instance
        val endTime = app.preferences.startLockdown(
            durationMinutes = minutes,
            triggerWord = triggerWord,
            sourceApp = "X Blocker Enforcement"
        )
        LockdownNotificationManager.startLockdownNotification(
            context = this,
            endTimeEpoch = endTime,
            triggerWord = triggerWord
        )
        val intent = Intent(this, LockdownActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(LockdownActivity.EXTRA_STRICT_MODE, true)
        }
        startActivity(intent)
    }

    companion object {
        const val EXTRA_OPEN_PANIC_MODE = "extra_open_panic_mode"
        const val EXTRA_QUICK_EMERGENCY_LOCK = "extra_quick_emergency_lock"
    }
}

@Composable
fun MainContent(
    openPanicModeExternal: Boolean = false,
    onConsumePanicModeExternal: () -> Unit = {},
    onSyncWindowSecureFlag: () -> Unit = {},
    onTriggerLockdown: (triggerWord: String, minutes: Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val app = ShieldApplication.instance
    val repository = app.repository
    val preferences = app.preferences
    val coroutineScope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf(ScreenTab.DASHBOARD) }
    var showPanicModeDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            LockdownNotificationManager.showOrUpdateAlwaysOnPanicNotification(context)
        }
    }

    // Request POST_NOTIFICATIONS automatically on Android 13+ so Panic SOS Notification Bar works immediately
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !LockdownNotificationManager.hasNotificationPermission(context)
        ) {
            runCatching {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            LockdownNotificationManager.showOrUpdateAlwaysOnPanicNotification(context)
        }
    }

    val isProtectionEnabled by preferences.isProtectionEnabled.collectAsStateWithLifecycle()
    val lockDurationMinutes by preferences.lockDurationMinutes.collectAsStateWithLifecycle()
    val strictModeEnabled by preferences.strictModeEnabled.collectAsStateWithLifecycle()

    val isBiometricLockEnabled by preferences.isBiometricLockEnabled.collectAsStateWithLifecycle()
    val isBlackoutPrivacyShieldEnabled by preferences.isBlackoutPrivacyShieldEnabled.collectAsStateWithLifecycle()
    val screenshotTempUnlockUntil by preferences.screenshotTemporaryUnlockUntil.collectAsStateWithLifecycle()
    val isSmartFocusEnabled by preferences.isSmartFocusEnabled.collectAsStateWithLifecycle()

    val isDarkTheme by preferences.isDarkTheme.collectAsStateWithLifecycle()
    val isPersianLanguage by preferences.isPersianLanguage.collectAsStateWithLifecycle()
    val githubRepoSlug by preferences.githubRepoSlug.collectAsStateWithLifecycle()

    var githubUpdateStatus by remember { mutableStateOf(GithubUpdateStatus()) }

    val runBackgroundGithubUpdateCheck = {
        coroutineScope.launch {
            githubUpdateStatus = githubUpdateStatus.copy(isChecking = true)
            val result = GithubUpdateChecker.checkLatestReleaseFromGithub(githubRepoSlug)
            githubUpdateStatus = result
        }
    }

    LaunchedEffect(githubRepoSlug) {
        runBackgroundGithubUpdateCheck()
    }

    var isBiometricSessionLocked by remember {
        mutableStateOf(preferences.getIsBiometricLockEnabled())
    }
    var isAppInBackgroundCurtain by remember { mutableStateOf(false) }

    LaunchedEffect(isBlackoutPrivacyShieldEnabled, screenshotTempUnlockUntil) {
        onSyncWindowSecureFlag()
    }

    DisposableEffect(lifecycleOwner, isBiometricLockEnabled, isBlackoutPrivacyShieldEnabled) {
        if (!isBlackoutPrivacyShieldEnabled) {
            isAppInBackgroundCurtain = false
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    runBackgroundGithubUpdateCheck()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    isAppInBackgroundCurtain = isBlackoutPrivacyShieldEnabled
                }
                Lifecycle.Event.ON_RESUME -> {
                    isAppInBackgroundCurtain = false
                    onSyncWindowSecureFlag()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(openPanicModeExternal) {
        if (openPanicModeExternal) {
            showPanicModeDialog = true
            onConsumePanicModeExternal()
        }
    }

    val streakStartTimestamp by preferences.streakStartTimestamp.collectAsStateWithLifecycle()
    val longestStreakDays by preferences.longestStreakDays.collectAsStateWithLifecycle()
    val slipsCount by preferences.slipsCount.collectAsStateWithLifecycle()
    val urgesDefeatedCount by preferences.urgesDefeatedCount.collectAsStateWithLifecycle()
    val personalPledge by preferences.personalPledge.collectAsStateWithLifecycle()
    val usageRefreshTrigger by preferences.usageRefreshTrigger.collectAsStateWithLifecycle()

    val keywords by repository.allKeywords.collectAsStateWithLifecycle(initialValue = emptyList())
    val incidents by repository.allIncidents.collectAsStateWithLifecycle(initialValue = emptyList())
    val incidentCount by repository.incidentCount.collectAsStateWithLifecycle(initialValue = 0)

    val appLimits by repository.allAppUsageLimits.collectAsStateWithLifecycle(initialValue = emptyList())
    val sectionRules by repository.allSectionRules.collectAsStateWithLifecycle(initialValue = emptyList())
    val focusSlots by repository.allFocusSlots.collectAsStateWithLifecycle(initialValue = emptyList())

    val streakDays = remember(streakStartTimestamp, incidents) { preferences.getStreakDays() }

    // Periodic Smart Focus Deep Work schedule evaluator
    LaunchedEffect(focusSlots, isSmartFocusEnabled, isProtectionEnabled) {
        while (true) {
            if (isProtectionEnabled && isSmartFocusEnabled && focusSlots.isNotEmpty()) {
                val activeMatch = SmartFocusEngine.checkAndFindActiveSlotToTrigger(
                    slots = focusSlots,
                    preferences = preferences
                )
                if (activeMatch != null) {
                    val (slot, remainingMinutes) = activeMatch
                    preferences.markFocusSlotTriggeredToday(slot.id)
                    coroutineScope.launch {
                        repository.recordIncident(
                            matchedWord = "Smart Focus: ${slot.title} (${slot.formattedTimeRange()})",
                            sourceApp = "Smart Focus Deep Work",
                            durationMinutes = remainingMinutes
                        )
                    }
                    onTriggerLockdown(
                        "Smart Focus: ${slot.title} (${slot.formattedTimeRange()})",
                        remainingMinutes
                    )
                }
            }
            delay(15_000L)
        }
    }

    // Keep the Always-On Panic Notification synchronized whenever key states change
    LaunchedEffect(isProtectionEnabled, lockDurationMinutes, streakDays, urgesDefeatedCount) {
        LockdownNotificationManager.showOrUpdateAlwaysOnPanicNotification(context)
    }

    BackHandler(enabled = currentTab != ScreenTab.DASHBOARD) {
        currentTab = ScreenTab.DASHBOARD
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ObsidianBg,
            topBar = {
                XBlockerCompactTopBar(
                    isDarkTheme = isDarkTheme,
                    isPersianLanguage = isPersianLanguage,
                    updateAvailableTag = if (githubUpdateStatus.isUpdateAvailable) githubUpdateStatus.latestVersionTag else null,
                    onToggleTheme = { preferences.setDarkTheme(!isDarkTheme) },
                    onToggleLanguage = { preferences.setPersianLanguage(!isPersianLanguage) },
                    onOpenInfoTab = { currentTab = ScreenTab.INFO }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = ObsidianSurface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == ScreenTab.DASHBOARD,
                        onClick = { currentTab = ScreenTab.DASHBOARD },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Shield",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tr("Shield", "محافظ"),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        },
                        colors = navigationBarColors(),
                        modifier = Modifier.testTag("nav_tab_dashboard")
                    )

                    NavigationBarItem(
                        selected = currentTab == ScreenTab.STREAK,
                        onClick = { currentTab = ScreenTab.STREAK },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Whatshot,
                                contentDescription = "Streak",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tr("Streak", "پاک‌زیستی"),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        },
                        colors = navigationBarColors(),
                        modifier = Modifier.testTag("nav_tab_streak")
                    )

                    NavigationBarItem(
                        selected = currentTab == ScreenTab.LIMITS,
                        onClick = { currentTab = ScreenTab.LIMITS },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.HourglassBottom,
                                contentDescription = "Limits",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tr("Limits", "محدودیت"),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        },
                        colors = navigationBarColors(),
                        modifier = Modifier.testTag("nav_tab_limits")
                    )

                    NavigationBarItem(
                        selected = currentTab == ScreenTab.KEYWORDS,
                        onClick = { currentTab = ScreenTab.KEYWORDS },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = "Rules",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tr("Rules", "قوانین"),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        },
                        colors = navigationBarColors(),
                        modifier = Modifier.testTag("nav_tab_keywords")
                    )

                    NavigationBarItem(
                        selected = currentTab == ScreenTab.INFO,
                        onClick = { currentTab = ScreenTab.INFO },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info & Backup",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tr("Info", "اطلاعات"),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        },
                        colors = navigationBarColors(),
                        modifier = Modifier.testTag("nav_tab_info")
                    )
                }
            }
        ) { innerPadding ->
            when (currentTab) {
                ScreenTab.DASHBOARD -> {
                    DashboardScreen(
                        isProtectionEnabled = isProtectionEnabled,
                        lockDurationMinutes = lockDurationMinutes,
                        strictModeEnabled = strictModeEnabled,
                        streakDays = streakDays,
                        incidentCount = incidentCount,
                        totalKeywordsCount = keywords.size,
                        incidents = incidents,
                        appLimits = appLimits,
                        sectionRules = sectionRules,
                        focusSlots = focusSlots,
                        isSmartFocusEnabled = isSmartFocusEnabled,
                        isBiometricLockEnabled = isBiometricLockEnabled,
                        isBlackoutPrivacyShieldEnabled = isBlackoutPrivacyShieldEnabled,
                        preferences = preferences,
                        usageRefreshTrigger = usageRefreshTrigger,
                        onToggleProtection = { preferences.setProtectionEnabled(it) },
                        onChangeDuration = { preferences.setLockDurationMinutes(it) },
                        onToggleStrictMode = { preferences.setStrictModeEnabled(it) },
                        onToggleSectionRule = { rule ->
                            coroutineScope.launch { repository.toggleSectionRule(rule) }
                        },
                        onAddSectionRule = { rule ->
                            coroutineScope.launch { repository.saveSectionRule(rule) }
                        },
                        onToggleSmartFocusMaster = { preferences.setSmartFocusEnabled(it) },
                        onToggleFocusSlot = { slot ->
                            coroutineScope.launch { repository.toggleFocusSlot(slot) }
                        },
                        onAddFocusSlot = { slot ->
                            coroutineScope.launch { repository.saveFocusSlot(slot) }
                        },
                        onStartFocusSessionNow = { title, durationMins ->
                            coroutineScope.launch {
                                repository.recordIncident(
                                    matchedWord = "Smart Focus: $title (${durationMins}m)",
                                    sourceApp = "Smart Focus Deep Work",
                                    durationMinutes = durationMins
                                )
                            }
                            onTriggerLockdown("Smart Focus: $title (${durationMins}m)", durationMins)
                        },
                        onToggleBiometricLock = { enabled ->
                            preferences.setBiometricLockEnabled(enabled)
                            if (!enabled) {
                                isBiometricSessionLocked = false
                            }
                        },
                        onLockAppNow = {
                            isBiometricSessionLocked = true
                        },
                        onToggleBlackoutShield = { enabled ->
                            preferences.setBlackoutPrivacyShieldEnabled(enabled)
                            if (enabled) {
                                preferences.clearTemporaryPersonalScreenshotWindow()
                            }
                            onSyncWindowSecureFlag()
                        },
                        onOpenTestSandbox = {},
                        onOpenPanicMode = { showPanicModeDialog = true },
                        onResetAppFromScratch = {},
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                ScreenTab.STREAK -> {
                    NoBeepStreakScreen(
                        streakStartTimestamp = streakStartTimestamp,
                        longestStreakDays = longestStreakDays,
                        slipsCount = slipsCount,
                        urgesDefeatedCount = urgesDefeatedCount,
                        onResetStreak = { reason ->
                            preferences.resetStreak(reason)
                        },
                        onOpenPanicMode = { showPanicModeDialog = true },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                ScreenTab.LIMITS -> {
                    AppLimitsAndSectionsScreen(
                        appLimits = appLimits,
                        sectionRules = sectionRules,
                        focusSlots = focusSlots,
                        preferences = preferences,
                        onToggleAppLimit = { limit ->
                            coroutineScope.launch { repository.toggleAppUsageLimit(limit) }
                        },
                        onDeleteAppLimit = { pkg ->
                            coroutineScope.launch { repository.deleteAppUsageLimit(pkg) }
                        },
                        onSaveAppLimit = { limit ->
                            coroutineScope.launch { repository.saveAppUsageLimit(limit) }
                        },
                        onToggleSectionRule = { rule ->
                            coroutineScope.launch { repository.toggleSectionRule(rule) }
                        },
                        onDeleteSectionRule = { id ->
                            coroutineScope.launch { repository.deleteSectionRule(id) }
                        },
                        onAddSectionRule = { rule ->
                            coroutineScope.launch { repository.saveSectionRule(rule) }
                        },
                        onToggleFocusSlot = { slot ->
                            coroutineScope.launch { repository.toggleFocusSlot(slot) }
                        },
                        onDeleteFocusSlot = { id ->
                            coroutineScope.launch { repository.deleteFocusSlot(id) }
                        },
                        onAddFocusSlot = { slot ->
                            coroutineScope.launch { repository.saveFocusSlot(slot) }
                        },
                        onStartFocusSlotNow = { slot ->
                            val durationMins = slot.getDurationMinutes()
                            coroutineScope.launch {
                                repository.recordIncident(
                                    matchedWord = "Smart Focus: ${slot.title} (${slot.formattedTimeRange()})",
                                    sourceApp = "Smart Focus Deep Work",
                                    durationMinutes = durationMins
                                )
                            }
                            onTriggerLockdown(
                                "Smart Focus: ${slot.title} (${slot.formattedTimeRange()})",
                                durationMins
                            )
                        },
                        onTriggerUsageLockdown = { appName, limitMins ->
                            onTriggerLockdown(
                                "Daily Limit Reached: $appName (${limitMins}m/day)",
                                lockDurationMinutes
                            )
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                ScreenTab.KEYWORDS -> {
                    KeywordManagerScreen(
                        keywords = keywords,
                        onToggleKeyword = { kw ->
                            coroutineScope.launch { repository.toggleKeyword(kw) }
                        },
                        onDeleteKeyword = { id ->
                            coroutineScope.launch { repository.deleteKeyword(id) }
                        },
                        onAddKeyword = { text, isDomain ->
                            coroutineScope.launch { repository.addCustomKeywordsBatch(text, isDomain) }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                ScreenTab.INFO -> {
                    InfoAndUpdateScreen(
                        githubRepoSlug = githubRepoSlug,
                        updateStatus = githubUpdateStatus,
                        onSaveGithubRepoSlug = { newSlug ->
                            preferences.setGithubRepoSlug(newSlug)
                        },
                        onCheckForUpdateNow = {
                            runBackgroundGithubUpdateCheck()
                        },
                        onExportBackupJson = {
                            repository.exportFullBackupJson()
                        },
                        onImportBackupJson = { rawJson ->
                            repository.importFullBackupJson(rawJson)
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }

            if (showPanicModeDialog) {
                PanicModeDialog(
                    streakDays = streakDays,
                    urgesDefeatedCount = urgesDefeatedCount,
                    personalPledge = personalPledge,
                    onSavePersonalPledge = { pledge ->
                        preferences.setPersonalPledge(pledge)
                    },
                    onUrgeDefeated = {
                        preferences.incrementUrgesDefeated()
                    },
                    onTriggerEmergencyLockdown = { emergencyMinutes ->
                        showPanicModeDialog = false
                        coroutineScope.launch {
                            repository.recordIncident(
                                matchedWord = "Emergency Panic Shield (${emergencyMinutes}m Voluntary Lock)",
                                sourceApp = "Emergency Panic SOS",
                                durationMinutes = emergencyMinutes
                            )
                        }
                        onTriggerLockdown(
                            "Emergency Panic Shield (${emergencyMinutes}m Voluntary Lock)",
                            emergencyMinutes
                        )
                    },
                    onDismiss = { showPanicModeDialog = false }
                )
            }
        }

        // Biometric Lodge Security Gate Overlay
        if (isBiometricSessionLocked) {
            BiometricLockOverlay(
                preferences = preferences,
                onUnlocked = { isBiometricSessionLocked = false }
            )
        }

        // Background Privacy Blackout Curtain (only active when user enables Anti-Agent Black Screen Shield)
        if (isAppInBackgroundCurtain && isBlackoutPrivacyShieldEnabled) {
            BlackoutPrivacyCurtain()
        }
    }
}

@Composable
private fun XBlockerCompactTopBar(
    isDarkTheme: Boolean,
    isPersianLanguage: Boolean,
    updateAvailableTag: String?,
    onToggleTheme: () -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenInfoTab: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ObsidianSurface)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CrimsonPrimary.copy(alpha = 0.16f))
                        .border(1.5.dp, CrimsonPrimary, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "X",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = CrimsonPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = EmeraldShield,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = tr("X Blocker", "ایکس بلاکر"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = ObsidianTextPrimary
                    )
                    Text(
                        text = "v${GithubUpdateChecker.CURRENT_APP_VERSION} • @mehdisrmd",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        color = ObsidianTextSecondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (updateAvailableTag != null) {
                    Surface(
                        onClick = onOpenInfoTab,
                        shape = RoundedCornerShape(8.dp),
                        color = AmberWarning.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, AmberWarning),
                        modifier = Modifier.testTag("top_update_available_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Update Available",
                                tint = AmberWarning,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = updateAvailableTag,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = AmberWarning
                            )
                        }
                    }
                }

                // Tiny Button 1: Persian RTL / English LTR Switch
                Surface(
                    onClick = onToggleLanguage,
                    shape = RoundedCornerShape(8.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, CyanTelemetry.copy(alpha = 0.55f)),
                    modifier = Modifier.testTag("tiny_language_toggle_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Switch Language FA/EN",
                            tint = CyanTelemetry,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPersianLanguage) "FA | فارسی" else "EN | FA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = ObsidianTextPrimary
                        )
                    }
                }

                // Tiny Button 2: Dark / White (Light) Theme Switch
                Surface(
                    onClick = onToggleTheme,
                    shape = RoundedCornerShape(8.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.testTag("tiny_theme_toggle_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Switch Dark/White Theme",
                            tint = if (isDarkTheme) AmberWarning else CrimsonPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isDarkTheme) tr("White", "سفید") else tr("Dark", "تاریک"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = ObsidianTextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun navigationBarColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = EmeraldShield,
    selectedTextColor = ObsidianTextPrimary,
    indicatorColor = EmeraldShield.copy(alpha = 0.18f),
    unselectedIconColor = ObsidianTextSecondary,
    unselectedTextColor = ObsidianTextSecondary
)
