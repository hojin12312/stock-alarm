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
    )
}
