package com.example.playground.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.playground.data.model.StockSearchResult
import com.example.playground.data.repo.StockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.IOException

enum class SearchErrorType { NETWORK, RATE_LIMIT, OTHER }

data class SearchError(val type: SearchErrorType, val message: String)

data class SearchUiState(
    val query: String = "",
    val loading: Boolean = false,
    val results: List<StockSearchResult> = emptyList(),
    val watchedSymbols: Set<String> = emptySet(),
    val error: SearchError? = null,
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
                val err = classify(result.exceptionOrNull())
                _state.value.copy(loading = false, results = emptyList(), error = err)
            }
        }
    }

    fun retry() = onSubmitSearch()

    fun addToWatchlist(result: StockSearchResult) {
        viewModelScope.launch { repo.addToWatchlist(result) }
    }

    private fun classify(t: Throwable?): SearchError {
        val message = t?.message ?: "검색 실패"
        val type = when {
            t is IOException -> SearchErrorType.NETWORK
            message.contains("429") -> SearchErrorType.RATE_LIMIT
            else -> SearchErrorType.OTHER
        }
        return SearchError(type, message)
    }

    class Factory(private val repo: StockRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SearchViewModel(repo) as T
    }
}
