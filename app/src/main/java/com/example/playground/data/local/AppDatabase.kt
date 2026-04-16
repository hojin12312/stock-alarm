package com.example.playground.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WatchlistEntity::class, NotificationEntity::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE watchlist ADD COLUMN lastQuantStatus TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE watchlist ADD COLUMN lastRsi2 REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE watchlist ADD COLUMN lastSma200 REAL DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS notifications (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        symbol TEXT NOT NULL,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        status TEXT NOT NULL,
                        detail TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        read INTEGER NOT NULL DEFAULT 0
                    )"""
                )
            }
        }
    }
}
