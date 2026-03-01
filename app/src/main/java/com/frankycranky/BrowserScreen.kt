package com.frankycranky

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.BackHandler
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen() {
    var url by remember { mutableStateOf("https://ww4.fmovies.co/") }
    var detectedVideoUrl by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // To handle back navigation in WebView, we need a reference
    var webView: WebView? by remember { mutableStateOf(null) }

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }

    if (showDialog && detectedVideoUrl != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Video Stream Detected!") },
            text = { Text("Do you want to download this video?\n\nURL: ${detectedVideoUrl?.take(50)}...") },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        downloadVideo(context, coroutineScope, detectedVideoUrl!!, 
                            onProgress = { msg -> downloadProgress = msg },
                            onComplete = { success, msg -> 
                                isDownloading = false
                                downloadProgress = msg
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        )
                        isDownloading = true
                        downloadProgress = "Starting download..."
                    }
                ) {
                    Text("Download")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isDownloading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = downloadProgress,
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }

        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webView = this
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val requestUrl = request?.url.toString()
                            
                            // Simple heuristic for video streams
                            if (requestUrl.contains(".m3u8") || requestUrl.contains(".mp4")) {
                                // Use a simple debounce or check if we already detected it recently
                                if (detectedVideoUrl != requestUrl) {
                                    Log.d("BrowserScreen", "Video Detected: $requestUrl")
                                    // Update state on UI thread
                                    view?.post {
                                        detectedVideoUrl = requestUrl
                                        showDialog = true
                                    }
                                }
                            }
                            return super.shouldInterceptRequest(view, request)
                        }
                    }
                    loadUrl(url)
                }
            },
            update = { view ->
                // Update logic if needed when url changes from outside
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

fun downloadVideo(
    context: Context,
    scope: CoroutineScope,
    videoUrl: String,
    onProgress: (String) -> Unit,
    onComplete: (Boolean, String) -> Unit
) {
    scope.launch(Dispatchers.IO) {
        val outputDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "MediaConverter")
        if (!outputDir.exists()) outputDir.mkdirs()
        
        val timestamp = System.currentTimeMillis()
        val outputFile = File(outputDir, "download_$timestamp.mp4")
        val outputAppPath = outputFile.absolutePath

        // FFmpeg command to download stream
        // User-Agent and headers might be needed, but let's try basic first
        val command = "-i \"$videoUrl\" -c copy -bsf:a aac_adtstoasc \"$outputAppPath\" -y"
        
        Log.d("BrowserScreen", "Download Command: $command")
        
        FFmpegKit.executeAsync(command, { session ->
            val returnCode = session.returnCode
            val isSuccess = ReturnCode.isSuccess(returnCode)
            scope.launch(Dispatchers.Main) {
                if (isSuccess) {
                    onComplete(true, "Downloaded to Movies/MediaConverter")
                } else {
                    onComplete(false, "Download failed: ${session.failStackTrace}")
                }
            }
        }, { log -> 
            // Log update
        }, { stats ->
            scope.launch(Dispatchers.Main) {
                 val time = stats.time / 1000
                 onProgress("Downloading... Time: ${time}s")
            }
        })
    }
}
