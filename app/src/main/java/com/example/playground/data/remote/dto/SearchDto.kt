package com.example.playground.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResponseDto(
    val quotes: List<SearchQuoteDto> = emptyList(),
)

@Serializable
data class SearchQuoteDto(
    val symbol: String? = null,
    val shortname: String? = null,
    val longname: String? = null,
    val exchange: String? = null,
    @SerialName("quoteType") val quoteType: String? = null,
    @SerialName("exchDisp") val exchangeDisplay: String? = null,
    @SerialName("typeDisp") val typeDisplay: String? = null,
)
