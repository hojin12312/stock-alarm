package com.example.playground.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.playground.data.model.ChartData
import com.example.playground.data.model.MaStatus
import com.example.playground.domain.ChartSignals

// 차트 아래에 한 줄씩 깔리는 과거 매수/매도 신호 바. 캘린더의 여러 날 걸친 일정처럼 길게 색띠로 표시.
@Composable
internal fun MaSignalTimelineBar(data: ChartData, modifier: Modifier = Modifier) {
    val buy = buyColor
    val sell = sellColor
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    val signal = ChartSignals.maSignalSeries(data.ma5Series, data.ma20Series)
    TimelineBarBox(label = "MA", modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(14.dp)) {
            val w = size.width
            val h = size.height
            drawRect(color = trackColor, size = Size(w, h))
            if (signal.isEmpty()) return@Canvas
            val n = signal.size
            var start = -1
            var current: MaStatus? = null
            fun flush(endExclusive: Int) {
                if (start < 0 || current == null) return
                val x0 = w * start / n.toFloat()
                val x1 = w * endExclusive / n.toFloat()
                val color = if (current == MaStatus.BUY) buy else sell
                drawRect(
                    color = color.copy(alpha = 0.9f),
                    topLeft = Offset(x0, 0f),
                    size = Size(x1 - x0, h),
                )
            }
            for (i in signal.indices) {
                val s = signal[i]
                if (s != current) {
                    flush(i)
                    start = i
                    current = s
                }
            }
            flush(n)
        }
    }
}

@Composable
internal fun RsiSignalTimelineBar(data: ChartData, modifier: Modifier = Modifier) {
    val buy = buyColor
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    // SMA(200) 계산에 200일 데이터가 필요. 범위가 짧으면 전부 null → 신호 없음.
    val hasEnoughData = data.sma200Series.any { it != null }
    val signalIndices = if (hasEnoughData) {
        ChartSignals.rsiBuyIndices(data.closes, data.sma200Series, data.rsi2Series)
    } else emptyList()
    TimelineBarBox(label = "RSI", modifier = modifier) {
        if (!hasEnoughData) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .padding(start = 36.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = "SMA(200) 계산에 1y 이상 필요",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Canvas(modifier = Modifier.fillMaxWidth().height(14.dp)) {
                val w = size.width
                val h = size.height
                drawRect(color = trackColor, size = Size(w, h))
                val n = data.closes.size.takeIf { it > 0 } ?: return@Canvas
                val barWidth = (w / n).coerceAtLeast(2f)
                signalIndices.forEach { i ->
                    val x = w * i / n.toFloat()
                    drawRect(
                        color = buy,
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth, h),
                    )
                }
            }
        }
    }
}

// label을 Canvas 위에 오버레이 — Row로 배치하면 label 폭만큼 Canvas가 줄어 차트 x축과 어긋남.
@Composable
private fun TimelineBarBox(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.padding(vertical = 2.dp)) {
        content()
        Text(
            text = label,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}
