package org.example.annochat.presentation.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import kotlin.math.abs
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinproject.shared.generated.resources.Res
import kotlinproject.shared.generated.resources.onboarding_campus_night
import kotlinproject.shared.generated.resources.onboarding_campus_sunset
import kotlinproject.shared.generated.resources.onboarding_cheetah
import kotlinproject.shared.generated.resources.onboarding_fox
import kotlinproject.shared.generated.resources.onboarding_panda
import kotlinproject.shared.generated.resources.onboarding_rabbit
import kotlinx.coroutines.launch
import org.example.annochat.data.session.SessionStorage
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.designsystem.AnonChatTypography
import org.example.annochat.presentation.navigation.BackHandler
import org.example.annochat.presentation.viewmodel.RoomsViewModel
import org.jetbrains.compose.resources.painterResource

/**
 * Premium 5-Slide Onboarding Flow matching AfterClass Cinematic Visual Identity.
 *
 * Slide 1: "Same campus. New people. Real conversations." (Campus night scene, handwritten note)
 * Slide 2: "No names. No pressure. Just you." (3D Animal avatars grid, doodles)
 * Slide 3: "Two people. One conversation. Zero identities." (Radar connection, speech bubble)
 * Slide 4: "More ways to connect." (Public Groups, Direct Chats, Your Space cards)
 * Slide 5: "Your identity can stay unknown. Your voice doesn't have to." (Sunset campus hero, Enter AfterClass CTA)
 */
