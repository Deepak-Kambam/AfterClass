package org.example.annochat.presentation.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import org.example.annochat.presentation.designsystem.AnonChatColors
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import org.example.annochat.presentation.components.StartupNoInternetScreen
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.example.annochat.data.repository.ChatRepository
import org.example.annochat.data.session.SessionStorage
import org.example.annochat.presentation.designsystem.AnonChatShapes
import org.example.annochat.presentation.designsystem.AnonChatSpacing
import org.example.annochat.presentation.designsystem.AnonChatTypography

/**
 * Full-screen "Unable to Load Rooms / No Internet" error screen.
 *
 * 3-Layer Visual Architecture:
 * 1. Layer 1 (Base): Full-screen expansive Wi-Fi symbol radiating from bottom-left across the whole screen.
 * 2. Layer 2 (Overlay): Faded gradient scrim overlay for smooth contrast and depth.
 * 3. Layer 3 (Foreground): "Unable to Load Rooms" title, dynamic failure reason details, and "Retry Connection" pill button.
 */
@Composable
fun NoInternetScreen(
    repository: ChatRepository,
    onRetrySuccess: () -> Unit,
    reason: String? = null,
    sessionStorage: SessionStorage? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isRetrying by remember { mutableStateOf(false) }
    var currentReason by remember(reason) { mutableStateOf(reason) }
    var showCustomUrlDialog by remember { mutableStateOf(false) }
    var customUrlInput by remember { mutableStateOf(sessionStorage?.getCustomBackendUrl() ?: "") }

    // Sequential entrance animation states
    val wifiAlpha = remember { Animatable(0f) }
    val wifiScale = remember { Animatable(0.96f) }
    val titleAlpha = remember { Animatable(0f) }
    val descAlpha = remember { Animatable(0f) }
    val buttonAlpha = remember { Animatable(0f) }
    val buttonOffsetY = remember { Animatable(20f) }

    LaunchedEffect(Unit) {
        // Step 1: Wi-Fi symbol entrance (fade + scale)
        launch {
            wifiAlpha.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
        }
        launch {
            wifiScale.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        }

        // Step 2: Title entrance
        launch {
            kotlinx.coroutines.delay(180)
            titleAlpha.animateTo(1f, tween(350, easing = FastOutSlowInEasing))
        }

        // Step 3: Description / Reason entrance
        launch {
            kotlinx.coroutines.delay(320)
            descAlpha.animateTo(1f, tween(350, easing = FastOutSlowInEasing))
        }

        // Step 4: Retry button entrance
        launch {
            kotlinx.coroutines.delay(450)
            buttonOffsetY.animateTo(0f, tween(400, easing = FastOutSlowInEasing))
        }
        launch {
            kotlinx.coroutines.delay(450)
            buttonAlpha.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
        }
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        StartupNoInternetScreen(
            onRetry = {
                if (!isRetrying) {
                    isRetrying = true
                    coroutineScope.launch {
                        repository.connect()
                        val result = repository.loadPublicRooms()
                        if (result.isSuccess) {
                            onRetrySuccess()
                        } else {
                            currentReason = result.exceptionOrNull()?.message ?: "Unable to connect"
                            isRetrying = false
                        }
                    }
                }
            },
            onConfigureServer = {
                showCustomUrlDialog = true
            },
            isRetrying = isRetrying
        )
    }

    // ── Dialog for Custom Backend URL Input ──────────────────────────────────
    if (showCustomUrlDialog && sessionStorage != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCustomUrlDialog = false },
            title = {
                Text(
                    text = "Backend Tunnel URL",
                    fontWeight = FontWeight.Bold,
                    color = AnonChatColors.TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AnonChatSpacing.sm)) {
                    Text(
                        text = "If your Wi-Fi blocks afterclass.bond, enter the active backend URL directly (e.g. https://afterclass.bond):",
                        fontSize = 12.sp,
                        color = AnonChatColors.TextSecondary
                    )

                    androidx.compose.material3.TextField(
                        value = customUrlInput,
                        onValueChange = { customUrlInput = it },
                        placeholder = {
                            Text("https://xxxx.trycloudflare.com", color = AnonChatColors.TextMuted, fontSize = 12.sp)
                        },
                        singleLine = true,
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = AnonChatColors.SurfaceTertiary,
                            unfocusedContainerColor = AnonChatColors.SurfaceTertiary,
                            focusedIndicatorColor = AnonChatColors.Primary,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        val input = customUrlInput.trim()
                        sessionStorage.setCustomBackendUrl(input.ifBlank { null })
                        showCustomUrlDialog = false
                        isRetrying = true
                        coroutineScope.launch {
                            repository.disconnect()
                            repository.connect()
                            val result = repository.loadPublicRooms()
                            if (result.isSuccess) {
                                onRetrySuccess()
                            } else {
                                currentReason = result.exceptionOrNull()?.message ?: "Unable to reach custom URL"
                                isRetrying = false
                            }
                        }
                    }
                ) {
                    Text("Connect", color = AnonChatColors.Primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        sessionStorage.setCustomBackendUrl(null)
                        customUrlInput = ""
                        showCustomUrlDialog = false
                    }
                ) {
                    Text("Reset Auto", color = AnonChatColors.TextSecondary)
                }
            },
            containerColor = AnonChatColors.SurfacePrimary
        )
    }
}

