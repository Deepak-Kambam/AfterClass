package org.example.annochat.presentation.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import org.example.annochat.presentation.components.LivePresenceBadge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.example.annochat.data.model.OnlineUserDto
import org.example.annochat.data.model.PredefinedChannels
import org.example.annochat.data.model.PublicRoomDto
import org.example.annochat.data.model.RoomDto
import org.example.annochat.data.model.isRoomMatching
import org.example.annochat.data.repository.ChatRepository
import org.example.annochat.presentation.components.AnimalAvatar
import org.example.annochat.presentation.components.AnonChatLargeHeader
import org.example.annochat.presentation.components.CreateGroupModal
import org.example.annochat.presentation.components.FullScreenImageData
import org.example.annochat.presentation.components.FullScreenImageViewerModal
import org.example.annochat.presentation.components.ActivePeersEmptyScreen
import org.example.annochat.presentation.components.PublicGroupsEmptyScreen
import org.example.annochat.presentation.components.MyGroupsEmptyScreen
import org.example.annochat.presentation.components.resolveAnimalSlug
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.designsystem.AnonChatShapes
import org.example.annochat.presentation.designsystem.AnonChatSpacing
import org.example.annochat.presentation.designsystem.AnonChatTypography
import org.example.annochat.presentation.theme.LocalAppearanceSettings
import kotlin.math.roundToInt

