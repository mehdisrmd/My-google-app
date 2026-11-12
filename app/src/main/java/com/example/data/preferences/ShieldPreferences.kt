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
import com.example.service.KeywordGraceEpisode
import com.example.service.KeywordGraceLogic
import com.example.service.ShieldAccessibilityService
import com.example.ui.theme.SeasonalTheme
import com.example.util.DayMath
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
            // The seasonal theme and the quiz result are the user's own
            // choices/results, not protection state. The manual reset
            // (resetAllPreferencesToFreshScratch) already keeps them; this
            // first-run wipe used to silently drop them.
            val preservedSeasonal = prefs.getString(KEY_SEASONAL_THEME, null)
            val hadQuiz = prefs.contains(KEY_HAS_COMPLETED_QUIZ)
            val preservedQuizDone = prefs.getBoolean(KEY_HAS_COMPLETED_QUIZ, false)
            val preservedQuizScore = prefs.getInt(KEY_QUIZ_SCORE_PERCENT, -1)
            val preservedQuizSevEn = prefs.getString(KEY_QUIZ_SEVERITY_EN, null)
            val preservedQuizSevFa = prefs.getString(KEY_QUIZ_SEVERITY_FA, null)
            val wipe = prefs.edit()
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
            if (preservedSeasonal != null) {
                wipe.putString(KEY_SEASONAL_THEME, preservedSeasonal)
            }
            if (hadQuiz) {
                wipe.putBoolean(KEY_HAS_COMPLETED_QUIZ, preservedQuizDone)
                    .putInt(KEY_QUIZ_SCORE_PERCENT, preservedQuizScore)
                if (preservedQuizSevEn != null) wipe.putString(KEY_QUIZ_SEVERITY_EN, preservedQuizSevEn)
                if (preservedQuizSevFa != null) wipe.putString(KEY_QUIZ_SEVERITY_FA, preservedQuizSevFa)
            }
            wipe.apply()
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

    // Uninstall Guard: persisted wall-clock deadline of the 5-hour wait
    private val _uninstallUnlockDeadline = MutableStateFlow(getUninstallUnlockDeadline())
    val uninstallUnlockDeadline: StateFlow<Long> = _uninstallUnlockDeadline.asStateFlow()

    // Seasonal Theme palette selection (stored as the enum name)
    private val _seasonalThemeName = MutableStateFlow(getSeasonalTheme().name)

    val seasonalThemeName: StateFlow<String> = _seasonalThemeName.asStateFlow()

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

    // Seasonal Theme (Spring / Summer / Autumn / Winter + Purple Glass default)
    fun getSeasonalTheme(): SeasonalTheme {
        return SeasonalTheme.fromName(prefs.getString(KEY_SEASONAL_THEME, null))
    }

    fun setSeasonalTheme(theme: SeasonalTheme) {
        prefs.edit().putString(KEY_SEASONAL_THEME, theme.name).apply()
        _seasonalThemeName.value = theme.name
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
            put("seasonalTheme", getSeasonalTheme().name)
            put("githubRepoSlug", getGithubRepoSlug())
            put("isSectionExitOnlyMode", getIsSectionExitOnlyMode())
            put("hasCompletedQuiz", hasCompletedOnboardingQuiz())
            put("quizScorePercent", getAddictionQuizScore())
            put("quizSeverityEn", getAddictionQuizSeverityEn())
            put("quizSeverityFa", getAddictionQuizSeverityFa())
        }
    }

    /**
     * True when settings may be imported or reset right now. Both rewrite the
     * protection flags, the PIN and the active lock, so they are refused while
     * a lock is running: otherwise a backup file (or a reset) would be a way
     * to switch protection off in the middle of a block. UI code should check
     * this first and show a message instead of calling the method.
     */
    fun canResetOrImport(): Boolean = !isCurrentlyLocked()

    fun importPreferencesFromJson(json: org.json.JSONObject) {
        if (!canResetOrImport()) return
        val now = System.currentTimeMillis()
        val prot = json.optBoolean("isProtectionEnabled", getIsProtectionEnabled())
        val dur = json.optInt("lockDurationMinutes", getLockDurationMinutes()).coerceIn(1, 1440)
        val strict = json.optBoolean("strictModeEnabled", getStrictModeEnabled())
        val preventUninst = json.optBoolean("isPreventUninstallEnabled", getIsPreventUninstallEnabled())
        // Note: old backups may contain a "coinsBalance" field — the coin
        // system was removed at the owner's request, so it is ignored here.
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
        val seasonalTheme = SeasonalTheme.fromName(
            json.optString("seasonalTheme", getSeasonalTheme().name)
        )
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
            .putString(KEY_SEASONAL_THEME, seasonalTheme.name)
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

        _seasonalThemeName.value = seasonalTheme.name
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
        // Protection can never be switched off in the middle of a running lock.
        if (!enabled && isCurrentlyLocked()) return
        val editor = prefs.edit().putBoolean(KEY_PROTECTION_ENABLED, enabled)
        if (enabled) {
            // Re-arming the shield consumes any pending/elapsed uninstall
            // unlock: switching protection off again needs a fresh 5h wait.

            editor.putLong(KEY_UNINSTALL_UNLOCK_DEADLINE, 0L)
            _uninstallUnlockDeadline.value = 0L
        }
        editor.apply()
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

        // Same rule as setProtectionEnabled: not while a lock is running.
        if (!enabled && isCurrentlyLocked()) return
        prefs.edit().putBoolean(KEY_PREVENT_UNINSTALL_ENABLED, enabled).apply()
        _isPreventUninstallEnabled.value = enabled
    }

    // Uninstall Guard — 5-hour wait before protection may be turned off.
    // The deadline is a persisted wall-clock timestamp, so the countdown
    // keeps running across reboots with no receiver or service needed.
    fun getUninstallUnlockDeadline(): Long {
        return prefs.getLong(KEY_UNINSTALL_UNLOCK_DEADLINE, 0L)
    }

    /**
     * Starts the 5-hour uninstall wait and returns the deadline. If a wait is
     * already running, the original deadline is kept (tapping again never
     * extends or restarts the countdown).
     */
    fun requestUninstallUnlock(nowMs: Long = System.currentTimeMillis()): Long {
        val existing = getUninstallUnlockDeadline()
        if (UninstallGuard.isRequested(existing) && !UninstallGuard.isUnlocked(existing, nowMs)) {
            return existing
        }
        val deadline = UninstallGuard.deadlineForRequest(nowMs)
        prefs.edit().putLong(KEY_UNINSTALL_UNLOCK_DEADLINE, deadline).apply()
        _uninstallUnlockDeadline.value = deadline
        return deadline
    }

    fun isUninstallUnlocked(nowMs: Long = System.currentTimeMillis()): Boolean {
        return UninstallGuard.isUnlocked(getUninstallUnlockDeadline(), nowMs)
    }


    /** Master protection may be switched OFF only after the wait elapsed. */
    fun canDisableProtection(nowMs: Long = System.currentTimeMillis()): Boolean {
        return UninstallGuard.canDisableProtection(getUninstallUnlockDeadline(), nowMs)
    }

    fun getUninstallWaitRemainingMillis(nowMs: Long = System.currentTimeMillis()): Long {
        return UninstallGuard.remainingMillis(getUninstallUnlockDeadline(), nowMs)
    }

    /**
     * Synchronizes the clean-streak bookkeeping (Best Chain) and returns the
     * new clean day milestone if a new day (Day 1, Day 2, Day 3...) was just
     * reached and hasn't been notified yet, or 0 otherwise.
     *
     * (This was previously `syncCleanDaysCoinsAndCheckMilestone` and also
     * awarded +1 coin per clean day; the coin system was removed at the
     * owner's request. Streak-day counting and the milestone return value
     * are unchanged.)
     */
    fun syncCleanDaysAndCheckMilestone(): Int {
        val currentStreakDays = getStreakDays()
        // Always ensure Best Chain is updated & remembered
        val recordedBest = prefs.getInt(KEY_LONGEST_STREAK, 0)
        if (currentStreakDays > recordedBest) {
            prefs.edit().putInt(KEY_LONGEST_STREAK, currentStreakDays).apply()
            _longestStreakDays.value = currentStreakDays
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

    /** All epoch days in [fromEpochDay, toEpochDay] that have a recorded relapse. */
    fun getRelapseEpochDaysInRange(fromEpochDay: Long, toEpochDay: Long): Set<Long> {
        if (toEpochDay < fromEpochDay) return emptySet()
        val result = mutableSetOf<Long>()

        var day = fromEpochDay
        while (day <= toEpochDay) {
            if (prefs.getBoolean("relapse_day_$day", false)) {
                result.add(day)
            }
            day++
        }
        return result
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

    /**
     * Opens the emergency Call/SMS window for the REST of the current block
     * episode (until the active lock's own end time, at least 120 seconds).
     * Used when the user taps Phone Call / Text on the block screen: the
     * dialer / SMS packages are exempt from overlay-blocking for the whole
     * episode (see [isPackageAllowedDuringLockdown]), and this makes
     * LockdownActivity's pull-back guard agree with that exemption for the
     * whole episode too — a long call or a careful message session must
     * never be cut short by the guard's old fixed 120-second window.
     */
    @Synchronized
    fun grantCallOrSmsAccessForCurrentLock() {
        val now = System.currentTimeMillis()

        val until = maxOf(
            prefs.getLong(KEY_EMERGENCY_COMM_WHITELIST_UNTIL, 0L),
            now + 120_000L,
            getActiveLockEndTime()
        )
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

        // Calls & Messages stay fully accessible for the whole block episode:
        // the default dialer / SMS app (and known dialer, messaging and
        // in-call packages) are never overlay-blocked while a block is active.
        // (Previously they were only allowed inside a short 120s window after
        // tapping the block screen's Call/SMS buttons.)
        if (isDialerOrSmsPkg) return true

        // The screens those apps open on the way: the contacts picker, the
        // photo / file picker for a message attachment, permission prompts
        // and the emergency-information screen. Without them, tapping "choose
        // contact" or "attach photo" would hand the screen back to the block
        // page in the middle of writing a message. They are only allowed while
        // the user's Call/SMS window (opened by the block screen's buttons) is
        // active, so they cannot be used as a way around the block.
        return pkgLower in COMM_HELPER_PACKAGES && hasActiveEmergencyCommWindow()
    }

    /**
     * True while the short "user just tapped Call/SMS on the block screen"
     * window is open. LockdownActivity uses this — not the package check
     * above — to decide whether leaving the block screen is a deliberate
     * trip to the dialer/messages or an attempt to escape the lock.
     *
     * The window is open strictly BEFORE its end time, the same boundary
     * [isCurrentlyLocked] uses for the lock itself.
     */
    fun hasActiveEmergencyCommWindow(nowMs: Long = System.currentTimeMillis()): Boolean {
        val whitelistUntil = prefs.getLong(KEY_EMERGENCY_COMM_WHITELIST_UNTIL, 0L)

        return nowMs < whitelistUntil
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
        // Local calendar day, so a slot's "already triggered today" flag flips
        // at the user's midnight, not at UTC midnight.
        val todayEpochDay = DayMath.localEpochDay(nowMs)
        val lastTriggeredDay = prefs.getLong("focus_slot_triggered_day_$slotId", -1L)
        return lastTriggeredDay == todayEpochDay
    }

    fun markFocusSlotTriggeredToday(
        slotId: Long,
        nowMs: Long = System.currentTimeMillis()
    ) {
        val todayEpochDay = DayMath.localEpochDay(nowMs)
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

    /**
     * Starts a lock and returns its end time.
     *
     * A lock that is still running is NEVER restarted, shortened or extended:
     * the existing end time is returned unchanged and nothing is written. A
     * timed block must end exactly when its own timer reaches zero, and a
     * second trigger (the 1-second ticker, an accessibility event, a focus
     * slot) used to overwrite the end time and restart the countdown.
     */
    @Synchronized
    fun startLockdown(
        durationMinutes: Int,
        triggerWord: String,
        sourceApp: String,
        graceAvailable: Boolean = false
    ): Long {
        val now = System.currentTimeMillis()
        val existingEnd = getActiveLockEndTime()
        if (existingEnd > now) return existingEnd

        val endTime = now + TimeUnit.MINUTES.toMillis(durationMinutes.toLong())
        prefs.edit()
            .putLong(KEY_ACTIVE_LOCK_END_TIME, endTime)
            .putInt(KEY_ACTIVE_LOCK_DURATION, durationMinutes)

            .putString(KEY_ACTIVE_LOCK_TRIGGER, triggerWord)
            .putString(KEY_ACTIVE_LOCK_APP, sourceApp)
            .putBoolean(KEY_ACTIVE_LOCK_GRACE_AVAILABLE, graceAvailable)
            .putLong(KEY_LAST_INCIDENT_TIME, now)
            .apply()
        _activeLockEndTime.value = endTime
        return endTime
    }

    @Synchronized
    fun clearLockdown() {
        prefs.edit()
            .putLong(KEY_ACTIVE_LOCK_END_TIME, 0L)
            .putLong(KEY_EMERGENCY_COMM_WHITELIST_UNTIL, 0L)
            .putBoolean(KEY_ACTIVE_LOCK_GRACE_AVAILABLE, false)
            .remove(KEY_ACTIVE_LOCK_TRIGGER)
            .remove(KEY_ACTIVE_LOCK_APP)
            .apply()
        _activeLockEndTime.value = 0L
    }

    /** True when the current lock may offer the one-time 10-second grace. */
    fun getActiveLockGraceAvailable(): Boolean {
        return prefs.getBoolean(KEY_ACTIVE_LOCK_GRACE_AVAILABLE, false)
    }

    // Keyword Grace episode state (see KeywordGraceLogic). Stored in prefs so
    // the accessibility service, the 1s enforcement ticker and the block
    // screen all share one consistent episode across the lockdown cycle.
    fun getKeywordGraceEpisode(): KeywordGraceEpisode {
        return KeywordGraceEpisode(
            word = prefs.getString(KEY_GRACE_EPISODE_WORD, "") ?: "",

            graceOffered = prefs.getBoolean(KEY_GRACE_EPISODE_OFFERED, false),
            graceUsed = prefs.getBoolean(KEY_GRACE_EPISODE_USED, false),
            graceUntilMs = prefs.getLong(KEY_KEYWORD_GRACE_UNTIL, 0L)
        )
    }

    fun saveKeywordGraceEpisode(episode: KeywordGraceEpisode) {
        prefs.edit()
            .putString(KEY_GRACE_EPISODE_WORD, episode.word)
            .putBoolean(KEY_GRACE_EPISODE_OFFERED, episode.graceOffered)
            .putBoolean(KEY_GRACE_EPISODE_USED, episode.graceUsed)
            .putLong(KEY_KEYWORD_GRACE_UNTIL, episode.graceUntilMs)
            .apply()
    }

    /**
     * The user tapped "10 seconds free" on a keyword block screen: mark the
     * current episode's grace as used, open the 10-second free window, and
     * end the current lockdown so the block screen dismisses immediately.
     * After the window, the accessibility service re-checks the screen: if
     * the trigger word is still there it re-blocks WITHOUT the grace button.
     *
     * Only valid while a lock that actually offers the grace is running. A
     * stale or repeated tap (the lock already ended, or it was a lock without
     * the button) changes nothing, so it can neither open a free window out
     * of nowhere nor clear someone else's lock.
     */
    @Synchronized
    fun beginKeywordGracePeriod(nowMs: Long = System.currentTimeMillis()) {
        if (!isCurrentlyLocked() || !getActiveLockGraceAvailable()) return
        val episode = getKeywordGraceEpisode()
        val updated = KeywordGraceLogic.onGraceButtonTapped(episode, nowMs)

        saveKeywordGraceEpisode(updated)
        clearLockdown()
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

    @Synchronized
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

    @Synchronized
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
    //
    // "Today" for usage counters is the LOCAL calendar day (DayMath), the same
    // day AppLimitBudget uses to end a daily-limit block at local midnight.
    // With UTC day keys the counter reset hours away from that midnight, which
    // could re-lock an app for another ~24 hours right after a block ended.
    private fun getTodaySecondsKey(packageName: String): String {
        val todayDay = DayMath.localEpochDay()
        return "usage_sec_${todayDay}_$packageName"
    }

    private fun getTodayMinutesLegacyKey(packageName: String): String {
        val todayDay = DayMath.localEpochDay()
        return "usage_${todayDay}_$packageName"

    }

    private fun getTodaySystemBaselineKey(packageName: String): String {
        val todayDay = DayMath.localEpochDay()
        return "usage_sys_base_${todayDay}_$packageName"
    }

    fun getDayAppUsageMinutes(daysAgo: Int, packageNames: List<String>): Int {
        if (packageNames.isEmpty()) return 0
        val targetEpochDay = DayMath.localEpochDay() - daysAgo
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

    @Synchronized
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

    @Synchronized
    fun resetTodayAppUsage(packageName: String, appName: String = "") {
        val equivalents = com.example.util.AppPackageResolver.getAllEquivalentPackages(packageName, appName)
        val editor = prefs.edit()
        // The baseline must be the CURRENT system figure, so this one read
        // bypasses the short-lived usage cache.
        val freshSystemUsage = systemUsageSnapshot(force = true)
        for (pkg in equivalents) {
            editor.remove(getTodaySecondsKey(pkg))
            editor.remove(getTodayMinutesLegacyKey(pkg))
            val currentSysSec = freshSystemUsage[pkg] ?: 0
            editor.putInt(getTodaySystemBaselineKey(pkg), currentSysSec)
        }
        editor.apply()
        _usageRefreshTrigger.value = System.currentTimeMillis()
    }

    fun refreshLiveUsageStats() {
        synchronized(usageCacheLock) { sysUsageCacheAtMs = 0L }
        _usageRefreshTrigger.value = System.currentTimeMillis()
    }

    private fun getAdjustedSystemUsageSecondsToday(packageName: String): Int {
        val raw = getRawSystemUsageSecondsToday(packageName)
        val baseline = prefs.getInt(getTodaySystemBaselineKey(packageName), 0)
        return (raw - baseline).coerceAtLeast(0)
    }

    private fun getRawSystemUsageSecondsToday(packageName: String): Int {
        return systemUsageSnapshot()[packageName] ?: 0

    }

    // The enforcement ticker reads today's usage every second. Aggregating the
    // whole day's usage stats for every read is expensive, so one snapshot of
    // ALL packages is shared for a few seconds. The cache is also dropped when
    // the local day changes, so yesterday's totals can never be served as
    // today's for a moment after midnight.
    private val usageCacheLock = Any()
    private var sysUsageCacheAtMs = 0L
    private var sysUsageCacheDay = Long.MIN_VALUE
    private var sysUsageCache: Map<String, Int> = emptyMap()

    private fun systemUsageSnapshot(force: Boolean = false): Map<String, Int> {
        synchronized(usageCacheLock) {
            val now = System.currentTimeMillis()
            val day = DayMath.localEpochDay(now)
            val fresh = sysUsageCacheAtMs != 0L &&
                day == sysUsageCacheDay &&
                now - sysUsageCacheAtMs in 0 until SYS_USAGE_CACHE_MS
            if (!force && fresh) return sysUsageCache

            sysUsageCache = if (!hasUsageStatsPermission(context)) {
                emptyMap()
            } else {
                try {
                    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                    if (usm == null) {
                        emptyMap()
                    } else {
                        val calendar = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)

                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val startOfDay = calendar.timeInMillis
                        usm.queryAndAggregateUsageStats(startOfDay, now)
                            .mapValues { (it.value.totalTimeInForeground / 1000L).toInt() }
                    }
                } catch (_: Exception) {
                    emptyMap()
                }
            }
            sysUsageCacheAtMs = now
            sysUsageCacheDay = day
            return sysUsageCache
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
        // A reset also clears the active lock, the PIN and every protection
        // flag, so it is refused while a lock is running (see canResetOrImport).
        if (!canResetOrImport()) return
        val now = System.currentTimeMillis()
        val todayEpochDay = TimeUnit.MILLISECONDS.toDays(now)
        val defaultPledge = "I am reclaiming my focus, energy, and self-respect. A 5-minute urge is never worth trading my clean streak."
        val preservedDark = getIsDarkTheme()
        val preservedPersian = getIsPersianLanguage()
        val preservedSeasonalTheme = getSeasonalTheme()
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
            .putString(KEY_SEASONAL_THEME, preservedSeasonalTheme.name)

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
        _seasonalThemeName.value = preservedSeasonalTheme.name
        _uninstallUnlockDeadline.value = 0L
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
        private const val KEY_SEASONAL_THEME = "key_seasonal_theme"
        private const val KEY_UNINSTALL_UNLOCK_DEADLINE = "key_uninstall_unlock_deadline"
        private const val KEY_ACTIVE_LOCK_GRACE_AVAILABLE = "key_active_lock_grace_available"
        private const val KEY_GRACE_EPISODE_WORD = "key_grace_episode_word"
        private const val KEY_GRACE_EPISODE_OFFERED = "key_grace_episode_offered"
        private const val KEY_GRACE_EPISODE_USED = "key_grace_episode_used"
        private const val KEY_KEYWORD_GRACE_UNTIL = "key_keyword_grace_until"
        private const val KEY_GITHUB_REPO_SLUG = "key_github_repo_slug"
        private const val KEY_SECTION_EXIT_ONLY_MODE = "key_section_exit_only_mode"
        private const val KEY_FRESH_SCRATCH_V4_INIT = "key_fresh_scratch_v4_init"
        private const val KEY_V04_DEFAULTS_MIGRATED = "key_v04_defaults_migrated"
        private const val KEY_V04_GITHUB_REPO_MIGRATED = "key_v04_github_repo_migrated"
        private const val KEY_V04_ALL_OFF_DEFAULT_MIGRATED = "key_v04_all_off_default_migrated"
        private const val KEY_V04_SECTIONS_OFF_MIGRATED = "key_v04_sections_off_migrated"

        /** How long one system usage snapshot is reused before re-querying. */
        private const val SYS_USAGE_CACHE_MS = 10_000L

        /**
         * Screens the dialer / messaging apps hand over to (contact picker,
         * attachment picker, permission prompts, emergency information).
         * Allowed during a lock only while the Call/SMS window is open.

         */
        private val COMM_HELPER_PACKAGES = setOf(
            "com.android.contacts",
            "com.google.android.contacts",
            "com.samsung.android.app.contacts",
            "com.android.documentsui",
            "com.google.android.documentsui",
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller",
            "com.android.emergency"
        )

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
