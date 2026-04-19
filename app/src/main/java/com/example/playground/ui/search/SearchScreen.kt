package com.example.playground.ui.search

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.playground.data.model.Market
import com.example.playground.data.model.StockSearchResult
import androidx.compose.material3.OutlinedButton

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    contentPadding: PaddingValues,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = "주식 검색",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("예: Samsung, AAPL, Tesla") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.onSubmitSearch() }),
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = viewModel::onSubmitSearch) {
                Text("검색")
            }
        }

        Spacer(Modifier.height(12.dp))

        when {
            state.loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                ErrorBlock(
                    error = state.error!!,
                    onRetry = viewModel::retry,
                )
            }
            state.results.isEmpty() -> {
                Text(
                    text = "검색어를 입력하고 '검색'을 눌러봐 🔎",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.results, key = { it.symbol }) { result ->
                        ResultCard(
                            result = result,
                            alreadyWatched = result.symbol in state.watchedSymbols,
                            onAdd = { viewModel.addToWatchlist(result) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultCard(
    result: StockSearchResult,
    alreadyWatched: Boolean,
    onAdd: () -> Unit,
) {
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
                    text = result.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${result.symbol} · ${result.exchange.ifBlank { "-" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                MarketChip(result.market)
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onAdd,
                enabled = !alreadyWatched,
            ) {
                Text(if (alreadyWatched) "등록됨" else "+관심")
            }
        }
    }
}

@Composable
private fun ErrorBlock(error: SearchError, onRetry: () -> Unit) {
    val (headline, detail) = when (error.type) {
        SearchErrorType.NETWORK -> "인터넷 연결을 확인해주세요" to error.message
        SearchErrorType.RATE_LIMIT -> "요청이 많아 잠시 후 다시 시도해주세요" to error.message
        SearchErrorType.OTHER -> "검색에 실패했어요" to error.message
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = headline,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onRetry) { Text("다시 시도") }
    }
}

@Composable
private fun MarketChip(market: Market) {
    val (label, color) = when (market) {
        Market.KR -> "KR" to MaterialTheme.colorScheme.tertiary
        Market.US -> "US" to MaterialTheme.colorScheme.primary
    }
    AssistChip(
        onClick = {},
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(labelColor = color),
    )
}
