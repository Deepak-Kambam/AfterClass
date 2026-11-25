package org.example.annochat.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt
import org.example.annochat.data.model.RoomDto
import org.example.annochat.data.model.RoomMemberDto
import org.example.annochat.data.model.isRoomMatching
import org.example.annochat.data.repository.ChatRepository
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.designsystem.AnonChatTypography
import org.example.annochat.presentation.viewmodel.ChatViewModel

/**
 * Premium Liquid-Glass Bottom Sheet displaying Group Info, Member list with live DP online dots,
 * WhatsApp-style Group Admin badge, In-sheet Group Name Editing, and Admin Delete Group authority.
 */
@Composable
fun GroupInfoBottomSheet(
    roomId: String,
    initialRoomName: String,
    repository: ChatRepository,
    viewModel: ChatViewModel,
    accentColor: Color = AnonChatColors.Primary,
    onAccentColor: Color = AnonChatColors.OnPrimary,
    startInEditMode: Boolean = false,
    onDismiss: () -> Unit,
    onAddUserClick: () -> Unit,
    onLeaveRoomClick: () -> Unit,
    onSaveSuccess: (newName: String) -> Unit = {},
    onSaveError: (errorMessage: String) -> Unit = {},
    onGroupDeleted: () -> Unit = {},
    onAvatarClick: (avatarUrl: String?, name: String?, publicId: String) -> Unit = { _, _, _ -> }
) {
    val userRooms by repository.userRooms.collectAsState()
    val onlineUsers by repository.onlineUsers.collectAsState()
    val myIdentity by repository.myIdentity.collectAsState()

    val currentRoom: RoomDto? = remember(userRooms, roomId) {
        userRooms.find { isRoomMatching(it.roomId, roomId) }
    }

    var currentName by remember(initialRoomName, currentRoom?.name, currentRoom?.cleanDisplayName) {
        mutableStateOf(currentRoom?.cleanDisplayName?.ifBlank { null } ?: currentRoom?.name?.ifBlank { null } ?: initialRoomName)
    }

    val focusManager = LocalFocusManager.current
    val uiState by viewModel.uiState.collectAsState()
    val myPublicId = myIdentity?.publicId ?: viewModel.getMyPublicId() ?: ""
    val isLocallyOwned = remember(roomId) { repository.sessionStorage.isMyOwnedRoom(roomId) }

    // Isolate members strictly per roomId; reset immediately on roomId change
    var serverMembers by remember(roomId) { mutableStateOf<List<RoomMemberDto>?>(null) }
    var isLoadingMembers by remember(roomId) { mutableStateOf(true) }

    // Load fresh members from server for this specific roomId
    LaunchedEffect(roomId) {
        isLoadingMembers = true
        val result = repository.getRoomMembers(roomId)
        result.onSuccess { members ->
            val scoped = members.filter {
                it.roomId.isNullOrBlank() || isRoomMatching(it.roomId, roomId)
            }.map {
                if (it.roomId.isNullOrBlank()) it.copy(roomId = roomId) else it
            }
            serverMembers = scoped
        }.onFailure {
            // Keep serverMembers as null so local room members can be used as fallback
        }
        isLoadingMembers = false
    }

    // Unified members list containing ONLY members associated with this specific group
    val membersList: List<RoomMemberDto> = remember(serverMembers, currentRoom?.members, myPublicId, roomId, isLocallyOwned, currentRoom?.isOwner) {
        val baseList = (serverMembers ?: currentRoom?.members?.filter { it.roomId.isNullOrBlank() || isRoomMatching(it.roomId, roomId) } ?: emptyList()).toMutableList()

        val isLocalOwnerOrCreator = isLocallyOwned || currentRoom?.isOwner == true

        if (isLocalOwnerOrCreator) {
            // Requirement: Completely purge any old/stale identity previously recorded as owner/admin.
            // When identity is switched, the old animal identity must NEVER appear as group admin or duplicate member.
            baseList.removeAll { member ->
                !member.publicId.equals(myPublicId, ignoreCase = true) &&
                (member.isOwner == 1 ||
                 member.role.equals("ADMIN", ignoreCase = true) ||
                 member.role.equals("OWNER", ignoreCase = true) ||
                 member.publicId.equals(currentRoom?.effectiveOwnerPublicId, ignoreCase = true) ||
                 member.publicId.equals(currentRoom?.ownerPublicId, ignoreCase = true) ||
                 member.publicId.equals(currentRoom?.createdBy, ignoreCase = true))
            }
        }

        // Ensure current user is included with their CURRENT active identity as Group Admin if owner/admin
        val isUserInGroup = baseList.any { it.publicId.equals(myPublicId, ignoreCase = true) } ||
                isLocalOwnerOrCreator ||
                currentRoom?.isMember == true ||
                currentRoom?.effectiveOwnerPublicId?.equals(myPublicId, ignoreCase = true) == true

        if (myPublicId.isNotBlank() && isUserInGroup) {
            val existingIndex = baseList.indexOfFirst { it.publicId.equals(myPublicId, ignoreCase = true) }
            val currentAnonName = viewModel.getMyAnonName() ?: "You"
            val currentAvatar = viewModel.getMyAvatarUrl()
            if (existingIndex >= 0) {
                if (isLocalOwnerOrCreator) {
                    baseList[existingIndex] = baseList[existingIndex].copy(
                        anonName = currentAnonName,
                        anonAvatar = currentAvatar,
                        isOwner = 1,
                        role = "ADMIN"
                    )
                }
            } else {
                baseList.add(
                    0,
                    RoomMemberDto(
                        roomId = roomId,
                        publicId = myPublicId,
                        anonName = currentAnonName,
                        anonAvatar = currentAvatar,
                        isOwner = if (isLocalOwnerOrCreator) 1 else 0,
                        role = if (isLocalOwnerOrCreator) "ADMIN" else "MEMBER"
                    )
                )
            }
        }

        baseList.distinctBy { it.publicId }
            .sortedWith(
                compareByDescending<RoomMemberDto> { it.publicId.equals(myPublicId, ignoreCase = true) }
                    .thenByDescending {
                        if (isLocalOwnerOrCreator) {
                            it.publicId.equals(myPublicId, ignoreCase = true) || it.role.equals("ADMIN", ignoreCase = true)
                        } else {
                            it.isAdmin || it.isOwner == 1 || (currentRoom?.effectiveOwnerPublicId?.equals(it.publicId, ignoreCase = true) == true)
                        }
                    }
                    .thenBy { it.anonName?.lowercase() ?: "" }
            )
    }

    // Determine if current user is the admin (who created the group or has admin role)
    val isCurrentUserAdmin = currentRoom?.isUserAdmin(myPublicId, isLocallyOwned) == true ||
            isLocallyOwned ||
            currentRoom?.isOwner == true ||
            (myPublicId.isNotBlank() && currentRoom?.effectiveOwnerPublicId?.equals(myPublicId, ignoreCase = true) == true) ||
            membersList.any { it.publicId.equals(myPublicId, ignoreCase = true) && it.isAdmin }

    var isEditingName by remember(isCurrentUserAdmin) { mutableStateOf(startInEditMode && isCurrentUserAdmin) }
    var editedName by remember(currentName) { mutableStateOf(currentName) }
    var isSavingName by remember { mutableStateOf(false) }
    var nameErrorMessage by remember { mutableStateOf<String?>(null) }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isDeletingGroup by remember { mutableStateOf(false) }

    // Drag-down to close gesture state
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val animatedDragOffsetY by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "sheetDragOffset"
    )

    // BackHandler: If user is actively typing in edit mode, back button cancels editing instead of dismissing bottom sheet
    org.example.annochat.presentation.navigation.BackHandler(enabled = isEditingName) {
        isEditingName = false
        editedName = currentName
        nameErrorMessage = null
    }

    fun saveGroupName() {
        if (!isCurrentUserAdmin) {
            isEditingName = false
            onSaveError("Only the group admin can modify group settings")
            return
        }
        if (isSavingName) return // Prevent double-tap
        val trimmed = editedName.trim()
        if (trimmed.length < 2 || trimmed.length > 32) {
            nameErrorMessage = "Group name must be between 2 and 32 characters"
            onSaveError("Group name must be between 2 and 32 characters")
            return
        }
        if (trimmed == currentName) {
            isEditingName = false
            return
        }
        isSavingName = true
        nameErrorMessage = null
        focusManager.clearFocus()

        viewModel.updateRoomName(trimmed) { result ->
            isSavingName = false
            result.onSuccess {
                currentName = trimmed
                isEditingName = false
                onDismiss()
                onSaveSuccess(trimmed)
            }.onFailure { err ->
                val errorMsg = err.message?.takeIf { it.isNotBlank() } ?: "Failed to rename group"
                nameErrorMessage = errorMsg
                onSaveError(errorMsg)
            }
        }
    }

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
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, animatedDragOffsetY.roundToInt().coerceAtLeast(0)) }
                    .heightIn(max = 680.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color(0xFF141416))
                    .border(
                        1.dp,
                        AnonChatColors.Border.copy(alpha = 0.6f),
                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* prevent dismiss from inner clicks */ }
                    .navigationBarsPadding()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── Drag Handle (Drag down to close) ──────────────────────
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onVerticalDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                                        },
                                        onDragEnd = {
                                            if (dragOffsetY > 100.dp.toPx()) {
                                                onDismiss()
                                            } else {
                                                dragOffsetY = 0f
                                            }
                                        },
                                        onDragCancel = {
                                            dragOffsetY = 0f
                                        }
                                    )
                                }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(AnonChatColors.TextMuted.copy(alpha = 0.55f))
                            )
                        }
                    }

                    // ── Header / Group Name Section ──────────────────────────
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onVerticalDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                                        },
                                        onDragEnd = {
                                            if (dragOffsetY > 100.dp.toPx()) {
                                                onDismiss()
                                            } else {
                                                dragOffsetY = 0f
                                            }
                                        },
                                        onDragCancel = {
                                            dragOffsetY = 0f
                                        }
                                    )
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Large Group Icon Avatar
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(AnonChatColors.Primary.copy(alpha = 0.10f))
                                    .border(1.5.dp, AnonChatColors.Primary.copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (currentRoom?.isPublic == false) AnonChatIcons.Lock else AnonChatIcons.Users,
                                    contentDescription = null,
                                    tint = AnonChatColors.TextPrimary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            // Group Name & Inline Rename Editor
                            if (!isEditingName) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = currentName,
                                        style = AnonChatTypography.ScreenTitle,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AnonChatColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isCurrentUserAdmin) {
                                        Spacer(Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(AnonChatColors.SurfaceSecondary)
                                                .clickable {
                                                    editedName = currentName
                                                    isEditingName = true
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = AnonChatIcons.Edit,
                                                contentDescription = "Edit group name",
                                                tint = AnonChatColors.TextSecondary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }

                                if (!currentRoom?.desc.isNullOrBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = currentRoom?.desc ?: "",
                                        style = AnonChatTypography.Body,
                                        fontSize = 12.5.sp,
                                        color = AnonChatColors.TextSecondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            } else {
                                // Inline Editor Box (matches Image 1)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(AnonChatColors.SurfaceSecondary)
                                        .border(1.dp, AnonChatColors.Border, RoundedCornerShape(16.dp))
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = "EDIT GROUP NAME",
                                        style = AnonChatTypography.Caption,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        letterSpacing = 0.8.sp,
                                        color = AnonChatColors.TextMuted
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    BasicTextField(
                                        value = editedName,
                                        onValueChange = {
                                            if (it.length <= 32) {
                                                editedName = it
                                                nameErrorMessage = null
                                            }
                                        },
                                        textStyle = TextStyle(
                                            color = AnonChatColors.TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        cursorBrush = SolidColor(AnonChatColors.Primary),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = { saveGroupName() }),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${editedName.trim().length}/32",
                                            fontSize = 11.sp,
                                            color = AnonChatColors.TextMuted
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            // Cancel Button
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(AnonChatColors.SurfacePrimary)
                                                    .clickable {
                                                        isEditingName = false
                                                        editedName = currentName
                                                        nameErrorMessage = null
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                                            ) {
                                                Text(
                                                    text = "Cancel",
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = AnonChatColors.TextSecondary
                                                )
                                            }

                                            // Save Button (Fixed UI as per Image 1: White pill with solid dark legible text)
                                            val isNameValid = editedName.trim().length in 2..32
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isNameValid) AnonChatColors.Primary
                                                        else AnonChatColors.Primary.copy(alpha = 0.35f)
                                                    )
                                                    .clickable(enabled = !isSavingName && isNameValid) {
                                                        saveGroupName()
                                                    }
                                                    .padding(horizontal = 18.dp, vertical = 7.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSavingName) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(13.dp),
                                                        strokeWidth = 2.dp,
                                                        color = AnonChatColors.OnPrimary
                                                    )
                                                } else {
                                                    Text(
                                                        text = "Save",
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isNameValid) AnonChatColors.OnPrimary else AnonChatColors.OnPrimary.copy(alpha = 0.5f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (nameErrorMessage != null) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = nameErrorMessage ?: "",
                                    fontSize = 12.sp,
                                    color = AnonChatColors.Error
                                )
                            }
                        }
                    }

                    // ── Unified Members Section Header ───────────────────────
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "MEMBERS (${membersList.size})",
                                    style = AnonChatTypography.SectionHeader,
                                    color = AnonChatColors.TextMuted
                                )

                                val onlineInGroupCount = membersList.count { m ->
                                    m.publicId.equals(myPublicId, ignoreCase = true) || onlineUsers.any { it.publicId == m.publicId }
                                }
                                if (onlineInGroupCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(AnonChatColors.Success.copy(alpha = 0.14f))
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "$onlineInGroupCount online",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = AnonChatColors.Success
                                        )
                                    }
                                }
                            }

                            // Add Member quick action
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(AnonChatColors.SurfaceElevated)
                                    .border(1.dp, AnonChatColors.Border, CircleShape)
                                    .clickable {
                                        onDismiss()
                                        onAddUserClick()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = AnonChatIcons.UserPlus,
                                        contentDescription = null,
                                        tint = AnonChatColors.TextPrimary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Add",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AnonChatColors.TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // ── UNIFIED MEMBERS LIST ─────────────────────────────────
                    if (isLoadingMembers && membersList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AnonChatColors.SurfaceSecondary.copy(alpha = 0.3f))
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = AnonChatColors.TextMuted
                                    )
                                    Text(
                                        text = "Loading members…",
                                        fontSize = 12.sp,
                                        color = AnonChatColors.TextMuted
                                    )
                                }
                            }
                        }
                    } else if (membersList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AnonChatColors.SurfaceSecondary.copy(alpha = 0.3f))
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No members found in this group",
                                    fontSize = 12.sp,
                                    color = AnonChatColors.TextMuted
                                )
                            }
                        }
                    } else {
                        items(membersList, key = { it.publicId }) { member ->
                            val isSelf = member.publicId.equals(myPublicId, ignoreCase = true)
                            val isMemberAdmin = if (isLocallyOwned || currentRoom?.isOwner == true) {
                                isSelf || member.role.equals("ADMIN", ignoreCase = true)
                            } else {
                                member.isAdmin ||
                                        member.isOwner == 1 ||
                                        (currentRoom?.ownerPublicId != null && member.publicId.equals(currentRoom.ownerPublicId, ignoreCase = true)) ||
                                        (currentRoom?.effectiveOwnerPublicId != null && member.publicId.equals(currentRoom.effectiveOwnerPublicId, ignoreCase = true))
                            }
                            val isOnline = isSelf || onlineUsers.any { it.publicId == member.publicId }

                            GroupMemberRow(
                                member = member,
                                isOnline = isOnline,
                                isSelf = isSelf,
                                isMemberAdmin = isMemberAdmin,
                                repository = repository,
                                onAvatarClick = onAvatarClick
                            )
                        }
                    }

                    // ── Bottom Action: Admin Delete Group / Member Leave Group ────
                    item {
                        Spacer(Modifier.height(8.dp))
                        if (isCurrentUserAdmin) {
                            // Admin has authority to Delete Group
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AnonChatColors.Error.copy(alpha = 0.12f))
                                    .border(1.dp, AnonChatColors.Error.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                    .clickable { showDeleteConfirmDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = AnonChatIcons.Trash,
                                        contentDescription = null,
                                        tint = AnonChatColors.Error,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Delete Group",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AnonChatColors.Error
                                    )
                                }
                            }
                        } else {
                            // Non-admin members can Leave Group
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AnonChatColors.SurfaceElevated)
                                    .border(1.dp, AnonChatColors.Border, RoundedCornerShape(14.dp))
                                    .clickable {
                                        onDismiss()
                                        onLeaveRoomClick()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = AnonChatIcons.LogOut,
                                        contentDescription = null,
                                        tint = AnonChatColors.Error,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Leave Group",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AnonChatColors.Error
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // ── Admin Delete Group Confirmation Dialog ───────────────────────────────
    if (showDeleteConfirmDialog) {
        Dialog(
            onDismissRequest = {
                if (!isDeletingGroup) showDeleteConfirmDialog = false
            },
            properties = DialogProperties(
                dismissOnBackPress = !isDeletingGroup,
                dismissOnClickOutside = !isDeletingGroup
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AnonChatColors.SurfaceElevated)
                    .border(1.dp, AnonChatColors.Border, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "Delete \"$currentName\"?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AnonChatColors.TextPrimary
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Are you sure you want to permanently delete this group? All messages and members will be removed. This action cannot be undone.",
                        fontSize = 13.5.sp,
                        color = AnonChatColors.TextSecondary,
                        lineHeight = 19.sp
                    )

                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(enabled = !isDeletingGroup) {
                                    showDeleteConfirmDialog = false
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = AnonChatColors.TextSecondary
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(AnonChatColors.Error)
                                .clickable(enabled = !isDeletingGroup) {
                                    isDeletingGroup = true
                                    viewModel.deleteGroup { result ->
                                        isDeletingGroup = false
                                        result.onSuccess {
                                            showDeleteConfirmDialog = false
                                            onDismiss()
                                            onGroupDeleted()
                                        }.onFailure { err ->
                                            showDeleteConfirmDialog = false
                                            onSaveError(err.message ?: "Failed to delete group")
                                        }
                                    }
                                }
                                .padding(horizontal = 18.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDeletingGroup) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Text(
                                    text = "Delete Group",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupMemberRow(
    member: RoomMemberDto,
    isOnline: Boolean,
    isSelf: Boolean,
    isMemberAdmin: Boolean,
    repository: ChatRepository,
    onAvatarClick: (avatarUrl: String?, name: String?, publicId: String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AnonChatColors.SurfaceSecondary.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with online / offline status dot
        Box(contentAlignment = Alignment.BottomEnd) {
            AnimalAvatar(
                repository = repository,
                avatarUrl = member.anonAvatar,
                animalName = member.anonName,
                publicId = member.publicId,
                size = 40.dp,
                borderWidth = 1.dp,
                borderColor = if (isOnline) AnonChatColors.Success.copy(alpha = 0.5f) else AnonChatColors.Border,
                onClick = {
                    onAvatarClick(member.anonAvatar, member.anonName, member.publicId)
                }
            )

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isOnline) AnonChatColors.Success else AnonChatColors.TextMuted.copy(alpha = 0.5f))
                    .border(1.5.dp, AnonChatColors.SurfacePrimary, CircleShape)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = member.anonName ?: "Anonymous Member",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AnonChatColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isSelf) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "(You)",
                        fontSize = 11.sp,
                        color = AnonChatColors.TextMuted
                    )
                }
            }

            Spacer(Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isOnline) AnonChatColors.Success else AnonChatColors.TextMuted.copy(alpha = 0.5f))
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = if (isOnline) "Online" else "Offline",
                    fontSize = 11.5.sp,
                    fontWeight = if (isOnline) FontWeight.Medium else FontWeight.Normal,
                    color = if (isOnline) AnonChatColors.Success else AnonChatColors.TextMuted
                )
            }
        }

        // WhatsApp-style Group Admin Badge
        if (isMemberAdmin) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AnonChatColors.Success.copy(alpha = 0.12f))
                    .border(1.dp, AnonChatColors.Success.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Group Admin",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AnonChatColors.Success
                )
            }
        }
    }
}
