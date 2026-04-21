package com.example.playground.data.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

// 시장별 정규장 시간대. 공휴일은 반영하지 않음 — 휴장일에 알림이 안 뜨는 쪽은 치명적이지 않아서 의도적으로 제외.
private val KR_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
private val US_ZONE: ZoneId = ZoneId.of("America/New_York")

private val KR_OPEN: LocalTime = LocalTime.of(9, 0)
private val KR_CLOSE: LocalTime = LocalTime.of(15, 30)
private val US_OPEN: LocalTime = LocalTime.of(9, 30)
private val US_CLOSE: LocalTime = LocalTime.of(16, 0)

fun Market.isOpenNow(now: Instant = Instant.now()): Boolean {
    val (zone, open, close) = when (this) {
        Market.KR -> Triple(KR_ZONE, KR_OPEN, KR_CLOSE)
        Market.US -> Triple(US_ZONE, US_OPEN, US_CLOSE)
    }
    val local = now.atZone(zone)
    val dow = local.dayOfWeek
    if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) return false
    val time = local.toLocalTime()
    return !time.isBefore(open) && time.isBefore(close)
}
