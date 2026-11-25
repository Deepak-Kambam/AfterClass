package org.example.annochat.presentation.components

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.example.annochat.data.model.PublicRoomDto
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.designsystem.AnonChatShapes
import org.example.annochat.presentation.designsystem.AnonChatSpacing
import org.example.annochat.presentation.designsystem.AnonChatTypography

/**
 * Sanitizes room name into a valid room ID.
 */
fun sanitizeRoomId(input: String): String {
    val clean = input.trim().lowercase().replace(Regex("[^a-z0-9_-]"), "")
    return clean.ifBlank { "room_${input.trim().hashCode().toString(16)}" }
}

@Composable
fun CreateOrJoinRoomModal(
    publicRooms: List<PublicRoomDto>,
    onDismiss: () -> Unit,
    onRoomSelected: (roomId: String, roomName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var customRoomName by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val accentColor = MaterialTheme.colorScheme.primary
    val onAccentColor = MaterialTheme.colorScheme.onPrimary

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
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF141416))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* prevent backdrop tap dismissal */ }
                    .padding(AnonChatSpacing.lg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 580.dp)
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
                                imageVector = AnonChatIcons.Plus,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Create or Join Room",
                                style = AnonChatTypography.BrandTitle,
                                color = AnonChatColors.TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Explore channels or enter custom space",
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
                                indication = ripple(bounded = true)
                            ) { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AnonChatIcons.Close,
                            contentDescription = "Close",
                            tint = AnonChatColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Section 1: Create / Join Custom Room
                Text(
                    text = "CUSTOM ROOM",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AnonChatColors.TextMuted,
                    letterSpacing = 1.sp
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = customRoomName,
                        onValueChange = { if (it.length <= 32) customRoomName = it },
                        placeholder = {
                            Text(
                                "e.g. night_study, campus_gaming",
                                color = AnonChatColors.TextMuted,
                                fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            val trimmed = customRoomName.trim()
                            if (trimmed.isNotBlank()) {
                                focusManager.clearFocus()
                                onRoomSelected(sanitizeRoomId(trimmed), trimmed)
                                onDismiss()
                            }
                        }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = AnonChatColors.SurfaceSecondary,
                            unfocusedContainerColor = AnonChatColors.SurfaceSecondary,
                            focusedTextColor = AnonChatColors.TextPrimary,
                            unfocusedTextColor = AnonChatColors.TextPrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, AnonChatColors.Border, RoundedCornerShape(12.dp))
                    )

                    val isValid = customRoomName.trim().isNotBlank()
                    AnonChatPrimaryButton(
                        text = "Enter",
                        enabled = isValid,
                        onClick = {
                            val trimmed = customRoomName.trim()
                            if (trimmed.isNotBlank()) {
                                focusManager.clearFocus()
                                onRoomSelected(sanitizeRoomId(trimmed), trimmed)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.width(80.dp)
                    )
                }

                Spacer(Modifier.height(22.dp))

                // Section 2: Public Campus Channels
                Text(
                    text = "PUBLIC CHANNELS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AnonChatColors.TextMuted,
                    letterSpacing = 1.sp
                )

                Spacer(Modifier.height(10.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (publicRooms.isEmpty()) {
                        Text(
                            text = "No public channels currently available.",
                            color = AnonChatColors.TextMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        publicRooms.forEach { room ->
                            PublicRoomRowItem(
                                room = room,
                                accentColor = accentColor,
                                onClick = {
                                    onRoomSelected(room.id, room.name)
                                    onDismiss()
                                }
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
private fun PublicRoomRowItem(
    room: PublicRoomDto,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AnonChatColors.SurfaceSecondary)
            .border(1.dp, AnonChatColors.Border, RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = accentColor),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AnonChatColors.SurfaceTertiary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (room.id) {
                        "cse" -> AnonChatIcons.Terminal
                        "ece" -> AnonChatIcons.Cpu
                        "mech" -> AnonChatIcons.Wrench
                        "civil" -> AnonChatIcons.Building
                        "faculty" -> AnonChatIcons.GraduationCap
                        "hostel" -> AnonChatIcons.Bed
                        else -> AnonChatIcons.Globe
                    },
                    contentDescription = null,
                    tint = AnonChatColors.TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column {
                Text(
                    text = room.name,
                    color = AnonChatColors.TextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (!room.desc.isNullOrBlank()) {
                    Text(
                        text = room.desc,
                        color = AnonChatColors.TextSecondary,
                        fontSize = 11.5.sp,
                        maxLines = 1
                    )
                }
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
