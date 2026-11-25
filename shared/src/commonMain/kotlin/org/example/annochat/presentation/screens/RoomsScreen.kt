package org.example.annochat.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue
import org.example.annochat.data.model.PublicRoomDto
import org.example.annochat.data.model.RoomActivityLevel
import org.example.annochat.presentation.components.AtmosphericBackground
import org.example.annochat.presentation.components.ConnectionStatusBadge
import org.example.annochat.presentation.components.EmptyState
import org.example.annochat.presentation.components.ErrorState
import org.example.annochat.presentation.components.LivePresenceDot
import org.example.annochat.presentation.components.AnonChatLargeHeader
import org.example.annochat.presentation.components.PublicRoomsEmptyScreen
import org.example.annochat.presentation.components.RoomItemSkeleton
import org.example.annochat.presentation.components.SliderToEnter
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.designsystem.AnonChatTypography
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import org.example.annochat.presentation.components.AmbientCarouselBackground
import org.example.annochat.presentation.theme.RoomVisualRegistry
import org.jetbrains.compose.resources.painterResource
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.example.annochat.data.repository.NotificationRepository
import org.example.annochat.network.ConnectionState
import org.example.annochat.network.ConnectionStatus
import org.example.annochat.presentation.components.NotificationsModal
import org.example.annochat.presentation.designsystem.AnonChatSpacing
import org.example.annochat.presentation.viewmodel.RoomsViewModel

@Composable
fun RoomsScreen(
    viewModel: RoomsViewModel,
    onRoomClick: (roomId: String, roomName: String) -> Unit,
    notificationRepository: NotificationRepository? = null,
    isLiquidGlassEnabled: Boolean = false,
    onToggleLiquidGlass: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showNotificationsModal by rememberSaveable { mutableStateOf(false) }
    val unreadCount by notificationRepository?.unreadCount?.collectAsState() ?: remember { mutableStateOf(0) }

    // When connection error occurs (or during retry) and no rooms loaded, render full-screen Oops error view
    val isError = (uiState.errorMessage != null || uiState.isRetrying) && uiState.publicRooms.isEmpty()
    if (isError) {
        ErrorState(
            title = "No internet connection!",
            message = "We couldn't reach the server.\nPlease check your connection\nand try again.",
            retryLabel = "Try again",
            isLoading = uiState.isRetrying,
            onRetry = { viewModel.retryRooms() },
            modifier = modifier
        )
        return
    }

    if (uiState.publicRooms.isEmpty() && !uiState.isLoading) {
        PublicRoomsEmptyScreen(
            onRefresh = { viewModel.loadRooms() },
            modifier = modifier.fillMaxSize()
        )
        return
    }

    val isModalOpen = showNotificationsModal

    val publicRoomIndex = remember(uiState.publicRooms) {
        val idx = uiState.publicRooms.indexOfFirst {
            it.name.contains("public", ignoreCase = true) || it.name.contains("anon", ignoreCase = true)
        }
        if (idx >= 0) idx else (if (uiState.publicRooms.size > 1) 1 else 0)
    }

    val pagerState = rememberPagerState(
        initialPage = publicRoomIndex,
        pageCount = { uiState.publicRooms.size }
    )

    Box(modifier = modifier.fillMaxSize()) {
        AmbientCarouselBackground(
            rooms = uiState.publicRooms,
            pagerState = pagerState,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (isModalOpen) {
                        alpha = 0.35f
                        scaleX = 0.98f
                        scaleY = 0.98f
                    }
                }
        ) {

            // ── Header Area ───────────────────────────────────────────────
            AnonChatLargeHeader(
                title = "Chats",
                subtitle = "Public Channels",
                contentBelowSubtitle = {
                    val connectionState = uiState.connectionState ?: ConnectionState(ConnectionStatus.CONNECTED)
                    ConnectionStatusBadge(
                        connectionState = connectionState,
                        onlineCount = uiState.onlineCount
                    )
                },
                rightContent = {
                    // Notification Button [🔔] immediately to the LEFT of Refresh
                    val notifInteraction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable(
                                interactionSource = notifInteraction,
                                indication = ripple(bounded = true, color = Color.White)
                            ) {
                                showNotificationsModal = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AnonChatIcons.Bell,
                            contentDescription = "Notifications",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )

                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = (-2).dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFF141416), CircleShape)
                                    .padding(horizontal = if (unreadCount > 9) 3.5.dp else 4.5.dp, vertical = 0.5.dp)
                            ) {
                                Text(
                                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    lineHeight = 9.sp
                                )
                            }
                        }
                    }

                    // Refresh Button [↻]
                    val refreshInteraction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable(
                                interactionSource = refreshInteraction,
                                indication = ripple(bounded = true, color = Color.White)
                            ) { viewModel.loadRooms() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AnonChatIcons.Refresh,
                            contentDescription = "Reload Rooms",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )

            // ── Main Content Area ────────────────────────────────────────
            when {
                // Loading skeletons
                uiState.isLoading && uiState.publicRooms.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = AnonChatSpacing.lg, vertical = AnonChatSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(AnonChatSpacing.sm)
                    ) {
                        repeat(4) { RoomItemSkeleton() }
                    }
                }

                // Empty state
                uiState.publicRooms.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = AnonChatSpacing.lg, vertical = AnonChatSpacing.md),
                        verticalArrangement = Arrangement.Top
                    ) {
                        Spacer(Modifier.height(AnonChatSpacing.xl))
                        EmptyState(
                            title = "No Public Rooms",
                            message = "No active conversation channels found on the server.",
                            icon = AnonChatIcons.Chat,
                            actionLabel = "Refresh",
                            onAction = { viewModel.loadRooms() }
                        )
                    }
                }

                // ── 3D Card Carousel with Room Cards ─────────────────────
                else -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Spacer(Modifier.height(16.dp))

                        // HorizontalPager for Carousel
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(horizontal = 44.dp),
                            pageSpacing = 16.dp,
                            verticalAlignment = Alignment.CenterVertically
                        ) { page ->
                            val room = uiState.publicRooms[page]
                            val rawOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            val pageOffset = rawOffset.absoluteValue.coerceIn(0f, 1f)
                            val scale = lerp(0.88f, 1f, 1f - pageOffset)
                            val alpha = lerp(0.40f, 1f, 1f - pageOffset)
                            val rotateY = -rawOffset.coerceIn(-1f, 1f) * 6f
                            val isCurrentPage = page == pagerState.currentPage

                            RoomCarouselCard(
                                room = room,
                                isCurrentPage = isCurrentPage,
                                activityLevel = uiState.roomActivityLevels[room.id] ?: RoomActivityLevel.IDLE,
                                onJoinClick = {
                                    onRoomClick(room.id, room.name)
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        this.alpha = alpha
                                        rotationY = rotateY
                                        translationY = pageOffset * 14.dp.toPx()
                                        cameraDistance = 16f * density
                                    }
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        // Carousel Page Indicator Dots with fluid stretch animation
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(uiState.publicRooms.size) { index ->
                                val isSelected = pagerState.currentPage == index
                                val dotWidth by animateDpAsState(
                                    targetValue = if (isSelected) 22.dp else 6.dp,
                                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                                    label = "carouselDotWidth"
                                )
                                Box(
                                    modifier = Modifier
                                        .height(6.dp)
                                        .width(dotWidth)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.28f))
                                )
                            }
                        }

                        // Flexible remaining space to prevent colliding with the bottom navigation
                        Spacer(Modifier.height(88.dp))
                    }
                }
            }
        }

        if (showNotificationsModal && notificationRepository != null) {
            NotificationsModal(
                notificationRepository = notificationRepository,
                onDismiss = { showNotificationsModal = false },
                onOpenRoom = onRoomClick
            )
        }
    }
}

