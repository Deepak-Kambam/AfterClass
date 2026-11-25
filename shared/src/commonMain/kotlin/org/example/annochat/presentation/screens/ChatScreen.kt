package org.example.annochat.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.launch
import org.example.annochat.presentation.navigation.BackHandler
import org.example.annochat.data.model.MessageDto
import org.example.annochat.data.model.OnlineUserDto
import org.example.annochat.network.ConnectionStatus
import org.example.annochat.presentation.components.StickerGifPicker
import org.example.annochat.presentation.components.GroupInfoBottomSheet
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import kotlinproject.shared.generated.resources.Res
import kotlinproject.shared.generated.resources.bg_empty_cat
import org.jetbrains.compose.resources.painterResource
import org.example.annochat.presentation.components.AnonChatTopBar
import org.example.annochat.presentation.components.AtmosphericBackground
import org.example.annochat.presentation.components.ConnectionStatusBadge
import org.example.annochat.presentation.components.ConnectionWarningBanner
import org.example.annochat.presentation.components.EmptyState
import org.example.annochat.presentation.components.ChatStreamEmptyScreen
import org.example.annochat.presentation.components.MessageBubble
import org.example.annochat.presentation.components.MessageComposer
import org.example.annochat.presentation.components.PendingComposerMedia
import org.example.annochat.presentation.components.extractMediaUrl
import org.example.annochat.presentation.components.getGifVisual
import org.example.annochat.presentation.components.getStickerEmoji
import org.example.annochat.presentation.components.PickedImageItem
import androidx.compose.runtime.mutableStateListOf
import org.example.annochat.presentation.components.rememberImagePickerLauncher
import org.example.annochat.presentation.components.EditorMediaItem
import org.example.annochat.presentation.components.FullScreenImageViewerModal
import org.example.annochat.presentation.components.FullScreenImageData
import org.example.annochat.presentation.components.ImagePreviewEditorModal
import org.example.annochat.presentation.components.MessageSkeleton
import org.example.annochat.presentation.components.TypingIndicator
import org.example.annochat.presentation.components.AnimalAvatar
import androidx.compose.ui.text.style.TextOverflow
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.designsystem.AnonChatShapes
import org.example.annochat.presentation.designsystem.AnonChatSpacing
import org.example.annochat.presentation.designsystem.AnonChatTypography
import org.example.annochat.presentation.viewmodel.ChatViewModel
import org.example.annochat.presentation.components.AnonChatPrimaryButton
import org.example.annochat.presentation.components.AnonChatSecondaryButton
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback

private val WaterflowEasing = FastOutSlowInEasing

