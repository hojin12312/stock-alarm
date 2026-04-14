package com.example.playground.data.source.kis

import com.example.playground.data.model.ChartData
import com.example.playground.data.model.Market
import com.example.playground.data.source.DataSourceId
import com.example.playground.data.source.StockDataSource
import com.example.playground.data.source.kis.dto.KisTokenRequest
import com.example.playground.domain.MaCalculator
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class KisDataSource(
    private val api: KisApi,
    private val credentials: KisCredentialStore,
    private val tokenStore: KisTokenStore,
) : StockDataSource {

    override val id: DataSourceId = DataSourceId.KIS

    override suspend fun fetchCloses(
        symbol: String,
        market: Market,
        exchangeHint: String?,
    ): List<Double> {
        return fetchRaw(symbol, market, exchangeHint).map { it.second }
    }

    override suspend fun fetchChart(
        symbol: String,
        market: Market,
        exchangeHint: String?,
        name: String,
        range: String,
    ): ChartData {
        val raw = fetchRaw(symbol, market, exchangeHint, range)
        val timestamps = raw.map { it.first }
        val closes = raw.map { it.second }
        return ChartData(
            symbol = symbol,
            name = name,
            timestamps = timestamps,
            closes = closes,
            ma5Series = MaCalculator.movingAverageSeries(closes, 5),
            ma20Series = MaCalculator.movingAverageSeries(closes, 20),
        )
    }

    /** 테스트 토큰 발급(설정 화면의 "저장 & 테스트" 버튼용). */
    suspend fun verifyCredentials(appkey: String, appsecret: String): Result<Long> = runCatching {
        val res = api.issueToken(KisTokenRequest(appkey = appkey, appsecret = appsecret))
        val token = res.accessToken ?: error(res.errorDescription ?: "토큰 발급 실패")
        val expiresAt = parseExpiresAt(res)
        credentials.save(appkey, appsecret)
        tokenStore.seed(KisTokenStore.IssuedToken(token = token, expiresAtEpochMs = expiresAt))
        expiresAt
    }

    private suspend fun fetchRaw(
        symbol: String,
        market: Market,
        exchangeHint: String?,
        range: String = "3mo",
    ): List<Pair<Long, Double>> {
        val creds = credentials.load()
            ?: throw IllegalStateException("한국투자증권 AppKey/Secret이 설정되지 않았어")
        val token = tokenStore.getValidToken { issueToken(creds) }
        val auth = "Bearer $token"
        val target = KisSymbolMapper.map(symbol, market, exchangeHint)
        return when (target) {
            is KisSymbolMapper.KisTarget.Domestic -> fetchDomestic(
                creds = creds,
                auth = auth,
                target = target,
                range = range,
            )

            is KisSymbolMapper.KisTarget.Oversea -> fetchOversea(
                creds = creds,
                auth = auth,
                target = target,
            )
        }
    }

    private suspend fun fetchDomestic(
        creds: KisCredentialStore.Credentials,
        auth: String,
        target: KisSymbolMapper.KisTarget.Domestic,
        range: String,
    ): List<Pair<Long, Double>> {
        val end = LocalDate.now()
        val start = end.minusDays(rangeToDays(range))
        val res = api.domesticDailyChart(
            authorization = auth,
            appkey = creds.appkey,
            appsecret = creds.appsecret,
            marketDiv = target.marketDiv,
            symbol = target.code,
            startDate = start.format(yyyymmdd),
            endDate = end.format(yyyymmdd),
        )
        if (res.rtCd != null && res.rtCd != "0") {
            error(res.msg ?: "KIS 국내 시세 조회 실패(rt_cd=${res.rtCd})")
        }
        return res.output
            .mapNotNull { item ->
                val dateStr = item.date ?: return@mapNotNull null
                val close = item.close?.toDoubleOrNull() ?: return@mapNotNull null
                val ts = parseDate(dateStr) ?: return@mapNotNull null
                ts to close
            }
            .sortedBy { it.first } // KIS는 최신순 → 오름차순으로 뒤집기
    }

    private suspend fun fetchOversea(
        creds: KisCredentialStore.Credentials,
        auth: String,
        target: KisSymbolMapper.KisTarget.Oversea,
    ): List<Pair<Long, Double>> {
        val res = api.overseaDailyChart(
            authorization = auth,
            appkey = creds.appkey,
            appsecret = creds.appsecret,
            excd = target.excd,
            symbol = target.code,
        )
        if (res.rtCd != null && res.rtCd != "0") {
            error(res.msg ?: "KIS 해외 시세 조회 실패(rt_cd=${res.rtCd})")
        }
        return res.output
            .mapNotNull { item ->
                val dateStr = item.date ?: return@mapNotNull null
                val close = item.close?.toDoubleOrNull() ?: return@mapNotNull null
                val ts = parseDate(dateStr) ?: return@mapNotNull null
                ts to close
            }
            .sortedBy { it.first }
    }

    private suspend fun issueToken(creds: KisCredentialStore.Credentials): KisTokenStore.IssuedToken {
        val res = api.issueToken(KisTokenRequest(appkey = creds.appkey, appsecret = creds.appsecret))
        val token = res.accessToken
            ?: error(res.errorDescription ?: "KIS 토큰 발급 실패")
        return KisTokenStore.IssuedToken(token = token, expiresAtEpochMs = parseExpiresAt(res))
    }

    private fun parseExpiresAt(res: com.example.playground.data.source.kis.dto.KisTokenResponse): Long {
        val seconds = res.expiresIn ?: 86_400L
        return System.currentTimeMillis() + seconds * 1000L
    }

    private fun parseDate(yyyyMMdd: String): Long? {
        return runCatching {
            LocalDate.parse(yyyyMMdd, yyyymmdd)
                .atStartOfDay(java.time.ZoneOffset.UTC)
                .toEpochSecond()
        }.getOrNull()
    }

    private fun rangeToDays(range: String): Long = when (range) {
        "1mo" -> 45L
        "3mo" -> 120L
        "6mo" -> 230L
        "1y" -> 400L
        else -> 120L
    }

    companion object {
        private val yyyymmdd: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
