package org.example.annochat.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.example.annochat.data.repository.UpdateState
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons

@Composable
fun AppUpdateDialog(
    state: UpdateState,
    onUpdateClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state is UpdateState.Idle) return

    val (info, isForced) = when (state) {
        is UpdateState.Available -> state.info to state.isForced
        is UpdateState.Downloading -> state.info to state.isForced
        is UpdateState.ReadyToInstall -> state.info to state.isForced
        is UpdateState.Error -> state.info to state.isForced
        UpdateState.Idle -> return
    }

    Dialog(
        onDismissRequest = {
            if (!isForced && state !is UpdateState.Downloading) {
                onDismissClick()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isForced && state !is UpdateState.Downloading,
            dismissOnClickOutside = !isForced && state !is UpdateState.Downloading,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .shadow(
                        elevation = 32.dp,
                        shape = RoundedCornerShape(28.dp),
                        spotColor = Color.Black,
                        ambientColor = Color.Black
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF121418))
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(26.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Update Badge Icon
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF1E2430),
                                        Color(0xFF141720)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        if (isForced) Color(0xFFE57373) else Color(0xFF64B5F6),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isForced) AnonChatIcons.Shield else AnonChatIcons.Download,
                            contentDescription = null,
                            tint = if (isForced) Color(0xFFFF6E6E) else Color(0xFF64B5F6),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Headline
                    Text(
                        text = if (isForced) "CRITICAL UPDATE" else "UPDATE AVAILABLE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isForced) Color(0xFFFF6E6E) else Color(0xFF8E99A8),
                        letterSpacing = 1.2.sp
                    )

                    Spacer(Modifier.height(4.dp))

                    // App Name & Version
                    Text(
                        text = "AnnoChat ${info?.latestVersionName ?: ""}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-0.3).sp
                    )

                    if (isForced) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "This version is required to continue using AnnoChat.",
                            fontSize = 12.5.sp,
                            color = Color(0xFFFFB4B4),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Release notes
                    val notes = info?.releaseNotes ?: emptyList()
                    if (notes.isNotEmpty() && state !is UpdateState.Downloading) {
                        Spacer(Modifier.height(18.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0C0E12))
                                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "What's new",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.90f)
                            )

                            Spacer(Modifier.height(10.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                notes.forEach { note ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "• ",
                                            fontSize = 13.sp,
                                            color = Color(0xFF64B5F6),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = note,
                                            fontSize = 12.5.sp,
                                            color = Color.White.copy(alpha = 0.75f),
                                            lineHeight = 17.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Download Progress View
                    if (state is UpdateState.Downloading) {
                        Spacer(Modifier.height(24.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (state.progress >= 0f) {
                                LinearProgressIndicator(
                                    progress = { state.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = Color(0xFF64B5F6),
                                    trackColor = Color.White.copy(alpha = 0.10f)
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${(state.progress * 100).toInt()}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    val sizeLabel = if (state.totalBytes > 0) {
                                        "${formatMb(state.bytesRead)} / ${formatMb(state.totalBytes)}"
                                    } else {
                                        formatMb(state.bytesRead)
                                    }
                                    Text(
                                        text = sizeLabel,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.60f)
                                    )
                                }
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = Color(0xFF64B5F6),
                                    strokeWidth = 3.dp
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Downloading update…",
                                    fontSize = 12.5.sp,
                                    color = Color.White.copy(alpha = 0.70f)
                                )
                            }
                        }
                    }

                    // Error View
                    if (state is UpdateState.Error) {
                        Spacer(Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF331616))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = state.message,
                                fontSize = 12.sp,
                                color = Color(0xFFFFB4B4),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Ready to Install View
                    if (state is UpdateState.ReadyToInstall) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Download complete. Tap below to launch installer.",
                            fontSize = 13.sp,
                            color = Color(0xFF81C784),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // Action Buttons
                    when (state) {
                        is UpdateState.Downloading -> {
                            // Non-interactive progress view
                            Text(
                                text = "Preparing package installation…",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.50f)
                            )
                        }
                        is UpdateState.ReadyToInstall -> {
                            AnonChatPrimaryButton(
                                text = "Install Now",
                                onClick = onUpdateClick,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        is UpdateState.Error -> {
                            AnonChatPrimaryButton(
                                text = "Retry Update",
                                onClick = onUpdateClick,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (!isForced) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "Later",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.60f),
                                    modifier = Modifier
                                        .clickable { onDismissClick() }
                                        .padding(8.dp)
                                )
                            }
                        }
                        is UpdateState.Available -> {
                            AnonChatPrimaryButton(
                                text = "Update",
                                onClick = onUpdateClick,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (!isForced) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "Later",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.60f),
                                    modifier = Modifier
                                        .clickable { onDismissClick() }
                                        .padding(8.dp)
                                )
                            }
                        }
                        UpdateState.Idle -> {}
                    }
                }
            }
        }
    }
}

private fun formatMb(bytes: Long): String {
    val mb = bytes.toFloat() / (1024 * 1024)
    val intPart = mb.toInt()
    val decPart = ((mb - intPart) * 10).toInt().coerceIn(0, 9)
    return "$intPart.$decPart MB"
}
