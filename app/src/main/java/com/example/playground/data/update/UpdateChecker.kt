package com.example.playground.data.update

import android.util.Log
import com.example.playground.BuildConfig

class UpdateChecker(
    private val api: UpdateApi,
    private val metadataUrl: String = DEFAULT_METADATA_URL,
    private val currentVersionCode: Int = BuildConfig.VERSION_CODE,
) {
    suspend fun check(): UpdateInfo? = runCatching {
        val info = api.fetchLatest(metadataUrl)
        if (info.versionCode > currentVersionCode) info else null
    }.onFailure { Log.w(TAG, "버전 확인 실패: ${it.message}") }.getOrNull()

    companion object {
        private const val TAG = "UpdateChecker"
        const val DEFAULT_METADATA_URL =
            "https://raw.githubusercontent.com/hojin12312/stock-alarm/main/dist/version.json"
    }
}
