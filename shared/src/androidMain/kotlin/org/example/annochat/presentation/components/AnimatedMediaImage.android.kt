package org.example.annochat.presentation.components

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.annochat.data.repository.ChatRepository
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.designsystem.AnonChatShapes
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer

/**
 * In-memory LRU byte cache for downloaded media assets to prevent repeated network requests.
 */
object MediaByteCache {
    private const val MAX_ENTRIES = 80
    private val cache = LinkedHashMap<String, ByteArray>(MAX_ENTRIES, 0.75f)

    fun get(key: String): ByteArray? {
        val bytes = cache.remove(key) ?: return null
        cache[key] = bytes
        return bytes
    }

    fun put(key: String, bytes: ByteArray) {
        cache.remove(key)
        if (cache.size >= MAX_ENTRIES) {
            val oldestKey = cache.keys.firstOrNull()
            if (oldestKey != null) {
                cache.remove(oldestKey)
            }
        }
        cache[key] = bytes
    }
}

@Composable
actual fun AnimatedMediaImage(
    repository: ChatRepository?,
    pathOrUrl: String?,
    contentDescription: String?,
    contentScale: ContentScale,
    modifier: Modifier,
    isGif: Boolean,
    localBytes: ByteArray?,
    onDimensionsKnown: ((width: Int, height: Int) -> Unit)?
) {
    val cacheKey = pathOrUrl ?: localBytes.hashCode().toString()
    var loadedBytes by remember(cacheKey) { mutableStateOf(localBytes ?: MediaByteCache.get(cacheKey)) }
    var isLoading by remember(cacheKey) { mutableStateOf(loadedBytes == null && !pathOrUrl.isNullOrBlank()) }
    var hasError by remember(cacheKey) { mutableStateOf(false) }

    var animDrawable by remember(cacheKey) { mutableStateOf<Drawable?>(null) }
    var staticBitmap by remember(cacheKey) { mutableStateOf<ImageBitmap?>(null) }

    val isActuallyGif = remember(isGif, pathOrUrl) {
        isGif || (pathOrUrl != null && (
            pathOrUrl.contains(".gif", ignoreCase = true) ||
            pathOrUrl.contains("tenor.com", ignoreCase = true) ||
            pathOrUrl.contains("giphy.com", ignoreCase = true)
        ))
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(cacheKey, loadedBytes) {
        if (loadedBytes == null && !pathOrUrl.isNullOrBlank()) {
            isLoading = true
            hasError = false
            withContext(Dispatchers.IO) {
                val finalBytes: ByteArray? = if (pathOrUrl.startsWith("content://") || pathOrUrl.startsWith("file://")) {
                    try {
                        val uri = android.net.Uri.parse(pathOrUrl)
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    } catch (_: Throwable) { null }
                } else {
                    var downloadedBytes: ByteArray? = null
                    if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
                        try {
                            var currentUrl = pathOrUrl
                            var redirects = 0
                            while (redirects < 4) {
                                val connection = URL(currentUrl).openConnection() as HttpURLConnection
                                connection.connectTimeout = 12000
                                connection.readTimeout = 15000
                                connection.instanceFollowRedirects = true
                                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                                connection.setRequestProperty("Accept", "image/gif,image/webp,image/*,*/*;q=0.8")
                                val code = connection.responseCode
                                if (code in 300..399) {
                                    val loc = connection.getHeaderField("Location")
                                    if (!loc.isNullOrBlank()) {
                                        currentUrl = loc
                                        redirects++
                                        continue
                                    }
                                }
                                downloadedBytes = connection.inputStream.use { it.readBytes() }
                                break
                            }
                        } catch (_: Throwable) {
                            downloadedBytes = null
                        }
                    }

                    if (downloadedBytes == null && repository != null) {
                        try {
                            val result = repository.getMediaBytes(pathOrUrl)
                            if (result.isSuccess) {
                                downloadedBytes = result.getOrThrow()
                            }
                        } catch (_: Throwable) {
                            downloadedBytes = null
                        }
                    }
                    downloadedBytes
                }

                if (finalBytes != null && finalBytes.isNotEmpty()) {
                    MediaByteCache.put(cacheKey, finalBytes)
                    loadedBytes = finalBytes
                } else {
                    hasError = true
                }
            }
            isLoading = false
        }

        loadedBytes?.let { bytes ->
            withContext(Dispatchers.Default) {
                try {
                    if (isActuallyGif && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(ByteBuffer.wrap(bytes))
                        val drawable = ImageDecoder.decodeDrawable(source) { _, info, _ ->
                            val size = info.size
                            if (size.width > 0 && size.height > 0) {
                                onDimensionsKnown?.invoke(size.width, size.height)
                            }
                        }
                        if (drawable is android.graphics.drawable.AnimatedImageDrawable) {
                            drawable.repeatCount = android.graphics.drawable.AnimatedImageDrawable.REPEAT_INFINITE
                        }
                        animDrawable = drawable
                        if (drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0) {
                            onDimensionsKnown?.invoke(drawable.intrinsicWidth, drawable.intrinsicHeight)
                        }
                    } else {
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bitmap != null) {
                            onDimensionsKnown?.invoke(bitmap.width, bitmap.height)
                            staticBitmap = bitmap.asImageBitmap()
                        } else {
                            hasError = true
                        }
                    }
                } catch (_: Throwable) {
                    hasError = true
                }
            }
        }
    }

    Box(
        modifier = modifier.clip(AnonChatShapes.card),
        contentAlignment = Alignment.Center
    ) {
        when {
            animDrawable != null -> {
                val drawable = animDrawable!!
                DisposableEffect(drawable) {
                    if (drawable is Animatable) {
                        drawable.start()
                    }
                    onDispose {
                        if (drawable is Animatable) {
                            drawable.stop()
                        }
                    }
                }
                AndroidView(
                    factory = { context ->
                        ImageView(context).apply {
                            this.scaleType = if (contentScale == ContentScale.Fit) {
                                ImageView.ScaleType.FIT_CENTER
                            } else {
                                ImageView.ScaleType.CENTER_CROP
                            }
                            setImageDrawable(drawable)
                            if (drawable is Animatable) {
                                (drawable as Animatable).start()
                            }
                        }
                    },
                    update = { view ->
                        view.scaleType = if (contentScale == ContentScale.Fit) {
                            ImageView.ScaleType.FIT_CENTER
                        } else {
                            ImageView.ScaleType.CENTER_CROP
                        }
                        if (view.drawable !== drawable) {
                            view.setImageDrawable(drawable)
                        }
                        if (drawable is Animatable && !(drawable as Animatable).isRunning) {
                            (drawable as Animatable).start()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            staticBitmap != null -> {
                Image(
                    bitmap = staticBitmap!!,
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
            }
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AnonChatColors.SurfaceSecondary),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = AnonChatColors.Primary
                    )
                }
            }
            hasError -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AnonChatColors.SurfaceTertiary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AnonChatIcons.Attachment,
                        contentDescription = "Failed to load",
                        tint = AnonChatColors.TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
