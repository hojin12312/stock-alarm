package com.example.playground.domain

import com.example.playground.data.model.MaStatus

data class QuantSnapshot(
    val rsi2: Double,
    val sma200: Double,
    val sma5: Double,
    val status: MaStatus,
)

object QuantCalculator {

    /**
     * RSI(2) + SMA(200) 전략.
     * 매수: 종가 > SMA(200) AND RSI(2) < 10
     * 매도: 그 외 (= 매수 조건 미충족)
     * 최소 201개 종가 필요 (SMA 200 + 변화량 1).
     */
    fun compute(closes: List<Double>): QuantSnapshot? {
        if (closes.size < 201) return null

        val sma200 = closes.takeLast(200).average()
        val sma5 = closes.takeLast(5).average()
        val rsi2 = rsi(closes, 2) ?: return null
        val lastClose = closes.last()

        val status = if (lastClose > sma200 && rsi2 < 10) MaStatus.BUY else MaStatus.SELL

        return QuantSnapshot(rsi2 = rsi2, sma200 = sma200, sma5 = sma5, status = status)
    }

    private fun rsi(closes: List<Double>, period: Int): Double? {
        if (closes.size < period + 1) return null
        val changes = closes.zipWithNext { a, b -> b - a }
        val recent = changes.takeLast(period)
        val avgGain = recent.filter { it > 0 }.sum() / period
        val avgLoss = recent.filter { it < 0 }.map { -it }.sum() / period
        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }

    /** i 번째 값 = [0..i] 종가 기반 단순 RSI(period). 데이터 부족 시 null. */
    fun rsiSeries(closes: List<Double>, period: Int = 2): List<Double?> {
        if (closes.isEmpty()) return emptyList()
        val result = ArrayList<Double?>(closes.size)
        // changes[i] = closes[i] - closes[i-1], changes[0] 은 없음
        val changes = DoubleArray(closes.size) { if (it == 0) 0.0 else closes[it] - closes[it - 1] }
        for (i in closes.indices) {
            // 최소 period 개의 변화량 필요 → i >= period
            if (i < period) {
                result.add(null)
                continue
            }
            var gain = 0.0
            var loss = 0.0
            for (k in (i - period + 1)..i) {
                val c = changes[k]
                if (c > 0) gain += c else if (c < 0) loss += -c
            }
            val avgGain = gain / period
            val avgLoss = loss / period
            result.add(if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + avgGain / avgLoss)))
        }
        return result
    }

    /** i 번째 값 = [i-period+1..i] 평균. 데이터 부족 구간은 null. */
    fun smaSeries(closes: List<Double>, period: Int): List<Double?> {
        if (closes.isEmpty() || period <= 0) return emptyList()
        val result = ArrayList<Double?>(closes.size)
        var sum = 0.0
        for (i in closes.indices) {
            sum += closes[i]
            if (i >= period) sum -= closes[i - period]
            result.add(if (i >= period - 1) sum / period else null)
        }
        return result
    }
}
