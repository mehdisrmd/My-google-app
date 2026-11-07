package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppUsageLimit
import com.example.data.model.BlockIncident
import com.example.data.model.BlockedKeyword
import com.example.data.model.FocusScheduleSlot
import com.example.data.model.SectionBlockRule
import kotlinx.coroutines.flow.Flow

@Dao
interface ShieldDao {
    @Query("SELECT * FROM blocked_keywords ORDER BY id DESC")
    fun getAllKeywords(): Flow<List<BlockedKeyword>>

    @Query("SELECT * FROM blocked_keywords ORDER BY id ASC")
    suspend fun getAllKeywordsSync(): List<BlockedKeyword>

    @Query("SELECT * FROM blocked_keywords WHERE isEnabled = 1")
    fun getEnabledKeywords(): Flow<List<BlockedKeyword>>

    @Query("SELECT * FROM blocked_keywords WHERE isEnabled = 1")
    suspend fun getEnabledKeywordsSync(): List<BlockedKeyword>

    @Query("SELECT COUNT(*) FROM blocked_keywords")
    suspend fun getKeywordCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeyword(keyword: BlockedKeyword): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeywords(keywords: List<BlockedKeyword>)

    @Update
    suspend fun updateKeyword(keyword: BlockedKeyword)

    @Delete
    suspend fun deleteKeyword(keyword: BlockedKeyword)

    @Query("DELETE FROM blocked_keywords WHERE id = :id")
    suspend fun deleteKeywordById(id: Long)

    @Query("SELECT * FROM block_incidents ORDER BY timestamp DESC")
    fun getAllIncidents(): Flow<List<BlockIncident>>

    @Query("SELECT * FROM block_incidents ORDER BY timestamp DESC")
    suspend fun getAllIncidentsSync(): List<BlockIncident>

    @Query("SELECT COUNT(*) FROM block_incidents")
    fun getIncidentCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: BlockIncident): Long

    @Query("DELETE FROM block_incidents")
    suspend fun clearIncidents()

    // App Usage Limits
    @Query("SELECT * FROM app_usage_limits ORDER BY appName ASC")
    fun getAllAppUsageLimits(): Flow<List<AppUsageLimit>>

    @Query("SELECT * FROM app_usage_limits ORDER BY appName ASC")
    suspend fun getAllAppUsageLimitsSync(): List<AppUsageLimit>

    @Query("SELECT * FROM app_usage_limits WHERE isEnabled = 1")
    suspend fun getEnabledAppUsageLimitsSync(): List<AppUsageLimit>

    @Query("SELECT * FROM app_usage_limits WHERE packageName = :pkg LIMIT 1")
    suspend fun getAppUsageLimitByPackage(pkg: String): AppUsageLimit?

    @Query("SELECT COUNT(*) FROM app_usage_limits")
    suspend fun getAppUsageLimitCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppUsageLimit(limit: AppUsageLimit)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppUsageLimits(limits: List<AppUsageLimit>)

    @Update
    suspend fun updateAppUsageLimit(limit: AppUsageLimit)

    @Query("DELETE FROM app_usage_limits WHERE packageName = :pkg")
    suspend fun deleteAppUsageLimit(pkg: String)

    // Section Block Rules
    @Query("SELECT * FROM section_block_rules ORDER BY id ASC")
    fun getAllSectionRules(): Flow<List<SectionBlockRule>>

    @Query("SELECT * FROM section_block_rules ORDER BY id ASC")
    suspend fun getAllSectionRulesSync(): List<SectionBlockRule>

    @Query("SELECT * FROM section_block_rules WHERE isEnabled = 1")
    suspend fun getEnabledSectionRulesSync(): List<SectionBlockRule>

    @Query("SELECT COUNT(*) FROM section_block_rules")
    suspend fun getSectionRuleCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSectionRule(rule: SectionBlockRule): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSectionRules(rules: List<SectionBlockRule>)

    @Update
    suspend fun updateSectionRule(rule: SectionBlockRule)

    @Query("DELETE FROM section_block_rules WHERE id = :id")
    suspend fun deleteSectionRuleById(id: Long)

    // Smart Focus Schedule Slots
    @Query("SELECT * FROM focus_schedule_slots ORDER BY startHour ASC, startMinute ASC")
    fun getAllFocusSlots(): Flow<List<FocusScheduleSlot>>

    @Query("SELECT * FROM focus_schedule_slots ORDER BY startHour ASC, startMinute ASC")
    suspend fun getAllFocusSlotsSync(): List<FocusScheduleSlot>

    @Query("SELECT * FROM focus_schedule_slots WHERE isEnabled = 1")
    suspend fun getEnabledFocusSlotsSync(): List<FocusScheduleSlot>

    @Query("SELECT COUNT(*) FROM focus_schedule_slots")
    suspend fun getFocusSlotCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSlot(slot: FocusScheduleSlot): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSlots(slots: List<FocusScheduleSlot>)

    @Update
    suspend fun updateFocusSlot(slot: FocusScheduleSlot)

    @Query("DELETE FROM focus_schedule_slots WHERE id = :id")
    suspend fun deleteFocusSlotById(id: Long)

    @Query("DELETE FROM blocked_keywords")
    suspend fun clearAllKeywords()

    @Query("DELETE FROM app_usage_limits")
    suspend fun clearAllAppUsageLimits()

    @Query("DELETE FROM section_block_rules")
    suspend fun clearAllSectionRules()

    @Query("DELETE FROM focus_schedule_slots")
    suspend fun clearAllFocusSlots()
}