private val QuickSuggestions = listOf(
    "👋 Hey everyone!",
    "👀 What's the tea?",
    "☕ Anyone around?",
    "😂 Lol so true",
    "🍿 Tell me more",
    "💯 Absolutely",
    "✨ Good vibes only",
    "🔥 Real",
    "🤫 Keep it lowkey",
    "📚 Study break vibes"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var isMenuOpen by remember { mutableStateOf(false) }
    var isAddUserDialogOpen by remember { mutableStateOf(false) }
    var isLeaveConfirmOpen by remember { mutableStateOf(false) }
    var isStickerPickerOpen by remember { mutableStateOf(false) }
    var showGroupInfoSheet by remember { mutableStateOf(false) }
    var openGroupInfoInEditMode by remember { mutableStateOf(false) }
    var onlineUsersList by remember { mutableStateOf<List<OnlineUserDto>>(emptyList()) }
    var isLoadingOnlineUsers by remember { mutableStateOf(false) }
    var customUserSearchQuery by remember { mutableStateOf("") }

    val clipboardManager = LocalClipboardManager.current
    val selectedMessageIds = uiState.selectedMessageIds

    BackHandler(enabled = selectedMessageIds.isNotEmpty()) {
        viewModel.clearSelection()
    }

    val pendingEditorImages = remember { mutableStateListOf<EditorMediaItem>() }
    var viewingFullScreenImage by remember { mutableStateOf<FullScreenImageData?>(null) }
    var pendingComposerMedia by remember { mutableStateOf<PendingComposerMedia?>(null) }

    val launchImagePicker = rememberImagePickerLauncher { pickedList ->
        if (pickedList.size == 1 && (pickedList[0].mimeType.contains("gif", ignoreCase = true) || pickedList[0].fileName.endsWith(".gif", ignoreCase = true))) {
            val picked = pickedList[0]
            pendingComposerMedia = PendingComposerMedia(
                bytes = picked.bytes,
                name = picked.fileName,
                isGif = true,
                isSticker = false,
                pathOrUrl = picked.uriString
            )
        } else {
            pickedList.forEachIndexed { index, picked ->
                val id = "img_${pendingEditorImages.size + index + 1}_${picked.bytes.size}"
                pendingEditorImages.add(
                    EditorMediaItem(
                        id = id,
                        bytes = picked.bytes,
                        fileName = picked.fileName,
                        mimeType = picked.mimeType,
                        caption = ""
                    )
                )
            }
        }
    }

    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val imeInsets = WindowInsets.ime
    val isKeyboardActive by remember {
        derivedStateOf { imeInsets.getBottom(density) > 0 }
    }

    val isUserAtBottom by remember { derivedStateOf { !listState.canScrollForward } }
    var hasUnreadMessages by remember { mutableStateOf(false) }

    val shouldLoadOlder by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex <= 1 && uiState.hasMoreOlderMessages && !uiState.isLoadingOlder && uiState.messages.isNotEmpty()
        }
    }
    LaunchedEffect(shouldLoadOlder) {
        if (shouldLoadOlder) {
            viewModel.loadOlderMessages()
        }
    }

    // WhatsApp-style smooth scroll down to latest message
    fun scrollToBottomSmoothly() {
        coroutineScope.launch {
            if (uiState.messages.isNotEmpty()) {
                val lastIdx = uiState.messages.size - 1
                val firstVisible = listState.firstVisibleItemIndex
                if (lastIdx - firstVisible > 6) {
                    listState.scrollToItem(lastIdx - 5)
                }
                listState.animateScrollToItem(
                    index = lastIdx,
                    scrollOffset = 0
                )
                hasUnreadMessages = false
            }
        }
    }

    // Auto-scroll when keyboard opens so latest messages are never covered
    LaunchedEffect(isKeyboardActive) {
        if (isKeyboardActive && uiState.messages.isNotEmpty()) {
            val lastIdx = uiState.messages.size - 1
            val firstVisible = listState.firstVisibleItemIndex
            if (lastIdx - firstVisible > 6) {
                listState.scrollToItem(lastIdx - 5)
            }
            listState.animateScrollToItem(lastIdx)
        }
    }

    // Auto-scroll to latest message on incoming events
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            val lastIdx = uiState.messages.size - 1
            val isOwnMessage = uiState.messages.lastOrNull()?.publicId == viewModel.uiState.value.myIdentity?.publicId
            if (isUserAtBottom || isOwnMessage) {
                val firstVisible = listState.firstVisibleItemIndex
                if (lastIdx - firstVisible > 6) {
                    listState.scrollToItem(lastIdx - 5)
                }
                listState.animateScrollToItem(lastIdx)
                hasUnreadMessages = false
            } else {
                hasUnreadMessages = true
            }
        }
    }

    // Reset unread state when manually reaching the bottom
    LaunchedEffect(isUserAtBottom) {
        if (isUserAtBottom) {
            hasUnreadMessages = false
        }
    }

    // Auto-scroll on initial load
    LaunchedEffect(uiState.messages.isNotEmpty()) {
        if (uiState.messages.isNotEmpty()) {
            listState.scrollToItem(uiState.messages.size - 1)
        }
    }

    // Show error snackbars
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    val isPrivateRoom = uiState.roomName.contains("&") || uiState.roomId.startsWith("dm_")
    val userRooms by viewModel.repository.userRooms.collectAsState()
    val publicRoomsList by viewModel.repository.publicRooms.collectAsState()
    val currentRoomFromRepo = userRooms.find { it.roomId == uiState.roomId }
    val isPublicRoom = !isPrivateRoom && (
        (currentRoomFromRepo != null && currentRoomFromRepo.isPublic && !currentRoomFromRepo.isPrivateGroup) ||
        publicRoomsList.any {
            it.id.equals(uiState.roomId, ignoreCase = true) ||
            it.name.equals(uiState.roomName, ignoreCase = true) ||
            it.name.equals(uiState.roomName.removePrefix("#"), ignoreCase = true)
        }
    )
    val isMyGroup = userRooms.any { it.roomId == uiState.roomId } ||
        uiState.roomId.startsWith("grp_") ||
        (!isPublicRoom && !isPrivateRoom)
    val myPublicId = uiState.myIdentity?.publicId ?: viewModel.getMyPublicId() ?: ""
    val isLocallyOwned = remember(uiState.roomId) { viewModel.repository.sessionStorage.isMyOwnedRoom(uiState.roomId) }
    val isGroupAdmin = currentRoomFromRepo?.isUserAdmin(myPublicId, isLocallyOwned) == true || isLocallyOwned
    val showMoreMenu = !isPublicRoom
    val effectiveRoomName = currentRoomFromRepo?.name?.ifBlank { null } ?: uiState.roomName
    val displayTitle = if (isPrivateRoom) effectiveRoomName else "#${effectiveRoomName.removePrefix("#")}"
    val hasUserMessages = uiState.messages.any { !isChatSystemMessage(it) }

    AtmosphericBackground(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!hasUserMessages && !uiState.isLoading) {
                Image(
                    painter = painterResource(Res.drawable.bg_empty_cat),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black.copy(alpha = 0.30f),
                                    0.40f to Color.Black.copy(alpha = 0.50f),
                                    0.75f to Color.Black.copy(alpha = 0.75f),
                                    1.0f to Color.Black.copy(alpha = 0.95f)
                                )
                            )
                        )
                )
            }

            val isChatSnackbarActive = snackbarHostState.currentSnackbarData != null
            val isAnyDialogOpen = isLeaveConfirmOpen || isAddUserDialogOpen || showGroupInfoSheet
            val shouldUnfocusChat = isAnyDialogOpen || isChatSnackbarActive
            val chatUnfocusProgress by animateFloatAsState(
                targetValue = if (shouldUnfocusChat) 1f else 0f,
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                label = "ChatUnfocusProgress"
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .graphicsLayer {
                        if (chatUnfocusProgress > 0.001f) {
                            alpha = 1f - (chatUnfocusProgress * 0.65f)
                            scaleX = 1f - (chatUnfocusProgress * 0.02f)
                            scaleY = 1f - (chatUnfocusProgress * 0.02f)
                        }
                    }
            ) {
                // Glass Top App Bar with status bar insets
                AnonChatTopBar(
                    title = displayTitle,
                    subtitle = null,
                    navigationIcon = AnonChatIcons.ArrowBack,
                    onNavigationClick = onBackClick,
                    onTitleClick = if (isMyGroup) { { openGroupInfoInEditMode = false; showGroupInfoSheet = true } } else null,
                    actions = {
                        uiState.connectionState?.let { conn ->
                            if (conn.status != ConnectionStatus.CONNECTED) {
                                ConnectionStatusBadge(
                                    connectionState = conn,
                                    onlineCount = uiState.onlineCount
                                )
                            }
                        }

                        // Dedicated Group Info / Options icon for groups in "My Groups"
                        if (isMyGroup) {
                            Box(
                                modifier = Modifier
                                    .size(AnonChatSpacing.touchTargetMin)
                                    .clip(CircleShape)
                                    .clickable {
                                        openGroupInfoInEditMode = false
                                        showGroupInfoSheet = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = AnonChatIcons.Info,
                                    contentDescription = "Group info & options",
                                    tint = AnonChatColors.TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // 3-dots menu button for 1-on-1 personal & grouped chat options (hidden in public rooms)
                        if (showMoreMenu) {
                            Box(
                                modifier = Modifier
                                    .size(AnonChatSpacing.touchTargetMin)
                                    .clip(CircleShape)
                                    .clickable { isMenuOpen = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = AnonChatIcons.MoreVert,
                                    contentDescription = "Chat options",
                                    tint = AnonChatColors.TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = isMenuOpen,
                                onDismissRequest = { isMenuOpen = false },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AnonChatColors.SurfaceElevated)
                            ) {
                                if (isMyGroup) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = AnonChatIcons.Info,
                                                    contentDescription = null,
                                                    tint = AnonChatColors.TextPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(10.dp))
                                                Text(
                                                    text = "Group Info",
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = AnonChatColors.TextPrimary
                                                )
                                            }
                                        },
                                        onClick = {
                                            isMenuOpen = false
                                            openGroupInfoInEditMode = false
                                            showGroupInfoSheet = true
                                        }
                                    )

                                    if (isGroupAdmin) {
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = AnonChatIcons.Edit,
                                                        contentDescription = null,
                                                        tint = AnonChatColors.TextPrimary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(Modifier.width(10.dp))
                                                    Text(
                                                        text = "Change Group Name",
                                                        fontSize = 13.5.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = AnonChatColors.TextPrimary
                                                    )
                                                }
                                            },
                                            onClick = {
                                                isMenuOpen = false
                                                openGroupInfoInEditMode = true
                                                showGroupInfoSheet = true
                                            }
                                        )
                                    }

                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = AnonChatIcons.Link,
                                                    contentDescription = null,
                                                    tint = AnonChatColors.TextPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(10.dp))
                                                Text(
                                                    text = "Invite via Link",
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = AnonChatColors.TextPrimary
                                                )
                                            }
                                        },
                                        onClick = {
                                            isMenuOpen = false
                                            val inviteUrl = "https://afterclass.app/join/${uiState.roomId}"
                                            clipboardManager.setText(AnnotatedString(inviteUrl))
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Invite link copied to clipboard")
                                            }
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = AnonChatIcons.UserPlus,
                                                    contentDescription = null,
                                                    tint = AnonChatColors.TextPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(10.dp))
                                                Text(
                                                    text = "Add Member",
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = AnonChatColors.TextPrimary
                                                )
                                            }
                                        },
                                        onClick = {
                                            isMenuOpen = false
                                            isLoadingOnlineUsers = true
                                            isAddUserDialogOpen = true
                                            viewModel.loadOnlineUsers { users ->
                                                onlineUsersList = users
                                                isLoadingOnlineUsers = false
                                            }
                                        }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = AnonChatIcons.UserPlus,
                                                    contentDescription = null,
                                                    tint = AnonChatColors.TextPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(10.dp))
                                                Text(
                                                    text = "Add User to Chat",
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = AnonChatColors.TextPrimary
                                                )
                                            }
                                        },
                                        onClick = {
                                            isMenuOpen = false
                                            isLoadingOnlineUsers = true
                                            isAddUserDialogOpen = true
                                            viewModel.loadOnlineUsers { users ->
                                                onlineUsersList = users
                                                isLoadingOnlineUsers = false
                                            }
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = AnonChatIcons.LogOut,
                                                    contentDescription = null,
                                                    tint = AnonChatColors.Error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(10.dp))
                                                Text(
                                                    text = if (isPrivateRoom) "Leave Chat" else "Leave Room",
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = AnonChatColors.Error
                                                )
                                            }
                                        },
                                        onClick = {
                                            isMenuOpen = false
                                            isLeaveConfirmOpen = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                )

                // Connection Warning Banner if offline or reconnecting
                uiState.connectionState?.let { conn ->
                    ConnectionWarningBanner(connectionState = conn)
                }

                // Message Stream Area (Extends full height down)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when {
                        // Loading State: Skeletons
                        uiState.isLoading && uiState.messages.isEmpty() -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(AnonChatSpacing.lg),
                                verticalArrangement = Arrangement.spacedBy(AnonChatSpacing.md)
                            ) {
                                MessageSkeleton(isOwn = false)
                                MessageSkeleton(isOwn = true)
                                MessageSkeleton(isOwn = false)
                                MessageSkeleton(isOwn = false)
                            }
                        }

                        // Empty State: Screen 7 Chat Stream Empty (shown when no user chat messages exist)
                        !hasUserMessages -> {
                            val systemNotices = uiState.messages.filter { isChatSystemMessage(it) }
                            if (systemNotices.isEmpty()) {
                                ChatStreamEmptyScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    renderBackground = false
                                )
                            } else {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    systemNotices.forEach { sysMsg ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = AnonChatSpacing.md, vertical = AnonChatSpacing.xs),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(AnonChatColors.SurfaceSecondary)
                                                    .border(0.5.dp, AnonChatColors.BorderSubtle, RoundedCornerShape(12.dp))
                                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = sysMsg.content ?: "",
                                                    style = AnonChatTypography.Caption,
                                                    color = AnonChatColors.TextSecondary,
                                                    fontSize = 11.5.sp,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }
                                    }
                                    ChatStreamEmptyScreen(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        renderBackground = false
                                    )
                                }
                            }
                        }

                        // Active Messages List with intelligent sender grouping
                        else -> {
                            val myIdentity = uiState.myIdentity
                            val messageItems = remember(uiState.messages, myIdentity) {
                                uiState.messages.map { message ->
                                    val myPid = myIdentity?.publicId ?: viewModel.getMyPublicId()
                                    val myName = myIdentity?.anonName ?: viewModel.getMyAnonName()
                                    val isOwn = message.postNum < 0 ||
                                                message.anonName == "You" ||
                                                viewModel.isOwnClientMsgId(message.clientMsgId) ||
                                                (!myPid.isNullOrBlank() && message.publicId == myPid) ||
                                                (!myName.isNullOrBlank() && message.anonName == myName)
                                    Pair(message, isOwn)
                                }
                            }

                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = AnonChatSpacing.md,
                                    end = AnonChatSpacing.md,
                                    top = AnonChatSpacing.md,
                                    bottom = 8.dp
                                )
                            ) {
                                if (uiState.isLoadingOlder) {
                                    item(key = "loading_older_indicator", contentType = "loading_indicator") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = AnonChatSpacing.sm),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = AnonChatColors.Primary
                                            )
                                        }
                                    }
                                }

                                itemsIndexed(
                                    messageItems,
                                    key = { _, item ->
                                        val msg = item.first
                                        val cid = msg.clientMsgId?.takeIf { it.isNotBlank() }
                                        val pNum = msg.postNum
                                        val time = msg.createdAt ?: ""
                                        val hash = (msg.content ?: msg.imagePath ?: "").hashCode()
                                        if (cid != null) "c_${cid}" else "p_${pNum}_${time}_${hash}"
                                    },
                                    contentType = { _, (message, _) ->
                                        when {
                                            isChatSystemMessage(message) -> "system"
                                            !message.imagePath.isNullOrBlank() ||
                                                    extractMediaUrl(message.content) != null ||
                                                    message.content?.startsWith("[Sticker]") == true ||
                                                    message.content?.startsWith("[GIF]") == true -> "media"
                                            else -> "text"
                                        }
                                    }
                                ) { index, (message, isOwn) ->
                                    val isSystem = isChatSystemMessage(message)

                                    if (isSystem) {
                                        // Render clean centered System Notice Capsule
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = AnonChatSpacing.xs),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(AnonChatColors.SurfaceSecondary)
                                                    .border(0.5.dp, AnonChatColors.BorderSubtle, RoundedCornerShape(12.dp))
                                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = message.content ?: "",
                                                    style = AnonChatTypography.Caption,
                                                    color = AnonChatColors.TextSecondary,
                                                    fontSize = 11.5.sp,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }
                                    } else {
                                        val isSameSenderAsPrevious = if (index > 0) {
                                            val (prevMsg, prevIsOwn) = messageItems[index - 1]
                                            if (isOwn && prevIsOwn) {
                                                true
                                            } else if (!isOwn && !prevIsOwn) {
                                                prevMsg.anonName == message.anonName && (prevMsg.publicId == message.publicId)
                                            } else {
                                                false
                                            }
                                        } else {
                                            false
                                        }

                                        val topSpacing = if (index == 0) {
                                            0.dp
                                        } else if (isSameSenderAsPrevious) {
                                            AnonChatSpacing.xs
                                        } else {
                                            AnonChatSpacing.md
                                        }

                                        Spacer(Modifier.height(topSpacing))

                                        Column(modifier = Modifier.fillMaxWidth().animateItem(
                                            fadeInSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                                            placementSpec = null,
                                            fadeOutSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
                                        )) {
                                            val isSingleSelected = selectedMessageIds.size == 1 && selectedMessageIds.contains(message.postNum)
                                            androidx.compose.animation.AnimatedVisibility(
                                                visible = isSingleSelected,
                                                enter = androidx.compose.animation.fadeIn(tween(180, easing = FastOutSlowInEasing)) + androidx.compose.animation.scaleIn(tween(180, easing = FastOutSlowInEasing)),
                                                exit = androidx.compose.animation.fadeOut(tween(140, easing = FastOutSlowInEasing)) + androidx.compose.animation.scaleOut(tween(140, easing = FastOutSlowInEasing))
                                            ) {
                                                Box(
                                                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                                    contentAlignment = if (isOwn) Alignment.CenterEnd else Alignment.CenterStart
                                                ) {
                                                    ReactionPickerPill(
                                                        onReactionClick = { emoji ->
                                                            viewModel.toggleReaction(message.postNum, emoji)
                                                            viewModel.clearSelection()
                                                        },
                                                        modifier = Modifier.padding(
                                                            start = if (!isOwn) 44.dp else 0.dp
                                                        )
                                                    )
                                                }
                                            }

                                            MessageBubble(
                                                message = message,
                                                isOwnMessage = isOwn,
                                                repository = viewModel.repository,
                                                ownAvatarUrl = uiState.myIdentity?.avatarUrl ?: viewModel.getMyAvatarUrl(),
                                                ownAnimalName = uiState.myIdentity?.animalName ?: uiState.myIdentity?.anonName ?: viewModel.getMyAnonName(),
                                                ownPublicId = uiState.myIdentity?.publicId ?: viewModel.getMyPublicId(),
                                                showSenderHeader = !isSameSenderAsPrevious,
                                                showTimestamp = true,
                                                isSelected = selectedMessageIds.contains(message.postNum),
                                                isSelectionMode = selectedMessageIds.isNotEmpty(),
                                                animateEntrance = uiState.newlyArrivedPostNums.contains(message.postNum),
                                                floatingReactions = uiState.activeFloatingReactions.filter { it.postNum == message.postNum }.map { it.emoji },
                                                onReactionClick = { emoji ->
                                                    viewModel.toggleReaction(message.postNum, emoji)
                                                },
                                                onReplyClick = {
                                                    viewModel.setReplyTo(message)
                                                },
                                                onLongPress = {
                                                    viewModel.toggleMessageSelection(message.postNum)
                                                },
                                                onClick = {
                                                    viewModel.toggleMessageSelection(message.postNum)
                                                },
                                                onImageClick = { path, name, caption ->
                                                    viewingFullScreenImage = FullScreenImageData(
                                                        pathOrUrl = path,
                                                        imageName = name,
                                                        caption = caption
                                                    )
                                                },
                                                onAvatarClick = { path, name ->
                                                    viewingFullScreenImage = FullScreenImageData(
                                                        pathOrUrl = path,
                                                        imageName = if (!name.isNullOrBlank()) "$name's Avatar" else "Avatar",
                                                        caption = name
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Scrim overlay to unfocus the screen when chat snackbar is active
                    val isChatSnackbarActive = snackbarHostState.currentSnackbarData != null
                    val chatSnackbarScrimAlpha by animateFloatAsState(
                        targetValue = if (isChatSnackbarActive) 0.65f else 0f,
                        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                        label = "ChatSnackbarScrim"
                    )

                    if (chatSnackbarScrimAlpha > 0.005f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = chatSnackbarScrimAlpha))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                }
                        )
                    }

                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 56.dp)
                            .padding(horizontal = AnonChatSpacing.md),
                        snackbar = { snackbarData ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = 28.dp,
                                        shape = RoundedCornerShape(20.dp),
                                        ambientColor = Color.Black,
                                        spotColor = Color.Black
                                    )
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF161618))
                                    .clickable {
                                        snackbarData.dismiss()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(AnonChatColors.Primary)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = snackbarData.visuals.message,
                                        color = AnonChatColors.TextPrimary,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 19.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (snackbarData.visuals.actionLabel != null) {
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = snackbarData.visuals.actionLabel ?: "",
                                            color = AnonChatColors.Primary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable {
                                                snackbarData.performAction()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )

                    // Scroll to Bottom FAB: Visible always when scrolled upwards, floats above input row
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isUserAtBottom,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut(),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = AnonChatSpacing.md, bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = CircleShape,
                                    ambientColor = Color.Black.copy(alpha = 0.9f),
                                    spotColor = Color.Black.copy(alpha = 0.9f)
                                )
                                .clip(CircleShape)
                                .background(Color(0xFF1E1E22))
                                .border(1.dp, Color(0xFF38383D), CircleShape)
                                .clickable {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    scrollToBottomSmoothly()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AnonChatIcons.ChevronDown,
                                contentDescription = "Scroll to bottom",
                                tint = AnonChatColors.Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            // Notification dot only visible when new message was sent while scrolled up
                            if (hasUnreadMessages) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(AnonChatColors.Primary)
                                    )
                            }
                        }
                    }
                }

                // Docked Controls: Suggestions, Selection Toolbar, Reply Bar, Message Composer, Stickers
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.50f),
                                    Color.Black.copy(alpha = 0.88f),
                                    Color(0xFF0A0A0A)
                                )
                            )
                        )
                        .padding(top = 4.dp)
                        .navigationBarsPadding()
                ) {
                    if (selectedMessageIds.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AnonChatColors.SurfacePrimary)
                        ) {
                            SelectionToolbar(
                                selectedCount = selectedMessageIds.size,
                                onClose = { viewModel.clearSelection() },
                                onCopy = {
                                    val selectedMsgs = uiState.messages.filter { selectedMessageIds.contains(it.postNum) }
                                    val copiedText = selectedMsgs.joinToString("\n") { it.content ?: "" }
                                    clipboardManager.setText(AnnotatedString(copiedText))
                                    viewModel.clearSelection()
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Copied to clipboard") }
                                }
                            )
                        }
                    } else {
                        // Typing Indicator
                        AnimatedVisibility(
                            visible = uiState.typingUsers.isNotEmpty(),
                            enter = fadeIn() + slideInVertically { it / 2 },
                            exit = fadeOut() + slideOutVertically { it / 2 },
                            modifier = Modifier.padding(horizontal = AnonChatSpacing.md, vertical = AnonChatSpacing.xs)
                        ) {
                            TypingIndicator(typingUsers = uiState.typingUsers)
                        }

                        // WhatsApp-Style Reply Preview Bar with spring transition
                        AnimatedVisibility(
                            visible = uiState.replyingTo != null,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            uiState.replyingTo?.let { replyTarget ->
                                val replyAuthor = if (!replyTarget.anonName.isNullOrBlank()) {
                                    replyTarget.anonName
                                } else {
                                    "Anonymous #${replyTarget.postNum}"
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = AnonChatSpacing.md, vertical = AnonChatSpacing.xs)
                                        .shadow(
                                            elevation = 8.dp,
                                            shape = RoundedCornerShape(16.dp),
                                            ambientColor = Color.Black.copy(alpha = 0.9f),
                                            spotColor = Color.Black.copy(alpha = 0.9f)
                                        )
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF1A1A1D))
                                        .border(1.dp, Color(0xFF333338), RoundedCornerShape(16.dp))
                                        .padding(horizontal = AnonChatSpacing.md, vertical = AnonChatSpacing.xs),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.5.dp)
                                                .height(30.dp)
                                                .clip(CircleShape)
                                                .background(AnonChatColors.Primary)
                                        )

                                        Spacer(Modifier.width(AnonChatSpacing.sm))

                                        Column {
                                            Text(
                                                text = "Replying to $replyAuthor",
                                                style = AnonChatTypography.Label,
                                                color = AnonChatColors.Primary,
                                                fontSize = 12.sp,
                                                maxLines = 1
                                            )
                                            Spacer(Modifier.height(1.dp))
                                            Text(
                                                text = replyTarget.content ?: (if (!replyTarget.imagePath.isNullOrBlank()) "Attachment" else "Message"),
                                                style = AnonChatTypography.Caption,
                                                color = AnonChatColors.TextSecondary,
                                                fontSize = 12.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(AnonChatColors.SurfaceTertiary)
                                            .clickable { viewModel.setReplyTo(null) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = AnonChatIcons.Close,
                                            contentDescription = "Cancel reply",
                                            tint = AnonChatColors.TextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Quick Message Suggestions with Dissolve Effect (active only when keyboard is inactive, no reply, no selection)
                        AnimatedVisibility(
                            visible = !isKeyboardActive && uiState.replyingTo == null && selectedMessageIds.isEmpty(),
                            enter = fadeIn(animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)) +
                                    expandVertically(animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)),
                            exit = fadeOut(animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)) +
                                    shrinkVertically(animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing))
                        ) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items(QuickSuggestions) { suggestion ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFF19191D).copy(alpha = 0.92f))
                                            .border(1.dp, Color(0xFF2E2E35), RoundedCornerShape(20.dp))
                                            .clickable {
                                                viewModel.onInputTextChange(suggestion)
                                            }
                                            .padding(horizontal = 14.dp, vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = suggestion,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = AnonChatColors.TextPrimary.copy(alpha = 0.95f),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        // Seamless Floating Message Composer
                        MessageComposer(
                            text = uiState.inputText,
                            onTextChange = viewModel::onInputTextChange,
                            onSend = {
                                val staged = pendingComposerMedia
                                val currentText = uiState.inputText.trim()
                                if (staged != null) {
                                    pendingComposerMedia = null
                                    if (staged.bytes != null) {
                                        viewModel.uploadAndSendImage(
                                            bytes = staged.bytes,
                                            fileName = if (staged.isGif) "animation.gif" else "image.jpg",
                                            mimeType = if (staged.isGif) "image/gif" else "image/jpeg",
                                            caption = currentText
                                        )
                                        viewModel.onInputTextChange("")
                                    } else if (!staged.pathOrUrl.isNullOrBlank()) {
                                        viewModel.sendMediaMessage(
                                            imagePath = staged.pathOrUrl,
                                            imageName = staged.name ?: (if (staged.isGif) "GIF" else "Sticker"),
                                            content = if (currentText.isNotBlank()) currentText else null
                                        )
                                        viewModel.onInputTextChange("")
                                    }
                                } else {
                                    val detectedMediaUrl = extractMediaUrl(currentText)
                                    if (detectedMediaUrl != null) {
                                        val stripped = currentText.replace(detectedMediaUrl, "").trim()
                                        viewModel.sendMediaMessage(
                                            imagePath = detectedMediaUrl,
                                            imageName = if (detectedMediaUrl.contains(".gif", ignoreCase = true)) "GIF Animation" else "Shared Image",
                                            content = if (stripped.isNotBlank()) stripped else null
                                        )
                                        viewModel.onInputTextChange("")
                                    } else {
                                        viewModel.sendMessage()
                                    }
                                }
                            },
                            pendingMedia = pendingComposerMedia,
                            onRemovePendingMedia = { pendingComposerMedia = null },
                            onMediaReceivedFromKeyboard = { picked ->
                                val isGif = picked.mimeType.contains("gif", ignoreCase = true) || picked.fileName.endsWith(".gif", ignoreCase = true)
                                pendingComposerMedia = PendingComposerMedia(
                                    bytes = picked.bytes,
                                    name = if (isGif) "GIF from Keyboard" else picked.fileName,
                                    isGif = isGif,
                                    isSticker = false,
                                    pathOrUrl = picked.uriString
                                )
                            },
                            repository = viewModel.repository,
                            isSending = uiState.isSending,
                            uploadProgress = uiState.mediaUploadProgress,
                            uploadStatus = uiState.mediaUploadStatus,
                            onCancelUpload = viewModel::cancelMediaUpload,
                            enabled = uiState.connectionState?.status != ConnectionStatus.DISCONNECTED,
                            placeholder = "Message $displayTitle…",
                            onPickImage = launchImagePicker,
                            isStickersOpen = isStickerPickerOpen,
                            onOpenStickers = {
                                isStickerPickerOpen = !isStickerPickerOpen
                            },
                            modifier = Modifier.padding(bottom = 2.dp)
                        )

                        // Inline/Expandable Sticker & GIF Picker
                        AnimatedVisibility(
                            visible = isStickerPickerOpen,
                            enter = slideInVertically { it } + fadeIn(),
                            exit = slideOutVertically { it } + fadeOut()
                        ) {
                            StickerGifPicker(
                                isOpen = isStickerPickerOpen,
                                onDismiss = { isStickerPickerOpen = false },
                                onSelectMedia = { path, name, isGif ->
                                    pendingComposerMedia = PendingComposerMedia(
                                        pathOrUrl = path,
                                        name = name,
                                        isGif = isGif,
                                        isSticker = !isGif,
                                        visualEmoji = if (isGif) getGifVisual(path) else getStickerEmoji(path)
                                    )
                                    isStickerPickerOpen = false
                                },
                                onLoadRemoteMedia = { callback ->
                                    viewModel.loadStickersAndGifs(callback)
                                }
                            )
                        }
                    }
                }
            }

        // Leave Chat Confirmation Dialog
        if (isLeaveConfirmOpen) {
            Dialog(
                onDismissRequest = { isLeaveConfirmOpen = false },
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
                        ) { isLeaveConfirmOpen = false },
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
                            ) { /* swallow clicks inside dialog */ }
                            .padding(22.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(AnonChatColors.SurfaceTertiary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = AnonChatIcons.LogOut,
                                    contentDescription = null,
                                    tint = AnonChatColors.Error,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = if (isPrivateRoom) "Leave Chat?" else "Leave Room?",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = AnonChatColors.TextPrimary
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "You will exit this conversation and return to the chat lobby.",
                                fontSize = 13.sp,
                                color = AnonChatColors.TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(20.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AnonChatSecondaryButton(
                                    text = "Cancel",
                                    onClick = { isLeaveConfirmOpen = false },
                                    modifier = Modifier.weight(1f)
                                )
                                AnonChatPrimaryButton(
                                    text = "Leave",
                                    onClick = {
                                        isLeaveConfirmOpen = false
                                        viewModel.leaveRoom {
                                            onBackClick()
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Add User to Chat Dialog (Spacious modal with authentic Animal DPs)
        if (isAddUserDialogOpen) {
            LaunchedEffect(Unit) {
                if (onlineUsersList.isEmpty()) {
                    val cached = viewModel.repository.onlineUsers.value
                    if (cached.isNotEmpty()) {
                        onlineUsersList = cached
                    }
                }
                isLoadingOnlineUsers = onlineUsersList.isEmpty()
                viewModel.loadOnlineUsers { users ->
                    onlineUsersList = users
                    isLoadingOnlineUsers = false
                }
            }

            Dialog(
                onDismissRequest = { isAddUserDialogOpen = false },
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                    usePlatformDefaultWidth = false
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.82f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { isAddUserDialogOpen = false },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp)
                            .fillMaxHeight(0.80f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF141416))
                            .border(1.dp, AnonChatColors.Border.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { /* swallow clicks inside dialog */ }
                            .padding(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Header: Icon + Title & Subtitle + Close Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(AnonChatColors.Primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = AnonChatIcons.UserPlus,
                                            contentDescription = null,
                                            tint = AnonChatColors.Primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Add User to Chat",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AnonChatColors.TextPrimary
                                        )
                                        Text(
                                            text = "Invite online peers to this room",
                                            fontSize = 11.5.sp,
                                            color = AnonChatColors.TextMuted
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(AnonChatColors.SurfaceSecondary)
                                        .clickable { isAddUserDialogOpen = false },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = AnonChatIcons.Close,
                                        contentDescription = "Close",
                                        tint = AnonChatColors.TextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Search field
                            TextField(
                                value = customUserSearchQuery,
                                onValueChange = { customUserSearchQuery = it },
                                placeholder = {
                                    Text(
                                        text = "Search name or paste Public ID…",
                                        fontSize = 13.sp,
                                        color = AnonChatColors.TextMuted
                                    )
                                },
                                trailingIcon = {
                                    if (customUserSearchQuery.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .clickable { customUserSearchQuery = "" },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = AnonChatIcons.Close,
                                                contentDescription = "Clear",
                                                tint = AnonChatColors.TextMuted,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = AnonChatColors.SurfaceSecondary,
                                    unfocusedContainerColor = AnonChatColors.SurfaceSecondary,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = AnonChatColors.TextPrimary,
                                    unfocusedTextColor = AnonChatColors.TextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, AnonChatColors.Border, RoundedCornerShape(12.dp))
                            )

                            Spacer(Modifier.height(16.dp))

                            val myPid = uiState.myIdentity?.publicId
                            val filteredPeers = onlineUsersList.filter { user ->
                                user.publicId != myPid &&
                                (customUserSearchQuery.isBlank() ||
                                 user.anonName?.contains(customUserSearchQuery, ignoreCase = true) == true ||
                                 user.publicId.contains(customUserSearchQuery, ignoreCase = true))
                            }

                            // Section Header: ONLINE PEERS with Count Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ONLINE PEERS",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AnonChatColors.TextMuted,
                                    letterSpacing = 0.8.sp
                                )
                                if (filteredPeers.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(AnonChatColors.SurfaceSecondary)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${filteredPeers.size} available",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = AnonChatColors.TextSecondary
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // Scrollable list filling all vertical space with .weight(1f)
                            if (isLoadingOnlineUsers) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(
                                            color = AnonChatColors.Primary,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            text = "Finding online peers…",
                                            fontSize = 12.sp,
                                            color = AnonChatColors.TextMuted
                                        )
                                    }
                                }
                            } else if (filteredPeers.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = AnonChatIcons.Users,
                                            contentDescription = null,
                                            tint = AnonChatColors.TextMuted.copy(alpha = 0.5f),
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(Modifier.height(10.dp))
                                        Text(
                                            text = if (onlineUsersList.isEmpty()) "No other online users right now" else "No matching peers found",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = AnonChatColors.TextSecondary,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = if (onlineUsersList.isEmpty()) "When other students are active, they'll appear here." else "Try searching by animal name or public ID.",
                                            fontSize = 11.5.sp,
                                            color = AnonChatColors.TextMuted,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    items(filteredPeers, key = { it.publicId }, contentType = { "peer_item" }) { user ->
                                        val peerName = user.anonName ?: "Anonymous Peer"
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(AnonChatColors.SurfaceSecondary)
                                                .border(1.dp, AnonChatColors.Border.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                            .clickable {
                                                viewModel.addUserToRoom(user.publicId) { success ->
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            if (success) "Invitation sent to $peerName. Waiting for response…"
                                                            else "Failed to invite $peerName"
                                                        )
                                                    }
                                                }
                                                isAddUserDialogOpen = false
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Authentic Animal DP / Avatar with online presence badge
                                            Box(
                                                modifier = Modifier.size(46.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                AnimalAvatar(
                                                    repository = viewModel.repository,
                                                    avatarUrl = user.anonAvatar,
                                                    animalName = user.anonName,
                                                    publicId = user.publicId,
                                                    size = 46.dp
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(11.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF22C55E))
                                                        .border(2.dp, Color(0xFF141416), CircleShape)
                                                        .align(Alignment.BottomEnd)
                                                )
                                            }

                                            Spacer(Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = peerName,
                                                    fontSize = 14.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = AnonChatColors.TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = "#${user.publicId.take(8)}",
                                                    fontSize = 11.5.sp,
                                                    color = AnonChatColors.TextMuted
                                                )
                                            }

                                            Spacer(Modifier.width(10.dp))

                                            // Stylish "Add +" Pill Button
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(AnonChatColors.Primary.copy(alpha = 0.15f))
                                                    .border(1.dp, AnonChatColors.Primary.copy(alpha = 0.45f), CircleShape)
                                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = AnonChatIcons.UserPlus,
                                                        contentDescription = null,
                                                        tint = AnonChatColors.Primary,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(
                                                        text = "Add",
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = AnonChatColors.Primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Direct ID fallback if user typed a specific ID not listed above
                            if (customUserSearchQuery.isNotBlank() && onlineUsersList.none { it.publicId == customUserSearchQuery.trim() }) {
                                Spacer(Modifier.height(12.dp))
                                AnonChatPrimaryButton(
                                    text = "Add by ID (${customUserSearchQuery.trim().take(12)})",
                                    onClick = {
                                        val targetId = customUserSearchQuery.trim()
                                        viewModel.addUserToRoom(targetId) { success ->
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    if (success) "Invitation sent to $targetId. Waiting for response…"
                                                    else "Failed to invite $targetId"
                                                )
                                            }
                                        }
                                        isAddUserDialogOpen = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }

        // Full screen chat image lightbox viewer
        viewingFullScreenImage?.let { fullScreenData ->
            FullScreenImageViewerModal(
                imageData = fullScreenData,
                repository = viewModel.repository,
                onDismiss = { viewingFullScreenImage = null }
            )
        }

        // Group Info & Management Bottom Sheet (Rename, Members with Online status dots, Admin Delete)
        if (showGroupInfoSheet) {
            GroupInfoBottomSheet(
                roomId = uiState.roomId,
                initialRoomName = uiState.roomName,
                repository = viewModel.repository,
                viewModel = viewModel,
                accentColor = AnonChatColors.Primary,
                onAccentColor = AnonChatColors.OnPrimary,
                startInEditMode = openGroupInfoInEditMode,
                onDismiss = {
                    showGroupInfoSheet = false
                    openGroupInfoInEditMode = false
                },
                onAddUserClick = {
                    showGroupInfoSheet = false
                    isLoadingOnlineUsers = true
                    isAddUserDialogOpen = true
                    viewModel.loadOnlineUsers { users ->
                        onlineUsersList = users
                        isLoadingOnlineUsers = false
                    }
                },
                onLeaveRoomClick = {
                    showGroupInfoSheet = false
                    isLeaveConfirmOpen = true
                },
                onSaveSuccess = { newName ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Group name updated to \"$newName\"")
                    }
                },
                onSaveError = { errorMsg ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(errorMsg)
                    }
                },
                onGroupDeleted = {
                    onBackClick()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Group deleted successfully")
                    }
                }
            )
        }

        // WhatsApp-style media preview & functional editor (with waterflow smooth animation)
        AnimatedVisibility(
            visible = pendingEditorImages.isNotEmpty(),
            enter = fadeIn(animationSpec = tween(380, easing = WaterflowEasing)) +
                    scaleIn(initialScale = 0.95f, animationSpec = tween(380, easing = WaterflowEasing)),
            exit = fadeOut(animationSpec = tween(280, easing = WaterflowEasing)) +
                    scaleOut(targetScale = 0.95f, animationSpec = tween(280, easing = WaterflowEasing))
        ) {
            val senderName = uiState.myIdentity?.anonName?.ifBlank { null } ?: "Danger"
            ImagePreviewEditorModal(
                mediaItems = pendingEditorImages,
                onPickMoreImages = { launchImagePicker() },
                recipientLabel = "$senderName (You)",
                onDismiss = { pendingEditorImages.clear() },
                onSend = { bakedList ->
                    pendingEditorImages.clear()
                    viewModel.uploadAndSendMultipleImages(bakedList)
                }
            )
        }
    }
}
}

@Composable
private fun SelectionToolbar(
    selectedCount: Int,
    onClose: () -> Unit,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AnonChatColors.SurfacePrimary)
            .padding(horizontal = AnonChatSpacing.md, vertical = AnonChatSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AnonChatColors.SurfaceSecondary)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AnonChatIcons.Close,
                    contentDescription = "Close selection",
                    tint = AnonChatColors.TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(AnonChatSpacing.md))
            Text(
                text = "$selectedCount selected",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = AnonChatColors.TextPrimary
            )
        }

        AnonChatPrimaryButton(
            text = "Copy",
            onClick = onCopy,
            isSmall = true
        )
    }
}

@Composable
fun ReactionPickerPill(
    onReactionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val reactions = listOf("👍", "❤️", "🔥", "😂", "😮", "👏")
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(AnonChatColors.SurfaceSecondary)
            .border(1.dp, AnonChatColors.Border, RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        reactions.forEach { emoji ->
            Text(
                text = emoji,
                fontSize = 24.sp,
                modifier = Modifier.clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { onReactionClick(emoji) }
            )
        }
    }
}

private fun isChatSystemMessage(message: MessageDto): Boolean {
    val content = message.content?.trim() ?: ""
    return message.publicId == "system" ||
            message.anonName.equals("system", ignoreCase = true) ||
            content.startsWith("Private room created") ||
            content.startsWith("Created public group") ||
            content.startsWith("Created private group") ||
            content.startsWith("🏷️") ||
            content.startsWith("__RENAME__:")
}
