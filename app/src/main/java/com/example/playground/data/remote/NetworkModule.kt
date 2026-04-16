package com.example.playground.data.remote

import com.example.playground.data.update.UpdateApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object NetworkModule {
    private const val BASE_URL = "https://query1.finance.yahoo.com/"
    private const val USER_AGENT =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val userAgentInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()
        chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
        redactHeader("Authorization")
        redactHeader("appkey")
        redactHeader("appsecret")
    }

    private val okHttp: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(userAgentInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttp)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val yahooApi: YahooFinanceApi = retrofit.create(YahooFinanceApi::class.java)

    private val noCacheInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("Cache-Control", "no-cache, no-store")
            .build()
        chain.proceed(request)
    }

    private val updateOkHttp: OkHttpClient = okHttp.newBuilder()
        .addInterceptor(noCacheInterceptor)
        .build()

    private val updateRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://raw.githubusercontent.com/")
        .client(updateOkHttp)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val updateApi: UpdateApi = updateRetrofit.create(UpdateApi::class.java)
}
