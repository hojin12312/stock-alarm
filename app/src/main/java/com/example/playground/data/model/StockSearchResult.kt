package com.example.playground.data.model

data class StockSearchResult(
    val symbol: String,
    val name: String,
    val exchange: String,
    val market: Market,
)
