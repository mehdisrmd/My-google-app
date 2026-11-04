package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "section_block_rules")
data class SectionBlockRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val targetPackage: String,
    val sectionName: String,
    val triggerPattern: String,
    val viewIdPattern: String = "",
    val isEnabled: Boolean = true
)
