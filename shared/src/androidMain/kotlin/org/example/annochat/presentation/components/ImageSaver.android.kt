package org.example.annochat.presentation.components

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
actual fun rememberImageSaver(): (bytes: ByteArray, fileName: String, onResult: (success: Boolean, message: String?) -> Unit) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { bytes, fileName, onResult ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val resolver = context.contentResolver
                    val cleanFileName = if (fileName.contains(".")) fileName else "$fileName.jpg"
                    val mimeType = when {
                        cleanFileName.endsWith(".png", ignoreCase = true) -> "image/png"
                        cleanFileName.endsWith(".webp", ignoreCase = true) -> "image/webp"
                        cleanFileName.endsWith(".gif", ignoreCase = true) -> "image/gif"
                        else -> "image/jpeg"
                    }

                    var savedSuccessfully = false
                    var resultMsg = "Image saved to gallery"

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val contentValues = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, cleanFileName)
                            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AnnoChat")
                            put(MediaStore.Images.Media.IS_PENDING, 1)
                        }

                        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                            ?: resolver.insert(MediaStore.Images.Media.getContentUri("external"), contentValues)

                        if (uri != null) {
                            resolver.openOutputStream(uri)?.use { stream ->
                                stream.write(bytes)
                                stream.flush()
                            }
                            contentValues.clear()
                            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                            resolver.update(uri, contentValues, null, null)
                            savedSuccessfully = true
                            resultMsg = "Image saved to Pictures/AnnoChat"
                        }
                    }

                    if (!savedSuccessfully) {
                        // Fallback writing to standard Pictures directory
                        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                        val annoChatDir = File(picturesDir, "AnnoChat")
                        if (!annoChatDir.exists()) annoChatDir.mkdirs()
                        val targetFile = File(annoChatDir, cleanFileName)
                        FileOutputStream(targetFile).use { fos ->
                            fos.write(bytes)
                            fos.flush()
                        }

                        // Trigger media scan so it appears in device photo gallery
                        try {
                            val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                                data = Uri.fromFile(targetFile)
                            }
                            context.sendBroadcast(scanIntent)
                        } catch (_: Throwable) {}

                        savedSuccessfully = true
                        resultMsg = "Image saved to gallery (${targetFile.name})"
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, resultMsg, Toast.LENGTH_SHORT).show()
                        onResult(true, resultMsg)
                    }
                } catch (e: Exception) {
                    val err = "Failed to save: ${e.message ?: "Unknown error"}"
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                        onResult(false, err)
                    }
                }
            }
        }
    }
}
