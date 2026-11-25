package org.example.annochat.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.example.annochat.data.repository.ChatRepository
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.designsystem.AnonChatShapes
import org.example.annochat.presentation.designsystem.AnonChatSpacing
import org.example.annochat.presentation.navigation.BackHandler

/**
 * Data bundle for presenting an image in the full screen lightbox viewer.
 */
data class FullScreenImageData(
    val pathOrUrl: String,
    val imageName: String? = null,
    val caption: String? = null,
    val localBytes: ByteArray? = null
)

/**
 * Full-Screen Image Lightbox Viewer with pinch-to-zoom, pan, double-tap zoom toggle,
 * device download/saving, and optional caption display.
 */
@Composable
fun FullScreenImageViewerModal(
    imageData: FullScreenImageData,
    repository: ChatRepository? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rawBytes by remember(imageData) { mutableStateOf<ByteArray?>(imageData.localBytes) }
    var bitmap by remember(imageData) {
        mutableStateOf<ImageBitmap?>(
            imageData.localBytes?.let { decodeImageBitmap(it) } ?: ImageBitmapCache.get(imageData.pathOrUrl)
        )
    }
    var isLoading by remember(imageData) { mutableStateOf(bitmap == null) }
    var hasError by remember(imageData) { mutableStateOf(false) }

    // Zoom & Pan state
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    var toastMessage by remember { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val imageSaver = rememberImageSaver()

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2800)
            toastMessage = null
        }
    }

    // Always fetch raw bytes if missing (needed for saving to gallery)
    LaunchedEffect(imageData.pathOrUrl) {
        if (rawBytes == null && imageData.pathOrUrl.isNotBlank() && repository != null) {
            if (bitmap == null) isLoading = true
            withContext(Dispatchers.Default) {
                val res = repository.getMediaBytes(imageData.pathOrUrl)
                if (res.isSuccess) {
                    val bytes = res.getOrThrow()
                    rawBytes = bytes
                    if (bitmap == null) {
                        val decoded = decodeImageBitmap(bytes)
                        if (decoded != null) {
                            ImageBitmapCache.put(imageData.pathOrUrl, decoded)
                            bitmap = decoded
                        } else {
                            hasError = true
                        }
                    }
                } else {
                    if (bitmap == null) hasError = true
                }
            }
            isLoading = false
        }
    }

    BackHandler(enabled = true, onBack = onDismiss)
    org.example.annochat.AllowScreenRotationWhileActive()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // ─────────────────────────────────────────────────────────────
            // 1. ZOOMABLE & PANNABLE IMAGE VIEW
            // ─────────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.2f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val targetScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = targetScale
                            if (targetScale > 1f) {
                                offset = Offset(
                                    x = offset.x + pan.x,
                                    y = offset.y + pan.y
                                )
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                when {
                    bitmap != null -> {
                        Image(
                            bitmap = bitmap!!,
                            contentDescription = imageData.imageName ?: "Full screen image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                }
                        )
                    }
                    isLoading -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = AnonChatColors.Primary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Loading image...",
                                color = AnonChatColors.TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                    hasError -> {
                        Text(
                            text = "Failed to load image",
                            color = AnonChatColors.Error,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────
            // 2. TOP APP BAR (BACK, TITLE, DOWNLOAD)
            // ─────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color(0x99000000))
                    .statusBarsPadding()
                    .padding(horizontal = AnonChatSpacing.md, vertical = AnonChatSpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = Color.White),
                            onClick = onDismiss
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AnonChatIcons.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Title
                Text(
                    text = imageData.imageName ?: "Photo",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Download / Save Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .clickable(
                            enabled = !isDownloading,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = Color.White),
                            onClick = {
                                if (isDownloading) return@clickable
                                isDownloading = true
                                coroutineScope.launch {
                                    var bytesToSave = rawBytes
                                    if (bytesToSave == null && imageData.pathOrUrl.isNotBlank() && repository != null) {
                                        toastMessage = "Fetching image..."
                                        withContext(Dispatchers.Default) {
                                            val res = repository.getMediaBytes(imageData.pathOrUrl)
                                            if (res.isSuccess) {
                                                bytesToSave = res.getOrThrow()
                                                rawBytes = bytesToSave
                                            }
                                        }
                                    }
                                    if (bytesToSave != null && bytesToSave.isNotEmpty()) {
                                        val fileName = imageData.imageName?.ifBlank { null } ?: "annochat_${bytesToSave.size}.jpg"
                                        imageSaver(bytesToSave, fileName) { success, msg ->
                                            isDownloading = false
                                            toastMessage = msg ?: if (success) "Image saved to gallery" else "Failed to save image"
                                        }
                                    } else {
                                        isDownloading = false
                                        toastMessage = "Could not download image."
                                    }
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = AnonChatIcons.Download,
                            contentDescription = "Download image",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────
            // 3. TOAST FEEDBACK
            // ─────────────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = toastMessage != null,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 64.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(AnonChatShapes.pill)
                        .background(Color(0xE61E1E1E))
                        .border(1.dp, Color(0x33FFFFFF), AnonChatShapes.pill)
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = toastMessage ?: "",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // ─────────────────────────────────────────────────────────────
            // 4. BOTTOM CAPTION OVERLAY (IF PRESENT)
            // ─────────────────────────────────────────────────────────────
            if (!imageData.caption.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color(0xAA000000))
                        .navigationBarsPadding()
                        .padding(horizontal = AnonChatSpacing.md, vertical = 14.dp)
                ) {
                    Text(
                        text = imageData.caption,
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
