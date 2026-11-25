package org.example.annochat.presentation.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ContentInfoCompat
import androidx.core.view.OnReceiveContentListener
import androidx.core.view.ViewCompat
import java.io.ByteArrayOutputStream
import kotlin.math.max

@Composable
actual fun rememberImagePickerLauncher(
    onImagesPicked: (List<PickedImageItem>) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val pickedList = mutableListOf<PickedImageItem>()
            val contentResolver = context.contentResolver
            uris.forEachIndexed { index, uri ->
                try {
                    val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                    val isGif = mimeType.contains("gif", ignoreCase = true)
                    val rawBytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (rawBytes != null && rawBytes.isNotEmpty()) {
                        val finalBytes = if (!isGif && rawBytes.size > 1_500_000) {
                            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOptions)
                            val maxDim = max(boundsOptions.outWidth, boundsOptions.outHeight)
                            if (maxDim > 2048) {
                                var sampleSize = 1
                                while ((maxDim / sampleSize) > 2048) {
                                    sampleSize *= 2
                                }
                                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                                val bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
                                if (bitmap != null) {
                                    val out = ByteArrayOutputStream()
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                                    bitmap.recycle()
                                    out.toByteArray()
                                } else rawBytes
                            } else rawBytes
                        } else rawBytes

                        val ext = when {
                            mimeType.contains("png") -> "png"
                            isGif -> "gif"
                            mimeType.contains("webp") -> "webp"
                            else -> "jpg"
                        }
                        val fileName = "img_${System.currentTimeMillis()}_$index.$ext"
                        pickedList.add(PickedImageItem(finalBytes, fileName, mimeType, uri.toString()))
                    }
                } catch (_: Throwable) {
                    // Ignore single item failure
                }
            }
            if (pickedList.isNotEmpty()) {
                onImagesPicked(pickedList)
            }
        }
    }

    return { launcher.launch("image/*") }
}

@Composable
actual fun RegisterKeyboardContentReceiver(
    onMediaReceived: (PickedImageItem) -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current

    DisposableEffect(view) {
        val listener = OnReceiveContentListener { _, payload ->
            val split = payload.partition { item -> item.uri != null }
            val uriContent = split.first
            val remaining = split.second

            if (uriContent != null) {
                val clip = uriContent.clip
                for (i in 0 until clip.itemCount) {
                    val uri = clip.getItemAt(i).uri
                    if (uri != null) {
                        try {
                            val contentResolver = context.contentResolver
                            val mimeType = contentResolver.getType(uri) ?: "image/gif"
                            val isGif = mimeType.contains("gif", ignoreCase = true)
                            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            if (bytes != null && bytes.isNotEmpty()) {
                                val ext = when {
                                    isGif -> "gif"
                                    mimeType.contains("png") -> "png"
                                    mimeType.contains("webp") -> "webp"
                                    else -> "jpg"
                                }
                                val prefix = if (isGif) "[GIF]" else if (mimeType.contains("webp")) "[Sticker]" else "media"
                                val fileName = "${prefix}_${System.currentTimeMillis()}.$ext"
                                onMediaReceived(
                                    PickedImageItem(
                                        bytes = bytes,
                                        fileName = fileName,
                                        mimeType = mimeType,
                                        uriString = uri.toString()
                                    )
                                )
                            }
                        } catch (_: Throwable) {
                            // Fallback gracefully if URI cannot be resolved
                        }
                    }
                }
            }
            remaining
        }

        ViewCompat.setOnReceiveContentListener(
            view,
            arrayOf("image/gif", "image/png", "image/jpeg", "image/webp"),
            listener
        )

        onDispose {
            ViewCompat.setOnReceiveContentListener(view, null, null)
        }
    }
}
