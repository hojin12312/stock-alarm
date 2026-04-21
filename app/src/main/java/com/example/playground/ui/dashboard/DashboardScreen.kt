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
import com.example.playground.domain.resolveDisplayStatus
import com.example.playground.ui.common.StatusBadge
import com.example.playground.util.formatDateTime
import com.example.playground.util.formatDecimal1
import com.example.playground.util.formatNumber

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    contentPadding: PaddingValues,
    onStockClick: (String, Set<AlgorithmType>) -> Unit,
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
                        "마지막 갱신: ${formatDateTime(state.lastRunAt!!)}"
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

        AlgorithmChecklist(
            selected = state.selectedAlgorithms,
            onToggle = viewModel::toggleAlgorithm,
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
                            selectedAlgorithms = state.selectedAlgorithms,
                            onClick = { onStockClick(stock.symbol, state.selectedAlgorithms) },
                        )
                    }
                }
            }
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
private fun DashboardCard(
    stock: WatchedStock,
    selectedAlgorithms: Set<AlgorithmType>,
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
                val status = if (selectedAlgorithms.size == 1) {
                    resolveDisplayStatus(selectedAlgorithms.first(), stock.lastStatus, stock.lastQuantStatus)
                } else {
                    stock.lastStatus
                }
                StatusBadge(status)
            }

            Spacer(Modifier.height(8.dp))

            val close = stock.lastClose?.let { formatNumber(it) } ?: "-"
            val ma5 = stock.lastMa5?.let { formatNumber(it) } ?: "-"
            val ma20 = stock.lastMa20?.let { formatNumber(it) } ?: "-"
            val rsi = stock.lastRsi2?.let { formatDecimal1(it) } ?: "-"
            val sma200 = stock.lastSma200?.let { formatNumber(it) } ?: "-"
            when {
                selectedAlgorithms.size == 1 && AlgorithmType.MA_CROSS in selectedAlgorithms ->
                    Text("5MA $ma5  ·  20MA $ma20  ·  종가 $close", style = MaterialTheme.typography.bodyMedium)
                selectedAlgorithms.size == 1 ->
                    Text("RSI(2) $rsi  ·  SMA200 $sma200  ·  종가 $close", style = MaterialTheme.typography.bodyMedium)
                else -> {
                    Text("5MA $ma5  ·  20MA $ma20", style = MaterialTheme.typography.bodyMedium)
                    Text("RSI(2) $rsi  ·  SMA200 $sma200  ·  종가 $close", style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (stock.lastUpdatedAt != null) {
                Text(
                    text = "업데이트 ${formatDateTime(stock.lastUpdatedAt)}",
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

