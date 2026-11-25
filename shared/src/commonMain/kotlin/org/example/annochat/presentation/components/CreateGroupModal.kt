package org.example.annochat.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import org.example.annochat.data.model.RoomDto
import org.example.annochat.data.repository.ChatRepository
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.designsystem.AnonChatSpacing
import org.example.annochat.presentation.designsystem.AnonChatTypography
import org.example.annochat.presentation.theme.LocalAppearanceSettings


@Composable
fun CreateGroupModal(
    repository: ChatRepository,
    initialIsPublic: Boolean = true,
    onDismiss: () -> Unit,
    onGroupCreated: (RoomDto) -> Unit,
    modifier: Modifier = Modifier
) {
    val appearance = LocalAppearanceSettings.current
    val accentColor = Color(appearance.accent.colorHex)
    val isLight = (0.2126f * accentColor.red + 0.7152f * accentColor.green + 0.0722f * accentColor.blue) > 0.45f
    val onAccentColor = if (isLight) Color.Black else Color.White

    var groupName by remember { mutableStateOf("") }
    var groupDesc by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(initialIsPublic) }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val trimmedName = groupName.trim()
    val isNameValid = trimmedName.length in 2..32

    fun submitCreation() {
        if (!isNameValid || isCreating) return
        focusManager.clearFocus()
        isCreating = true
        errorMessage = null

        val effectiveDesc = if (groupDesc.isNotBlank()) groupDesc.trim() else null

        scope.launch {
            val result = repository.createRoom(
                name = trimmedName,
                desc = effectiveDesc,
                isPublic = isPublic
            )
            result.onSuccess { createdRoom ->
                isCreating = false
                onGroupCreated(createdRoom)
                onDismiss()
            }.onFailure { err ->
                isCreating = false
                errorMessage = err.message ?: "Failed to create group. Please check your connection."
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isCreating) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isCreating,
            dismissOnClickOutside = !isCreating,
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
                ) {
                    if (!isCreating) onDismiss()
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF141416))
                    .border(1.dp, AnonChatColors.Border, RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* prevent backdrop tap dismissal */ }
                    .padding(AnonChatSpacing.lg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 600.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AnonChatSpacing.sm)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AnonChatColors.SurfaceSecondary)
                                    .border(1.dp, AnonChatColors.Border, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = AnonChatIcons.Users,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Create Group",
                                    style = AnonChatTypography.BrandTitle,
                                    color = AnonChatColors.TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Start a space for your campus circle",
                                    style = AnonChatTypography.Caption,
                                    color = AnonChatColors.TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Close button
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(AnonChatColors.SurfaceSecondary)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    enabled = !isCreating
                                ) { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AnonChatIcons.Close,
                                contentDescription = "Close",
                                tint = AnonChatColors.TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Error Banner
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        errorMessage?.let { error ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF3B1818))
                                    .border(1.dp, Color(0xFF6B2A2A), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = error,
                                    color = Color(0xFFFF8B8B),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Group Name Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GROUP NAME",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AnonChatColors.TextMuted,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${groupName.length}/32",
                            fontSize = 11.sp,
                            color = if (groupName.length > 32) Color.Red else AnonChatColors.TextMuted
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = groupName,
                        onValueChange = {
                            if (it.length <= 32) groupName = it
                            if (errorMessage != null) errorMessage = null
                        },
                        placeholder = {
                            Text(
                                text = "e.g. Night Coders, Hackathon Prep",
                                color = AnonChatColors.TextMuted,
                                fontSize = 13.5.sp
                            )
                        },
                        singleLine = true,
                        enabled = !isCreating,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = AnonChatColors.SurfaceSecondary,
                            unfocusedContainerColor = AnonChatColors.SurfaceSecondary,
                            focusedTextColor = AnonChatColors.TextPrimary,
                            unfocusedTextColor = AnonChatColors.TextPrimary,
                            focusedIndicatorColor = accentColor,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledContainerColor = AnonChatColors.SurfaceSecondary.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AnonChatColors.Border, RoundedCornerShape(12.dp))
                    )

                    Spacer(Modifier.height(16.dp))

                    // Group Description Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOPIC / DESCRIPTION (OPTIONAL)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AnonChatColors.TextMuted,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${groupDesc.length}/140",
                            fontSize = 11.sp,
                            color = AnonChatColors.TextMuted
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = groupDesc,
                        onValueChange = {
                            if (it.length <= 140) groupDesc = it
                        },
                        placeholder = {
                            Text(
                                text = "What is this group about?",
                                color = AnonChatColors.TextMuted,
                                fontSize = 13.5.sp
                            )
                        },
                        maxLines = 3,
                        enabled = !isCreating,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            submitCreation()
                        }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = AnonChatColors.SurfaceSecondary,
                            unfocusedContainerColor = AnonChatColors.SurfaceSecondary,
                            focusedTextColor = AnonChatColors.TextPrimary,
                            unfocusedTextColor = AnonChatColors.TextPrimary,
                            focusedIndicatorColor = accentColor,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledContainerColor = AnonChatColors.SurfaceSecondary.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AnonChatColors.Border, RoundedCornerShape(12.dp))
                    )

                    Spacer(Modifier.height(20.dp))

                    // Visibility Selector
                    Text(
                        text = "VISIBILITY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AnonChatColors.TextMuted,
                        letterSpacing = 1.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Public option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isPublic) AnonChatColors.SurfaceSecondary else Color.Transparent)
                                .border(
                                    width = if (isPublic) 1.5.dp else 1.dp,
                                    color = if (isPublic) accentColor else AnonChatColors.Border,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = !isCreating) { isPublic = true }
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = AnonChatIcons.Globe,
                                        contentDescription = null,
                                        tint = if (isPublic) accentColor else AnonChatColors.TextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Public",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isPublic) AnonChatColors.TextPrimary else AnonChatColors.TextSecondary
                                    )
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = "Discoverable in campus feed",
                                    fontSize = 11.sp,
                                    color = AnonChatColors.TextMuted,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        // Private option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (!isPublic) AnonChatColors.SurfaceSecondary else Color.Transparent)
                                .border(
                                    width = if (!isPublic) 1.5.dp else 1.dp,
                                    color = if (!isPublic) accentColor else AnonChatColors.Border,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = !isCreating) { isPublic = false }
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = AnonChatIcons.Shield,
                                        contentDescription = null,
                                        tint = if (!isPublic) accentColor else AnonChatColors.TextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Private",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (!isPublic) AnonChatColors.TextPrimary else AnonChatColors.TextSecondary
                                    )
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = "Join by link or direct invite",
                                    fontSize = 11.sp,
                                    color = AnonChatColors.TextMuted,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Create Group Action Button with loading state & double-tap protection
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isNameValid && !isCreating) accentColor
                                else accentColor.copy(alpha = 0.35f)
                            )
                            .clickable(
                                enabled = isNameValid && !isCreating,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                submitCreation()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCreating) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = onAccentColor
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Creating Space...",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = onAccentColor
                                )
                            }
                        } else {
                            Text(
                                text = "Create Group",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isNameValid) onAccentColor else onAccentColor.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}
