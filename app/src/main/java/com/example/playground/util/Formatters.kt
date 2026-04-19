package com.example.playground.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 1000 이상이면 천단위 콤마·정수, 미만이면 소수점 2자리. 가격·이평선·SMA 등 공용.
fun formatNumber(value: Double): String =
    if (value >= 1000) String.format(Locale.US, "%,.0f", value)
    else String.format(Locale.US, "%.2f", value)

// RSI 등 소수점 1자리로 표시.
fun formatDecimal1(value: Double): String =
    String.format(Locale.US, "%.1f", value)

// 초 단위 epoch → "yy-MM-dd" (차트 축 레이블).
fun formatDateYmd(epochSec: Long): String =
    SimpleDateFormat("yy-MM-dd", Locale.KOREA).format(Date(epochSec * 1000L))

// 밀리초 단위 epoch → "MM-dd HH:mm" (대시보드·알림 카드).
fun formatDateTime(epochMs: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.KOREA).format(Date(epochMs))

// 밀리초 단위 epoch → "HH:mm" (설정 화면 KIS 토큰 만료 시각).
fun formatClock(epochMs: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMs))
