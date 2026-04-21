package com.example.playground.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
    TimelineBarRow(
        label = "MA",
        modifier = modifier,
    ) {
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
    val signalIndices = ChartSignals.rsiBuyIndices(data.closes, data.sma200Series, data.rsi2Series)
    TimelineBarRow(
        label = "RSI",
        modifier = modifier,
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(14.dp)) {
            val w = size.width
            val h = size.height
            drawRect(color = trackColor, size = Size(w, h))
            val n = data.closes.size.takeIf { it > 0 } ?: return@Canvas
            // 시그널 발생일마다 2dp 폭의 녹색 세로 바
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

@Composable
private fun TimelineBarRow(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(32.dp),
        )
        Spacer(Modifier.width(4.dp))
        content()
    }
}
