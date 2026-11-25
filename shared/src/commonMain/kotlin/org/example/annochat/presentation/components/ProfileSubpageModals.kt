package org.example.annochat.presentation.components

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinproject.shared.generated.resources.Res
import kotlinproject.shared.generated.resources.afterclass_mark
import kotlinx.coroutines.launch
import org.example.annochat.data.repository.ChatRepository
import org.example.annochat.data.session.SessionStorage
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.designsystem.AnonChatSpacing
import org.example.annochat.presentation.designsystem.AnonChatTypography
import org.jetbrains.compose.resources.painterResource

@Composable
fun ProfileModalWrapper(
    title: String,
    icon: ImageVector? = null,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
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
                modifier = Modifier
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
                            if (icon != null) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(AnonChatColors.SurfaceSecondary)
                                        .border(1.dp, AnonChatColors.Border, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = AnonChatColors.Primary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                            Text(
                                text = title,
                                style = AnonChatTypography.Label,
                                color = AnonChatColors.TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(AnonChatColors.SurfaceSecondary)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AnonChatIcons.Close,
                                contentDescription = "Close",
                                tint = AnonChatColors.TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(AnonChatSpacing.md))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(AnonChatColors.BorderSubtle)
                    )
                    Spacer(Modifier.height(AnonChatSpacing.md))

                    // Modal Content
                    content()
                }
            }
        }
    }
}

@Composable
fun PrivacyCenterModal(onDismiss: () -> Unit) {
    ProfileModalWrapper(
        title = "Privacy Center",
        icon = AnonChatIcons.Lock,
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AnonChatSpacing.md)) {
            PrivacyItem(
                title = "Zero Identity Linking",
                description = "AfterClass never requests or links your real name, email, phone number, or student ID."
            )
            PrivacyItem(
                title = "Ephemeral Storage",
                description = "All keys, identities, and room memberships are tied to ephemeral session tokens that can be purged on command."
            )
            PrivacyItem(
                title = "Dynamic Masking",
                description = "Public identifiers and animal avatars are randomized per session to prevent cross-room correlation and profiling."
            )
            PrivacyItem(
                title = "Encrypted In-Flight Transport",
                description = "All messages and media transit through TLS 1.3 WebSocket streams via Cloudflare Edge Tunnels."
            )
        }
    }
}

@Composable
fun SessionSecurityModal(onDismiss: () -> Unit) {
    ProfileModalWrapper(
        title = "Session Security",
        icon = AnonChatIcons.Shield,
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AnonChatSpacing.md)) {
            PrivacyItem(
                title = "Session Lifecycle",
                description = "Your session token is generated locally and lives strictly within your device's sandboxed storage."
            )
            PrivacyItem(
                title = "Cryptographic Hex Tokens",
                description = "Anonymous sessions use cryptographically random 16-hex byte identifiers that contain no personal or device telemetry."
            )
            PrivacyItem(
                title = "Instant Session Destruction",
                description = "Triggering 'Refresh Session & Identity' immediately overwrites the existing token and purges local memory."
            )
        }
    }
}

@Composable
fun AnonymousIdentityGuideModal(onDismiss: () -> Unit) {
    ProfileModalWrapper(
        title = "Anonymous Identity",
        icon = AnonChatIcons.Paw,
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AnonChatSpacing.md)) {
            PrivacyItem(
                title = "How Animal Personas Work",
                description = "When you connect, AfterClass assigns you a distinct animal persona with an authentic nature avatar."
            )
            PrivacyItem(
                title = "Scientific Names & Badges",
                description = "Each animal persona includes its biological taxon name to create a fun, memorable campus vibe."
            )
            PrivacyItem(
                title = "Regeneration Anytime",
                description = "You can change your identity whenever you want using the Change Identity control on your profile card."
            )
        }
    }
}

@Composable
fun AboutAfterClassModal(onDismiss: () -> Unit) {
    ProfileModalWrapper(
        title = "About AfterClass",
        onDismiss = onDismiss
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AnonChatSpacing.md),
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(Res.drawable.afterclass_mark),
                contentDescription = "AfterClass",
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("AfterClass", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Campus Anonymous Network", color = AnonChatColors.TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text("Version 2.4.0 (Cloud Native)", color = AnonChatColors.TextMuted, fontSize = 11.sp)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AnonChatColors.Divider)
            )

            Text(
                text = "AfterClass provides a privacy-first, zero-profiling campus space for open discussion, peer matching, and ephemeral messaging. No ads, no tracking, and no permanent digital footprint.",
                style = AnonChatTypography.BodySecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = AnonChatColors.TextSecondary
            )
        }
    }
}

@Composable
fun CommunityGuidelinesModal(onDismiss: () -> Unit) {
    ProfileModalWrapper(
        title = "Community Guidelines",
        icon = AnonChatIcons.Sparkles,
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AnonChatSpacing.md)) {
            PrivacyItem(
                title = "1. Respect Anonymity",
                description = "Never attempt to de-anonymize, dox, or reveal the real-world identity of another student."
            )
            PrivacyItem(
                title = "2. Constructive Atmosphere",
                description = "Harassment, hate speech, bullying, or threats will result in immediate session blacklisting."
            )
            PrivacyItem(
                title = "3. Academic Integrity",
                description = "Do not share unauthorized exam materials or engage in academic dishonesty."
            )
            PrivacyItem(
                title = "4. Keep Campus Safe",
                description = "Use the anonymous feedback channel to report critical vulnerabilities or abusive behavior."
            )
        }
    }
}

@Composable
private fun PrivacyItem(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AnonChatColors.SurfaceSecondary)
            .border(1.dp, AnonChatColors.Border, RoundedCornerShape(12.dp))
            .padding(AnonChatSpacing.md)
    ) {
        Text(
            text = title,
            style = AnonChatTypography.Label,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = AnonChatColors.TextPrimary
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = description,
            style = AnonChatTypography.BodySecondary,
            fontSize = 12.sp,
            lineHeight = 16.5.sp,
            color = AnonChatColors.TextSecondary
        )
    }
}

