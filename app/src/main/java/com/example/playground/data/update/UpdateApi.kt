package com.example.playground.data.update

import retrofit2.http.GET
import retrofit2.http.Url

interface UpdateApi {
    @GET
    suspend fun fetchLatest(@Url url: String): UpdateInfo
}
