package com.example.playground.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.playground.data.model.MaStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY addedAt ASC")
    fun observeAll(): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist ORDER BY addedAt ASC")
    suspend fun getAll(): List<WatchlistEntity>

    @Query("SELECT symbol FROM watchlist")
    fun observeSymbols(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: WatchlistEntity): Long

    @Query("DELETE FROM watchlist WHERE symbol = :symbol")
    suspend fun delete(symbol: String)

    @Query(
        """
        UPDATE watchlist
        SET lastStatus = :status,
            lastMa5 = :ma5,
            lastMa20 = :ma20,
            lastClose = :close,
            lastUpdatedAt = :updatedAt,
            lastQuantStatus = :quantStatus,
            lastRsi2 = :rsi2,
            lastSma200 = :sma200,
            prevPrevMa5 = :prevPrevMa5
        WHERE symbol = :symbol
        """
    )
    suspend fun updateSnapshot(
        symbol: String,
        ma5: Double,
        ma20: Double,
        status: MaStatus,
        close: Double,
        updatedAt: Long,
        quantStatus: MaStatus? = null,
        rsi2: Double? = null,
        sma200: Double? = null,
        prevPrevMa5: Double? = null,
    )

    @Query("UPDATE watchlist SET lastExtremaNotifyDate = :date WHERE symbol = :symbol")
    suspend fun markExtremaNotified(symbol: String, date: String)
}
