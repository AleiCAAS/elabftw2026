package com.example.elabftwclient.webview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.result.ActivityResultLauncher

class WebViewFileChooser {
    private var callback: ValueCallback<Array<Uri>>? = null
    private var allowMultiple: Boolean = false
    private var acceptedTypes: Array<String> = emptyArray()

    fun prepareFileChooser(
        filePathCallback: ValueCallback<Array<Uri>>?,
        params: WebChromeClient.FileChooserParams?
    ) {
        cancelPendingCallback()
        callback = filePathCallback
        allowMultiple = params?.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE
        acceptedTypes = params?.acceptTypes ?: emptyArray()
    }

    fun openFileChooser(context: Context, launcher: ActivityResultLauncher<Intent>) {
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                type = "*/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple)
                val mimes = acceptedTypes.filter { it.isNotBlank() }.toTypedArray()
                if (mimes.isNotEmpty()) {
                    putExtra(Intent.EXTRA_MIME_TYPES, mimes)
                }
            }
            launcher.launch(intent)
        } catch (_: Exception) {
            cancelPendingCallback()
        }
    }

    fun handleActivityResult(context: Context, resultCode: Int, data: Intent?) {
        if (callback == null) return
        if (resultCode != Activity.RESULT_OK || data == null) {
            complete(null)
            return
        }
        val uris = mutableListOf<Uri>()
        data.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) {
                clip.getItemAt(i).uri?.let { uris.add(it) }
            }
        }
        if (uris.isEmpty()) {
            data.data?.let { uris.add(it) }
        }
        val safe = if (allowMultiple) uris.distinct() else uris.distinct().take(1)
        if (safe.isEmpty()) complete(null) else complete(safe.toTypedArray())
    }

    fun cancelPendingCallback() {
        complete(null)
    }

    private fun complete(result: Array<Uri>?) {
        val pending = callback
        callback = null
        pending?.onReceiveValue(result)
    }
}