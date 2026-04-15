package com.example.playground.ui.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.playground.data.update.UpdateInfo

@Composable
fun UpdateDialog(
    info: UpdateInfo,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 버전이 나왔어요") },
        text = {
            Column(modifier = Modifier) {
                Text("v${info.versionName} 으로 업데이트할 수 있어요.")
                if (info.notes.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(info.notes)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("지금 업데이트") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("나중에") }
        },
    )
}
