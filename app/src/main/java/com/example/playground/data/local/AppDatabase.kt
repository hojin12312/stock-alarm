package com.example.playground.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WatchlistEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE watchlist ADD COLUMN lastQuantStatus TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE watchlist ADD COLUMN lastRsi2 REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE watchlist ADD COLUMN lastSma200 REAL DEFAULT NULL")
            }
        }
    }
}
