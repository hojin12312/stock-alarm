package com.example.playground.data.model

enum class MarketIndex(
    val symbol: String,
    val displayName: String,
    val market: Market,
) {
    KOSPI("^KS11", "코스피", Market.KR),
    KOSDAQ("^KQ11", "코스닥", Market.KR),
    NASDAQ("^IXIC", "나스닥", Market.US),
    DOW("^DJI", "다우존스", Market.US),
    SP500("^GSPC", "S&P 500", Market.US),
    GOLD("GC=F", "금 선물", Market.US),
    USDKRW("USDKRW=X", "USD/KRW", Market.US),
}
