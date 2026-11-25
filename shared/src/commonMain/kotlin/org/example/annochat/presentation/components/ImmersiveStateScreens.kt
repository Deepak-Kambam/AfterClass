package org.example.annochat.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinproject.shared.generated.resources.Res
import kotlinproject.shared.generated.resources.bg_empty_box
import kotlinproject.shared.generated.resources.bg_empty_cat
import kotlinproject.shared.generated.resources.bg_empty_peers
import kotlinproject.shared.generated.resources.bg_empty_public_groups
import kotlinproject.shared.generated.resources.bg_empty_rooms
import kotlinproject.shared.generated.resources.bg_error_astronaut
import kotlinproject.shared.generated.resources.bg_startup_offline
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

// ─────────────────────────────────────────────────────────────────────────────
// BASE: Immersive Screen Template with Ease-In Ease-Out Sequential Entrance
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ImmersiveStateContainer(
    backgroundImage: DrawableResource,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.BottomCenter,
    applyStatusBarPadding: Boolean = true,
    applyNavigationBarPadding: Boolean = true,
    gradientStops: List<Pair<Float, Color>> = listOf(
        0.0f to Color.Black.copy(alpha = 0.20f),
        0.45f to Color.Black.copy(alpha = 0.60f),
        0.80f to Color.Black.copy(alpha = 0.88f),
        1.0f to Color.Black.copy(alpha = 0.98f)
    ),
    content: @Composable () -> Unit
) {
    var isEntered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isEntered = true
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0f,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "immersiveAlpha"
    )
    val contentScale by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0.97f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "immersiveScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Fullscreen background artwork
        Image(
            painter = painterResource(backgroundImage),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Depth Gradient Scrim Overlay for optimal readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colorStops = gradientStops.toTypedArray()))
        )

        // Foreground content with ease-in ease-out transition
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (applyStatusBarPadding) Modifier.statusBarsPadding() else Modifier)
                .then(if (applyNavigationBarPadding) Modifier.navigationBarsPadding() else Modifier)
                .alpha(contentAlpha)
                .scale(contentScale),
            contentAlignment = contentAlignment
        ) {
            content()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BUTTONS: Pill Action Buttons (Solid White & Frosted Glass)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SolidWhitePillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isLoading: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
        label = "whiteButtonScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "spinTransition")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinRotation"
    )

    Box(
        modifier = modifier
            .scale(buttonScale)
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(Color.White)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.Black.copy(alpha = 0.20f)),
                onClick = onClick
            )
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier
                        .size(19.dp)
                        .then(if (isLoading) Modifier.rotate(rotation) else Modifier)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = Color.Black,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp
            )
        }
    }
}

