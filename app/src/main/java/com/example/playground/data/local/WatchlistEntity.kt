package com.example.playground.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.playground.data.model.MaStatus
import com.example.playground.data.model.Market
import com.example.playground.data.model.WatchedStock

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val symbol: String,
    val name: String,
    val exchange: String,
    val market: Market,
    val lastStatus: MaStatus?,
    val lastMa5: Double?,
    val lastMa20: Double?,
    val lastClose: Double?,
    val lastUpdatedAt: Long?,
    val addedAt: Long,
    val lastQuantStatus: MaStatus? = null,
    val lastRsi2: Double? = null,
    val lastSma200: Double? = null,
    val prevPrevMa5: Double? = null,
    val lastExtremaNotifyDate: String? = null,
) {
    fun toDomain(): WatchedStock = WatchedStock(
        symbol = symbol,
        name = name,
        exchange = exchange,
        market = market,
        lastStatus = lastStatus,
        lastMa5 = lastMa5,
        lastMa20 = lastMa20,
        lastClose = lastClose,
        lastUpdatedAt = lastUpdatedAt,
        addedAt = addedAt,
        lastQuantStatus = lastQuantStatus,
        lastRsi2 = lastRsi2,
        lastSma200 = lastSma200,
    )
}
