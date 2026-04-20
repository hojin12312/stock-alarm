package com.example.playground.ui.update

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.playground.data.update.UpdateInfo

private const val RELEASES_URL = "https://github.com/hojin12312/stock-alarm/releases"

@Composable
fun UpdateDialog(
    info: UpdateInfo,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 버전이 나왔어요") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text("v${info.versionName} 으로 업데이트할 수 있어요.")
                if (info.notes.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(info.notes)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "📜 전체 업데이트 히스토리 보기",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = TextDecoration.Underline,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        },
                )
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
