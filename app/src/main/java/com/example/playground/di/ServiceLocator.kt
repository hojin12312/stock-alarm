package com.example.playground.di

import android.content.Context
import androidx.room.Room
import com.example.playground.data.local.AppDatabase
import com.example.playground.data.remote.NetworkModule
import com.example.playground.data.repo.StockRepository
import com.example.playground.notification.Notifier

object ServiceLocator {
    @Volatile private var database: AppDatabase? = null
    @Volatile private var repository: StockRepository? = null
    @Volatile private var notifier: Notifier? = null

    fun provideDatabase(context: Context): AppDatabase =
        database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "playground.db",
            ).build().also { database = it }
        }

    fun provideRepository(context: Context): StockRepository =
        repository ?: synchronized(this) {
            repository ?: StockRepository(
                dao = provideDatabase(context).watchlistDao(),
                api = NetworkModule.yahooApi,
            ).also { repository = it }
        }

    fun provideNotifier(context: Context): Notifier =
        notifier ?: synchronized(this) {
            notifier ?: Notifier(context.applicationContext).also { notifier = it }
        }
}
