package com.example.playground.data.source.kis

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 한국투자증권 AppKey/AppSecret 암호화 저장소.
 * Android Keystore 기반 MasterKey + AES256_SIV(키) / AES256_GCM(값).
 */
class KisCredentialStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    data class Credentials(val appkey: String, val appsecret: String)

    fun save(appkey: String, appsecret: String) {
        prefs.edit()
            .putString(KEY_APPKEY, appkey)
            .putString(KEY_APPSECRET, appsecret)
            .apply()
    }

    fun load(): Credentials? {
        val appkey = prefs.getString(KEY_APPKEY, null) ?: return null
        val appsecret = prefs.getString(KEY_APPSECRET, null) ?: return null
        if (appkey.isBlank() || appsecret.isBlank()) return null
        return Credentials(appkey, appsecret)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun hasCredentials(): Boolean = load() != null

    companion object {
        private const val PREF_NAME = "kis_secure_prefs"
        private const val KEY_APPKEY = "appkey"
        private const val KEY_APPSECRET = "appsecret"
    }
}
