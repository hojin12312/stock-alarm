package com.example.playground.data.source

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
        val res = api.search(q = query)
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

    override suspend fun fetchCloses(
        symbol: String,
        market: Market,
        exchangeHint: String?,
    ): List<Double> {
        val res = api.chart(symbol = symbol)
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
}
