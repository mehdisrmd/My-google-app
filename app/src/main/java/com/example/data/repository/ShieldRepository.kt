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
import org.json.JSONArray
import org.json.JSONObject

data class BackupRestoreSummary(
    val success: Boolean,
    val keywordsRestored: Int = 0,
    val appLimitsRestored: Int = 0,
    val sectionRulesRestored: Int = 0,
    val focusSlotsRestored: Int = 0,
    val messageEn: String = "",
    val messageFa: String = ""
)

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
            removePredefinedExampleLimitsIfAny()
        }
    }

    private suspend fun removePredefinedExampleLimitsIfAny() {
        val all = dao.getEnabledAppUsageLimitsSync()
        for (limit in all) {
            if (limit.lastModifiedTimestamp == 0L) {
                dao.deleteAppUsageLimit(limit.packageName)
            }
        }
    }

    suspend fun seedDefaultKeywordsIfNeeded() {
        val existingPairs = dao.getAllKeywordsSync()
            .map { it.text to it.category }
            .toSet()
        val missing = DefaultKeywords.getPreloadedKeywords()
            .filter { (it.text to it.category) !in existingPairs }
        if (missing.isNotEmpty()) {
            dao.insertKeywords(missing)
        }
    }

    suspend fun seedDefaultSectionRulesIfNeeded() {
        if (dao.getSectionRuleCount() == 0) {
            dao.insertSectionRules(DefaultSectionRules.getDefaultRules())
            preferences.consumeSectionRulesOffMigration()
        } else if (preferences.consumeSectionRulesOffMigration()) {
            val existing = dao.getAllSectionRulesSync()
            for (rule in existing) {
                if (rule.isEnabled) {
                    dao.insertSectionRule(rule.copy(isEnabled = false))
                }
            }
        }
    }

    suspend fun resetEntireAppFromScratch() {
        preferences.resetAllPreferencesToFreshScratch()
        dao.clearIncidents()
        dao.clearAllKeywords()
        dao.clearAllAppUsageLimits()
        dao.clearAllSectionRules()
        dao.clearAllFocusSlots()

        seedDefaultKeywordsIfNeeded()
        seedDefaultSectionRulesIfNeeded()
    }

    /**
     * Exports all user information (Preferences, Clean Streak, Urges Defeated, Personal Pledge,
     * Keywords/Domains, App Usage Limits, Section Block Rules, and Smart Focus Slots) to JSON.
     */
    suspend fun exportFullBackupJson(): String {
        val root = JSONObject()
        root.put("backupFormatVersion", 1)
        root.put("appName", "X Blocker")
        root.put("exportedAtEpochMs", System.currentTimeMillis())
        root.put("preferences", preferences.exportPreferencesToJson())

        val keywordsArr = JSONArray()
        for (kw in dao.getAllKeywordsSync()) {
            keywordsArr.put(
                JSONObject().apply {
                    put("text", kw.text)
                    put("category", kw.category.name)
                    put("isEnabled", kw.isEnabled)
                    put("isRegex", kw.isRegex)
                }
            )
        }
        root.put("keywords", keywordsArr)

        val limitsArr = JSONArray()
        for (limit in dao.getAllAppUsageLimitsSync()) {
            limitsArr.put(
                JSONObject().apply {
                    put("packageName", limit.packageName)
                    put("appName", limit.appName)
                    put("dailyLimitMinutes", limit.dailyLimitMinutes)
                    put("isEnabled", limit.isEnabled)
                    put("lastModifiedTimestamp", limit.lastModifiedTimestamp)
                }
            )
        }
        root.put("appLimits", limitsArr)

        val sectionsArr = JSONArray()
        for (rule in dao.getAllSectionRulesSync()) {
            sectionsArr.put(
                JSONObject().apply {
                    put("targetPackage", rule.targetPackage)
                    put("sectionName", rule.sectionName)
                    put("triggerPattern", rule.triggerPattern)
                    put("viewIdPattern", rule.viewIdPattern)
                    put("isEnabled", rule.isEnabled)
                }
            )
        }
        root.put("sectionRules", sectionsArr)

        val focusArr = JSONArray()
        for (slot in dao.getAllFocusSlotsSync()) {
            focusArr.put(
                JSONObject().apply {
                    put("title", slot.title)
                    put("startHour", slot.startHour)
                    put("startMinute", slot.startMinute)
                    put("endHour", slot.endHour)
                    put("endMinute", slot.endMinute)
                    put("daysOfWeekCsv", slot.daysOfWeekCsv)
                    put("isEnabled", slot.isEnabled)
                }
            )
        }
        root.put("focusSlots", focusArr)

        return root.toString(2)
    }

    /**
     * Restores all user information from a JSON backup string.
     */
    suspend fun importFullBackupJson(rawJson: String): BackupRestoreSummary {
        return try {
            val root = JSONObject(rawJson.trim())
            val prefsObj = root.optJSONObject("preferences")
            if (prefsObj != null) {
                preferences.importPreferencesFromJson(prefsObj)
            }

            var kwCount = 0
            val keywordsArr = root.optJSONArray("keywords")
            if (keywordsArr != null && keywordsArr.length() > 0) {
                val restoredKeywords = mutableListOf<BlockedKeyword>()
                for (i in 0 until keywordsArr.length()) {
                    val item = keywordsArr.optJSONObject(i) ?: continue
                    val text = item.optString("text", "").trim()
                    if (text.isEmpty()) continue
                    val catName = item.optString("category", BlockedCategory.CUSTOM_KEYWORD.name)
                    val category = runCatching { BlockedCategory.valueOf(catName) }
                        .getOrDefault(BlockedCategory.CUSTOM_KEYWORD)
                    restoredKeywords.add(
                        BlockedKeyword(
                            text = text,
                            category = category,
                            isEnabled = item.optBoolean("isEnabled", true),
                            isRegex = item.optBoolean("isRegex", false)
                        )
                    )
                }
                if (restoredKeywords.isNotEmpty()) {
                    dao.clearAllKeywords()
                    dao.insertKeywords(restoredKeywords)
                    kwCount = restoredKeywords.size
                }
            }

            var limitCount = 0
            val limitsArr = root.optJSONArray("appLimits")
            if (limitsArr != null) {
                val restoredLimits = mutableListOf<AppUsageLimit>()
                for (i in 0 until limitsArr.length()) {
                    val item = limitsArr.optJSONObject(i) ?: continue
                    val pkg = item.optString("packageName", "").trim()
                    val name = item.optString("appName", "").trim()
                    if (pkg.isEmpty() || name.isEmpty()) continue
                    restoredLimits.add(
                        AppUsageLimit(
                            packageName = pkg,
                            appName = name,
                            dailyLimitMinutes = item.optInt("dailyLimitMinutes", 30).coerceIn(1, 1440),
                            isEnabled = item.optBoolean("isEnabled", true),
                            lastModifiedTimestamp = item.optLong("lastModifiedTimestamp", System.currentTimeMillis())
                        )
                    )
                }
                dao.clearAllAppUsageLimits()
                if (restoredLimits.isNotEmpty()) {
                    dao.insertAppUsageLimits(restoredLimits)
                    limitCount = restoredLimits.size
                }
            }

            var sectionCount = 0
            val sectionsArr = root.optJSONArray("sectionRules")
            if (sectionsArr != null && sectionsArr.length() > 0) {
                val restoredSections = mutableListOf<SectionBlockRule>()
                for (i in 0 until sectionsArr.length()) {
                    val item = sectionsArr.optJSONObject(i) ?: continue
                    val targetPkg = item.optString("targetPackage", "").trim()
                    val secName = item.optString("sectionName", "").trim()
                    val trig = item.optString("triggerPattern", "").trim()
                    if (targetPkg.isEmpty() || secName.isEmpty() || trig.isEmpty()) continue
                    restoredSections.add(
                        SectionBlockRule(
                            targetPackage = targetPkg,
                            sectionName = secName,
                            triggerPattern = trig,
                            viewIdPattern = item.optString("viewIdPattern", ""),
                            isEnabled = item.optBoolean("isEnabled", true)
                        )
                    )
                }
                if (restoredSections.isNotEmpty()) {
                    dao.clearAllSectionRules()
                    dao.insertSectionRules(restoredSections)
                    sectionCount = restoredSections.size
                }
            }

            var focusCount = 0
            val focusArr = root.optJSONArray("focusSlots")
            if (focusArr != null) {
                val restoredFocus = mutableListOf<FocusScheduleSlot>()
                for (i in 0 until focusArr.length()) {
                    val item = focusArr.optJSONObject(i) ?: continue
                    val title = item.optString("title", "").trim()
                    if (title.isEmpty()) continue
                    restoredFocus.add(
                        FocusScheduleSlot(
                            title = title,
                            startHour = item.optInt("startHour", 9).coerceIn(0, 23),
                            startMinute = item.optInt("startMinute", 0).coerceIn(0, 59),
                            endHour = item.optInt("endHour", 11).coerceIn(0, 23),
                            endMinute = item.optInt("endMinute", 0).coerceIn(0, 59),
                            daysOfWeekCsv = item.optString("daysOfWeekCsv", "1,2,3,4,5,6,7"),
                            isEnabled = item.optBoolean("isEnabled", true)
                        )
                    )
                }
                dao.clearAllFocusSlots()
                if (restoredFocus.isNotEmpty()) {
                    dao.insertFocusSlots(restoredFocus)
                    focusCount = restoredFocus.size
                }
            }

            BackupRestoreSummary(
                success = true,
                keywordsRestored = kwCount,
                appLimitsRestored = limitCount,
                sectionRulesRestored = sectionCount,
                focusSlotsRestored = focusCount,
                messageEn = "Backup restored! ($kwCount rules, $limitCount app limits, $sectionCount section guards, $focusCount focus slots & streak data)",
                messageFa = "پشتیبان با موفقیت بازیابی شد! ($kwCount قانون، $limitCount محدودیت برنامه، $sectionCount محافظ بخش و اطلاعات روزهای پاک)"
            )
        } catch (e: Exception) {
            BackupRestoreSummary(
                success = false,
                messageEn = "Invalid backup JSON format: ${e.localizedMessage ?: "Parse error"}",
                messageFa = "فرمت فایل پشتیبان نامعتبر است. لطفاً یک فایل یا متن JSON معتبر انتخاب کنید."
            )
        }
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

    suspend fun saveSectionRule(rule: SectionBlockRule) {
        dao.insertSectionRule(rule)
    }

    suspend fun toggleSectionRule(rule: SectionBlockRule) {
        dao.updateSectionRule(rule.copy(isEnabled = !rule.isEnabled))
    }

    suspend fun deleteSectionRule(id: Long) {
        dao.deleteSectionRuleById(id)
    }

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