/**
 * Modern Carousel Room Card featuring dark frosted depth, full-bleed hero image,
 * clean group title, description, and capsule action slider.
 */
@Composable
private fun RoomCarouselCard(
    room: PublicRoomDto,
    isCurrentPage: Boolean,
    activityLevel: RoomActivityLevel,
    onJoinClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = remember(room) { RoomVisualRegistry.getPalette(room) }
    val imageRes = remember(room) { RoomVisualRegistry.getImage(room) }

    // Only compute breathing/glow animations when the room is active AND visible (isCurrentPage)
    val shouldAnimate = isCurrentPage && activityLevel != RoomActivityLevel.IDLE
    val pulseTransition = rememberInfiniteTransition(label = "roomHeat")

    val breathingBorderAlpha by if (shouldAnimate) {
        pulseTransition.animateFloat(
            initialValue = if (activityLevel == RoomActivityLevel.VERY_ACTIVE) 0.35f else 0.20f,
            targetValue = if (activityLevel == RoomActivityLevel.VERY_ACTIVE) 0.65f else 0.40f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = if (activityLevel == RoomActivityLevel.VERY_ACTIVE) 1600 else 3200,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "roomHeatAlpha"
        )
    } else {
        remember(isCurrentPage) { mutableStateOf(if (isCurrentPage) 0.30f else 0.12f) }
    }

    val topSheenAlpha by if (shouldAnimate && activityLevel == RoomActivityLevel.VERY_ACTIVE) {
        pulseTransition.animateFloat(
            initialValue = 0.03f,
            targetValue = 0.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "roomTopSheen"
        )
    } else {
        remember(isCurrentPage) { mutableStateOf(if (isCurrentPage) 0.025f else 0.01f) }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF08090C))
            .border(
                width = if (activityLevel == RoomActivityLevel.VERY_ACTIVE && isCurrentPage) 1.5.dp else 1.2.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        palette.cardBorderGlow.copy(alpha = breathingBorderAlpha * 1.2f),
                        Color.White.copy(alpha = breathingBorderAlpha * 0.35f),
                        Color.White.copy(alpha = 0.03f)
                    )
                ),
                shape = RoundedCornerShape(32.dp)
            )
    ) {
        // ── 1. Full-Bleed Card Image (Naturally occupies visual space from the very top) ──
        Image(
            painter = painterResource(imageRes),
            contentDescription = room.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // ── 2. Bottom Multi-Stop Dark Scrim for Title, Desc, and Slider ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        0.35f to Color.Transparent,
                        0.58f to Color(0xFF030406).copy(alpha = 0.70f),
                        0.78f to Color(0xFF020304).copy(alpha = 0.94f),
                        1.0f to Color(0xFF010203).copy(alpha = 0.98f)
                    )
                )
        )

        // ── 3. Subtle Top Edge Sheen for Glass/Specular Highlight ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            palette.cardSheen.copy(alpha = topSheenAlpha * 1.5f),
                            Color.Transparent
                        )
                    )
                )
        )

        // ── 4. Card Bottom Content Overlay ─────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = formatRoomTitle(room.name),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = (-0.4).sp,
                maxLines = 1,
                lineHeight = 28.sp
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = getRoomDescription(room.desc, room.name),
                fontSize = 13.5.sp,
                color = Color.White.copy(alpha = 0.75f),
                lineHeight = 18.sp,
                maxLines = 2
            )

            Spacer(Modifier.height(18.dp))

            // Action Slider to enter room
            SliderToEnter(
                text = "Slide to enter room",
                onCompleted = onJoinClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Self-contained linear interpolation helper.
 */
private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + fraction * (stop - start)
}

