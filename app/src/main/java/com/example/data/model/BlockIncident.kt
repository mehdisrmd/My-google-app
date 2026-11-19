package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "block_incidents")
data class BlockIncident(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val matchedWord: String,
    val sourceApp: String,
    val lockDurationMinutes: Int,
    /**
     * #14: file NAME (not a full path) of this block's screenshot inside
     * the app-private `filesDir/blocklog/` folder — see ScreenshotStore.
     * Storing the bare name keeps the row valid if the app's private
     * directory is ever relocated. Null when no capture exists (API < 30,
     * secure window, system rate limit, or a non-service block).
     */
    val screenshotPath: String? = null,
    /**
     * #14: short human-readable reason category for the block ("Adult
     * site", "Blocked keyword", "Daily app limit reached", ...). Null on
     * rows recorded before this field existed and on in-app manual locks;
     * the detail view falls back to [matchedWord] then.
     */
    val blockReason: String? = null,
    /** #14: the blocked host/URL when the block came from a URL (adult-site path). */
    val blockedUrl: String? = null
)
