package com.example.elabftwclient.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.elabftwclient.storage.ServerConfigStore

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val store = remember { ServerConfigStore(context) }

    var loaded by remember { mutableStateOf(false) }
    var serverUrl by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        serverUrl = store.getServerUrl()
        loaded = true
    }

    when {
        !loaded -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        serverUrl.isBlank() -> {
            SettingsScreen(
                initialUrl = "",
                showBackButton = false,
                onSaved = { url -> serverUrl = url },
                onBack = {}
            )
        }
        else -> {
            Box(Modifier.fillMaxSize()) {
                key(serverUrl) {
                    WebViewScreen(
                        serverUrl = serverUrl,
                        onOpenSettings = { showSettings = true }
                    )
                }
                if (showSettings) {
                    SettingsScreen(
                        initialUrl = serverUrl,
                        showBackButton = true,
                        onSaved = { url ->
                            serverUrl = url
                            showSettings = false
                        },
                        onBack = { showSettings = false }
                    )
                }
            }
        }
    }
}