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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import org.example.annochat.data.model.BroadcastSeverity
import org.example.annochat.data.repository.BroadcastRepository
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.designsystem.AnonChatShapes
import org.example.annochat.presentation.designsystem.AnonChatSpacing
import org.example.annochat.presentation.designsystem.AnonChatTypography

/**
 * Clean, production-quality Host Broadcast Composer Dialog.
 *
 * Exclusively accessible to authorized hosts (identity.isHost == true).
 * Allows broadcasting system announcements across campus or to specific rooms.
 */
@Composable
fun AdminBroadcastDialog(
    broadcastRepository: BroadcastRepository,
    currentRoomId: String? = null,
    onDismiss: () -> Unit
) {
    var message by remember { mutableStateOf("") }
    var selectedSeverity by remember { mutableStateOf(BroadcastSeverity.INFO) }
    var selectedDuration by remember { mutableIntStateOf(30) }
    var targetRoom by remember { mutableStateOf(currentRoomId?.takeIf { it != "main" } ?: "") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(AnonChatShapes.container)
                .background(Color(0xFF111111))
                .border(1.2.dp, AnonChatColors.BorderHighlight.copy(alpha = 0.40f), AnonChatShapes.container)
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AnonChatIcons.BroadcastBeacon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Campus Broadcast",
                            style = AnonChatTypography.BrandTitle,
                            color = AnonChatColors.TextPrimary
                        )
                        Text(
                            text = "Send official system announcement",
                            fontSize = 12.5.sp,
                            color = AnonChatColors.TextSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !isSubmitting) { onDismiss() },
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

                Spacer(Modifier.height(16.dp))

                // Message Text Field
                Text(
                    text = "ANNOUNCEMENT MESSAGE",
                    style = AnonChatTypography.SectionHeader,
                    color = AnonChatColors.TextMuted
                )
                Spacer(Modifier.height(6.dp))
                TextField(
                    value = message,
                    onValueChange = { if (it.length <= 300) message = it },
                    placeholder = {
                        Text("e.g. Server maintenance begins at 10:30 PM", color = AnonChatColors.TextDisabled, fontSize = 14.sp)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .clip(AnonChatShapes.card)
                        .border(1.dp, AnonChatColors.Border, AnonChatShapes.card),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF181818),
                        unfocusedContainerColor = Color(0xFF141414),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = AnonChatColors.TextPrimary,
                        unfocusedTextColor = AnonChatColors.TextPrimary
                    )
                )

                Spacer(Modifier.height(14.dp))

                // Severity Selector
                Text(
                    text = "SEVERITY LEVEL",
                    style = AnonChatTypography.SectionHeader,
                    color = AnonChatColors.TextMuted
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BroadcastSeverity.entries.forEach { severity ->
                        val isSelected = selectedSeverity == severity
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(AnonChatShapes.card)
                                .background(if (isSelected) Color(0xFF262626) else Color(0xFF141414))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Color.White else AnonChatColors.BorderSubtle,
                                    shape = AnonChatShapes.card
                                )
                                .clickable { selectedSeverity = severity }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = severity.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else AnonChatColors.TextSecondary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Duration Selector
                Text(
                    text = "DISPLAY DURATION",
                    style = AnonChatTypography.SectionHeader,
                    color = AnonChatColors.TextMuted
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(15 to "15s", 30 to "30s", 60 to "60s", 0 to "Persistent").forEach { (dur, label) ->
                        val isSelected = selectedDuration == dur
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(AnonChatShapes.pill)
                                .background(if (isSelected) Color.White else Color(0xFF161616))
                                .border(
                                    1.dp,
                                    if (isSelected) Color.White else AnonChatColors.BorderSubtle,
                                    AnonChatShapes.pill
                                )
                                .clickable { selectedDuration = dur }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.Black else AnonChatColors.TextSecondary
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFDD8888),
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AnonChatSecondaryButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isSubmitting
                    )
                    AnonChatPrimaryButton(
                        text = if (isSubmitting) "Broadcasting…" else "Broadcast Now",
                        onClick = {
                            if (message.isBlank()) {
                                errorMessage = "Message cannot be empty."
                                return@AnonChatPrimaryButton
                            }
                            isSubmitting = true
                            errorMessage = null
                            scope.launch {
                                val result = broadcastRepository.sendAdminBroadcast(
                                    message = message.trim(),
                                    severity = selectedSeverity,
                                    roomId = targetRoom.trim().ifBlank { null },
                                    duration = selectedDuration
                                )
                                isSubmitting = false
                                if (result.isSuccess) {
                                    onDismiss()
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Broadcast failed."
                                }
                            }
                        },
                        modifier = Modifier.weight(1.3f),
                        enabled = !isSubmitting && message.isNotBlank()
                    )
                }
            }
        }
    }
}
