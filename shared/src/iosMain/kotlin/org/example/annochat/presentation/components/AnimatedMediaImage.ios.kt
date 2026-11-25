package org.example.annochat.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.annochat.data.repository.ChatRepository
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.designsystem.AnonChatShapes

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
    var bitmap by remember(pathOrUrl, localBytes) {
        mutableStateOf<ImageBitmap?>(
            if (localBytes != null) decodeImageBitmap(localBytes) else null
        )
    }
    var isLoading by remember(pathOrUrl, localBytes) {
        mutableStateOf(bitmap == null && !pathOrUrl.isNullOrBlank())
    }
    var hasError by remember(pathOrUrl, localBytes) { mutableStateOf(false) }

    LaunchedEffect(pathOrUrl, localBytes) {
        if (bitmap == null && !pathOrUrl.isNullOrBlank()) {
            if (repository != null) {
                isLoading = true
                hasError = false
                withContext(Dispatchers.Default) {
                    val result = repository.getMediaBytes(pathOrUrl)
                    if (result.isSuccess) {
                        val decoded = decodeImageBitmap(result.getOrThrow())
                        if (decoded != null) {
                            bitmap = decoded
                        } else {
                            hasError = true
                        }
                    } else {
                        hasError = true
                    }
                }
                isLoading = false
            } else {
                hasError = true
            }
        }
    }

    Box(
        modifier = modifier.clip(AnonChatShapes.card),
        contentAlignment = Alignment.Center
    ) {
        when {
            bitmap != null -> {
                Image(
                    bitmap = bitmap!!,
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
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AnonChatColors.SurfaceTertiary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AnonChatIcons.Attachment,
                        contentDescription = null,
                        tint = AnonChatColors.TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
