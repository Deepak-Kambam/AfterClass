package org.example.annochat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import org.example.annochat.data.repository.BroadcastRepository
import org.example.annochat.data.repository.BroadcastRepositoryImpl
import org.example.annochat.data.repository.ChatRepository
import org.example.annochat.data.repository.ChatRepositoryImpl
import org.example.annochat.data.repository.NotificationRepositoryImpl
import org.example.annochat.presentation.components.AfterClassBroadcastBanner
import org.example.annochat.presentation.components.AppUpdateDialog
import org.example.annochat.data.repository.UpdateManager
import org.example.annochat.data.session.SessionStorage
import org.example.annochat.data.session.rememberPlatformSessionStorage
import org.example.annochat.network.ApiClient
import org.example.annochat.network.DpdnsEndpointDiscoveryService
import org.example.annochat.network.EndpointDiscoveryService
import org.example.annochat.network.NetworkConfig
import org.example.annochat.network.WebSocketClient
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.navigation.BackHandler
import org.example.annochat.presentation.navigation.Screen
import org.example.annochat.presentation.navigation.rememberNavigator
import org.example.annochat.presentation.screens.AppearanceScreen
import org.example.annochat.presentation.screens.ChatScreen
import org.example.annochat.presentation.screens.MainContainerScreen
import org.example.annochat.presentation.screens.NoInternetScreen
import org.example.annochat.presentation.screens.OnboardingScreen
import org.example.annochat.presentation.screens.RoomsScreen
import org.example.annochat.presentation.screens.SplashScreen
import org.example.annochat.presentation.theme.AnonChatTheme
import androidx.compose.runtime.collectAsState
import io.kotzilla.sdk.compose.KotzillaTrackScreen
import androidx.compose.foundation.background

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import org.example.annochat.data.model.IncomingEvent
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.designsystem.AnonChatSpacing
import org.example.annochat.presentation.viewmodel.ChatViewModel
import org.example.annochat.presentation.viewmodel.RoomsViewModel
import org.example.annochat.presentation.components.AnonChatPrimaryButton
import org.example.annochat.presentation.components.AnonChatSecondaryButton
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import kotlinproject.shared.generated.resources.Res
import org.example.annochat.presentation.components.NavTab
import kotlinproject.shared.generated.resources.bg_space

