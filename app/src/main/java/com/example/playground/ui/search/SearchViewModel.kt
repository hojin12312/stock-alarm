package com.example.playground.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.playground.data.model.StockSearchResult
import com.example.playground.data.repo.StockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val loading: Boolean = false,
    val results: List<StockSearchResult> = emptyList(),
    val watchedSymbols: Set<String> = emptySet(),
    val error: String? = null,
)

class SearchViewModel(
    private val repo: StockRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    init {
        repo.observeWatchedSymbols()
            .onEach { symbols ->
                _state.value = _state.value.copy(watchedSymbols = symbols.toSet())
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(q: String) {
        _state.value = _state.value.copy(query = q, error = null)
    }

    fun onSubmitSearch() {
        val q = _state.value.query.trim()
        if (q.isEmpty()) return
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val result = repo.search(q)
            _state.value = if (result.isSuccess) {
                _state.value.copy(loading = false, results = result.getOrDefault(emptyList()))
            } else {
                _state.value.copy(
                    loading = false,
                    results = emptyList(),
                    error = result.exceptionOrNull()?.message ?: "검색 실패",
                )
            }
        }
    }

    fun addToWatchlist(result: StockSearchResult) {
        viewModelScope.launch { repo.addToWatchlist(result) }
    }

    class Factory(private val repo: StockRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SearchViewModel(repo) as T
    }
}
