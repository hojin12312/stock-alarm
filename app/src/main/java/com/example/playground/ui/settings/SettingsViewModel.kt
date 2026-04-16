package com.example.playground.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.playground.data.prefs.AppSettings
import com.example.playground.data.source.DataSourceId
import com.example.playground.data.source.kis.KisCredentialStore
import com.example.playground.data.source.kis.KisDataSource
import com.example.playground.data.update.UpdateInfo
import com.example.playground.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class SettingsUiState(
    val dataSource: DataSourceId = DataSourceId.YAHOO,
    val kisKeyInput: String = "",
    val kisSecretInput: String = "",
    val kisHasStoredKey: Boolean = false,
    val kisTokenExpiresAt: Long = 0L,
    val busy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val checkingUpdate: Boolean = false,
    val pendingUpdate: UpdateInfo? = null,
)

class SettingsViewModel(
    private val settings: AppSettings,
    private val credentials: KisCredentialStore,
    private val kisDataSource: KisDataSource,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState(kisHasStoredKey = credentials.hasCredentials()))
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        settings.dataSourceId
            .onEach { id -> _state.value = _state.value.copy(dataSource = id) }
            .launchIn(viewModelScope)
        settings.kisTokenExpiresAt
            .onEach { t -> _state.value = _state.value.copy(kisTokenExpiresAt = t) }
            .launchIn(viewModelScope)
    }

    fun onKeyInput(v: String) {
        _state.value = _state.value.copy(kisKeyInput = v, error = null)
    }

    fun onSecretInput(v: String) {
        _state.value = _state.value.copy(kisSecretInput = v, error = null)
    }

    fun selectDataSource(id: DataSourceId) {
        viewModelScope.launch {
            settings.setDataSourceId(id)
            _state.value = _state.value.copy(
                dataSource = id,
                message = if (id == DataSourceId.KIS && !credentials.hasCredentials())
                    "AppKey/Secret을 입력한 뒤 저장해줘"
                else null,
            )
        }
    }

    fun saveAndTest() {
        val appkey = _state.value.kisKeyInput.trim()
        val secret = _state.value.kisSecretInput.trim()
        if (appkey.length < 16 || secret.length < 16) {
            _state.value = _state.value.copy(error = "AppKey/Secret 길이가 너무 짧아")
            return
        }
        _state.value = _state.value.copy(busy = true, error = null, message = null)
        viewModelScope.launch {
            val result = kisDataSource.verifyCredentials(appkey, secret)
            _state.value = if (result.isSuccess) {
                _state.value.copy(
                    busy = false,
                    kisKeyInput = "",
                    kisSecretInput = "",
                    kisHasStoredKey = true,
                    kisTokenExpiresAt = result.getOrThrow(),
                    message = "인증 성공! 이제 KIS로 시세를 받아올게",
                )
            } else {
                _state.value.copy(
                    busy = false,
                    error = result.exceptionOrNull()?.message ?: "인증 실패",
                )
            }
        }
    }

    fun clearCredentials() {
        credentials.clear()
        _state.value = _state.value.copy(
            kisHasStoredKey = false,
            kisTokenExpiresAt = 0L,
            message = "저장된 키를 지웠어",
        )
    }

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null, error = null)
    }

    fun checkForUpdate() {
        if (_state.value.checkingUpdate) return
        _state.value = _state.value.copy(checkingUpdate = true, error = null, message = null)
        viewModelScope.launch {
            val info = ServiceLocator.provideUpdateChecker().check()
            _state.value = if (info != null) {
                _state.value.copy(checkingUpdate = false, pendingUpdate = info)
            } else {
                _state.value.copy(checkingUpdate = false, message = "최신 버전이야! ✨")
            }
        }
    }

    fun dismissUpdate() {
        _state.value = _state.value.copy(pendingUpdate = null)
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(
            settings = ServiceLocator.provideAppSettings(context),
            credentials = ServiceLocator.provideKisCredentialStore(context),
            kisDataSource = ServiceLocator.provideKisDataSource(context),
        ) as T
    }
}
