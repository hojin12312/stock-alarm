package com.example.playground.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.playground.di.ServiceLocator

class MaCrossoverWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val repo = ServiceLocator.provideRepository(context)
        val notifier = ServiceLocator.provideNotifier(context)

        val updates = repo.refreshAll()
        var crossed = 0
        updates.forEach { update ->
            if (update.crossed) {
                crossed++
                notifier.notifyCrossover(
                    symbol = update.symbol,
                    name = update.name,
                    newStatus = update.current,
                    ma5 = update.snapshot.ma5,
                    ma20 = update.snapshot.ma20,
                    close = update.close,
                    market = update.market.name,
                )
            }
            if (update.quantCrossed && update.quantSnapshot != null) {
                crossed++
                notifier.notifyQuantSignal(
                    symbol = update.symbol,
                    name = update.name,
                    newStatus = update.currentQuant!!,
                    rsi2 = update.quantSnapshot.rsi2,
                    sma200 = update.quantSnapshot.sma200,
                    close = update.close,
                    market = update.market.name,
                )
            }
        }
        Log.i(TAG, "MaCrossoverWorker done — total=${updates.size}, crossed=$crossed")
        return Result.success()
    }

    companion object {
        private const val TAG = "MaCrossoverWorker"
        const val UNIQUE_NAME = "ma_crossover_periodic"
        const val ONE_SHOT_NAME = "ma_crossover_oneshot"
    }
}
