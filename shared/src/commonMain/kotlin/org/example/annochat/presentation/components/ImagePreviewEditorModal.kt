package org.example.annochat.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.designsystem.AnonChatShapes
import org.example.annochat.presentation.designsystem.AnonChatSpacing
import org.example.annochat.presentation.designsystem.AnonChatTypography
import org.example.annochat.presentation.navigation.BackHandler

private val WaterflowEasing = FastOutSlowInEasing

/**
 * Freehand stroke in doodle mode.
 */
data class EditorDoodleStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float = 7f
)

/**
 * Placed emoji sticker on the canvas.
 */
data class EditorPlacedSticker(
    val id: Long,
    val emoji: String,
    val offset: Offset
)

/**
 * Text annotation overlay on the canvas.
 */
data class EditorPlacedText(
    val id: Long,
    val text: String,
    val color: Color,
    val offset: Offset
)

/**
 * Editable image bundle representing a photo in the multi-image editor session.
 */
class EditorMediaItem(
    val id: String,
    val bytes: ByteArray,
    val fileName: String,
    val mimeType: String,
    caption: String = "",
    rotationDegrees: Float = 0f,
    cropRect: NormalizedCropRect? = null,
    isFlippedHorizontal: Boolean = false,
    aiStyle: AiRestyleStyle = AiRestyleStyle.ORIGINAL,
    doodleStrokes: List<EditorDoodleStroke> = emptyList(),
    placedStickers: List<EditorPlacedSticker> = emptyList(),
    placedTexts: List<EditorPlacedText> = emptyList()
) {
    var caption by mutableStateOf(caption)
    var rotationDegrees by mutableStateOf(rotationDegrees)
    var cropRect by mutableStateOf(cropRect)
    var isFlippedHorizontal by mutableStateOf(isFlippedHorizontal)
    var aiStyle by mutableStateOf(aiStyle)
    var doodleStrokes by mutableStateOf(doodleStrokes)
    var placedStickers by mutableStateOf(placedStickers)
    var placedTexts by mutableStateOf(placedTexts)
}

/**
 * Baked image result ready to be uploaded and dispatched into chat.
 */
data class BakedMediaItem(
    val bytes: ByteArray,
    val fileName: String,
    val mimeType: String,
    val caption: String
)

/**
 * AI Restyle presets with color matrix transformations.
 */
enum class AiRestyleStyle(val label: String, val matrix: ColorMatrix?) {
    ORIGINAL("Original", null),
    CYBERPUNK("Cyberpunk", ColorMatrix(floatArrayOf(
        1.25f, 0.0f,  0.35f, 0.0f, 15f,
        0.0f,  1.15f, 0.35f, 0.0f, 5f,
        0.35f, 0.0f,  1.55f, 0.0f, 30f,
        0.0f,  0.0f,  0.0f,  1.0f, 0f
    ))),
    ANIME_VIVID("Anime Vivid", ColorMatrix(floatArrayOf(
        1.45f, -0.2f, -0.2f, 0.0f, 20f,
        -0.2f, 1.45f, -0.2f, 0.0f, 20f,
        -0.2f, -0.2f, 1.45f, 0.0f, 20f,
        0.0f,  0.0f,  0.0f,  1.0f, 0f
    ))),
    NOIR_FILM("Noir Cinema", ColorMatrix(floatArrayOf(
        0.33f, 0.59f, 0.11f, 0.0f, -10f,
        0.33f, 0.59f, 0.11f, 0.0f, -10f,
        0.33f, 0.59f, 0.11f, 0.0f, -10f,
        0.0f,  0.0f,  0.0f,  1.0f, 0f
    ))),
    WARM_VINTAGE("Vintage Warm", ColorMatrix(floatArrayOf(
        0.393f * 1.5f, 0.769f * 1.1f, 0.189f * 0.9f, 0.0f, 10f,
        0.349f * 1.2f, 0.686f * 1.2f, 0.168f * 0.8f, 0.0f, 5f,
        0.272f * 0.9f, 0.534f * 0.9f, 0.131f * 1.4f, 0.0f, -10f,
        0.0f,          0.0f,          0.0f,          1.0f, 0f
    ))),
    EMERALD_NEON("Emerald Sci-Fi", ColorMatrix(floatArrayOf(
        0.2f,  0.4f,  0.1f,  0.0f, -10f,
        0.1f,  1.45f, 0.2f,  0.0f, 25f,
        0.1f,  0.35f, 0.35f, 0.0f, 5f,
        0.0f,  0.0f,  0.0f,  1.0f, 0f
    ))),
    PENCIL_SKETCH("Pencil Sketch", ColorMatrix(floatArrayOf(
        1.7f, 1.7f, 1.7f, 0.0f, -135f,
        1.7f, 1.7f, 1.7f, 0.0f, -135f,
        1.7f, 1.7f, 1.7f, 0.0f, -135f,
        0.0f, 0.0f, 0.0f, 1.0f, 0f
    )))
}

/**
 * Image preview and functional editor modal with full Screenshot 1 & 2 capability.
 */