/**
 * Custom Canvas drawing representing the full-screen expansive Wi-Fi arcs radiating from the
 * bottom-left across the entire screen, moved upwards for prominent framing.
 */
@Composable
private fun FullScreenWifiArtwork(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Origin of Wi-Fi concentric waves moved downwards by 11dp
        val originX = w * 0.08f
        val originY = h * 0.48f + 11.dp.toPx()

        // Smooth radial gradient fading from pure white at origin to silver/gray/transparent
        val waveBrush = Brush.radialGradient(
            colors = listOf(
                Color.White,
                Color(0xFFE4E4E7),
                Color(0xFFA1A1AA),
                Color(0xFF71717A),
                Color(0xFF3F3F46),
                Color(0xFF18181B),
                Color.Transparent
            ),
            center = Offset(originX, originY),
            radius = w * 1.60f
        )

        // 1. Bottom-Left Dot
        drawCircle(
            brush = waveBrush,
            radius = 34.dp.toPx(),
            center = Offset(originX, originY)
        )

        // 2. Inner Wave Arc
        val r1 = 85.dp.toPx()
        val s1 = 34.dp.toPx()
        drawArc(
            brush = waveBrush,
            startAngle = -86f,
            sweepAngle = 82f,
            useCenter = false,
            topLeft = Offset(originX - r1, originY - r1),
            size = Size(r1 * 2, r1 * 2),
            style = Stroke(width = s1, cap = StrokeCap.Round)
        )

        // 3. Middle Wave Arc 1
        val r2 = 160.dp.toPx()
        val s2 = 40.dp.toPx()
        drawArc(
            brush = waveBrush,
            startAngle = -88f,
            sweepAngle = 84f,
            useCenter = false,
            topLeft = Offset(originX - r2, originY - r2),
            size = Size(r2 * 2, r2 * 2),
            style = Stroke(width = s2, cap = StrokeCap.Round)
        )

        // 4. Middle Wave Arc 2
        val r3 = 245.dp.toPx()
        val s3 = 46.dp.toPx()
        drawArc(
            brush = waveBrush,
            startAngle = -90f,
            sweepAngle = 86f,
            useCenter = false,
            topLeft = Offset(originX - r3, originY - r3),
            size = Size(r3 * 2, r3 * 2),
            style = Stroke(width = s3, cap = StrokeCap.Round)
        )

        // 5. Outer Wave Arc (Sweeps across the full screen width & height)
        val r4 = 340.dp.toPx()
        val s4 = 52.dp.toPx()
        drawArc(
            brush = waveBrush,
            startAngle = -90f,
            sweepAngle = 88f,
            useCenter = false,
            topLeft = Offset(originX - r4, originY - r4),
            size = Size(r4 * 2, r4 * 2),
            style = Stroke(width = s4, cap = StrokeCap.Round)
        )
    }
}

