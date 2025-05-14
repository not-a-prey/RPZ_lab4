package com.example.player

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay


@Composable
fun AudioPlayer(
    uri: Uri,
    isPlaying: Boolean,
    fileName: String,
    onPlayPauseToggle: () -> Unit
) {
    val context = LocalContext.current

    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var sliderPosition by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Ініціалізація ExoPlayer для аудіофайлів
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = isPlaying
        }
    }

    // Апдейтимо стан плеєра кожен раз коли стан isPlaying змінюється
    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying
    }

    // Змінюємо плеєри при зміні посилання
    LaunchedEffect(uri) {
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
    }

    // Стан повзунка
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            if (!isDragging && exoPlayer.duration > 0) {
                currentPosition = exoPlayer.currentPosition
                duration = exoPlayer.duration
                sliderPosition = if (duration > 0) currentPosition.toFloat() / duration else 0f
            }
            delay(500) // Update every half second
        }
    }

    LaunchedEffect(Unit) {
        while (duration <= 0) {
            if (exoPlayer.duration > 0) {
                duration = exoPlayer.duration
            }
            delay(100)
        }
    }

    // Спостерігаємо за станом програвання аудіо
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    onPlayPauseToggle()
                }
            }
        }

        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // UI аудіоплеєра
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ім'я файлу
        Text(
            text = fileName,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(bottom = 32.dp)
                .fillMaxWidth()
        )

        // повзунок часу
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(200.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                contentDescription = if (isPlaying) "Playing" else "Paused",
                modifier = Modifier.size(84.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // статус програвання аудіо
        Text(
            text = if (isPlaying) "Now Playing" else "Paused",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 10.dp, bottom = 15.dp)
        )

        // додаємо сам повзунок та час відтворення аудіо
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 8.dp)
        ) {
            // час
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatDuration(currentPosition))
                Text(formatDuration(duration))
            }

            // повзунок
            Slider(
                value = sliderPosition,
                onValueChange = {
                    isDragging = true
                    sliderPosition = it
                    currentPosition = (it * duration).toLong()
                },
                onValueChangeFinished = {
                    isDragging = false
                    exoPlayer.seekTo(currentPosition)
                },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

// Форматування часу мм:сс
fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}