package org.example.annochat.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.example.annochat.data.model.MessageDto
import org.example.annochat.data.repository.ChatRepository
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.designsystem.AnonChatShapes
import org.example.annochat.presentation.designsystem.AnonChatSpacing
import org.example.annochat.presentation.designsystem.AnonChatTypography
import kotlin.math.roundToInt

/**
 * Extracts a media (GIF/image) URL from raw message content string.
 * Supports direct links ending in media extensions and provider URLs (Tenor, Giphy).
 */
fun extractMediaUrl(content: String?): String? {
    if (content.isNullOrBlank()) return null
    val trimmed = content.trim()

    // 1. Direct web URLs with image/gif extensions (with or without protocol)
    val urlWithExtRegex = Regex("""(?:https?://|content://|file://)[^\s"'<>]+\.(gif|webp|png|jpg|jpeg)(\?[^\s"'<>]*)?""", RegexOption.IGNORE_CASE)
    val match = urlWithExtRegex.find(trimmed)
    if (match != null) return match.value

    // 2. URLs without protocol ending in media extensions (e.g. media.tenor.com/.../foo.gif)
    val noProtoRegex = Regex("""(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,}[^\s"'<>]+\.(gif|webp|png|jpg|jpeg)(\?[^\s"'<>]*)?""", RegexOption.IGNORE_CASE)
    val matchNoProto = noProtoRegex.find(trimmed)
    if (matchNoProto != null) return "https://${matchNoProto.value}"

    // 3. GIF provider platforms (Tenor, Giphy, Imgur, Gstatic)
    val providerRegex = Regex("""(?:https?://)[^\s"'<>]*(tenor\.com|giphy\.com|gph\.is|imgur\.com)[^\s"'<>]*""", RegexOption.IGNORE_CASE)
    val providerMatch = providerRegex.find(trimmed)
    if (providerMatch != null) return providerMatch.value

    // 4. Any string that ends with .gif or contains a .gif path
    val anyGifRegex = Regex("""[^\s"'<>]+\.gif(\?[^\s"'<>]*)?""", RegexOption.IGNORE_CASE)
    val anyGifMatch = anyGifRegex.find(trimmed)
    if (anyGifMatch != null) {
        val v = anyGifMatch.value
        return if (v.startsWith("http://") || v.startsWith("https://") || v.startsWith("content://") || v.startsWith("file://")) v else "https://$v"
    }

    return null
}

/**
 * Parses "WIDTHxHEIGHT" string (e.g. "800x600") into a Pair of Ints.
 */
fun parseImageDims(dims: String?): Pair<Int, Int>? {
    if (dims.isNullOrBlank()) return null
    val parts = dims.lowercase().split("x")
    if (parts.size == 2) {
        val w = parts[0].trim().toIntOrNull()
        val h = parts[1].trim().toIntOrNull()
        if (w != null && h != null && w > 0 && h > 0) {
            return Pair(w, h)
        }
    }
    return null
}

/**
 * WhatsApp-style interactive message bubble:
 * - Swipe right on ANY message to reply (bounded distance with spring release).
 * - Expanded long press hit area for message selection & emoji reactions.
 * - Automatic rich media & GIF URL parsing (renders actual animated media instead of plain text URLs).
 * - Independent bounded containers preventing visual collisions and overlap with adjacent messages.
 */
