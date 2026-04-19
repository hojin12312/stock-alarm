package com.example.playground.ui.chart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.playground.data.model.AlgorithmType
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
    private val algorithmType: AlgorithmType = AlgorithmType.MA_CROSS,
) : ViewModel() {

    private val _state = MutableStateFlow(ChartUiState(loading = true))
    val state: StateFlow<ChartUiState> = _state.asStateFlow()

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
        load(range)
    }

    private fun load(range: String) {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val result = repo.fetchChart(symbol, range)
            _state.value = if (result.isSuccess) {
                _state.value.copy(loading = false, data = result.getOrNull())
            } else {
                _state.value.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.message ?: "차트 로딩 실패",
                )
            }
        }
    }

    class Factory(
        private val repo: StockRepository,
        private val settings: AppSettings,
        private val symbol: String,
        private val algorithmType: AlgorithmType = AlgorithmType.MA_CROSS,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ChartViewModel(repo, settings, symbol, algorithmType) as T
    }
}
