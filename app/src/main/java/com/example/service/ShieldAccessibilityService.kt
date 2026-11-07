package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.accessibility.AccessibilityEvent
import com.example.ShieldApplication
import com.example.data.model.AppUsageLimit
import com.example.data.model.BlockedKeyword
import com.example.data.model.FocusScheduleSlot
import com.example.data.model.SectionBlockRule
import com.example.notification.LockdownNotificationManager
import com.example.ui.focus.SmartFocusEngine
import com.example.ui.lockdown.LockdownActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ShieldAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var cachedKeywords: List<BlockedKeyword> = emptyList()
    private var cachedSectionRules: List<SectionBlockRule> = emptyList()
    private var cachedAppLimits: List<AppUsageLimit> = emptyList()
    private var cachedFocusSlots: List<FocusScheduleSlot> = emptyList()

    private var lastScanTime = 0L
    private val scanThrottleMs = 150L
    private var lastSectionExitTime = 0L
    private val sectionExitCooldownMs = 1200L

    // Foreground tracking for daily app usage limit
    private var currentForegroundPkg: String? = null
    private var foregroundStartTime: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        LockdownNotificationManager.showOrUpdateAlwaysOnPanicNotification(this)
        observeRules()
        startSmartFocusTicker()
    }

    private fun observeRules() {
        val repo = runCatching { ShieldApplication.instance.repository }.getOrNull() ?: return

        serviceScope.launch {
            try {
                repo.allKeywords.collectLatest { list ->
                    cachedKeywords = list.filter { it.isEnabled }
                }
            } catch (_: Exception) {}
        }

        serviceScope.launch {
            try {
                repo.allSectionRules.collectLatest { list ->
                    cachedSectionRules = list.filter { it.isEnabled }
                }
            } catch (_: Exception) {}
        }

        serviceScope.launch {
            try {
                repo.allAppUsageLimits.collectLatest { list ->
                    cachedAppLimits = list.filter { it.isEnabled }
                }
            } catch (_: Exception) {}
        }

        serviceScope.launch {
            try {
                repo.allFocusSlots.collectLatest { list ->
                    cachedFocusSlots = list.filter { it.isEnabled }
                }
            } catch (_: Exception) {}
        }
    }

    private fun startSmartFocusTicker() {
        serviceScope.launch {
            while (true) {
                checkSmartFocusSchedule()
                checkActiveForegroundAppUsageAndLimits()
                delay(3_000L)
            }
        }
    }

    private fun checkActiveForegroundAppUsageAndLimits() {
        val app = runCatching { ShieldApplication.instance }.getOrNull() ?: return
        val preferences = app.preferences
        if (!preferences.getIsProtectionEnabled() || preferences.isCurrentlyLocked()) return

        val activeWindowPkg = runCatching { rootInActiveWindow?.packageName?.toString() }.getOrNull()
        val systemStatsPkg = preferences.queryRecentForegroundPackageFromSystem()
        val candidatePkg = when {
            !activeWindowPkg.isNullOrBlank() &&
                !com.example.util.AppPackageResolver.isTransientSystemPackage(activeWindowPkg, applicationContext.packageName) -> activeWindowPkg
            !systemStatsPkg.isNullOrBlank() &&
                !com.example.util.AppPackageResolver.isTransientSystemPackage(systemStatsPkg, applicationContext.packageName) -> systemStatsPkg
            else -> currentForegroundPkg
        } ?: return

        handleAppUsageTracking(candidatePkg, AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, preferences)
    }

    private fun checkSmartFocusSchedule() {
        val app = runCatching { ShieldApplication.instance }.getOrNull() ?: return
        val preferences = app.preferences
        if (!preferences.getIsProtectionEnabled()) return

        val match = SmartFocusEngine.checkAndFindActiveSlotToTrigger(
            slots = cachedFocusSlots,
            preferences = preferences
        )
        if (match != null) {
            val (slot, durationMins) = match
            preferences.markFocusSlotTriggeredToday(slot.id)
            triggerCustomLockdown(
                reason = "Smart Focus: ${slot.title} (${slot.formattedTimeRange()})",
                sourcePackage = "Smart Focus Deep Work",
                durationMinutes = durationMins
            )
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val app = runCatching { ShieldApplication.instance }.getOrNull() ?: return
        val preferences = app.preferences

        // If currently in active lockdown, ensure lockdown screen stays on top if user tries switching apps
        if (preferences.isCurrentlyLocked()) {
            val packageName = event.packageName?.toString() ?: ""
            if (packageName.isNotEmpty() &&
                !com.example.util.AppPackageResolver.isTransientSystemPackage(packageName, applicationContext.packageName)
            ) {
                bringLockdownToFront()
            }
            return
        }

        if (!preferences.getIsProtectionEnabled()) {
            return
        }

        // Check Smart Focus Deep Work schedule
        checkSmartFocusSchedule()

        val sourcePackage = event.packageName?.toString() ?: return
        if (sourcePackage == applicationContext.packageName) {
            return
        }

        // 1. App Usage Tracking & Restriction Check
        if (!com.example.util.AppPackageResolver.isTransientSystemPackage(sourcePackage, applicationContext.packageName)) {
            handleAppUsageTracking(sourcePackage, event.eventType, preferences)
        }

        val now = System.currentTimeMillis()
        if (now - lastScanTime < scanThrottleMs) {
            return
        }
        lastScanTime = now

        val rootNode = rootInActiveWindow ?: return

        serviceScope.launch {
            // 2. In-App Section Blocker Check (Instagram Reels, Telegram Search Box, YouTube Shorts)
            val sectionRules = cachedSectionRules
            if (sectionRules.isNotEmpty()) {
                val matchedSection = TextContentScanner.scanForSectionRule(
                    root = rootNode,
                    rules = sectionRules,
                    currentPackage = sourcePackage
                )
                if (matchedSection != null) {
                    val nowSection = System.currentTimeMillis()
                    if (nowSection - lastSectionExitTime >= sectionExitCooldownMs) {
                        lastSectionExitTime = nowSection
                        if (preferences.getIsSectionExitOnlyMode()) {
                            // Stop ONLY that specific section (e.g., exit Instagram Reels or close Telegram Search box)
                            // without blocking or closing the entire Instagram / Telegram app!
                            performGlobalAction(GLOBAL_ACTION_BACK)
                            triggerVibration()
                            serviceScope.launch(Dispatchers.IO) {
                                app.repository.recordIncident(
                                    matchedWord = "Section Stopped: ${matchedSection.sectionName}",
                                    sourceApp = sourcePackage,
                                    durationMinutes = 0
                                )
                            }
                        } else {
                            // Full Lockdown Mode if user disabled Section-Exit-Only mode
                            performGlobalAction(GLOBAL_ACTION_BACK)
                            performGlobalAction(GLOBAL_ACTION_HOME)
                            onBlockedWordDetected(
                                matchedWord = "Section: ${matchedSection.sectionName}",
                                sourcePackage = sourcePackage
                            )
                        }
                    }
                    return@launch
                }
            }

            // 3. On-Screen Explicit Word & URL Scanner Check (English & Persian)
            val keywords = cachedKeywords
            if (keywords.isNotEmpty()) {
                val match = TextContentScanner.scanNodeTree(
                    root = rootNode,
                    activeKeywords = keywords,
                    ignorePackage = applicationContext.packageName
                )
                if (match != null) {
                    onBlockedWordDetected(
                        matchedWord = match.matchedKeyword,
                        sourcePackage = sourcePackage
                    )
                }
            }
        }
    }

    private fun handleAppUsageTracking(
        sourcePackage: String,
        eventType: Int,
        preferences: com.example.data.preferences.ShieldPreferences
    ) {
        val now = System.currentTimeMillis()
        val prevPkg = currentForegroundPkg

        if (prevPkg != null && prevPkg != sourcePackage && foregroundStartTime > 0L) {
            val elapsedSeconds = ((now - foregroundStartTime) / 1000L).toInt().coerceIn(0, 3600)
            if (elapsedSeconds > 0) {
                preferences.addAppUsageSeconds(prevPkg, elapsedSeconds)
            }
            currentForegroundPkg = sourcePackage
            foregroundStartTime = now
        } else if (prevPkg == sourcePackage && foregroundStartTime > 0L) {
            val elapsedSeconds = ((now - foregroundStartTime) / 1000L).toInt()
            if (elapsedSeconds >= 3) {
                preferences.addAppUsageSeconds(sourcePackage, elapsedSeconds)
                foregroundStartTime = now
            }
        } else if (prevPkg == null) {
            currentForegroundPkg = sourcePackage
            foregroundStartTime = now
        }

        // Check if sourcePackage (or any of its known package variants/aliases) exceeds daily limit
        val limit = cachedAppLimits.find {
            it.isEnabled && com.example.util.AppPackageResolver.matchesAppLimit(it, sourcePackage, this)
        }
        if (limit != null) {
            val usedMinutes = maxOf(
                preferences.getTodayAppUsageMinutes(sourcePackage, limit.appName),
                preferences.getTodayAppUsageMinutes(limit.packageName, limit.appName)
            )
            if (usedMinutes >= limit.dailyLimitMinutes) {
                performGlobalAction(GLOBAL_ACTION_HOME)
                val hours = limit.dailyLimitMinutes / 60
                val mins = limit.dailyLimitMinutes % 60
                val limitStr = when {
                    hours > 0 && mins == 0 -> "${hours}h"
                    hours > 0 -> "${hours}h ${mins}m"
                    else -> "${mins}m"
                }
                onBlockedWordDetected(
                    matchedWord = "Daily limit exceeded ($limitStr) on ${limit.appName}",
                    sourcePackage = limit.appName
                )
            }
        }
    }

    private fun onBlockedWordDetected(matchedWord: String, sourcePackage: String) {
        val app = ShieldApplication.instance
        val durationMinutes = app.preferences.getLockDurationMinutes()
        triggerCustomLockdown(
            reason = matchedWord,
            sourcePackage = sourcePackage,
            durationMinutes = durationMinutes
        )
    }

    private fun triggerCustomLockdown(
        reason: String,
        sourcePackage: String,
        durationMinutes: Int
    ) {
        val app = ShieldApplication.instance
        val preferences = app.preferences

        // 1. Kick user immediately out of the app to home screen
        performGlobalAction(GLOBAL_ACTION_HOME)

        // 2. Trigger warning vibration
        triggerVibration()

        // 3. Start lockdown state
        val endTime = preferences.startLockdown(
            durationMinutes = durationMinutes,
            triggerWord = reason,
            sourceApp = sourcePackage
        )

        // 4. Start ongoing foreground notification
        LockdownNotificationManager.startLockdownNotification(
            context = this,
            endTimeEpoch = endTime,
            triggerWord = reason
        )

        // 5. Log incident to database
        serviceScope.launch(Dispatchers.IO) {
            app.repository.recordIncident(
                matchedWord = reason,
                sourceApp = sourcePackage,
                durationMinutes = durationMinutes
            )
        }

        // 6. Launch LockdownActivity
        bringLockdownToFront()
    }

    private fun bringLockdownToFront() {
        val intent = Intent(this, LockdownActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra(LockdownActivity.EXTRA_STRICT_MODE, true)
        }
        startActivity(intent)
    }

    private fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 300), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 300), -1)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(400)
                }
            }
        } catch (_: Exception) {}
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        val now = System.currentTimeMillis()
        val pkg = currentForegroundPkg
        if (pkg != null && foregroundStartTime > 0L) {
            val elapsedSeconds = ((now - foregroundStartTime) / 1000L).toInt().coerceIn(0, 3600)
            if (elapsedSeconds > 0) {
                runCatching {
                    ShieldApplication.instance.preferences.addAppUsageSeconds(pkg, elapsedSeconds)
                }
            }
        }
    }
}
