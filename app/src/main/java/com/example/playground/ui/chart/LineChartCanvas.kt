package com.example.playground.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.playground.data.model.AlgorithmType
import com.example.playground.data.model.ChartData
import com.example.playground.data.model.MaStatus
import com.example.playground.domain.ChartSignals
import com.example.playground.domain.MaCrossDirection
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
internal fun LineChartCanvas(data: ChartData, algorithmType: AlgorithmType) {
    val priceColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val ma5Color = ma5LineColor
    val ma20Color = ma20LineColor
    val buy = buyColor
    val sell = sellColor

    val closes = data.closes
    if (closes.isEmpty()) return
    // y축 범위 — 종가·5MA·20MA 모두 포함하도록
    val allValues = closes + data.ma5Series.filterNotNull() + data.ma20Series.filterNotNull()
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

        // 알고리즘별 과거 신호 배경 오버레이 (라인보다 먼저 그려서 뒤에 깔리도록)
        when (algorithmType) {
            AlgorithmType.MA_CROSS -> drawMaSignalBackground(
                signal = ChartSignals.maSignalSeries(data.ma5Series, data.ma20Series),
                buy = buy,
                sell = sell,
                xOf = ::xOf,
                plotTop = padTop,
                plotHeight = plotH,
                totalWidth = w,
            )
            AlgorithmType.RSI_SMA200 -> {
                // RSI 전략은 점멸형이라 배경 대신 점 마커만 사용 (라인 다음에 그림)
            }
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
        drawSeries(data.ma5Series, ma5Color, ::xOf, ::yOf, dashed = false, strokeWidth = 3f)
        drawSeries(data.ma20Series, ma20Color, ::xOf, ::yOf, dashed = true, strokeWidth = 3f)

        // 알고리즘별 포인트 마커 (라인 위에 그리도록 마지막에)
        when (algorithmType) {
            AlgorithmType.MA_CROSS -> {
                val signal = ChartSignals.maSignalSeries(data.ma5Series, data.ma20Series)
                val crosses = ChartSignals.maCrossPoints(signal)
                crosses.forEach { cp ->
                    val cx = xOf(cp.index)
                    val cy = yOf(closes[cp.index])
                    when (cp.direction) {
                        MaCrossDirection.GOLDEN -> drawTriangleUp(cx, cy, 10f, buy)
                        MaCrossDirection.DEAD -> drawTriangleDown(cx, cy, 10f, sell)
                    }
                }
            }
            AlgorithmType.RSI_SMA200 -> {
                val buyIdx = ChartSignals.rsiBuyIndices(closes, data.sma200Series, data.rsi2Series)
                buyIdx.forEach { i ->
                    drawCircle(
                        color = buy,
                        radius = 5f,
                        center = Offset(xOf(i), yOf(closes[i])),
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawMaSignalBackground(
    signal: List<MaStatus?>,
    buy: Color,
    sell: Color,
    xOf: (Int) -> Float,
    plotTop: Float,
    plotHeight: Float,
    totalWidth: Float,
) {
    if (signal.isEmpty()) return
    val alpha = 0.1f
    var start = -1
    var currentStatus: MaStatus? = null
    fun flush(endExclusive: Int) {
        if (start < 0 || currentStatus == null) return
        val color = (if (currentStatus == MaStatus.BUY) buy else sell).copy(alpha = alpha)
        val x0 = if (start == 0) 0f else xOf(start)
        val x1 = if (endExclusive >= signal.size) totalWidth else xOf(endExclusive)
        drawRect(
            color = color,
            topLeft = Offset(x0, plotTop),
            size = Size(x1 - x0, plotHeight),
        )
    }
    for (i in signal.indices) {
        val s = signal[i]
        if (s != currentStatus) {
            flush(i)
            start = i
            currentStatus = s
        }
    }
    flush(signal.size)
}

private fun DrawScope.drawTriangleUp(cx: Float, cy: Float, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(cx, cy - size)
        lineTo(cx - size, cy + size * 0.8f)
        lineTo(cx + size, cy + size * 0.8f)
        close()
    }
    drawPath(path, color = color)
}

private fun DrawScope.drawTriangleDown(cx: Float, cy: Float, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(cx, cy + size)
        lineTo(cx - size, cy - size * 0.8f)
        lineTo(cx + size, cy - size * 0.8f)
        close()
    }
    drawPath(path, color = color)
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
