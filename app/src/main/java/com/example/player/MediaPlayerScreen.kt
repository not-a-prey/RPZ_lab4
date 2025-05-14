package com.example.player

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPlayerScreen(viewModel: MediaPlayerViewModel = viewModel()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val mediaUri by viewModel.mediaUri.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isVideo by viewModel.isVideo.collectAsState()
    val fileName by viewModel.fileName.collectAsState()

    var downloadUrl by remember { mutableStateOf("") }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }

    val permissionState = remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        permissionState.value = allGranted
    }

    // Попросити дозвіл на використання внутрішнього сховища
    LaunchedEffect(Unit) {
        val requiredPermissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.INTERNET
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET
            )
        }

        val allPermissionsGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        if (!allPermissionsGranted) {
            permissionLauncher.launch(requiredPermissions)
        } else {
            permissionState.value = true
        }
    }

    if (!permissionState.value) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "This app needs storage permissions to access media files.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(
                    onClick = {
                        permissionLauncher.launch(
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                arrayOf(
                                    Manifest.permission.READ_MEDIA_AUDIO,
                                    Manifest.permission.READ_MEDIA_VIDEO,
                                    Manifest.permission.INTERNET
                                )
                            } else {
                                arrayOf(
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.INTERNET
                                )
                            }
                        )
                    }
                ) {
                    Text("Grant Permissions")
                }
            }
        }
        return
    }

    val audioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val fileName = getFileName(context, uri)
            val fileNameWithoutExt = fileName.substringBeforeLast(".")
            viewModel.setMediaUri(it, false, fileNameWithoutExt)
        }
    }

    val videoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val fileName = getFileName(context, uri)
            val fileNameWithoutExt = fileName.substringBeforeLast(".")
            viewModel.setMediaUri(it, true, fileNameWithoutExt)
        }
    }

    Scaffold(
        topBar = {
            if (mediaUri != null) {
                TopAppBar(
                    title = { Text(fileName) },
                    actions = {
                        IconButton(onClick = { viewModel.resetPlayer() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        // Вигляд медіаплеєра
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (mediaUri != null) {
                    if (isVideo) {
                        VideoPlayer(
                            uri = mediaUri!!,
                            isPlaying = isPlaying,
                            onPlayPauseToggle = { viewModel.togglePlayPause() }
                        )
                    } else {
                        AudioPlayer(
                            uri = mediaUri!!,
                            isPlaying = isPlaying,
                            fileName = fileName,
                            onPlayPauseToggle = { viewModel.togglePlayPause() }
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Select a media file to play")
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (mediaUri == null) {
                    Button(
                        onClick = { audioLauncher.launch("audio/*") },
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                            Text("Select Audio", maxLines = 2)
                    }

                    Button(
                        onClick = { videoLauncher.launch("video/*") },
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                        Text("Select Video")
                    }

                    Button(
                        onClick = { showDownloadDialog = true },
                        enabled = !isDownloading
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download")
                    }
                }
                else {
                    Button(onClick = { audioLauncher.launch("audio/*") }) {
                        Text("Select Audio")
                    }

                    Button(onClick = { videoLauncher.launch("video/*") }) {
                        Text("Select Video")
                    }

                    Button(onClick = { viewModel.togglePlayPause() }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(if (isPlaying) "Pause" else "Play")
                        }
                    }
                }
            }
        }
    }

    // Діалог завантажень (Download Dialog)
    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadDialog = false },
            title = { Text("Download") },
            text = {
                Column {
                    TextField(
                        value = downloadUrl,
                        onValueChange = { downloadUrl = it },
                        label = { Text("URL") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter URL of audio/video file") }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Files will be saved to your device",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (downloadUrl.isNotEmpty()) {
                            isDownloading = true
                            showDownloadDialog = false

                            coroutineScope.launch {
                                val fileUri = downloadFileToAppStorage(context, downloadUrl)
                                isDownloading = false

                                // Якщо завантаження вдалося,
                                // то відразу переходимо на відповідний плеєр
                                fileUri?.let {
                                    val fileName = getFileName(context, it)
                                    val fileNameWithoutExt = fileName.substringBeforeLast(".")
                                    val isVideo = fileName.endsWith(".mp4", true) ||
                                            fileName.endsWith(".avi", true) ||
                                            fileName.endsWith(".mkv", true)

                                    viewModel.setMediaUri(it, isVideo, fileNameWithoutExt)
                                }
                            }
                        }
                    },
                    enabled = !isDownloading
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Download")
                    }
                }
            },
            dismissButton = {
                Button(onClick = { showDownloadDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}