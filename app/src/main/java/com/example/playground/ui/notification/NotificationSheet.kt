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
import com.example.playground.ui.theme.AppColors
import com.example.playground.util.formatDateTime
import java.util.Calendar

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
    val filterActive = filter.type != null || filter.status != null || filter.market != null

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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
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
            FilterChip(
                selected = filter.type == "MA_EXTREMA",
                onClick = { onTypeFilter("MA_EXTREMA") },
                label = { Text("5MA 극점") },
            )

            Spacer(Modifier.width(4.dp))

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
                    text = if (filterActive) "해당 조건의 알림이 없어" else "알림이 없어",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val grouped = groupByDate(items)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false),
            ) {
                grouped.forEach { (label, groupItems) ->
                    item(key = "header:$label") {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                        )
                    }
                    items(groupItems.size, key = { idx -> "item:${groupItems[idx].id}" }) { idx ->
                        val item = groupItems[idx]
                        SwipeToDeleteItem(
                            item = item,
                            onDelete = { onDelete(item.id) },
                        )
                    }
                }
            }
        }
    }
}

private enum class DateGroup(val order: Int, val label: String) {
    TODAY(0, "오늘"),
    YESTERDAY(1, "어제"),
    THIS_WEEK(2, "이번 주"),
    LAST_WEEK(3, "지난 주"),
    OLDER(4, "그 이전"),
}

// 현재 시각 기준으로 알림을 오늘/어제/이번 주/지난 주/그 이전으로 분류.
private fun groupByDate(items: List<NotificationEntity>): List<Pair<String, List<NotificationEntity>>> {
    val now = Calendar.getInstance()
    val todayStart = startOfDay(now)
    val yesterdayStart = todayStart - ONE_DAY_MS
    val weekStart = startOfWeek(now)
    val lastWeekStart = weekStart - ONE_WEEK_MS

    val grouped = items.groupBy { item ->
        val t = item.createdAt
        when {
            t >= todayStart -> DateGroup.TODAY
            t >= yesterdayStart -> DateGroup.YESTERDAY
            t >= weekStart -> DateGroup.THIS_WEEK
            t >= lastWeekStart -> DateGroup.LAST_WEEK
            else -> DateGroup.OLDER
        }
    }
    return DateGroup.values()
        .sortedBy { it.order }
        .mapNotNull { group -> grouped[group]?.let { group.label to it } }
}

private fun startOfDay(cal: Calendar): Long {
    val c = cal.clone() as Calendar
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

private fun startOfWeek(cal: Calendar): Long {
    val c = cal.clone() as Calendar
    c.set(Calendar.DAY_OF_WEEK, c.firstDayOfWeek)
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

private const val ONE_DAY_MS = 24L * 60 * 60 * 1000
private const val ONE_WEEK_MS = 7L * ONE_DAY_MS

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
    val ext = AppColors.extended
    val isExtrema = item.type == "MA_EXTREMA"
    val isBullish = if (isExtrema) item.status == "LOW" else item.status == "BUY"
    val statusColor = if (isBullish) ext.buy else ext.sell
    val typeLabel = when (item.type) {
        "MA" -> "MA"
        "RSI" -> "RSI"
        "MA_EXTREMA" -> "5MA"
        else -> item.type
    }
    val statusLabel = when {
        isExtrema && item.status == "LOW" -> "저점"
        isExtrema && item.status == "HIGH" -> "고점"
        item.status == "BUY" -> "매수"
        else -> "매도"
    }
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
