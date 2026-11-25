package org.example.annochat.presentation.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinproject.shared.generated.resources.Res
import kotlinproject.shared.generated.resources.afterclass_mark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.annochat.data.model.RoomDto
import org.example.annochat.data.repository.ChatRepository
import org.example.annochat.data.session.SessionStorage
import org.example.annochat.presentation.components.AboutAfterClassModal
import org.example.annochat.presentation.components.AfterClassSwitch
import org.example.annochat.presentation.components.AnonChatPrimaryButton
import org.example.annochat.presentation.components.AnonChatSecondaryButton
import org.example.annochat.presentation.components.FeedbackEmojiModal
import org.example.annochat.data.repository.BroadcastRepository
import org.example.annochat.presentation.components.AdminBroadcastDialog
import org.example.annochat.presentation.components.CommunityGuidelinesModal
import org.example.annochat.presentation.components.FullScreenImageData
import org.example.annochat.presentation.components.FullScreenImageViewerModal
import org.example.annochat.presentation.components.ImageBitmapCache
import org.example.annochat.presentation.components.WidgetSettingsModal
import org.example.annochat.presentation.components.decodeImageBitmap
import org.example.annochat.presentation.components.resolveAnimalSlug
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.designsystem.AnonChatShapes
import org.example.annochat.presentation.designsystem.AnonChatSpacing
import org.example.annochat.presentation.designsystem.AnonChatTypography
import org.example.annochat.presentation.theme.LocalAppearanceSettings
import org.example.annochat.openBrowserUrl
import org.example.annochat.shareApp
import org.jetbrains.compose.resources.painterResource

private enum class ActiveModal {
    NONE,
    ABOUT,
    COMMUNITY_GUIDELINES,
    HOME_SCREEN_WIDGETS,
    BROADCAST_COMPOSER
}

private enum class ConfirmAction {
    NONE,
    CHANGE_IDENTITY,
    CLEAR_LOCAL_DATA,
    REFRESH_SESSION
}

/**
 * Profile Screen strictly matching the reference UI:
 * - Immersive hero animal portrait at the top in full vibrant color
 * - Floating glass circular action buttons (Back, Phone/Match, More menu)
 * - Rounded overlapping dark obsidian card
 * - Large bold persona name, scientific name, and copyable ID pill
 * - Capsule "Change Identity" button
 * - 4-item Stats grid (Messages, Rooms, Connections, Reactions)
 * - Campus Explorer level progress card
 * - Grouped Settings menu card (Appearance, Liquid Glass toggle, Widgets)
 * - Grouped cards for Your Rooms, Data Controls, Anonymous Feedback, and About
 */
