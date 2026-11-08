package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.example.ShieldApplication
import com.example.data.model.AppUsageLimit
import com.example.data.model.BlockedKeyword
import com.example.data.model.FocusScheduleSlot
import com.example.data.model.SectionBlockRule
import com.example.data.preferences.ShieldPreferences
import com.example.data.repository.ShieldRepository
import com.example.notification.LockdownNotificationManager
import com.example.ui.lockdown.LockdownActivity
import com.example.util.AppPackageResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ShieldAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var repository: ShieldRepository
    private lateinit var preferences: ShieldPreferences

    @Volatile
    private var cachedKeywords: List<BlockedKeyword> = emptyList()

    @Volatile
    private var cachedSectionRules: List<SectionBlockRule> = emptyList()

    @Volatile
    private var cachedAppLimits: List<AppUsageLimit> = emptyList()

    @Volatile
    private var cachedFocusSlots: List<FocusScheduleSlot> = emptyList()

    private var lastScanTimestamp = 0L
    private var lastForegroundPackage: String = ""
    private var lastUsageTickTimestamp = 0L

    override fun onCreate() {
        super.onCreate()
        val app = application as ShieldApplication
        repository = app.repository
        preferences = app.preferences

        // Keep active rules cached in memory for zero-latency scanning
        serviceScope.launch {
            repository.allKeywords.collect { list ->
                cachedKeywords = list.filter { it.isEnabled }
            }
        }
        serviceScope.launch {
            repository.allSectionRules.collect { list ->
                cachedSectionRules = list.filter { it.isEnabled }
            }
        }
        serviceScope.launch {
            repository.allAppUsageLimits.collect { list ->
                cachedAppLimits = list.filter { it.isEnabled }
            }
        }
        serviceScope.launch {
            repository.allFocusSlots.collect { list ->
                cachedFocusSlots = list.filter { it.isEnabled }
            }
        }

        // Background ticker every 1 second to enforce:
        // 1. Active Lockdown Strict Screen Pinning (allows only X Blocker + temporary Call/SMS window)
        // 2. Real-time Per-App Daily Usage Limits
        // 3. Scheduled Smart Focus Auto-Activation
        // 4. Daily Clean Day Milestone & Coin Reward check
        serviceScope.launch {
            while (true) {
                delay(1000L)
                val now = System.currentTimeMillis()
                try {
                    LockdownNotificationManager.checkAndNotifyCleanDayMilestone(this@ShieldAccessibilityService)
                } catch (_: Exception) {
                }

                // 1. Check Smart Focus Schedules
                if (preferences.getIsSmartFocusEnabled() && !preferences.isCurrentlyLocked()) {
                    val activeMatch = com.example.ui.focus.SmartFocusEngine.checkAndFindActiveSlotToTrigger(
                        slots = cachedFocusSlots,
                        preferences = preferences
                    )
                    if (activeMatch != null) {
                        val (slot, durationMins) = activeMatch
                        preferences.markFocusSlotTriggeredToday(slot.id, now)
                        triggerLockdown(
                            triggerWord = "Smart Focus: ${slot.title}",
                            sourceApp = "Scheduled Focus",
                            customDurationMinutes = durationMins
                        )
                    }
                }

                val rootNode = try {
                    rootInActiveWindow
                } catch (_: Exception) {
                    null
                }
                val rootPkg = rootNode?.packageName?.toString().orEmpty()
                val sysPkg = preferences.queryRecentForegroundPackageFromSystem().orEmpty()
                val activePkg = when {
                    rootPkg.isNotBlank() && !isIgnoredSystemUiPackage(rootPkg) -> rootPkg
                    sysPkg.isNotBlank() && !isIgnoredSystemUiPackage(sysPkg) -> sysPkg
                    else -> lastForegroundPackage
                }

                if (activePkg.isNotBlank()) {
                    lastForegroundPackage = activePkg
                }

                // 2. Strict Lockdown Enforcement: If timer is counting down, ONLY allow X Blocker
                //    or whitelisted Phone Call / SMS apps launched from the Block Screen!
                if (preferences.isCurrentlyLocked()) {
                    if (activePkg.isNotBlank() &&
                        !preferences.isPackageAllowedDuringLockdown(activePkg)
                    ) {
                        launchLockdownOverlay(
                            triggerWord = preferences.getActiveLockTrigger(),
                            sourceApp = preferences.getActiveLockApp(),
                            endTimeEpoch = preferences.getActiveLockEndTime()
                        )
                    }
                    continue
                }

                if (!preferences.getIsProtectionEnabled()) continue

                // 3. Track Per-App Usage & Enforce Daily Limit
                if (activePkg.isNotBlank() &&
                    activePkg != packageName &&
                    !isSystemLauncherOrKeyboard(activePkg)
                ) {
                    val elapsedSec = if (lastUsageTickTimestamp > 0L) {
                        ((now - lastUsageTickTimestamp) / 1000L).toInt().coerceIn(1, 5)
                    } else {
                        1
                    }
                    lastUsageTickTimestamp = now
                    preferences.addAppUsageSeconds(activePkg, elapsedSec)

                    val matchedLimit = cachedAppLimits.firstOrNull { limit ->
                        limit.isEnabled && AppPackageResolver.matchesAppLimit(
                            limit = limit,
                            candidatePackage = activePkg,
                            context = this@ShieldAccessibilityService
                        )
                    }
                    if (matchedLimit != null) {
                        val usedMins = preferences.getTodayAppUsageMinutes(
                            packageName = matchedLimit.packageName,
                            appName = matchedLimit.appName
                        )
                        if (usedMins >= matchedLimit.dailyLimitMinutes) {
                            triggerLockdown(
                                triggerWord = "Daily Limit (${matchedLimit.dailyLimitMinutes}m): ${matchedLimit.appName}",
                                sourceApp = matchedLimit.appName
                            )
                        }
                    }
                } else {
                    lastUsageTickTimestamp = now
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkgName = event.packageName?.toString() ?: return
        if (!isIgnoredSystemUiPackage(pkgName)) {
            lastForegroundPackage = pkgName
        }

        // 1. If device is currently in active lockdown, block leaving X Blocker (except whitelisted Call/SMS)
        if (preferences.isCurrentlyLocked()) {
            if (!preferences.isPackageAllowedDuringLockdown(pkgName)) {
                launchLockdownOverlay(
                    triggerWord = preferences.getActiveLockTrigger(),
                    sourceApp = preferences.getActiveLockApp(),
                    endTimeEpoch = preferences.getActiveLockEndTime()
                )
            }
            return
        }

        if (pkgName == packageName) return

        val rootNode = try {
            rootInActiveWindow
        } catch (_: Exception) {
            null
        } ?: event.source

        // 2. Prevent Uninstall Guard (Item 7)
        if (preferences.getIsPreventUninstallEnabled()) {
            if (TextContentScanner.isAttemptingToUninstallOrDisableXBlocker(rootNode, pkgName)) {
                performGlobalAction(GLOBAL_ACTION_BACK)
                performGlobalAction(GLOBAL_ACTION_HOME)
                triggerLockdown(
                    triggerWord = "Prevent Uninstall Guard",
                    sourceApp = "System Settings"
                )
                return
            }
        }

        if (!preferences.getIsProtectionEnabled()) return
        if (isSystemLauncherOrKeyboard(pkgName)) return

        val now = System.currentTimeMillis()
        if (now - lastScanTimestamp < 220L) return
        lastScanTimestamp = now

        val activeRootPkg = rootNode?.packageName?.toString()?.takeIf { it.isNotBlank() } ?: pkgName

        // 3. Check Per-App Daily Time Limit
        val matchedLimit = cachedAppLimits.firstOrNull { limit ->
            limit.isEnabled && AppPackageResolver.matchesAppLimit(
                limit = limit,
                candidatePackage = activeRootPkg,
                context = this@ShieldAccessibilityService
            )
        }
        if (matchedLimit != null) {
            val usedMins = preferences.getTodayAppUsageMinutes(
                packageName = matchedLimit.packageName,
                appName = matchedLimit.appName
            )
            if (usedMins >= matchedLimit.dailyLimitMinutes) {
                triggerLockdown(
                    triggerWord = "Daily Limit (${matchedLimit.dailyLimitMinutes}m): ${matchedLimit.appName}",
                    sourceApp = matchedLimit.appName
                )
                return
            }
        }

        // 4. Check Specific App Section Blocker (Instagram Reels & Telegram Search Box)
        // Confirmed by User (Item 4): Show Full Block Page for both Telegram Search and Instagram Reels!
        val matchedSectionRule = TextContentScanner.findMatchingSectionRule(
            root = rootNode,
            currentPackage = activeRootPkg,
            rules = cachedSectionRules
        )
        if (matchedSectionRule != null) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            if (preferences.getIsSectionExitOnlyMode()) {
                val appLabel = resolveReadableAppName(activeRootPkg)
                serviceScope.launch {
                    repository.recordIncident(
                        matchedWord = "Section Blocked: ${matchedSectionRule.sectionName}",
                        sourceApp = appLabel,
                        durationMinutes = 0
                    )
                }
            } else {
                triggerLockdown(
                    triggerWord = "${matchedSectionRule.sectionName} (${resolveReadableAppName(activeRootPkg)})",
                    sourceApp = resolveReadableAppName(activeRootPkg)
                )
            }
            return
        }

        // 5. Check Explicit Keywords & Adult Domains
        val screenTexts = mutableListOf<String>()
        event.text.forEach { charSeq ->
            charSeq?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { screenTexts.add(it) }
        }
        event.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let {
            screenTexts.add(it)
        }
        screenTexts.addAll(TextContentScanner.extractScreenText(rootNode))

        val matchedWord = TextContentScanner.findMatchingKeyword(screenTexts, cachedKeywords)
        if (matchedWord != null) {
            triggerLockdown(
                triggerWord = matchedWord,
                sourceApp = resolveReadableAppName(activeRootPkg)
            )
        }
    }

    private fun triggerLockdown(
        triggerWord: String,
        sourceApp: String,
        customDurationMinutes: Int? = null
    ) {
        val durationMinutes = customDurationMinutes ?: preferences.getLockDurationMinutes()
        val endTime = preferences.startLockdown(
            durationMinutes = durationMinutes,
            triggerWord = triggerWord,
            sourceApp = sourceApp
        )

        serviceScope.launch {
            repository.recordIncident(
                matchedWord = triggerWord,
                sourceApp = sourceApp,
                durationMinutes = durationMinutes
            )
        }

        performGlobalAction(GLOBAL_ACTION_BACK)
        performGlobalAction(GLOBAL_ACTION_HOME)

        LockdownNotificationManager.startLockdownNotification(
            context = this,
            endTimeEpoch = endTime,
            triggerWord = triggerWord
        )

        launchLockdownOverlay(
            triggerWord = triggerWord,
            sourceApp = sourceApp,
            endTimeEpoch = endTime
        )
    }

    private fun launchLockdownOverlay(
        triggerWord: String,
        sourceApp: String,
        endTimeEpoch: Long
    ) {
        mainHandler.post {
            val lockIntent = Intent(this, LockdownActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
                putExtra(LockdownActivity.EXTRA_TRIGGER_WORD, triggerWord)
                putExtra(LockdownActivity.EXTRA_SOURCE_APP, sourceApp)
                putExtra(LockdownActivity.EXTRA_END_TIME_EPOCH, endTimeEpoch)
            }
            try {
                startActivity(lockIntent)
            } catch (_: Exception) {
            }
        }
    }

    private fun isIgnoredSystemUiPackage(pkgName: String): Boolean {
        val lower = pkgName.lowercase()
        return lower == "com.android.systemui" ||
            lower.contains("inputmethod") ||
            lower.contains("keyboard") ||
            lower == "android"
    }

    private fun isSystemLauncherOrKeyboard(pkgName: String): Boolean {
        val lower = pkgName.lowercase()
        return lower.contains("launcher") ||
            lower.contains("systemui") ||
            lower.contains("inputmethod") ||
            lower.contains("keyboard") ||
            lower.contains("nexuslauncher") ||
            lower.contains("pixel.launcher") ||
            lower.contains("trebuchet")
    }

    private fun resolveReadableAppName(pkgName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(pkgName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            pkgName.substringAfterLast('.')
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    override fun onInterrupt() {
        // Required override for AccessibilityService
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
