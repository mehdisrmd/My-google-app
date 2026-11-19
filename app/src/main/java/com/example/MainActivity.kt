package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
import com.example.ui.info.AppInfo
import com.example.ui.info.InfoAndUpdateScreen
import com.example.ui.keywords.BlocklistActivationBlockedScreen
import com.example.ui.keywords.BlocklistWaitScreen
import com.example.ui.keywords.KeywordManagerScreen
import com.example.ui.limits.AppLimitsAndSectionsScreen
import com.example.ui.lockdown.LockdownActivity
import com.example.ui.panic.PanicModeDialog
import com.example.ui.quiz.ScientificAddictionQuizDialog
import com.example.ui.report.ReportBugDialog
import com.example.ui.security.PinLockOverlay
import com.example.ui.security.BlackoutPrivacyCurtain
import com.example.ui.streak.NoBeepStreakScreen
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.GlassGlowBackground
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.CyanTelemetry
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.liquidGlass
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SeasonalTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianTextPrimary
import com.example.ui.theme.ObsidianTextSecondary
import com.example.util.ShakeDetector
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

    // Shake-to-report (#22): MainContent consumes this flag and opens the
    // report dialog. Set only by the hard-shake detector below.
    private var shakeReportRequest by mutableStateOf(false)

    private val shakeDetector = ShakeDetector()

    private val sensorManager: SensorManager? by lazy {
        getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }

    private val shakeSensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
            // Never fire while a lockdown is running (the LockdownActivity
            // normally covers us and pauses this activity anyway — this is
            // the belt-and-braces check for the moment of the shake).
            val app = runCatching { ShieldApplication.instance }.getOrNull() ?: return
            if (app.preferences.isCurrentlyLocked()) return
            val fired = shakeDetector.onSample(
                timestampMs = event.timestamp / 1_000_000L,
                x = event.values[0],
                y = event.values[1],
                z = event.values[2]
            )
            if (fired) {
                shakeReportRequest = true
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        syncWindowSecureFlag()

        // Notifications are minimal by design: create the streak channel and
        // make sure the single daily 9:00 AM streak alarm is scheduled.
        LockdownNotificationManager.createNotificationChannel(this)
        LockdownNotificationManager.scheduleDailyStreakAlarm(this)

        handleNotificationIntent(intent)

        setContent {
            val app = ShieldApplication.instance
            val isDarkTheme by app.preferences.isDarkTheme.collectAsStateWithLifecycle()
            val isPersianLanguage by app.preferences.isPersianLanguage.collectAsStateWithLifecycle()
            val seasonalThemeName by app.preferences.seasonalThemeName.collectAsStateWithLifecycle()

            MyApplicationTheme(
                darkTheme = isDarkTheme,
                isPersianRtl = isPersianLanguage,
                seasonalTheme = SeasonalTheme.fromName(seasonalThemeName)
            ) {
                MainContent(
                    openPanicModeExternal = openPanicModeRequest,
                    onConsumePanicModeExternal = { openPanicModeRequest = false },
                    shakeReportExternal = shakeReportRequest,
                    onConsumeShakeReportExternal = { shakeReportRequest = false },
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
        checkActiveLockdown()
        registerShakeListener()
    }

    override fun onPause() {
        // Shake-to-report is foreground-only: the accelerometer listener
        // lives exactly between onResume and onPause.
        sensorManager?.unregisterListener(shakeSensorListener)
        super.onPause()
    }

    private fun registerShakeListener() {
        val manager = sensorManager ?: return
        val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        shakeDetector.reset()
        manager.registerListener(
            shakeSensorListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME
        )
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
        val intent = Intent(this, LockdownActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(LockdownActivity.EXTRA_STRICT_MODE, app.preferences.getStrictModeEnabled())
            putExtra(LockdownActivity.EXTRA_TRIGGER_WORD, triggerWord)
            putExtra(LockdownActivity.EXTRA_SOURCE_APP, "X Blocker Enforcement")
            putExtra(LockdownActivity.EXTRA_END_TIME_EPOCH, endTime)
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
    shakeReportExternal: Boolean = false,
    onConsumeShakeReportExternal: () -> Unit = {},
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
    var showReportBugDialog by remember { mutableStateOf(false) }

    val hasCompletedQuiz by preferences.hasCompletedQuiz.collectAsStateWithLifecycle()
    var showAddictionQuizDialog by remember {
        mutableStateOf(!preferences.hasCompletedOnboardingQuiz())
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            LockdownNotificationManager.scheduleDailyStreakAlarm(context)
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !LockdownNotificationManager.hasNotificationPermission(context)
        ) {
            runCatching {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val isProtectionEnabled by preferences.isProtectionEnabled.collectAsStateWithLifecycle()
    val lockDurationMinutes by preferences.lockDurationMinutes.collectAsStateWithLifecycle()
    val strictModeEnabled by preferences.strictModeEnabled.collectAsStateWithLifecycle()

    val isBlackoutPrivacyShieldEnabled by preferences.isBlackoutPrivacyShieldEnabled.collectAsStateWithLifecycle()
    val screenshotTempUnlockUntil by preferences.screenshotTemporaryUnlockUntil.collectAsStateWithLifecycle()
    val isSmartFocusEnabled by preferences.isSmartFocusEnabled.collectAsStateWithLifecycle()

    val isDarkTheme by preferences.isDarkTheme.collectAsStateWithLifecycle()
    val isPersianLanguage by preferences.isPersianLanguage.collectAsStateWithLifecycle()

    // #7: the PIN gate is mandatory — every (re)created activity starts
    // locked, and the overlay itself routes to setup when no PIN exists.
    var isPinSessionLocked by remember {
        mutableStateOf(true)
    }

    // #30 (batch 2): the PIN is demanded on EVERY entry into the app UI.
    // (a) When a lockdown ends, preferences raise the relock flag (see
    // ShieldPreferences.clearLockdown) — consume it here so returning
    // from the block screen lands on the PIN gate, never inside.
    val pinRelockRequired by preferences.pinRelockRequired.collectAsStateWithLifecycle()
    LaunchedEffect(pinRelockRequired) {
        if (pinRelockRequired) {
            isPinSessionLocked = true
            preferences.setPinRelockRequired(false)
        }
    }

    // (b) Leaving the app and coming back re-locks too: ON_STOP arms the
    // relock, ON_RESUME applies it (only once a PIN exists — the first
    // setup flow must not fight itself).
    var relockOnNextResume by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    relockOnNextResume = true
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (relockOnNextResume) {
                        relockOnNextResume = false
                        if (preferences.isPinSet()) {
                            isPinSessionLocked = true
                        }
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // #13 first-launch gate: while no adult domain list exists on disk,
    // a wait screen covers the app and the first download runs. A failed
    // download is not a lockout — the wait screen offers Retry, or
    // continuing with the built-in starter list (see BlocklistWaitScreen).
    var blocklistGatePassed by remember {
        mutableStateOf(app.adultBlocklistStore.hasList())
    }
    var blocklistDownloadFailed by remember { mutableStateOf(false) }

    // #29 (batch 2): the first-launch download was skipped (or replaced
    // by the starter seed) — the list counts as not updated and the app
    // stays not-fully-activated until a real download succeeds.
    val blocklistUpdatePending by preferences.blocklistUpdatePending.collectAsStateWithLifecycle()

    fun startBlocklistDownload() {
        coroutineScope.launch {
            blocklistDownloadFailed = false
            val result = app.adultBlocklistUpdater.updateNow()
            if (result.success || app.adultBlocklistStore.hasList()) {
                blocklistGatePassed = true
            } else {
                blocklistDownloadFailed = true
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!blocklistGatePassed) {
            startBlocklistDownload()
        }
    }

    // #12/#13 adult blocking state for the Rules tab card.
    val isAdultBlockEnabled by preferences.isAdultBlockEnabled.collectAsStateWithLifecycle()
    val blocklistLastUpdatedMs by preferences.adultBlocklistLastUpdatedMs.collectAsStateWithLifecycle()
    val blocklistDomainCount by preferences.adultBlocklistDomainCount.collectAsStateWithLifecycle()
    val userBlocklistDomains by preferences.userBlocklistDomains.collectAsStateWithLifecycle()
    var isBlocklistUpdating by remember { mutableStateOf(false) }
    var isAppInBackgroundCurtain by remember { mutableStateOf(false) }

    LaunchedEffect(isBlackoutPrivacyShieldEnabled, screenshotTempUnlockUntil) {
        onSyncWindowSecureFlag()
    }

    DisposableEffect(lifecycleOwner, isBlackoutPrivacyShieldEnabled) {
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

    // Shake-to-report (#22): a hard shake (detected in MainActivity while
    // resumed) opens the report box — unless the PIN gate or the
    // first-launch blocklist gate is covering the app, in which case the
    // request is simply dropped.
    LaunchedEffect(shakeReportExternal) {
        if (shakeReportExternal) {
            if (!isPinSessionLocked && blocklistGatePassed) {
                showReportBugDialog = true
            }
            onConsumeShakeReportExternal()
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

    BackHandler(enabled = currentTab != ScreenTab.DASHBOARD) {
        currentTab = ScreenTab.DASHBOARD
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Liquid Glass: ambient purple glows live behind the whole UI;
        // translucent surfaces above them read as real glass.
        GlassGlowBackground()
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                XBlockerCompactTopBar(
                    isDarkTheme = isDarkTheme,
                    isPersianLanguage = isPersianLanguage,
                    onToggleTheme = { preferences.setDarkTheme(!isDarkTheme) },
                    onToggleLanguage = { preferences.setPersianLanguage(!isPersianLanguage) }
                )
            },
            bottomBar = {
                // Minimal floating tab bar (#23): a slim Liquid Glass pill
                // detached from the screen edges, replacing the stock
                // Material3 NavigationBar. Only the selected destination
                // shows its label, inside a soft accent pill; the other
                // four are quiet icons. Same five destinations, same
                // navigation logic — visuals/UX only.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .shadow(
                                elevation = 14.dp,
                                shape = RoundedCornerShape(36.dp),
                                clip = false
                            )
                            .liquidGlass(
                                shape = RoundedCornerShape(36.dp),
                                fillAlpha = 0.72f
                            )
                            .height(62.dp)
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        XBlockerNavItem(
                            tab = ScreenTab.DASHBOARD,
                            currentTab = currentTab,
                            icon = Icons.Default.Shield,
                            label = tr("Shield", "محافظ"),
                            testTag = "nav_tab_dashboard",
                            onSelect = { currentTab = it }
                        )
                        XBlockerNavItem(
                            tab = ScreenTab.STREAK,
                            currentTab = currentTab,
                            icon = Icons.Default.Whatshot,
                            label = tr("Streak", "پاک‌زیستی"),
                            testTag = "nav_tab_streak",
                            onSelect = { currentTab = it }
                        )
                        XBlockerNavItem(
                            tab = ScreenTab.LIMITS,
                            currentTab = currentTab,
                            icon = Icons.Default.HourglassBottom,
                            label = tr("Limits", "محدودیت"),
                            testTag = "nav_tab_limits",
                            onSelect = { currentTab = it }
                        )
                        XBlockerNavItem(
                            tab = ScreenTab.KEYWORDS,
                            currentTab = currentTab,
                            icon = Icons.Default.Block,
                            label = tr("Rules", "قوانین"),
                            testTag = "nav_tab_keywords",
                            onSelect = { currentTab = it }
                        )
                        XBlockerNavItem(
                            tab = ScreenTab.INFO,
                            currentTab = currentTab,
                            icon = Icons.Default.Info,
                            label = tr("Info", "اطلاعات"),
                            testTag = "nav_tab_info",
                            onSelect = { currentTab = it }
                        )
                    }
                }
            }
        ) { innerPadding ->
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally(animationSpec = tween(240)) { width -> width / 4 } +
                            fadeIn(animationSpec = tween(220)) +
                            scaleIn(initialScale = 0.96f, animationSpec = tween(220)))
                            .togetherWith(
                                slideOutHorizontally(animationSpec = tween(200)) { width -> -width / 5 } +
                                    fadeOut(animationSpec = tween(180))
                            )
                    } else {
                        (slideInHorizontally(animationSpec = tween(240)) { width -> -width / 4 } +
                            fadeIn(animationSpec = tween(220)) +
                            scaleIn(initialScale = 0.96f, animationSpec = tween(220)))
                            .togetherWith(
                                slideOutHorizontally(animationSpec = tween(200)) { width -> width / 5 } +
                                    fadeOut(animationSpec = tween(180))
                            )
                    }
                },
                label = "main_tab_animated_transition"
            ) { selectedTab ->
                when (selectedTab) {
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
                        onLockAppNow = {
                            isPinSessionLocked = true
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
                        onOpenAddictionQuiz = { showAddictionQuizDialog = true },
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
                        onOpenAddictionQuiz = { showAddictionQuizDialog = true },
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
                        adultBlockEnabled = isAdultBlockEnabled,
                        onToggleAdultBlock = { enabled ->
                            preferences.setAdultBlockEnabled(enabled)
                        },
                        blocklistDomainCount = blocklistDomainCount,
                        blocklistLastUpdatedMs = blocklistLastUpdatedMs,
                        isBlocklistUpdating = isBlocklistUpdating,
                        onUpdateBlocklistNow = {
                            if (!isBlocklistUpdating) {
                                isBlocklistUpdating = true
                                coroutineScope.launch {
                                    try {
                                        app.adultBlocklistUpdater.updateNow()
                                    } finally {
                                        isBlocklistUpdating = false
                                    }
                                }
                            }
                        },
                        userBlocklistDomains = userBlocklistDomains.toList(),
                        onAddUserBlocklistDomain = { domain ->
                            val previous = preferences.getUserBlocklistDomains()
                            val updated = previous + domain
                            preferences.setUserBlocklistDomains(updated)
                            app.adultBlocklistStore.rebuildWithUserDomains(updated, previous)
                            preferences.setAdultBlocklistDomainCount(
                                app.adultBlocklistStore.readDomains().size
                            )
                        },
                        onRemoveUserBlocklistDomain = { domain ->
                            val previous = preferences.getUserBlocklistDomains()
                            val updated = previous - domain
                            preferences.setUserBlocklistDomains(updated)
                            app.adultBlocklistStore.rebuildWithUserDomains(updated, previous)
                            preferences.setAdultBlocklistDomainCount(
                                app.adultBlocklistStore.readDomains().size
                            )
                        },
                        incidents = incidents,
                        incidentStreakDays = streakDays,
                        incidentAppLimits = appLimits,
                        incidentPreferences = preferences,
                        incidentUsageRefreshTrigger = usageRefreshTrigger,
                        onDeleteIncidents = { toDelete ->
                            coroutineScope.launch { repository.deleteIncidents(toDelete) }
                        },
                        onClearIncidentHistory = {
                            coroutineScope.launch { repository.clearHistory() }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                ScreenTab.INFO -> {
                    InfoAndUpdateScreen(
                        onExportBackupJson = {
                            repository.exportFullBackupJson()
                        },
                        onImportBackupJson = { rawJson ->
                            repository.importFullBackupJson(rawJson)
                        },
                        preferences = preferences,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
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

            // Shake-to-report box (#22)
            if (showReportBugDialog) {
                ReportBugDialog(
                    onDismiss = { showReportBugDialog = false }
                )
            }

            // NoBeep-Style Scientific Addiction Assessment Quiz (Item 16: First launch + Retake)
            if (showAddictionQuizDialog) {
                ScientificAddictionQuizDialog(
                    isFirstLaunchOnboarding = !hasCompletedQuiz,
                    onQuizCompleted = { scorePercent, severityEn, severityFa ->
                        preferences.saveAddictionQuizResult(
                            scorePercent = scorePercent,
                            severityEn = severityEn,
                            severityFa = severityFa
                        )
                    },
                    onDismiss = { showAddictionQuizDialog = false }
                )
            }
        }

        // PIN Security Gate Overlay (#7: PIN-only, mandatory)
        if (isPinSessionLocked) {
            PinLockOverlay(
                preferences = preferences,
                onUnlocked = { isPinSessionLocked = false }
            )
        }

        // First-launch blocklist gate (#13): drawn last so it covers
        // everything — including the PIN gate — until the first domain
        // list exists. The download keeps running behind the PIN gate on
        // later launches; this only appears when there is no list at all.
        if (!blocklistGatePassed) {
            BlocklistWaitScreen(
                isFailed = blocklistDownloadFailed,
                onRetry = { startBlocklistDownload() },
                onContinueWithSeed = {
                    coroutineScope.launch {
                        app.adultBlocklistUpdater.writeFallbackSeed()
                        // #29: the starter seed is not a real update —
                        // the app stays not-fully-activated.
                        preferences.setBlocklistUpdatePending(true)
                        blocklistGatePassed = true
                    }
                },
                onSkip = {
                    // #29: skipping defers activation until a real
                    // download succeeds (the updater clears the flag).
                    preferences.setBlocklistUpdatePending(true)
                    blocklistGatePassed = true
                }
            )
        }

        // #29 (batch 2): after a skip, the main UI stays covered by the
        // activation screen (PIN first — this sits under the wait screen
        // but over the tabs) until a successful list update lands. Its
        // button routes straight back into the update flow.
        if (blocklistUpdatePending && !isPinSessionLocked && blocklistGatePassed) {
            BlocklistActivationBlockedScreen(
                onUpdateNow = {
                    blocklistGatePassed = false
                    startBlocklistDownload()
                }
            )
        }

        // Background Privacy Blackout Curtain
        if (isAppInBackgroundCurtain && isBlackoutPrivacyShieldEnabled) {
            BlackoutPrivacyCurtain()
        }
    }
}

@Composable
private fun XBlockerCompactTopBar(
    isDarkTheme: Boolean,
    isPersianLanguage: Boolean,
    onToggleTheme: () -> Unit,
    onToggleLanguage: () -> Unit
) {
    val themeIconRotation by animateFloatAsState(
        targetValue = if (isDarkTheme) 0f else 180f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "theme_icon_rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ObsidianSurface.copy(alpha = 0.82f))
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
                Image(
                    painter = painterResource(id = R.drawable.ic_stop_sign_x_blocker_1794123976741),
                    contentDescription = "X Blocker Minimal Icon",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, EmeraldShield, RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = tr("X Blocker", "ایکس بلاکر"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = ObsidianTextPrimary
                    )
                    Text(
                        text = "v${AppInfo.CURRENT_APP_VERSION} • @mehdisrmd",
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

                // Tiny Button 2: Dark / White Theme Switch (Icon ONLY — no "Dark"/"White" text!)
                Surface(
                    onClick = onToggleTheme,
                    shape = RoundedCornerShape(8.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.testTag("tiny_theme_toggle_button")
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Switch Dark/White Theme",
                            tint = if (isDarkTheme) AmberWarning else CrimsonPrimary,
                            modifier = Modifier
                                .size(15.dp)
                                .rotate(themeIconRotation)
                        )
                    }
                }
            }
        }
    }
}

/**
 * One destination of the minimal floating bottom bar (#23).
 *
 * Unselected items are icon-only; the selected item expands — animated —
 * into a soft accent pill (seasonal shield accent at low alpha, the same
 * indicator color the old Material3 bar used) showing icon + label. The
 * item's touch target stays 48dp tall even though the visible pill is
 * smaller, and the label doubles as the icon's content description so
 * screen readers announce every destination, selected or not.
 */
@Composable
private fun RowScope.XBlockerNavItem(
    tab: ScreenTab,
    currentTab: ScreenTab,
    icon: ImageVector,
    label: String,
    testTag: String,
    onSelect: (ScreenTab) -> Unit
) {
    val selected = currentTab == tab
    val interactionSource = remember { MutableInteractionSource() }
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "nav_icon_scale_$testTag"
    )
    val pillColor by animateColorAsState(
        targetValue = if (selected) {
            EmeraldShield.copy(alpha = 0.22f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(250),
        label = "nav_pill_color_$testTag"
    )
    Box(
        modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onSelect(tab) }
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
                .clip(RoundedCornerShape(24.dp))
                .background(pillColor)
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) EmeraldShield else ObsidianTextSecondary,
                modifier = Modifier
                    .size(22.dp)
                    .scale(iconScale)
            )
            if (selected) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    color = ObsidianTextPrimary
                )
            }
        }
    }
}