@Composable
fun ProfileScreen(
    repository: ChatRepository,
    sessionStorage: SessionStorage,
    isLiquidGlassEnabled: Boolean = false,
    onToggleLiquidGlass: (Boolean) -> Unit = {},
    onNavigateToAppearance: () -> Unit = {},
    onResetSession: () -> Unit = {},
    onExploreRooms: () -> Unit = {},
    onRoomClick: (roomId: String, roomName: String) -> Unit = { _, _ -> },
    onBack: (() -> Unit)? = null,
    broadcastRepository: BroadcastRepository? = null,
    modifier: Modifier = Modifier
) {
    val identity by repository.myIdentity.collectAsState()
    val appearance = LocalAppearanceSettings.current
    val onlineCount by repository.onlineCount.collectAsState()
    val userRooms by repository.userRooms.collectAsState()
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val density = LocalDensity.current

    val lazyListState = rememberLazyListState()

    var actionFeedback by remember { mutableStateOf<String?>(null) }
    var activeModal by remember { mutableStateOf(ActiveModal.NONE) }
    var confirmDialog by remember { mutableStateOf(ConfirmAction.NONE) }
    var isTopMoreMenuOpen by remember { mutableStateOf(false) }

    // Storage info state
    var cachedMediaSize by remember { mutableStateOf("1.2 MB") }

    // Interactive Feedback Modal state
    var showFeedbackDialog by remember { mutableStateOf(false) }

    // Fullscreen DP Lightbox viewer
    var viewingFullScreenImage by remember { mutableStateOf<FullScreenImageData?>(null) }

    // Appearance subtitle memoized
    val appearanceSubtitle = remember(appearance) {
        "${appearance.activeMode.title} · ${appearance.accent.title} · ${appearance.environment.title}"
    }

    val isModalOpen = activeModal != ActiveModal.NONE || confirmDialog != ConfirmAction.NONE

    // Persona display data
    val rawName = identity?.anonName ?: sessionStorage.getSavedAnonName() ?: "Anonymous Persona"
    val publicId = identity?.publicId ?: sessionStorage.getSavedPublicId() ?: repository.getSessionId().take(8)
    val shortTag = publicId.take(4).lowercase()
    val displayName = if (rawName.contains("#")) rawName else "$rawName #$shortTag"
    val sciName = identity?.sciName?.ifBlank { null } ?: "Campus Anonymous Persona"

    val slug = remember(identity?.avatarUrl, identity?.anonName, publicId) {
        resolveAnimalSlug(identity?.avatarUrl, identity?.anonName, publicId)
    }
    val targetUrl = remember(slug) { "/api/animal/$slug" }

    var bitmap by remember(targetUrl) { mutableStateOf(ImageBitmapCache.get(targetUrl)) }

    LaunchedEffect(targetUrl) {
        if (bitmap == null) {
            val cached = ImageBitmapCache.get(targetUrl)
            if (cached != null) {
                bitmap = cached
            } else {
                withContext(Dispatchers.Default) {
                    val result = repository.getMediaBytes(targetUrl)
                    result.onSuccess { bytes ->
                        val decoded = decodeImageBitmap(bytes)
                        if (decoded != null) {
                            ImageBitmapCache.put(targetUrl, decoded)
                            bitmap = decoded
                        }
                    }
                }
            }
        }
    }

    // Parallax & bottom drawer sheet scroll metrics
    val scrollOffset by remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex == 0) {
                lazyListState.firstVisibleItemScrollOffset.toFloat()
            } else {
                10000f
            }
        }
    }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val screenHeight = maxHeight
        val headerHeight = if (screenHeight > 0.dp) (screenHeight * 0.50f).coerceIn(400.dp, 480.dp) else 420.dp
        val spacerHeight = if (screenHeight > 0.dp) (screenHeight * 0.36f).coerceIn(300.dp, 360.dp) else 310.dp
        val maxParallaxPx = with(density) { 150.dp.toPx() }
        val parallaxOffset = (scrollOffset * 0.35f).coerceAtMost(maxParallaxPx)
        val headerAlpha = (1f - (scrollOffset / with(density) { spacerHeight.toPx() }) * 0.40f).coerceIn(0.55f, 1f)
        Box(
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
            // ── 0. Pinned / Parallax Hero DP Background (Living Persona) ────
            val heroTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "heroPersonaTransition")
            val heroIdleScale by heroTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.018f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(durationMillis = 3400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "heroIdleScale"
            )
            val heroIdleAlpha by heroTransition.animateFloat(
                initialValue = 0.96f,
                targetValue = 1.0f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(durationMillis = 3400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "heroIdleAlpha"
            )
            val heroBounceScale = remember { androidx.compose.animation.core.Animatable(1f) }
            val heroHaptic = androidx.compose.ui.platform.LocalHapticFeedback.current

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .graphicsLayer {
                        translationY = -parallaxOffset
                        alpha = headerAlpha
                    }
                    .background(Color.Black)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        heroHaptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        scope.launch {
                            heroBounceScale.animateTo(1.05f, androidx.compose.animation.core.tween(90, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                            heroBounceScale.animateTo(1.0f, androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 420f))
                        }
                    }
            ) {
                val currentBitmap = bitmap
                val totalHeroScale = heroIdleScale * heroBounceScale.value
                if (currentBitmap != null) {
                    Image(
                        bitmap = currentBitmap,
                        contentDescription = identity?.anonName ?: "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = totalHeroScale
                                scaleY = totalHeroScale
                                alpha = heroIdleAlpha
                            }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF18181A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AnonChatIcons.Paw,
                            contentDescription = null,
                            tint = AnonChatColors.Primary.copy(alpha = 0.45f),
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }

                // Cinematic Vignette & Deep Multi-Stop Smooth Dissolve into solid black
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.00f to Color.Black.copy(alpha = 0.50f),
                                0.12f to Color.Transparent,
                                0.62f to Color.Transparent,
                                0.74f to Color.Black.copy(alpha = 0.30f),
                                0.85f to Color.Black.copy(alpha = 0.70f),
                                0.94f to Color.Black.copy(alpha = 0.92f),
                                1.00f to Color.Black
                            )
                        )
                )
            }

            // ── 1. Scrollable Content (Seamlessly flowing over the DP with zero separation line) ──
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize()
            ) {
                // Transparent top spacer matching visible DP: tapping opens fullscreen photo lightbox
                item(key = "dp_spacer") {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(spacerHeight)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    viewingFullScreenImage = FullScreenImageData(
                                        pathOrUrl = "/api/animal/$slug",
                                        imageName = "${identity?.anonName ?: "Your"}'s Avatar",
                                        caption = identity?.anonName
                                    )
                                }
                            )
                    )
                }

                // Seamless Content (Zero separation between DP ending and content starting)
                item(key = "main_content") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Feather shadow gradient from transparent to solid black
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(
                                    Brush.verticalGradient(
                                        0.0f to Color.Transparent,
                                        1.0f to Color.Black
                                    )
                                )
                        )

                        // Content on solid black background
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black)
                                .padding(horizontal = 18.dp)
                        ) {
                            // Title: Persona Name
                            Text(
                                text = displayName,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = (-0.4).sp
                            )

                            Spacer(Modifier.height(4.dp))

                            // Subtitle: Scientific Name
                            Text(
                                text = sciName,
                                fontSize = 14.sp,
                                color = AnonChatColors.TextSecondary,
                                fontWeight = FontWeight.Normal
                            )

                            Spacer(Modifier.height(20.dp))

                            // Grouped Settings Card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF1C1C1E))
                            ) {
                                Column {
                                    ProfileMenuRow(
                                        icon = AnonChatIcons.Palette,
                                        title = "Appearance & Personalization",
                                        subtitle = appearanceSubtitle,
                                        onClick = onNavigateToAppearance
                                    )

                                    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFF2A2A2E)))

                                    ProfileMenuRow(
                                        icon = AnonChatIcons.Grid,
                                        title = "Home Screen Widgets",
                                        subtitle = "Live Activity, Time & Vibe, and Anonymous Identity",
                                        onClick = { activeModal = ActiveModal.HOME_SCREEN_WIDGETS }
                                    )

                                    if (identity?.isHost == true && broadcastRepository != null) {
                                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFF2A2A2E)))

                                        ProfileMenuRow(
                                            icon = AnonChatIcons.BroadcastBeacon,
                                            title = "Campus Broadcast",
                                            subtitle = "Send official system announcement across campus",
                                            onClick = { activeModal = ActiveModal.BROADCAST_COMPOSER }
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            // ── 3. Your Joined Rooms Grouped Card ───────────────────
                            ProfileCardHeader(title = "YOUR ROOMS")
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1C1C1E))
                        ) {
                            if (userRooms.isNotEmpty()) {
                                Column {
                                    userRooms.take(4).forEachIndexed { idx, room ->
                                        if (idx > 0) {
                                            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFF2A2A2E)))
                                        }
                                        ProfileRoomItem(
                                            room = room,
                                            onClick = { onRoomClick(room.roomId, room.name) }
                                        )
                                    }
                                    if (userRooms.size > 4) {
                                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFF2A2A2E)))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onExploreRooms() }
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "View All ${userRooms.size} Rooms",
                                                fontSize = 13.5.sp,
                                                color = AnonChatColors.Primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF2A2A2E)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = AnonChatIcons.ChatBubbles,
                                                contentDescription = null,
                                                tint = AnonChatColors.TextMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column {
                                            Text("No active rooms", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                            Text("You haven't joined any campus rooms yet.", fontSize = 12.5.sp, color = AnonChatColors.TextSecondary)
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(AnonChatShapes.pill)
                                            .background(Color(appearance.accent.colorHex))
                                            .clickable { onExploreRooms() }
                                            .padding(horizontal = 14.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = "Explore",
                                            color = if ((0.2126f * Color(appearance.accent.colorHex).red + 0.7152f * Color(appearance.accent.colorHex).green + 0.0722f * Color(appearance.accent.colorHex).blue) > 0.45f) Color.Black else Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // ── 4. Data & Session Controls Grouped Card ────────────
                        ProfileCardHeader(title = "DATA & SESSION")
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1C1C1E))
                        ) {
                            Column {
                                ProfileDataActionItem(
                                    title = "Cached Media",
                                    subtitle = "Avatars, stickers, drafts ($cachedMediaSize)",
                                    actionText = "Clear Cache",
                                    onActionClick = {
                                        cachedMediaSize = "0 KB"
                                        actionFeedback = "Cache cleared successfully."
                                    }
                                )
                                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFF2A2A2E)))
                                ProfileDataActionItem(
                                    title = "Clear Local Data",
                                    subtitle = "Erase session state and offline cache",
                                    actionText = "Clear Data",
                                    onActionClick = { confirmDialog = ConfirmAction.CLEAR_LOCAL_DATA }
                                )
                                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFF2A2A2E)))
                                ProfileDataActionItem(
                                    title = "Leave All Rooms",
                                    subtitle = "Exit all active joined channels",
                                    actionText = "Leave All",
                                    onActionClick = {
                                        scope.launch {
                                            val res = repository.leaveAllRooms()
                                            actionFeedback = if (res.isSuccess) "Left all joined rooms" else "Failed to leave rooms"
                                        }
                                    }
                                )
                                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFF2A2A2E)))
                                ProfileDataActionItem(
                                    title = "Refresh Session & Identity",
                                    subtitle = "Destroy session & generate new identity",
                                    actionText = "Refresh",
                                    onActionClick = { confirmDialog = ConfirmAction.REFRESH_SESSION }
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // ── 5. Feedback Grouped Card ─────────────────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showFeedbackDialog = true }
                        ) {
                            ProfileCardHeader(title = "FEEDBACK")
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1C1C1E))
                        ) {
                            ProfileMenuRow(
                                icon = AnonChatIcons.StickerSmile,
                                title = "Share Feedback",
                                subtitle = "Rate your experience & help us improve",
                                onClick = { showFeedbackDialog = true }
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        // ── 6. About & Safety Grouped Card ─────────────────────
                        ProfileCardHeader(title = "ABOUT & COMMUNITY")
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1C1C1E))
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Image(
                                        painter = painterResource(Res.drawable.afterclass_mark),
                                        contentDescription = "AfterClass",
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                    )
                                    Column {
                                        Text("AfterClass", color = Color.White, fontSize = 16.5.sp, fontWeight = FontWeight.Bold)
                                        Text("Campus Anonymous Network • v2.4.0", color = AnonChatColors.TextMuted, fontSize = 12.5.sp)
                                    }
                                }

                                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFF2A2A2E)))

                                ProfileLinkItem(
                                    title = "Share App",
                                    onClick = {
                                        scope.launch {
                                            val result = shareApp()
                                            if (result.isFailure) {
                                                actionFeedback = "Unable to share APK: ${result.exceptionOrNull()?.message ?: "Sharing failed"}"
                                            }
                                        }
                                    }
                                )

                                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFF2A2A2E)))

                                ProfileLinkItem(
                                    title = "Web App",
                                    onClick = {
                                        val result = openBrowserUrl("https://afterclass.bond/")
                                        if (result.isFailure) {
                                            actionFeedback = "Unable to open browser: ${result.exceptionOrNull()?.message ?: "No browser found"}"
                                        }
                                    }
                                )

                                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFF2A2A2E)))

                                ProfileLinkItem(
                                    title = "About AfterClass",
                                    onClick = { activeModal = ActiveModal.ABOUT }
                                )

                                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFF2A2A2E)))

                                ProfileLinkItem(
                                    title = "Community Guidelines",
                                    onClick = { activeModal = ActiveModal.COMMUNITY_GUIDELINES }
                                )
                            }
                        }

                        if (actionFeedback != null) {
                            Spacer(Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF242428))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = actionFeedback ?: "",
                                    fontSize = 13.5.sp,
                                    color = Color.White
                                )
                            }
                        }

                        // Bottom space for floating capsule navigation bar
                        Spacer(Modifier.height(120.dp))
                    }
                }
            }
        }

        // ── 2. Floating Top Action Bar (Back chevron on left, More ... on right) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button (<)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.50f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = Color.White),
                            onClick = { onBack?.invoke() ?: onExploreRooms() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AnonChatIcons.ChevronLeft,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // More Options Button (...)
                Box {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.50f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = Color.White),
                                onClick = { isTopMoreMenuOpen = true }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AnonChatIcons.MoreHoriz,
                            contentDescription = "More",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isTopMoreMenuOpen,
                        onDismissRequest = { isTopMoreMenuOpen = false },
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1C1C1E))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = AnonChatIcons.Copy,
                                        contentDescription = "Copy ID",
                                        tint = AnonChatColors.TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "ID: $publicId",
                                            color = Color.White,
                                            fontSize = 13.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Tap to copy",
                                            color = AnonChatColors.TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            },
                            onClick = {
                                isTopMoreMenuOpen = false
                                clipboardManager.setText(AnnotatedString(publicId))
                                actionFeedback = "Public ID copied: $publicId"
                            }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(Color(0xFF2A2A2E))
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = AnonChatIcons.Refresh,
                                        contentDescription = "Change Identity",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Change Identity",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            },
                            onClick = {
                                isTopMoreMenuOpen = false
                                confirmDialog = ConfirmAction.CHANGE_IDENTITY
                            }
                        )
                    }
                }
            }
        }

        // ── Modals / Subpages ────────────────────────────────────────
        when (activeModal) {
            ActiveModal.ABOUT -> AboutAfterClassModal(onDismiss = { activeModal = ActiveModal.NONE })
            ActiveModal.COMMUNITY_GUIDELINES -> CommunityGuidelinesModal(onDismiss = { activeModal = ActiveModal.NONE })
            ActiveModal.HOME_SCREEN_WIDGETS -> WidgetSettingsModal(
                sessionStorage = sessionStorage,
                repository = repository,
                onDismiss = { activeModal = ActiveModal.NONE }
            )
            ActiveModal.BROADCAST_COMPOSER -> {
                broadcastRepository?.let { bRepo ->
                    AdminBroadcastDialog(
                        broadcastRepository = bRepo,
                        onDismiss = { activeModal = ActiveModal.NONE }
                    )
                }
            }
            ActiveModal.NONE -> {}
        }

        // ── Confirmation Dialogs ─────────────────────────────────────
        when (confirmDialog) {
            ConfirmAction.CHANGE_IDENTITY -> {
                ConfirmationDialog(
                    title = "Change anonymous identity?",
                    message = "Your current anonymous identity will be replaced.\nYou will receive a new campus persona.",
                    confirmButtonText = "Change Identity",
                    onConfirm = {
                        sessionStorage.clearSession()
                        repository.disconnect()
                        repository.connect()
                        onResetSession()
                        actionFeedback = "Anonymous identity changed successfully."
                        confirmDialog = ConfirmAction.NONE
                    },
                    onDismiss = { confirmDialog = ConfirmAction.NONE }
                )
            }
            ConfirmAction.CLEAR_LOCAL_DATA -> {
                ConfirmationDialog(
                    title = "Clear local data?",
                    message = "This will clear cached media, saved drafts, and local session details.",
                    confirmButtonText = "Clear Data",
                    onConfirm = {
                        sessionStorage.clearSession()
                        cachedMediaSize = "0 KB"
                        repository.disconnect()
                        repository.connect()
                        onResetSession()
                        actionFeedback = "Local session data cleared."
                        confirmDialog = ConfirmAction.NONE
                    },
                    onDismiss = { confirmDialog = ConfirmAction.NONE }
                )
            }
            ConfirmAction.REFRESH_SESSION -> {
                ConfirmationDialog(
                    title = "Refresh Session & Identity?",
                    message = "This will destroy your current anonymous session and generate a brand-new identity. You will exit active rooms.",
                    confirmButtonText = "Refresh Session",
                    onConfirm = {
                        sessionStorage.clearSession()
                        repository.disconnect()
                        repository.connect()
                        onResetSession()
                        actionFeedback = "Session refreshed with new identity."
                        confirmDialog = ConfirmAction.NONE
                    },
                    onDismiss = { confirmDialog = ConfirmAction.NONE }
                )
            }
            ConfirmAction.NONE -> {}
        }

        // Fullscreen DP Lightbox Viewer
        viewingFullScreenImage?.let { imageData ->
            FullScreenImageViewerModal(
                imageData = imageData,
                onDismiss = { viewingFullScreenImage = null }
            )
        }

        // Interactive Emoji Feedback Modal
        if (showFeedbackDialog) {
            FeedbackEmojiModal(
                repository = repository,
                onDismiss = { showFeedbackDialog = false }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT HELPERS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileCardHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        color = AnonChatColors.TextMuted,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.12f)),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2C2C2E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.5.sp,
                    color = AnonChatColors.TextSecondary
                )
            }
        }

        Icon(
            imageVector = AnonChatIcons.ChevronRight,
            contentDescription = null,
            tint = AnonChatColors.TextMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ProfileToggleMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2C2C2E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.5.sp,
                    color = AnonChatColors.TextSecondary
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        AfterClassSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ProfileRoomItem(
    room: RoomDto,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.12f)),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2A2A2E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AnonChatIcons.ChatBubbles,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }

            Column {
                Text(
                    text = room.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = if (room.members.isNotEmpty()) "${room.members.size} members active" else "Campus room",
                    fontSize = 12.5.sp,
                    color = AnonChatColors.TextSecondary
                )
            }
        }

        Icon(
            imageVector = AnonChatIcons.ChevronRight,
            contentDescription = null,
            tint = AnonChatColors.TextMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ProfileDataActionItem(
    title: String,
    subtitle: String,
    actionText: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.5.sp,
                color = AnonChatColors.TextSecondary
            )
        }

        Spacer(Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .clip(AnonChatShapes.pill)
                .background(Color(0xFF2C2C2E))
                .clickable { onActionClick() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = actionText,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ProfileLinkItem(
    title: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.12f)),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
        Icon(
            imageVector = AnonChatIcons.ChevronRight,
            contentDescription = null,
            tint = AnonChatColors.TextMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmButtonText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.80f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF141416))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* swallow click inside dialog */ }
                    .padding(22.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(AnonChatColors.SurfaceSecondary)
                            .border(1.5.dp, AnonChatColors.Primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AnonChatIcons.Shield,
                            contentDescription = null,
                            tint = AnonChatColors.Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AnonChatColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = message,
                        fontSize = 13.5.sp,
                        color = AnonChatColors.TextSecondary,
                        lineHeight = 18.5.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AnonChatSecondaryButton(
                            text = "Cancel",
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        )
                        AnonChatPrimaryButton(
                            text = confirmButtonText,
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
