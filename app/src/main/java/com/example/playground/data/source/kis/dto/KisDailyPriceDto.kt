package com.example.playground.data.source.kis.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 국내주식 기간별시세 응답 (일/주/월 공용).
 * tr_id=FHKST03010100
 */
@Serializable
data class KisDomesticDailyResponse(
    @SerialName("rt_cd") val rtCd: String? = null,
    @SerialName("msg_cd") val msgCd: String? = null,
    @SerialName("msg1") val msg: String? = null,
    @SerialName("output2") val output: List<KisDomesticDailyItem> = emptyList(),
)

@Serializable
data class KisDomesticDailyItem(
    @SerialName("stck_bsop_date") val date: String? = null, // yyyyMMdd
    @SerialName("stck_clpr") val close: String? = null,
    @SerialName("stck_oprc") val open: String? = null,
    @SerialName("stck_hgpr") val high: String? = null,
    @SerialName("stck_lwpr") val low: String? = null,
)
