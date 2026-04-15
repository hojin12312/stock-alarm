package com.example.playground.data.remote

import com.example.playground.data.remote.dto.ChartResponseDto
import com.example.playground.data.remote.dto.SearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface YahooFinanceApi {
    @GET
    suspend fun search(
        @Url url: String,
        @Query("q") q: String,
        @Query("quotesCount") quotesCount: Int = 10,
        @Query("newsCount") newsCount: Int = 0,
        @Query("lang") lang: String = "ko-KR",
    ): SearchResponseDto

    @GET("v8/finance/chart/{symbol}")
    suspend fun chart(
        @Path("symbol") symbol: String,
        @Query("range") range: String = "3mo",
        @Query("interval") interval: String = "1d",
    ): ChartResponseDto
}
