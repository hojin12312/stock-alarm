package com.example.playground.data.model

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
    val status: MaStatus?
        get() {
            val ma5 = lastMa5 ?: return null
            val ma20 = lastMa20 ?: return null
            return if (ma5 < ma20) MaStatus.BUY else MaStatus.SELL
        }
}
