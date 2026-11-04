package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.BlockedCategory

class Converters {
    @TypeConverter
    fun fromCategory(category: BlockedCategory): String {
        return category.name
    }

    @TypeConverter
    fun toCategory(value: String): BlockedCategory {
        return try {
            BlockedCategory.valueOf(value)
        } catch (e: Exception) {
            BlockedCategory.CUSTOM_KEYWORD
        }
    }
}
