package com.example.playground.data.source.kis.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KisTokenRequest(
    @SerialName("grant_type") val grantType: String = "client_credentials",
    val appkey: String,
    val appsecret: String,
)

@Serializable
data class KisTokenResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("token_type") val tokenType: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("access_token_token_expired") val expiresAtStr: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
    @SerialName("error_code") val errorCode: String? = null,
)
