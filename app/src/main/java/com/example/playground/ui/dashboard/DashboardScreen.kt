package com.example.playground.ui.dashboard

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.playground.data.model.AlgorithmType
import com.example.playground.data.model.MaStatus
import com.example.playground.data.model.Market
import com.example.playground.data.model.WatchedStock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    contentPadding: PaddingValues,
    onStockClick: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filtered = state.filtered

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "대시보드",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = if (state.lastRunAt != null)
                        "마지막 갱신: ${formatTime(state.lastRunAt!!)}"
                    else "갱신 전",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = { viewModel.refreshNow() },
                enabled = !state.refreshing,
            ) {
                if (state.refreshing) {
                    CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                } else {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "지금 새로고침",
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        AlgorithmTabs(
            selected = state.algorithmType,
            onSelect = viewModel::setAlgorithmType,
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.statusFilter == null,
                    onClick = { viewModel.setStatusFilter(null) },
                    label = { Text("전체") },
                )
                FilterChip(
                    selected = state.statusFilter == MaStatus.BUY,
                    onClick = { viewModel.setStatusFilter(MaStatus.BUY) },
                    label = { Text("매수") },
                )
                FilterChip(
                    selected = state.statusFilter == MaStatus.SELL,
                    onClick = { viewModel.setStatusFilter(MaStatus.SELL) },
                    label = { Text("매도") },
                )
            }

            Spacer(Modifier.weight(1f))

            MarketDropdown(
                selected = state.marketFilter,
                onSelect = viewModel::setMarketFilter,
            )
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = state.textFilter,
            onValueChange = viewModel::setTextFilter,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("종목명 / 심볼 필터") },
        )

        Spacer(Modifier.height(12.dp))

        when {
            state.items.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "관심목록이 비어 있어. 먼저 검색 탭에서 추가해봐 ✨",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            filtered.isEmpty() -> {
                Text(
                    text = "필터 조건에 맞는 종목이 없어",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> {
                if (state.items.any { it.lastStatus == null }) {
                    Text(
                        text = "갱신 전 종목이 있어. 우상단 새로고침 버튼을 눌러봐.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = { it.symbol }) { stock ->
                        DashboardCard(
                            stock = stock,
                            algorithmType = state.algorithmType,
                            onClick = { onStockClick(stock.symbol) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlgorithmTabs(selected: AlgorithmType, onSelect: (AlgorithmType) -> Unit) {
    val tabs = AlgorithmType.entries
    val selectedIndex = tabs.indexOf(selected)
    TabRow(selectedTabIndex = selectedIndex) {
        tabs.forEach { type ->
            Tab(
                selected = type == selected,
                onClick = { onSelect(type) },
                text = {
                    Text(
                        when (type) {
                            AlgorithmType.MA_CROSS -> "MA 교차"
                            AlgorithmType.RSI_SMA200 -> "RSI 전략"
                        }
                    )
                },
            )
        }
    }
}

@Composable
private fun DashboardCard(
    stock: WatchedStock,
    algorithmType: AlgorithmType,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stock.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stock.symbol,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val status = when (algorithmType) {
                    AlgorithmType.MA_CROSS -> stock.lastStatus
                    AlgorithmType.RSI_SMA200 -> stock.lastQuantStatus
                }
                StatusBadge(status)
            }

            Spacer(Modifier.height(8.dp))

            val close = stock.lastClose?.let { formatNumber(it) } ?: "-"
            val detail = when (algorithmType) {
                AlgorithmType.MA_CROSS -> {
                    val ma5 = stock.lastMa5?.let { formatNumber(it) } ?: "-"
                    val ma20 = stock.lastMa20?.let { formatNumber(it) } ?: "-"
                    "5MA $ma5  ·  20MA $ma20  ·  종가 $close"
                }
                AlgorithmType.RSI_SMA200 -> {
                    val rsi = stock.lastRsi2?.let { String.format(Locale.US, "%.1f", it) } ?: "-"
                    val sma200 = stock.lastSma200?.let { formatNumber(it) } ?: "-"
                    "RSI(2) $rsi  ·  SMA200 $sma200  ·  종가 $close"
                }
            }
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (stock.lastUpdatedAt != null) {
                Text(
                    text = "업데이트 ${formatTime(stock.lastUpdatedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MarketDropdown(selected: Market?, onSelect: (Market?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (selected) {
        null -> "전체"
        Market.US -> "미국"
        Market.KR -> "한국"
    }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(label)
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("전체") },
                onClick = { onSelect(null); expanded = false },
            )
            DropdownMenuItem(
                text = { Text("미국") },
                onClick = { onSelect(Market.US); expanded = false },
            )
            DropdownMenuItem(
                text = { Text("한국") },
                onClick = { onSelect(Market.KR); expanded = false },
            )
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

private fun formatNumber(value: Double): String {
    return if (value >= 1000) String.format(Locale.US, "%,.0f", value)
    else String.format(Locale.US, "%.2f", value)
}

private fun formatTime(epochMs: Long): String {
    val fmt = SimpleDateFormat("MM-dd HH:mm", Locale.KOREA)
    return fmt.format(Date(epochMs))
}
