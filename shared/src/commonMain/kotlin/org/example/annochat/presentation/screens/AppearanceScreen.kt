package org.example.annochat.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import org.example.annochat.data.model.AfterClassAccent
import org.example.annochat.data.model.AfterClassMode
import org.example.annochat.data.model.AppTextSize
import org.example.annochat.data.model.AppearanceSettings
import org.example.annochat.data.model.CampusEnvironment
import org.example.annochat.data.repository.ChatRepository
import org.example.annochat.data.session.SessionStorage
import org.example.annochat.presentation.components.AnimalAvatar
import org.example.annochat.presentation.components.AfterClassSlider
import org.example.annochat.presentation.components.AfterClassSwitch
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.designsystem.AnonChatShapes
import org.example.annochat.presentation.designsystem.AnonChatSpacing

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    sessionStorage: SessionStorage,
    repository: ChatRepository,
    onBack: () -> Unit,
    onSettingsChanged: (AppearanceSettings) -> Unit,
    modifier: Modifier = Modifier,
    profileAnimalName: String? = null,
    profilePublicId: String? = null,
    profileAvatarUrl: String? = null
) {
    var settings by remember { mutableStateOf(sessionStorage.getAppearanceSettings()) }
    var showResetDialog by remember { mutableStateOf(false) }

    fun updateSettings(newSettings: AppearanceSettings) {
        settings = newSettings
        sessionStorage.saveAppearanceSettings(newSettings)
        onSettingsChanged(newSettings)
    }

    val activeAccentColor = Color(settings.accent.colorHex)
    val activeOnAccentColor = if (isLightColor(activeAccentColor)) Color.Black else Color.White

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AnonChatSpacing.md, vertical = AnonChatSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AnonChatColors.SurfaceSecondary)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = AnonChatColors.Primary),
                            onClick = onBack
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AnonChatIcons.ChevronLeft,
                        contentDescription = "Back",
                        tint = AnonChatColors.TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(AnonChatSpacing.md))

                Text(
                    text = "Appearance",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AnonChatColors.TextPrimary
                )
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (showResetDialog) {
                            alpha = 0.35f
                            scaleX = 0.98f
                            scaleY = 0.98f
                        }
                    }
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AnonChatSpacing.lg)
                    .navigationBarsPadding()
            ) {
                Spacer(modifier = Modifier.height(AnonChatSpacing.xs))

                // ── SECTION 1: AFTERCLASS MODES ───────────────────────────────
                SectionHeader(
                    title = "AFTERCLASS MODES",
                    subtitle = "Instant atmospheric presets"
                )

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AfterClassMode.entries.forEach { mode ->
                        val isSelected = settings.activeMode == mode
                        ModePresetCard(
                            mode = mode,
                            isSelected = isSelected,
                            accentColor = activeAccentColor,
                            onClick = {
                                val preset = AppearanceSettings.forMode(mode)
                                updateSettings(preset)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ── SECTION 2: CAMPUS ENVIRONMENT ─────────────────────────────
                SectionHeader(
                    title = "CAMPUS ENVIRONMENT",
                    subtitle = "Lighting & background atmosphere"
                )

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 2
                ) {
                    CampusEnvironment.entries.forEach { env ->
                        val isSelected = settings.environment == env
                        EnvironmentCard(
                            environment = env,
                            isSelected = isSelected,
                            accentColor = activeAccentColor,
                            onClick = {
                                updateSettings(settings.copy(environment = env))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Background Intensity Slider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(AnonChatColors.SurfacePrimary)
                        .border(1.dp, AnonChatColors.Border, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Background Intensity",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AnonChatColors.TextPrimary
                            )
                            Text(
                                text = "${(settings.environmentIntensity * 100).toInt()}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeAccentColor
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        AfterClassSlider(
                            value = settings.environmentIntensity,
                            onValueChange = { updateSettings(settings.copy(environmentIntensity = it)) },
                            valueRange = 0.0f..1.0f,
                            activeColor = activeAccentColor
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Subtle", fontSize = 11.sp, color = AnonChatColors.TextMuted)
                            Text(text = "Immersive", fontSize = 11.sp, color = AnonChatColors.TextMuted)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ── SECTION 3: AFTERCLASS ACCENT ──────────────────────────────
                SectionHeader(
                    title = "AFTERCLASS ACCENT",
                    subtitle = "Signature highlight colors across the interface"
                )

                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 3
                ) {
                    AfterClassAccent.entries.forEach { accent ->
                        val isSelected = settings.accent == accent
                        AccentSwatchCard(
                            accent = accent,
                            isSelected = isSelected,
                            onClick = {
                                updateSettings(settings.copy(accent = accent))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ── SECTION 4: MOTION & ANIMATIONS ───────────────────────────
                SectionHeader(
                    title = "MOTION & ANIMATIONS",
                    subtitle = "App transitions and interactive animation effects"
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(AnonChatColors.SurfacePrimary)
                        .border(1.dp, AnonChatColors.Border, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    // Global Animations Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Motion & Animations",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AnonChatColors.TextPrimary
                            )
                            Text(
                                text = "Transitions, avatar physics & particle sweeps",
                                fontSize = 12.sp,
                                color = AnonChatColors.TextSecondary
                            )
                        }

                        AfterClassSwitch(
                            checked = settings.animationsEnabled,
                            onCheckedChange = { updateSettings(settings.copy(animationsEnabled = it)) },
                            activeColor = activeAccentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ── SECTION 5: TYPOGRAPHY & SCALE (Draggable Scale System) ───
                SectionHeader(
                    title = "TYPOGRAPHY & SCALE",
                    subtitle = "App text sizing preference"
                )

                Spacer(modifier = Modifier.height(12.dp))

                val currentStep = when (settings.textSize) {
                    AppTextSize.COMPACT -> 0f
                    AppTextSize.DEFAULT -> 1f
                    AppTextSize.LARGE -> 2f
                }
                var sliderPosition by remember(settings.textSize) { mutableStateOf(currentStep) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(AnonChatColors.SurfacePrimary)
                        .border(1.dp, AnonChatColors.Border, RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Column {
                        // Header Row with live size badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Text Size Scale",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AnonChatColors.TextPrimary
                            )

                            // Dynamic live badge showing current selection
                            Box(
                                modifier = Modifier
                                    .clip(AnonChatShapes.pill)
                                    .background(activeAccentColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${settings.textSize.title} · ${settings.textSize.subtitle}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = activeAccentColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Draggable Slider Row with A typography glyphs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Small A (Compact)
                            Text(
                                text = "A",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (settings.textSize == AppTextSize.COMPACT) activeAccentColor else AnonChatColors.TextMuted,
                                modifier = Modifier
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { updateSettings(settings.copy(textSize = AppTextSize.COMPACT)) }
                                    )
                                    .padding(horizontal = 4.dp, vertical = 6.dp)
                            )

                            // Draggable Slider Track with tactile snapping
                            Box(modifier = Modifier.weight(1f)) {
                                Slider(
                                    value = sliderPosition,
                                    onValueChange = { newPos ->
                                        sliderPosition = newPos
                                        val snapped = when (newPos.roundToInt().coerceIn(0, 2)) {
                                            0 -> AppTextSize.COMPACT
                                            1 -> AppTextSize.DEFAULT
                                            else -> AppTextSize.LARGE
                                        }
                                        if (snapped != settings.textSize) {
                                            updateSettings(settings.copy(textSize = snapped))
                                        }
                                    },
                                    valueRange = 0f..2f,
                                    steps = 1,
                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                    thumb = {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .shadow(elevation = 4.dp, shape = CircleShape)
                                                .clip(CircleShape)
                                                .background(if (isLightColor(activeAccentColor)) Color.White else activeAccentColor)
                                                .border(
                                                    width = 1.5.dp,
                                                    color = if (isLightColor(activeAccentColor)) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.90f),
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isLightColor(activeAccentColor)) Color.Black.copy(alpha = 0.6f) else Color.White)
                                            )
                                        }
                                    },
                                    track = { sliderState ->
                                        SliderDefaults.Track(
                                            sliderState = sliderState,
                                            colors = SliderDefaults.colors(
                                                activeTrackColor = if (isLightColor(activeAccentColor)) Color.White else activeAccentColor,
                                                inactiveTrackColor = AnonChatColors.SurfaceTertiary
                                            ),
                                            modifier = Modifier.height(6.dp),
                                            drawStopIndicator = null
                                        )
                                    }
                                )
                            }

                            // Large A (Large)
                            Text(
                                text = "A",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (settings.textSize == AppTextSize.LARGE) activeAccentColor else AnonChatColors.TextMuted,
                                modifier = Modifier
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { updateSettings(settings.copy(textSize = AppTextSize.LARGE)) }
                                    )
                                    .padding(horizontal = 4.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Step Notch Labels
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 26.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Compact",
                                fontSize = 11.5.sp,
                                fontWeight = if (settings.textSize == AppTextSize.COMPACT) FontWeight.Bold else FontWeight.Normal,
                                color = if (settings.textSize == AppTextSize.COMPACT) AnonChatColors.TextPrimary else AnonChatColors.TextMuted,
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { updateSettings(settings.copy(textSize = AppTextSize.COMPACT)) }
                                )
                            )
                            Text(
                                text = "Default",
                                fontSize = 11.5.sp,
                                fontWeight = if (settings.textSize == AppTextSize.DEFAULT) FontWeight.Bold else FontWeight.Normal,
                                color = if (settings.textSize == AppTextSize.DEFAULT) AnonChatColors.TextPrimary else AnonChatColors.TextMuted,
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { updateSettings(settings.copy(textSize = AppTextSize.DEFAULT)) }
                                )
                            )
                            Text(
                                text = "Large",
                                fontSize = 11.5.sp,
                                fontWeight = if (settings.textSize == AppTextSize.LARGE) FontWeight.Bold else FontWeight.Normal,
                                color = if (settings.textSize == AppTextSize.LARGE) AnonChatColors.TextPrimary else AnonChatColors.TextMuted,
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { updateSettings(settings.copy(textSize = AppTextSize.LARGE)) }
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                // ── SECTION 7: RESTORE DEFAULT EXPERIENCE (Capsule) ───────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AnonChatShapes.pill)
                        .background(AnonChatColors.SurfacePrimary)
                        .border(1.dp, AnonChatColors.Border, AnonChatShapes.pill)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = AnonChatColors.Error),
                            onClick = { showResetDialog = true }
                        )
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = AnonChatIcons.RotateLeft,
                            contentDescription = "Restore",
                            tint = AnonChatColors.Error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Restore Default Experience",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AnonChatColors.Error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Confirmation Dialog for Restore Default Experience
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = AnonChatColors.SurfaceElevated,
            title = {
                Text(
                    text = "Restore Default Appearance?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AnonChatColors.TextPrimary
                )
            },
            text = {
                Text(
                    text = "This will reset all theme modes, colors, and motion pacing back to After Hours defaults. Your anonymous identity, chats, and keys will remain untouched.",
                    fontSize = 14.sp,
                    color = AnonChatColors.TextSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(AnonChatShapes.pill)
                        .background(AnonChatColors.Error.copy(alpha = 0.15f))
                        .clickable {
                            sessionStorage.restoreDefaultAppearance()
                            val def = AppearanceSettings()
                            settings = def
                            onSettingsChanged(def)
                            showResetDialog = false
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Reset Appearance",
                        fontWeight = FontWeight.Bold,
                        color = AnonChatColors.Error,
                        fontSize = 13.sp
                    )
                }
            },
            dismissButton = {
                Box(
                    modifier = Modifier
                        .clip(AnonChatShapes.pill)
                        .clickable { showResetDialog = false }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Cancel",
                        color = AnonChatColors.TextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String
) {
    Column {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = AnonChatColors.TextMuted,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = AnonChatColors.TextSecondary
        )
    }
}

@Composable
private fun ModePresetCard(
    mode: AfterClassMode,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val onAccentColor = if (isLightColor(accentColor)) Color.Black else Color.White
    val borderColor = if (isSelected) accentColor else AnonChatColors.Border
    val bg = if (isSelected) accentColor else AnonChatColors.SurfacePrimary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AnonChatShapes.pill)
            .background(bg)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, AnonChatShapes.pill)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = accentColor),
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = mode.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) onAccentColor else AnonChatColors.TextPrimary
                    )
                    if (mode == AfterClassMode.AFTER_HOURS) {
                        Box(
                            modifier = Modifier
                                .clip(AnonChatShapes.pill)
                                .background(if (isSelected) onAccentColor.copy(alpha = 0.15f) else AnonChatColors.SurfaceTertiary)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "DEFAULT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) onAccentColor else AnonChatColors.TextMuted
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = mode.subtitle,
                    fontSize = 12.sp,
                    color = if (isSelected) onAccentColor.copy(alpha = 0.70f) else AnonChatColors.TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) onAccentColor else Color.Transparent)
                    .border(1.5.dp, if (isSelected) onAccentColor else AnonChatColors.Border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = AnonChatIcons.Check,
                        contentDescription = "Selected",
                        tint = if (onAccentColor == Color.Black) Color.White else Color.Black,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EnvironmentCard(
    environment: CampusEnvironment,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onAccentColor = if (isLightColor(accentColor)) Color.Black else Color.White
    val borderColor = if (isSelected) accentColor else AnonChatColors.Border
    val bg = if (isSelected) accentColor else AnonChatColors.SurfacePrimary

    Box(
        modifier = modifier
            .clip(AnonChatShapes.pill)
            .background(bg)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, AnonChatShapes.pill)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = accentColor),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = environment.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) onAccentColor else AnonChatColors.TextPrimary,
                    maxLines = 1
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(onAccentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AnonChatIcons.Check,
                            contentDescription = "Selected",
                            tint = if (onAccentColor == Color.Black) Color.White else Color.Black,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
            Text(
                text = environment.subtitle,
                fontSize = 11.sp,
                color = if (isSelected) onAccentColor.copy(alpha = 0.70f) else AnonChatColors.TextMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AccentSwatchCard(
    accent: AfterClassAccent,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val swatchColor = Color(accent.colorHex)
    val onSwatchColor = if (isLightColor(swatchColor)) Color.Black else Color.White
    val borderColor = if (isSelected) swatchColor else AnonChatColors.Border
    val bg = if (isSelected) swatchColor else AnonChatColors.SurfacePrimary

    Box(
        modifier = modifier
            .height(76.dp)
            .clip(AnonChatShapes.pill)
            .background(bg)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, AnonChatShapes.pill)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = swatchColor),
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) onSwatchColor.copy(alpha = 0.15f) else swatchColor)
                    .border(
                        1.dp,
                        if (isSelected) onSwatchColor.copy(alpha = 0.35f) else AnonChatColors.Border,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = AnonChatIcons.Check,
                        contentDescription = "Selected",
                        tint = onSwatchColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = accent.title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) onSwatchColor else AnonChatColors.TextPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SegmentedChip(
    title: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val onAccentColor = if (isLightColor(accentColor)) Color.Black else Color.White
    val borderColor = if (isSelected) accentColor else AnonChatColors.Border
    val bg = if (isSelected) accentColor else AnonChatColors.SurfacePrimary

    Box(
        modifier = modifier
            .clip(AnonChatShapes.pill)
            .background(bg)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, AnonChatShapes.pill)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = accentColor),
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) onAccentColor else AnonChatColors.TextPrimary,
                maxLines = 1
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = if (isSelected) onAccentColor.copy(alpha = 0.70f) else AnonChatColors.TextMuted,
                    maxLines = 1
                )
            }
        }
    }
}

private fun isLightColor(color: Color): Boolean {
    val luminance = 0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue
    return luminance > 0.45f
}
