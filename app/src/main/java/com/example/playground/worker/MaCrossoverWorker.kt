package com.example.playground.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.playground.data.model.isOpenNow
import com.example.playground.data.model.todayLocalDate
import com.example.playground.data.repo.StockRepository
import com.example.playground.di.ServiceLocator
import com.example.playground.notification.Notifier

class MaCrossoverWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val repo = ServiceLocator.provideRepository(context)
        val notifier = ServiceLocator.provideNotifier(context)
        val settings = ServiceLocator.provideAppSettings(context)

        val crossEnabled = settings.currentMaCrossNotifyEnabled()
        val extremaEnabled = settings.currentMaExtremaNotifyEnabled()

        val updates = repo.refreshAll()
        var crossed = 0
        var skipped = 0
        var extremaNotified = 0
        updates.forEach { update ->
            // Off-hours: refresh state in DB but suppress notifications,
            // to avoid early-morning alerts catching up on yesterday's intraday crossover.
            val marketOpen = update.market.isOpenNow()
            val hasCrossSignal = update.crossed || (update.quantCrossed && update.quantSnapshot != null)
            val hasExtremaSignal = update.extremaDirection != null && update.prevMa5 != null

            if ((hasCrossSignal || hasExtremaSignal) && !marketOpen) {
                skipped++
                Log.d(TAG, "skip notify — ${update.symbol} ${update.market} closed")
                return@forEach
            }

            if (crossEnabled && update.crossed) {
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
            if (crossEnabled && update.quantCrossed && update.quantSnapshot != null) {
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
            if (extremaEnabled && hasExtremaSignal) {
                val today = update.market.todayLocalDate()
                if (update.lastExtremaNotifyDate == today) {
                    Log.d(TAG, "skip extrema — ${update.symbol} already notified today")
                } else {
                    extremaNotified++
                    val direction = when (update.extremaDirection!!) {
                        StockRepository.ExtremaDirection.LOW -> Notifier.Ma5ExtremaDirection.LOW
                        StockRepository.ExtremaDirection.HIGH -> Notifier.Ma5ExtremaDirection.HIGH
                    }
                    notifier.notifyMa5Extrema(
                        symbol = update.symbol,
                        name = update.name,
                        direction = direction,
                        prevMa5 = update.prevMa5!!,
                        currentMa5 = update.snapshot.ma5,
                        close = update.close,
                        market = update.market.name,
                    )
                    repo.markExtremaNotified(update.symbol, today)
                }
            }
        }
        Log.i(TAG, "MaCrossoverWorker done — total=${updates.size}, crossed=$crossed, extrema=$extremaNotified, skipped=$skipped")
        return Result.success()
    }

    companion object {
        private const val TAG = "MaCrossoverWorker"
        const val UNIQUE_NAME = "ma_crossover_periodic"
        const val ONE_SHOT_NAME = "ma_crossover_oneshot"
    }
}
