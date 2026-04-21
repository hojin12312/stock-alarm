package com.example.playground.domain

import com.example.playground.data.model.MaStatus

enum class MaCrossDirection { GOLDEN, DEAD }

data class MaCrossPoint(val index: Int, val direction: MaCrossDirection)

object ChartSignals {

    /**
     * 각 i 시점의 MA 신호.
     * 5MA < 20MA → BUY (루루 정의), 5MA > 20MA → SELL, 같거나 미정의 → null.
     */
    fun maSignalSeries(ma5: List<Double?>, ma20: List<Double?>): List<MaStatus?> {
        val n = minOf(ma5.size, ma20.size)
        val out = ArrayList<MaStatus?>(n)
        for (i in 0 until n) {
            val a = ma5[i]
            val b = ma20[i]
            out.add(
                when {
                    a == null || b == null -> null
                    a < b -> MaStatus.BUY
                    a > b -> MaStatus.SELL
                    else -> null
                }
            )
        }
        return out
    }

    /** 신호 시계열에서 전환이 일어난 지점만 뽑아 반환. */
    fun maCrossPoints(signal: List<MaStatus?>): List<MaCrossPoint> {
        val out = mutableListOf<MaCrossPoint>()
        var prev: MaStatus? = null
        for (i in signal.indices) {
            val cur = signal[i] ?: continue
            if (prev != null && prev != cur) {
                // prev=SELL(5MA>20MA) → cur=BUY(5MA<20MA): 5MA가 20MA를 하향 돌파 = 골든(매수 시작)
                val dir = if (cur == MaStatus.BUY) MaCrossDirection.GOLDEN else MaCrossDirection.DEAD
                out.add(MaCrossPoint(i, dir))
            }
            prev = cur
        }
        return out
    }

    /** RSI 전략 BUY 시그널 발생 인덱스들: close > sma200 AND rsi2 < 10. */
    fun rsiBuyIndices(
        closes: List<Double>,
        sma200: List<Double?>,
        rsi2: List<Double?>,
    ): List<Int> {
        val n = minOf(closes.size, sma200.size, rsi2.size)
        val out = mutableListOf<Int>()
        for (i in 0 until n) {
            val c = closes[i]
            val s = sma200[i] ?: continue
            val r = rsi2[i] ?: continue
            if (c > s && r < 10.0) out.add(i)
        }
        return out
    }

    /** RSI 전략 SELL 시그널 발생 인덱스들: close > sma200 AND rsi2 > 70. */
    fun rsiSellIndices(
        closes: List<Double>,
        sma200: List<Double?>,
        rsi2: List<Double?>,
    ): List<Int> {
        val n = minOf(closes.size, sma200.size, rsi2.size)
        val out = mutableListOf<Int>()
        for (i in 0 until n) {
            val c = closes[i]
            val s = sma200[i] ?: continue
            val r = rsi2[i] ?: continue
            if (c > s && r > 70.0) out.add(i)
        }
        return out
    }
}
