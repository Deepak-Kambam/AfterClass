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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import org.example.annochat.data.model.AppNotification
import org.example.annochat.data.model.NotificationType
import org.example.annochat.data.repository.NotificationRepository
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.designsystem.AnonChatShapes
import org.example.annochat.presentation.designsystem.AnonChatSpacing
import org.example.annochat.presentation.designsystem.AnonChatTypography

@Composable
fun NotificationsModal(
    notificationRepository: NotificationRepository,
    onDismiss: () -> Unit,
    onOpenRoom: (roomId: String, roomName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val notifications by notificationRepository.notifications.collectAsState()
    val unreadCount by notificationRepository.unreadCount.collectAsState()
    val scope = rememberCoroutineScope()
    val accentColor = MaterialTheme.colorScheme.primary

    // Reading notifications automatically marks them as read, clearing badge in Candy Menu
    LaunchedEffect(Unit) {
        notificationRepository.markAllAsRead()
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
                ) {
                    // Modal Header
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
                                imageVector = AnonChatIcons.Bell,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Notifications",
                                style = AnonChatTypography.BrandTitle,
                                color = AnonChatColors.TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${notifications.size} total alerts",
                                style = AnonChatTypography.Caption,
                                color = AnonChatColors.TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (notifications.isNotEmpty()) {
                            Text(
                                text = "Clear All",
                                color = AnonChatColors.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { notificationRepository.clearAll() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

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
                }

                Spacer(Modifier.height(16.dp))

                // Content List
                if (notifications.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(AnonChatColors.SurfaceSecondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AnonChatIcons.Bell,
                                contentDescription = null,
                                tint = AnonChatColors.TextMuted,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "No Notifications",
                            color = AnonChatColors.TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Invites, announcements, and channel messages will arrive here.",
                            color = AnonChatColors.TextSecondary,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(notifications, key = { it.id }) { notif ->
                            NotificationCardItem(
                                notification = notif,
                                accentColor = accentColor,
                                onAcceptInvite = {
                                    notif.inviteId?.let { id ->
                                        scope.launch {
                                            notificationRepository.respondToInvite(id, true)
                                        }
                                    }
                                },
                                onDeclineInvite = {
                                    notif.inviteId?.let { id ->
                                        scope.launch {
                                            notificationRepository.respondToInvite(id, false)
                                        }
                                    }
                                },
                                onOpenMessageRoom = {
                                    onDismiss()
                                    onOpenRoom(notif.roomId ?: "main", notif.roomName ?: notif.title)
                                },
                                onDelete = {
                                    notificationRepository.removeNotification(notif.id)
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
private fun NotificationCardItem(
    notification: AppNotification,
    accentColor: Color,
    onAcceptInvite: () -> Unit,
    onDeclineInvite: () -> Unit,
    onOpenMessageRoom: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AnonChatColors.SurfaceSecondary)
            .border(
                1.dp,
                if (!notification.isRead) accentColor.copy(alpha = 0.40f) else AnonChatColors.Border,
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(
                                when (notification.type) {
                                    NotificationType.INVITE -> accentColor.copy(alpha = 0.20f)
                                    NotificationType.ANNOUNCEMENT -> AnonChatColors.SurfaceTertiary
                                    NotificationType.MESSAGE -> AnonChatColors.SurfaceTertiary
                                    NotificationType.SYSTEM -> AnonChatColors.SurfaceTertiary
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (notification.type) {
                                NotificationType.INVITE -> AnonChatIcons.Paw
                                NotificationType.ANNOUNCEMENT -> AnonChatIcons.Globe
                                NotificationType.MESSAGE -> AnonChatIcons.Chat
                                NotificationType.SYSTEM -> AnonChatIcons.Sparkles
                            },
                            contentDescription = null,
                            tint = when (notification.type) {
                                NotificationType.INVITE -> accentColor
                                else -> AnonChatColors.TextPrimary
                            },
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        text = notification.title,
                        color = AnonChatColors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = notification.timeLabel,
                        color = AnonChatColors.TextMuted,
                        fontSize = 11.sp
                    )

                    Icon(
                        imageVector = AnonChatIcons.Close,
                        contentDescription = "Dismiss",
                        tint = AnonChatColors.TextMuted,
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .clickable { onDelete() }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = notification.message,
                color = AnonChatColors.TextSecondary,
                fontSize = 12.5.sp,
                lineHeight = 17.sp
            )

            // Conditional Action Buttons
            when (notification.type) {
                NotificationType.INVITE -> {
                    Spacer(Modifier.height(12.dp))
                    if (!notification.isHandled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AnonChatSecondaryButton(
                                text = "Decline",
                                onClick = onDeclineInvite,
                                modifier = Modifier.weight(1f)
                            )
                            AnonChatPrimaryButton(
                                text = "Accept",
                                onClick = onAcceptInvite,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Text(
                            text = "✓ Invite Handled",
                            color = AnonChatColors.TextMuted,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                NotificationType.MESSAGE -> {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenMessageRoom() }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Jump to conversation",
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = AnonChatIcons.ArrowRight,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                else -> {}
            }
        }
    }
}
