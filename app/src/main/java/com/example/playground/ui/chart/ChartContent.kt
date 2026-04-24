package com.example.playground.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.playground.data.model.AlgorithmType
import com.example.playground.data.model.ChartData
import com.example.playground.data.model.MaStatus
import com.example.playground.domain.resolveDisplayStatus
import com.example.playground.ui.common.StatusBadge
import com.example.playground.util.formatDateYmd
import com.example.playground.util.formatDecimal1
import com.example.playground.util.formatNumber

@Composable
internal fun ChartContent(
    data: ChartData,
    selectedAlgorithms: Set<AlgorithmType>,
    onToggleAlgorithm: (AlgorithmType) -> Unit,
    range: String,
    onRangeSelect: (String) -> Unit,
) {
    val totalSize = data.closes.size
    var zoomedCount by remember { mutableStateOf(data.displayCount.coerceIn(10, totalSize.coerceAtLeast(10))) }
    var scrollOffset by remember { mutableStateOf(0) }

    // range 변경 시 줌·스크롤 초기화
    LaunchedEffect(data.displayCount) {
        zoomedCount = data.displayCount.coerceIn(10, totalSize.coerceAtLeast(10))
        scrollOffset = 0
    }

    val maxOffset = (totalSize - zoomedCount).coerceAtLeast(0)
    val clampedOffset = scrollOffset.coerceIn(0, maxOffset)
    val displayData = data.copy(displayCount = zoomedCount, displayOffset = clampedOffset)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        StatusHeader(data, selectedAlgorithms)

        Spacer(Modifier.height(12.dp))

        AlgorithmChecklist(selectedAlgorithms, onToggleAlgorithm)

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("1mo", "3mo", "6mo", "1y", "2y", "5y").forEach { r ->
                FilterChip(
                    selected = r == range,
                    onClick = { onRangeSelect(r) },
                    label = { Text(r) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            if (totalSize == 0) return@detectTransformGestures
                            val curCount = zoomedCount
                            val curOffset = scrollOffset.coerceIn(0, (totalSize - curCount).coerceAtLeast(0))
                            if (zoom != 1f) {
                                val newCount = (curCount / zoom).roundToInt().coerceIn(10, totalSize)
                                val pivotItem = curOffset + (centroid.x / size.width * curCount).roundToInt()
                                scrollOffset = (pivotItem - (centroid.x / size.width * newCount).roundToInt())
                                    .coerceIn(0, (totalSize - newCount).coerceAtLeast(0))
                                zoomedCount = newCount
                            } else {
                                val pixPerItem = size.width / curCount.toFloat()
                                if (pixPerItem > 0f) {
                                    val delta = (pan.x / pixPerItem).roundToInt()
                                    if (delta != 0) {
                                        scrollOffset = (curOffset + delta)
                                            .coerceIn(0, (totalSize - curCount).coerceAtLeast(0))
                                    }
                                }
                            }
                        }
                    }
                    .padding(12.dp),
            ) {
                LineChartCanvas(data = displayData)
            }
        }

        Spacer(Modifier.height(8.dp))

        // 차트 x축에 정렬되도록 좌우 패딩을 Card 내부와 동일하게.
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            if (AlgorithmType.MA_CROSS in selectedAlgorithms) {
                MaSignalTimelineBar(displayData)
            }
            if (AlgorithmType.RSI_SMA200 in selectedAlgorithms) {
                RsiSignalTimelineBar(displayData)
            }
        }

        Spacer(Modifier.height(12.dp))

        LegendRow(selectedAlgorithms)

        Spacer(Modifier.height(8.dp))

        if (displayData.displayTimestamps.isNotEmpty()) {
            val first = formatDateYmd(displayData.displayTimestamps.first())
            val last = formatDateYmd(displayData.displayTimestamps.last())
            Text(
                text = "$first ~ $last  ·  데이터 ${displayData.displayCloses.size}개",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AlgorithmChecklist(
    selected: Set<AlgorithmType>,
    onToggle: (AlgorithmType) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = AlgorithmType.MA_CROSS in selected,
            onClick = { onToggle(AlgorithmType.MA_CROSS) },
            label = { Text("MA 교차") },
        )
        FilterChip(
            selected = AlgorithmType.RSI_SMA200 in selected,
            onClick = { onToggle(AlgorithmType.RSI_SMA200) },
            label = { Text("RSI 전략") },
        )
    }
}

@Composable
private fun StatusHeader(data: ChartData, selected: Set<AlgorithmType>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "현재가",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = data.lastClose?.let { formatNumber(it) } ?: "-",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(4.dp))
                if (AlgorithmType.MA_CROSS in selected) {
                    val ma5 = data.lastMa5?.let { formatNumber(it) } ?: "-"
                    val ma20 = data.lastMa20?.let { formatNumber(it) } ?: "-"
                    Text(
                        text = "5MA $ma5  ·  20MA $ma20",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (AlgorithmType.RSI_SMA200 in selected) {
                    val quant = data.quantSnapshot
                    val rsi = quant?.rsi2?.let { formatDecimal1(it) } ?: "-"
                    val sma200 = quant?.sma200?.let { formatNumber(it) } ?: "-"
                    Text(
                        text = "RSI(2) $rsi  ·  SMA200 $sma200",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            StatusColumn(data, selected)
        }
    }
}

@Composable
private fun StatusColumn(data: ChartData, selected: Set<AlgorithmType>) {
    // 단일 선택이면 뱃지 하나, 둘 다면 라벨 달고 두 개 세로 나열
    val showMa = AlgorithmType.MA_CROSS in selected
    val showRsi = AlgorithmType.RSI_SMA200 in selected
    val ma = data.maStatus
    val rsi = data.quantSnapshot?.status
    when {
        showMa && showRsi -> Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            LabeledBadge(label = "MA", status = ma)
            LabeledBadge(label = "RSI", status = rsi)
        }
        showMa -> StatusBadge(resolveDisplayStatus(AlgorithmType.MA_CROSS, ma, rsi))
        showRsi -> StatusBadge(resolveDisplayStatus(AlgorithmType.RSI_SMA200, ma, rsi))
    }
}

@Composable
private fun LabeledBadge(label: String, status: MaStatus?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StatusBadge(status)
    }
}

@Composable
private fun LegendRow(selected: Set<AlgorithmType>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LegendItem(color = MaterialTheme.colorScheme.primary, label = "종가")
            LegendItem(color = ma5LineColor, label = "5MA")
            LegendItem(color = ma20LineColor, label = "20MA (점선)")
        }
        if (AlgorithmType.MA_CROSS in selected) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LegendItem(color = buyColor, label = "매수 구간")
                LegendItem(color = sellColor, label = "매도 구간")
            }
        }
        if (AlgorithmType.RSI_SMA200 in selected) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LegendItem(color = buyColor, label = "매수 신호")
                LegendItem(color = sellColor, label = "매도 신호")
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(3.dp)
                .padding(end = 0.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 4f,
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}
