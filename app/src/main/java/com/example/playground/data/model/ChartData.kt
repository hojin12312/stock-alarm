package com.example.playground.data.model

import com.example.playground.domain.QuantCalculator

data class ChartData(
    val symbol: String,
    val name: String,
    val timestamps: List<Long>,
    val closes: List<Double>,
    val ma5Series: List<Double?>,
    val ma20Series: List<Double?>,
    // 화면에 표시할 마지막 N개. Int.MAX_VALUE = 전체.
    val displayCount: Int = Int.MAX_VALUE,
    // 최신 기준으로 건너뛸 개수 (0 = 가장 최근, 양수 = 과거로 이동).
    val displayOffset: Int = 0,
) {
    // 실제 표시 개수 (데이터 범위 클램핑)
    private val dc get() = displayCount.coerceIn(1, closes.size.coerceAtLeast(1))
    private val startIdx get() = (closes.size - dc - displayOffset).coerceAtLeast(0)
    private val displayLen get() = dc.coerceAtMost(closes.size - startIdx)

    // 현재 표시 윈도우 슬라이스
    val displayTimestamps: List<Long> get() = timestamps.subList(startIdx, startIdx + displayLen)
    val displayCloses: List<Double> get() = closes.subList(startIdx, startIdx + displayLen)
    val displayMa5: List<Double?> get() = ma5Series.subList(startIdx, startIdx + displayLen)
    val displayMa20: List<Double?> get() = ma20Series.subList(startIdx, startIdx + displayLen)

    // 현재값은 전체 데이터의 마지막 기준 (상태 뱃지용)
    val lastClose: Double? get() = closes.lastOrNull()
    val lastMa5: Double? get() = ma5Series.lastOrNull()
    val lastMa20: Double? get() = ma20Series.lastOrNull()
    val maStatus: MaStatus?
        get() {
            val ma5 = lastMa5 ?: return null
            val ma20 = lastMa20 ?: return null
            return if (ma5 < ma20) MaStatus.BUY else MaStatus.SELL
        }

    val status: MaStatus? get() = maStatus

    // 전체 데이터로 계산 (정확한 현재 상태를 위해)
    val quantSnapshot by lazy { QuantCalculator.compute(closes) }

    // 전체 기간 시계열 (과거 신호 계산용)
    val rsi2Series: List<Double?> by lazy { QuantCalculator.rsiSeries(closes, 2) }
    val sma200Series: List<Double?> by lazy { QuantCalculator.smaSeries(closes, 200) }

    // 표시 윈도우만큼 슬라이싱된 시계열 (타임라인 바 렌더링용)
    val displayRsi2: List<Double?> get() = rsi2Series.subList(startIdx, startIdx + displayLen)
    val displaySma200: List<Double?> get() = sma200Series.subList(startIdx, startIdx + displayLen)
}
