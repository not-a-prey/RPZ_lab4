package com.example.player

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MediaPlayerViewModel : ViewModel() {
    private val _mediaUri = MutableStateFlow<Uri?>(null)
    val mediaUri = _mediaUri.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _isVideo = MutableStateFlow(false)
    val isVideo = _isVideo.asStateFlow()

    private val _fileName = MutableStateFlow("")
    val fileName = _fileName.asStateFlow()

    fun setMediaUri(uri: Uri?, isVideo: Boolean, fileName: String = "") {
        _mediaUri.value = uri
        _isVideo.value = isVideo
        _fileName.value = fileName
    }

    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
    }

    fun setPlaying(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun resetPlayer() {
        _mediaUri.value = null
        _isPlaying.value = false
        _fileName.value = ""
    }
}