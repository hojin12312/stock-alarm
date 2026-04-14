package com.example.playground.data.repo

import android.util.Log
import com.example.playground.data.local.WatchlistDao
import com.example.playground.data.local.WatchlistEntity
import com.example.playground.data.model.ChartData
import com.example.playground.data.model.MaStatus
import com.example.playground.data.model.Market
import com.example.playground.data.model.StockSearchResult
import com.example.playground.data.model.WatchedStock
import com.example.playground.data.remote.YahooFinanceApi
import com.example.playground.domain.MaCalculator
import com.example.playground.domain.MaSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StockRepository(
    private val dao: WatchlistDao,
    private val api: YahooFinanceApi,
) {
    fun observeWatchlist(): Flow<List<WatchedStock>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeWatchedSymbols(): Flow<List<String>> = dao.observeSymbols()

    suspend fun addToWatchlist(result: StockSearchResult) {
        dao.insert(
            WatchlistEntity(
                symbol = result.symbol,
                name = result.name,
                exchange = result.exchange,
                market = result.market,
                lastStatus = null,
                lastMa5 = null,
                lastMa20 = null,
                lastClose = null,
                lastUpdatedAt = null,
                addedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun removeFromWatchlist(symbol: String) {
        dao.delete(symbol)
    }

    suspend fun search(query: String): Result<List<StockSearchResult>> = runCatching {
        if (query.isBlank()) return@runCatching emptyList()
        val res = api.search(q = query)
        res.quotes.mapNotNull { dto ->
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
    }.onFailure { Log.w(TAG, "search failed: $query", it) }

    suspend fun fetchCloses(symbol: String): List<Double> {
        val res = api.chart(symbol = symbol)
        val result = res.chart.result?.firstOrNull() ?: return emptyList()
        val raw = result.indicators?.quote?.firstOrNull()?.close ?: return emptyList()
        return raw.filterNotNull()
    }

    suspend fun fetchChart(symbol: String, range: String = "3mo"): Result<ChartData> = runCatching {
        val res = api.chart(symbol = symbol, range = range)
        val result = res.chart.result?.firstOrNull()
            ?: error("차트 응답이 비어 있어")
        val rawCloses = result.indicators?.quote?.firstOrNull()?.close
            ?: error("종가 데이터가 없어")
        val rawTimestamps = result.timestamp
        // null 종가가 섞여있는 인덱스는 건너뛴다 — timestamp도 같은 인덱스만 남김.
        val pairs = rawCloses.mapIndexed { i, c -> i to c }
            .mapNotNull { (i, c) -> if (c != null && i < rawTimestamps.size) rawTimestamps[i] to c else null }
        val timestamps = pairs.map { it.first }
        val closes = pairs.map { it.second }
        val name = dao.getAll().firstOrNull { it.symbol == symbol }?.name ?: symbol
        ChartData(
            symbol = symbol,
            name = name,
            timestamps = timestamps,
            closes = closes,
            ma5Series = MaCalculator.movingAverageSeries(closes, 5),
            ma20Series = MaCalculator.movingAverageSeries(closes, 20),
        )
    }.onFailure { Log.w(TAG, "fetchChart($symbol) failed", it) }

    /**
     * 최신 종가로 이평선 계산 후 DB 업데이트. 교차 발생 여부를 반환한다.
     * @return Pair(이전 상태, 새 상태) — 이전 상태가 null이면 최초 계산(알림 없음)
     */
    suspend fun refreshSnapshot(
        symbol: String,
    ): RefreshOutcome = runCatching {
        val entity = dao.getAll().firstOrNull { it.symbol == symbol }
            ?: return@runCatching RefreshOutcome.Skipped("not in watchlist")
        val closes = fetchCloses(symbol)
        val snapshot = MaCalculator.compute(closes)
            ?: return@runCatching RefreshOutcome.Skipped("insufficient data")
        val close = closes.last()
        dao.updateSnapshot(
            symbol = symbol,
            ma5 = snapshot.ma5,
            ma20 = snapshot.ma20,
            status = snapshot.status,
            close = close,
            updatedAt = System.currentTimeMillis(),
        )
        RefreshOutcome.Updated(
            symbol = entity.symbol,
            name = entity.name,
            prev = entity.lastStatus,
            current = snapshot.status,
            snapshot = snapshot,
            close = close,
        )
    }.getOrElse {
        Log.w(TAG, "refreshSnapshot($symbol) failed", it)
        RefreshOutcome.Skipped(it.message ?: "error")
    }

    suspend fun refreshAll(): List<RefreshOutcome.Updated> {
        val all = dao.getAll()
        return all.mapNotNull { entity ->
            val outcome = refreshSnapshot(entity.symbol)
            (outcome as? RefreshOutcome.Updated)
        }
    }

    private fun resolveMarket(symbol: String): Market {
        val suffix = symbol.substringAfterLast('.', "")
        return when (suffix.uppercase()) {
            "KS", "KQ" -> Market.KR
            else -> Market.US
        }
    }

    sealed interface RefreshOutcome {
        data class Updated(
            val symbol: String,
            val name: String,
            val prev: MaStatus?,
            val current: MaStatus,
            val snapshot: MaSnapshot,
            val close: Double,
        ) : RefreshOutcome {
            val crossed: Boolean get() = prev != null && prev != current
        }

        data class Skipped(val reason: String) : RefreshOutcome
    }

    companion object {
        private const val TAG = "StockRepository"
    }
}