/**
 * Format channel title without leading hashtag or '/anon/' prefix (e.g. '/anon/ Public' -> 'Public').
 */
internal fun formatRoomTitle(name: String): String {
    var title = name.trimStart('#').trim()
    if (title.startsWith("/anon/", ignoreCase = true)) {
        title = title.removePrefix("/anon/").trim()
    } else if (title.startsWith("anon/", ignoreCase = true)) {
        title = title.removePrefix("anon/").trim()
    } else if (title.startsWith("/anon", ignoreCase = true)) {
        title = title.removePrefix("/anon").trim()
    }
    return title.ifBlank { name }
}

/**
 * Dynamic room description fallback.
 */
private fun getRoomDescription(desc: String?, name: String): String {
    if (!desc.isNullOrBlank()) return desc
    val key = name.lowercase()
    return when {
        key.contains("cse") -> "Computer Science & Engineering"
        key.contains("anon") || key.contains("public") -> "All Campus Anonymous Chat"
        key.contains("elect") -> "Electronics & Communication"
        key.contains("mech") -> "Mechanical Engineering"
        key.contains("civil") -> "Civil Engineering"
        key.contains("faculty") -> "Faculty & Academics Lounge"
        key.contains("hostel") -> "Hostel Life & Late Night Talks"
        else -> "Campus anonymous group discussions"
    }
}

/**
 * Icon resolution matching room category.
 */
private fun getRoomIcon(icon: String?, name: String): ImageVector {
    val key = (icon ?: name).lowercase()
    return when {
        key.contains("terminal") || key.contains("cse") || key.contains("code") || key.contains("tech") || key.contains("dev") -> AnonChatIcons.Terminal
        key.contains("zap") || key.contains("elect") || key.contains("ece") || key.contains("chip") || key.contains("circuit") -> AnonChatIcons.Cpu
        key.contains("tool") || key.contains("mech") || key.contains("gear") || key.contains("wrench") || key.contains("eng") -> AnonChatIcons.Wrench
        key.contains("civil") || key.contains("build") || key.contains("arch") || key.contains("construct") -> AnonChatIcons.Building
        key.contains("book") || key.contains("faculty") || key.contains("prof") || key.contains("edu") || key.contains("grad") -> AnonChatIcons.GraduationCap
        key.contains("hostel") || key.contains("dorm") || key.contains("bed") || key.contains("living") || key.contains("moon") -> AnonChatIcons.Bed
        key.contains("globe") || key.contains("public") || key.contains("anon") || key.contains("world") || key.contains("campus") -> AnonChatIcons.Globe
        key.contains("lock") || key.contains("shield") || key.contains("sec") || key.contains("privacy") -> AnonChatIcons.Lock
        else -> AnonChatIcons.Chat
    }
}
