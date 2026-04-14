package com.example.playground.di

import android.content.Context
import androidx.room.Room
import com.example.playground.data.local.AppDatabase
import com.example.playground.data.prefs.AppSettings
import com.example.playground.data.remote.NetworkModule
import com.example.playground.data.repo.StockRepository
import com.example.playground.data.source.DataSourceId
import com.example.playground.data.source.StockDataSource
import com.example.playground.data.source.YahooFinanceDataSource
import com.example.playground.data.source.kis.KisCredentialStore
import com.example.playground.data.source.kis.KisDataSource
import com.example.playground.data.source.kis.KisNetworkModule
import com.example.playground.data.source.kis.KisTokenStore
import com.example.playground.notification.Notifier

object ServiceLocator {
    @Volatile private var database: AppDatabase? = null
    @Volatile private var repository: StockRepository? = null
    @Volatile private var notifier: Notifier? = null
    @Volatile private var appSettings: AppSettings? = null
    @Volatile private var yahooSource: YahooFinanceDataSource? = null
    @Volatile private var kisCredentialStore: KisCredentialStore? = null
    @Volatile private var kisTokenStore: KisTokenStore? = null
    @Volatile private var kisSource: KisDataSource? = null

    fun provideDatabase(context: Context): AppDatabase =
        database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "playground.db",
            ).build().also { database = it }
        }

    fun provideNotifier(context: Context): Notifier =
        notifier ?: synchronized(this) {
            notifier ?: Notifier(context.applicationContext).also { notifier = it }
        }

    fun provideAppSettings(context: Context): AppSettings =
        appSettings ?: synchronized(this) {
            appSettings ?: AppSettings(context.applicationContext).also { appSettings = it }
        }

    fun provideYahooDataSource(): YahooFinanceDataSource =
        yahooSource ?: synchronized(this) {
            yahooSource ?: YahooFinanceDataSource(NetworkModule.yahooApi).also { yahooSource = it }
        }

    fun provideKisCredentialStore(context: Context): KisCredentialStore =
        kisCredentialStore ?: synchronized(this) {
            kisCredentialStore ?: KisCredentialStore(context.applicationContext)
                .also { kisCredentialStore = it }
        }

    fun provideKisTokenStore(context: Context): KisTokenStore =
        kisTokenStore ?: synchronized(this) {
            kisTokenStore ?: KisTokenStore(provideAppSettings(context))
                .also { kisTokenStore = it }
        }

    fun provideKisDataSource(context: Context): KisDataSource =
        kisSource ?: synchronized(this) {
            kisSource ?: KisDataSource(
                api = KisNetworkModule.api,
                credentials = provideKisCredentialStore(context),
                tokenStore = provideKisTokenStore(context),
            ).also { kisSource = it }
        }

    /** 현재 설정된 활성 데이터 소스 — Repository/워커에서 매 호출마다 이걸 부른다. */
    suspend fun provideActiveDataSource(context: Context): StockDataSource {
        val id = provideAppSettings(context).currentDataSourceId()
        return when (id) {
            DataSourceId.KIS -> provideKisDataSource(context)
            DataSourceId.YAHOO -> provideYahooDataSource()
        }
    }

    fun provideRepository(context: Context): StockRepository =
        repository ?: synchronized(this) {
            repository ?: StockRepository(
                dao = provideDatabase(context).watchlistDao(),
                searchSource = provideYahooDataSource(),
                activeDataSource = { provideActiveDataSource(context) },
            ).also { repository = it }
        }
}
