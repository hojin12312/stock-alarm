package com.example.playground.data.repo

import android.util.Log
import com.example.playground.data.local.WatchlistDao
import com.example.playground.data.local.WatchlistEntity
import com.example.playground.data.model.ChartData
import com.example.playground.data.model.MaStatus
import com.example.playground.data.model.StockSearchResult
import com.example.playground.data.model.WatchedStock
import com.example.playground.data.source.StockDataSource
import com.example.playground.data.source.YahooFinanceDataSource
import com.example.playground.domain.MaCalculator
import com.example.playground.domain.MaSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StockRepository(
    private val dao: WatchlistDao,
    private val searchSource: YahooFinanceDataSource,
    private val activeDataSource: suspend () -> StockDataSource,
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

    /** 검색은 항상 Yahoo Finance 고정(KIS·기타 증권사는 종목 검색 API를 제공하지 않음). */
    suspend fun search(query: String): Result<List<StockSearchResult>> = runCatching {
        searchSource.search(query)
    }.onFailure { Log.w(TAG, "search failed: $query", it) }

    suspend fun fetchCloses(symbol: String): List<Double> {
        val entity = dao.getAll().firstOrNull { it.symbol == symbol } ?: return emptyList()
        return activeDataSource().fetchCloses(
            symbol = symbol,
            market = entity.market,
            exchangeHint = entity.exchange,
        )
    }

    suspend fun fetchChart(symbol: String, range: String = "3mo"): Result<ChartData> = runCatching {
        val entity = dao.getAll().firstOrNull { it.symbol == symbol }
            ?: error("관심목록에 없는 종목이야")
        activeDataSource().fetchChart(
            symbol = symbol,
            market = entity.market,
            exchangeHint = entity.exchange,
            name = entity.name,
            range = range,
        )
    }.onFailure { Log.w(TAG, "fetchChart($symbol) failed", it) }

    /**
     * 최신 종가로 이평선 계산 후 DB 업데이트. 교차 발생 여부를 반환한다.
     * @return Pair(이전 상태, 새 상태) — 이전 상태가 null이면 최초 계산(알림 없음)
     */
    suspend fun refreshSnapshot(symbol: String): RefreshOutcome = runCatching {
        val entity = dao.getAll().firstOrNull { it.symbol == symbol }
            ?: return@runCatching RefreshOutcome.Skipped("not in watchlist")
        val closes = activeDataSource().fetchCloses(
            symbol = symbol,
            market = entity.market,
            exchangeHint = entity.exchange,
        )
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
