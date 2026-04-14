package com.example.playground.data.source.kis.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 해외주식 기간별시세 응답.
 * tr_id=HHDFS76240000
 */
@Serializable
data class KisOverseaDailyResponse(
    @SerialName("rt_cd") val rtCd: String? = null,
    @SerialName("msg_cd") val msgCd: String? = null,
    @SerialName("msg1") val msg: String? = null,
    @SerialName("output2") val output: List<KisOverseaDailyItem> = emptyList(),
)

@Serializable
data class KisOverseaDailyItem(
    @SerialName("xymd") val date: String? = null, // yyyyMMdd
    @SerialName("clos") val close: String? = null,
    @SerialName("open") val open: String? = null,
    @SerialName("high") val high: String? = null,
    @SerialName("low") val low: String? = null,
)
