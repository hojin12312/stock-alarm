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
import com.example.playground.domain.QuantCalculator
import com.example.playground.domain.QuantSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException

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
    }.recoverCatching { error ->
        Log.w(TAG, "search failed: $query", error)
        throw mapSearchError(error)
    }

    private fun mapSearchError(error: Throwable): Throwable {
        val message = when (error) {
            is HttpException -> when (error.code()) {
                429 -> "야후 서버가 일시적으로 요청을 차단했어 (HTTP 429). 잠시 후 다시 시도해줘."
                in 500..599 -> "야후 서버 오류 (HTTP ${error.code()}). 잠시 후 다시 시도해줘."
                else -> "검색 실패 (HTTP ${error.code()}): ${error.message()}"
            }
            is IOException -> "네트워크 연결을 확인해줘 (${error.message ?: error.javaClass.simpleName})"
            else -> "검색 실패: ${error.message ?: error.javaClass.simpleName}"
        }
        return RuntimeException(message, error)
    }

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

    suspend fun refreshSnapshot(symbol: String): RefreshOutcome = runCatching {
        val entity = dao.getAll().firstOrNull { it.symbol == symbol }
            ?: return@runCatching RefreshOutcome.Skipped("not in watchlist")
        val closes = activeDataSource().fetchCloses(
            symbol = symbol,
            market = entity.market,
            exchangeHint = entity.exchange,
        )
        val maSnapshot = MaCalculator.compute(closes)
            ?: return@runCatching RefreshOutcome.Skipped("insufficient data")
        val quantSnapshot = QuantCalculator.compute(closes)
        val close = closes.last()
        dao.updateSnapshot(
            symbol = symbol,
            ma5 = maSnapshot.ma5,
            ma20 = maSnapshot.ma20,
            status = maSnapshot.status,
            close = close,
            updatedAt = System.currentTimeMillis(),
            quantStatus = quantSnapshot?.status,
            rsi2 = quantSnapshot?.rsi2,
            sma200 = quantSnapshot?.sma200,
        )
        RefreshOutcome.Updated(
            symbol = entity.symbol,
            name = entity.name,
            prev = entity.lastStatus,
            current = maSnapshot.status,
            snapshot = maSnapshot,
            close = close,
            prevQuant = entity.lastQuantStatus,
            currentQuant = quantSnapshot?.status,
            quantSnapshot = quantSnapshot,
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
            val prevQuant: MaStatus? = null,
            val currentQuant: MaStatus? = null,
            val quantSnapshot: QuantSnapshot? = null,
        ) : RefreshOutcome {
            val crossed: Boolean get() = prev != null && prev != current
            val quantCrossed: Boolean get() = prevQuant != null && currentQuant != null && prevQuant != currentQuant
        }

        data class Skipped(val reason: String) : RefreshOutcome
    }

    companion object {
        private const val TAG = "StockRepository"
    }
}
