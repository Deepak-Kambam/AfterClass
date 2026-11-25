package org.example.annochat.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.example.annochat.data.repository.ChatRepository
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.designsystem.AnonChatSpacing

/**
 * Staged media model for in-composer verification before sending.
 */
data class PendingComposerMedia(
    val pathOrUrl: String? = null,
    val name: String? = null,
    val bytes: ByteArray? = null,
    val isGif: Boolean = false,
    val isSticker: Boolean = false,
    val visualEmoji: String? = null
)

/**
 * Modern, state-of-the-art message composer with elevated floating shadows
 */
@Composable
fun MessageComposer(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    pendingMedia: PendingComposerMedia? = null,
    onRemovePendingMedia: (() -> Unit)? = null,
    onMediaReceivedFromKeyboard: ((PickedImageItem) -> Unit)? = null,
    repository: ChatRepository? = null,
    isSending: Boolean = false,
    uploadProgress: Float? = null,
    uploadStatus: String? = null,
    onCancelUpload: (() -> Unit)? = null,
    enabled: Boolean = true,
    placeholder: String = "Type a message...",
    onOpenStickers: (() -> Unit)? = null,
    onPickImage: (() -> Unit)? = null,
    isStickersOpen: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (onMediaReceivedFromKeyboard != null) {
        RegisterKeyboardContentReceiver(onMediaReceived = onMediaReceivedFromKeyboard)
    }

    val coroutineScope = rememberCoroutineScope()
    val canSend = (text.isNotBlank() || pendingMedia != null) && enabled && !isSending

    // Click micro-bounce scale for the send button
    val buttonClickScale = remember { Animatable(1f) }
    val textInteractionSource = remember { MutableInteractionSource() }

    val appearance = org.example.annochat.presentation.theme.LocalAppearanceSettings.current
    val accentColor = Color(appearance.accent.colorHex)
    val isLight = (0.2126f * accentColor.red + 0.7152f * accentColor.green + 0.0722f * accentColor.blue) > 0.45f
    val onAccentColor = if (isLight) Color.Black else Color.White

    // Send icon and background transitions
    val fabBgColor by animateColorAsState(
        targetValue = if (canSend) accentColor else Color(0xFF222224),
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "fabBg"
    )
    val fabBorderColor by animateColorAsState(
        targetValue = if (canSend) accentColor else Color(0xFF38383A),
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "fabBorder"
    )
    val sendIconTint by animateColorAsState(
        targetValue = if (canSend) onAccentColor else Color(0xFF9E9EA0),
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "sendIconTint"
    )

    // Emoji/Keyboard toggle rotation
    val emojiRotation by animateFloatAsState(
        targetValue = if (isStickersOpen) 180f else 0f,
        animationSpec = tween(
            durationMillis = 250,
            easing = FastOutSlowInEasing
        ),
        label = "emojiRotation"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // In-composer staged media preview card with cancel [X] option
        AnimatedVisibility(
            visible = pendingMedia != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            if (pendingMedia != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AnonChatColors.SurfaceSecondary)
                        .border(1.dp, AnonChatColors.Border, RoundedCornerShape(14.dp))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Media Visual / Thumbnail
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AnonChatColors.SurfaceTertiary),
                            contentAlignment = Alignment.Center
                        ) {
                            if (pendingMedia.bytes != null || !pendingMedia.pathOrUrl.isNullOrBlank()) {
                                AnimatedMediaImage(
                                    repository = repository,
                                    pathOrUrl = pendingMedia.pathOrUrl,
                                    localBytes = pendingMedia.bytes,
                                    isGif = pendingMedia.isGif,
                                    contentDescription = pendingMedia.name,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (!pendingMedia.visualEmoji.isNullOrBlank()) {
                                Text(text = pendingMedia.visualEmoji, fontSize = 26.sp)
                            } else {
                                Icon(
                                    imageVector = AnonChatIcons.Attachment,
                                    contentDescription = null,
                                    tint = AnonChatColors.TextMuted,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AnonChatColors.Primary)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = if (pendingMedia.isGif) "GIF" else if (pendingMedia.isSticker) "STICKER" else "PHOTO",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AnonChatColors.OnPrimary
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = pendingMedia.name ?: if (pendingMedia.isGif) "Animated GIF" else "Attached Media",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AnonChatColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Ready to send. Add a caption below or tap send.",
                                fontSize = 11.sp,
                                color = AnonChatColors.TextSecondary
                            )
                        }

                        // Close / Remove media button
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(AnonChatColors.SurfaceTertiary)
                                .clickable { onRemovePendingMedia?.invoke() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AnonChatIcons.Close,
                                contentDescription = "Remove media",
                                tint = AnonChatColors.TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        if (uploadProgress != null) {
            MediaUploadProgressSlider(
                progress = uploadProgress,
                status = uploadStatus,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        ambientColor = Color.Black.copy(alpha = 0.8f),
                        spotColor = Color.Black.copy(alpha = 0.8f)
                    )
                    .clip(CircleShape)
                    .background(Color(0xFF242424))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = Color(0xFFFF5252))
                    ) {
                        onCancelUpload?.invoke()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AnonChatIcons.Close,
                    contentDescription = "Cancel upload",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            // ─────────────────────────────────────────────────────────────
            // 1. Elevated Floating Input Pill with Deep Ambient Shadow
            // ─────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 46.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color.Black.copy(alpha = 0.95f),
                        spotColor = Color.Black.copy(alpha = 0.95f)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1A1A1C))
                    .border(
                        width = 1.dp,
                        color = Color(0xFF333338),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(start = 6.dp, end = 6.dp, top = 3.dp, bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // [Left] Emoji / Keyboard toggle button
                if (onOpenStickers != null) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable(
                                enabled = enabled && !isSending,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = AnonChatColors.TextMuted)
                            ) { onOpenStickers() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isStickersOpen) AnonChatIcons.Keyboard else AnonChatIcons.StickerSmile,
                            contentDescription = if (isStickersOpen) "Close stickers" else "Stickers and emojis",
                            tint = if (isStickersOpen) AnonChatColors.Primary else Color(0xFFD1D1D6),
                            modifier = Modifier
                                .size(22.dp)
                                .rotate(emojiRotation)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                }

                // [Center] Multi-line BasicTextField with High Contrast Placeholder
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 15.sp,
                            color = Color(0xFF8E8E93),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    val scrollState = rememberScrollState()
                    BasicTextField(
                        value = text,
                        onValueChange = onTextChange,
                        enabled = enabled && !isSending,
                        interactionSource = textInteractionSource,
                        textStyle = TextStyle(
                            color = AnonChatColors.TextPrimary,
                            fontSize = 15.sp,
                            lineHeight = 21.sp
                        ),
                        cursorBrush = SolidColor(accentColor),
                        minLines = 1,
                        maxLines = 5,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (canSend) {
                                    coroutineScope.launch {
                                        buttonClickScale.animateTo(0.88f, tween(80, easing = FastOutSlowInEasing))
                                        buttonClickScale.animateTo(1f, tween(120, easing = FastOutSlowInEasing))
                                    }
                                    onSend()
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 110.dp)
                            .verticalScroll(scrollState)
                    )
                }

                // [Right] Attachment paperclip button
                if (onPickImage != null) {
                    Spacer(Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable(
                                enabled = enabled && !isSending,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = AnonChatColors.TextMuted)
                            ) { onPickImage() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AnonChatIcons.Attachment,
                            contentDescription = "Attach file or image",
                            tint = Color(0xFFD1D1D6),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // ─────────────────────────────────────────────────────────────
            // 2. Elevated Floating Send Action Button with Drop Shadow
            // ─────────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .scale(buttonClickScale.value)
                    .shadow(
                        elevation = 10.dp,
                        shape = CircleShape,
                        ambientColor = Color.Black.copy(alpha = 0.95f),
                        spotColor = Color.Black.copy(alpha = 0.95f)
                    )
                    .clip(CircleShape)
                    .background(fabBgColor)
                    .border(1.dp, fabBorderColor, CircleShape)
                    .clickable(
                        enabled = canSend,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(
                            bounded = true,
                            color = AnonChatColors.OnPrimary
                        )
                    ) {
                        coroutineScope.launch {
                            buttonClickScale.animateTo(0.88f, tween(80, easing = FastOutSlowInEasing))
                            buttonClickScale.animateTo(1f, tween(120, easing = FastOutSlowInEasing))
                        }
                        onSend()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        color = AnonChatColors.OnPrimary,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = AnonChatIcons.Send,
                        contentDescription = "Send message",
                        tint = sendIconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
}
