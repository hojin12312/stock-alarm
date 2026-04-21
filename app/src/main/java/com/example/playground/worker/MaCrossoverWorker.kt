package com.example.playground.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.playground.data.model.isOpenNow
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
        var skipped = 0
        updates.forEach { update ->
            // 장 시간 외에는 상태는 DB에 갱신되지만 알림은 발송하지 않음.
            // 이유: 장외에 워커가 깨면서 어제 장중에 발생한 교차를 뒤늦게 감지해 새벽에 알림이 울리는 문제를 방지.
            val marketOpen = update.market.isOpenNow()
            val hasSignal = update.crossed || (update.quantCrossed && update.quantSnapshot != null)
            if (hasSignal && !marketOpen) {
                skipped++
                Log.d(TAG, "skip notify — ${update.symbol} ${update.market} closed")
                return@forEach
            }
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
        Log.i(TAG, "MaCrossoverWorker done — total=${updates.size}, crossed=$crossed, skipped=$skipped")
        return Result.success()
    }

    companion object {
        private const val TAG = "MaCrossoverWorker"
        const val UNIQUE_NAME = "ma_crossover_periodic"
        const val ONE_SHOT_NAME = "ma_crossover_oneshot"
    }
}
