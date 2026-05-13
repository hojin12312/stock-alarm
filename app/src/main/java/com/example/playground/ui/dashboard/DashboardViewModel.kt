package com.example.playground.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.playground.data.model.AlgorithmType
import com.example.playground.data.model.MaStatus
import com.example.playground.data.model.Market
import com.example.playground.data.model.WatchedStock
import com.example.playground.data.model.isOpenNow
import com.example.playground.data.model.todayLocalDate
import com.example.playground.data.prefs.AppSettings
import com.example.playground.data.repo.StockRepository
import com.example.playground.notification.Notifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class DashboardUiState(
    val items: List<WatchedStock> = emptyList(),
    val selectedAlgorithms: Set<AlgorithmType> = setOf(AlgorithmType.MA_CROSS),
    val statusFilter: MaStatus? = null,
    val marketFilter: Market? = null,
    val textFilter: String = "",
    val refreshing: Boolean = false,
    val lastRunAt: Long? = null,
) {
    val filtered: List<WatchedStock>
        get() {
            val text = textFilter.trim().lowercase()
            return items.filter { item ->
                val passesAlgo = when (selectedAlgorithms.size) {
                    1 -> {
                        val status = when (selectedAlgorithms.first()) {
                            AlgorithmType.MA_CROSS -> item.lastStatus
                            AlgorithmType.RSI_SMA200 -> item.lastQuantStatus
                        }
                        statusFilter == null || status == statusFilter
                    }
                    else -> {
                        val ma = item.lastStatus
                        val rsi = item.lastQuantStatus
                        ma != null && rsi != null && ma == rsi &&
                            (statusFilter == null || ma == statusFilter)
                    }
                }
                val marketOk = marketFilter == null || item.market == marketFilter
                val textOk = text.isEmpty() ||
                    item.name.lowercase().contains(text) ||
                    item.symbol.lowercase().contains(text)
                passesAlgo && marketOk && textOk
            }
        }
}

class DashboardViewModel(
    private val repo: StockRepository,
    private val notifier: Notifier,
    private val settings: AppSettings,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        repo.observeWatchlist()
            .onEach { items -> _state.value = _state.value.copy(items = items) }
            .launchIn(viewModelScope)
    }

    fun toggleAlgorithm(type: AlgorithmType) {
        val current = _state.value.selectedAlgorithms
        val new = if (type in current) current - type else current + type
        if (new.isEmpty()) return
        _state.value = _state.value.copy(selectedAlgorithms = new)
    }

    fun setStatusFilter(filter: MaStatus?) {
        _state.value = _state.value.copy(statusFilter = filter)
    }

    fun setMarketFilter(filter: Market?) {
        _state.value = _state.value.copy(marketFilter = filter)
    }

    fun setTextFilter(text: String) {
        _state.value = _state.value.copy(textFilter = text)
    }

    fun refreshNow() {
        if (_state.value.refreshing) return
        _state.value = _state.value.copy(refreshing = true)
        viewModelScope.launch {
            val crossEnabled = settings.currentMaCrossNotifyEnabled()
            val extremaEnabled = settings.currentMaExtremaNotifyEnabled()
            val updates = repo.refreshAll()
            updates.forEach { update ->
                // Mirror MaCrossoverWorker policy: suppress notifications off-hours, keep DB fresh.
                val marketOpen = update.market.isOpenNow()
                val hasCrossSignal = update.crossed || (update.quantCrossed && update.quantSnapshot != null)
                val hasExtremaSignal = update.extremaDirection != null && update.prevMa5 != null
                if ((hasCrossSignal || hasExtremaSignal) && !marketOpen) return@forEach

                if (crossEnabled && update.crossed) {
                    notifier.notifyCrossover(
                        symbol = update.symbol,
                        name = update.name,
                        newStatus = update.current,
                        ma5 = update.snapshot.ma5,
                        ma20 = update.snapshot.ma20,
                        close = update.close,
                        market = update.market.name,
                    )
                }
                if (crossEnabled && update.quantCrossed && update.quantSnapshot != null) {
                    notifier.notifyQuantSignal(
                        symbol = update.symbol,
                        name = update.name,
                        newStatus = update.currentQuant!!,
                        rsi2 = update.quantSnapshot.rsi2,
                        sma200 = update.quantSnapshot.sma200,
                        close = update.close,
                        market = update.market.name,
                    )
                }
                if (extremaEnabled && hasExtremaSignal) {
                    val today = update.market.todayLocalDate()
                    if (update.lastExtremaNotifyDate != today) {
                        val direction = when (update.extremaDirection!!) {
                            StockRepository.ExtremaDirection.LOW -> Notifier.Ma5ExtremaDirection.LOW
                            StockRepository.ExtremaDirection.HIGH -> Notifier.Ma5ExtremaDirection.HIGH
                        }
                        notifier.notifyMa5Extrema(
                            symbol = update.symbol,
                            name = update.name,
                            direction = direction,
                            prevMa5 = update.prevMa5!!,
                            currentMa5 = update.snapshot.ma5,
                            close = update.close,
                            market = update.market.name,
                        )
                        repo.markExtremaNotified(update.symbol, today)
                    }
                }
            }
            _state.value = _state.value.copy(
                refreshing = false,
                lastRunAt = System.currentTimeMillis(),
            )
        }
    }

    class Factory(
        private val repo: StockRepository,
        private val notifier: Notifier,
        private val settings: AppSettings,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DashboardViewModel(repo, notifier, settings) as T
    }
}
