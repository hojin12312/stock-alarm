package com.example.playground.ui.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.playground.data.model.WatchedStock
import com.example.playground.data.repo.StockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class WatchlistUiState(
    val items: List<WatchedStock> = emptyList(),
)

class WatchlistViewModel(
    private val repo: StockRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(WatchlistUiState())
    val state: StateFlow<WatchlistUiState> = _state.asStateFlow()

    init {
        repo.observeWatchlist()
            .onEach { _state.value = WatchlistUiState(it) }
            .launchIn(viewModelScope)
    }

    fun remove(symbol: String) {
        viewModelScope.launch { repo.removeFromWatchlist(symbol) }
    }

    class Factory(private val repo: StockRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WatchlistViewModel(repo) as T
    }
}
