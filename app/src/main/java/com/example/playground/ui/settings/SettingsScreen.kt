package com.example.playground.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.playground.BuildConfig
import com.example.playground.data.source.DataSourceId
import com.example.playground.di.ServiceLocator
import com.example.playground.ui.update.UpdateDialog
import com.example.playground.util.formatClock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    contentPadding: PaddingValues,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message, state.error) {
        val msg = state.error ?: state.message
        if (msg != null) {
            snackbar.showSnackbar(msg)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        modifier = Modifier.padding(contentPadding),
        topBar = { TopAppBar(title = { Text("설정") }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("데이터 소스", style = MaterialTheme.typography.titleMedium)
            Text(
                "시세·차트·알림에 쓰일 소스를 골라줘. 종목 검색은 항상 Yahoo Finance로 동작해.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(),
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    DataSourceOption(
                        label = "Yahoo Finance (기본)",
                        description = "비공식 API, 인증 불필요",
                        selected = state.dataSource == DataSourceId.YAHOO,
                        onClick = { viewModel.selectDataSource(DataSourceId.YAHOO) },
                    )
                    DataSourceOption(
                        label = "한국투자증권 (KIS)",
                        description = "공식 Open API, AppKey/Secret 필요",
                        selected = state.dataSource == DataSourceId.KIS,
                        onClick = { viewModel.selectDataSource(DataSourceId.KIS) },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("앱 정보", style = MaterialTheme.typography.titleMedium)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "현재 버전: v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = { viewModel.checkForUpdate() },
                        enabled = !state.checkingUpdate,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.checkingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(16.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.padding(horizontal = 4.dp))
                        }
                        Text("업데이트 확인")
                    }
                }
            }

            state.pendingUpdate?.let { info ->
                val context = LocalContext.current
                UpdateDialog(
                    info = info,
                    onConfirm = {
                        ServiceLocator.provideUpdateInstaller(context).start(info)
                        viewModel.dismissUpdate()
                    },
                    onDismiss = { viewModel.dismissUpdate() },
                )
            }

            Spacer(Modifier.height(8.dp))

            if (state.dataSource == DataSourceId.KIS) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("KIS 인증 정보", fontWeight = FontWeight.SemiBold)
                        Text(
                            "키는 기기 Keystore로 암호화되어 저장돼. 토큰 원문은 메모리에만 유지돼.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        val stored = state.kisHasStoredKey
                        if (stored) {
                            Text(
                                text = buildString {
                                    append("저장된 AppKey: ")
                                    if (state.kisKeySuffix.isNotEmpty()) {
                                        append("****_****_")
                                        append(state.kisKeySuffix)
                                    } else {
                                        append("✓")
                                    }
                                    if (state.kisTokenExpiresAt > System.currentTimeMillis()) {
                                        append(" · 토큰 만료 ")
                                        append(formatClock(state.kisTokenExpiresAt))
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                            )
                        } else {
                            Text(
                                "저장된 키 없음",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }

                        var keyVisible by remember { mutableStateOf(false) }
                        var secretVisible by remember { mutableStateOf(false) }

                        OutlinedTextField(
                            value = state.kisKeyInput,
                            onValueChange = viewModel::onKeyInput,
                            label = { Text("AppKey") },
                            singleLine = true,
                            visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { keyVisible = !keyVisible }) {
                                    Icon(
                                        imageVector = if (keyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (keyVisible) "AppKey 가리기" else "AppKey 보기",
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.busy,
                        )
                        OutlinedTextField(
                            value = state.kisSecretInput,
                            onValueChange = viewModel::onSecretInput,
                            label = { Text("AppSecret") },
                            singleLine = true,
                            visualTransformation = if (secretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { secretVisible = !secretVisible }) {
                                    Icon(
                                        imageVector = if (secretVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (secretVisible) "AppSecret 가리기" else "AppSecret 보기",
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.busy,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(
                                onClick = viewModel::saveAndTest,
                                enabled = !state.busy,
                            ) {
                                if (state.busy) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.height(16.dp),
                                        strokeWidth = 2.dp,
                                    )
                                    Spacer(Modifier.padding(horizontal = 4.dp))
                                }
                                Text("저장 & 테스트")
                            }
                            if (stored) {
                                TextButton(
                                    onClick = viewModel::clearCredentials,
                                    enabled = !state.busy,
                                ) { Text("키 삭제") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DataSourceOption(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.padding(horizontal = 8.dp))
        Column {
            Text(label, fontWeight = FontWeight.Medium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

