package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ShieldDatabase : RoomDatabase() {
    abstract fun shieldDao(): ShieldDao

    companion object {
        /**
         * v3 → v4 (#14): block incidents gain a screenshot file name, a
         * block-reason label, and the blocked URL. All three are nullable
         * additions, so existing rows survive untouched with NULLs — this
         * is a REAL migration, not the destructive fallback below, so the
         * user's incident history and rules are never wiped by it.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE block_incidents ADD COLUMN screenshotPath TEXT")
                db.execSQL("ALTER TABLE block_incidents ADD COLUMN blockReason TEXT")
                db.execSQL("ALTER TABLE block_incidents ADD COLUMN blockedUrl TEXT")
            }
        }

        @Volatile
        private var INSTANCE: ShieldDatabase? = null

        fun getDatabase(context: Context): ShieldDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShieldDatabase::class.java,
                    "shield_block.db"
                )
                    .addMigrations(MIGRATION_3_4)
                    // Last-resort only: any version jump WITHOUT a written
                    // migration still rebuilds instead of crashing. The
                    // 3 → 4 path above always takes the real migration.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
