package com.example.playground.data.model

data class WatchedStock(
    val symbol: String,
    val name: String,
    val exchange: String,
    val market: Market,
    val lastStatus: MaStatus?,
    val lastMa5: Double?,
    val lastMa20: Double?,
    val lastClose: Double?,
    val lastUpdatedAt: Long?,
    val addedAt: Long,
)
