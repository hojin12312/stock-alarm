package com.example.playground

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.playground.di.ServiceLocator
import com.example.playground.worker.MaCrossoverWorker
import java.util.concurrent.TimeUnit

class PlaygroundApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 알림 채널 등록 (Notifier 생성자에서 ensureChannel)
        ServiceLocator.provideNotifier(this)
        enqueuePeriodicWorker()
    }

    private fun enqueuePeriodicWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<MaCrossoverWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            MaCrossoverWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
