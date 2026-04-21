package com.example.playground.ui.chart

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.playground.data.model.AlgorithmType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    viewModel: ChartViewModel,
    algorithmType: AlgorithmType = AlgorithmType.MA_CROSS,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // 초기값은 진입 시 전달된 알고리즘. 화면 안에서 토글 가능, 회전 시 유지.
    var selectedAlgorithms by rememberSaveable(stateSaver = AlgorithmSetSaver) {
        mutableStateOf(setOf(algorithmType))
    }

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
                    selectedAlgorithms = selectedAlgorithms,
                    onToggleAlgorithm = { type ->
                        val next = if (type in selectedAlgorithms) {
                            selectedAlgorithms - type
                        } else {
                            selectedAlgorithms + type
                        }
                        if (next.isNotEmpty()) selectedAlgorithms = next
                    },
                    range = state.range,
                    onRangeSelect = viewModel::selectRange,
                )
            }
        }
    }
}

private val AlgorithmSetSaver: Saver<Set<AlgorithmType>, List<String>> = Saver(
    save = { it.map(AlgorithmType::name) },
    restore = { it.map(AlgorithmType::valueOf).toSet() },
)
