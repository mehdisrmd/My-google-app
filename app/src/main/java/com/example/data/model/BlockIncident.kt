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
    val lockDurationMinutes: Int
)
