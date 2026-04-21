package com.example.playground.ui.chart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.playground.data.model.ChartData
import com.example.playground.data.prefs.AppSettings
import com.example.playground.data.repo.StockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChartUiState(
    val loading: Boolean = false,
    val data: ChartData? = null,
    val range: String = AppSettings.DEFAULT_CHART_RANGE,
    val error: String? = null,
)

class ChartViewModel(
    private val repo: StockRepository,
    private val settings: AppSettings,
    private val symbol: String,
) : ViewModel() {

    private val _state = MutableStateFlow(ChartUiState(loading = true))
    val state: StateFlow<ChartUiState> = _state.asStateFlow()

    // 항상 5y 전체 데이터를 캐시 — range 전환 시 네트워크 없이 displayCount만 교체
    private var fullData: ChartData? = null

    init {
        viewModelScope.launch {
            val saved = settings.currentChartRange()
            _state.value = _state.value.copy(range = saved)
            load(saved)
        }
    }

    fun selectRange(range: String) {
        if (range == _state.value.range) return
        _state.value = _state.value.copy(range = range)
        viewModelScope.launch { settings.setChartRange(range) }
        val cached = fullData
        if (cached != null) {
            // 이미 로딩된 전체 데이터에서 displayCount만 업데이트 (네트워크 없음)
            _state.value = _state.value.copy(
                data = cached.copy(displayCount = calcDisplayCount(cached.timestamps, range))
            )
        } else {
            load(range)
        }
    }

    private fun load(range: String) {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            // 항상 5y를 fetch — RSI/SMA200 계산에 충분한 과거 데이터 확보
            val result = repo.fetchChart(symbol, "5y")
            _state.value = if (result.isSuccess) {
                val data = result.getOrNull()!!
                fullData = data
                _state.value.copy(
                    loading = false,
                    data = data.copy(displayCount = calcDisplayCount(data.timestamps, range)),
                )
            } else {
                _state.value.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.message ?: "차트 로딩 실패",
                )
            }
        }
    }

    // timestamps(초 단위 epoch)에서 range 기간에 해당하는 데이터 개수를 계산
    private fun calcDisplayCount(timestamps: List<Long>, range: String): Int {
        val nowSec = System.currentTimeMillis() / 1000L
        val cutoff = when (range) {
            "1mo" -> nowSec - 30L  * 24 * 3600
            "3mo" -> nowSec - 90L  * 24 * 3600
            "6mo" -> nowSec - 180L * 24 * 3600
            "1y"  -> nowSec - 365L * 24 * 3600
            "2y"  -> nowSec - 730L * 24 * 3600
            else  -> return timestamps.size  // "5y" = 전체
        }
        return timestamps.count { it >= cutoff }.coerceAtLeast(1)
    }

    class Factory(
        private val repo: StockRepository,
        private val settings: AppSettings,
        private val symbol: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ChartViewModel(repo, settings, symbol) as T
    }
}
