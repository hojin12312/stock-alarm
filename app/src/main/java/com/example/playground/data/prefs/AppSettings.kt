package com.example.playground.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.playground.data.source.DataSourceId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class AppSettings(private val context: Context) {

    val dataSourceId: Flow<DataSourceId> = context.dataStore.data.map { prefs ->
        when (prefs[KEY_DATA_SOURCE]) {
            DataSourceId.KIS.name -> DataSourceId.KIS
            else -> DataSourceId.YAHOO
        }
    }

    val kisTokenExpiresAt: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_KIS_TOKEN_EXPIRES_AT] ?: 0L
    }

    val chartRange: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CHART_RANGE] ?: DEFAULT_CHART_RANGE
    }

    val maCrossNotifyEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_MA_CROSS_NOTIFY] ?: true
    }

    val maExtremaNotifyEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_MA_EXTREMA_NOTIFY] ?: false
    }

    suspend fun currentDataSourceId(): DataSourceId = dataSourceId.first()

    suspend fun currentChartRange(): String = chartRange.first()

    suspend fun currentMaCrossNotifyEnabled(): Boolean = maCrossNotifyEnabled.first()

    suspend fun currentMaExtremaNotifyEnabled(): Boolean = maExtremaNotifyEnabled.first()

    suspend fun setDataSourceId(id: DataSourceId) {
        context.dataStore.edit { it[KEY_DATA_SOURCE] = id.name }
    }

    suspend fun setKisTokenExpiresAt(epochMs: Long) {
        context.dataStore.edit { it[KEY_KIS_TOKEN_EXPIRES_AT] = epochMs }
    }

    suspend fun setChartRange(range: String) {
        context.dataStore.edit { it[KEY_CHART_RANGE] = range }
    }

    suspend fun setMaCrossNotifyEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_MA_CROSS_NOTIFY] = enabled }
    }

    suspend fun setMaExtremaNotifyEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_MA_EXTREMA_NOTIFY] = enabled }
    }

    companion object {
        const val DEFAULT_CHART_RANGE = "3mo"

        private val KEY_DATA_SOURCE = stringPreferencesKey("data_source_id")
        private val KEY_KIS_TOKEN_EXPIRES_AT = longPreferencesKey("kis_token_expires_at")
        private val KEY_CHART_RANGE = stringPreferencesKey("chart_range")
        private val KEY_MA_CROSS_NOTIFY = booleanPreferencesKey("ma_cross_notify_enabled")
        private val KEY_MA_EXTREMA_NOTIFY = booleanPreferencesKey("ma_extrema_notify_enabled")
    }
}
