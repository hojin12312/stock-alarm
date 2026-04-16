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
}
