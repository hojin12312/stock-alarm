package com.example.playground.ui.common

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.playground.data.model.MaStatus
import com.example.playground.ui.theme.AppColors

// BUY/SELL/대기 상태를 공통 AssistChip으로 표시. 대시보드·차트 화면 공용.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusBadge(status: MaStatus?) {
    val ext = AppColors.extended
    val (label, color) = when (status) {
        MaStatus.BUY -> "매수" to ext.buy
        MaStatus.SELL -> "매도" to ext.sell
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
