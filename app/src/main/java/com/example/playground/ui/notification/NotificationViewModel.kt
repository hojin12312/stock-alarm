package com.example.playground.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.playground.data.local.NotificationDao
import com.example.playground.data.local.NotificationEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotificationFilter(
    val type: String? = null,     // null=전체, "MA", "RSI"
    val status: String? = null,   // null=전체, "BUY", "SELL"
    val market: String? = null,   // null=전체, "US", "KR"
)

class NotificationViewModel(
    private val dao: NotificationDao,
) : ViewModel() {

    private val allItems = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unreadCount: StateFlow<Int> = dao.observeUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _filter = MutableStateFlow(NotificationFilter())
    val filter: StateFlow<NotificationFilter> = _filter

    val items: StateFlow<List<NotificationEntity>> = combine(allItems, _filter) { list, f ->
        list.filter { item ->
            val typeOk = f.type == null || item.type == f.type
            val statusOk = f.status == null || item.status == f.status
            val marketOk = f.market == null || item.market == f.market
            typeOk && statusOk && marketOk
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setTypeFilter(type: String?) {
        _filter.value = _filter.value.copy(type = type)
    }

    fun setStatusFilter(status: String?) {
        _filter.value = _filter.value.copy(status = status)
    }

    fun setMarketFilter(market: String?) {
        _filter.value = _filter.value.copy(market = market)
    }

    fun markAllRead() {
        viewModelScope.launch { dao.markAllRead() }
    }

    fun delete(id: Long) {
        viewModelScope.launch { dao.deleteById(id) }
    }

    fun deleteAll() {
        viewModelScope.launch { dao.deleteAll() }
    }

    class Factory(private val dao: NotificationDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NotificationViewModel(dao) as T
    }
}
