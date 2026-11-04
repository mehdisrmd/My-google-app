package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_keywords")
data class BlockedKeyword(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val category: BlockedCategory,
    val isEnabled: Boolean = true,
    val isRegex: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis()
)
