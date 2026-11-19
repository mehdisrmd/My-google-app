package com.example.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * #14: the v3 → v4 migration must be a REAL migration — the block log's
 * new columns are added in place and existing incident rows survive with
 * NULLs, instead of the destructive fallback wiping the user's history.
 */
@RunWith(RobolectricTestRunner::class)
class ShieldDatabaseMigrationTest {

    /** Opens a database containing only the v3 block_incidents table. */
    private fun openV3Database(name: String): SupportSQLiteDatabase {
        val config = SupportSQLiteOpenHelper.Configuration
            .builder(RuntimeEnvironment.getApplication())
            .name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `block_incidents` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`timestamp` INTEGER NOT NULL, " +
                            "`matchedWord` TEXT NOT NULL, " +
                            "`sourceApp` TEXT NOT NULL, " +
                            "`lockDurationMinutes` INTEGER NOT NULL)"
                    )
                }

                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) = Unit
            })
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase
    }

    @Test
    fun `migration 3 to 4 adds the block log columns and keeps existing rows`() {
        val db = openV3Database("migration_3_4_test.db")
        db.execSQL(
            "INSERT INTO block_incidents (timestamp, matchedWord, sourceApp, lockDurationMinutes) " +
                "VALUES (1700000000000, 'badword', 'Chrome', 5)"
        )

        ShieldDatabase.MIGRATION_3_4.migrate(db)

        // The pre-existing row survives, with NULLs in the new columns.
        db.query(
            "SELECT matchedWord, screenshotPath, blockReason, blockedUrl FROM block_incidents"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("badword", cursor.getString(0))
            assertTrue(cursor.isNull(1))
            assertTrue(cursor.isNull(2))
            assertTrue(cursor.isNull(3))
        }

        // And new rows can carry the block-log fields.
        db.execSQL(
            "INSERT INTO block_incidents " +
                "(timestamp, matchedWord, sourceApp, lockDurationMinutes, " +
                "screenshotPath, blockReason, blockedUrl) VALUES " +
                "(1700000001000, 'Adult Site Blocked: example.com', 'Chrome', 5, " +
                "'block_1700000001000_1.jpg', 'Adult site', 'example.com')"
        )
        db.query("SELECT COUNT(*) FROM block_incidents").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(2, cursor.getInt(0))
        }
        db.close()
    }
}
