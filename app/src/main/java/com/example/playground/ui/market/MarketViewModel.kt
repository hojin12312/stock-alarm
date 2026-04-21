package com.example.playground.ui.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.playground.data.model.MarketIndex
import com.example.playground.data.repo.StockRepository
import com.example.playground.util.formatNumber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.util.Locale

data class MarketCardState(
    val index: MarketIndex,
    val isLoading: Boolean = true,
    val price: String? = null,
    val changePercent: String? = null,
    val isPositive: Boolean? = null,
    val error: String? = null,
)

class MarketViewModel(private val repo: StockRepository) : ViewModel() {

    private val _states = MutableStateFlow(MarketIndex.values().map { MarketCardState(it) })
    val states: StateFlow<List<MarketCardState>> = _states.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadAll()
    }

    fun refresh() {
        _isRefreshing.value = true
        _states.value = MarketIndex.values().map { MarketCardState(it, isLoading = true) }
        loadAll()
    }

    private fun loadAll() {
        viewModelScope.launch {
            val jobs = MarketIndex.values().map { index -> launch { loadIndex(index) } }
            jobs.joinAll()
            _isRefreshing.value = false
        }
    }

    private suspend fun loadIndex(index: MarketIndex) {
        val result = repo.fetchChartDirect(index.symbol, index.displayName, "5d")
        val newState = if (result.isSuccess) {
            val closes = result.getOrNull()!!.closes
            when {
                closes.size >= 2 -> {
                    val price = closes.last()
                    val prev = closes[closes.size - 2]
                    val change = (price - prev) / prev * 100.0
                    val sign = if (change >= 0) "+" else ""
                    MarketCardState(
                        index = index,
                        isLoading = false,
                        price = formatPrice(index, price),
                        changePercent = "$sign${"%.2f".format(change)}%",
                        isPositive = change >= 0,
                    )
                }
                closes.size == 1 -> MarketCardState(
                    index = index,
                    isLoading = false,
                    price = formatPrice(index, closes.last()),
                )
                else -> MarketCardState(index = index, isLoading = false, error = "데이터 없음")
            }
        } else {
            MarketCardState(index = index, isLoading = false, error = "로딩 실패")
        }
        _states.value = _states.value.map { if (it.index == index) newState else it }
    }

    private fun formatPrice(index: MarketIndex, price: Double): String =
        if (index == MarketIndex.USDKRW) String.format(Locale.US, "%,.2f", price)
        else formatNumber(price)

    class Factory(private val repo: StockRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MarketViewModel(repo) as T
    }
}
