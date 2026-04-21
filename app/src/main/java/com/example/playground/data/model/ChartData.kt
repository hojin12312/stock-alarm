package com.example.playground.data.model

import com.example.playground.domain.QuantCalculator

data class ChartData(
    val symbol: String,
    val name: String,
    val timestamps: List<Long>,
    val closes: List<Double>,
    val ma5Series: List<Double?>,
    val ma20Series: List<Double?>,
) {
    val lastClose: Double? get() = closes.lastOrNull()
    val lastMa5: Double? get() = ma5Series.lastOrNull()
    val lastMa20: Double? get() = ma20Series.lastOrNull()
    val maStatus: MaStatus?
        get() {
            val ma5 = lastMa5 ?: return null
            val ma20 = lastMa20 ?: return null
            return if (ma5 < ma20) MaStatus.BUY else MaStatus.SELL
        }

    /** 하위 호환 */
    val status: MaStatus? get() = maStatus

    val quantSnapshot by lazy { QuantCalculator.compute(closes) }

    // RSI(2) / SMA(200) 시계열 — 차트 과거 신호 오버레이용. 데이터 부족 구간은 null.
    val rsi2Series: List<Double?> by lazy { QuantCalculator.rsiSeries(closes, 2) }
    val sma200Series: List<Double?> by lazy { QuantCalculator.smaSeries(closes, 200) }
}
