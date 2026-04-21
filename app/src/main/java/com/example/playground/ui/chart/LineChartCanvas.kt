package com.example.playground.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.playground.data.model.ChartData
import com.example.playground.ui.theme.AppColors

// 범례(ChartContent)와 캔버스가 공용으로 쓰는 차트 라인 색상 — 다크 모드 대응.
internal val ma5LineColor: Color
    @Composable @ReadOnlyComposable
    get() = AppColors.extended.ma5Line

internal val ma20LineColor: Color
    @Composable @ReadOnlyComposable
    get() = AppColors.extended.ma20Line

internal val buyColor: Color
    @Composable @ReadOnlyComposable
    get() = AppColors.extended.buy

internal val sellColor: Color
    @Composable @ReadOnlyComposable
    get() = AppColors.extended.sell

@Composable
internal fun LineChartCanvas(data: ChartData) {
    val priceColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val ma5Color = ma5LineColor
    val ma20Color = ma20LineColor

    // 표시 윈도우 슬라이스만 렌더링
    val closes = data.displayCloses
    if (closes.isEmpty()) return
    val ma5 = data.displayMa5
    val ma20 = data.displayMa20
    val allValues = closes + ma5.filterNotNull() + ma20.filterNotNull()
    val minValue = allValues.min()
    val maxValue = allValues.max()
    val range = (maxValue - minValue).takeIf { it > 0 } ?: 1.0

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val padTop = 8f
        val padBottom = 8f
        val plotH = h - padTop - padBottom
        val n = closes.size
        if (n < 2) return@Canvas

        fun xOf(i: Int): Float = w * i / (n - 1).toFloat()
        fun yOf(value: Double): Float {
            val ratio = ((value - minValue) / range).toFloat()
            return padTop + plotH * (1f - ratio)
        }

        for (i in 0..4) {
            val y = padTop + plotH * i / 4f
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f,
            )
        }

        val pricePath = Path().apply {
            moveTo(xOf(0), yOf(closes[0]))
            for (i in 1 until n) lineTo(xOf(i), yOf(closes[i]))
        }
        drawPath(
            path = pricePath,
            color = priceColor,
            style = Stroke(width = 4f),
        )

        // null 구간은 끊어서 그림
        drawSeries(ma5, ma5Color, ::xOf, ::yOf, dashed = false, strokeWidth = 3f)
        drawSeries(ma20, ma20Color, ::xOf, ::yOf, dashed = true, strokeWidth = 3f)
    }
}

private fun DrawScope.drawSeries(
    series: List<Double?>,
    color: Color,
    xOf: (Int) -> Float,
    yOf: (Double) -> Float,
    dashed: Boolean,
    strokeWidth: Float,
) {
    val effect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f) else null
    var pathStarted = false
    var path = Path()
    for (i in series.indices) {
        val v = series[i]
        if (v == null) {
            if (pathStarted) {
                drawPath(path, color = color, style = Stroke(width = strokeWidth, pathEffect = effect))
                path = Path()
                pathStarted = false
            }
            continue
        }
        if (!pathStarted) {
            path.moveTo(xOf(i), yOf(v))
            pathStarted = true
        } else {
            path.lineTo(xOf(i), yOf(v))
        }
    }
    if (pathStarted) {
        drawPath(path, color = color, style = Stroke(width = strokeWidth, pathEffect = effect))
    }
}