@Composable
fun App(
    networkConfig: NetworkConfig = remember { defaultNetworkConfig() },
    sessionStorage: SessionStorage = rememberPlatformSessionStorage(),
    discoveryService: EndpointDiscoveryService? = remember(networkConfig) {
        if (!networkConfig.discoveryUrl.isNullOrBlank()) {
            DpdnsEndpointDiscoveryService(discoveryUrl = networkConfig.discoveryUrl)
        } else null
    },
    deepLinkUri: String? = null,
    onRepositoryCreated: ((ChatRepository) -> Unit)? = null
) {
    val repository: ChatRepository = remember(networkConfig, sessionStorage, discoveryService) {
        val apiClient = ApiClient(networkConfig)
        lateinit var wsClient: WebSocketClient
        wsClient = WebSocketClient(
            config = networkConfig,
            onReconnectAttempt = {
                discoveryService?.resolveConfig(forceRefresh = true)?.onSuccess { resolved ->
                    apiClient.updateConfig(resolved)
                    wsClient.updateConfig(resolved)
                }
            }
        )
        ChatRepositoryImpl(
            apiClient = apiClient,
            webSocketClient = wsClient,
            sessionStorage = sessionStorage,
            discoveryService = discoveryService
        ).also { repo ->
            onRepositoryCreated?.invoke(repo)
        }
    }

    DisposableEffect(repository) {
        onDispose {
            repository.disconnect()
        }
    }

    // App launch starts on Splash: runs splash animation + backend check concurrently
    val navigator = rememberNavigator(initialScreen = Screen.Splash)
    val roomsViewModel = remember { RoomsViewModel(repository, sessionStorage) }
    val notificationRepository = remember(repository, sessionStorage) {
        NotificationRepositoryImpl(
            chatRepository = repository,
            sessionStorage = sessionStorage
        )
    }
    val broadcastRepository: BroadcastRepository = remember(repository, sessionStorage) {
        val impl = repository as ChatRepositoryImpl
        BroadcastRepositoryImpl(
            apiClient = impl.apiClient,
            webSocketClient = impl.webSocketClient,
            chatRepository = repository,
            sessionStorage = sessionStorage
        )
    }

    val updateManager = remember(repository) {
        val apiClient = (repository as? ChatRepositoryImpl)?.apiClient ?: ApiClient(networkConfig)
        UpdateManager(apiClient)
    }
    val updateState by updateManager.state.collectAsState()

    // Non-blocking update check: triggered after launch reaches stable state
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500)
        updateManager.checkForUpdates()
    }

    LaunchedEffect(navigator.currentScreen) {
        val currentRoom = when (val scr = navigator.currentScreen) {
            is Screen.Chat -> scr.roomId
            else -> "main"
        }
        broadcastRepository.onRoomChanged(currentRoom)
    }

    val globalSnackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var pendingInvite by remember { mutableStateOf<IncomingEvent.PrivateChatInvite?>(null) }

    // Global WebSocket event listener for chat invites, declines, and match results across ALL screens
    LaunchedEffect(repository) {
        repository.incomingEvents.collect { event ->
            when (event) {
                is IncomingEvent.PrivateChatInvite -> {
                    // Another user sent us a private chat or match request!
                    pendingInvite = event
                }
                is IncomingEvent.RoomJoined -> {
                    val isPrivate = event.room.roomId.startsWith("dm_") || event.room.roomId != "main"
                    if (isPrivate) {
                        sessionStorage.saveLastActiveRoom(event.room.roomId, event.room.cleanDisplayName)
                        navigator.navigateTo(Screen.Chat(event.room.roomId, event.room.cleanDisplayName))
                    }
                }
                is IncomingEvent.RoomInvitationAccepted -> {
                    coroutineScope.launch {
                        val groupLabel = event.roomName?.ifBlank { null }?.let { " to \"$it\"" } ?: ""
                        globalSnackbarHostState.showSnackbar("🎉 ${event.acceptedBy} accepted your invitation and joined the group$groupLabel!")
                    }
                }
                is IncomingEvent.RoomUpdated -> {
                    val sysContent = event.systemMessage?.content
                    if (sysContent != null && sysContent.contains("accepted the invite and joined")) {
                        coroutineScope.launch {
                            globalSnackbarHostState.showSnackbar("🎉 $sysContent")
                        }
                    }
                }
                is IncomingEvent.PrivateChatDeclined -> {
                    coroutineScope.launch {
                        val msg = if (event.isGroup) "${event.declinedBy} declined your group invitation." else "${event.declinedBy} declined your chat request."
                        globalSnackbarHostState.showSnackbar(msg)
                    }
                }
                is IncomingEvent.PrivateChatSent -> {
                    coroutineScope.launch {
                        val msg = if (event.isGroup) {
                            "Group invitation sent to ${event.targetName}. Waiting for response…"
                        } else {
                            "Chat request sent to ${event.targetName}. Waiting for response…"
                        }
                        globalSnackbarHostState.showSnackbar(msg)
                    }
                }
                else -> {}
            }
        }
    }

    LaunchedEffect(deepLinkUri) {
        if (!deepLinkUri.isNullOrBlank()) {
            when {
                deepLinkUri.contains("rooms") || deepLinkUri.contains("chats") -> {
                    roomsViewModel.setTab(NavTab.CHATS)
                    if (sessionStorage.isOnboardingCompleted() && navigator.currentScreen !is Screen.Rooms) {
                        navigator.navigateTo(Screen.Rooms)
                    }
                }
                deepLinkUri.contains("profile") -> {
                    roomsViewModel.setTab(NavTab.PROFILE)
                    if (sessionStorage.isOnboardingCompleted() && navigator.currentScreen !is Screen.Rooms) {
                        navigator.navigateTo(Screen.Rooms)
                    }
                }
            }
        }
    }

    // Persisted Liquid Glass Theme state
    var isLiquidGlassEnabled by remember {
        mutableStateOf(sessionStorage.isLiquidGlassEnabled())
    }

    // Persisted Branded Appearance & Personalization state
    var appearanceSettings by remember {
        mutableStateOf(sessionStorage.getAppearanceSettings())
    }

    // Back button handling: pop screen if backstack has history (e.g. Chat -> Rooms)
    BackHandler(enabled = navigator.canPopBack) {
        if (navigator.currentScreen is Screen.Chat) {
            sessionStorage.saveLastActiveRoom(null, null)
        }
        navigator.popBack()
    }

    val navEaseInOutSlide = remember {
        tween<androidx.compose.ui.unit.IntOffset>(durationMillis = 340, easing = FastOutSlowInEasing)
    }
    val navEaseInOutBackSlide = remember {
        tween<androidx.compose.ui.unit.IntOffset>(durationMillis = 300, easing = FastOutSlowInEasing)
    }
    val navEaseInOutFade = remember {
        tween<Float>(durationMillis = 260, easing = FastOutSlowInEasing)
    }

    AnonChatTheme(
        liquidGlassEnabled = isLiquidGlassEnabled,
        appearanceSettings = appearanceSettings
    ) {
        org.example.annochat.presentation.components.CampusEnvironmentBackground(
            modifier = Modifier.fillMaxSize()
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent
            ) { _ ->
                Box(modifier = Modifier.fillMaxSize()) {
                    val activeBroadcast by broadcastRepository.activeBroadcast.collectAsState()
                val isBroadcastActive = activeBroadcast != null && navigator.currentScreen !is Screen.Splash && navigator.currentScreen !is Screen.NoInternet
                val isGlobalSnackbarActive = globalSnackbarHostState.currentSnackbarData != null
                val isScreenUnfocused = pendingInvite != null || isBroadcastActive || isGlobalSnackbarActive

                val unfocusProgress by animateFloatAsState(
                    targetValue = if (isScreenUnfocused) 1f else 0f,
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                    label = "UnfocusProgress"
                )

                AnimatedContent(
                    targetState = navigator.currentScreen,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (unfocusProgress > 0.001f) {
                                alpha = 1f - (unfocusProgress * 0.65f)
                                scaleX = 1f - (unfocusProgress * 0.025f)
                                scaleY = 1f - (unfocusProgress * 0.025f)
                            }
                        },
                    transitionSpec = {
                        if (targetState is Screen.Chat && initialState is Screen.Rooms) {
                            // Forward navigation into Chat: strictly ease-in, ease-out fluid slide from right with fade
                            (slideInHorizontally(navEaseInOutSlide) { it } + fadeIn(navEaseInOutFade))
                                .togetherWith(slideOutHorizontally(navEaseInOutSlide) { -it / 3 } + fadeOut(navEaseInOutFade))
                                .apply { targetContentZIndex = 1f }
                        } else if (targetState is Screen.Rooms && initialState is Screen.Chat) {
                            // Back navigation from Chat: strictly ease-in, ease-out slide out to right cleanly revealing Rooms
                            (slideInHorizontally(navEaseInOutBackSlide) { -it / 3 } + fadeIn(navEaseInOutFade))
                                .togetherWith(slideOutHorizontally(navEaseInOutBackSlide) { it } + fadeOut(navEaseInOutFade))
                                .apply { targetContentZIndex = -1f }
                        } else if (navigator.lastDirection == org.example.annochat.presentation.navigation.NavDirection.Backward) {
                            // Standard backward navigation (strictly ease-in, ease-out)
                            (slideInHorizontally(navEaseInOutBackSlide) { -it / 3 } + fadeIn(navEaseInOutFade))
                                .togetherWith(slideOutHorizontally(navEaseInOutBackSlide) { it } + fadeOut(navEaseInOutFade))
                                .apply { targetContentZIndex = -1f }
                        } else if (targetState is Screen.NoInternet) {
                            // Forward transition into No Internet Screen: strictly ease-in, ease-out slide from right + fade
                            (slideInHorizontally(navEaseInOutSlide) { it } + fadeIn(navEaseInOutFade))
                                .togetherWith(slideOutHorizontally(navEaseInOutSlide) { -it / 3 } + fadeOut(navEaseInOutFade))
                                .apply { targetContentZIndex = 1f }
                        } else if (initialState is Screen.NoInternet) {
                            // Backward transition from No Internet Screen: strictly ease-in, ease-out slide to right + fade
                            (slideInHorizontally(navEaseInOutBackSlide) { -it / 3 } + fadeIn(navEaseInOutFade))
                                .togetherWith(slideOutHorizontally(navEaseInOutBackSlide) { it } + fadeOut(navEaseInOutFade))
                                .apply { targetContentZIndex = -1f }
                        } else if (initialState is Screen.Splash) {
                            // Direct splash replacement transitions (strictly ease-in, ease-out)
                            fadeIn(navEaseInOutFade).togetherWith(fadeOut(navEaseInOutFade))
                        } else if (targetState is Screen.Rooms && initialState is Screen.Onboarding) {
                            (fadeIn(navEaseInOutFade) + slideInHorizontally(navEaseInOutSlide) { it / 3 })
                                .togetherWith(fadeOut(navEaseInOutFade))
                        } else {
                            // Standard forward navigation (strictly ease-in, ease-out): entering slides from right
                            (slideInHorizontally(navEaseInOutSlide) { it } + fadeIn(navEaseInOutFade))
                                .togetherWith(slideOutHorizontally(navEaseInOutSlide) { -it / 3 } + fadeOut(navEaseInOutFade))
                                .apply { targetContentZIndex = 1f }
                        }
                    }
                ) { screen ->
                    when (screen) {
                        is Screen.Splash -> {
                            KotzillaTrackScreen("SplashScreen")
                            SplashScreen(
                                repository = repository,
                                onStartupSuccess = {
                                    if (!sessionStorage.isOnboardingCompleted()) {
                                        navigator.replaceWith(Screen.Onboarding)
                                    } else {
                                        navigator.replaceWith(Screen.Rooms)
                                        val lastRoomId = sessionStorage.getLastActiveRoomId()
                                        val lastRoomName = sessionStorage.getLastActiveRoomName()
                                        if (!lastRoomId.isNullOrBlank()) {
                                            navigator.navigateTo(Screen.Chat(lastRoomId, lastRoomName ?: lastRoomId))
                                        }
                                    }
                                },
                                onStartupFailure = { reason ->
                                    navigator.replaceWith(Screen.NoInternet(reason))
                                }
                            )
                        }

                        is Screen.NoInternet -> {
                            KotzillaTrackScreen("NoInternetScreen")
                            NoInternetScreen(
                                repository = repository,
                                sessionStorage = sessionStorage,
                                reason = screen.reason,
                                onRetrySuccess = {
                                    if (!sessionStorage.isOnboardingCompleted()) {
                                        navigator.replaceWith(Screen.Onboarding)
                                    } else {
                                        navigator.replaceWith(Screen.Rooms)
                                        val lastRoomId = sessionStorage.getLastActiveRoomId()
                                        val lastRoomName = sessionStorage.getLastActiveRoomName()
                                        if (!lastRoomId.isNullOrBlank()) {
                                            navigator.navigateTo(Screen.Chat(lastRoomId, lastRoomName ?: lastRoomId))
                                        }
                                    }
                                }
                            )
                        }

                        is Screen.Onboarding -> {
                            KotzillaTrackScreen("OnboardingScreen")
                            OnboardingScreen(
                                viewModel = roomsViewModel,
                                sessionStorage = sessionStorage,
                                onCompleteOnboarding = {
                                    sessionStorage.setOnboardingCompleted(true)
                                    navigator.replaceWith(Screen.Rooms)
                                }
                            )
                        }

                        is Screen.Rooms -> {
                            KotzillaTrackScreen("MainContainerScreen")
                            MainContainerScreen(
                                roomsViewModel = roomsViewModel,
                                repository = repository,
                                sessionStorage = sessionStorage,
                                notificationRepository = notificationRepository,
                                broadcastRepository = broadcastRepository,
                                onRoomClick = { roomId, roomName ->
                                    sessionStorage.saveLastActiveRoom(roomId, roomName)
                                    navigator.navigateTo(Screen.Chat(roomId, roomName))
                                },
                                isLiquidGlassEnabled = isLiquidGlassEnabled,
                                onToggleLiquidGlass = { isLiquidGlassEnabled = it },
                                onNavigateToAppearance = {
                                    navigator.navigateTo(Screen.Appearance)
                                }
                            )
                        }

                        is Screen.Appearance -> {
                            KotzillaTrackScreen("AppearanceScreen")
                            val identity by repository.myIdentity.collectAsState()
                            AppearanceScreen(
                                sessionStorage = sessionStorage,
                                repository = repository,
                                onBack = { navigator.popBack() },
                                onSettingsChanged = { newSettings ->
                                    appearanceSettings = newSettings
                                },
                                profileAnimalName = identity?.animalName ?: identity?.anonName,
                                profilePublicId = identity?.publicId,
                                profileAvatarUrl = identity?.avatarUrl
                            )
                        }

                        is Screen.Chat -> {
                            KotzillaTrackScreen("ChatScreen")
                            val chatViewModel = remember(screen.roomId) {
                                ChatViewModel(
                                    roomId = screen.roomId,
                                    roomName = screen.roomName,
                                    repository = repository,
                                    sessionStorage = sessionStorage
                                )
                            }
                            DisposableEffect(chatViewModel) {
                                onDispose {
                                    chatViewModel.clear()
                                }
                            }

                            ChatScreen(
                                viewModel = chatViewModel,
                                onBackClick = {
                                    sessionStorage.saveLastActiveRoom(null, null)
                                    navigator.popBack()
                                }
                            )
                        }
                    }
                }


                // Scrim overlay to unfocus and dim the screen when any broadcast banner, snackbar, or modal is active
                val scrimAlpha = unfocusProgress * 0.65f
                if (scrimAlpha > 0.005f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = scrimAlpha))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (isGlobalSnackbarActive) {
                                    globalSnackbarHostState.currentSnackbarData?.dismiss()
                                }
                            }
                    )
                }

                // Campus System Broadcast Banner (globally visible across all screens, elevated dialog box)
                androidx.compose.animation.AnimatedVisibility(
                    visible = isBroadcastActive,
                    enter = slideInVertically(
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) { -it } + fadeIn(tween(250, easing = FastOutSlowInEasing)),
                    exit = slideOutVertically(
                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                    ) { -it } + fadeOut(tween(180, easing = FastOutSlowInEasing)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 8.dp)
                ) {
                    activeBroadcast?.let { bc ->
                        AfterClassBroadcastBanner(
                            broadcast = bc,
                            onDismiss = {
                                broadcastRepository.dismissBroadcast(bc.broadcastId)
                            }
                        )
                    }
                }

                // Top floating toast notification banner (elevated, focused, borderless, 100% opaque)
                SnackbarHost(
                    hostState = globalSnackbarHostState,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = if (isBroadcastActive) 108.dp else 16.dp)
                        .padding(horizontal = AnonChatSpacing.md),
                    snackbar = { snackbarData ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 28.dp,
                                    shape = RoundedCornerShape(20.dp),
                                    ambientColor = Color.Black,
                                    spotColor = Color.Black
                                )
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF161618))
                                .clickable {
                                    snackbarData.dismiss()
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Focused accent pulse indicator (borderless)
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(AnonChatColors.Primary)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = snackbarData.visuals.message,
                                    color = AnonChatColors.TextPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 19.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                if (snackbarData.visuals.actionLabel != null) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = snackbarData.visuals.actionLabel ?: "",
                                        color = AnonChatColors.Primary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable {
                                            snackbarData.performAction()
                                        }
                                    )
                                }
                            }
                        }
                    }
                )

                // Global Incoming 1-on-1 Chat / Match Request Alert Dialog
                pendingInvite?.let { invite ->
                    val isGroupInvite = invite.isGroup || !invite.roomId.isNullOrBlank()
                    val groupTitle = invite.roomName?.ifBlank { null } ?: "Private Group"
                    Dialog(
                        onDismissRequest = {
                            coroutineScope.launch {
                                repository.respondPrivateChat(invite.inviteId, accepted = false)
                                globalSnackbarHostState.showSnackbar("Declined invitation to \"$groupTitle\"")
                            }
                            pendingInvite = null
                        },
                        properties = DialogProperties(
                            dismissOnBackPress = true,
                            dismissOnClickOutside = false,
                            usePlatformDefaultWidth = false
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.80f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(Color(0xFF141416))
                                    .padding(24.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(AnonChatColors.SurfaceSecondary)
                                            .border(1.5.dp, AnonChatColors.Primary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isGroupInvite) AnonChatIcons.Users else AnonChatIcons.Paw,
                                            contentDescription = null,
                                            tint = AnonChatColors.Primary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }

                                    Spacer(Modifier.height(14.dp))

                                    Text(
                                        text = if (isGroupInvite) "Group Chat Invitation" else "Private Chat Request",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AnonChatColors.TextPrimary
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    Text(
                                        text = if (isGroupInvite) {
                                            "${invite.fromName} invited you to join \"$groupTitle\"."
                                        } else {
                                            "${invite.fromName} wants to start a private 1-on-1 encrypted chat with you."
                                        },
                                        fontSize = 13.sp,
                                        color = AnonChatColors.TextSecondary,
                                        lineHeight = 18.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )

                                    Spacer(Modifier.height(20.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        AnonChatSecondaryButton(
                                            text = "Decline",
                                            onClick = {
                                                coroutineScope.launch {
                                                    repository.respondPrivateChat(invite.inviteId, accepted = false)
                                                    globalSnackbarHostState.showSnackbar("Declined invitation to \"$groupTitle\"")
                                                }
                                                pendingInvite = null
                                            },
                                            modifier = Modifier.weight(1f)
                                        )

                                        AnonChatPrimaryButton(
                                            text = "Accept",
                                            onClick = {
                                                coroutineScope.launch {
                                                    repository.respondPrivateChat(invite.inviteId, accepted = true)
                                                    val joinMsg = if (isGroupInvite) "Joined group \"$groupTitle\"" else "Connected with ${invite.fromName}"
                                                    globalSnackbarHostState.showSnackbar(joinMsg)
                                                }
                                                if (isGroupInvite && !invite.roomId.isNullOrBlank()) {
                                                    sessionStorage.saveLastActiveRoom(invite.roomId, groupTitle)
                                                    navigator.navigateTo(Screen.Chat(invite.roomId, groupTitle))
                                                }
                                                pendingInvite = null
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // In-app APK Update Dialog (Dark theme, progress tracking, force-update support)
                    AppUpdateDialog(
                        state = updateState,
                        onUpdateClick = { updateManager.startDownload() },
                        onDismissClick = { updateManager.dismissUpdate() }
                    )
                }
            }
            }
        }
    }
}