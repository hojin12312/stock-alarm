package com.example.playground.data.source

import android.util.Log
import com.example.playground.data.model.ChartData
import com.example.playground.data.model.Market
import com.example.playground.data.model.StockSearchResult
import com.example.playground.data.remote.YahooFinanceApi
import com.example.playground.domain.MaCalculator

class YahooFinanceDataSource(
    private val api: YahooFinanceApi,
) : StockDataSource {

    override val id: DataSourceId = DataSourceId.YAHOO

    suspend fun search(query: String): List<StockSearchResult> {
        if (query.isBlank()) return emptyList()
        val res = searchWithFallback(query)
        return res.quotes.mapNotNull { dto ->
            val symbol = dto.symbol ?: return@mapNotNull null
            val type = dto.quoteType?.uppercase()
            if (type != null && type != "EQUITY" && type != "ETF") return@mapNotNull null
            StockSearchResult(
                symbol = symbol,
                name = dto.longname ?: dto.shortname ?: symbol,
                exchange = dto.exchangeDisplay ?: dto.exchange ?: "",
                market = resolveMarket(symbol),
            )
        }
    }

    private suspend fun searchWithFallback(query: String): com.example.playground.data.remote.dto.SearchResponseDto {
        var lastError: Throwable? = null
        for (host in SEARCH_HOSTS) {
            try {
                return api.search(url = "https://$host/v1/finance/search", q = query)
            } catch (t: Throwable) {
                Log.w(TAG, "search via $host failed: ${t.message}")
                lastError = t
            }
        }
        throw lastError ?: IllegalStateException("검색 실패 (호스트 목록 비어 있음)")
    }

    override suspend fun fetchCloses(
        symbol: String,
        market: Market,
        exchangeHint: String?,
    ): List<Double> {
        val res = api.chart(symbol = symbol, range = "1y")
        val result = res.chart.result?.firstOrNull() ?: return emptyList()
        val raw = result.indicators?.quote?.firstOrNull()?.close ?: return emptyList()
        return raw.filterNotNull()
    }

    override suspend fun fetchChart(
        symbol: String,
        market: Market,
        exchangeHint: String?,
        name: String,
        range: String,
    ): ChartData {
        val res = api.chart(symbol = symbol, range = range)
        val result = res.chart.result?.firstOrNull() ?: error("차트 응답이 비어 있어")
        val rawCloses = result.indicators?.quote?.firstOrNull()?.close
            ?: error("종가 데이터가 없어")
        val rawTimestamps = result.timestamp
        val pairs = rawCloses.mapIndexed { i, c -> i to c }
            .mapNotNull { (i, c) -> if (c != null && i < rawTimestamps.size) rawTimestamps[i] to c else null }
        val timestamps = pairs.map { it.first }
        val closes = pairs.map { it.second }
        return ChartData(
            symbol = symbol,
            name = name,
            timestamps = timestamps,
            closes = closes,
            ma5Series = MaCalculator.movingAverageSeries(closes, 5),
            ma20Series = MaCalculator.movingAverageSeries(closes, 20),
        )
    }

    private fun resolveMarket(symbol: String): Market {
        val suffix = symbol.substringAfterLast('.', "")
        return when (suffix.uppercase()) {
            "KS", "KQ" -> Market.KR
            else -> Market.US
        }
    }

    companion object {
        private const val TAG = "YahooFinanceDS"
        private val SEARCH_HOSTS = listOf(
            "query1.finance.yahoo.com",
            "query2.finance.yahoo.com",
        )
    }
}