@Composable
fun FrostedGlassPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isLoading: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
        label = "frostedButtonScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "spinTransition")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinRotation"
    )

    Box(
        modifier = modifier
            .scale(buttonScale)
            .height(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(23.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.25f)),
                onClick = onClick
            )
            .padding(horizontal = 26.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(18.dp)
                        .then(if (isLoading) Modifier.rotate(rotation) else Modifier)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = Color.White,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. No Internet Screen (App Startup / Backend Discovery)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StartupNoInternetScreen(
    onRetry: () -> Unit,
    onConfigureServer: () -> Unit,
    isRetrying: Boolean = false,
    modifier: Modifier = Modifier
) {
    ImmersiveStateContainer(
        backgroundImage = Res.drawable.bg_startup_offline,
        gradientStops = listOf(
            0.0f to Color.Black.copy(alpha = 0.20f),
            0.35f to Color.Black.copy(alpha = 0.45f),
            0.65f to Color.Black.copy(alpha = 0.78f),
            1.0f to Color.Black.copy(alpha = 0.96f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Ambient glowing frosted glass badge with WifiOff icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AnonChatIcons.WifiOff,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Unable to connect",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.5).sp
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Looks like you're offline.\nCheck your internet connection\nand try again.",
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = Color(0xFFA1A1AA),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(36.dp))

            // Primary action: Retry Connection
            SolidWhitePillButton(
                text = "Retry Connection",
                icon = AnonChatIcons.Refresh,
                isLoading = isRetrying,
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(0.72f)
            )

            Spacer(Modifier.height(18.dp))

            // Secondary link: Configure Server
            Text(
                text = "Configure Server",
                fontSize = 13.5.sp,
                color = Color(0xFF8E8E93),
                fontWeight = FontWeight.Medium,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onConfigureServer)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Monochromatic Error State (RoomsScreen Network Failure)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MonochromaticNetworkErrorScreen(
    onRetry: () -> Unit,
    isRetrying: Boolean = false,
    title: String = "No internet connection!",
    message: String = "We couldn't reach the server.\nPlease check your connection\nand try again.",
    modifier: Modifier = Modifier
) {
    ImmersiveStateContainer(
        backgroundImage = Res.drawable.bg_error_astronaut,
        gradientStops = listOf(
            0.0f to Color.Transparent,
            0.20f to Color.Black.copy(alpha = 0.20f),
            0.50f to Color.Black.copy(alpha = 0.65f),
            0.80f to Color.Black.copy(alpha = 0.90f),
            1.0f to Color.Black
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Upper area: Large glowing translucent Wi-Fi symbol with 'x'
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                // Soft radial ambient sheen
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.18f),
                                    Color.White.copy(alpha = 0.04f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Icon(
                    imageVector = AnonChatIcons.WifiOff,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.90f),
                    modifier = Modifier.size(72.dp)
                )
            }

            // Lower area: Title, message, and Try again button
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.5).sp
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = message,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = Color(0xFFA1A1AA),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(32.dp))

                SolidWhitePillButton(
                    text = "Try again",
                    icon = AnonChatIcons.Refresh,
                    isLoading = isRetrying,
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(0.65f)
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Public Rooms Empty (RoomsScreen)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PublicRoomsEmptyScreen(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val roomsBounce = remember { Animatable(1f) }

    ImmersiveStateContainer(
        backgroundImage = Res.drawable.bg_empty_rooms,
        gradientStops = listOf(
            0.0f to Color.Black.copy(alpha = 0.25f),
            0.45f to Color.Black.copy(alpha = 0.65f),
            1.0f to Color.Black.copy(alpha = 0.94f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 116.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Frosted dark glass badge with white chat bubble icon
            Box(
                modifier = Modifier
                    .scale(roomsBounce.value)
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF18181E).copy(alpha = 0.75f))
                    .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.20f))
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        coroutineScope.launch {
                            roomsBounce.animateTo(1.08f, tween(90, easing = FastOutSlowInEasing))
                            roomsBounce.animateTo(1.0f, spring(dampingRatio = 0.55f, stiffness = 420f))
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AnonChatIcons.ChatBubbles,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "No Public Rooms",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.3).sp
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "No active conversation channels\nfound on the server.",
                fontSize = 14.5.sp,
                lineHeight = 21.sp,
                color = Color(0xFFA1A1AA),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            FrostedGlassPillButton(
                text = "Refresh",
                icon = AnonChatIcons.Refresh,
                onClick = onRefresh
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Active Peers Empty (OnlineUsersScreen - Active)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ActivePeersEmptyScreen(
    modifier: Modifier = Modifier,
    applyStatusBarPadding: Boolean = false,
    renderBackground: Boolean = false
) {
    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 116.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = AnonChatIcons.Users,
                contentDescription = null,
                tint = Color(0xFFA1A1AA),
                modifier = Modifier.size(44.dp)
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "No other users\nonline right now",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp,
                letterSpacing = (-0.3).sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Share the room or try again\nin a few moments.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color(0xFFA1A1AA),
                textAlign = TextAlign.Center
            )
        }
    }

    if (renderBackground) {
        ImmersiveStateContainer(
            backgroundImage = Res.drawable.bg_empty_peers,
            applyStatusBarPadding = applyStatusBarPadding,
            gradientStops = listOf(
                0.0f to Color.Black.copy(alpha = 0.20f),
                0.45f to Color.Black.copy(alpha = 0.60f),
                1.0f to Color.Black.copy(alpha = 0.94f)
            ),
            modifier = modifier
        ) {
            content()
        }
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Public Groups Empty (OnlineUsersScreen - Public Groups)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PublicGroupsEmptyScreen(
    onCreateGroup: () -> Unit,
    modifier: Modifier = Modifier,
    applyStatusBarPadding: Boolean = false,
    renderBackground: Boolean = false
) {
    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 116.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (renderBackground) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF18181E))
                        .border(1.dp, AnonChatColors.BorderSubtle, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AnonChatIcons.Users,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = AnonChatIcons.Users,
                    contentDescription = null,
                    tint = Color(0xFFA1A1AA),
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "No public groups\navailable",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp,
                letterSpacing = (-0.3).sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Tap Create to start the first\npublic campus channel.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color(0xFFA1A1AA),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            if (renderBackground) {
                FrostedGlassPillButton(
                    text = "+ Create Group",
                    onClick = onCreateGroup
                )
            } else {
                SolidWhitePillButton(
                    text = "+ Create Group",
                    onClick = onCreateGroup
                )
            }
        }
    }

    if (renderBackground) {
        ImmersiveStateContainer(
            backgroundImage = Res.drawable.bg_empty_public_groups,
            applyStatusBarPadding = applyStatusBarPadding,
            gradientStops = listOf(
                0.0f to Color.Black.copy(alpha = 0.25f),
                0.45f to Color.Black.copy(alpha = 0.65f),
                1.0f to Color.Black.copy(alpha = 0.94f)
            ),
            modifier = modifier
        ) {
            content()
        }
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. My Groups Empty (OnlineUsersScreen - My Groups)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MyGroupsEmptyScreen(
    onCreateGroup: () -> Unit,
    modifier: Modifier = Modifier,
    applyStatusBarPadding: Boolean = false,
    renderBackground: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val boxBounce = remember { Animatable(1f) }

    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 116.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (renderBackground) {
                Box(
                    modifier = Modifier
                        .scale(boxBounce.value)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF18181E))
                        .border(1.dp, AnonChatColors.BorderSubtle, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.20f))
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            coroutineScope.launch {
                                boxBounce.animateTo(1.08f, tween(90, easing = FastOutSlowInEasing))
                                boxBounce.animateTo(1.0f, spring(dampingRatio = 0.55f, stiffness = 420f))
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AnonChatIcons.Users,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = AnonChatIcons.Users,
                    contentDescription = null,
                    tint = Color(0xFFA1A1AA),
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "My Groups",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.3).sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "No groups yet.\nCreate a space for your community.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color(0xFFA1A1AA),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            if (renderBackground) {
                FrostedGlassPillButton(
                    text = "+ Create Group",
                    onClick = onCreateGroup
                )
            } else {
                SolidWhitePillButton(
                    text = "+ Create Group",
                    onClick = onCreateGroup
                )
            }
        }
    }

    if (renderBackground) {
        ImmersiveStateContainer(
            backgroundImage = Res.drawable.bg_empty_box,
            applyStatusBarPadding = applyStatusBarPadding,
            gradientStops = listOf(
                0.0f to Color.Black.copy(alpha = 0.20f),
                0.45f to Color.Black.copy(alpha = 0.55f),
                1.0f to Color.Black.copy(alpha = 0.94f)
            ),
            modifier = modifier
        ) {
            content()
        }
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. Chat Stream Empty (ChatScreen)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ChatStreamEmptyScreen(
    modifier: Modifier = Modifier,
    renderBackground: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val tapBounce = remember { Animatable(1f) }
    var showNudgeHint by remember { mutableStateOf(false) }

    val handleTap: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        coroutineScope.launch {
            tapBounce.animateTo(1.05f, tween(90, easing = FastOutSlowInEasing))
            tapBounce.animateTo(1.0f, spring(dampingRatio = 0.55f, stiffness = 420f))
        }
        showNudgeHint = true
        coroutineScope.launch {
            kotlinx.coroutines.delay(2400)
            showNudgeHint = false
        }
    }

    // Gentle floating animation for sleep "z z Z" bubbles
    val infiniteTransition = rememberInfiniteTransition(label = "zzzAnimation")
    val zzzOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "zzzOffset"
    )
    val zzzAlpha by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "zzzAlpha"
    )

    val emptyContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .scale(tapBounce.value)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = handleTap
                )
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Floating z z Z sleep bubbles
            Row(
                modifier = Modifier
                    .offset { IntOffset(0, zzzOffset.toInt()) }
                    .alpha(zzzAlpha),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "z",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.65f)
                )
                Text(
                    text = "z",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Text(
                    text = "Z",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            // Interactive response hint pill
            AnimatedVisibility(
                visible = showNudgeHint,
                enter = fadeIn(tween(180)) + slideInVertically { -it / 2 },
                exit = fadeOut(tween(180)) + slideOutVertically { -it / 2 }
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Say hi to wake the room! 🐾",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "No messages yet",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.3).sp
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Be the first to start\nthe conversation!",
                fontSize = 14.5.sp,
                lineHeight = 21.sp,
                color = Color(0xFFA1A1AA),
                textAlign = TextAlign.Center
            )
        }
    }

    if (renderBackground) {
        ImmersiveStateContainer(
            backgroundImage = Res.drawable.bg_empty_cat,
            applyStatusBarPadding = false,
            applyNavigationBarPadding = false,
            gradientStops = listOf(
                0.0f to Color.Black.copy(alpha = 0.30f),
                0.40f to Color.Black.copy(alpha = 0.50f),
                0.75f to Color.Black.copy(alpha = 0.75f),
                1.0f to Color.Black.copy(alpha = 0.95f)
            ),
            modifier = modifier
        ) {
            emptyContent()
        }
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            emptyContent()
        }
    }
}
