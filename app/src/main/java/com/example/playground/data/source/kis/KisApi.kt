package com.example.playground.data.source.kis

import com.example.playground.data.source.kis.dto.KisDomesticDailyResponse
import com.example.playground.data.source.kis.dto.KisOverseaDailyResponse
import com.example.playground.data.source.kis.dto.KisTokenRequest
import com.example.playground.data.source.kis.dto.KisTokenResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface KisApi {

    @POST("oauth2/tokenP")
    suspend fun issueToken(@Body body: KisTokenRequest): KisTokenResponse

    @GET("uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice")
    suspend fun domesticDailyChart(
        @Header("authorization") authorization: String,
        @Header("appkey") appkey: String,
        @Header("appsecret") appsecret: String,
        @Header("tr_id") trId: String = TR_ID_DOMESTIC_DAILY,
        @Header("custtype") custType: String = "P",
        @Query("FID_COND_MRKT_DIV_CODE") marketDiv: String, // "J" KRX, "Q" KOSDAQ
        @Query("FID_INPUT_ISCD") symbol: String,
        @Query("FID_INPUT_DATE_1") startDate: String, // yyyyMMdd
        @Query("FID_INPUT_DATE_2") endDate: String,   // yyyyMMdd
        @Query("FID_PERIOD_DIV_CODE") periodDiv: String = "D", // 일봉
        @Query("FID_ORG_ADJ_PRC") adjusted: String = "0",       // 0=수정주가
    ): KisDomesticDailyResponse

    @GET("uapi/overseas-price/v1/quotations/dailyprice")
    suspend fun overseaDailyChart(
        @Header("authorization") authorization: String,
        @Header("appkey") appkey: String,
        @Header("appsecret") appsecret: String,
        @Header("tr_id") trId: String = TR_ID_OVERSEA_DAILY,
        @Header("custtype") custType: String = "P",
        @Query("AUTH") auth: String = "",
        @Query("EXCD") excd: String,         // NAS/NYS/AMS
        @Query("SYMB") symbol: String,
        @Query("GUBN") gubn: String = "0",   // 0=일, 1=주, 2=월
        @Query("BYMD") bymd: String = "",    // 조회 기준일 yyyyMMdd (공란=당일)
        @Query("MODP") modp: String = "1",   // 1=수정주가
    ): KisOverseaDailyResponse

    companion object {
        const val BASE_URL = "https://openapi.koreainvestment.com:9443/"
        const val TR_ID_DOMESTIC_DAILY = "FHKST03010100"
        const val TR_ID_OVERSEA_DAILY = "HHDFS76240000"
    }
}
