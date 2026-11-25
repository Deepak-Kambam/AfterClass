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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.annochat.data.model.WIDGET_VIBE_QUOTES
import org.example.annochat.data.repository.ChatRepository
import org.example.annochat.data.session.SessionStorage
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.theme.LocalAppearanceSettings
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.designsystem.AnonChatShapes
import org.example.annochat.presentation.designsystem.AnonChatSpacing
import org.example.annochat.presentation.designsystem.AnonChatTypography

@Composable
fun WidgetSettingsModal(
    sessionStorage: SessionStorage,
    repository: ChatRepository,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val onlineCount by repository.onlineCount.collectAsState()
    val identity by repository.myIdentity.collectAsState()
    val userRooms by repository.userRooms.collectAsState()
    val publicRooms by repository.publicRooms.collectAsState()

    var showOnlineCount by remember { mutableStateOf(sessionStorage.isWidgetOnlineCountEnabled()) }
    var showIdentity by remember { mutableStateOf(sessionStorage.isWidgetIdentityEnabled()) }
    var updateInterval by remember { mutableStateOf(sessionStorage.getWidgetUpdateIntervalMinutes()) }

    var syncSuccessFeedback by remember { mutableStateOf(false) }
    var showInstructions by remember { mutableStateOf(false) }

    val appearance = LocalAppearanceSettings.current
    val accentColor = Color(appearance.accent.colorHex)
    val isLight = (0.2126f * accentColor.red + 0.7152f * accentColor.green + 0.0722f * accentColor.blue) > 0.45f
    val onAccentColor = if (isLight) Color.Black else Color.White

    val effectiveAnonName = identity?.anonName ?: sessionStorage.getSavedAnonName() ?: "Moon Fox"
    val effectiveTag = (identity?.publicId ?: sessionStorage.getSavedPublicId() ?: "8A21").take(4).uppercase()
    val activeRoomName = userRooms.maxByOrNull { it.messageCount }?.name
        ?: publicRooms.firstOrNull()?.name
        ?: "General · Late Night"

    fun persistAndSync() {
        sessionStorage.setWidgetOnlineCountEnabled(showOnlineCount)
        sessionStorage.setWidgetIdentityEnabled(showIdentity)
        sessionStorage.setWidgetUpdateIntervalMinutes(updateInterval)
        repository.syncWidgets()
    }

    ProfileModalWrapper(
        title = "Home Screen Widgets",
        icon = AnonChatIcons.Grid,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AnonChatSpacing.md)
        ) {
            Text(
                text = "Glance at live campus activity, nocturnal quotes, and your anonymous persona directly from your home screen.",
                style = AnonChatTypography.Body,
                color = AnonChatColors.TextSecondary,
                fontSize = 13.sp
            )

            // ── Widget Previews Section ──────────────────────────────────
            Text(
                text = "AVAILABLE WIDGETS",
                style = AnonChatTypography.Caption,
                color = AnonChatColors.TextMuted,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )

            // Widget 1: Small Time & Vibe (2x2)
            WidgetPreviewContainer(title = "Small • Time + Brand + Online Count (2x2)") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AfterClass",
                            style = AnonChatTypography.ScreenTitle,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "🌙",
                            fontSize = 18.sp
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "11:47",
                            style = AnonChatTypography.ScreenTitle,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "PM",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "“Campus never sleeps.”",
                        style = AnonChatTypography.Caption,
                        color = Color(0xFFA0A0AB),
                        fontSize = 12.sp,
                        maxLines = 1
                    )

                    Spacer(Modifier.height(12.dp))

                    // Bottom Capsule: [ 👥 312 online ]
                    Box(
                        modifier = Modifier
                            .clip(AnonChatShapes.pill)
                            .background(Color(0xFF161619))
                            .border(1.dp, Color(0xFF28282D), AnonChatShapes.pill)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = AnonChatIcons.Users,
                                contentDescription = null,
                                tint = Color(0xFF888892),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (onlineCount > 0) "$onlineCount" else "312",
                                style = AnonChatTypography.Caption,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "online",
                                style = AnonChatTypography.Caption,
                                color = Color(0xFF888892),
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
            }

            // Widget 2: Medium Live Activity (4x2)
            WidgetPreviewContainer(title = "Medium • Live Activity + Identity + Quick Action (4x2)") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AfterClass",
                            style = AnonChatTypography.ScreenTitle,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Live campus. Stay anonymous.",
                            style = AnonChatTypography.Caption,
                            color = Color(0xFF80808C),
                            fontSize = 11.5.sp
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Left Sub-card: Live Activity
                        Column(
                            modifier = Modifier
                                .weight(1.15f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF131316))
                                .border(1.dp, Color(0xFF242429), RoundedCornerShape(14.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = AnonChatIcons.Users,
                                    contentDescription = null,
                                    tint = Color(0xFF888892),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = if (onlineCount > 0) "$onlineCount" else "312",
                                    style = AnonChatTypography.Label,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "online now",
                                    style = AnonChatTypography.Caption,
                                    color = Color(0xFF888892),
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            // 14-bar Activity Histogram
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                val heights = listOf(0.25f, 0.35f, 0.45f, 0.75f, 1.0f, 0.65f, 0.4f, 0.3f, 0.35f, 0.55f, 0.8f, 1.0f, 0.7f, 0.5f)
                                heights.forEach { frac ->
                                    Box(
                                        modifier = Modifier
                                            .width(4.5.dp)
                                            .height((20 * frac).dp)
                                            .clip(RoundedCornerShape(1.5.dp))
                                            .background(Color.White.copy(alpha = 0.2f + (frac * 0.8f)))
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = "Most active: #${activeRoomName.removePrefix("#").removePrefix("/").removePrefix("anon/").trim()}",
                                style = AnonChatTypography.Caption,
                                color = Color(0xFFA0A0AA),
                                fontSize = 10.5.sp,
                                maxLines = 1
                            )
                        }

                        // Right Sub-card: Identity + Quick Action
                        Box(
                            modifier = Modifier
                                .weight(0.85f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF131316))
                                .border(1.dp, Color(0xFF242429), RoundedCornerShape(14.dp))
                                .padding(8.dp)
                        ) {
                            // Forward Action Button
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.TopEnd)
                                    .clip(CircleShape)
                                    .background(Color(0xFF222227))
                                    .border(1.dp, Color(0xFF36363E), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(">", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🦊", fontSize = 28.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = effectiveAnonName.substringBefore("#").trim(),
                                    style = AnonChatTypography.Label,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = "#$effectiveTag",
                                    style = AnonChatTypography.Caption,
                                    color = Color(0xFF787884),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // ── Privacy & Display Controls ────────────────────────────────
            Spacer(Modifier.height(4.dp))
            Text(
                text = "WIDGET PREFERENCES",
                style = AnonChatTypography.Caption,
                color = AnonChatColors.TextMuted,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )

            WidgetToggleOption(
                title = "Show Live Online Count",
                subtitle = "Display real-time active users on campus",
                checked = showOnlineCount,
                onCheckedChange = {
                    showOnlineCount = it
                    persistAndSync()
                }
            )

            WidgetToggleOption(
                title = "Show Anonymous Identity",
                subtitle = "Display your animal persona and tag on the identity widget",
                checked = showIdentity,
                onCheckedChange = {
                    showIdentity = it
                    persistAndSync()
                }
            )

            // Update Frequency Selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF111111))
                    .border(1.dp, AnonChatColors.BorderSubtle, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "Background Update Interval",
                    style = AnonChatTypography.Label,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
                Text(
                    text = "Controls how often the widget requests timeline updates",
                    style = AnonChatTypography.Caption,
                    color = AnonChatColors.TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15 to "15m", 30 to "30m (Default)", 60 to "60m").forEach { (mins, label) ->
                        val selected = updateInterval == mins
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(AnonChatShapes.pill)
                                .background(if (selected) accentColor else Color(0xFF1E1E1E))
                                .border(
                                    1.dp,
                                    if (selected) accentColor else AnonChatColors.BorderSubtle,
                                    AnonChatShapes.pill
                                )
                                .clickable {
                                    updateInterval = mins
                                    persistAndSync()
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (selected) onAccentColor else AnonChatColors.TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // ── Force Sync Button ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AnonChatShapes.pill)
                    .background(if (syncSuccessFeedback) Color(0xFF222222) else accentColor)
                    .clickable {
                        persistAndSync()
                        syncSuccessFeedback = true
                        coroutineScope.launch {
                            delay(1800)
                            syncSuccessFeedback = false
                        }
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (syncSuccessFeedback) "✓ Widgets Synchronized" else "Sync Widgets Now",
                    color = if (syncSuccessFeedback) Color.White else onAccentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // ── Instructions Accordion ────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showInstructions = !showInstructions }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "How to add widgets to Home Screen",
                    style = AnonChatTypography.Caption,
                    color = AnonChatColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (showInstructions) "▲" else "▼",
                    color = AnonChatColors.TextMuted,
                    fontSize = 10.sp
                )
            }

            AnimatedVisibility(
                visible = showInstructions,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F0F0F))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "• Android: Touch & hold an empty space on your home screen -> Tap 'Widgets' -> Search 'AfterClass' -> Drag widget to placement.",
                        style = AnonChatTypography.Caption,
                        color = AnonChatColors.TextSecondary,
                        fontSize = 11.5.sp
                    )
                    Text(
                        text = "• iOS: Touch & hold home screen until apps jiggle -> Tap '+' in upper corner -> Search 'AfterClass' -> Choose size & tap 'Add Widget'.",
                        style = AnonChatTypography.Caption,
                        color = AnonChatColors.TextSecondary,
                        fontSize = 11.5.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun WidgetPreviewContainer(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = AnonChatTypography.Caption,
            color = AnonChatColors.TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF090909))
                .border(1.dp, Color(0xFF222222), RoundedCornerShape(18.dp))
        ) {
            content()
        }
    }
}

@Composable
private fun WidgetToggleOption(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF111111))
            .border(1.dp, AnonChatColors.BorderSubtle, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = AnonChatTypography.Label,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
            Text(
                text = subtitle,
                style = AnonChatTypography.Caption,
                color = AnonChatColors.TextSecondary,
                fontSize = 11.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color.White,
                uncheckedThumbColor = AnonChatColors.TextMuted,
                uncheckedTrackColor = Color(0xFF222222),
                uncheckedBorderColor = Color(0xFF333333)
            )
        )
    }
}
