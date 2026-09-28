package com.example.elabftwclient.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.elabftwclient.webview.WebViewDownloadHandler
import com.example.elabftwclient.webview.WebViewFileChooser

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    serverUrl: String,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val webViewRef = remember { arrayOfNulls<WebView>(1) }

    var progress by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var lastBackTime by remember { mutableLongStateOf(0L) }

    var pendingSsl by remember { mutableStateOf<Pair<SslErrorHandler, String>?>(null) }

    val fileChooser = remember { WebViewFileChooser() }
    val downloadHandler = remember(context) { WebViewDownloadHandler(context) }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        fileChooser.handleActivityResult(context, result.resultCode, result.data)
    }

    val homeUrl = remember(serverUrl) {
        val base = serverUrl.trimEnd('/')
        "$base/experiments.php"
    }

    DisposableEffect(lifecycleOwner, webViewRef) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    webViewRef[0]?.onPause()
                    CookieManager.getInstance().flush()
                }
                Lifecycle.Event.ON_RESUME -> {
                    webViewRef[0]?.onResume()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    webViewRef[0]?.onPause()
                    CookieManager.getInstance().flush()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler(enabled = true) {
        when {
            pendingSsl != null -> {
                pendingSsl!!.first.cancel()
                pendingSsl = null
            }
            showMenu -> {
                showMenu = false
            }
            webViewRef[0]?.canGoBack() == true -> {
                webViewRef[0]?.goBack()
            }
            else -> {
                val now = System.currentTimeMillis()
                if (now - lastBackTime < 2000) {
                    (context as? Activity)?.finish()
                } else {
                    lastBackTime = now
                    Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true

                        useWideViewPort = true
                        loadWithOverviewMode = false

                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false

                        allowFileAccess = false
                        allowContentAccess = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        mediaPlaybackRequiresUserGesture = false
                        javaScriptCanOpenWindowsAutomatically = false
                        setSupportMultipleWindows(false)
                        textZoom = 100
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }

                    CookieManager.getInstance().apply {
                        setAcceptCookie(true)
                        setAcceptThirdPartyCookies(this@apply, true)
                    }

                    webViewClient = object : WebViewClient() {

                        override fun onPageStarted(
                            view: WebView?,
                            url: String?,
                            favicon: Bitmap?
                        ) {
                            super.onPageStarted(view, url, favicon)
                            loading = true
                            hasError = false
                            errorText = null
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            loading = false
                            progress = 100
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val u = request?.url?.toString() ?: return false
                            return if (u.startsWith("http://") || u.startsWith("https://")) {
                                false
                            } else {
                                try {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(u))
                                    )
                                } catch (_: Exception) {
                                    Toast.makeText(
                                        context,
                                        "No app to open this link",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                true
                            }
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                hasError = true
                                loading = false
                                errorText = error?.description?.toString()
                                    ?: "Unable to load page"
                            }
                        }

                        override fun onReceivedHttpError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            errorResponse: WebResourceResponse?
                        ) {
                            super.onReceivedHttpError(view, request, errorResponse)
                            if (request?.isForMainFrame != true) return
                            val code = errorResponse?.statusCode ?: return
                            if (code >= 400) {
                                hasError = true
                                loading = false
                                errorText = "Server returned HTTP $code"
                            }
                        }

                        override fun onReceivedSslError(
                            view: WebView?,
                            handler: SslErrorHandler?,
                            error: SslError?
                        ) {
                            if (handler == null) return
                            val url = error?.url ?: ""
                            pendingSsl = handler to url
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                            if (newProgress >= 100) loading = false
                        }

                        override fun onShowFileChooser(
                            webView: WebView?,
                            filePathCallback: ValueCallback<Array<Uri>>?,
                            fileChooserParams: FileChooserParams?
                        ): Boolean {
                            fileChooser.prepareFileChooser(filePathCallback, fileChooserParams)
                            fileChooser.openFileChooser(context, fileChooserLauncher)
                            return true
                        }
                    }

                    setDownloadListener(downloadHandler)

                    loadUrl(homeUrl)
                    webViewRef[0] = this
                }
            }
        )

        if (loading && progress in 1..99 && !hasError) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            )
        }

        if (hasError) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Unable to load page",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = errorText ?: "Check your network and server address",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        hasError = false
                        loading = true
                        progress = 0
                        webViewRef[0]?.reload()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Retry")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        hasError = false
                        loading = true
                        progress = 0
                        webViewRef[0]?.loadUrl(homeUrl)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Back to Home")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        hasError = false
                        loading = true
                        progress = 0
                        onOpenSettings()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Change Server URL")
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = { showMenu = !showMenu },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Menu",
                    modifier = Modifier.size(22.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Reload") },
                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        hasError = false
                        loading = true
                        progress = 0
                        webViewRef[0]?.reload()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Home") },
                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        hasError = false
                        loading = true
                        progress = 0
                        webViewRef[0]?.loadUrl(homeUrl)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Settings") },
                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onOpenSettings()
                    }
                )
            }
        }
    }

    val ssl = pendingSsl
    if (ssl != null) {
        AlertDialog(
            onDismissRequest = {
                ssl.first.cancel()
                pendingSsl = null
            },
            title = { Text("SSL Certificate Warning") },
            text = {
                Text(
                    "The server certificate cannot be verified:\n\n" +
                            ssl.second + "\n\n" +
                            "This is common for internal lab servers with self-signed certificates. " +
                            "Proceed only if you trust this server."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    ssl.first.proceed()
                    pendingSsl = null
                }) { Text("Proceed") }
            },
            dismissButton = {
                TextButton(onClick = {
                    ssl.first.cancel()
                    pendingSsl = null
                }) { Text("Cancel") }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            fileChooser.cancelPendingCallback()
            CookieManager.getInstance().flush()

            webViewRef[0]?.apply {
                stopLoading()
                onPause()
                loadUrl("about:blank")
                webChromeClient = null
                webViewClient = WebViewClient()
                (parent as? ViewGroup)?.removeView(this)
                removeAllViews()
                destroy()
            }
            webViewRef[0] = null
        }
    }
}