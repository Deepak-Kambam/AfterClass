package org.example.annochat.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import org.example.annochat.data.repository.ChatRepository
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.theme.LocalAppearanceSettings

enum class NavTab(
    val title: String,
    val icon: ImageVector
) {
    CHATS("Chats", AnonChatIcons.ChatBubbles),
    ONLINE("People", AnonChatIcons.Users),
    PROFILE("Profile", AnonChatIcons.PersonCircle)
}

/**
 * Premium Floating Capsuled Bottom Navigation Bar with illuminated dome and bottom glow bar.
 *
 * Exact visual match to screenshot:
 * - Sizing: 272dp width x 64dp height capsule pill (RoundedCornerShape(32.dp)).
 * - Active tab: Illuminated glossy dome with top radial specular sheen + crisp bottom luminous white indicator line.
 * - Tab items: Icon/Avatar with unread badge + Text label ("Chats", "People", "Profile").
 * - Fluid ease-in ease-out sliding animation between tabs.
 */
@Composable
fun CapsuleBottomNavBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
    isRotatingGlowEnabled: Boolean = true,
    repository: ChatRepository? = null,
    profileAvatarUrl: String? = null,
    profileAnimalName: String? = null,
    profilePublicId: String? = null,
    chatsBadgeCount: Int? = null,
    contactsBadgeCount: Int? = null
) {
    val appearance = LocalAppearanceSettings.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Floating Outer Pill Container with Translucent Glass Backing & Drop Shadow
        Box(
            modifier = Modifier
                .width(204.dp)
                .height(46.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(23.dp),
                    clip = false,
                    spotColor = Color.Black.copy(alpha = 0.85f),
                    ambientColor = Color.Black.copy(alpha = 0.60f)
                )
                .clip(RoundedCornerShape(23.dp))
                .background(Color(0xFF141416).copy(alpha = 0.92f)) // Translucent obsidian glass backing
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.20f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    ),
                    shape = RoundedCornerShape(23.dp)
                )
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 2.dp, vertical = 2.dp)
            ) {
                val tabCount = NavTab.entries.size
                val itemWidth = maxWidth / tabCount

                // Smooth fluid sliding position with strictly ease-in, ease-out easing
                val targetIndex = selectedTab.ordinal.toFloat()
                val animatedIndex by animateFloatAsState(
                    targetValue = targetIndex,
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = FastOutSlowInEasing
                    ),
                    label = "activeIndicatorPosition"
                )

                // Elastic elongation: subtle stretch during transit
                val diff = abs(targetIndex - animatedIndex)
                val stretchFactor = 1f + (diff * 0.16f).coerceAtMost(0.20f)
                val basePillWidth = itemWidth - 3.dp
                val dynamicWidth = (basePillWidth.value * stretchFactor).dp.coerceAtMost((itemWidth.value * 1.20f).dp)
                val targetCenterX = ((animatedIndex + 0.5f) * itemWidth.value).dp
                val pillLeft = targetCenterX - (dynamicWidth / 2)

                // ─────────────────────────────────────────────────────────────
                // Active Tab Background: Illuminated Glossy Dome & Bottom Glow Line
                // ─────────────────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .offset { IntOffset(pillLeft.roundToPx(), 0) }
                        .width(dynamicWidth)
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.22f),
                                    Color.White.copy(alpha = 0.08f),
                                    Color.White.copy(alpha = 0.02f)
                                )
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(21.dp))
                ) {
                    // Top ambient specular sheen
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.25f),
                                        Color.White.copy(alpha = 0.05f),
                                        Color.Transparent
                                    ),
                                    center = Offset(140f, 0f),
                                    radius = 200f
                                )
                            )
                    )

                    // Bottom ambient glow halo
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 1.dp)
                            .size(width = 26.dp, height = 7.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.80f),
                                        Color.White.copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Crisp bottom white indicator line bar
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 1.5.dp)
                            .size(width = 16.dp, height = 1.5.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Color.White)
                    )
                }

                // ─────────────────────────────────────────────────────────────
                // Tab Items Layer: Icon + Badge + Text Label
                // ─────────────────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

                    NavTab.entries.forEach { tab ->
                        val isSelected = tab == selectedTab
                        val badge = when (tab) {
                            NavTab.CHATS -> chatsBadgeCount
                            NavTab.ONLINE -> contactsBadgeCount
                            else -> null
                        }

                        CapsuleNavItem(
                            tab = tab,
                            isSelected = isSelected,
                            badgeCount = badge,
                            repository = repository,
                            profileAvatarUrl = profileAvatarUrl,
                            profileAnimalName = profileAnimalName,
                            profilePublicId = profilePublicId,
                            onClick = {
                                if (tab != selectedTab) {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                }
                                onTabSelected(tab)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CapsuleNavItem(
    tab: NavTab,
    isSelected: Boolean,
    badgeCount: Int?,
    repository: ChatRepository? = null,
    profileAvatarUrl: String? = null,
    profileAnimalName: String? = null,
    profilePublicId: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color(0xFF8E8E93),
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "navContentColor"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.04f else 0.98f,
        animationSpec = tween(
            durationMillis = 260,
            easing = FastOutSlowInEasing
        ),
        label = "navScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else 0.68f,
        animationSpec = tween(
            durationMillis = 220,
            easing = FastOutSlowInEasing
        ),
        label = "navAlpha"
    )

    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(21.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.15f)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .scale(scale)
                .alpha(alpha)
                .padding(bottom = 1.5.dp), // Optical centering above the bottom glow line
            contentAlignment = Alignment.Center
        ) {
            // Icon or Avatar with Badge
            Box(contentAlignment = Alignment.TopEnd) {
                if (tab == NavTab.PROFILE) {
                    if (repository != null) {
                        AnimalAvatar(
                            repository = repository,
                            avatarUrl = profileAvatarUrl,
                            animalName = profileAnimalName,
                            publicId = profilePublicId,
                            size = 20.dp,
                            borderWidth = if (isSelected) 1.5.dp else 1.dp,
                            borderColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.40f),
                            forceAnimalImage = true,
                            enableLivingIdleAnimation = isSelected,
                            enableInteractionBounce = false,
                            fallbackIcon = AnonChatIcons.Paw
                        )
                    } else {
                        val slug = resolveAnimalSlug(profileAvatarUrl, profileAnimalName, profilePublicId)
                        val bundledRes = remember(slug) { getBundledAnimalResource(slug) }
                        Image(
                            painter = painterResource(bundledRes),
                            contentDescription = tab.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.40f),
                                    shape = CircleShape
                                )
                        )
                    }
                } else {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Notification Badge: white circle with black count text
                if (badgeCount != null && badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(6.dp.roundToPx(), (-3).dp.roundToPx()) }
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color(0xFF141416), CircleShape)
                            .padding(horizontal = 3.dp, vertical = 0.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            lineHeight = 8.5.sp
                        )
                    }
                }
            }
        }
    }
}
