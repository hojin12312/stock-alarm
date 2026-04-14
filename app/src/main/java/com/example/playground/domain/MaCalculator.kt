package com.example.playground.domain

import com.example.playground.data.model.MaStatus

data class MaSnapshot(
    val ma5: Double,
    val ma20: Double,
    val status: MaStatus,
)

object MaCalculator {
    fun compute(closes: List<Double>): MaSnapshot? {
        if (closes.size < 20) return null
        val recent20 = closes.takeLast(20)
        val recent5 = closes.takeLast(5)
        val ma20 = recent20.average()
        val ma5 = recent5.average()
        val status = if (ma5 < ma20) MaStatus.BUY else MaStatus.SELL
        return MaSnapshot(ma5 = ma5, ma20 = ma20, status = status)
    }

    /**
     * 종가 리스트에서 단순 이동평균 시리즈를 만든다.
     * window 미만 위치는 null. 차트 라인용.
     */
    fun movingAverageSeries(closes: List<Double>, window: Int): List<Double?> {
        if (window <= 0) return emptyList()
        return List(closes.size) { i ->
            if (i + 1 < window) null
            else {
                var sum = 0.0
                for (j in (i - window + 1)..i) sum += closes[j]
                sum / window
            }
        }
    }
}
