package org.example.annochat.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.example.annochat.data.repository.ChatRepository

/**
 * Platform-aware media component for rendering static and animated rich media (GIFs, WebP, images).
 * Respects aspect ratio, bounds to max dimensions, and provides smooth native playback without jitter.
 */
@Composable
expect fun AnimatedMediaImage(
    repository: ChatRepository? = null,
    pathOrUrl: String? = null,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    modifier: Modifier = Modifier,
    isGif: Boolean = false,
    localBytes: ByteArray? = null,
    onDimensionsKnown: ((width: Int, height: Int) -> Unit)? = null
)
