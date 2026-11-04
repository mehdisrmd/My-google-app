package com.example.data.repository

import com.example.data.local.DefaultKeywords
import com.example.data.local.DefaultSectionRules
import com.example.data.local.ShieldDao
import com.example.data.model.AppUsageLimit
import com.example.data.model.BlockIncident
import com.example.data.model.BlockedCategory
import com.example.data.model.BlockedKeyword
import com.example.data.model.FocusScheduleSlot
import com.example.data.model.SectionBlockRule
import com.example.data.preferences.ShieldPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ShieldRepository(
    private val dao: ShieldDao,
    val preferences: ShieldPreferences
) {
    val allKeywords: Flow<List<BlockedKeyword>> = dao.getAllKeywords()
    val allIncidents: Flow<List<BlockIncident>> = dao.getAllIncidents()
    val incidentCount: Flow<Int> = dao.getIncidentCount()

    val allAppUsageLimits: Flow<List<AppUsageLimit>> = dao.getAllAppUsageLimits()
    val allSectionRules: Flow<List<SectionBlockRule>> = dao.getAllSectionRules()
    val allFocusSlots: Flow<List<FocusScheduleSlot>> = dao.getAllFocusSlots()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedDefaultKeywordsIfNeeded()
            seedDefaultSectionRulesIfNeeded()
            seedDefaultAppLimitsIfNeeded()
            seedDefaultFocusSlotsIfNeeded()
        }
    }

    suspend fun seedDefaultKeywordsIfNeeded() {
        if (dao.getKeywordCount() == 0) {
            dao.insertKeywords(DefaultKeywords.getPreloadedKeywords())
        }
    }

    suspend fun seedDefaultSectionRulesIfNeeded() {
        if (dao.getSectionRuleCount() == 0) {
            dao.insertSectionRules(DefaultSectionRules.getDefaultRules())
        }
    }

    suspend fun seedDefaultAppLimitsIfNeeded() {
        if (dao.getAppUsageLimitCount() > 0) return
        val limits = listOf(
            AppUsageLimit(
                packageName = "com.instagram.android",
                appName = "Instagram",
                dailyLimitMinutes = 120, // 2 hours
                isEnabled = true,
                lastModifiedTimestamp = 0L
            ),
            AppUsageLimit(
                packageName = "org.telegram.messenger",
                appName = "Telegram",
                dailyLimitMinutes = 60, // 1 hour
                isEnabled = false,
                lastModifiedTimestamp = 0L
            ),
            AppUsageLimit(
                packageName = "com.google.android.youtube",
                appName = "YouTube",
                dailyLimitMinutes = 90,
                isEnabled = false,
                lastModifiedTimestamp = 0L
            ),
            AppUsageLimit(
                packageName = "com.android.chrome",
                appName = "Chrome",
                dailyLimitMinutes = 120,
                isEnabled = false,
                lastModifiedTimestamp = 0L
            )
        )
        for (lim in limits) {
            dao.insertAppUsageLimit(lim)
        }
    }

    suspend fun seedDefaultFocusSlotsIfNeeded() {
        if (dao.getFocusSlotCount() > 0) return
        val defaultSlots = listOf(
            FocusScheduleSlot(
                title = "Morning Deep Work",
                startHour = 9,
                startMinute = 0,
                endHour = 10,
                endMinute = 30,
                daysOfWeekCsv = "1,2,3,4,5",
                isEnabled = false,
                autoTriggerLockdown = true
            ),
            FocusScheduleSlot(
                title = "Afternoon Focus Sprint",
                startHour = 14,
                startMinute = 0,
                endHour = 15,
                endMinute = 30,
                daysOfWeekCsv = "1,2,3,4,5",
                isEnabled = false,
                autoTriggerLockdown = true
            )
        )
        dao.insertFocusSlots(defaultSlots)
    }

    /**
     * Resets the entire application (preferences, usage counters, cooldowns, incidents,
     * custom rules, app limits, and focus slots) back to a clean factory state so the user
     * can test the app from scratch.
     */
    suspend fun resetEntireAppFromScratch() {
        preferences.resetAllPreferencesToFreshScratch()
        dao.clearIncidents()
        dao.clearAllKeywords()
        dao.clearAllAppUsageLimits()
        dao.clearAllSectionRules()
        dao.clearAllFocusSlots()

        seedDefaultKeywordsIfNeeded()
        seedDefaultSectionRulesIfNeeded()
        seedDefaultAppLimitsIfNeeded()
        seedDefaultFocusSlotsIfNeeded()
    }

    suspend fun getActiveKeywordsSync(): List<BlockedKeyword> {
        return dao.getEnabledKeywordsSync()
    }

    suspend fun getActiveSectionRulesSync(): List<SectionBlockRule> {
        return dao.getEnabledSectionRulesSync()
    }

    suspend fun getActiveAppUsageLimitsSync(): List<AppUsageLimit> {
        return dao.getEnabledAppUsageLimitsSync()
    }

    suspend fun getActiveFocusSlotsSync(): List<FocusScheduleSlot> {
        return dao.getEnabledFocusSlotsSync()
    }

    suspend fun addCustomKeyword(text: String, isDomain: Boolean) {
        val trimmed = text.trim().lowercase()
        if (trimmed.isEmpty()) return
        val category = if (isDomain) BlockedCategory.CUSTOM_DOMAIN else BlockedCategory.CUSTOM_KEYWORD
        val keyword = BlockedKeyword(
            text = trimmed,
            category = category,
            isEnabled = true,
            isRegex = false
        )
        dao.insertKeyword(keyword)
    }

    /**
     * Batch import multiple words or domains separated by comma, newline, or space.
     */
    suspend fun addCustomKeywordsBatch(rawText: String, isDomain: Boolean): Int {
        val tokens = rawText.split(Regex("[,\\n\\r]+"))
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .distinct()

        if (tokens.isEmpty()) return 0

        val category = if (isDomain) BlockedCategory.CUSTOM_DOMAIN else BlockedCategory.CUSTOM_KEYWORD
        val entities = tokens.map { token ->
            BlockedKeyword(
                text = token,
                category = category,
                isEnabled = true,
                isRegex = false
            )
        }
        dao.insertKeywords(entities)
        return entities.size
    }

    suspend fun toggleKeyword(keyword: BlockedKeyword) {
        dao.updateKeyword(keyword.copy(isEnabled = !keyword.isEnabled))
    }

    suspend fun deleteKeyword(id: Long) {
        dao.deleteKeywordById(id)
    }

    // App Usage Limit CRUD with strict 2-hour cooldown enforcement
    suspend fun saveAppUsageLimit(
        limit: AppUsageLimit,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        val existing = dao.getAppUsageLimitByPackage(limit.packageName)
        if (existing != null && existing.isCooldownActive(nowMs)) {
            return false
        }
        val stamped = if (limit.lastModifiedTimestamp > 0L) {
            limit
        } else {
            limit.copy(lastModifiedTimestamp = nowMs)
        }
        dao.insertAppUsageLimit(stamped)
        return true
    }

    suspend fun toggleAppUsageLimit(
        limit: AppUsageLimit,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        val existing = dao.getAppUsageLimitByPackage(limit.packageName) ?: limit
        // If turning OFF an active limit, enforce the 2-hour cooldown rule
        if (existing.isEnabled && existing.isCooldownActive(nowMs)) {
            return false
        }
        dao.updateAppUsageLimit(
            existing.copy(
                isEnabled = !existing.isEnabled,
                lastModifiedTimestamp = nowMs
            )
        )
        return true
    }

    suspend fun deleteAppUsageLimit(
        pkg: String,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        val existing = dao.getAppUsageLimitByPackage(pkg)
        if (existing != null && existing.isCooldownActive(nowMs)) {
            return false
        }
        dao.deleteAppUsageLimit(pkg)
        return true
    }

    // Section Block Rule CRUD
    suspend fun saveSectionRule(rule: SectionBlockRule) {
        dao.insertSectionRule(rule)
    }

    suspend fun toggleSectionRule(rule: SectionBlockRule) {
        dao.updateSectionRule(rule.copy(isEnabled = !rule.isEnabled))
    }

    suspend fun deleteSectionRule(id: Long) {
        dao.deleteSectionRuleById(id)
    }

    // Smart Focus Schedule Slot CRUD
    suspend fun saveFocusSlot(slot: FocusScheduleSlot): Long {
        return dao.insertFocusSlot(slot)
    }

    suspend fun updateFocusSlot(slot: FocusScheduleSlot) {
        dao.updateFocusSlot(slot)
    }

    suspend fun toggleFocusSlot(slot: FocusScheduleSlot) {
        dao.updateFocusSlot(slot.copy(isEnabled = !slot.isEnabled))
    }

    suspend fun deleteFocusSlot(id: Long) {
        dao.deleteFocusSlotById(id)
    }

    suspend fun recordIncident(
        matchedWord: String,
        sourceApp: String,
        durationMinutes: Int
    ) {
        val incident = BlockIncident(
            matchedWord = matchedWord,
            sourceApp = sourceApp,
            lockDurationMinutes = durationMinutes
        )
        dao.insertIncident(incident)
    }

    suspend fun clearHistory() {
        dao.clearIncidents()
    }
}