@Composable
fun MessageBubble(
    message: MessageDto,
    isOwnMessage: Boolean,
    repository: ChatRepository? = null,
    ownAvatarUrl: String? = null,
    ownAnimalName: String? = null,
    ownPublicId: String? = null,
    showSenderHeader: Boolean = true,
    showTimestamp: Boolean = true,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onReactionClick: (emoji: String) -> Unit = {},
    onReplyClick: () -> Unit = {},
    onLongPress: () -> Unit = {},
    onClick: () -> Unit = {},
    onImageClick: ((pathOrUrl: String, imageName: String?, caption: String?) -> Unit)? = null,
    onAvatarClick: ((pathOrUrl: String, name: String?) -> Unit)? = null,
    animateEntrance: Boolean = false,
    floatingReactions: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current
    val offsetX = remember { Animatable(0f) }

    val entranceScale = remember { Animatable(if (animateEntrance) 0.93f else 1f) }
    val entranceAlpha = remember { Animatable(if (animateEntrance) 0.65f else 1f) }

    if (animateEntrance) {
        androidx.compose.runtime.LaunchedEffect(message.postNum) {
            launch {
                entranceScale.animateTo(1f, tween(durationMillis = 220, easing = FastOutSlowInEasing))
            }
            launch {
                entranceAlpha.animateTo(1f, tween(durationMillis = 180, easing = FastOutSlowInEasing))
            }
        }
    }

    val triggerThresholdPx = with(density) { 56.dp.toPx() }
    val maxSwipePx = with(density) { 80.dp.toPx() }

    val bubbleShape = if (isOwnMessage) AnonChatShapes.bubbleOwn else AnonChatShapes.bubblePeer
    val bubbleColor = if (isOwnMessage) AnonChatColors.BubbleOwn else AnonChatColors.BubblePeer
    val bubbleBorderColor = if (isOwnMessage) AnonChatColors.BubbleOwnBorder else AnonChatColors.BubblePeerBorder

    val progress = (offsetX.value / triggerThresholdPx).coerceIn(0f, 1f)

    // Selection visual effects
    val scale by animateFloatAsState(targetValue = if (isSelected) 0.98f else 1f)
    val overlayAlpha by animateFloatAsState(targetValue = if (isSelected) 0.15f else 0f)
    val selectionOverlayColor = if (isSelected) AnonChatColors.Primary.copy(alpha = overlayAlpha) else AnonChatColors.SurfacePrimary.copy(alpha = 0f)

    // Automatically detect GIF / media URLs within text content
    val mediaUrlFromContent = remember(message.content) { extractMediaUrl(message.content) }
    val effectiveImagePath = message.imagePath ?: mediaUrlFromContent

    val isGifUrl = effectiveImagePath != null && (
        effectiveImagePath.contains(".gif", ignoreCase = true) ||
        effectiveImagePath.contains("tenor.com", ignoreCase = true) ||
        effectiveImagePath.contains("giphy.com", ignoreCase = true) ||
        effectiveImagePath.contains("gph.is", ignoreCase = true)
    )

    val isStickerTag = message.content?.startsWith("[Sticker]") == true || effectiveImagePath?.startsWith("sticker_") == true
    val isGifTag = isGifUrl || message.content?.startsWith("[GIF]") == true || effectiveImagePath?.startsWith("gif_") == true
    val hasMedia = !effectiveImagePath.isNullOrBlank() || isStickerTag || isGifTag

    // If content was solely the GIF URL, do not show redundant URL text below the media
    val remainingContent = remember(message.content, mediaUrlFromContent) {
        if (mediaUrlFromContent != null && message.content != null) {
            val rawExtracted = extractMediaUrl(message.content)
            var stripped = message.content
            if (rawExtracted != null) {
                stripped = stripped.replace(rawExtracted, "")
            }
            stripped = stripped.replace(mediaUrlFromContent, "")
                .replace("https://", "")
                .replace("http://", "")
                .trim()
            if (stripped.isBlank() || stripped.matches(Regex("""[^\s]+\.(gif|webp|png|jpg|jpeg)(\?[^\s]*)?""", RegexOption.IGNORE_CASE))) null else stripped
        } else {
            message.content
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(message.postNum, isSelectionMode) {
                detectTapGestures(
                    onLongPress = {
                        if (!isSelectionMode) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongPress()
                        }
                    },
                    onTap = {
                        if (isSelectionMode) {
                            onClick()
                        }
                    }
                )
            }
            .clickable(
                enabled = isSelectionMode,
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .padding(vertical = 1.dp, horizontal = 2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val maxBubbleWidth = (maxWidth * 0.76f).coerceIn(200.dp, 330.dp)

        // WhatsApp style Reply Action Indicator on the left of the row
        if (offsetX.value > 1f) {
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(34.dp)
                    .scale(0.5f + (0.5f * progress))
                    .rotate(-30f * (1f - progress))
                    .alpha(progress)
                    .clip(CircleShape)
                    .background(if (progress >= 1f) AnonChatColors.Primary else AnonChatColors.SurfaceTertiary)
                    .border(
                        1.dp,
                        if (progress >= 1f) AnonChatColors.Primary else AnonChatColors.Border,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AnonChatIcons.Reply,
                    contentDescription = "Swipe to reply",
                    tint = if (progress >= 1f) AnonChatColors.SurfacePrimary else AnonChatColors.TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Message Row: [Peer Avatar] -> [Header, Bubble, Footer] -> [Own Avatar]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(message.postNum) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                val current = offsetX.value
                                val newOffset = (current + dragAmount).coerceIn(0f, maxSwipePx)
                                offsetX.snapTo(newOffset)
                            }
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                if (offsetX.value >= triggerThresholdPx) {
                                    onReplyClick()
                                }
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(
                                        durationMillis = 260,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(
                                        durationMillis = 260,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                            }
                        }
                    )
                },
            horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            // Peer Avatar (Left side)
            if (!isOwnMessage) {
                if (showSenderHeader) {
                    if (repository != null) {
                        val slug = remember(message.anonAvatar, message.anonName, message.publicId) {
                            resolveAnimalSlug(message.anonAvatar, message.anonName, message.publicId)
                        }
                        val targetUrl = remember(slug) { "/api/animal/$slug" }
                        AnimalAvatar(
                            repository = repository,
                            avatarUrl = message.anonAvatar,
                            animalName = message.anonName,
                            publicId = message.publicId,
                            size = 36.dp,
                            onClick = if (onAvatarClick != null) {
                                { onAvatarClick(targetUrl, message.anonName) }
                            } else null,
                            modifier = Modifier.padding(top = 2.dp, end = 8.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp, end = 8.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AnonChatColors.SurfaceSecondary)
                                .border(1.dp, AnonChatColors.Border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AnonChatIcons.Paw,
                                contentDescription = null,
                                tint = AnonChatColors.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(44.dp))
                }
            }

            // Bubble Column: Header -> Bubble Content -> Footer
            Column(
                horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start,
                modifier = Modifier
                    .widthIn(min = 80.dp, max = maxBubbleWidth)
                    .padding(horizontal = 2.dp)
            ) {
                // Sender Header: e.g. "Anonymous #10 14:01" or "14:01 You"
                if (showSenderHeader) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(
                            bottom = 4.dp,
                            start = if (isOwnMessage) 0.dp else 4.dp,
                            end = if (isOwnMessage) 4.dp else 0.dp
                        )
                    ) {
                        if (!isOwnMessage) {
                            Text(
                                text = message.anonName ?: "Anonymous",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AnonChatColors.TextPrimary,
                                maxLines = 1
                            )
                            if (showTimestamp && message.createdAt != null) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = formatTimestamp(message.createdAt),
                                    fontSize = 11.sp,
                                    color = AnonChatColors.TextMuted
                                )
                            }
                        } else {
                            if (showTimestamp && message.createdAt != null) {
                                Text(
                                    text = formatTimestamp(message.createdAt),
                                    fontSize = 11.sp,
                                    color = AnonChatColors.TextMuted
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                text = "You",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AnonChatColors.TextPrimary
                            )
                        }
                    }
                }

                // Bubble Container with interactive hit target
                Box(contentAlignment = if (isOwnMessage) Alignment.BottomEnd else Alignment.BottomStart) {
                    Column(
                        modifier = Modifier
                            .scale(scale * entranceScale.value)
                            .alpha(entranceAlpha.value)
                            .clip(bubbleShape)
                            .background(bubbleColor)
                            .background(selectionOverlayColor)
                            .border(1.dp, if (isSelected) AnonChatColors.Primary else bubbleBorderColor, bubbleShape)
                            .pointerInput(message.postNum, isSelectionMode) {
                                detectTapGestures(
                                    onLongPress = {
                                        if (!isSelectionMode) {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onLongPress()
                                        }
                                    },
                                    onTap = {
                                        if (isSelectionMode) {
                                            onClick()
                                        }
                                    }
                                )
                            }
                            .padding(horizontal = AnonChatSpacing.md, vertical = AnonChatSpacing.sm)
                    ) {
                        // Quoted Reply Snippet
                        if (message.replyTo != null && message.replyTo > 0) {
                            QuotedReplyBox(
                                replyTo = message.replyTo,
                                replyName = message.replyName,
                                replySnippet = message.replySnippet,
                                onClick = onReplyClick
                            )
                        }

                        // Rich Media (Photo / Animated GIF / Sticker)
                        if (hasMedia) {
                            if (message.replyTo != null && message.replyTo > 0) {
                                Spacer(Modifier.height(AnonChatSpacing.xs))
                            }
                            MediaBubbleAttachment(
                                repository = repository,
                                imagePath = effectiveImagePath,
                                imageName = message.imageName,
                                content = remainingContent,
                                imageDims = message.imageDims,
                                maxAvailableWidth = maxBubbleWidth - (AnonChatSpacing.md * 2),
                                onImageClick = onImageClick,
                                onLongPress = {
                                    if (!isSelectionMode) {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onLongPress()
                                    }
                                },
                                isSelectionMode = isSelectionMode
                            )
                        }

                        // Text Body (Wraps long URLs and text cleanly)
                        remainingContent?.let { text ->
                            if (text.isNotBlank() && !isStickerTag && !isGifTag) {
                                if ((message.replyTo != null && message.replyTo > 0) || hasMedia) {
                                    Spacer(Modifier.height(AnonChatSpacing.xs))
                                }
                                Text(
                                    text = text,
                                    style = AnonChatTypography.MessageBody,
                                    color = AnonChatColors.TextPrimary,
                                    softWrap = true,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }
                    }

                    // Floating reaction effects
                    if (floatingReactions.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .align(if (isOwnMessage) Alignment.TopEnd else Alignment.TopStart)
                                .offset(y = (-10).dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            floatingReactions.forEach { emoji ->
                                FloatingReactionEffect(emoji = emoji)
                            }
                        }
                    }
                }

                // Footer: Reactions + Delivery Status
                Column(
                    horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start,
                    modifier = Modifier.padding(
                        top = 4.dp,
                        start = if (isOwnMessage) 0.dp else 4.dp,
                        end = if (isOwnMessage) 4.dp else 0.dp
                    )
                ) {
                    if (message.reactions.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(AnonChatSpacing.xxs),
                            modifier = Modifier.padding(bottom = 3.dp)
                        ) {
                            message.reactions.forEach { (emoji, count) ->
                                Box(
                                    modifier = Modifier
                                        .clip(AnonChatShapes.pill)
                                        .background(AnonChatColors.SurfaceTertiary)
                                        .border(1.dp, AnonChatColors.Border, AnonChatShapes.pill)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(bounded = true, color = AnonChatColors.Primary)
                                        ) { onReactionClick(emoji) }
                                        .padding(horizontal = AnonChatSpacing.xs, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$emoji $count",
                                        style = AnonChatTypography.Caption,
                                        color = AnonChatColors.TextPrimary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = if (isOwnMessage) "Delivered ✓" else "Delivered",
                        fontSize = 11.sp,
                        color = AnonChatColors.TextMuted
                    )
                }
            }

            // Own Avatar (Right side)
            if (isOwnMessage) {
                if (showSenderHeader) {
                    if (repository != null) {
                        AnimalAvatar(
                            repository = repository,
                            avatarUrl = ownAvatarUrl ?: message.anonAvatar,
                            animalName = ownAnimalName ?: message.anonName,
                            publicId = ownPublicId ?: message.publicId,
                            size = 36.dp,
                            modifier = Modifier.padding(top = 2.dp, start = 8.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp, start = 8.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AnonChatColors.SurfaceSecondary)
                                .border(1.dp, AnonChatColors.Border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AnonChatIcons.Person,
                                contentDescription = null,
                                tint = AnonChatColors.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(44.dp))
                }
            }
        }
    }
}

@Composable
private fun MediaBubbleAttachment(
    repository: ChatRepository?,
    imagePath: String?,
    imageName: String?,
    content: String?,
    imageDims: String? = null,
    maxAvailableWidth: Dp = 260.dp,
    onImageClick: ((pathOrUrl: String, imageName: String?, caption: String?) -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    isSelectionMode: Boolean = false
) {
    val isCuratedSticker = imagePath?.startsWith("sticker_") == true || (content?.startsWith("[Sticker]") == true && (imagePath == null || imagePath.startsWith("sticker_")))
    val isCuratedGif = imagePath?.startsWith("gif_") == true || (content?.startsWith("[GIF]") == true && (imagePath == null || imagePath.startsWith("gif_")))

    val isGif = isCuratedGif || (imagePath != null && (
        imagePath.contains(".gif", ignoreCase = true) ||
        imagePath.contains("tenor.com", ignoreCase = true) ||
        imagePath.contains("giphy.com", ignoreCase = true) ||
        imagePath.contains("gph.is", ignoreCase = true)
    )) || content?.startsWith("[GIF]") == true

    val isSticker = isCuratedSticker || (imagePath != null && imagePath.contains("sticker", ignoreCase = true))

    val isMediaAsset = !imagePath.isNullOrBlank() && !isCuratedSticker && !isCuratedGif

    when {
        isMediaAsset -> {
            val initialDims = remember(imageDims) { parseImageDims(imageDims) }
            var dynamicDims by remember(imagePath) { mutableStateOf(initialDims) }

            val aspectRatio = dynamicDims?.let { (w, h) ->
                (w.toFloat() / h.toFloat()).coerceIn(0.55f, 2.2f)
            } ?: if (isSticker) 1.0f else 1.33f

            val maxHeight = if (isSticker) 200.dp else 280.dp
            val minHeight = if (isSticker) 100.dp else 140.dp
            val renderedHeight = (maxAvailableWidth / aspectRatio).coerceIn(minHeight, maxHeight)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(renderedHeight)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSticker) Color.Transparent else AnonChatColors.SurfaceSecondary)
                    .border(
                        width = 1.dp,
                        color = if (isSticker) AnonChatColors.BorderSubtle else AnonChatColors.Border,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .pointerInput(imagePath, isSelectionMode) {
                        detectTapGestures(
                            onTap = {
                                if (isSelectionMode) {
                                    onLongPress?.invoke()
                                } else if (onImageClick != null && imagePath != null) {
                                    onImageClick(imagePath, imageName, content)
                                }
                            },
                            onLongPress = {
                                onLongPress?.invoke()
                            }
                        )
                    }
            ) {
                AnimatedMediaImage(
                    repository = repository,
                    pathOrUrl = imagePath,
                    contentDescription = imageName ?: if (isGif) "Animated GIF" else "Attached Media",
                    contentScale = if (isSticker) ContentScale.Fit else ContentScale.Crop,
                    isGif = isGif,
                    onDimensionsKnown = { w, h ->
                        if (dynamicDims == null && w > 0 && h > 0) {
                            dynamicDims = Pair(w, h)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        isCuratedGif -> {
            val gifKey = when {
                !imagePath.isNullOrBlank() -> imagePath
                content != null -> content.removePrefix("[GIF]").trim()
                else -> "mindblown"
            }
            val visual = getGifVisual(gifKey)
            Box(
                modifier = Modifier
                    .widthIn(min = 130.dp, max = 190.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AnonChatColors.SurfaceTertiary)
                    .border(1.dp, AnonChatColors.Border, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(AnonChatColors.SurfaceSecondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = visual, fontSize = 28.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AnonChatColors.Primary)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "GIF",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = AnonChatColors.OnPrimary
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = imageName ?: if (content?.startsWith("[GIF]") == true) content.removePrefix("[GIF]").trim() else "GIF Animation",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AnonChatColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        isCuratedSticker -> {
            val stickerKey = when {
                !imagePath.isNullOrBlank() -> imagePath
                content != null -> content.removePrefix("[Sticker]").trim()
                else -> "sparkles"
            }
            val displayName = imageName ?: if (content?.startsWith("[Sticker]") == true) content.removePrefix("[Sticker]").trim() else null
            val emoji = getStickerEmoji(stickerKey)
            Box(
                modifier = Modifier
                    .widthIn(min = 120.dp, max = 180.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AnonChatColors.SurfaceTertiary)
                    .border(1.dp, AnonChatColors.Border, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = emoji, fontSize = 48.sp)
                    if (!displayName.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = displayName,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = AnonChatColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        else -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AnonChatShapes.badge)
                    .background(AnonChatColors.SurfaceTertiary)
                    .border(1.dp, AnonChatColors.Border, AnonChatShapes.badge)
                    .padding(horizontal = AnonChatSpacing.sm, vertical = AnonChatSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = AnonChatIcons.Attachment,
                    contentDescription = "Attachment",
                    tint = AnonChatColors.TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(AnonChatSpacing.xs))
                Text(
                    text = imageName ?: "Media Attachment",
                    style = AnonChatTypography.Caption,
                    color = AnonChatColors.TextSecondary
                )
            }
        }
    }
}

fun getStickerEmoji(rawPath: String): String {
    val clean = rawPath.lowercase().replace("sticker_", "").trim()
    return when (clean) {
        "fox" -> "🦊"
        "wolf" -> "🐺"
        "panda" -> "🐼"
        "lion" -> "🦁"
        "tiger" -> "🐯"
        "koala" -> "🐨"
        "raccoon" -> "🦝"
        "bear" -> "🐻"
        "rabbit" -> "🐰"
        "frog" -> "🐸"
        "penguin" -> "🐧"
        "owl" -> "🦉"
        "llama" -> "🦙"
        "unicorn" -> "🦄"
        "dragon" -> "🐲"
        "dolphin" -> "🐬"
        "fire" -> "🔥"
        "rocket" -> "🚀"
        "100", "hundred" -> "💯"
        "sparkles" -> "✨"
        "party" -> "🎉"
        "boom" -> "💥"
        "claps" -> "👏"
        "chill" -> "🤙"
        "skull" -> "💀"
        "bot" -> "🤖"
        "cool" -> "🕶️"
        "bolt" -> "⚡"
        "heart" -> "🖤"
        "target" -> "🎯"
        "coffee" -> "☕"
        "shield" -> "🛡️"
        else -> "✨"
    }
}

fun getGifVisual(path: String): String {
    val clean = path.lowercase().replace("gif_", "").trim()
    return when (clean) {
        "mindblown" -> "🤯"
        "hacker" -> "💻"
        "catvibes" -> "🐱"
        "dancing" -> "🕺"
        "cheers" -> "🥂"
        "facepalm" -> "🤦"
        "rollsafe" -> "🧠"
        "popcorn" -> "🍿"
        "loading" -> "⏳"
        "micdrop" -> "🎤"
        "wave" -> "👋"
        "celebrate" -> "🏆"
        else -> "🎬"
    }
}

@Composable
private fun QuotedReplyBox(
    replyTo: Long,
    replyName: String?,
    replySnippet: String?,
    onClick: () -> Unit
) {
    val cleanAuthor = if (!replyName.isNullOrBlank()) {
        replyName.trim().removePrefix(">>").trim()
    } else {
        "Message #$replyTo"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AnonChatColors.SurfaceTertiary.copy(alpha = 0.9f))
            .border(1.dp, AnonChatColors.Border.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = AnonChatColors.Primary)
            ) { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.5.dp)
                .height(30.dp)
                .clip(CircleShape)
                .background(AnonChatColors.Primary)
        )

        Spacer(Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cleanAuthor,
                style = AnonChatTypography.Label,
                color = AnonChatColors.Primary,
                fontSize = 12.sp,
                maxLines = 1
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = if (!replySnippet.isNullOrBlank()) replySnippet.trim() else "Attachment",
                style = AnonChatTypography.Caption,
                color = AnonChatColors.TextSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun FloatingReactionEffect(emoji: String) {
    val animOffsetY = remember { Animatable(0f) }
    val animAlpha = remember { Animatable(1f) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        launch {
            animOffsetY.animateTo(-45f, tween(1100, easing = FastOutSlowInEasing))
        }
        launch {
            animAlpha.animateTo(0f, tween(1100, easing = FastOutSlowInEasing))
        }
    }

    Box(
        modifier = Modifier
            .offset(y = animOffsetY.value.dp)
            .alpha(animAlpha.value)
            .size(28.dp)
            .clip(CircleShape)
            .background(AnonChatColors.SurfaceTertiary)
            .border(1.dp, AnonChatColors.Border, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = 16.sp)
    }
}

fun formatTimestamp(isoString: String): String {
    return try {
        if (isoString.contains("T")) {
            val timePart = isoString.substringAfter("T").substringBefore(".")
            val parts = timePart.split(":")
            if (parts.size >= 2) "${parts[0]}:${parts[1]}" else timePart
        } else if (isoString.contains(" ")) {
            val parts = isoString.split(" ")
            if (parts.size >= 2) parts[1].take(5) else isoString
        } else {
            isoString
        }
    } catch (_: Exception) {
        isoString
    }
}
