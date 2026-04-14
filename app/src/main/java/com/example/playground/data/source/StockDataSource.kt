package com.example.playground.data.source

import com.example.playground.data.model.ChartData
import com.example.playground.data.model.Market

/**
 * 시세·차트 조회용 추상 데이터 소스.
 * 검색은 Yahoo Finance 고정(증권사 Open API 대부분이 종목 검색을 제공하지 않음).
 */
interface StockDataSource {
    val id: DataSourceId

    suspend fun fetchCloses(symbol: String, market: Market, exchangeHint: String?): List<Double>

    suspend fun fetchChart(
        symbol: String,
        market: Market,
        exchangeHint: String?,
        name: String,
        range: String,
    ): ChartData
}

enum class DataSourceId { YAHOO, KIS }
