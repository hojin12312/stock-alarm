package com.example.playground.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.playground.data.local.NotificationDao
import com.example.playground.data.local.NotificationEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val dao: NotificationDao,
) : ViewModel() {

    val items: StateFlow<List<NotificationEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unreadCount: StateFlow<Int> = dao.observeUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

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
