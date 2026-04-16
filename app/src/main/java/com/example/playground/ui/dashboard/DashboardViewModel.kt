package com.example.playground.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.playground.data.model.AlgorithmType
import com.example.playground.data.model.MaStatus
import com.example.playground.data.model.Market
import com.example.playground.data.model.WatchedStock
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
    val algorithmType: AlgorithmType = AlgorithmType.MA_CROSS,
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
                val itemStatus = when (algorithmType) {
                    AlgorithmType.MA_CROSS -> item.lastStatus
                    AlgorithmType.RSI_SMA200 -> item.lastQuantStatus
                }
                val statusOk = statusFilter == null || itemStatus == statusFilter
                val marketOk = marketFilter == null || item.market == marketFilter
                val textOk = text.isEmpty() ||
                    item.name.lowercase().contains(text) ||
                    item.symbol.lowercase().contains(text)
                statusOk && marketOk && textOk
            }
        }
}

class DashboardViewModel(
    private val repo: StockRepository,
    private val notifier: Notifier,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        repo.observeWatchlist()
            .onEach { items -> _state.value = _state.value.copy(items = items) }
            .launchIn(viewModelScope)
    }

    fun setAlgorithmType(type: AlgorithmType) {
        _state.value = _state.value.copy(algorithmType = type, statusFilter = null)
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
            val updates = repo.refreshAll()
            updates.forEach { update ->
                if (update.crossed) {
                    notifier.notifyCrossover(
                        symbol = update.symbol,
                        name = update.name,
                        newStatus = update.current,
                        ma5 = update.snapshot.ma5,
                        ma20 = update.snapshot.ma20,
                        close = update.close,
                    )
                }
                if (update.quantCrossed && update.quantSnapshot != null) {
                    notifier.notifyQuantSignal(
                        symbol = update.symbol,
                        name = update.name,
                        newStatus = update.currentQuant!!,
                        rsi2 = update.quantSnapshot.rsi2,
                        sma200 = update.quantSnapshot.sma200,
                        close = update.close,
                    )
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
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DashboardViewModel(repo, notifier) as T
    }
}