@Composable
fun OnlineUsersScreen(
    repository: ChatRepository,
    onDirectChatCreated: (roomId: String, roomName: String) -> Unit = { _, _ -> },
    primarySegment: Int = 0,
    onPrimarySegmentSelected: (Int) -> Unit = {},
    groupsSegment: Int = 0,
    onGroupsSegmentSelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val appearance = LocalAppearanceSettings.current
    val accentColor = Color(appearance.accent.colorHex)
    val isLight = (0.2126f * accentColor.red + 0.7152f * accentColor.green + 0.0722f * accentColor.blue) > 0.45f
    val onAccentColor = if (isLight) Color.Black else Color.White

    val onlineCount by repository.onlineCount.collectAsState()
    val myIdentity by repository.myIdentity.collectAsState()
    val onlineUsers by repository.onlineUsers.collectAsState()
    val rawDiscoverableGroups by repository.discoverableGroups.collectAsState()
    val rawUserRooms by repository.userRooms.collectAsState()
    val hasLoadedOnce by repository.hasLoadedOnlineUsers.collectAsState()
    val scope = rememberCoroutineScope()

    // Back navigation hierarchy within Online / Groups screen:
    // When inside Groups tab, pressing back steps back to Public Groups, then Active peers
    org.example.annochat.presentation.navigation.BackHandler(enabled = primarySegment == 1) {
        if (groupsSegment != 0) {
            onGroupsSegmentSelected(0)
        } else {
            onPrimarySegmentSelected(0)
        }
    }

    var isManualRefreshing by remember { mutableStateOf(false) }
    var showCreateModal by remember { mutableStateOf(false) }
    var viewingFullScreenImage by remember { mutableStateOf<FullScreenImageData?>(null) }
    var pinnedRoomIds by remember { mutableStateOf(setOf<String>()) }
    var leftRoomIds by remember { mutableStateOf(setOf<String>()) }
    var deletedRoomIds by remember { mutableStateOf(setOf<String>()) }

    // 1. Discoverable Public Groups: ONLY user-created public groups from Online (never predefined campus channels)
    val publicRooms = remember(rawDiscoverableGroups, rawUserRooms, deletedRoomIds, pinnedRoomIds) {
        val combined = rawDiscoverableGroups
            .filter { !PredefinedChannels.isPredefined(it.id) && !deletedRoomIds.contains(it.id) }
            .toMutableList()
        for (room in rawUserRooms) {
            if (room.isPublic && !room.isPrivateGroup && !PredefinedChannels.isPredefined(room.roomId) && !deletedRoomIds.contains(room.roomId)) {
                if (combined.none { it.id.equals(room.roomId, ignoreCase = true) || it.name.equals(room.name, ignoreCase = true) }) {
                    combined.add(
                        PublicRoomDto(
                            id = room.roomId,
                            name = room.cleanDisplayName,
                            desc = room.effectiveDesc ?: room.desc,
                            memberCount = room.memberCount
                        )
                    )
                }
            }
        }
        combined.sortedByDescending { pinnedRoomIds.contains(it.id) }
    }

    // 2. My Groups: user-specific private and joined/owned groups (never predefined campus channels)
    val userRooms = remember(rawUserRooms, deletedRoomIds) {
        rawUserRooms.filter { (it.isPrivateGroup || !it.isPublic || it.isOwner) && !PredefinedChannels.isPredefined(it.roomId) && !deletedRoomIds.contains(it.roomId) }
    }

    val activePeers = remember(onlineUsers, myIdentity) {
        val myPid = myIdentity?.publicId
        onlineUsers.filter { it.publicId.isNotBlank() && it.publicId != myPid }
    }

    val sortedUserRooms = remember(userRooms, pinnedRoomIds) {
        userRooms.sortedByDescending { pinnedRoomIds.contains(it.roomId) }
    }

    fun refreshData(isManual: Boolean = false) {
        scope.launch {
            if (isManual) isManualRefreshing = true
            val r1 = async { repository.loadUserRooms() }
            val r2 = async { repository.loadDiscoverableGroups() }
            val r3 = async { repository.loadOnlineUsers() }
            r1.await()
            r2.await()
            r3.await()
            if (isManual) isManualRefreshing = false
        }
    }

    LaunchedEffect(Unit) {
        refreshData(isManual = false)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Large Transparent Header ───────────────────────────────────────
            AnonChatLargeHeader(
                title = "Online",
                rightContent = {
                    // Online live count badge with ambient presence pulse
                    LivePresenceBadge(onlineCount = onlineCount)

                    // Refresh Button with smooth micro-animation during refresh
                    val refreshInteraction = remember { MutableInteractionSource() }
                    val infiniteTransition = rememberInfiniteTransition()
                    val spinAngle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 800, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "RefreshSpin"
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable(
                                interactionSource = refreshInteraction,
                                indication = ripple(bounded = true, color = Color.White)
                            ) { refreshData(isManual = true) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AnonChatIcons.Refresh,
                            contentDescription = "Refresh Data",
                            tint = Color.White,
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer {
                                    if (isManualRefreshing) {
                                        rotationZ = spinAngle
                                    }
                                }
                        )
                    }
                }
            )

            // ── Primary Segment Button: [ Active ] / [ Groups ] ────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AnonChatSpacing.md)
                    .padding(top = 4.dp, bottom = 12.dp)
            ) {
                CapsuledSegmentControl(
                    selectedSegment = primarySegment,
                    onSegmentSelected = { onPrimarySegmentSelected(it) },
                    labels = listOf("Active (${activePeers.size})", "Groups (${publicRooms.size + userRooms.size})"),
                    accentColor = accentColor,
                    onAccentColor = onAccentColor
                )
            }

            if (primarySegment == 1) {
                // ── Groups Segment: Nested [ Public Groups ] / [ My Groups ] ─
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AnonChatSpacing.md)
                        .padding(bottom = 12.dp)
                ) {
                    CapsuledSegmentControl(
                        selectedSegment = groupsSegment,
                        onSegmentSelected = { onGroupsSegmentSelected(it) },
                        labels = listOf("Public Groups (${publicRooms.size})", "My Groups (${userRooms.size})"),
                        accentColor = accentColor,
                        onAccentColor = onAccentColor
                    )
                }
            }

            // ── Content Area: Full Bleed Immersive Empty State or Scrollable List ──
            if (primarySegment == 0) {
                // ── Active Peers Segment ─────────────────────────────────
                if (activePeers.isEmpty() && (hasLoadedOnce || onlineCount <= 1)) {
                    ActivePeersEmptyScreen(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                } else if (!hasLoadedOnce && activePeers.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = AnonChatSpacing.md, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AnonChatColors.SurfacePrimary)
                                    .border(1.dp, AnonChatColors.Border, RoundedCornerShape(10.dp))
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(
                            start = AnonChatSpacing.md,
                            end = AnonChatSpacing.md,
                            top = 4.dp,
                            bottom = 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        item {
                            Text(
                                text = "ONLINE PEERS (${activePeers.size})",
                                style = AnonChatTypography.SectionHeader,
                                color = AnonChatColors.TextMuted,
                                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                            )
                        }
                        items(activePeers, key = { it.publicId }) { user ->
                            OnlineUserRow(
                                user = user,
                                repository = repository,
                                accentColor = accentColor,
                                onAccentColor = onAccentColor,
                                onAvatarClick = {
                                    val slug = resolveAnimalSlug(user.anonAvatar, user.anonName, user.publicId)
                                    viewingFullScreenImage = FullScreenImageData(
                                        pathOrUrl = "/api/animal/$slug",
                                        imageName = "${user.anonName ?: "User"}'s Avatar",
                                        caption = user.anonName
                                    )
                                },
                                onChatClick = {
                                    scope.launch {
                                        repository.requestPrivateChat(user.publicId)
                                    }
                                }
                            )
                        }
                    }
                }
            } else if (groupsSegment == 0) {
                // ── Public Groups ────────────────────────────────────
                if (publicRooms.isEmpty()) {
                    PublicGroupsEmptyScreen(
                        onCreateGroup = { showCreateModal = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(
                            start = AnonChatSpacing.md,
                            end = AnonChatSpacing.md,
                            top = 4.dp,
                            bottom = 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "DISCOVERABLE GROUPS (${publicRooms.size})",
                                    style = AnonChatTypography.SectionHeader,
                                    color = AnonChatColors.TextMuted
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.12f))
                                        .border(1.dp, accentColor.copy(alpha = 0.35f), CircleShape)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(bounded = true, color = accentColor)
                                        ) { showCreateModal = true }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = AnonChatIcons.Plus,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "Create",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor
                                        )
                                    }
                                }
                            }
                        }

                        items(publicRooms, key = { it.id }) { room ->
                            val isLocallyOwned = remember(room.id) { repository.sessionStorage.isMyOwnedRoom(room.id) }
                            val matchingUserRoom = remember(room.id, rawUserRooms) { rawUserRooms.find { isRoomMatching(it.roomId, room.id) } }
                            val isRoomAdmin = matchingUserRoom?.isUserAdmin(myIdentity?.publicId, isLocallyOwned) == true || isLocallyOwned
                            val isRoomPinned = pinnedRoomIds.contains(room.id)
                            val isRoomLeft = leftRoomIds.contains(room.id)
                            PublicGroupRow(
                                room = room,
                                isAdmin = isRoomAdmin,
                                isPinned = isRoomPinned,
                                isLeft = isRoomLeft,
                                accentColor = accentColor,
                                onAccentColor = onAccentColor,
                                onOpenRoom = { onDirectChatCreated(room.id, room.name) },
                                onPin = {
                                    pinnedRoomIds = if (isRoomPinned) {
                                        pinnedRoomIds - room.id
                                    } else {
                                        pinnedRoomIds + room.id
                                    }
                                },
                                onLeave = {
                                    // 1st swipe: Leave group
                                    leftRoomIds = leftRoomIds + room.id
                                    scope.launch {
                                        repository.leaveRoom(room.id)
                                    }
                                },
                                onDelete = {
                                    // 2nd swipe: Delete permanently from UI and backend
                                    deletedRoomIds = deletedRoomIds + room.id
                                    leftRoomIds = leftRoomIds - room.id
                                    pinnedRoomIds = pinnedRoomIds - room.id
                                    scope.launch {
                                        repository.deleteRoom(room.id)
                                        repository.loadDiscoverableGroups()
                                        repository.loadUserRooms()
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // ── My Groups ────────────────────────────────────────
                if (userRooms.isEmpty()) {
                    MyGroupsEmptyScreen(
                        onCreateGroup = { showCreateModal = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(
                            start = AnonChatSpacing.md,
                            end = AnonChatSpacing.md,
                            top = 4.dp,
                            bottom = 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "MY GROUPS (${userRooms.size})",
                                    style = AnonChatTypography.SectionHeader,
                                    color = AnonChatColors.TextMuted
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.12f))
                                        .border(1.dp, accentColor.copy(alpha = 0.35f), CircleShape)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(bounded = true, color = accentColor)
                                        ) { showCreateModal = true }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = AnonChatIcons.Plus,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "Create",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor
                                        )
                                    }
                                }
                            }
                        }

                        items(sortedUserRooms, key = { it.roomId }) { room ->
                            val isLocallyOwned = remember(room.roomId) { repository.sessionStorage.isMyOwnedRoom(room.roomId) }
                            val isRoomAdmin = room.isUserAdmin(myIdentity?.publicId, isLocallyOwned) || isLocallyOwned
                            val isRoomPinned = pinnedRoomIds.contains(room.roomId)
                            val isRoomLeft = leftRoomIds.contains(room.roomId)
                            JoinedGroupRow(
                                room = room,
                                isAdmin = isRoomAdmin,
                                isPinned = isRoomPinned,
                                isLeft = isRoomLeft,
                                accentColor = accentColor,
                                onAccentColor = onAccentColor,
                                onOpenRoom = { onDirectChatCreated(room.roomId, room.cleanDisplayName) },
                                onPin = {
                                    pinnedRoomIds = if (isRoomPinned) {
                                        pinnedRoomIds - room.roomId
                                    } else {
                                        pinnedRoomIds + room.roomId
                                    }
                                },
                                onLeave = {
                                    // 1st swipe: Leave group
                                    leftRoomIds = leftRoomIds + room.roomId
                                    scope.launch {
                                        repository.leaveRoom(room.roomId)
                                    }
                                },
                                onDelete = {
                                    // 2nd swipe: Delete permanently from UI and backend
                                    deletedRoomIds = deletedRoomIds + room.roomId
                                    leftRoomIds = leftRoomIds - room.roomId
                                    pinnedRoomIds = pinnedRoomIds - room.roomId
                                    scope.launch {
                                        repository.deleteRoom(room.roomId)
                                        repository.loadUserRooms()
                                        repository.loadDiscoverableGroups()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Full Screen DP Lightbox Viewer
        viewingFullScreenImage?.let { fullScreenData ->
            FullScreenImageViewerModal(
                imageData = fullScreenData,
                repository = repository,
                onDismiss = { viewingFullScreenImage = null }
            )
        }

        // Create Group Dialog
        if (showCreateModal) {
            CreateGroupModal(
                repository = repository,
                initialIsPublic = (groupsSegment == 0),
                onDismiss = { showCreateModal = false },
                onGroupCreated = { newRoom ->
                    // Auto-select Groups tab and appropriate category (0 for Public, 1 for My Groups / Private)
                    val isRoomPublic = newRoom.isPublic && !newRoom.isPrivateGroup
                    val targetCategory = if (isRoomPublic) 0 else 1
                    onPrimarySegmentSelected(1)
                    onGroupsSegmentSelected(targetCategory)
                    showCreateModal = false
                    leftRoomIds = leftRoomIds - newRoom.roomId
                    deletedRoomIds = deletedRoomIds - newRoom.roomId
                    onDirectChatCreated(newRoom.roomId, newRoom.cleanDisplayName)
                }
            )
        }
    }
}

@Composable
private fun OnlineUserRow(
    user: OnlineUserDto,
    repository: ChatRepository,
    accentColor: Color,
    onAccentColor: Color,
    onAvatarClick: () -> Unit = {},
    onChatClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Real animal avatar with online status ring and zoom-in click
            Box(contentAlignment = Alignment.BottomEnd) {
                AnimalAvatar(
                    repository = repository,
                    avatarUrl = user.anonAvatar,
                    animalName = user.anonName,
                    publicId = user.publicId,
                    size = 38.dp,
                    borderWidth = 1.dp,
                    borderColor = AnonChatColors.Border,
                    onClick = onAvatarClick
                )

                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(AnonChatColors.Success)
                        .border(1.dp, AnonChatColors.SurfacePrimary, CircleShape)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.anonName ?: "Anonymous Peer",
                    fontSize = 14.sp,
                    color = AnonChatColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!user.sciName.isNullOrBlank()) {
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = user.sciName,
                        style = AnonChatTypography.Caption,
                        color = AnonChatColors.TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = onChatClick,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = onAccentColor
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text(
                    text = "Chat",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = onAccentColor
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(AnonChatColors.BorderSubtle)
        )
    }
}

/**
 * Shared swipeable group card container.
 * Encapsulates the exact same horizontal drag physics, animation easing (FastOutSlowInEasing),
 * snapping, gesture thresholds, dynamic corner rounding, action reveal panels (Pin on right swipe,
 * Leave/Delete on left swipe), and confirmation dialogs for both Public and Private groups.
 */
@Composable
private fun SwipeableGroupCard(
    roomId: String,
    roomDisplayName: String,
    isAdmin: Boolean = false,
    isPinned: Boolean = false,
    isLeft: Boolean = false,
    accentColor: Color,
    onAccentColor: Color,
    onPin: () -> Unit = {},
    onLeave: () -> Unit = {},
    onDelete: () -> Unit = {},
    content: @Composable (hasLeft: Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Animatable offset in pixels; positive = swiped right (pin), negative = swiped left (delete/leave)
    val offsetX = remember { Animatable(0f) }

    // Swipe thresholds (in dp)
    val pinThresholdDp = 100.dp
    val deleteThresholdDp = 100.dp
    val pinThresholdPx = with(density) { pinThresholdDp.toPx() }
    val deleteThresholdPx = with(density) { deleteThresholdDp.toPx() }
    val maxSwipePx = with(density) { 140.dp.toPx() }

    // Track whether action was already triggered (prevent double-fire)
    var actionFired by remember { mutableStateOf(false) }

    // Dialog state
    var showPinDialog by remember { mutableStateOf(false) }
    var showLeaveDeleteDialog by remember { mutableStateOf(false) }

    // Track whether user has left the group:
    var hasLeft by remember(roomId, isLeft) { mutableStateOf(isLeft) }
    LaunchedEffect(isLeft) {
        hasLeft = isLeft
    }

    val isDeleteAction = hasLeft || (isAdmin && hasLeft)

    // Derived fractions [0..1]
    val pinFraction = (offsetX.value / pinThresholdPx).coerceIn(0f, 1f)
    val deleteFraction = (-offsetX.value / deleteThresholdPx).coerceIn(0f, 1f)

    // Color of revealed background
    val pinBgColor = Color(0xFFFFB800)   // amber / gold for pin
    val deleteBgColor = Color(0xFFE53935) // vivid red for delete/leave

    val revealedBgColor = when {
        offsetX.value > 0f -> pinBgColor.copy(alpha = pinFraction)
        offsetX.value < 0f -> deleteBgColor.copy(alpha = deleteFraction)
        else -> Color.Transparent
    }

    // Corner rounding: only dynamic during active swipe, 0.dp when idle (clean flat list)
    val revealCornerDp = if (offsetX.value != 0f) (16f * if (offsetX.value > 0f) pinFraction else deleteFraction).dp else 0.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (revealCornerDp > 0.dp) Modifier.clip(RoundedCornerShape(revealCornerDp))
                else Modifier
            )
    ) {
        // ── Background action panels ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(revealedBgColor)
        ) {
            // Pin hint (left side)
            if (offsetX.value > 0f) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = AnonChatIcons.Pin,
                        contentDescription = "Pin",
                        tint = Color.White.copy(alpha = pinFraction),
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                scaleX = 0.6f + 0.4f * pinFraction
                                scaleY = 0.6f + 0.4f * pinFraction
                            }
                    )
                    if (pinFraction > 0.5f) {
                        Text(
                            text = if (isPinned) "Unpin" else "Pin",
                            color = Color.White.copy(alpha = (pinFraction - 0.5f) * 2f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            // Delete / Leave hint (right side)
            if (offsetX.value < 0f) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (deleteFraction > 0.5f) {
                        Text(
                            text = if (isDeleteAction) "Delete" else "Leave",
                            color = Color.White.copy(alpha = (deleteFraction - 0.5f) * 2f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = if (isDeleteAction) AnonChatIcons.Trash else AnonChatIcons.LogOut,
                        contentDescription = if (isDeleteAction) "Delete" else "Leave",
                        tint = Color.White.copy(alpha = deleteFraction),
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                scaleX = 0.6f + 0.4f * deleteFraction
                                scaleY = 0.6f + 0.4f * deleteFraction
                            }
                    )
                }
            }
        }

        // ── Foreground row (slides with drag) ─────────────────────────────────
        Column(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .then(
                    if (offsetX.value != 0f) Modifier.background(Color.Black)
                    else Modifier
                )
                .pointerInput(roomId, isDeleteAction) {
                    detectHorizontalDragGestures(
                        onDragStart = { actionFired = false },
                        onDragEnd = {
                            scope.launch {
                                when {
                                    // Threshold reached for pin (swipe left-to-right)
                                    offsetX.value >= pinThresholdPx && !actionFired -> {
                                        actionFired = true
                                        showPinDialog = true
                                        offsetX.animateTo(
                                            0f,
                                            animationSpec = tween(
                                                durationMillis = 260,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                    }
                                    // Threshold reached for delete/leave (swipe right-to-left)
                                    offsetX.value <= -deleteThresholdPx && !actionFired -> {
                                        actionFired = true
                                        showLeaveDeleteDialog = true
                                        offsetX.animateTo(
                                            0f,
                                            animationSpec = tween(
                                                durationMillis = 260,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                    }
                                    // Below threshold – ease back
                                    else -> {
                                        offsetX.animateTo(
                                            0f,
                                            animationSpec = tween(
                                                durationMillis = 260,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                offsetX.animateTo(
                                    0f,
                                    animationSpec = tween(
                                        durationMillis = 260,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                val newVal = (offsetX.value + dragAmount).coerceIn(-maxSwipePx, maxSwipePx)
                                offsetX.snapTo(newVal)
                            }
                        }
                    )
                }
        ) {
            content(hasLeft)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(AnonChatColors.BorderSubtle)
            )
        }

        if (showPinDialog) {
            GroupPinConfirmDialog(
                roomName = roomDisplayName,
                isPinned = isPinned,
                accentColor = accentColor,
                onAccentColor = onAccentColor,
                onAccept = {
                    onPin()
                    showPinDialog = false
                },
                onDecline = {
                    showPinDialog = false
                }
            )
        }

        if (showLeaveDeleteDialog) {
            GroupLeaveDeleteDialog(
                roomName = roomDisplayName,
                isAdmin = isAdmin,
                accentColor = accentColor,
                onAccentColor = onAccentColor,
                onLeave = {
                    hasLeft = true
                    onLeave()
                    showLeaveDeleteDialog = false
                },
                onLeaveAndDelete = {
                    hasLeft = true
                    onLeave()
                    onDelete()
                    showLeaveDeleteDialog = false
                },
                onDecline = {
                    showLeaveDeleteDialog = false
                }
            )
        }
    }
}

@Composable
private fun PublicGroupRow(
    room: PublicRoomDto,
    isAdmin: Boolean = false,
    isPinned: Boolean = false,
    isLeft: Boolean = false,
    accentColor: Color,
    onAccentColor: Color,
    onOpenRoom: () -> Unit,
    onPin: () -> Unit = {},
    onLeave: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val categoryMatch = Regex("^\\[([^\\]]+)\\]\\s*(.*)").find(room.desc ?: "")
    val categoryTag = categoryMatch?.groupValues?.getOrNull(1)
    val cleanDesc = categoryMatch?.groupValues?.getOrNull(2)?.ifBlank { null } ?: room.desc

    val pinBgColor = Color(0xFFFFB800)

    val iconVector = when {
        room.icon == "terminal" || categoryTag.equals("Tech & Code", ignoreCase = true) || room.name.contains("tech", ignoreCase = true) || room.name.contains("code", ignoreCase = true) -> AnonChatIcons.Terminal
        room.icon == "book-open" || categoryTag.equals("Study & Notes", ignoreCase = true) || room.name.contains("study", ignoreCase = true) -> AnonChatIcons.GraduationCap
        room.icon == "moon" || categoryTag.equals("Hostel Life", ignoreCase = true) || room.name.contains("hostel", ignoreCase = true) -> AnonChatIcons.Bed
        room.icon == "home" -> AnonChatIcons.Building
        room.icon == "zap" || categoryTag.equals("Chill & Social", ignoreCase = true) -> AnonChatIcons.Sparkles
        room.icon == "tool" || categoryTag.equals("Gaming & Fun", ignoreCase = true) -> AnonChatIcons.Sparkles
        room.icon == "globe" || categoryTag.equals("General", ignoreCase = true) -> AnonChatIcons.Globe
        room.icon == "users" -> AnonChatIcons.Users
        else -> AnonChatIcons.Globe
    }

    SwipeableGroupCard(
        roomId = room.id,
        roomDisplayName = room.name,
        isAdmin = isAdmin,
        isPinned = isPinned,
        isLeft = isLeft,
        accentColor = accentColor,
        onAccentColor = onAccentColor,
        onPin = onPin,
        onLeave = onLeave,
        onDelete = onDelete
    ) { hasLeft ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple()
                ) { onOpenRoom() }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AnonChatColors.SurfaceSecondary)
                    .border(1.dp, AnonChatColors.Border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = if (isPinned) pinBgColor else accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = room.name,
                        fontSize = 14.sp,
                        color = AnonChatColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (!categoryTag.isNullOrBlank() && !hasLeft) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(accentColor.copy(alpha = 0.12f))
                                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = categoryTag.uppercase(),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(1.dp))
                val subtitle = when {
                    hasLeft -> "You left this group • Swipe left again to delete"
                    !cleanDesc.isNullOrBlank() -> cleanDesc
                    room.memberCount > 0 -> "${room.memberCount} members"
                    else -> "Public Channel"
                }
                Text(
                    text = subtitle,
                    style = AnonChatTypography.Caption,
                    color = if (hasLeft) Color(0xFFEF5350) else AnonChatColors.TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pinned indicator: show pin icon beside the button
                if (isPinned) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(pinBgColor.copy(alpha = 0.16f))
                            .border(1.dp, pinBgColor.copy(alpha = 0.40f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AnonChatIcons.Pin,
                            contentDescription = "Pinned",
                            tint = pinBgColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                // If user has left: replace "Open" with "Left" (white text on red background)
                if (hasLeft) {
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935))
                            .clickable { onDelete() }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Left",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Button(
                        onClick = onOpenRoom,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = onAccentColor
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = "Open",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = onAccentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JoinedGroupRow(
    room: RoomDto,
    isAdmin: Boolean = false,
    isPinned: Boolean = false,
    isLeft: Boolean = false,
    accentColor: Color,
    onAccentColor: Color,
    onOpenRoom: () -> Unit,
    onPin: () -> Unit = {},
    onLeave: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val rawDesc = room.effectiveDesc ?: room.desc ?: ""
    val categoryMatch = Regex("^\\[([^\\]]+)\\]\\s*(.*)").find(rawDesc)
    val categoryTag = categoryMatch?.groupValues?.getOrNull(1)?.takeIf { !it.equals("Private", ignoreCase = true) }
    val cleanDesc = categoryMatch?.groupValues?.getOrNull(2)?.ifBlank { null } ?: rawDesc.ifBlank { null }

    val isGroupPrivate = room.isPrivateGroup || !room.isPublic
    val pinBgColor = Color(0xFFFFB800)
    val groupIcon = when {
        isGroupPrivate -> AnonChatIcons.Shield
        categoryTag.equals("Tech & Code", ignoreCase = true) || room.name.contains("tech", ignoreCase = true) -> AnonChatIcons.Terminal
        categoryTag.equals("Study & Notes", ignoreCase = true) || room.name.contains("study", ignoreCase = true) -> AnonChatIcons.GraduationCap
        categoryTag.equals("Hostel Life", ignoreCase = true) || room.name.contains("hostel", ignoreCase = true) -> AnonChatIcons.Bed
        categoryTag.equals("Chill & Social", ignoreCase = true) -> AnonChatIcons.Sparkles
        categoryTag.equals("Gaming & Fun", ignoreCase = true) -> AnonChatIcons.Sparkles
        categoryTag.equals("General", ignoreCase = true) -> AnonChatIcons.Globe
        else -> AnonChatIcons.Users
    }

    SwipeableGroupCard(
        roomId = room.roomId,
        roomDisplayName = room.cleanDisplayName,
        isAdmin = isAdmin,
        isPinned = isPinned,
        isLeft = isLeft,
        accentColor = accentColor,
        onAccentColor = onAccentColor,
        onPin = onPin,
        onLeave = onLeave,
        onDelete = onDelete
    ) { hasLeft ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple()
                ) { onOpenRoom() }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AnonChatColors.SurfaceSecondary)
                    .border(1.dp, AnonChatColors.Border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = groupIcon,
                    contentDescription = null,
                    tint = if (isPinned) pinBgColor else accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = room.cleanDisplayName,
                        fontSize = 14.sp,
                        color = AnonChatColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Private Group Badge (if private)
                    if (isGroupPrivate && !hasLeft) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, AnonChatColors.Border, RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = AnonChatIcons.Shield,
                                    contentDescription = null,
                                    tint = AnonChatColors.TextMuted,
                                    modifier = Modifier.size(9.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = "PRIVATE",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AnonChatColors.TextMuted,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    // Category Badge (e.g. TECH & CODE, STUDY & NOTES, GENERAL)
                    if (!categoryTag.isNullOrBlank() && !hasLeft) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(accentColor.copy(alpha = 0.12f))
                                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = categoryTag.uppercase(),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(1.dp))
                val subtitle = when {
                    hasLeft -> "You left this group • Swipe left again to delete"
                    !cleanDesc.isNullOrBlank() -> cleanDesc
                    room.members.isNotEmpty() -> "${room.members.size} members • ${room.messageCount} messages"
                    else -> "Channel • #${room.roomId.take(8)}"
                }
                Text(
                    text = subtitle,
                    style = AnonChatTypography.Caption,
                    color = if (hasLeft) Color(0xFFEF5350) else AnonChatColors.TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pinned indicator: show only pin icon beside the button
                if (isPinned) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(pinBgColor.copy(alpha = 0.16f))
                            .border(1.dp, pinBgColor.copy(alpha = 0.40f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AnonChatIcons.Pin,
                            contentDescription = "Pinned",
                            tint = pinBgColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                // If user has left: replace "Open" with "Left" (white text on red background)
                if (hasLeft) {
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935))
                            .clickable { onDelete() }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Left",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Button(
                        onClick = onOpenRoom,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = onAccentColor
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = "Open",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = onAccentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupPinConfirmDialog(
    roomName: String,
    isPinned: Boolean,
    accentColor: Color,
    onAccentColor: Color,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Dialog(
        onDismissRequest = onDecline,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDecline() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF141416))
                    .border(1.dp, AnonChatColors.Border, RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* prevent backdrop tap dismissal */ }
                    .padding(22.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFB800).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFFFB800).copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AnonChatIcons.Pin,
                            contentDescription = null,
                            tint = Color(0xFFFFB800),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = if (isPinned) "Unpin Group" else "Pin Group",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = AnonChatColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = if (isPinned) {
                            "Do you want to unpin \"$roomName\" from the top of your groups list?"
                        } else {
                            "Do you want to pin \"$roomName\" to the top of your groups list?"
                        },
                        fontSize = 13.sp,
                        color = AnonChatColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(Modifier.height(22.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Decline Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AnonChatColors.SurfaceSecondary)
                                .border(1.dp, AnonChatColors.Border, RoundedCornerShape(12.dp))
                                .clickable { onDecline() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Decline",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AnonChatColors.TextSecondary
                            )
                        }

                        // Accept Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFB800))
                                .clickable { onAccept() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isPinned) "Unpin" else "Pin",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupLeaveDeleteDialog(
    roomName: String,
    isAdmin: Boolean = false,
    accentColor: Color,
    onAccentColor: Color,
    onLeave: () -> Unit,
    onLeaveAndDelete: () -> Unit,
    onDecline: () -> Unit
) {
    Dialog(
        onDismissRequest = onDecline,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDecline() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF141416))
                    .border(1.dp, AnonChatColors.Border, RoundedCornerShape(22.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* prevent backdrop tap dismissal */ }
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                // Top-right subtle circular close (X) button with generous touch target
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F1F23))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = AnonChatColors.TextMuted)
                        ) { onDecline() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AnonChatIcons.Close,
                        contentDescription = "Close dialog",
                        tint = AnonChatColors.TextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Center Top Trash / LogOut Icon in subtle circle
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935).copy(alpha = 0.14f))
                            .border(1.dp, Color(0xFFE53935).copy(alpha = 0.32f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAdmin) AnonChatIcons.Trash else AnonChatIcons.LogOut,
                            contentDescription = null,
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = if (isAdmin) "Disband or Leave Group" else "Leave Group",
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = AnonChatColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = if (isAdmin) {
                            "Are you sure you want to leave or permanently disband \"$roomName\"? Deleting permanently removes the group for all members."
                        } else {
                            "Are you sure you want to leave \"$roomName\"? You can rejoin anytime if it is a public group."
                        },
                        fontSize = 13.sp,
                        color = AnonChatColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(Modifier.height(22.dp))

                    if (isAdmin) {
                        // Two equal-width, equal-height modern capsule/pill buttons in ONE horizontal row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Delete Group: Destructive red treatment
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE53935))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.3f))
                                    ) { onLeaveAndDelete() }
                                    .padding(horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = AnonChatIcons.Trash,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Delete Group",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // 2. Leave Group: Subtle dark/outlined treatment with red accent/text
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF261919))
                                    .border(1.dp, Color(0xFFE53935).copy(alpha = 0.45f), CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true, color = Color(0xFFFF7070).copy(alpha = 0.2f))
                                    ) { onLeave() }
                                    .padding(horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = AnonChatIcons.LogOut,
                                        contentDescription = null,
                                        tint = Color(0xFFFF7070),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Leave Group",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFFF7070),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    } else {
                        // Non-admin: Single centered capsule button for Leave Group
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE53935))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.3f))
                                ) { onLeave() }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = AnonChatIcons.LogOut,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Leave Group",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
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
 * High-contrast capsuled segmented button with a gentle,
 * low-intensity spring sliding transition, dynamically styled with the user's active accent color.
 */
@Composable
private fun CapsuledSegmentControl(
    selectedSegment: Int,
    onSegmentSelected: (Int) -> Unit,
    labels: List<String>,
    accentColor: Color,
    onAccentColor: Color,
    height: androidx.compose.ui.unit.Dp = 42.dp,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(AnonChatColors.SurfaceSecondary)
            .border(1.dp, AnonChatColors.Border, CircleShape)
            .padding(3.dp)
    ) {
        val tabWidth = maxWidth / labels.size

        // Sliding capsule pill with strictly ease-in ease-out transition
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedSegment,
            animationSpec = tween(
                durationMillis = 280,
                easing = FastOutSlowInEasing
            ),
            label = "SegmentIndicatorOffset"
        )

        // The dynamic accent active pill
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(accentColor)
        )

        // Interactive labels row
        Row(modifier = Modifier.fillMaxSize()) {
            labels.forEachIndexed { index, title ->
                val isSelected = selectedSegment == index
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) onAccentColor else AnonChatColors.TextSecondary,
                    animationSpec = tween(durationMillis = 180),
                    label = "SegmentTextColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSegmentSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = if (height < 40.dp) 11.5.sp else 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