@Composable
fun OnboardingScreen(
    viewModel: RoomsViewModel? = null,
    sessionStorage: SessionStorage? = null,
    onCompleteOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalPages = 5
    val pagerState = rememberPagerState(pageCount = { totalPages })
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    fun complete() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        sessionStorage?.setOnboardingCompleted(true)
        sessionStorage?.setAhaMomentCompleted(true)
        onCompleteOnboarding()
    }

    fun advance() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (pagerState.currentPage < totalPages - 1) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        } else {
            complete()
        }
    }

    // Hardware back navigation between slides
    BackHandler(enabled = pagerState.currentPage > 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070709))
    ) {
        // Horizontal Pager for the 5 onboarding slides with smooth cinematic transitions
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).coerceIn(-1f, 1f)
            val absOffset = abs(pageOffset)
            val pageProgress = 1f - absOffset

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Smooth zoom & fade transition
                        val scale = 0.90f + pageProgress * 0.10f
                        scaleX = scale
                        scaleY = scale
                        alpha = (0.20f + pageProgress * 0.80f).coerceIn(0f, 1f)
                        // Cinematic parallax shift
                        translationX = pageOffset * size.width * 0.18f
                    }
            ) {
                when (page) {
                    0 -> SlideSameCampus()
                    1 -> SlideNoNames()
                    2 -> SlideTwoPeople()
                    3 -> SlideMoreWays()
                    4 -> SlideYourIdentity(onEnter = { complete() })
                }
            }
        }

        // ── Top Header: Skip Button (Always visible on all slides) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Skip",
                style = AnonChatTypography.BodySecondary.copy(
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f)
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { complete() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // ── Bottom Navigation Bar (Dots indicator + Circle Next Button) ──
        // Slides 0..3 have dots on the left and white circle arrow button on the right.
        // Slide 4 has its own full-width "Enter AfterClass →" button.
        if (pagerState.currentPage < 4) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 5 Dots Pagination Indicator
                DotsIndicator(
                    currentPage = pagerState.currentPage,
                    totalPages = totalPages
                )

                // Circular White Next Button
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { advance() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AnonChatIcons.ArrowRight,
                        contentDescription = "Next",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SLIDE 1: "Same campus. New people. Real conversations."
// ─────────────────────────────────────────────────────────────
@Composable
private fun SlideSameCampus() {
    val infiniteTransition = rememberInfiniteTransition(label = "slide1Ambient")
    val campusScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "campusScale"
    )
    val noteBob by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "noteBob"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Lower half campus illustration with subtle breathing scale
        Image(
            painter = painterResource(Res.drawable.onboarding_campus_night),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.70f)
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    scaleX = campusScale
                    scaleY = campusScale
                }
        )

        // Gradient fade to ensure crisp contrast for text and status bar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xFF070709),
                            0.30f to Color(0xFF070709),
                            0.48f to Color(0xFF070709).copy(alpha = 0.50f),
                            0.70f to Color.Transparent,
                            0.88f to Color(0xFF070709).copy(alpha = 0.70f),
                            1.00f to Color(0xFF070709).copy(alpha = 0.95f)
                        )
                    )
                )
        )

        // Text & Handwritten Annotation Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 28.dp)
                .padding(top = 44.dp)
        ) {
            // Main Bold Display Headline
            Text(
                text = "Same campus.\nNew people.\nReal conversations.",
                style = AnonChatTypography.HeroTitle.copy(
                    fontSize = 32.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.8).sp,
                    color = Color.White
                )
            )

            Spacer(Modifier.height(4.dp))
            CurvedUnderline(width = 175.dp)

            Spacer(Modifier.height(18.dp))

            // Handwritten Script Note: "Different people. 🙂 / Same vibe."
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp)
                    .offset(y = noteBob.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Different\npeople. 🙂\nSame vibe.",
                    style = TextStyle(
                        fontFamily = FontFamily.Cursive,
                        fontStyle = FontStyle.Italic,
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFBAC7D5)
                    ),
                    modifier = Modifier.graphicsLayer { rotationZ = -6f },
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SLIDE 2: "No names. No pressure. Just you."
// ─────────────────────────────────────────────────────────────
@Composable
private fun SlideNoNames() {
    val infiniteTransition = rememberInfiniteTransition(label = "slide2Floating")
    val float1 by infiniteTransition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float1"
    )
    val float2 by infiniteTransition.animateFloat(
        initialValue = 3.5f,
        targetValue = -3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float2"
    )
    val arrowWobble by infiniteTransition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrowWobble"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 44.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header Text
        Column {
            Text(
                text = "No names.\nNo pressure.\nJust you.",
                style = AnonChatTypography.HeroTitle.copy(
                    fontSize = 32.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.8).sp,
                    color = Color.White
                )
            )

            Spacer(Modifier.height(4.dp))
            CurvedUnderline(width = 90.dp)

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Chat freely without profiles\nto impress or awkward\nintroductions.",
                style = AnonChatTypography.Body.copy(
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = Color(0xFF9E9E9E)
                )
            )
        }

        // Center: 2x2 Animal Cards Cluster with Floating Micro-Animations
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Row (Arctic Fox & Midnight Panda)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Arctic Fox with Crown doodle (floating bob 1)
                    Box(modifier = Modifier.offset(y = float1.dp)) {
                        CrownDoodle(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = 6.dp, y = (-18).dp)
                        )
                        AnimalAvatarCard(
                            imageRes = Res.drawable.onboarding_fox,
                            name = "Arctic Fox",
                            tag = "#23",
                            rotation = -3f
                        )
                    }

                    // Midnight Panda (floating bob 2)
                    Box(modifier = Modifier.offset(y = float2.dp)) {
                        AnimalAvatarCard(
                            imageRes = Res.drawable.onboarding_panda,
                            name = "Midnight Panda",
                            tag = "#17",
                            rotation = 2f
                        )
                    }
                }

                // Bottom Row (Quantum Cheetah & Silent Rabbit)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quantum Cheetah
                    Box(modifier = Modifier.offset(y = (-float2).dp)) {
                        AnimalAvatarCard(
                            imageRes = Res.drawable.onboarding_cheetah,
                            name = "Quantum Cheetah",
                            tag = "#42",
                            rotation = -2f
                        )
                    }

                    // Silent Rabbit
                    Box(modifier = Modifier.offset(y = (-float1).dp)) {
                        AnimalAvatarCard(
                            imageRes = Res.drawable.onboarding_rabbit,
                            name = "Silent Rabbit",
                            tag = "#08",
                            rotation = 3f
                        )
                    }
                }
            }
        }

        // Handwritten note: "Just animals. No identities." with curved arrow
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Just\nanimals.\nNo identities.",
                    style = TextStyle(
                        fontFamily = FontFamily.Cursive,
                        fontStyle = FontStyle.Italic,
                        fontSize = 17.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFBAC7D5)
                    ),
                    modifier = Modifier.graphicsLayer { rotationZ = -5f }
                )
            }

            Spacer(Modifier.width(10.dp))

            CurvedArrowDoodle(
                modifier = Modifier
                    .size(width = 38.dp, height = 30.dp)
                    .graphicsLayer { rotationZ = 10f + arrowWobble }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SLIDE 3: "Two people. One conversation. Zero identities."
// ─────────────────────────────────────────────────────────────
@Composable
private fun SlideTwoPeople() {
    val infiniteTransition = rememberInfiniteTransition(label = "slide3Typing")
    val dot1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 0, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )
    val avatarPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatarPulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 44.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Column {
            Text(
                text = "Two people.\nOne conversation.\nZero identities.",
                style = AnonChatTypography.HeroTitle.copy(
                    fontSize = 32.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.8).sp,
                    color = Color.White
                )
            )

            Spacer(Modifier.height(4.dp))
            CurvedUnderline(width = 145.dp)

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Meet and talk with\nfellow students anonymously.",
                style = AnonChatTypography.Body.copy(
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = Color(0xFF9E9E9E)
                )
            )
        }

        // Center Graphic: Radar soundwaves connecting two illuminated animal avatars
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            contentAlignment = Alignment.Center
        ) {
            // Concentric radar ripple background
            RadarSoundwaves(modifier = Modifier.fillMaxSize())

            // Two avatars connected by typing bubble
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left avatar: Arctic Fox in illuminated white ring with gentle pulse
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                        .background(Color(0xFF141419))
                        .graphicsLayer {
                            scaleX = avatarPulse
                            scaleY = avatarPulse
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(Res.drawable.onboarding_fox),
                        contentDescription = "Arctic Fox",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                    )
                }

                // Connecting speech bubble pill with animated typing dots
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1C1C24))
                        .border(1.dp, Color(0xFF2E2E38), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .offset(y = dot1Offset.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.90f))
                        )
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .offset(y = dot2Offset.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.90f))
                        )
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .offset(y = dot3Offset.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.90f))
                        )
                    }
                }

                // Right avatar: Midnight Panda in illuminated white ring
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                        .background(Color(0xFF141419))
                        .graphicsLayer {
                            scaleX = avatarPulse
                            scaleY = avatarPulse
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(Res.drawable.onboarding_panda),
                        contentDescription = "Midnight Panda",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }

        // Handwritten note: "Real people. Real conversations."
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Real people.\nReal conversations.",
                style = TextStyle(
                    fontFamily = FontFamily.Cursive,
                    fontStyle = FontStyle.Italic,
                    fontSize = 19.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFBAC7D5)
                ),
                modifier = Modifier.graphicsLayer { rotationZ = -5f },
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SLIDE 4: "More ways to connect."
// ─────────────────────────────────────────────────────────────
@Composable
private fun SlideMoreWays() {
    val infiniteTransition = rememberInfiniteTransition(label = "slide4Airplane")
    val planeBob by infiniteTransition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "planeBob"
    )
    val planeRotate by infiniteTransition.animateFloat(
        initialValue = -25f,
        targetValue = -18f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "planeRotate"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 44.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Column {
            Text(
                text = "More ways\nto connect.",
                style = AnonChatTypography.HeroTitle.copy(
                    fontSize = 32.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.8).sp,
                    color = Color.White
                )
            )

            Spacer(Modifier.height(4.dp))
            CurvedUnderline(width = 105.dp)

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Join rooms, find people nearby,\nor create your own space.",
                style = AnonChatTypography.Body.copy(
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = Color(0xFF9E9E9E)
                )
            )
        }

        // Center 3 Feature Cards
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FeatureConnectCard(
                icon = AnonChatIcons.Users,
                title = "Public Groups",
                subtitle = "Explore open rooms and meet new people"
            )

            FeatureConnectCard(
                icon = AnonChatIcons.Bolt,
                title = "Direct Chats",
                subtitle = "Private 1-on-1 conversations with campus peers"
            )

            FeatureConnectCard(
                icon = AnonChatIcons.ChatBubbles,
                title = "Your Space",
                subtitle = "Create your own group or hang out in existing ones"
            )
        }

        // Hand-drawn Paper Airplane Doodle at bottom left with floating animation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = AnonChatIcons.Send,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .size(28.dp)
                    .offset(y = planeBob.dp)
                    .graphicsLayer { rotationZ = planeRotate }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SLIDE 5: "Your identity can stay unknown. Your voice doesn't have to."
