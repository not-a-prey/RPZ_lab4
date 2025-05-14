package com.example.player

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.util.UUID


//Отримуємо ім'я файлу з URI
fun getFileName(context: Context, uri: Uri): String {
    var result = ""

    try {
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        result = it.getString(displayNameIndex)
                    }
                }
            }
        }

        if (result.isEmpty()) {
            result = uri.path?.let { path ->
                path.lastIndexOf('/').let { lastIndex ->
                    if (lastIndex != -1) path.substring(lastIndex + 1) else path
                }
            } ?: "Unknown"
        }
    } catch (e: Exception) {
        result = "Unknown"
    }

    return result
}

//Завантаження файлу з URL-адреси до сховища пристрою
//Повертає URI завантаженого файлу або нуль, якщо завантаження провалилось
suspend fun downloadFileToAppStorage(context: Context, url: String): Uri? = withContext(Dispatchers.IO) {
    try {
        val encodedFileName = url.substringAfterLast("/").ifEmpty { "download_${UUID.randomUUID()}" }

        val fileName = java.net.URLDecoder.decode(encodedFileName, "UTF-8")

        val isVideo = fileName.endsWith(".mp4", true) ||
                fileName.endsWith(".avi", true) ||
                fileName.endsWith(".mkv", true) ||
                fileName.endsWith(".webm", true)

        val mimeType = when {
            fileName.endsWith(".mp3", true) -> "audio/mpeg"
            fileName.endsWith(".wav", true) -> "audio/wav"
            fileName.endsWith(".ogg", true) -> "audio/ogg"
            fileName.endsWith(".mp4", true) -> "video/mp4"
            fileName.endsWith(".avi", true) -> "video/avi"
            fileName.endsWith(".mkv", true) -> "video/x-matroska"
            fileName.endsWith(".webm", true) -> "video/webm"
            isVideo -> "video/*"
            else -> "audio/*"
        }

        // Створюємо постійний файл
        val fileUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+ use MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.IS_PENDING, 1)

                // Set the appropriate collection based on file type
                if (isVideo) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/SimpleMediaPlayer")
                } else {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/SimpleMediaPlayer")
                }
            }

            val collection = if (isVideo) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }

            val itemUri = context.contentResolver.insert(collection, contentValues)

            // Download the file
            itemUri?.let { uri ->
                val connection = URL(url).openConnection()
                connection.connect()

                connection.getInputStream().use { input ->
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        input.copyTo(output)
                    }
                }

                // Now that we're finished, update the IS_PENDING flag
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }

            itemUri
        } else {
            // For older Android versions, save to app directory first
            val downloadsDir = getAppDownloadsDir(context)
            val file = File(downloadsDir, fileName)

            // Download the file
            val connection = URL(url).openConnection()
            connection.connect()

            connection.getInputStream().use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }



            file.toUri()
        }

        return@withContext fileUri
    } catch (e: IOException) {
        // Оброблюємо помилку
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
        return@withContext null
    }
}


//Папка завантажень у самому додатку
fun getAppDownloadsDir(context: Context): File {
    val downloadsDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        File(context.getExternalFilesDir(null), "downloads")
    } else {
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Player")
    }

    if (!downloadsDir.exists()) {
        downloadsDir.mkdirs()
    }

    return downloadsDir
}