@Composable
fun ImagePreviewEditorModal(
    mediaItems: List<EditorMediaItem>,
    onPickMoreImages: () -> Unit = {},
    recipientLabel: String = "Danger (You)",
    onDismiss: () -> Unit,
    onSend: (List<BakedMediaItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    if (mediaItems.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    org.example.annochat.AllowScreenRotationWhileActive()

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    var activeIndex by remember { mutableStateOf(0) }
    val safeIndex = activeIndex.coerceIn(0, mediaItems.size - 1)
    val currentItem = mediaItems[safeIndex]

    // Cache decoded bitmaps
    val bitmapMap = remember { mutableMapOf<String, ImageBitmap?>() }
    val currentBitmap = remember(currentItem.id) {
        bitmapMap.getOrPut(currentItem.id) { decodeImageBitmap(currentItem.bytes) }
    }

    // Caption handling
    var sameCaptionForAll by remember { mutableStateOf(false) }
    var sharedCaptionText by remember { mutableStateOf("") }

    // Interactive modes
    var isCropMode by remember { mutableStateOf(false) }
    var isDoodleActive by remember { mutableStateOf(false) }
    var isStickerPickerOpen by remember { mutableStateOf(false) }
    var isTextEditorOpen by remember { mutableStateOf(false) }
    var isAiRestyleOpen by remember { mutableStateOf(false) }

    // Crop Studio temporary state matching Screenshot 1
    var tempCropLeft by remember(currentItem.id, isCropMode) { mutableStateOf(currentItem.cropRect?.left ?: 0f) }
    var tempCropTop by remember(currentItem.id, isCropMode) { mutableStateOf(currentItem.cropRect?.top ?: 0f) }
    var tempCropRight by remember(currentItem.id, isCropMode) { mutableStateOf(currentItem.cropRect?.right ?: 1f) }
    var tempCropBottom by remember(currentItem.id, isCropMode) { mutableStateOf(currentItem.cropRect?.bottom ?: 1f) }
    var tempBaseRotation by remember(currentItem.id, isCropMode) { mutableStateOf(currentItem.rotationDegrees) }
    var tempFineAngle by remember(currentItem.id, isCropMode) { mutableStateOf(0f) }
    var tempFlippedHorizontal by remember(currentItem.id, isCropMode) { mutableStateOf(currentItem.isFlippedHorizontal) }
    var selectedRatio by remember(currentItem.id, isCropMode) { mutableStateOf("Original") }

    var isBakingAndSending by remember { mutableStateOf(false) }
    var activeBrushColor by remember { mutableStateOf(Color.White) }
    var currentDoodleStroke by remember { mutableStateOf<EditorDoodleStroke?>(null) }

    // Canvas size tracking
    var canvasWidth by remember { mutableStateOf(1000f) }
    var canvasHeight by remember { mutableStateOf(1000f) }

    var nextId by remember { mutableStateOf(100L) }
    var editingTextValue by remember { mutableStateOf("") }
    var editingTextColor by remember { mutableStateOf(Color.White) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2000)
            toastMessage = null
        }
    }

    // Bouncing chevron animation
    val infiniteTransition = rememberInfiniteTransition()
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val animatedRotation by animateFloatAsState(
        targetValue = currentItem.rotationDegrees,
        animationSpec = tween(durationMillis = 200)
    )

    BackHandler(enabled = true) {
        when {
            isCropMode -> isCropMode = false
            isDoodleActive -> isDoodleActive = false
            isStickerPickerOpen -> isStickerPickerOpen = false
            isTextEditorOpen -> isTextEditorOpen = false
            isAiRestyleOpen -> isAiRestyleOpen = false
            else -> onDismiss()
        }
    }

    var isCaptionFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val animatedBottomPadding by animateDpAsState(
        targetValue = if (isCropMode) 140.dp else 190.dp,
        animationSpec = tween(380, easing = WaterflowEasing)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                    isCaptionFocused = false
                    if (isAiRestyleOpen) isAiRestyleOpen = false
                    if (isStickerPickerOpen) isStickerPickerOpen = false
                    if (isTextEditorOpen) isTextEditorOpen = false
                }
            }
    ) {
        // ─────────────────────────────────────────────────────────────
        // 1. CENTER IMAGE CANVAS (LIVE CROPPED PREVIEW)
        // ─────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = 60.dp,
                    bottom = animatedBottomPadding
                )
                .onSizeChanged {
                    canvasWidth = it.width.toFloat().coerceAtLeast(100f)
                    canvasHeight = it.height.toFloat().coerceAtLeast(100f)
                },
            contentAlignment = Alignment.Center
        ) {
            if (currentBitmap != null) {
                    if (!isCropMode) {
                        // ─────────────────────────────────────────────────────────
                        // SCREENSHOT 2: DISPLAY CROPPED, ROTATED & FLIPPED RESULT
                        // ─────────────────────────────────────────────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        rotationZ = animatedRotation
                                        scaleX = if (currentItem.isFlippedHorizontal) -1f else 1f
                                    }
                            ) {
                                val crop = currentItem.cropRect ?: NormalizedCropRect(0f, 0f, 1f, 1f)
                                val srcW = currentBitmap.width
                                val srcH = currentBitmap.height

                                val srcLeft = (crop.left.coerceIn(0f, 1f) * srcW).toInt()
                                val srcTop = (crop.top.coerceIn(0f, 1f) * srcH).toInt()
                                val srcRight = (crop.right.coerceIn(0f, 1f) * srcW).toInt().coerceAtLeast(srcLeft + 1)
                                val srcBottom = (crop.bottom.coerceIn(0f, 1f) * srcH).toInt().coerceAtLeast(srcTop + 1)

                                val cropW = (srcRight - srcLeft).coerceAtLeast(1)
                                val cropH = (srcBottom - srcTop).coerceAtLeast(1)

                                val canvasW = size.width
                                val canvasH = size.height
                                if (canvasW > 0f && canvasH > 0f) {
                                    val canvasAspect = canvasW / canvasH
                                    val cropAspect = cropW.toFloat() / cropH.toFloat()

                                    val dstW: Float
                                    val dstH: Float
                                    if (cropAspect > canvasAspect) {
                                        dstW = canvasW
                                        dstH = canvasW / cropAspect
                                    } else {
                                        dstH = canvasH
                                        dstW = canvasH * cropAspect
                                    }

                                    val dstLeft = (canvasW - dstW) / 2f
                                    val dstTop = (canvasH - dstH) / 2f

                                    drawImage(
                                        image = currentBitmap,
                                        srcOffset = IntOffset(srcLeft, srcTop),
                                        srcSize = IntSize(cropW, cropH),
                                        dstOffset = IntOffset(dstLeft.toInt(), dstTop.toInt()),
                                        dstSize = IntSize(dstW.toInt(), dstH.toInt()),
                                        colorFilter = currentItem.aiStyle.matrix?.let { ColorFilter.colorMatrix(it) }
                                    )
                                }
                            }

                            // Doodle Canvas Overlay
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(isDoodleActive, activeBrushColor, currentItem.id) {
                                        if (!isDoodleActive) return@pointerInput
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                currentDoodleStroke = EditorDoodleStroke(
                                                    points = listOf(offset),
                                                    color = activeBrushColor
                                                )
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                currentDoodleStroke = currentDoodleStroke?.let { stroke ->
                                                    stroke.copy(points = stroke.points + change.position)
                                                }
                                            },
                                            onDragEnd = {
                                                currentDoodleStroke?.let {
                                                    currentItem.doodleStrokes = currentItem.doodleStrokes + it
                                                }
                                                currentDoodleStroke = null
                                            },
                                            onDragCancel = {
                                                currentDoodleStroke = null
                                            }
                                        )
                                    }
                            ) {
                                val allStrokes = currentItem.doodleStrokes + listOfNotNull(currentDoodleStroke)
                                allStrokes.forEach { stroke ->
                                    if (stroke.points.size > 1) {
                                        val path = Path().apply {
                                            moveTo(stroke.points.first().x, stroke.points.first().y)
                                            for (i in 1 until stroke.points.size) {
                                                lineTo(stroke.points[i].x, stroke.points[i].y)
                                            }
                                        }
                                        drawPath(
                                            path = path,
                                            color = stroke.color,
                                            style = Stroke(
                                                width = stroke.strokeWidth,
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            )
                                        )
                                    }
                                }
                            }

                            // Placed Stickers
                            currentItem.placedStickers.forEach { sticker ->
                                var stickerOffset by remember(sticker.id) { mutableStateOf(sticker.offset) }
                                Text(
                                    text = sticker.emoji,
                                    fontSize = 42.sp,
                                    modifier = Modifier
                                        .offset { IntOffset(stickerOffset.x.toInt(), stickerOffset.y.toInt()) }
                                        .pointerInput(sticker.id) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                stickerOffset = Offset(
                                                    stickerOffset.x + dragAmount.x,
                                                    stickerOffset.y + dragAmount.y
                                                )
                                                val updated = currentItem.placedStickers.map {
                                                    if (it.id == sticker.id) it.copy(offset = stickerOffset) else it
                                                }
                                                currentItem.placedStickers = updated
                                            }
                                        }
                                )
                            }

                            // Placed Text Annotations
                            currentItem.placedTexts.forEach { textItem ->
                                var textOffset by remember(textItem.id) { mutableStateOf(textItem.offset) }
                                Box(
                                    modifier = Modifier
                                        .offset { IntOffset(textOffset.x.toInt(), textOffset.y.toInt()) }
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .border(1.dp, textItem.color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .pointerInput(textItem.id) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                textOffset = Offset(
                                                    textOffset.x + dragAmount.x,
                                                    textOffset.y + dragAmount.y
                                                )
                                                val updated = currentItem.placedTexts.map {
                                                    if (it.id == textItem.id) it.copy(offset = textOffset) else it
                                                }
                                                currentItem.placedTexts = updated
                                            }
                                        }
                                ) {
                                    Text(
                                        text = textItem.text,
                                        color = textItem.color,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        // ─────────────────────────────────────────────────────────
                        // SCREENSHOT 1: CROP & ROTATE STUDIO MODE
                        // ─────────────────────────────────────────────────────────
                        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                            val w = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                            val h = constraints.maxHeight.toFloat().coerceAtLeast(1f)
                            val srcW = currentBitmap.width.toFloat()
                            val srcH = currentBitmap.height.toFloat()

                            // Calculate image fitted rect
                            val is90or270 = ((tempBaseRotation.toInt() / 90) % 2 != 0)
                            val effW = if (is90or270) srcH else srcW
                            val effH = if (is90or270) srcW else srcH
                            val imgAspect = effW / effH
                            val areaAspect = w / h

                            val displayW: Float
                            val displayH: Float
                            if (imgAspect > areaAspect) {
                                displayW = w * 0.90f
                                displayH = displayW / imgAspect
                            } else {
                                displayH = h * 0.90f
                                displayW = displayH * imgAspect
                            }

                            val imgLeft = (w - displayW) / 2f
                            val imgTop = (h - displayH) / 2f

                            val liveRotation = tempBaseRotation + tempFineAngle

                            // 1. Draw live rotated & flipped image
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = currentBitmap,
                                    contentDescription = "Crop Base",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size((displayW / density).dp, (displayH / density).dp)
                                        .graphicsLayer {
                                            rotationZ = liveRotation
                                            scaleX = if (tempFlippedHorizontal) -1f else 1f
                                        }
                                )
                            }

                            // 2. Crop Overlay & Grid (Screenshot 1)
                            val boxL = imgLeft + tempCropLeft * displayW
                            val boxT = imgTop + tempCropTop * displayH
                            val boxR = imgLeft + tempCropRight * displayW
                            val boxB = imgTop + tempCropBottom * displayH
                            val boxW = (boxR - boxL).coerceAtLeast(30f)
                            val boxH = (boxB - boxT).coerceAtLeast(30f)

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Scrim surrounding crop box
                                drawRect(Color.Black.copy(alpha = 0.65f), size = Size(w, boxT))
                                drawRect(Color.Black.copy(alpha = 0.65f), topLeft = Offset(0f, boxB), size = Size(w, h - boxB))
                                drawRect(Color.Black.copy(alpha = 0.65f), topLeft = Offset(0f, boxT), size = Size(boxL, boxH))
                                drawRect(Color.Black.copy(alpha = 0.65f), topLeft = Offset(boxR, boxT), size = Size(w - boxR, boxH))

                                // White border
                                drawRect(
                                    color = Color.White,
                                    topLeft = Offset(boxL, boxT),
                                    size = Size(boxW, boxH),
                                    style = Stroke(width = 1.5.dp.toPx())
                                )

                                // 3x3 Rule-of-Thirds Grid lines
                                val thirdW = boxW / 3f
                                val thirdH = boxH / 3f
                                drawLine(Color.White.copy(alpha = 0.5f), Offset(boxL + thirdW, boxT), Offset(boxL + thirdW, boxB), 1.dp.toPx())
                                drawLine(Color.White.copy(alpha = 0.5f), Offset(boxL + thirdW * 2, boxT), Offset(boxL + thirdW * 2, boxB), 1.dp.toPx())
                                drawLine(Color.White.copy(alpha = 0.5f), Offset(boxL, boxT + thirdH), Offset(boxR, boxT + thirdH), 1.dp.toPx())
                                drawLine(Color.White.copy(alpha = 0.5f), Offset(boxL, boxT + thirdH * 2), Offset(boxR, boxT + thirdH * 2), 1.dp.toPx())

                                // 4 Corner Brackets (L-shapes matching Screenshot 1)
                                val cL = 24.dp.toPx()
                                val cT = 4.dp.toPx()
                                // Top-Left
                                drawLine(Color.White, Offset(boxL - cT / 2, boxT), Offset(boxL + cL, boxT), cT)
                                drawLine(Color.White, Offset(boxL, boxT - cT / 2), Offset(boxL, boxT + cL), cT)
                                // Top-Right
                                drawLine(Color.White, Offset(boxR - cL, boxT), Offset(boxR + cT / 2, boxT), cT)
                                drawLine(Color.White, Offset(boxR, boxT - cT / 2), Offset(boxR, boxT + cL), cT)
                                // Bottom-Left
                                drawLine(Color.White, Offset(boxL - cT / 2, boxB), Offset(boxL + cL, boxB), cT)
                                drawLine(Color.White, Offset(boxL, boxB - cL), Offset(boxL, boxB + cT / 2), cT)
                                // Bottom-Right
                                drawLine(Color.White, Offset(boxR - cL, boxB), Offset(boxR + cT / 2, boxB), cT)
                                drawLine(Color.White, Offset(boxR, boxB - cL), Offset(boxR, boxB + cT / 2), cT)

                                // 4 Midpoint Handle Bars (matching Screenshot 1)
                                val midL = 18.dp.toPx()
                                // Top mid
                                drawLine(Color.White, Offset(boxL + boxW / 2 - midL / 2, boxT), Offset(boxL + boxW / 2 + midL / 2, boxT), cT)
                                // Bottom mid
                                drawLine(Color.White, Offset(boxL + boxW / 2 - midL / 2, boxB), Offset(boxL + boxW / 2 + midL / 2, boxB), cT)
                                // Left mid
                                drawLine(Color.White, Offset(boxL, boxT + boxH / 2 - midL / 2), Offset(boxL, boxT + boxH / 2 + midL / 2), cT)
                                // Right mid
                                drawLine(Color.White, Offset(boxR, boxT + boxH / 2 - midL / 2), Offset(boxR, boxT + boxH / 2 + midL / 2), cT)
                            }

                            // Draggable Handles
                            // Center move
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(boxL.toInt(), boxT.toInt()) }
                                    .size((boxW / density).dp, (boxH / density).dp)
                                    .pointerInput(displayW, displayH) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            val curW = tempCropRight - tempCropLeft
                                            val curH = tempCropBottom - tempCropTop
                                            val dx = dragAmount.x / displayW
                                            val dy = dragAmount.y / displayH
                                            val newL = (tempCropLeft + dx).coerceIn(0f, 1f - curW)
                                            val newT = (tempCropTop + dy).coerceIn(0f, 1f - curH)
                                            tempCropLeft = newL
                                            tempCropTop = newT
                                            tempCropRight = newL + curW
                                            tempCropBottom = newT + curH
                                        }
                                    }
                            )

                            // Top-Left corner
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset((boxL - 25).toInt(), (boxT - 25).toInt()) }
                                    .size(50.dp)
                                    .pointerInput(displayW, displayH) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            tempCropLeft = (tempCropLeft + dragAmount.x / displayW).coerceIn(0f, tempCropRight - 0.1f)
                                            tempCropTop = (tempCropTop + dragAmount.y / displayH).coerceIn(0f, tempCropBottom - 0.1f)
                                        }
                                    }
                            )

                            // Top-Right corner
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset((boxR - 25).toInt(), (boxT - 25).toInt()) }
                                    .size(50.dp)
                                    .pointerInput(displayW, displayH) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            tempCropRight = (tempCropRight + dragAmount.x / displayW).coerceIn(tempCropLeft + 0.1f, 1f)
                                            tempCropTop = (tempCropTop + dragAmount.y / displayH).coerceIn(0f, tempCropBottom - 0.1f)
                                        }
                                    }
                            )

                            // Bottom-Left corner
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset((boxL - 25).toInt(), (boxB - 25).toInt()) }
                                    .size(50.dp)
                                    .pointerInput(displayW, displayH) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            tempCropLeft = (tempCropLeft + dragAmount.x / displayW).coerceIn(0f, tempCropRight - 0.1f)
                                            tempCropBottom = (tempCropBottom + dragAmount.y / displayH).coerceIn(tempCropTop + 0.1f, 1f)
                                        }
                                    }
                            )

                            // Bottom-Right corner
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset((boxR - 25).toInt(), (boxB - 25).toInt()) }
                                    .size(50.dp)
                                    .pointerInput(displayW, displayH) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            tempCropRight = (tempCropRight + dragAmount.x / displayW).coerceIn(tempCropLeft + 0.1f, 1f)
                                            tempCropBottom = (tempCropBottom + dragAmount.y / displayH).coerceIn(tempCropTop + 0.1f, 1f)
                                        }
                                    }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Loading preview...",
                        color = AnonChatColors.TextSecondary,
                        style = AnonChatTypography.Body
                    )
                }
            }

            // ─────────────────────────────────────────────────────────────
            // 2. TOP ACTION BAR (WATERFLOW ANIMATION)
            // ─────────────────────────────────────────────────────────────
            AnimatedContent(
                targetState = isCropMode,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(380, easing = WaterflowEasing)) +
                     slideInVertically(animationSpec = tween(380, easing = WaterflowEasing)) { -it / 3 }) togetherWith
                    (fadeOut(animationSpec = tween(280, easing = WaterflowEasing)) +
                     slideOutVertically(animationSpec = tween(280, easing = WaterflowEasing)) { -it / 3 })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) { targetCropMode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AnonChatSpacing.md, vertical = AnonChatSpacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (targetCropMode) {
                    // Cancel button (X)
                    EditorCircleIconButton(
                        icon = AnonChatIcons.Close,
                        contentDescription = "Cancel crop",
                        onClick = { isCropMode = false }
                    )

                    Text(
                        text = "Crop & Rotate",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Reset button
                        EditorCircleIconButton(
                            icon = AnonChatIcons.Reset,
                            contentDescription = "Reset crop",
                            onClick = {
                                tempCropLeft = 0f
                                tempCropTop = 0f
                                tempCropRight = 1f
                                tempCropBottom = 1f
                                tempBaseRotation = 0f
                                tempFineAngle = 0f
                                tempFlippedHorizontal = false
                                selectedRatio = "Original"
                                toastMessage = "Reset to original"
                            }
                        )

                        // Done button (White circle with checkmark)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(AnonChatColors.Primary)
                                .clickable {
                                    val hasCrop = (tempCropLeft > 0.01f || tempCropTop > 0.01f || tempCropRight < 0.99f || tempCropBottom < 0.99f)
                                    currentItem.cropRect = if (hasCrop) NormalizedCropRect(tempCropLeft, tempCropTop, tempCropRight, tempCropBottom) else null
                                    currentItem.rotationDegrees = ((tempBaseRotation + tempFineAngle) % 360f + 360f) % 360f
                                    currentItem.isFlippedHorizontal = tempFlippedHorizontal
                                    isCropMode = false
                                    toastMessage = "Crop applied"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AnonChatIcons.Check,
                                contentDescription = "Done crop",
                                tint = AnonChatColors.OnPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    // Normal Editor Top Bar
                    EditorCircleIconButton(
                        icon = AnonChatIcons.Close,
                        contentDescription = "Close preview",
                        onClick = onDismiss
                    )

                    if (mediaItems.size > 1) {
                        Box(
                            modifier = Modifier
                                .clip(AnonChatShapes.pill)
                                .background(Color(0x66000000))
                                .border(1.dp, Color(0x33FFFFFF), AnonChatShapes.pill)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${safeIndex + 1} / ${mediaItems.size}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Crop & Rotate Studio button
                        EditorCircleIconButton(
                            icon = AnonChatIcons.CropRotate,
                            contentDescription = "Crop and Rotate",
                            onClick = {
                                isCropMode = true
                                isDoodleActive = false
                                isStickerPickerOpen = false
                                isTextEditorOpen = false
                                isAiRestyleOpen = false
                            }
                        )

                        // Sticker icon
                        EditorCircleIconButton(
                            icon = AnonChatIcons.StickerSmile,
                            contentDescription = "Add stickers",
                            isActive = isStickerPickerOpen,
                            onClick = {
                                isStickerPickerOpen = !isStickerPickerOpen
                                isTextEditorOpen = false
                                isDoodleActive = false
                                isAiRestyleOpen = false
                            }
                        )

                        // Text Tool ("Aa")
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isTextEditorOpen) Color.White else Color(0x33FFFFFF))
                                .border(
                                    width = 1.dp,
                                    color = if (isTextEditorOpen) Color.White else Color(0x44FFFFFF),
                                    shape = CircleShape
                                )
                                .clickable {
                                    isTextEditorOpen = !isTextEditorOpen
                                    isStickerPickerOpen = false
                                    isDoodleActive = false
                                    isAiRestyleOpen = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aa",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isTextEditorOpen) Color.Black else Color.White
                            )
                        }

                        // Pencil icon
                        EditorCircleIconButton(
                            icon = AnonChatIcons.Pencil,
                            contentDescription = "Draw on photo",
                            isActive = isDoodleActive,
                            onClick = {
                                isDoodleActive = !isDoodleActive
                                isStickerPickerOpen = false
                                isTextEditorOpen = false
                                isAiRestyleOpen = false
                                if (isDoodleActive) {
                                    toastMessage = "Draw on photo"
                                }
                            }
                        )
                    }
                }
            }
        }

            // ─────────────────────────────────────────────────────────────
            // 3. TOAST NOTIFICATION
            // ─────────────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = toastMessage != null,
                enter = fadeIn(animationSpec = tween(380, easing = WaterflowEasing)) +
                        slideInVertically(animationSpec = tween(380, easing = WaterflowEasing)) { -it / 2 },
                exit = fadeOut(animationSpec = tween(280, easing = WaterflowEasing)) +
                       slideOutVertically(animationSpec = tween(280, easing = WaterflowEasing)) { -it / 2 },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(AnonChatShapes.pill)
                        .background(Color(0xE61E1E1E))
                        .border(1.dp, Color(0x33FFFFFF), AnonChatShapes.pill)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
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
            // 4. DRAWING PALETTE (WHEN DOODLE IS ACTIVE)
            // ─────────────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = isDoodleActive && !isCropMode,
                enter = fadeIn(animationSpec = tween(380, easing = WaterflowEasing)) +
                        slideInVertically(animationSpec = tween(380, easing = WaterflowEasing)) { -it / 2 },
                exit = fadeOut(animationSpec = tween(280, easing = WaterflowEasing)) +
                       slideOutVertically(animationSpec = tween(280, easing = WaterflowEasing)) { -it / 2 },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(AnonChatShapes.pill)
                        .background(Color(0xE61E1E1E))
                        .border(1.dp, Color(0x33FFFFFF), AnonChatShapes.pill)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val brushColors = listOf(
                        Color.White,
                        Color(0xFFFF3B30),
                        Color(0xFFFFDF00),
                        Color(0xFF00C7FF),
                        Color(0xFF34C759),
                        Color(0xFFFF2D55)
                    )

                    brushColors.forEach { color ->
                        val isSelected = activeBrushColor == color
                        val dotSize by animateDpAsState(if (isSelected) 32.dp else 24.dp)

                        Box(
                            modifier = Modifier
                                .size(dotSize)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) {
                                        if (color == Color.White) Color.Black else Color.White
                                    } else {
                                        Color(0x44FFFFFF)
                                    },
                                    shape = CircleShape
                                )
                                .clickable { activeBrushColor = color },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = AnonChatIcons.Check,
                                    contentDescription = "Selected color",
                                    tint = if (color == Color.White) Color.Black else Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    EditorCircleIconButton(
                        icon = AnonChatIcons.Undo,
                        contentDescription = "Undo stroke",
                        onClick = {
                            if (currentItem.doodleStrokes.isNotEmpty()) {
                                currentItem.doodleStrokes = currentItem.doodleStrokes.dropLast(1)
                            }
                        },
                        modifier = Modifier.size(30.dp)
                    )

                    EditorCircleIconButton(
                        icon = AnonChatIcons.Trash,
                        contentDescription = "Clear strokes",
                        onClick = { currentItem.doodleStrokes = emptyList() },
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            // ─────────────────────────────────────────────────────────────
            // 5. STICKER TRAY
            // ─────────────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = isStickerPickerOpen && !isCropMode,
                enter = fadeIn(animationSpec = tween(380, easing = WaterflowEasing)) +
                        slideInVertically(animationSpec = tween(380, easing = WaterflowEasing)) { it / 2 },
                exit = fadeOut(animationSpec = tween(280, easing = WaterflowEasing)) +
                       slideOutVertically(animationSpec = tween(280, easing = WaterflowEasing)) { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 190.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AnonChatSpacing.md)
                        .clip(AnonChatShapes.container)
                        .background(Color(0xF0181818))
                        .border(1.dp, Color(0x33FFFFFF), AnonChatShapes.container)
                        .padding(AnonChatSpacing.md)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tap emoji to place",
                                color = AnonChatColors.TextSecondary,
                                fontSize = 12.sp
                            )
                            if (currentItem.placedStickers.isNotEmpty()) {
                                EditorCircleIconButton(
                                    icon = AnonChatIcons.Trash,
                                    contentDescription = "Clear stickers",
                                    onClick = { currentItem.placedStickers = emptyList() },
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        val emojis = listOf(
                            "🔥", "😂", "❤️", "👍", "🚀", "😎", "🎉", "💯",
                            "👻", "⚡", "🐱", "🐶", "🍕", "☕", "🎮", "✨",
                            "🥳", "👏", "😍", "🎯", "💎", "⭐", "🏆", "👀"
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(emojis) { emoji ->
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF262626))
                                        .clickable {
                                            val id = nextId++
                                            currentItem.placedStickers = currentItem.placedStickers + EditorPlacedSticker(
                                                id = id,
                                                emoji = emoji,
                                                offset = Offset(0f, -40f * (currentItem.placedStickers.size % 4))
                                            )
                                            toastMessage = "Added $emoji"
                                            isStickerPickerOpen = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 24.sp)
                                }
                            }
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────
            // 6. TEXT OVERLAY CREATOR
            // ─────────────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = isTextEditorOpen && !isCropMode,
                enter = fadeIn(animationSpec = tween(380, easing = WaterflowEasing)) +
                        slideInVertically(animationSpec = tween(380, easing = WaterflowEasing)) { it / 2 },
                exit = fadeOut(animationSpec = tween(280, easing = WaterflowEasing)) +
                       slideOutVertically(animationSpec = tween(280, easing = WaterflowEasing)) { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 190.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AnonChatSpacing.md)
                        .clip(AnonChatShapes.container)
                        .background(Color(0xF0181818))
                        .border(1.dp, Color(0x33FFFFFF), AnonChatShapes.container)
                        .padding(AnonChatSpacing.md)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Add Text Overlay",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (currentItem.placedTexts.isNotEmpty()) {
                                    EditorCircleIconButton(
                                        icon = AnonChatIcons.Trash,
                                        contentDescription = "Clear texts",
                                        onClick = { currentItem.placedTexts = emptyList() },
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(AnonChatColors.Primary)
                                        .clickable {
                                            if (editingTextValue.isNotBlank()) {
                                                val id = nextId++
                                                currentItem.placedTexts = currentItem.placedTexts + EditorPlacedText(
                                                    id = id,
                                                    text = editingTextValue.trim(),
                                                    color = editingTextColor,
                                                    offset = Offset(0f, 35f * (currentItem.placedTexts.size % 4))
                                                )
                                                editingTextValue = ""
                                                toastMessage = "Text added"
                                            }
                                            isTextEditorOpen = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = AnonChatIcons.Check,
                                        contentDescription = "Done text",
                                        tint = AnonChatColors.OnPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF262626))
                                .border(1.dp, editingTextColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            if (editingTextValue.isEmpty()) {
                                Text(
                                    text = "Type text overlay...",
                                    color = AnonChatColors.TextMuted,
                                    fontSize = 14.sp
                                )
                            }
                            BasicTextField(
                                value = editingTextValue,
                                onValueChange = { editingTextValue = it },
                                textStyle = TextStyle(
                                    color = editingTextColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                cursorBrush = SolidColor(editingTextColor),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val colors = listOf(
                                Color.White,
                                Color(0xFFFFDF00),
                                Color(0xFF00C7FF),
                                Color(0xFFFF3B30),
                                Color(0xFF34C759),
                                Color(0xFFFF2D55)
                            )
                            colors.forEach { color ->
                                val isSelected = editingTextColor == color
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 30.dp else 24.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) {
                                                if (color == Color.White) Color.Black else Color.White
                                            } else {
                                                Color(0x44FFFFFF)
                                            },
                                            shape = CircleShape
                                        )
                                        .clickable { editingTextColor = color }
                                )
                            }
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────
            // 7 & 8. CROP STUDIO CONTROLS & UNIFIED BOTTOM PANEL (WATERFLOW ANIMATION)
            // ─────────────────────────────────────────────────────────────
            AnimatedContent(
                targetState = isCropMode,
                transitionSpec = {
                    (slideInVertically(animationSpec = tween(380, easing = WaterflowEasing)) { it / 3 } +
                     fadeIn(animationSpec = tween(380, easing = WaterflowEasing))) togetherWith
                    (slideOutVertically(animationSpec = tween(280, easing = WaterflowEasing)) { it / 3 } +
                     fadeOut(animationSpec = tween(280, easing = WaterflowEasing)))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) { targetCropMode ->
                if (targetCropMode) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black)
                            .navigationBarsPadding()
                            .padding(bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                    // Ratio selection row with Flip on Left & Rotate on Right
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Flip Horizontal Button (|◀|▶)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (tempFlippedHorizontal) Color(0x33FFD700) else Color.Transparent)
                                .clickable {
                                    tempFlippedHorizontal = !tempFlippedHorizontal
                                    toastMessage = if (tempFlippedHorizontal) "Flipped" else "Original orientation"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AnonChatIcons.FlipHorizontal,
                                contentDescription = "Flip horizontal",
                                tint = if (tempFlippedHorizontal) Color(0xFFFFD700) else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Ratios Carousel (Free, Original, 1:1, 2:3, 3:4, 16:9)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        ) {
                            // Freeform [ ]
                            item {
                                CropRatioButton(
                                    isSelected = selectedRatio == "Free",
                                    onClick = { selectedRatio = "Free" }
                                ) {
                                    Icon(
                                        imageVector = AnonChatIcons.CropFree,
                                        contentDescription = "Free crop",
                                        tint = if (selectedRatio == "Free") Color(0xFFFFD700) else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Original (Film frame)
                            item {
                                CropRatioButton(
                                    isSelected = selectedRatio == "Original",
                                    onClick = {
                                        selectedRatio = "Original"
                                        tempCropLeft = 0f
                                        tempCropTop = 0f
                                        tempCropRight = 1f
                                        tempCropBottom = 1f
                                    }
                                ) {
                                    Icon(
                                        imageVector = AnonChatIcons.CropOriginal,
                                        contentDescription = "Original ratio",
                                        tint = if (selectedRatio == "Original") Color(0xFFFFD700) else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // 1:1 Square
                            item {
                                CropRatioButton(
                                    isSelected = selectedRatio == "1:1",
                                    onClick = {
                                        selectedRatio = "1:1"
                                        val curW = currentBitmap?.width?.toFloat() ?: 1f
                                        val curH = currentBitmap?.height?.toFloat() ?: 1f
                                        if (curW > curH) {
                                            val normW = curH / curW
                                            tempCropLeft = (1f - normW) / 2f
                                            tempCropRight = tempCropLeft + normW
                                            tempCropTop = 0f
                                            tempCropBottom = 1f
                                        } else {
                                            val normH = curW / curH
                                            tempCropTop = (1f - normH) / 2f
                                            tempCropBottom = tempCropTop + normH
                                            tempCropLeft = 0f
                                            tempCropRight = 1f
                                        }
                                    }
                                ) {
                                    Text(
                                        text = "1:1",
                                        color = if (selectedRatio == "1:1") Color(0xFFFFD700) else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // 2:3 Ratio
                            item {
                                CropRatioButton(
                                    isSelected = selectedRatio == "2:3",
                                    onClick = {
                                        selectedRatio = "2:3"
                                        tempCropLeft = 0.15f
                                        tempCropTop = 0.05f
                                        tempCropRight = 0.85f
                                        tempCropBottom = 0.95f
                                    }
                                ) {
                                    Text(
                                        text = "2:3",
                                        color = if (selectedRatio == "2:3") Color(0xFFFFD700) else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // 3:4 Ratio
                            item {
                                CropRatioButton(
                                    isSelected = selectedRatio == "3:4",
                                    onClick = {
                                        selectedRatio = "3:4"
                                        tempCropLeft = 0.10f
                                        tempCropTop = 0.08f
                                        tempCropRight = 0.90f
                                        tempCropBottom = 0.92f
                                    }
                                ) {
                                    Text(
                                        text = "3:4",
                                        color = if (selectedRatio == "3:4") Color(0xFFFFD700) else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // 16:9 Ratio
                            item {
                                CropRatioButton(
                                    isSelected = selectedRatio == "16:9",
                                    onClick = {
                                        selectedRatio = "16:9"
                                        tempCropLeft = 0.02f
                                        tempCropTop = 0.22f
                                        tempCropRight = 0.98f
                                        tempCropBottom = 0.78f
                                    }
                                ) {
                                    Text(
                                        text = "16:9",
                                        color = if (selectedRatio == "16:9") Color(0xFFFFD700) else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Rotate 90° Button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    tempBaseRotation = (tempBaseRotation + 90f) % 360f
                                    toastMessage = "Rotated ${tempBaseRotation.toInt()}°"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AnonChatIcons.Rotate90Crop,
                                contentDescription = "Rotate 90",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Straighten Angle Indicator (Screenshot 1: clickable to reset to 0°)
                    val totalRot = tempBaseRotation + tempFineAngle
                    val signedAngle = (totalRot % 360f).let {
                        if (it > 180f) it - 360f else if (it < -180f) it + 360f else it
                    }.toInt()

                    Text(
                        text = "$signedAngle°",
                        color = if (signedAngle != 0) Color(0xFFFFD700) else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            tempFineAngle = 0f
                            tempBaseRotation = 0f
                        }
                    )

                    // Straighten Ruler / Dial: full unconstrained 360° continuous rotation in either direction
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .padding(horizontal = 20.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(onDoubleTap = {
                                    tempFineAngle = 0f
                                    tempBaseRotation = 0f
                                })
                            }
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    // Smooth 1:1 continuous rotation in either direction with NO angle clamps
                                    tempFineAngle -= dragAmount.x * 0.28f
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val pxPerDegree = 3.6.dp.toPx()
                            val centerX = size.width / 2f
                            val h = size.height
                            val scrollOffset = (tempBaseRotation + tempFineAngle) * pxPerDegree

                            val minDeg = (((scrollOffset - centerX) / pxPerDegree).toInt()) - 2
                            val maxDeg = (((scrollOffset + centerX) / pxPerDegree).toInt()) + 2

                            for (deg in minDeg..maxDeg) {
                                if (deg % 2 == 0) {
                                    val x = centerX + (deg * pxPerDegree - scrollOffset)
                                    if (x in 0f..size.width) {
                                        val isMajor10 = deg % 10 == 0
                                        val isMajor5 = deg % 5 == 0
                                        val tickHeight = when {
                                            isMajor10 -> 15.dp.toPx()
                                            isMajor5 -> 10.dp.toPx()
                                            else -> 5.5.dp.toPx()
                                        }
                                        val alpha = when {
                                            isMajor10 -> 0.9f
                                            isMajor5 -> 0.55f
                                            else -> 0.25f
                                        }
                                        drawLine(
                                            color = Color.White.copy(alpha = alpha),
                                            start = Offset(x, h - tickHeight),
                                            end = Offset(x, h),
                                            strokeWidth = if (isMajor10) 1.8.dp.toPx() else 1.dp.toPx()
                                        )
                                    }
                                }
                            }

                            // Center indicator tick (Gold accent matching Screenshot 1)
                            drawLine(
                                color = Color(0xFFFFD700),
                                start = Offset(centerX, 0f),
                                end = Offset(centerX, h),
                                strokeWidth = 2.5.dp.toPx()
                            )
                        }
                    }
                }
            } else {
                    // ─────────────────────────────────────────────────────────────
                    // 8. UNIFIED BOTTOM PANEL (SCREENSHOT 2)
                    // Structured in a single Column: Chevron -> AI Drawer -> Thumbnails -> Caption -> Send
                    // Zero overlapping because all elements are sequentially stacked in one Column.
                    // ─────────────────────────────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xCC000000))
                            .padding(horizontal = AnonChatSpacing.md, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                    // Upward chevron indicating swipe up for filters (hidden when typing or drawer open)
                    if (!isAiRestyleOpen && !isCaptionFocused) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isAiRestyleOpen = true }
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures { _, dragAmount ->
                                        if (dragAmount < -8f) isAiRestyleOpen = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .offset(y = bounceOffset.dp)
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x66000000)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = AnonChatIcons.ChevronUp,
                                    contentDescription = "Swipe up for filters",
                                    tint = Color.White.copy(alpha = 0.95f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // AI Style Drawer (Appears right above thumbnails with zero hardcoded padding; drag down or tap outside to close)
                    AnimatedVisibility(
                        visible = isAiRestyleOpen && !isCaptionFocused,
                        enter = fadeIn(animationSpec = tween(380, easing = WaterflowEasing)) +
                                slideInVertically(animationSpec = tween(380, easing = WaterflowEasing)) { it },
                        exit = fadeOut(animationSpec = tween(280, easing = WaterflowEasing)) +
                               slideOutVertically(animationSpec = tween(280, easing = WaterflowEasing)) { it }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AnonChatShapes.container)
                                .background(Color(0xF2141414))
                                .border(1.dp, Color(0x33FFFFFF), AnonChatShapes.container)
                                .padding(horizontal = AnonChatSpacing.md, vertical = 10.dp)
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures { _, dragAmount ->
                                        if (dragAmount > 10f) isAiRestyleOpen = false
                                    }
                                }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Filters & AI Styles",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(AiRestyleStyle.values()) { style ->
                                        val isSelected = currentItem.aiStyle == style
                                        Box(
                                            modifier = Modifier
                                                .clip(AnonChatShapes.pill)
                                                .background(if (isSelected) Color.White else Color(0xFF242424))
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) Color.White else Color(0x33FFFFFF),
                                                    shape = AnonChatShapes.pill
                                                )
                                                .clickable {
                                                    currentItem.aiStyle = style
                                                    toastMessage = "Applied ${style.label}"
                                                }
                                                .padding(horizontal = 14.dp, vertical = 7.dp)
                                        ) {
                                            Text(
                                                text = style.label,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.Black else Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Multi-Image Thumbnail Strip (Shown when multiple photos are selected)
                    if (mediaItems.size > 1) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            itemsIndexed(mediaItems) { idx, item ->
                                val isSelected = idx == safeIndex
                                val thumbBitmap = remember(item.id) {
                                    bitmapMap.getOrPut(item.id) { decodeImageBitmap(item.bytes) }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF202020))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) AnonChatColors.Primary else Color(0x33FFFFFF),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { activeIndex = idx },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (thumbBitmap != null) {
                                        Image(
                                            bitmap = thumbBitmap,
                                            contentDescription = "Photo ${idx + 1}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text("${idx + 1}", color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // "Same caption for all photos" toggle row (Only shown when multiple photos exist)
                    if (mediaItems.size > 1) {
                        Row(
                            modifier = Modifier
                                .clip(AnonChatShapes.pill)
                                .clickable {
                                    sameCaptionForAll = !sameCaptionForAll
                                    if (sameCaptionForAll && sharedCaptionText.isBlank()) {
                                        sharedCaptionText = currentItem.caption
                                    }
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(if (sameCaptionForAll) AnonChatColors.Primary else Color.Transparent)
                                    .border(1.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (sameCaptionForAll) {
                                    Icon(
                                        imageVector = AnonChatIcons.Check,
                                        contentDescription = null,
                                        tint = AnonChatColors.OnPrimary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Same caption for all photos",
                                color = Color(0xEEFFFFFF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Caption Input Field (Clicking the photo icon triggers multi-select picker)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(AnonChatShapes.pill)
                                .background(Color(0xFF1E1E1E))
                                .border(1.dp, Color(0x33FFFFFF), AnonChatShapes.pill)
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Tapping gallery icon in caption area launches picker to select more pictures
                                Icon(
                                    imageVector = AnonChatIcons.Photo,
                                    contentDescription = "Select more pictures",
                                    tint = Color(0xDDFFFFFF),
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clickable(onClick = onPickMoreImages)
                                )

                                Box(modifier = Modifier.weight(1f)) {
                                    val currentCaption = if (sameCaptionForAll) sharedCaptionText else currentItem.caption
                                    if (currentCaption.isEmpty()) {
                                        Text(
                                            text = if (sameCaptionForAll) "Add caption for all photos..." else "Add a caption...",
                                            color = Color(0x88FFFFFF),
                                            fontSize = 14.sp
                                        )
                                    }
                                    BasicTextField(
                                        value = currentCaption,
                                        onValueChange = { newCaption ->
                                            if (sameCaptionForAll) {
                                                sharedCaptionText = newCaption
                                                mediaItems.forEach { it.caption = newCaption }
                                            } else {
                                                currentItem.caption = newCaption
                                            }
                                        },
                                        textStyle = TextStyle(
                                            color = Color.White,
                                            fontSize = 14.sp
                                        ),
                                        cursorBrush = SolidColor(Color.White),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { focusState ->
                                                if (focusState.isFocused) {
                                                    isCaptionFocused = true
                                                    isAiRestyleOpen = false
                                                } else {
                                                    isCaptionFocused = false
                                                }
                                            }
                                    )
                                }
                            }
                        }
                    }

                    // Recipient status chip (Left) + Flat Monochrome Send Button (Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Recipient chip
                        Box(
                            modifier = Modifier
                                .clip(AnonChatShapes.pill)
                                .background(Color(0xFF1E1E1E))
                                .border(1.dp, Color(0x22FFFFFF), AnonChatShapes.pill)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = recipientLabel,
                                color = Color(0xDDFFFFFF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Flat Monochrome Send Button (White circle, black send icon)
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(AnonChatColors.Primary)
                                .clickable(
                                    enabled = !isBakingAndSending,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, color = AnonChatColors.OnPrimary)
                                ) {
                                    if (isBakingAndSending) return@clickable
                                    isBakingAndSending = true
                                    toastMessage = "Sending..."

                                    coroutineScope.launch(Dispatchers.Default) {
                                        val bakedList = mediaItems.map { item ->
                                            val finalCaption = if (sameCaptionForAll) sharedCaptionText else item.caption
                                            val bakedBytes = bakeEditedImage(
                                                originalBytes = item.bytes,
                                                rotationDegrees = item.rotationDegrees,
                                                cropRect = item.cropRect,
                                                isFlippedHorizontal = item.isFlippedHorizontal,
                                                aiStyle = item.aiStyle,
                                                doodleStrokes = item.doodleStrokes,
                                                placedStickers = item.placedStickers,
                                                placedTexts = item.placedTexts,
                                                canvasWidth = canvasWidth,
                                                canvasHeight = canvasHeight
                                            )
                                            BakedMediaItem(
                                                bytes = bakedBytes,
                                                fileName = item.fileName,
                                                mimeType = item.mimeType,
                                                caption = finalCaption
                                            )
                                        }
                                        withContext(Dispatchers.Main) {
                                            isBakingAndSending = false
                                            onSend(bakedList)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isBakingAndSending) {
                                CircularProgressIndicator(
                                    color = AnonChatColors.OnPrimary,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = AnonChatIcons.Send,
                                    contentDescription = "Send photos",
                                    tint = AnonChatColors.OnPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Aspect ratio button matching Screenshot 1 styling.
 */
@Composable
private fun CropRatioButton(
    isSelected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF181818))
            .border(
                width = if (isSelected) 1.8.dp else 1.dp,
                color = if (isSelected) Color(0xFFFFD700) else Color(0x33FFFFFF),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Circular subtle icon button for top navigation and actions.
 */
@Composable
private fun EditorCircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isActive: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (isActive) Color.White else Color(0x33FFFFFF))
            .border(
                width = 1.dp,
                color = if (isActive) Color.White else Color(0x44FFFFFF),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) Color.Black else Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}
