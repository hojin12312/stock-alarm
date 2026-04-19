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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.playground.data.model.AlgorithmType
import com.example.playground.data.model.ChartData
import com.example.playground.domain.resolveDisplayStatus
import com.example.playground.ui.common.StatusBadge
import com.example.playground.util.formatDateYmd
import com.example.playground.util.formatDecimal1
import com.example.playground.util.formatNumber

@Composable
internal fun ChartContent(
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

        LegendRow()

        Spacer(Modifier.height(8.dp))

        if (data.timestamps.isNotEmpty()) {
            val first = formatDateYmd(data.timestamps.first())
            val last = formatDateYmd(data.timestamps.last())
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
                        val rsi = quant?.rsi2?.let { formatDecimal1(it) } ?: "-"
                        val sma200 = quant?.sma200?.let { formatNumber(it) } ?: "-"
                        Text(
                            text = "RSI(2) $rsi  ·  SMA200 $sma200",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            StatusBadge(resolveDisplayStatus(algorithmType, data.maStatus, data.quantSnapshot?.status))
        }
    }
}

@Composable
private fun LegendRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LegendItem(color = MaterialTheme.colorScheme.primary, label = "종가")
        LegendItem(color = Ma5LineColor, label = "5MA")
        LegendItem(color = Ma20LineColor, label = "20MA (점선)")
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
