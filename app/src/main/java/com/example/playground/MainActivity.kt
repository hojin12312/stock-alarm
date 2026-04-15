package com.example.playground

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.playground.data.update.UpdateInfo
import com.example.playground.di.ServiceLocator
import com.example.playground.ui.nav.PlaygroundApp
import com.example.playground.ui.update.UpdateDialog

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            var pendingUpdate by remember { mutableStateOf<UpdateInfo?>(null) }

            LaunchedEffect(Unit) {
                pendingUpdate = ServiceLocator.provideUpdateChecker().check()
            }

            Box(modifier = Modifier.fillMaxSize()) {
                PlaygroundApp()
                pendingUpdate?.let { info ->
                    UpdateDialog(
                        info = info,
                        onConfirm = {
                            ServiceLocator.provideUpdateInstaller(this@MainActivity).start(info)
                            pendingUpdate = null
                        },
                        onDismiss = { pendingUpdate = null },
                    )
                }
            }
        }
    }
}
