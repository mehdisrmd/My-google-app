package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.AppUsageLimit
import com.example.data.model.BlockIncident
import com.example.data.model.BlockedKeyword
import com.example.data.model.FocusScheduleSlot
import com.example.data.model.SectionBlockRule

@Database(
    entities = [
        BlockedKeyword::class,
        BlockIncident::class,
        AppUsageLimit::class,
        SectionBlockRule::class,
        FocusScheduleSlot::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ShieldDatabase : RoomDatabase() {
    abstract fun shieldDao(): ShieldDao

    companion object {
        @Volatile
        private var INSTANCE: ShieldDatabase? = null

        fun getDatabase(context: Context): ShieldDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShieldDatabase::class.java,
                    "shield_block.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
