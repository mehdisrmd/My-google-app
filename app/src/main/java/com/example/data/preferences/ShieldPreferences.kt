package com.example.data.preferences

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.example.service.ShieldAccessibilityService
import com.example.widget.StreakWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ShieldPreferences(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("shield_block_prefs", Context.MODE_PRIVATE)

    init {
        if (!prefs.getBoolean(KEY_FRESH_SCRATCH_V3_INIT, false)) {
            val now = System.currentTimeMillis()
            val defaultPledge =
                "I am reclaiming my focus, energy, and self-respect. A 5-minute urge is never worth trading my clean streak."
            prefs.edit()
                .clear()
                .putBoolean(KEY_FRESH_SCRATCH_V3_INIT, true)
                .putBoolean(KEY_PROTECTION_ENABLED, true)
                .putInt(KEY_LOCK_DURATION_MINUTES, 5)
                .putBoolean(KEY_STRICT_MODE_ENABLED, true)
                .putLong(KEY_ACTIVE_LOCK_END_TIME, 0L)
                .putLong(KEY_STREAK_START_TIME, now)
                .putInt(KEY_LONGEST_STREAK, 0)
                .putInt(KEY_SLIPS_COUNT, 0)
                .putString(KEY_LAST_SLIP_REASON, "")
                .putInt(KEY_URGES_DEFEATED_COUNT, 0)
                .putString(KEY_PERSONAL_PLEDGE, defaultPledge)
                .putBoolean(KEY_BIOMETRIC_LOCK_ENABLED, false)
                .putString(KEY_SECURITY_PIN_CODE, "0000")
                .putBoolean(KEY_BLACKOUT_PRIVACY_SHIELD, false)
                .putLong(KEY_SCREENSHOT_TEMP_UNLOCK_UNTIL, 0L)
                .putBoolean(KEY_SMART_FOCUS_ENABLED, true)
                .apply()
        }
    }

    private val _isProtectionEnabled = MutableStateFlow(getIsProtectionEnabled())
    val isProtectionEnabled: StateFlow<Boolean> = _isProtectionEnabled.asStateFlow()

    private val _lockDurationMinutes = MutableStateFlow(getLockDurationMinutes())
    val lockDurationMinutes: StateFlow<Int> = _lockDurationMinutes.asStateFlow()

    private val _strictModeEnabled = MutableStateFlow(getStrictModeEnabled())
    val strictModeEnabled: StateFlow<Boolean> = _strictModeEnabled.asStateFlow()

    private val _activeLockEndTime = MutableStateFlow(getActiveLockEndTime())
    val activeLockEndTime: StateFlow<Long> = _activeLockEndTime.asStateFlow()

    private val _streakStartTimestamp = MutableStateFlow(getStreakStartTimestamp())
    val streakStartTimestamp: StateFlow<Long> = _streakStartTimestamp.asStateFlow()

    private val _longestStreakDays = MutableStateFlow(getLongestStreakDays())
    val longestStreakDays: StateFlow<Int> = _longestStreakDays.asStateFlow()

    private val _slipsCount = MutableStateFlow(getSlipsCount())
    val slipsCount: StateFlow<Int> = _slipsCount.asStateFlow()

    private val _lastSlipReason = MutableStateFlow(getLastSlipReason())
    val lastSlipReason: StateFlow<String> = _lastSlipReason.asStateFlow()

    private val _urgesDefeatedCount = MutableStateFlow(getUrgesDefeatedCount())
    val urgesDefeatedCount: StateFlow<Int> = _urgesDefeatedCount.asStateFlow()

    private val _personalPledge = MutableStateFlow(getPersonalPledge())
    val personalPledge: StateFlow<String> = _personalPledge.asStateFlow()

    private val _usageRefreshTrigger = MutableStateFlow(0L)
    val usageRefreshTrigger: StateFlow<Long> = _usageRefreshTrigger.asStateFlow()

    // Biometric Lodge / Security Lock State
    private val _isBiometricLockEnabled = MutableStateFlow(getIsBiometricLockEnabled())
    val isBiometricLockEnabled: StateFlow<Boolean> = _isBiometricLockEnabled.asStateFlow()

    private val _securityPinCode = MutableStateFlow(getSecurityPinCode())
    val securityPinCode: StateFlow<String> = _securityPinCode.asStateFlow()

    // Anti-Agent Blackout Screen Shield (FLAG_SECURE) & Personal Screenshot Mode
    private val _isBlackoutPrivacyShieldEnabled = MutableStateFlow(getIsBlackoutPrivacyShieldEnabled())
    val isBlackoutPrivacyShieldEnabled: StateFlow<Boolean> = _isBlackoutPrivacyShieldEnabled.asStateFlow()

    private val _screenshotTemporaryUnlockUntil = MutableStateFlow(getScreenshotTemporaryUnlockUntil())
    val screenshotTemporaryUnlockUntil: StateFlow<Long> = _screenshotTemporaryUnlockUntil.asStateFlow()

    // Smart Focus Master Toggle
    private val _isSmartFocusEnabled = MutableStateFlow(getIsSmartFocusEnabled())
    val isSmartFocusEnabled: StateFlow<Boolean> = _isSmartFocusEnabled.asStateFlow()

    fun getIsProtectionEnabled(): Boolean {
        return prefs.getBoolean(KEY_PROTECTION_ENABLED, true)
    }

    fun setProtectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PROTECTION_ENABLED, enabled).apply()
        _isProtectionEnabled.value = enabled
    }

    fun getLockDurationMinutes(): Int {
        return prefs.getInt(KEY_LOCK_DURATION_MINUTES, 5)
    }

    fun setLockDurationMinutes(minutes: Int) {
        val safeMinutes = minutes.coerceIn(1, 1440)
        prefs.edit().putInt(KEY_LOCK_DURATION_MINUTES, safeMinutes).apply()
        _lockDurationMinutes.value = safeMinutes
    }

    fun getStrictModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_STRICT_MODE_ENABLED, true)
    }

    fun setStrictModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STRICT_MODE_ENABLED, enabled).apply()
        _strictModeEnabled.value = enabled
    }

    // Biometric Lodge Methods
    fun getIsBiometricLockEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_LOCK_ENABLED, false)
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_LOCK_ENABLED, enabled).apply()
        _isBiometricLockEnabled.value = enabled
    }

    fun getSecurityPinCode(): String {
        return prefs.getString(KEY_SECURITY_PIN_CODE, "0000") ?: "0000"
    }

    fun setSecurityPinCode(pin: String) {
        val clean = pin.filter { it.isDigit() }.take(6).ifEmpty { "0000" }
        prefs.edit().putString(KEY_SECURITY_PIN_CODE, clean).apply()
        _securityPinCode.value = clean
    }

    fun verifySecurityPin(inputPin: String): Boolean {
        return inputPin.trim() == getSecurityPinCode()
    }

    // Anti-Agent Blackout Shield (FLAG_SECURE) & Personal Screenshot Window
    // Off by default so the app is visible in the browser streaming emulator until enabled by user
    fun getIsBlackoutPrivacyShieldEnabled(): Boolean {
        return prefs.getBoolean(KEY_BLACKOUT_PRIVACY_SHIELD, false)
    }

    fun setBlackoutPrivacyShieldEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLACKOUT_PRIVACY_SHIELD, enabled).apply()
        _isBlackoutPrivacyShieldEnabled.value = enabled
    }

    fun getScreenshotTemporaryUnlockUntil(): Long {
        return prefs.getLong(KEY_SCREENSHOT_TEMP_UNLOCK_UNTIL, 0L)
    }

    fun enableTemporaryPersonalScreenshotWindow(durationSeconds: Int = 60): Long {
        val until = System.currentTimeMillis() + (durationSeconds * 1000L)
        prefs.edit().putLong(KEY_SCREENSHOT_TEMP_UNLOCK_UNTIL, until).apply()
        _screenshotTemporaryUnlockUntil.value = until
        return until
    }

    fun clearTemporaryPersonalScreenshotWindow() {
        prefs.edit().putLong(KEY_SCREENSHOT_TEMP_UNLOCK_UNTIL, 0L).apply()
        _screenshotTemporaryUnlockUntil.value = 0L
    }

    fun isFlagSecureCurrentlyActive(nowMs: Long = System.currentTimeMillis()): Boolean {
        if (!getIsBlackoutPrivacyShieldEnabled()) return false
        val tempUnlockUntil = getScreenshotTemporaryUnlockUntil()
        return nowMs >= tempUnlockUntil
    }

    // Smart Focus Methods
    fun getIsSmartFocusEnabled(): Boolean {
        return prefs.getBoolean(KEY_SMART_FOCUS_ENABLED, true)
    }

    fun setSmartFocusEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SMART_FOCUS_ENABLED, enabled).apply()
        _isSmartFocusEnabled.value = enabled
    }

    fun wasFocusSlotTriggeredToday(
        slotId: Long,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        val todayEpochDay = TimeUnit.MILLISECONDS.toDays(nowMs)
        val lastTriggeredDay = prefs.getLong("focus_slot_triggered_day_$slotId", -1L)
        return lastTriggeredDay == todayEpochDay
    }

    fun markFocusSlotTriggeredToday(
        slotId: Long,
        nowMs: Long = System.currentTimeMillis()
    ) {
        val todayEpochDay = TimeUnit.MILLISECONDS.toDays(nowMs)
        prefs.edit().putLong("focus_slot_triggered_day_$slotId", todayEpochDay).apply()
    }

    fun getActiveLockEndTime(): Long {
        return prefs.getLong(KEY_ACTIVE_LOCK_END_TIME, 0L)
    }

    fun getActiveLockDurationMinutes(): Int {
        return prefs.getInt(KEY_ACTIVE_LOCK_DURATION, 5)
    }

    fun getActiveLockTrigger(): String {
        return prefs.getString(KEY_ACTIVE_LOCK_TRIGGER, "Explicit Content") ?: "Explicit Content"
    }

    fun getActiveLockApp(): String {
        return prefs.getString(KEY_ACTIVE_LOCK_APP, "Browser") ?: "Browser"
    }

    fun startLockdown(
        durationMinutes: Int,
        triggerWord: String,
        sourceApp: String
    ): Long {
        val endTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(durationMinutes.toLong())
        prefs.edit()
            .putLong(KEY_ACTIVE_LOCK_END_TIME, endTime)
            .putInt(KEY_ACTIVE_LOCK_DURATION, durationMinutes)
            .putString(KEY_ACTIVE_LOCK_TRIGGER, triggerWord)
            .putString(KEY_ACTIVE_LOCK_APP, sourceApp)
            .putLong(KEY_LAST_INCIDENT_TIME, System.currentTimeMillis())
            .apply()
        _activeLockEndTime.value = endTime
        return endTime
    }

    fun clearLockdown() {
        prefs.edit()
            .putLong(KEY_ACTIVE_LOCK_END_TIME, 0L)
            .remove(KEY_ACTIVE_LOCK_TRIGGER)
            .remove(KEY_ACTIVE_LOCK_APP)
            .apply()
        _activeLockEndTime.value = 0L
    }

    fun isCurrentlyLocked(): Boolean {
        val endTime = getActiveLockEndTime()
        return endTime > System.currentTimeMillis()
    }

    fun getStreakStartTimestamp(): Long {
        val start = prefs.getLong(KEY_STREAK_START_TIME, 0L)
        if (start == 0L) {
            val now = System.currentTimeMillis()
            prefs.edit().putLong(KEY_STREAK_START_TIME, now).apply()
            return now
        }
        return start
    }

    fun getStreakDays(): Int {
        val start = getStreakStartTimestamp()
        val diff = (System.currentTimeMillis() - start).coerceAtLeast(0L)
        return (diff / TimeUnit.DAYS.toMillis(1)).toInt()
    }

    fun getLongestStreakDays(): Int {
        val currentDays = getStreakDays()
        val recordedBest = prefs.getInt(KEY_LONGEST_STREAK, 0)
        return maxOf(currentDays, recordedBest)
    }

    fun getSlipsCount(): Int {
        return prefs.getInt(KEY_SLIPS_COUNT, 0)
    }

    fun getLastSlipReason(): String {
        return prefs.getString(KEY_LAST_SLIP_REASON, "") ?: ""
    }

    fun getUrgesDefeatedCount(): Int {
        return prefs.getInt(KEY_URGES_DEFEATED_COUNT, 0)
    }

    fun incrementUrgesDefeated(): Int {
        val updated = getUrgesDefeatedCount() + 1
        val todayEpochDay = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())
        val dailyKey = "urges_day_$todayEpochDay"
        val dailyCurrent = prefs.getInt(dailyKey, 0)
        prefs.edit()
            .putInt(KEY_URGES_DEFEATED_COUNT, updated)
            .putInt(dailyKey, dailyCurrent + 1)
            .apply()
        _urgesDefeatedCount.value = updated
        _usageRefreshTrigger.value = System.currentTimeMillis()
        return updated
    }

    fun getDayUrgesDefeated(daysAgo: Int): Int {
        val targetDay = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis()) - daysAgo
        return prefs.getInt("urges_day_$targetDay", 0)
    }

    fun getPersonalPledge(): String {
        return prefs.getString(
            KEY_PERSONAL_PLEDGE,
            "I am reclaiming my focus, energy, and self-respect. A 5-minute urge is never worth trading my clean streak."
        ) ?: "I am reclaiming my focus, energy, and self-respect. A 5-minute urge is never worth trading my clean streak."
    }

    fun setPersonalPledge(pledge: String) {
        val cleanPledge = pledge.trim()
        if (cleanPledge.isNotEmpty()) {
            prefs.edit().putString(KEY_PERSONAL_PLEDGE, cleanPledge).apply()
            _personalPledge.value = cleanPledge
        }
    }

    fun resetStreak(reason: String = "") {
        val currentDays = getStreakDays()
        val best = prefs.getInt(KEY_LONGEST_STREAK, 0)
        val newBest = maxOf(currentDays, best)
        val slips = prefs.getInt(KEY_SLIPS_COUNT, 0) + 1
        val now = System.currentTimeMillis()
        prefs.edit()
            .putInt(KEY_LONGEST_STREAK, newBest)
            .putLong(KEY_STREAK_START_TIME, now)
            .putInt(KEY_SLIPS_COUNT, slips)
            .putString(KEY_LAST_SLIP_REASON, reason)
            .apply()

        _streakStartTimestamp.value = now
        _longestStreakDays.value = newBest
        _slipsCount.value = slips
        _lastSlipReason.value = reason

        StreakWidgetProvider.updateAllWidgets(context)
    }

    // Daily App Usage Tracking (accurate to the second + optional UsageStatsManager sync)
    private fun getTodaySecondsKey(packageName: String): String {
        val todayDay = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())
        return "usage_sec_${todayDay}_$packageName"
    }

    private fun getTodayMinutesLegacyKey(packageName: String): String {
        val todayDay = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())
        return "usage_${todayDay}_$packageName"
    }

    fun getDayAppUsageMinutes(daysAgo: Int, packageNames: List<String>): Int {
        if (packageNames.isEmpty()) return 0
        val targetEpochDay = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis()) - daysAgo
        var totalSeconds = 0
        for (pkg in packageNames.distinct()) {
            val equivalents = com.example.util.AppPackageResolver.getAllEquivalentPackages(pkg)
            var pkgBestSec = 0
            for (eqPkg in equivalents) {
                val sec = prefs.getInt("usage_sec_${targetEpochDay}_$eqPkg", 0)
                val legacyMin = prefs.getInt("usage_${targetEpochDay}_$eqPkg", 0)
                val localSec = maxOf(sec, legacyMin * 60)
                val sysSec = if (daysAgo == 0) getSystemUsageSecondsToday(eqPkg) else 0
                pkgBestSec = maxOf(pkgBestSec, localSec, sysSec)
            }
            totalSeconds += pkgBestSec
        }
        return totalSeconds / 60
    }

    fun getTodayAppUsageSeconds(packageName: String, appName: String = ""): Int {
        val equivalents = com.example.util.AppPackageResolver.getAllEquivalentPackages(packageName, appName)
        var maxLocalSeconds = 0
        var maxSystemSeconds = 0
        for (pkg in equivalents) {
            val trackedSeconds = prefs.getInt(getTodaySecondsKey(pkg), 0)
            val legacyMinutes = prefs.getInt(getTodayMinutesLegacyKey(pkg), 0)
            val localTotalSeconds = maxOf(trackedSeconds, legacyMinutes * 60)
            if (localTotalSeconds > maxLocalSeconds) {
                maxLocalSeconds = localTotalSeconds
            }
            val sysSec = getSystemUsageSecondsToday(pkg)
            if (sysSec > maxSystemSeconds) {
                maxSystemSeconds = sysSec
            }
        }
        return maxOf(maxLocalSeconds, maxSystemSeconds)
    }

    fun getTodayAppUsageMinutes(packageName: String, appName: String = ""): Int {
        return getTodayAppUsageSeconds(packageName, appName) / 60
    }

    fun addAppUsageSeconds(packageName: String, seconds: Int) {
        if (seconds <= 0) return
        val key = getTodaySecondsKey(packageName)
        val current = prefs.getInt(key, 0)
        val updated = current + seconds
        prefs.edit()
            .putInt(key, updated)
            .putInt(getTodayMinutesLegacyKey(packageName), updated / 60)
            .apply()
        _usageRefreshTrigger.value = System.currentTimeMillis()
    }

    fun addAppUsageMinutes(packageName: String, minutes: Int) {
        addAppUsageSeconds(packageName, minutes * 60)
    }

    fun resetTodayAppUsage(packageName: String, appName: String = "") {
        val equivalents = com.example.util.AppPackageResolver.getAllEquivalentPackages(packageName, appName)
        val editor = prefs.edit()
        for (pkg in equivalents) {
            editor.remove(getTodaySecondsKey(pkg))
            editor.remove(getTodayMinutesLegacyKey(pkg))
        }
        editor.apply()
        _usageRefreshTrigger.value = System.currentTimeMillis()
    }

    private fun getSystemUsageSecondsToday(packageName: String): Int {
        if (!hasUsageStatsPermission(context)) return 0
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return 0
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calendar.timeInMillis
            val now = System.currentTimeMillis()
            val stats = usm.queryAndAggregateUsageStats(startOfDay, now)
            val totalMillis = stats[packageName]?.totalTimeInForeground ?: 0L
            (totalMillis / 1000L).toInt()
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Queries UsageStatsManager for the most recently foregrounded package within the last 60 seconds
     * when Usage Access permission is granted.
     */
    fun queryRecentForegroundPackageFromSystem(): String? {
        if (!hasUsageStatsPermission(context)) return null
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
            val now = System.currentTimeMillis()
            val events = usm.queryEvents(now - 60_000L, now) ?: return null
            val event = android.app.usage.UsageEvents.Event()
            var latestForegroundPkg: String? = null
            var latestTimestamp = 0L
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED ||
                    @Suppress("DEPRECATION")
                    (event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND)
                ) {
                    if (event.timeStamp >= latestTimestamp && !event.packageName.isNullOrBlank()) {
                        latestTimestamp = event.timeStamp
                        latestForegroundPkg = event.packageName
                    }
                }
            }
            latestForegroundPkg
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Checks if the one-time Fresh Scratch v3 initialization has occurred yet.
     */
    fun consumeOneTimeFreshScratchResetFlag(): Boolean {
        val alreadyDone = prefs.getBoolean(KEY_FRESH_SCRATCH_V3_INIT, false)
        if (!alreadyDone) {
            prefs.edit().putBoolean(KEY_FRESH_SCRATCH_V3_INIT, true).apply()
            return true
        }
        return false
    }

    /**
     * Completely resets all preferences, usage timers, cooldowns, streaks, slips, urges,
     * active lockdowns, and security locks so the user can test the app from scratch.
     */
    fun resetAllPreferencesToFreshScratch() {
        val now = System.currentTimeMillis()
        val defaultPledge = "I am reclaiming my focus, energy, and self-respect. A 5-minute urge is never worth trading my clean streak."
        prefs.edit()
            .clear()
            .putBoolean(KEY_FRESH_SCRATCH_V3_INIT, true)
            .putBoolean(KEY_PROTECTION_ENABLED, true)
            .putInt(KEY_LOCK_DURATION_MINUTES, 5)
            .putBoolean(KEY_STRICT_MODE_ENABLED, true)
            .putLong(KEY_ACTIVE_LOCK_END_TIME, 0L)
            .putLong(KEY_STREAK_START_TIME, now)
            .putInt(KEY_LONGEST_STREAK, 0)
            .putInt(KEY_SLIPS_COUNT, 0)
            .putString(KEY_LAST_SLIP_REASON, "")
            .putInt(KEY_URGES_DEFEATED_COUNT, 0)
            .putString(KEY_PERSONAL_PLEDGE, defaultPledge)
            .putBoolean(KEY_BIOMETRIC_LOCK_ENABLED, false)
            .putString(KEY_SECURITY_PIN_CODE, "0000")
            .putBoolean(KEY_BLACKOUT_PRIVACY_SHIELD, false)
            .putLong(KEY_SCREENSHOT_TEMP_UNLOCK_UNTIL, 0L)
            .putBoolean(KEY_SMART_FOCUS_ENABLED, true)
            .apply()

        _isProtectionEnabled.value = true
        _lockDurationMinutes.value = 5
        _strictModeEnabled.value = true
        _activeLockEndTime.value = 0L
        _streakStartTimestamp.value = now
        _longestStreakDays.value = 0
        _slipsCount.value = 0
        _lastSlipReason.value = ""
        _urgesDefeatedCount.value = 0
        _personalPledge.value = defaultPledge
        _isBiometricLockEnabled.value = false
        _securityPinCode.value = "0000"
        _isBlackoutPrivacyShieldEnabled.value = false
        _screenshotTemporaryUnlockUntil.value = 0L
        _isSmartFocusEnabled.value = true
        _usageRefreshTrigger.value = now

        StreakWidgetProvider.updateAllWidgets(context)
    }

    companion object {
        private const val KEY_PROTECTION_ENABLED = "key_protection_enabled"
        private const val KEY_LOCK_DURATION_MINUTES = "key_lock_duration_minutes"
        private const val KEY_STRICT_MODE_ENABLED = "key_strict_mode_enabled"
        private const val KEY_ACTIVE_LOCK_END_TIME = "key_active_lock_end_time"
        private const val KEY_ACTIVE_LOCK_DURATION = "key_active_lock_duration"
        private const val KEY_ACTIVE_LOCK_TRIGGER = "key_active_lock_trigger"
        private const val KEY_ACTIVE_LOCK_APP = "key_active_lock_app"
        private const val KEY_LAST_INCIDENT_TIME = "key_last_incident_time"
        private const val KEY_INSTALL_TIME = "key_install_time"
        private const val KEY_STREAK_START_TIME = "key_streak_start_time"
        private const val KEY_LONGEST_STREAK = "key_longest_streak"
        private const val KEY_SLIPS_COUNT = "key_slips_count"
        private const val KEY_LAST_SLIP_REASON = "key_last_slip_reason"
        private const val KEY_URGES_DEFEATED_COUNT = "key_urges_defeated_count"
        private const val KEY_PERSONAL_PLEDGE = "key_personal_pledge"
        private const val KEY_BIOMETRIC_LOCK_ENABLED = "key_biometric_lock_enabled"
        private const val KEY_SECURITY_PIN_CODE = "key_security_pin_code"
        private const val KEY_BLACKOUT_PRIVACY_SHIELD = "key_blackout_privacy_shield_v2"
        private const val KEY_SCREENSHOT_TEMP_UNLOCK_UNTIL = "key_screenshot_temp_unlock_until"
        private const val KEY_SMART_FOCUS_ENABLED = "key_smart_focus_enabled"
        private const val KEY_FRESH_SCRATCH_V3_INIT = "key_fresh_scratch_v3_init"

        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
                ?: return false
            val enabledServices = am.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            )
            val expectedServiceName = ShieldAccessibilityService::class.java.name
            for (service in enabledServices) {
                if (service.resolveInfo.serviceInfo.name == expectedServiceName ||
                    service.id.contains("ShieldAccessibilityService")
                ) {
                    return true
                }
            }
            return false
        }

        fun hasOverlayPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }

        fun isIgnoringBatteryOptimizations(context: Context): Boolean {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
            return pm.isIgnoringBatteryOptimizations(context.packageName)
        }

        fun hasUsageStatsPermission(context: Context): Boolean {
            return try {
                val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    appOps.unsafeCheckOpNoThrow(
                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(),
                        context.packageName
                    )
                } else {
                    @Suppress("DEPRECATION")
                    appOps.checkOpNoThrow(
                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(),
                        context.packageName
                    )
                }
                mode == AppOpsManager.MODE_ALLOWED
            } catch (_: Exception) {
                false
            }
        }

        fun requestUsageStatsPermission(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                try {
                    val fallback = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(fallback)
                } catch (_: Exception) {
                }
            }
        }

        fun requestDisableBatteryOptimization(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                try {
                    val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(fallback)
                } catch (_: Exception) {
                }
            }
        }
    }
}
