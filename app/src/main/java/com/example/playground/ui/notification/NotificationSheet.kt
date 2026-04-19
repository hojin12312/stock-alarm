package com.example.playground.ui.notification

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.playground.data.local.NotificationEntity
import com.example.playground.util.formatDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSheetContent(
    items: List<NotificationEntity>,
    filter: NotificationFilter,
    onTypeFilter: (String?) -> Unit,
    onStatusFilter: (String?) -> Unit,
    onMarketFilter: (String?) -> Unit,
    onDelete: (Long) -> Unit,
    onDeleteAll: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "알림",
                style = MaterialTheme.typography.titleLarge,
            )
            if (items.isNotEmpty()) {
                TextButton(onClick = onDeleteAll) {
                    Text("전체 삭제", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 필터 행
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // 알고리즘 타입 필터
            FilterChip(
                selected = filter.type == null,
                onClick = { onTypeFilter(null) },
                label = { Text("전체") },
            )
            FilterChip(
                selected = filter.type == "MA",
                onClick = { onTypeFilter("MA") },
                label = { Text("MA") },
            )
            FilterChip(
                selected = filter.type == "RSI",
                onClick = { onTypeFilter("RSI") },
                label = { Text("RSI") },
            )

            Spacer(Modifier.width(4.dp))

            // 매수/매도 필터
            FilterChip(
                selected = filter.status == "BUY",
                onClick = { onStatusFilter(if (filter.status == "BUY") null else "BUY") },
                label = { Text("매수") },
            )
            FilterChip(
                selected = filter.status == "SELL",
                onClick = { onStatusFilter(if (filter.status == "SELL") null else "SELL") },
                label = { Text("매도") },
            )

            Spacer(Modifier.width(4.dp))

            // 시장 필터
            FilterChip(
                selected = filter.market == "US",
                onClick = { onMarketFilter(if (filter.market == "US") null else "US") },
                label = { Text("미국") },
            )
            FilterChip(
                selected = filter.market == "KR",
                onClick = { onMarketFilter(if (filter.market == "KR") null else "KR") },
                label = { Text("한국") },
            )
        }

        Spacer(Modifier.height(8.dp))

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "알림이 없어",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false),
            ) {
                items(items, key = { it.id }) { item ->
                    SwipeToDeleteItem(
                        item = item,
                        onDelete = { onDelete(item.id) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteItem(
    item: NotificationEntity,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()

    if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
        LaunchedEffect(Unit) { onDelete() }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                    MaterialTheme.colorScheme.errorContainer
                else Color.Transparent,
                label = "bg",
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, MaterialTheme.shapes.medium)
                    .padding(end = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "삭제",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        NotificationCard(item)
    }
}

@Composable
private fun NotificationCard(item: NotificationEntity) {
    val statusColor = if (item.status == "BUY") Color(0xFF2E7D32) else Color(0xFFC62828)
    val typeLabel = if (item.type == "MA") "MA" else "RSI"
    val statusLabel = if (item.status == "BUY") "매수" else "매도"
    val marketLabel = when (item.market) {
        "US" -> "🇺🇸"
        "KR" -> "🇰🇷"
        else -> ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.read)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$marketLabel [$typeLabel $statusLabel] ${item.name}",
                    style = MaterialTheme.typography.titleSmall,
                    color = statusColor,
                )
                Text(
                    text = formatDateTime(item.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.detail,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

