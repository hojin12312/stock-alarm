package com.example.playground.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.playground.data.model.AlgorithmType
import com.example.playground.data.model.ChartData
import com.example.playground.data.model.MaStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    viewModel: ChartViewModel,
    algorithmType: AlgorithmType = AlgorithmType.MA_CROSS,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = state.data?.name ?: "차트",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = state.data?.symbol ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                }
            },
        )

        when {
            state.loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "오류: ${state.error}",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            state.data != null -> {
                ChartContent(
                    data = state.data!!,
                    algorithmType = algorithmType,
                    range = state.range,
                    onRangeSelect = viewModel::selectRange,
                )
            }
        }
    }
}

@Composable
private fun ChartContent(
    data: ChartData,
    algorithmType: AlgorithmType,
    range: String,
    onRangeSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        StatusHeader(data, algorithmType)

        Spacer(Modifier.height(12.dp))

        // 기간 토글
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("1mo", "3mo", "6mo", "1y").forEach { r ->
                FilterChip(
                    selected = r == range,
                    onClick = { onRangeSelect(r) },
                    label = { Text(r) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // 차트 그리기
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                LineChartCanvas(data)
            }
        }

        Spacer(Modifier.height(12.dp))

        // 범례
        LegendRow()

        Spacer(Modifier.height(8.dp))

        // 기간 정보
        if (data.timestamps.isNotEmpty()) {
            val first = formatDate(data.timestamps.first())
            val last = formatDate(data.timestamps.last())
            Text(
                text = "$first ~ $last  ·  데이터 ${data.closes.size}개",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusHeader(data: ChartData, algorithmType: AlgorithmType) {
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
                when (algorithmType) {
                    AlgorithmType.MA_CROSS -> {
                        val ma5 = data.lastMa5?.let { formatNumber(it) } ?: "-"
                        val ma20 = data.lastMa20?.let { formatNumber(it) } ?: "-"
                        Text(
                            text = "5MA $ma5  ·  20MA $ma20",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    AlgorithmType.RSI_SMA200 -> {
                        val quant = data.quantSnapshot
                        val rsi = quant?.rsi2?.let { String.format(Locale.US, "%.1f", it) } ?: "-"
                        val sma200 = quant?.sma200?.let { formatNumber(it) } ?: "-"
                        Text(
                            text = "RSI(2) $rsi  ·  SMA200 $sma200",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            val status = when (algorithmType) {
                AlgorithmType.MA_CROSS -> data.maStatus
                AlgorithmType.RSI_SMA200 -> data.quantSnapshot?.status
            }
            StatusBadge(status)
        }
    }
}

@Composable
private fun StatusBadge(status: MaStatus?) {
    val (label, color) = when (status) {
        MaStatus.BUY -> "매수" to Color(0xFF2E7D32)
        MaStatus.SELL -> "매도" to Color(0xFFC62828)
        null -> "대기" to MaterialTheme.colorScheme.outline
    }
    AssistChip(
        onClick = {},
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.15f),
            labelColor = color,
        ),
    )
}

@Composable
private fun LineChartCanvas(data: ChartData) {
    val priceColor = MaterialTheme.colorScheme.primary
    val ma5Color = Color(0xFFFB8C00)
    val ma20Color = Color(0xFF8E24AA)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

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

        // 가로 그리드 4줄
        for (i in 0..4) {
            val y = padTop + plotH * i / 4f
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f,
            )
        }

        fun xOf(i: Int): Float = w * i / (n - 1).toFloat()
        fun yOf(value: Double): Float {
            val ratio = ((value - minValue) / range).toFloat()
            return padTop + plotH * (1f - ratio)
        }

        // 종가 라인
        val pricePath = Path().apply {
            moveTo(xOf(0), yOf(closes[0]))
            for (i in 1 until n) lineTo(xOf(i), yOf(closes[i]))
        }
        drawPath(
            path = pricePath,
            color = priceColor,
            style = Stroke(width = 4f),
        )

        // 5MA / 20MA — null 구간은 끊어서 그림
        drawSeries(data.ma5Series, ma5Color, ::xOf, ::yOf, dashed = false, strokeWidth = 3f)
        drawSeries(data.ma20Series, ma20Color, ::xOf, ::yOf, dashed = true, strokeWidth = 3f)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSeries(
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

@Composable
private fun LegendRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LegendItem(color = MaterialTheme.colorScheme.primary, label = "종가")
        LegendItem(color = Color(0xFFFB8C00), label = "5MA")
        LegendItem(color = Color(0xFF8E24AA), label = "20MA (점선)")
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

private fun formatNumber(value: Double): String =
    if (value >= 1000) String.format(Locale.US, "%,.0f", value)
    else String.format(Locale.US, "%.2f", value)

private fun formatDate(epochSec: Long): String {
    val fmt = SimpleDateFormat("yy-MM-dd", Locale.KOREA)
    return fmt.format(Date(epochSec * 1000L))
}
