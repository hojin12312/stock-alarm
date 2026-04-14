package com.example.playground.data.source.kis

import com.example.playground.data.model.Market

/**
 * Yahoo 심볼(예: 005930.KS, AAPL) → KIS API 파라미터 변환.
 */
object KisSymbolMapper {

    sealed interface KisTarget {
        data class Domestic(val code: String, val marketDiv: String) : KisTarget
        data class Oversea(val code: String, val excd: String) : KisTarget
    }

    fun map(symbol: String, market: Market, exchangeHint: String?): KisTarget {
        return when (market) {
            Market.KR -> mapDomestic(symbol)
            Market.US -> mapOversea(symbol, exchangeHint)
        }
    }

    private fun mapDomestic(symbol: String): KisTarget.Domestic {
        val parts = symbol.split('.', limit = 2)
        val code = parts.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("invalid domestic symbol: $symbol")
        val suffix = parts.getOrNull(1)?.uppercase().orEmpty()
        // KIS 국내 일봉 API는 KRX 통합(J) 하나로 KOSPI/KOSDAQ 모두 조회된다.
        val marketDiv = when (suffix) {
            "KQ" -> "J"
            else -> "J"
        }
        return KisTarget.Domestic(code = code, marketDiv = marketDiv)
    }

    private fun mapOversea(symbol: String, exchangeHint: String?): KisTarget.Oversea {
        val code = symbol.substringBefore('.').replace("-", "")
        val excd = resolveExcd(exchangeHint)
        return KisTarget.Oversea(code = code, excd = excd)
    }

    private fun resolveExcd(hint: String?): String {
        if (hint == null) return "NAS"
        val upper = hint.uppercase()
        return when {
            upper.contains("NAS") || upper.contains("NMS") || upper.contains("NGM") -> "NAS"
            upper.contains("NYS") || upper.contains("NYQ") || upper.contains("NYSE") -> "NYS"
            upper.contains("AMEX") || upper.contains("ASE") || upper.contains("AMS") -> "AMS"
            else -> "NAS"
        }
    }
}