// ─────────────────────────────────────────────────────────────
@Composable
private fun SlideYourIdentity(onEnter: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "slide5Ambient")
    val sunsetZoom by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sunsetZoom"
    )
    val noteBob by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "noteBob"
    )
    val buttonScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "buttonScale"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Fullscreen twilight/sunset campus illustration with subtle zoom
        Image(
            painter = painterResource(Res.drawable.onboarding_campus_sunset),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = sunsetZoom
                    scaleY = sunsetZoom
                }
        )

        // Gradient fade overlay for top text and bottom CTA button
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xFF070709),
                            0.28f to Color(0xFF070709),
                            0.45f to Color(0xFF070709).copy(alpha = 0.35f),
                            0.65f to Color.Transparent,
                            0.82f to Color(0xFF070709).copy(alpha = 0.60f),
                            1.00f to Color(0xFF070709).copy(alpha = 0.95f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 44.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Headline
            Column {
                Text(
                    text = "Your identity\ncan stay unknown.",
                    style = AnonChatTypography.HeroTitle.copy(
                        fontSize = 32.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.8).sp,
                        color = Color.White
                    )
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Your voice doesn't have to.",
                    style = AnonChatTypography.Body.copy(
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        color = Color(0xFFD4D4D8)
                    )
                )
            }

            // Sunset sky handwritten script note with gentle float
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp, top = 20.dp)
                    .offset(y = noteBob.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Real people.\nReal conversations.\nZero identities.",
                    style = TextStyle(
                        fontFamily = FontFamily.Cursive,
                        fontStyle = FontStyle.Italic,
                        fontSize = 18.5.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFFE0C2) // glowing warm sunset golden-peach tone
                    ),
                    modifier = Modifier.graphicsLayer { rotationZ = -6f },
                    textAlign = TextAlign.Center
                )
            }

            // Bottom Section: Centered 5 dots + Wide "Enter AfterClass →" button
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                DotsIndicator(
                    currentPage = 4,
                    totalPages = 5
                )

                // Wide White Pill CTA Button with gentle breathing scale
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .graphicsLayer {
                            scaleX = buttonScale
                            scaleY = buttonScale
                        }
                        .clip(RoundedCornerShape(27.dp))
                        .background(Color.White)
                        .clickable(interactionSource = interactionSource, indication = null) {
                            onEnter()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Enter AfterClass",
                            style = AnonChatTypography.Button.copy(
                                color = Color.Black,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Icon(
                            imageVector = AnonChatIcons.ArrowRight,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// COMPONENT: Animal Avatar Card (used on Slide 2)
// ─────────────────────────────────────────────────────────────
@Composable
private fun AnimalAvatarCard(
    imageRes: org.jetbrains.compose.resources.DrawableResource,
    name: String,
    tag: String,
    rotation: Float
) {
    Box(
        modifier = Modifier
            .width(135.dp)
            .height(152.dp)
            .graphicsLayer { rotationZ = rotation }
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141419))
            .border(1.dp, Color(0xFF26262E), RoundedCornerShape(20.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = name,
                style = AnonChatTypography.Caption.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                maxLines = 1,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = tag,
                style = AnonChatTypography.Caption.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF8E9AA8)
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// COMPONENT: Feature Connect Card (used on Slide 4)
// ─────────────────────────────────────────────────────────────
@Composable
private fun FeatureConnectCard(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF141419))
            .border(1.dp, Color(0xFF26262E), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Circular Icon container
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22222A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        style = AnonChatTypography.RoomTitle.copy(
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = AnonChatTypography.Caption.copy(
                            fontSize = 12.sp,
                            color = Color(0xFF9E9E9E)
                        ),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector = AnonChatIcons.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF6E6E78),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// COMPONENT: 5-Dots Pagination Indicator
// ─────────────────────────────────────────────────────────────
@Composable
private fun DotsIndicator(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalPages) {
            val isActive = i == currentPage
            val dotWidth by animateDpAsState(
                targetValue = if (isActive) 24.dp else 6.dp,
                animationSpec = tween(
                    durationMillis = 260,
                    easing = FastOutSlowInEasing
                ),
                label = "dotWidth"
            )
            val dotAlpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.28f,
                animationSpec = tween(260, easing = FastOutSlowInEasing),
                label = "dotAlpha"
            )

            Box(
                modifier = Modifier
                    .size(width = dotWidth, height = 6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = dotAlpha))
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// CANVAS DOODLE: Curved Underline Brush Stroke
// ─────────────────────────────────────────────────────────────
@Composable
private fun CurvedUnderline(
    width: Dp,
    color: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(width = width, height = 10.dp)) {
        val path = Path().apply {
            moveTo(2f, 3f)
            quadraticTo(
                size.width * 0.5f, size.height,
                size.width - 2f, 4f
            )
        }
        drawPath(
            path = path,
            color = color.copy(alpha = 0.90f),
            style = Stroke(
                width = 2.4f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────
// CANVAS DOODLE: Hand-Drawn Crown Doodle (Slide 2)
// ─────────────────────────────────────────────────────────────
@Composable
private fun CrownDoodle(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Canvas(modifier = modifier.size(26.dp, 18.dp)) {
        val path = Path().apply {
            moveTo(2f, 15f)
            lineTo(size.width - 2f, 15f)
            lineTo(size.width - 4f, 4f)
            lineTo(size.width * 0.65f, 10f)
            lineTo(size.width * 0.5f, 2f)
            lineTo(size.width * 0.35f, 10f)
            lineTo(4f, 4f)
            close()
        }
        drawPath(
            path = path,
            color = color.copy(alpha = 0.90f),
            style = Stroke(
                width = 2.0f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        // Little circles on crown tips
        drawCircle(color = color, radius = 1.8f, center = Offset(4f, 4f))
        drawCircle(color = color, radius = 1.8f, center = Offset(size.width * 0.5f, 2f))
        drawCircle(color = color, radius = 1.8f, center = Offset(size.width - 4f, 4f))
    }
}

// ─────────────────────────────────────────────────────────────
// CANVAS DOODLE: Curved Arrow Doodle (Slide 2)
// ─────────────────────────────────────────────────────────────
@Composable
private fun CurvedArrowDoodle(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(2f, size.height - 4f)
            quadraticTo(
                size.width * 0.6f, size.height + 4f,
                size.width - 4f, 8f
            )
        }
        drawPath(
            path = path,
            color = color.copy(alpha = 0.85f),
            style = Stroke(width = 2f, cap = StrokeCap.Round)
        )
        // Arrow head
        val headPath = Path().apply {
            moveTo(size.width - 12f, 12f)
            lineTo(size.width - 4f, 8f)
            lineTo(size.width - 6f, 18f)
        }
        drawPath(
            path = headPath,
            color = color.copy(alpha = 0.85f),
            style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

// ─────────────────────────────────────────────────────────────
// CANVAS DOODLE: Radar Soundwaves & Resonance Arcs (Slide 3)
// ─────────────────────────────────────────────────────────────
@Composable
private fun RadarSoundwaves(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "radarPulse")
    val ring1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1"
    )
    val ring2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, delayMillis = 933, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2"
    )
    val ring3 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, delayMillis = 1866, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring3"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val minRadius = 45.dp.toPx()
        val maxRadius = 155.dp.toPx()

        // Continuous radiating ripples
        listOf(ring1, ring2, ring3).forEach { progress ->
            val radius = minRadius + progress * (maxRadius - minRadius)
            val alpha = (1f - progress) * 0.28f
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = radius,
                center = center,
                style = Stroke(width = 1.2f)
            )
        }

        // Hand-drawn soundwave arcs above and below the avatars
        // Above left avatar
        drawArc(
            color = Color.White.copy(alpha = 0.70f),
            startAngle = 210f,
            sweepAngle = 40f,
            useCenter = false,
            topLeft = Offset(center.x - 120.dp.toPx(), center.y - 65.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(60.dp.toPx(), 45.dp.toPx()),
            style = Stroke(width = 2f, cap = StrokeCap.Round)
        )
        // Above right avatar
        drawArc(
            color = Color.White.copy(alpha = 0.70f),
            startAngle = 290f,
            sweepAngle = 40f,
            useCenter = false,
            topLeft = Offset(center.x + 60.dp.toPx(), center.y - 65.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(60.dp.toPx(), 45.dp.toPx()),
            style = Stroke(width = 2f, cap = StrokeCap.Round)
        )
    }
}
