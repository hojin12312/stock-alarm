package com.example.playground.ui.theme

import androidx.compose.ui.graphics.Color

// 도메인 확장 색상 — 라이트/다크 모드 별 변종. Material3 기본 colorScheme에 포함되지 않는 매수·매도·차트 라인 색을 관리.
data class ExtendedColors(
    val buy: Color,
    val sell: Color,
    val ma5Line: Color,
    val ma20Line: Color,
)

internal val ExtendedLight = ExtendedColors(
    buy = Color(0xFF2E7D32),
    sell = Color(0xFFC62828),
    ma5Line = Color(0xFFFB8C00),
    ma20Line = Color(0xFF8E24AA),
)

internal val ExtendedDark = ExtendedColors(
    buy = Color(0xFF66BB6A),
    sell = Color(0xFFEF5350),
    ma5Line = Color(0xFFFFB74D),
    ma20Line = Color(0xFFBA68C8),
)
