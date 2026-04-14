package com.example.playground.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChartResponseDto(
    val chart: ChartBodyDto,
)

@Serializable
data class ChartBodyDto(
    val result: List<ChartResultDto>? = null,
    val error: ChartErrorDto? = null,
)

@Serializable
data class ChartErrorDto(
    val code: String? = null,
    val description: String? = null,
)

@Serializable
data class ChartResultDto(
    val indicators: ChartIndicatorsDto? = null,
)

@Serializable
data class ChartIndicatorsDto(
    val quote: List<ChartQuoteDto> = emptyList(),
)

@Serializable
data class ChartQuoteDto(
    val close: List<Double?> = emptyList(),
)
