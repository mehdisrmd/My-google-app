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
import android.provider.Telephony
import android.telecom.TelecomManager
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
        val now = System.currentTimeMillis()
        val todayEpochDay = TimeUnit.MILLISECONDS.toDays(now)

        if (!prefs.getBoolean(KEY_FRESH_SCRATCH_V4_INIT, false)) {
            val defaultPledge =
                "I am reclaiming my focus, energy, and self-respect. A 5-minute urge is never worth trading my clean streak."
            val preservedDark = prefs.getBoolean(KEY_IS_DARK_THEME, true)
            val preservedPersian = prefs.getBoolean(KEY_IS_PERSIAN_LANGUAGE, false)
            prefs.edit()
                .clear()
                .putBoolean(KEY_FRESH_SCRATCH_V4_INIT, true)
                .putBoolean(KEY_V04_DEFAULTS_MIGRATED, true)
                .putBoolean(KEY_V04_GITHUB_REPO_MIGRATED, true)
                .putBoolean(KEY_V04_ALL_OFF_DEFAULT_MIGRATED, true)
                .putBoolean(KEY_PROTECTION_ENABLED, false)
                .putInt(KEY_LOCK_DURATION_MINUTES, 5)
                .putBoolean(KEY_STRICT_MODE_ENABLED, false)
                .putBoolean(KEY_PREVENT_UNINSTALL_ENABLED, false)
                .putLong(KEY_ACTIVE_LOCK_END_TIME, 0L)
                .putLong(KEY_STREAK_START_TIME, now)
                .putLong(KEY_FIRST_INSTALL_EPOCH_DAY, todayEpochDay)
                .putInt(KEY_LONGEST_STREAK, 0)
                .putInt(KEY_COINS_BALANCE, 0)
                .putInt(KEY_LAST_REWARDED_STREAK_DAY, 0)
                .putInt(KEY_LAST_NOTIFIED_CLEAN_DAY, 0)
                .putInt(KEY_SLIPS_COUNT, 0)
                .putString(KEY_LAST_SLIP_REASON, "")
                .putInt(KEY_URGES_DEFEATED_COUNT, 0)
                .putString(KEY_PERSONAL_PLEDGE, defaultPledge)
                .putBoolean(KEY_BIOMETRIC_LOCK_ENABLED, false)
                .putString(KEY_SECURITY_PIN_CODE, "0000")
                .putBoolean(KEY_BLACKOUT_PRIVACY_SHIELD, false)
                .putLong(KEY_SCREENSHOT_TEMP_UNLOCK_UNTIL, 0L)
                .putBoolean(KEY_SMART_FOCUS_ENABLED, false)
                .putBoolean(KEY_IS_DARK_THEME, preservedDark)
                .putBoolean(KEY_IS_PERSIAN_LANGUAGE, preservedPersian)
                .putString(KEY_GITHUB_REPO_SLUG, DEFAULT_GITHUB_REPO)
                .putBoolean(KEY_SECTION_EXIT_ONLY_MODE, false)
                .apply()
        } else {
            val editor = prefs.edit()
            var needsApply = false
            if (!prefs.getBoolean(KEY_V04_DEFAULTS_MIGRATED, false)) {
                editor.putBoolean(KEY_V04_DEFAULTS_MIGRATED, true)
                editor.putBoolean(KEY_SECTION_EXIT_ONLY_MODE, false)
                if (!prefs.contains(KEY_FIRST_INSTALL_EPOCH_DAY)) {
                    editor.putLong(KEY_FIRST_INSTALL_EPOCH_DAY, todayEpochDay)
                }
                needsApply = true
            }
            if (!prefs.getBoolean(KEY_V04_GITHUB_REPO_MIGRATED, false)) {
                editor.putBoolean(KEY_V04_GITHUB_REPO_MIGRATED, true)
                editor.putString(KEY_GITHUB_REPO_SLUG, DEFAULT_GITHUB_REPO)
                needsApply = true
            }
            if (!prefs.getBoolean(KEY_V04_ALL_OFF_DEFAULT_MIGRATED, false)) {
                editor.putBoolean(KEY_V04_ALL_OFF_DEFAULT_MIGRATED, true)
                editor.putBoolean(KEY_PROTECTION_ENABLED, false)
                editor.putBoolean(KEY_STRICT_MODE_ENABLED, false)
                editor.putBoolean(KEY_PREVENT_UNINSTALL_ENABLED, false)
                editor.putBoolean(KEY_SMART_FOCUS_ENABLED, false)
                editor.putBoolean(KEY_BIOMETRIC_LOCK_ENABLED, false)
                editor.putBoolean(KEY_BLACKOUT_PRIVACY_SHIELD, false)
                needsApply = true
            }
            if (needsApply) {
                editor.apply()
            }
        }
    }

    private val _isProtectionEnabled = MutableStateFlow(getIsProtectionEnabled())
    val isProtectionEnabled: StateFlow<Boolean> = _isProtectionEnabled.asStateFlow()

    private val _lockDurationMinutes = MutableStateFlow(getLockDurationMinutes())
    val lockDurationMinutes: StateFlow<Int> = _lockDurationMinutes.asStateFlow()

    private val _strictModeEnabled = MutableStateFlow(getStrictModeEnabled())
    val strictModeEnabled: StateFlow<Boolean> = _strictModeEnabled.asStateFlow()

    private val _isPreventUninstallEnabled = MutableStateFlow(getIsPreventUninstallEnabled())
    val isPreventUninstallEnabled: StateFlow<Boolean> = _isPreventUninstallEnabled.asStateFlow()

    private val _coinsBalance = MutableStateFlow(getCoinsBalance())
    val coinsBalance: StateFlow<Int> = _coinsBalance.asStateFlow()

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

    // Anti-Agent Blackout Screen Shield (FLAG_SECURE)
    private val _isBlackoutPrivacyShieldEnabled = MutableStateFlow(getIsBlackoutPrivacyShieldEnabled())
    val isBlackoutPrivacyShieldEnabled: StateFlow<Boolean> = _isBlackoutPrivacyShieldEnabled.asStateFlow()

    private val _screenshotTemporaryUnlockUntil = MutableStateFlow(getScreenshotTemporaryUnlockUntil())
    val screenshotTemporaryUnlockUntil: StateFlow<Long> = _screenshotTemporaryUnlockUntil.asStateFlow()

    // Smart Focus Master Toggle
    private val _isSmartFocusEnabled = MutableStateFlow(getIsSmartFocusEnabled())
    val isSmartFocusEnabled: StateFlow<Boolean> = _isSmartFocusEnabled.asStateFlow()

    // Theme (Dark vs White/Light) & Language (Persian RTL vs English LTR)
    private val _isDarkTheme = MutableStateFlow(getIsDarkTheme())
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _isPersianLanguage = MutableStateFlow(getIsPersianLanguage())
    val isPersianLanguage: StateFlow<Boolean> = _isPersianLanguage.asStateFlow()

    // GitHub Repo Slug for Auto-Update Checker
    private val _githubRepoSlug = MutableStateFlow(getGithubRepoSlug())
    val githubRepoSlug: StateFlow<String> = _githubRepoSlug.asStateFlow()

    // Section Blocker Mode: false = Show Full Block Page (Default for v0.4), true = Exit Only
    private val _isSectionExitOnlyMode = MutableStateFlow(getIsSectionExitOnlyMode())
    val isSectionExitOnlyMode: StateFlow<Boolean> = _isSectionExitOnlyMode.asStateFlow()

    // Scientific Addiction Quiz State
    private val _hasCompletedQuiz = MutableStateFlow(hasCompletedOnboardingQuiz())
    val hasCompletedQuiz: StateFlow<Boolean> = _hasCompletedQuiz.asStateFlow()

    private val _quizScorePercent = MutableStateFlow(getAddictionQuizScore())
    val quizScorePercent: StateFlow<Int> = _quizScorePercent.asStateFlow()

    private val _quizSeverityEn = MutableStateFlow(getAddictionQuizSeverityEn())
    val quizSeverityEn: StateFlow<String> = _quizSeverityEn.asStateFlow()

    private val _quizSeverityFa = MutableStateFlow(getAddictionQuizSeverityFa())
    val quizSeverityFa: StateFlow<String> = _quizSeverityFa.asStateFlow()

    fun getIsDarkTheme(): Boolean {
        return prefs.getBoolean(KEY_IS_DARK_THEME, true)
    }

    fun setDarkTheme(dark: Boolean) {
        prefs.edit().putBoolean(KEY_IS_DARK_THEME, dark).apply()
        _isDarkTheme.value = dark
    }

    fun getIsPersianLanguage(): Boolean {
        return prefs.getBoolean(KEY_IS_PERSIAN_LANGUAGE, false)
    }

    fun setPersianLanguage(persian: Boolean) {
        prefs.edit().putBoolean(KEY_IS_PERSIAN_LANGUAGE, persian).apply()
        _isPersianLanguage.value = persian
    }

    fun getGithubRepoSlug(): String {
        val raw = prefs.getString(KEY_GITHUB_REPO_SLUG, DEFAULT_GITHUB_REPO) ?: DEFAULT_GITHUB_REPO
        return normalizeGithubSlug(raw)
    }

    fun getFullGithubRepoUrl(): String {
        return "https://github.com/${getGithubRepoSlug()}"
    }

    fun setGithubRepoSlug(slugOrUrl: String) {
        val cleaned = normalizeGithubSlug(slugOrUrl)
        prefs.edit().putString(KEY_GITHUB_REPO_SLUG, cleaned).apply()
        _githubRepoSlug.value = cleaned
    }

    private fun normalizeGithubSlug(input: String): String {
        val trimmed = input.trim()
            .removePrefix("https://www.github.com/")
            .removePrefix("http://www.github.com/")
            .removePrefix("https://github.com/")
            .removePrefix("http://github.com/")
            .removePrefix("github.com/")
            .removeSuffix("/")
            .removeSuffix(".git")
        val segments = trimmed.split("/").filter { it.isNotBlank() }
        return if (segments.size >= 2) {
            "${segments[0]}/${segments[1]}"
        } else {
            trimmed.ifEmpty { DEFAULT_GITHUB_REPO }
        }
    }

    fun exportPreferencesToJson(): org.json.JSONObject {
        return org.json.JSONObject().apply {
            put("isProtectionEnabled", getIsProtectionEnabled())
            put("lockDurationMinutes", getLockDurationMinutes())
            put("strictModeEnabled", getStrictModeEnabled())
            put("isPreventUninstallEnabled", getIsPreventUninstallEnabled())
            put("coinsBalance", getCoinsBalance())
            put("firstInstallEpochDay", getFirstInstallEpochDay())
            put("streakStartTimestamp", getStreakStartTimestamp())
            put("longestStreakDays", getLongestStreakDays())
            put("slipsCount", getSlipsCount())
            put("lastSlipReason", getLastSlipReason())
            put("urgesDefeatedCount", getUrgesDefeatedCount())
            put("personalPledge", getPersonalPledge())
            put("isBiometricLockEnabled", getIsBiometricLockEnabled())
            put("securityPinCode", getSecurityPinCode())
            put("isBlackoutPrivacyShieldEnabled", getIsBlackoutPrivacyShieldEnabled())
            put("isSmartFocusEnabled", getIsSmartFocusEnabled())
            put("isDarkTheme", getIsDarkTheme())
            put("isPersianLanguage", getIsPersianLanguage())
            put("githubRepoSlug", getGithubRepoSlug())
            put("isSectionExitOnlyMode", getIsSectionExitOnlyMode())
            put("hasCompletedQuiz", hasCompletedOnboardingQuiz())
            put("quizScorePercent", getAddictionQuizScore())
            put("quizSeverityEn", getAddictionQuizSeverityEn())
            put("quizSeverityFa", getAddictionQuizSeverityFa())
        }
    }

    fun importPreferencesFromJson(json: org.json.JSONObject) {
        val now = System.currentTimeMillis()
        val prot = json.optBoolean("isProtectionEnabled", getIsProtectionEnabled())
        val dur = json.optInt("lockDurationMinutes", getLockDurationMinutes()).coerceIn(1, 1440)
        val strict = json.optBoolean("strictModeEnabled", getStrictModeEnabled())
        val preventUninst = json.optBoolean("isPreventUninstallEnabled", getIsPreventUninstallEnabled())
        val coins = json.optInt("coinsBalance", getCoinsBalance()).coerceAtLeast(0)
        val firstInstallDay = json.optLong("firstInstallEpochDay", getFirstInstallEpochDay())
        val streakStart = json.optLong("streakStartTimestamp", getStreakStartTimestamp()).takeIf { it > 0L } ?: now
        val longest = json.optInt("longestStreakDays", getLongestStreakDays()).coerceAtLeast(0)
        val slips = json.optInt("slipsCount", getSlipsCount()).coerceAtLeast(0)
        val slipReason = json.optString("lastSlipReason", getLastSlipReason())
        val urges = json.optInt("urgesDefeatedCount", getUrgesDefeatedCount()).coerceAtLeast(0)
        val pledge = json.optString("personalPledge", getPersonalPledge())
        val bio = json.optBoolean("isBiometricLockEnabled", getIsBiometricLockEnabled())
        val pin = json.optString("securityPinCode", getSecurityPinCode()).filter { it.isDigit() }.take(6).ifEmpty { "0000" }
        val blackout = json.optBoolean("isBlackoutPrivacyShieldEnabled", getIsBlackoutPrivacyShieldEnabled())
        val focus = json.optBoolean("isSmartFocusEnabled", getIsSmartFocusEnabled())
        val dark = json.optBoolean("isDarkTheme", getIsDarkTheme())
        val persian = json.optBoolean("isPersianLanguage", getIsPersianLanguage())
        val repo = normalizeGithubSlug(json.optString("githubRepoSlug", getGithubRepoSlug()))
        val exitOnly = json.optBoolean("isSectionExitOnlyMode", getIsSectionExitOnlyMode())
        val quizDone = json.optBoolean("hasCompletedQuiz", hasCompletedOnboardingQuiz())
        val quizScore = json.optInt("quizScorePercent", getAddictionQuizScore())
        val quizSevEn = json.optString("quizSeverityEn", getAddictionQuizSeverityEn())
        val quizSevFa = json.optString("quizSeverityFa", getAddictionQuizSeverityFa())

        prefs.edit()
            .putBoolean(KEY_PROTECTION_ENABLED, prot)
            .putInt(KEY_LOCK_DURATION_MINUTES, dur)
            .putBoolean(KEY_STRICT_MODE_ENABLED, strict)
            .putBoolean(KEY_PREVENT_UNINSTALL_ENABLED, preventUninst)
            .putInt(KEY_COINS_BALANCE, coins)
            .putLong(KEY_FIRST_INSTALL_EPOCH_DAY, firstInstallDay)
            .putLong(KEY_STREAK_START_TIME, streakStart)
            .putInt(KEY_LONGEST_STREAK, longest)
            .putInt(KEY_SLIPS_COUNT, slips)
            .putString(KEY_LAST_SLIP_REASON, slipReason)
            .putInt(KEY_URGES_DEFEATED_COUNT, urges)
            .putString(KEY_PERSONAL_PLEDGE, pledge)
            .putBoolean(KEY_BIOMETRIC_LOCK_ENABLED, bio)
            .putString(KEY_SECURITY_PIN_CODE, pin)
            .putBoolean(KEY_BLACKOUT_PRIVACY_SHIELD, blackout)
            .putBoolean(KEY_SMART_FOCUS_ENABLED, focus)
            .putBoolean(KEY_IS_DARK_THEME, dark)
            .putBoolean(KEY_IS_PERSIAN_LANGUAGE, persian)
            .putString(KEY_GITHUB_REPO_SLUG, repo)
            .putBoolean(KEY_SECTION_EXIT_ONLY_MODE, exitOnly)
            .putBoolean(KEY_HAS_COMPLETED_QUIZ, quizDone)
            .putInt(KEY_QUIZ_SCORE_PERCENT, quizScore)
            .putString(KEY_QUIZ_SEVERITY_EN, quizSevEn)
            .putString(KEY_QUIZ_SEVERITY_FA, quizSevFa)
            .apply()

        _isProtectionEnabled.value = prot
        _lockDurationMinutes.value = dur
        _strictModeEnabled.value = strict
        _isPreventUninstallEnabled.value = preventUninst
        _coinsBalance.value = coins
        _streakStartTimestamp.value = streakStart
        _longestStreakDays.value = maxOf(longest, getStreakDays())
        _slipsCount.value = slips
        _lastSlipReason.value = slipReason
        _urgesDefeatedCount.value = urges
        _personalPledge.value = pledge
        _isBiometricLockEnabled.value = bio
        _securityPinCode.value = pin
        _isBlackoutPrivacyShieldEnabled.value = blackout
        _isSmartFocusEnabled.value = focus
        _isDarkTheme.value = dark
        _isPersianLanguage.value = persian
        _githubRepoSlug.value = repo
        _isSectionExitOnlyMode.value = exitOnly
        _hasCompletedQuiz.value = quizDone
        _quizScorePercent.value = quizScore
        _quizSeverityEn.value = quizSevEn
        _quizSeverityFa.value = quizSevFa
        _usageRefreshTrigger.value = now

        StreakWidgetProvider.updateAllWidgets(context)
    }

    fun getIsSectionExitOnlyMode(): Boolean {
        return prefs.getBoolean(KEY_SECTION_EXIT_ONLY_MODE, false)
    }

    fun setSectionExitOnlyMode(exitOnly: Boolean) {
        prefs.edit().putBoolean(KEY_SECTION_EXIT_ONLY_MODE, exitOnly).apply()
        _isSectionExitOnlyMode.value = exitOnly
    }

    fun getIsProtectionEnabled(): Boolean {
        return prefs.getBoolean(KEY_PROTECTION_ENABLED, false)
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
        return prefs.getBoolean(KEY_STRICT_MODE_ENABLED, false)
    }

    fun setStrictModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STRICT_MODE_ENABLED, enabled).apply()
        _strictModeEnabled.value = enabled
    }

    // Prevent Uninstall Guard
    fun getIsPreventUninstallEnabled(): Boolean {
        return prefs.getBoolean(KEY_PREVENT_UNINSTALL_ENABLED, false)
    }

    fun setPreventUninstallEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PREVENT_UNINSTALL_ENABLED, enabled).apply()
        _isPreventUninstallEnabled.value = enabled
    }

    // Coin Reward System (+1 Coin for each Clean Day)
    fun getCoinsBalance(): Int {
        return prefs.getInt(KEY_COINS_BALANCE, 0).coerceAtLeast(0)
    }

    fun addCoins(amount: Int): Int {
        if (amount <= 0) return getCoinsBalance()
        val updated = getCoinsBalance() + amount
        prefs.edit().putInt(KEY_COINS_BALANCE, updated).apply()
        _coinsBalance.value = updated
        return updated
    }

    fun spendCoins(cost: Int = 20): Boolean {
        if (cost <= 0) return true
        val current = getCoinsBalance()
        if (current < cost) return false
        val updated = current - cost
        prefs.edit().putInt(KEY_COINS_BALANCE, updated).apply()
        _coinsBalance.value = updated
        return true
    }

    /**
     * Synchronizes daily clean streak coins (+1 coin per clean streak day reached)
     * and returns the new clean day milestone if a new day (Day 1, Day 2, Day 3...) was just reached
     * and hasn't been notified yet, or 0 otherwise.
     */
    fun syncCleanDaysCoinsAndCheckMilestone(): Int {
        val currentStreakDays = getStreakDays()
        // Always ensure Best Chain is updated & remembered
        val recordedBest = prefs.getInt(KEY_LONGEST_STREAK, 0)
        if (currentStreakDays > recordedBest) {
            prefs.edit().putInt(KEY_LONGEST_STREAK, currentStreakDays).apply()
            _longestStreakDays.value = currentStreakDays
        }

        val lastRewardedDay = prefs.getInt(KEY_LAST_REWARDED_STREAK_DAY, 0)
        if (currentStreakDays > lastRewardedDay) {
            val newlyEarnedCoins = (currentStreakDays - lastRewardedDay).coerceAtLeast(1)
            val newBalance = getCoinsBalance() + newlyEarnedCoins
            prefs.edit()
                .putInt(KEY_COINS_BALANCE, newBalance)
                .putInt(KEY_LAST_REWARDED_STREAK_DAY, currentStreakDays)
                .apply()
            _coinsBalance.value = newBalance
        }

        val lastNotifiedDay = prefs.getInt(KEY_LAST_NOTIFIED_CLEAN_DAY, 0)
        if (currentStreakDays >= 1 && currentStreakDays > lastNotifiedDay) {
            prefs.edit().putInt(KEY_LAST_NOTIFIED_CLEAN_DAY, currentStreakDays).apply()
            return currentStreakDays
        }
        return 0
    }

    // Install Day & Duolingo-Style Daily Clean Status Tracking
    fun getFirstInstallEpochDay(): Long {
        val today = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())
        val saved = prefs.getLong(KEY_FIRST_INSTALL_EPOCH_DAY, -1L)
        if (saved <= 0L) {
            prefs.edit().putLong(KEY_FIRST_INSTALL_EPOCH_DAY, today).apply()
            return today
        }
        return saved
    }

    fun isDayBeforeInstall(daysAgo: Int, nowMs: Long = System.currentTimeMillis()): Boolean {
        val todayEpochDay = TimeUnit.MILLISECONDS.toDays(nowMs)
        val targetEpochDay = todayEpochDay - daysAgo
        return targetEpochDay < getFirstInstallEpochDay()
    }

    fun didRelapseOnDay(daysAgo: Int, nowMs: Long = System.currentTimeMillis()): Boolean {
        val todayEpochDay = TimeUnit.MILLISECONDS.toDays(nowMs)
        val targetEpochDay = todayEpochDay - daysAgo
        return prefs.getBoolean("relapse_day_$targetEpochDay", false)
    }

    // Scientific Addiction Assessment Quiz Methods
    fun hasCompletedOnboardingQuiz(): Boolean {
        return prefs.getBoolean(KEY_HAS_COMPLETED_QUIZ, false)
    }

    fun getAddictionQuizScore(): Int {
        return prefs.getInt(KEY_QUIZ_SCORE_PERCENT, -1)
    }

    fun getAddictionQuizSeverityEn(): String {
        return prefs.getString(KEY_QUIZ_SEVERITY_EN, "Not Assessed") ?: "Not Assessed"
    }

    fun getAddictionQuizSeverityFa(): String {
        return prefs.getString(KEY_QUIZ_SEVERITY_FA, "ارزیابی نشده") ?: "ارزیابی نشده"
    }

    fun saveAddictionQuizResult(
        scorePercent: Int,
        severityEn: String,
        severityFa: String
    ) {
        val safeScore = scorePercent.coerceIn(0, 100)
        prefs.edit()
            .putBoolean(KEY_HAS_COMPLETED_QUIZ, true)
            .putInt(KEY_QUIZ_SCORE_PERCENT, safeScore)
            .putString(KEY_QUIZ_SEVERITY_EN, severityEn)
            .putString(KEY_QUIZ_SEVERITY_FA, severityFa)
            .apply()
        _hasCompletedQuiz.value = true
        _quizScorePercent.value = safeScore
        _quizSeverityEn.value = severityEn
        _quizSeverityFa.value = severityFa
    }

    // Temporary Emergency Call / SMS Whitelist Window During Block Page Countdown
    fun grantTemporaryCallOrSmsAccess(durationSeconds: Int = 120) {
        val until = System.currentTimeMillis() + (durationSeconds * 1000L)
        prefs.edit().putLong(KEY_EMERGENCY_COMM_WHITELIST_UNTIL, until).apply()
    }

    fun isPackageAllowedDuringLockdown(packageName: String): Boolean {
        val pkgLower = packageName.lowercase()
        if (pkgLower == context.packageName.lowercase()) return true

        // Always allow active incall UI / emergency dialer packages
        val knownDialerOrSmsMarkers = listOf(
            "com.android.dialer",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.samsung.android.incallui",
            "com.android.incallui",
            "com.android.server.telecom",
            "com.android.phone",
            "com.android.mms",
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.miui.mms"
        )
        val defaultSmsPkg = runCatching { Telephony.Sms.getDefaultSmsPackage(context)?.lowercase() }.getOrNull()
        val defaultDialerPkg = runCatching {
            val tm = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            tm?.defaultDialerPackage?.lowercase()
        }.getOrNull()

        val isDialerOrSmsPkg = knownDialerOrSmsMarkers.any { pkgLower.contains(it) } ||
            (defaultSmsPkg != null && pkgLower == defaultSmsPkg) ||
            (defaultDialerPkg != null && pkgLower == defaultDialerPkg)

        if (!isDialerOrSmsPkg) return false

        val whitelistUntil = prefs.getLong(KEY_EMERGENCY_COMM_WHITELIST_UNTIL, 0L)
        return System.currentTimeMillis() <= whitelistUntil
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

    // Anti-Agent Blackout Shield (FLAG_SECURE)
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
        return prefs.getBoolean(KEY_SMART_FOCUS_ENABLED, false)
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
            .putLong(KEY_EMERGENCY_COMM_WHITELIST_UNTIL, 0L)
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
        val currentDays = (diff / TimeUnit.DAYS.toMillis(1)).toInt()
        val recordedBest = prefs.getInt(KEY_LONGEST_STREAK, 0)
        if (currentDays > recordedBest) {
            prefs.edit().putInt(KEY_LONGEST_STREAK, currentDays).apply()
        }
        return currentDays
    }

    fun getLongestStreakDays(): Int {
        val currentDays = getStreakDays()
        val recordedBest = prefs.getInt(KEY_LONGEST_STREAK, 0)
        val best = maxOf(currentDays, recordedBest)
        if (best > recordedBest) {
            prefs.edit().putInt(KEY_LONGEST_STREAK, best).apply()
        }
        return best
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
        val todayEpochDay = TimeUnit.MILLISECONDS.toDays(now)

        prefs.edit()
            .putInt(KEY_LONGEST_STREAK, newBest)
            .putLong(KEY_STREAK_START_TIME, now)
            .putInt(KEY_LAST_REWARDED_STREAK_DAY, 0)
            .putInt(KEY_LAST_NOTIFIED_CLEAN_DAY, 0)
            .putInt(KEY_SLIPS_COUNT, slips)
            .putString(KEY_LAST_SLIP_REASON, reason)
            .putBoolean("relapse_day_$todayEpochDay", true)
            .apply()

        _streakStartTimestamp.value = now
        _longestStreakDays.value = newBest
        _slipsCount.value = slips
        _lastSlipReason.value = reason
        _usageRefreshTrigger.value = now

        StreakWidgetProvider.updateAllWidgets(context)
    }

    // Daily App Usage Tracking
    private fun getTodaySecondsKey(packageName: String): String {
        val todayDay = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())
        return "usage_sec_${todayDay}_$packageName"
    }

    private fun getTodayMinutesLegacyKey(packageName: String): String {
        val todayDay = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())
        return "usage_${todayDay}_$packageName"
    }

    private fun getTodaySystemBaselineKey(packageName: String): String {
        val todayDay = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())
        return "usage_sys_base_${todayDay}_$packageName"
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
                val sysSec = if (daysAgo == 0) getAdjustedSystemUsageSecondsToday(eqPkg) else 0
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
            val sysSec = getAdjustedSystemUsageSecondsToday(pkg)
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
            val currentSysSec = getRawSystemUsageSecondsToday(pkg)
            editor.putInt(getTodaySystemBaselineKey(pkg), currentSysSec)
        }
        editor.apply()
        _usageRefreshTrigger.value = System.currentTimeMillis()
    }

    fun refreshLiveUsageStats() {
        _usageRefreshTrigger.value = System.currentTimeMillis()
    }

    private fun getAdjustedSystemUsageSecondsToday(packageName: String): Int {
        val raw = getRawSystemUsageSecondsToday(packageName)
        val baseline = prefs.getInt(getTodaySystemBaselineKey(packageName), 0)
        return (raw - baseline).coerceAtLeast(0)
    }

    private fun getRawSystemUsageSecondsToday(packageName: String): Int {
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

    fun consumeOneTimeFreshScratchResetFlag(): Boolean {
        val alreadyDone = prefs.getBoolean(KEY_FRESH_SCRATCH_V4_INIT, false)
        if (!alreadyDone) {
            prefs.edit().putBoolean(KEY_FRESH_SCRATCH_V4_INIT, true).apply()
            return true
        }
        return false
    }

    fun resetAllPreferencesToFreshScratch() {
        val now = System.currentTimeMillis()
        val todayEpochDay = TimeUnit.MILLISECONDS.toDays(now)
        val defaultPledge = "I am reclaiming my focus, energy, and self-respect. A 5-minute urge is never worth trading my clean streak."
        val preservedDark = getIsDarkTheme()
        val preservedPersian = getIsPersianLanguage()
        val preservedRepo = getGithubRepoSlug()
        val preservedQuizDone = hasCompletedOnboardingQuiz()
        val preservedQuizScore = getAddictionQuizScore()
        val preservedQuizSevEn = getAddictionQuizSeverityEn()
        val preservedQuizSevFa = getAddictionQuizSeverityFa()

        prefs.edit()
            .clear()
            .putBoolean(KEY_FRESH_SCRATCH_V4_INIT, true)
            .putBoolean(KEY_V04_DEFAULTS_MIGRATED, true)
            .putBoolean(KEY_V04_GITHUB_REPO_MIGRATED, true)
            .putBoolean(KEY_V04_ALL_OFF_DEFAULT_MIGRATED, true)
            .putBoolean(KEY_PROTECTION_ENABLED, false)
            .putInt(KEY_LOCK_DURATION_MINUTES, 5)
            .putBoolean(KEY_STRICT_MODE_ENABLED, false)
            .putBoolean(KEY_PREVENT_UNINSTALL_ENABLED, false)
            .putLong(KEY_ACTIVE_LOCK_END_TIME, 0L)
            .putLong(KEY_STREAK_START_TIME, now)
            .putLong(KEY_FIRST_INSTALL_EPOCH_DAY, todayEpochDay)
            .putInt(KEY_LONGEST_STREAK, 0)
            .putInt(KEY_COINS_BALANCE, 0)
            .putInt(KEY_LAST_REWARDED_STREAK_DAY, 0)
            .putInt(KEY_LAST_NOTIFIED_CLEAN_DAY, 0)
            .putInt(KEY_SLIPS_COUNT, 0)
            .putString(KEY_LAST_SLIP_REASON, "")
            .putInt(KEY_URGES_DEFEATED_COUNT, 0)
            .putString(KEY_PERSONAL_PLEDGE, defaultPledge)
            .putBoolean(KEY_BIOMETRIC_LOCK_ENABLED, false)
            .putString(KEY_SECURITY_PIN_CODE, "0000")
            .putBoolean(KEY_BLACKOUT_PRIVACY_SHIELD, false)
            .putLong(KEY_SCREENSHOT_TEMP_UNLOCK_UNTIL, 0L)
            .putBoolean(KEY_SMART_FOCUS_ENABLED, false)
            .putBoolean(KEY_IS_DARK_THEME, preservedDark)
            .putBoolean(KEY_IS_PERSIAN_LANGUAGE, preservedPersian)
            .putString(KEY_GITHUB_REPO_SLUG, preservedRepo)
            .putBoolean(KEY_SECTION_EXIT_ONLY_MODE, false)
            .putBoolean(KEY_HAS_COMPLETED_QUIZ, preservedQuizDone)
            .putInt(KEY_QUIZ_SCORE_PERCENT, preservedQuizScore)
            .putString(KEY_QUIZ_SEVERITY_EN, preservedQuizSevEn)
            .putString(KEY_QUIZ_SEVERITY_FA, preservedQuizSevFa)
            .apply()

        _isProtectionEnabled.value = false
        _lockDurationMinutes.value = 5
        _strictModeEnabled.value = false
        _isPreventUninstallEnabled.value = false
        _coinsBalance.value = 0
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
        _isSmartFocusEnabled.value = false
        _isDarkTheme.value = preservedDark
        _isPersianLanguage.value = preservedPersian
        _githubRepoSlug.value = preservedRepo
        _isSectionExitOnlyMode.value = false
        _usageRefreshTrigger.value = now

        StreakWidgetProvider.updateAllWidgets(context)
    }

    fun consumeSectionRulesOffMigration(): Boolean {
        if (!prefs.getBoolean(KEY_V04_SECTIONS_OFF_MIGRATED, false)) {
            prefs.edit().putBoolean(KEY_V04_SECTIONS_OFF_MIGRATED, true).apply()
            return true
        }
        return false
    }

    companion object {
        const val DEFAULT_GITHUB_REPO = "mehdisrmd/My-google-app"
        private const val KEY_PROTECTION_ENABLED = "key_protection_enabled"
        private const val KEY_LOCK_DURATION_MINUTES = "key_lock_duration_minutes"
        private const val KEY_STRICT_MODE_ENABLED = "key_strict_mode_enabled"
        private const val KEY_PREVENT_UNINSTALL_ENABLED = "key_prevent_uninstall_enabled"
        private const val KEY_COINS_BALANCE = "key_coins_balance"
        private const val KEY_LAST_REWARDED_STREAK_DAY = "key_last_rewarded_streak_day"
        private const val KEY_LAST_NOTIFIED_CLEAN_DAY = "key_last_notified_clean_day"
        private const val KEY_FIRST_INSTALL_EPOCH_DAY = "key_first_install_epoch_day"
        private const val KEY_EMERGENCY_COMM_WHITELIST_UNTIL = "key_emergency_comm_whitelist_until"
        private const val KEY_HAS_COMPLETED_QUIZ = "key_has_completed_quiz"
        private const val KEY_QUIZ_SCORE_PERCENT = "key_quiz_score_percent"
        private const val KEY_QUIZ_SEVERITY_EN = "key_quiz_severity_en"
        private const val KEY_QUIZ_SEVERITY_FA = "key_quiz_severity_fa"
        private const val KEY_ACTIVE_LOCK_END_TIME = "key_active_lock_end_time"
        private const val KEY_ACTIVE_LOCK_DURATION = "key_active_lock_duration"
        private const val KEY_ACTIVE_LOCK_TRIGGER = "key_active_lock_trigger"
        private const val KEY_ACTIVE_LOCK_APP = "key_active_lock_app"
        private const val KEY_LAST_INCIDENT_TIME = "key_last_incident_time"
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
        private const val KEY_IS_DARK_THEME = "key_is_dark_theme"
        private const val KEY_IS_PERSIAN_LANGUAGE = "key_is_persian_language"
        private const val KEY_GITHUB_REPO_SLUG = "key_github_repo_slug"
        private const val KEY_SECTION_EXIT_ONLY_MODE = "key_section_exit_only_mode"
        private const val KEY_FRESH_SCRATCH_V4_INIT = "key_fresh_scratch_v4_init"
        private const val KEY_V04_DEFAULTS_MIGRATED = "key_v04_defaults_migrated"
        private const val KEY_V04_GITHUB_REPO_MIGRATED = "key_v04_github_repo_migrated"
        private const val KEY_V04_ALL_OFF_DEFAULT_MIGRATED = "key_v04_all_off_default_migrated"
        private const val KEY_V04_SECTIONS_OFF_MIGRATED = "key_v04_sections_off_migrated"

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

        fun requestUsageStatsPermission(context: Context): Boolean {
            return try {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                true
            } catch (_: Exception) {
                try {
                    val fallback = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(fallback)
                    true
                } catch (_: Exception) {
                    false
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
