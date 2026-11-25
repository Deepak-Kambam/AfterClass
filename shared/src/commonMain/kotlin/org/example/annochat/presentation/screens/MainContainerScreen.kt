package org.example.annochat.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.annochat.data.repository.ChatRepository
import org.example.annochat.data.session.SessionStorage
import org.example.annochat.presentation.components.CapsuleBottomNavBar
import org.example.annochat.presentation.components.NavTab
import org.example.annochat.presentation.designsystem.AnonChatColors
import io.kotzilla.sdk.compose.KotzillaTrackScreen
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.designsystem.AnonChatSpacing
import org.example.annochat.presentation.viewmodel.RoomsViewModel

import org.example.annochat.data.repository.BroadcastRepository
import org.example.annochat.data.repository.NotificationRepository

@Composable
fun MainContainerScreen(
    roomsViewModel: RoomsViewModel,
    repository: ChatRepository,
    sessionStorage: SessionStorage,
    notificationRepository: NotificationRepository? = null,
    broadcastRepository: BroadcastRepository? = null,
    onRoomClick: (roomId: String, roomName: String) -> Unit,
    isLiquidGlassEnabled: Boolean,
    onToggleLiquidGlass: (Boolean) -> Unit,
    onNavigateToAppearance: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val roomsState by roomsViewModel.uiState.collectAsState()
    val selectedTab = roomsState.selectedTab
    val identity by repository.myIdentity.collectAsState()
    val onlineCount by repository.onlineCount.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {

            // Directional Macro Transitions when Bottom Navbar Options Change
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    val slideSpec = tween<IntOffset>(durationMillis = 320, easing = FastOutSlowInEasing)
                    val fadeSpec = tween<Float>(durationMillis = 280, easing = FastOutSlowInEasing)

                    if (forward) {
                        // Moving forward (to the right): Slide in from right, exit left
                        (slideInHorizontally(slideSpec) { (it * 0.25f).toInt() } + fadeIn(fadeSpec))
                            .togetherWith(slideOutHorizontally(slideSpec) { (-it * 0.25f).toInt() } + fadeOut(fadeSpec))
                    } else {
                        // Moving backward (to the left): Slide in from left, exit right
                        (slideInHorizontally(slideSpec) { (-it * 0.25f).toInt() } + fadeIn(fadeSpec))
                            .togetherWith(slideOutHorizontally(slideSpec) { (it * 0.25f).toInt() } + fadeOut(fadeSpec))
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { tab ->
                when (tab) {
                    NavTab.CHATS -> {
                        KotzillaTrackScreen("RoomsTab")
                        RoomsScreen(
                            viewModel = roomsViewModel,
                            onRoomClick = onRoomClick,
                            notificationRepository = notificationRepository,
                            isLiquidGlassEnabled = isLiquidGlassEnabled,
                            onToggleLiquidGlass = onToggleLiquidGlass
                        )
                    }

                    NavTab.ONLINE -> {
                        KotzillaTrackScreen("OnlineUsersTab")
                        OnlineUsersScreen(
                            repository = repository,
                            onDirectChatCreated = onRoomClick,
                            primarySegment = roomsState.onlinePrimarySegment,
                            onPrimarySegmentSelected = { roomsViewModel.setOnlinePrimarySegment(it) },
                            groupsSegment = roomsState.onlineGroupsSegment,
                            onGroupsSegmentSelected = { roomsViewModel.setOnlineGroupsSegment(it) }
                        )
                    }

                    NavTab.PROFILE -> {
                        KotzillaTrackScreen("ProfileTab")
                        ProfileScreen(
                            repository = repository,
                            sessionStorage = sessionStorage,
                            isLiquidGlassEnabled = isLiquidGlassEnabled,
                            onToggleLiquidGlass = onToggleLiquidGlass,
                            onNavigateToAppearance = onNavigateToAppearance,
                            onResetSession = {
                                roomsViewModel.loadRooms()
                            },
                            onExploreRooms = {
                                roomsViewModel.setTab(NavTab.CHATS)
                            },
                            onRoomClick = onRoomClick,
                            broadcastRepository = broadcastRepository
                        )
                    }
                }
            }


            CapsuleBottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { roomsViewModel.setTab(it) },
                isRotatingGlowEnabled = sessionStorage.isRotatingGlowEnabled(),
                repository = repository,
                profileAvatarUrl = identity?.avatarUrl,
                profileAnimalName = identity?.animalName ?: identity?.anonName ?: sessionStorage.getSavedAnonName(),
                profilePublicId = identity?.publicId ?: sessionStorage.getSavedPublicId(),
                chatsBadgeCount = if (roomsState.publicRooms.isNotEmpty()) roomsState.publicRooms.size else null,
                contactsBadgeCount = if (onlineCount > 0) onlineCount else null,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

