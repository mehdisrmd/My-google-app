package com.example

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notification.LockdownNotificationManager
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.focus.SmartFocusEngine
import com.example.ui.history.IncidentHistoryScreen
import com.example.ui.keywords.KeywordManagerScreen
import com.example.ui.limits.AppLimitsAndSectionsScreen
import com.example.ui.lockdown.LockdownActivity
import com.example.ui.panic.PanicModeDialog
import com.example.ui.sandbox.TestSandboxDialog
import com.example.ui.security.BiometricLockOverlay
import com.example.ui.security.BlackoutPrivacyCurtain
import com.example.ui.streak.NoBeepStreakScreen
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ScreenTab {
    DASHBOARD,
    STREAK,
    LIMITS,
    KEYWORDS,
    HISTORY
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
            MyApplicationTheme(darkTheme = true) {
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
        if (intent.getBooleanExtra(EXTRA_QUICK_EMERGENCY_LOCK, false)) {
            intent.removeExtra(EXTRA_QUICK_EMERGENCY_LOCK)
            val app = runCatching { ShieldApplication.instance }.getOrNull()
            val lockMins = app?.preferences?.getLockDurationMinutes() ?: 15
            triggerLockdown("Notification Quick Panic Lock (${lockMins}m)", lockMins)
            return
        }
        if (intent.getBooleanExtra(EXTRA_OPEN_PANIC_MODE, false)) {
            intent.removeExtra(EXTRA_OPEN_PANIC_MODE)
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
            sourceApp = "ShieldBlock Enforcement"
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
    var showSandboxDialog by remember { mutableStateOf(false) }
    var showPanicModeDialog by remember { mutableStateOf(false) }

    val isProtectionEnabled by preferences.isProtectionEnabled.collectAsStateWithLifecycle()
    val lockDurationMinutes by preferences.lockDurationMinutes.collectAsStateWithLifecycle()
    val strictModeEnabled by preferences.strictModeEnabled.collectAsStateWithLifecycle()

    val isBiometricLockEnabled by preferences.isBiometricLockEnabled.collectAsStateWithLifecycle()
    val isBlackoutPrivacyShieldEnabled by preferences.isBlackoutPrivacyShieldEnabled.collectAsStateWithLifecycle()
    val screenshotTempUnlockUntil by preferences.screenshotTemporaryUnlockUntil.collectAsStateWithLifecycle()
    val isSmartFocusEnabled by preferences.isSmartFocusEnabled.collectAsStateWithLifecycle()

    var isBiometricSessionLocked by remember {
        mutableStateOf(preferences.getIsBiometricLockEnabled())
    }
    var isAppInBackgroundCurtain by remember { mutableStateOf(false) }

    // Synchronize window FLAG_SECURE whenever blackout shield or 60s screenshot timer changes
    LaunchedEffect(isBlackoutPrivacyShieldEnabled, screenshotTempUnlockUntil) {
        onSyncWindowSecureFlag()
        val now = System.currentTimeMillis()
        if (screenshotTempUnlockUntil > now) {
            delay(screenshotTempUnlockUntil - now + 150L)
            onSyncWindowSecureFlag()
        }
    }

    DisposableEffect(lifecycleOwner, isBiometricLockEnabled, isBlackoutPrivacyShieldEnabled) {
        if (!isBlackoutPrivacyShieldEnabled) {
            isAppInBackgroundCurtain = false
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
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

    // Custom back navigation: return to Dashboard if on another tab
    BackHandler(enabled = currentTab != ScreenTab.DASHBOARD) {
        currentTab = ScreenTab.DASHBOARD
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ObsidianBg,
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
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text("Shield", fontWeight = FontWeight.SemiBold) },
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
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text("Streak", fontWeight = FontWeight.SemiBold) },
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
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text("Limits", fontWeight = FontWeight.SemiBold) },
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
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text("Rules", fontWeight = FontWeight.SemiBold) },
                        colors = navigationBarColors(),
                        modifier = Modifier.testTag("nav_tab_keywords")
                    )

                    NavigationBarItem(
                        selected = currentTab == ScreenTab.HISTORY,
                        onClick = { currentTab = ScreenTab.HISTORY },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text("Log", fontWeight = FontWeight.SemiBold) },
                        colors = navigationBarColors(),
                        modifier = Modifier.testTag("nav_tab_history")
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
                        focusSlots = focusSlots,
                        isSmartFocusEnabled = isSmartFocusEnabled,
                        isBiometricLockEnabled = isBiometricLockEnabled,
                        isBlackoutPrivacyShieldEnabled = isBlackoutPrivacyShieldEnabled,
                        screenshotTempUnlockUntil = screenshotTempUnlockUntil,
                        preferences = preferences,
                        usageRefreshTrigger = usageRefreshTrigger,
                        onToggleProtection = { preferences.setProtectionEnabled(it) },
                        onChangeDuration = { preferences.setLockDurationMinutes(it) },
                        onToggleStrictMode = { preferences.setStrictModeEnabled(it) },
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
                        onAllowScreenshot60s = {
                            preferences.enableTemporaryPersonalScreenshotWindow(60)
                            onSyncWindowSecureFlag()
                        },
                        onOpenTestSandbox = { showSandboxDialog = true },
                        onOpenPanicMode = { showPanicModeDialog = true },
                        onResetAppFromScratch = {
                            coroutineScope.launch {
                                repository.resetEntireAppFromScratch()
                                isBiometricSessionLocked = false
                                onSyncWindowSecureFlag()
                            }
                        },
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

                ScreenTab.HISTORY -> {
                    IncidentHistoryScreen(
                        incidents = incidents,
                        streakDays = streakDays,
                        appLimits = appLimits,
                        preferences = preferences,
                        usageRefreshTrigger = usageRefreshTrigger,
                        onClearHistory = {
                            coroutineScope.launch { repository.clearHistory() }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }

            if (showSandboxDialog) {
                TestSandboxDialog(
                    keywords = keywords,
                    sectionRules = sectionRules,
                    onTriggerLockdown = { triggerWord, minutes ->
                        showSandboxDialog = false
                        onTriggerLockdown(triggerWord, minutes)
                    },
                    onDismiss = { showSandboxDialog = false }
                )
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
private fun navigationBarColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = EmeraldShield,
    selectedTextColor = Color.White,
    indicatorColor = EmeraldShield.copy(alpha = 0.18f),
    unselectedIconColor = ObsidianTextSecondary,
    unselectedTextColor = ObsidianTextSecondary
)
