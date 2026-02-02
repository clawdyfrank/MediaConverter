package com.frankycranky

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.frankycranky.ui.theme.MediaConverterTheme
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.Session
import com.arthenica.ffmpegkit.Statistics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

private const val TAG = "MediaConverter"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MediaConverterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MediaConverterApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaConverterApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedVideoName by remember { mutableStateOf("") }
    var selectedFormat by remember { mutableStateOf("mp3") }
    var selectedBitrate by remember { mutableStateOf("192k") }
    var isConverting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Select a video to start") }
    var progress by remember { mutableFloatStateOf(0f) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedVideoUri = uri
        selectedVideoName = uri?.let { getFileName(context, it) } ?: ""
        statusMessage = if (uri != null) "Video selected: $selectedVideoName" else "Selection cancelled"
        progress = 0f
    }

    val formats = listOf("mp3", "m4a", "wav")
    val bitrates = listOf("128k", "192k", "256k", "320k")
    var formatExpanded by remember { mutableStateOf(false) }
    var bitrateExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Media Converter") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (selectedVideoName.isEmpty()) "No file selected" else "File: $selectedVideoName",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (isConverting || progress > 0f) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = "Progress: ${(progress * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }

            Button(
                onClick = { videoPickerLauncher.launch("video/*") },
                enabled = !isConverting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select Video")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    ExposedDropdownMenuBox(
                        expanded = formatExpanded,
                        onExpandedChange = { formatExpanded = !formatExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedFormat.uppercase(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Format") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = formatExpanded,
                            onDismissRequest = { formatExpanded = false }
                        ) {
                            formats.forEach { format ->
                                DropdownMenuItem(
                                    text = { Text(format.uppercase()) },
                                    onClick = {
                                        selectedFormat = format
                                        formatExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Box(modifier = Modifier.weight(1f)) {
                    ExposedDropdownMenuBox(
                        expanded = bitrateExpanded,
                        onExpandedChange = { bitrateExpanded = !bitrateExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedBitrate,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Bitrate") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bitrateExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = bitrateExpanded,
                            onDismissRequest = { bitrateExpanded = false }
                        ) {
                            bitrates.forEach { bitrate ->
                                DropdownMenuItem(
                                    text = { Text(bitrate) },
                                    onClick = {
                                        selectedBitrate = bitrate
                                        bitrateExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    selectedVideoUri?.let { uri ->
                        isConverting = true
                        progress = 0f
                        statusMessage = "Starting conversion..."
                        convertVideoToAudio(context, coroutineScope, uri, selectedVideoName, selectedFormat, selectedBitrate, 
                            onProgress = { p -> progress = p },
                            onResult = { success, message ->
                                isConverting = false
                                statusMessage = message
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                },
                enabled = selectedVideoUri != null && !isConverting,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                if (isConverting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Converting...")
                } else {
                    Text("Convert to Audio")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = statusMessage, style = MaterialTheme.typography.bodySmall)
        }
    }
}

fun convertVideoToAudio(
    context: Context,
    scope: CoroutineScope,
    videoUri: Uri,
    videoName: String,
    format: String,
    bitrate: String,
    onProgress: (Float) -> Unit,
    onResult: (Boolean, String) -> Unit
) {
    scope.launch(Dispatchers.IO) {
        val inputFilePath = copyUriToCache(context, videoUri)
        if (inputFilePath == null) {
            scope.launch(Dispatchers.Main) { onResult(false, "Failed to process input file") }
            return@launch
        }

        val outputDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
            "Convert"
        )
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val outputFileName = videoName.substringBeforeLast(".") + "_" + System.currentTimeMillis() + "." + format
        val outputFile = File(outputDir, outputFileName)
        val outputFilePath = outputFile.absolutePath

        Log.d(TAG, "Input: $inputFilePath")
        Log.d(TAG, "Output: $outputFilePath")

        FFprobeKit.getMediaInformationAsync(inputFilePath) { infoSession ->
            val mediaInformation = infoSession.mediaInformation
            val duration = mediaInformation?.duration?.toDouble() ?: 1.0
            
            val audioCodec = when (format) {
                "mp3" -> "libmp3lame"
                "m4a" -> "aac"
                else -> "pcm_s16le"
            }
            
            // Simplified command with logs
            val command = if (format == "wav") {
                "-i \"$inputFilePath\" -vn -acodec $audioCodec \"$outputFilePath\" -y"
            } else {
                "-i \"$inputFilePath\" -vn -acodec $audioCodec -ab $bitrate \"$outputFilePath\" -y"
            }

            Log.d(TAG, "Executing command: $command")

            FFmpegKit.executeAsync(command, { conversionSession ->
                val returnCode = conversionSession.returnCode
                val isSuccess = ReturnCode.isSuccess(returnCode)
                
                val logs = conversionSession.allLogsAsString
                Log.d(TAG, "FFmpeg Logs: $logs")

                scope.launch(Dispatchers.Main) {
                    if (isSuccess) {
                        onProgress(1f)
                        onResult(true, "Success! Saved to App Music folder")
                    } else {
                        val failMsg = conversionSession.failStackTrace ?: "Unknown error (check logcat)"
                        onResult(false, "Failed: $failMsg")
                    }
                }
                File(inputFilePath).delete()
            }, { log ->
                Log.d(TAG, "FFmpeg Output: ${log.message}")
            }, { statistics: Statistics ->
                val timeInMilliseconds = statistics.time
                if (timeInMilliseconds > 0) {
                    val p = (timeInMilliseconds / (duration * 1000)).toFloat()
                    scope.launch(Dispatchers.Main) {
                        onProgress(p.coerceIn(0f, 0.99f))
                    }
                }
            })
        }
    }
}

fun copyUriToCache(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val tempFile = File(context.cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")
        val outputStream = FileOutputStream(tempFile)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        tempFile.absolutePath
    } catch (e: Exception) {
        Log.e(TAG, "Cache error", e)
        null
    }
}

fun getFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) result = cursor.getString(index)
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "video_file"
}
