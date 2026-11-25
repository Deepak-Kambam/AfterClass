package org.example.annochat.data.session

import org.example.annochat.data.model.OnboardingState

/**
 * Lightweight anonymous session storage and user preference abstraction.
 *
 * Persists a generated session ID (e.g. `sess_<16_hex_chars>`), onboarding completion state,
 * personalized onboarding answers, and user visual theme preferences.
 */
interface SessionStorage {
    fun getOrCreateSessionId(): String
    fun getSessionId(): String?
    fun saveSessionId(sessionId: String)
    fun clearSession()

    // Onboarding Preference & Answers
    fun isOnboardingCompleted(): Boolean
    fun setOnboardingCompleted(completed: Boolean)

    fun getOnboardingIntent(): String?
    fun setOnboardingIntent(intent: String?)

    fun getOnboardingVibe(): String?
    fun setOnboardingVibe(vibe: String?)

    fun getOnboardingInterest(): String?
    fun setOnboardingInterest(interest: String?)

    fun isAhaMomentCompleted(): Boolean
    fun setAhaMomentCompleted(completed: Boolean)

    fun isPremiumScreenShown(): Boolean
    fun setPremiumScreenShown(shown: Boolean)

    fun getOnboardingState(): OnboardingState
    fun saveOnboardingState(state: OnboardingState)

    // Liquid Glass Visual Theme Preference
    fun isLiquidGlassEnabled(): Boolean
    fun setLiquidGlassEnabled(enabled: Boolean)

    // Rotating Glow Border Preference
    fun isRotatingGlowEnabled(): Boolean
    fun setRotatingGlowEnabled(enabled: Boolean)

    // Motion & Animations Preference
    fun isAnimationsEnabled(): Boolean
    fun setAnimationsEnabled(enabled: Boolean)

    // Notification Preferences
    fun isMessageNotificationsEnabled(): Boolean
    fun setMessageNotificationsEnabled(enabled: Boolean)

    fun isPrivateChatRequestsEnabled(): Boolean
    fun setPrivateChatRequestsEnabled(enabled: Boolean)

    fun isNotificationPreviewEnabled(): Boolean
    fun setNotificationPreviewEnabled(enabled: Boolean)

    // Appearance & Personalization Settings
    fun getAppearanceSettings(): org.example.annochat.data.model.AppearanceSettings
    fun saveAppearanceSettings(settings: org.example.annochat.data.model.AppearanceSettings)
    fun restoreDefaultAppearance()

    // Custom Backend / Tunnel URL Override
    fun getCustomBackendUrl(): String?
    fun setCustomBackendUrl(url: String?)

    // Identity persistence
    fun getSavedPublicId(): String?
    fun getSavedAnonName(): String?
    fun saveIdentity(publicId: String, anonName: String)

    // State Persistence (App resume & Navigation restoration)
    fun getLastActiveRoomId(): String?
    fun getLastActiveRoomName(): String?
    fun saveLastActiveRoom(roomId: String?, roomName: String?)

    fun getLastActiveTab(): Int
    fun saveLastActiveTab(tab: Int)

    fun getDraftMessage(roomId: String): String?
    fun saveDraftMessage(roomId: String, draft: String?)

    // Home Screen Widget Preferences
    fun isWidgetOnlineCountEnabled(): Boolean
    fun setWidgetOnlineCountEnabled(enabled: Boolean)

    fun isWidgetIdentityEnabled(): Boolean
    fun setWidgetIdentityEnabled(enabled: Boolean)

    fun isWidgetQuickMatchEnabled(): Boolean
    fun setWidgetQuickMatchEnabled(enabled: Boolean)

    fun getWidgetUpdateIntervalMinutes(): Int
    fun setWidgetUpdateIntervalMinutes(minutes: Int)

    // System Broadcast Deduplication & Dismissal Tracking
    fun isBroadcastDismissed(broadcastId: String): Boolean = false
    fun markBroadcastDismissed(broadcastId: String) {}

    // Group Customization & Local Overrides (Rename persistence & Private group privacy tracking)
    fun getCustomRoomName(roomId: String): String? = null
    fun saveCustomRoomName(roomId: String, name: String?) {}

    fun isRoomLocallyMarkedPrivate(roomId: String): Boolean = false
    fun markRoomLocallyPrivate(roomId: String, isPrivate: Boolean = true) {}

    // My Rooms & Group Ownership Persistence
    fun addMyRoom(roomId: String, isOwner: Boolean = true, isPrivate: Boolean = false) {}
    fun removeMyRoom(roomId: String) {}
    fun markRoomDeleted(roomId: String) { removeMyRoom(roomId) }
    fun isRoomDeleted(roomId: String): Boolean = false
    fun isMyRoom(roomId: String): Boolean = false
    fun isMyOwnedRoom(roomId: String): Boolean = false
    fun getMyRoomIds(): Set<String> = emptySet()
    fun getMyOwnedRoomIds(): Set<String> = emptySet()
    fun saveRoomSnapshot(room: org.example.annochat.data.model.RoomDto) {}
    fun getSavedRoom(roomId: String): org.example.annochat.data.model.RoomDto? = null
    fun getAllSavedRooms(): List<org.example.annochat.data.model.RoomDto> = emptyList()

    // Room Messages Persistence & Caching
    fun saveRoomMessages(roomId: String, messages: List<org.example.annochat.data.model.MessageDto>) {}
    fun getRoomMessages(roomId: String): List<org.example.annochat.data.model.MessageDto> = emptyList()
}

/**
 * In-memory fallback / standard implementation of SessionStorage.
 */
class InMemorySessionStorage(
    private var cachedSessionId: String? = null,
    private var onboardingCompleted: Boolean = false,
    private var onboardingIntent: String? = null,
    private var onboardingVibe: String? = null,
    private var onboardingInterest: String? = null,
    private var ahaMomentCompleted: Boolean = false,
    private var premiumScreenShown: Boolean = false,
    private var liquidGlassEnabled: Boolean = true,
    private var rotatingGlowEnabled: Boolean = true,
    private var animationsEnabled: Boolean = true,
    private var messageNotificationsEnabled: Boolean = true,
    private var privateChatRequestsEnabled: Boolean = true,
    private var notificationPreviewEnabled: Boolean = true,
    private var customBackendUrl: String? = null,
    private var savedPublicId: String? = null,
    private var savedAnonName: String? = null,
    private var lastActiveRoomId: String? = null,
    private var lastActiveRoomName: String? = null,
    private var lastActiveTab: Int = 0,
    private val draftMessages: MutableMap<String, String> = mutableMapOf(),
    private var widgetOnlineCountEnabled: Boolean = true,
    private var widgetIdentityEnabled: Boolean = true,
    private var widgetQuickMatchEnabled: Boolean = true,
    private var widgetUpdateIntervalMinutes: Int = 30,
    private val dismissedBroadcastIds: MutableSet<String> = mutableSetOf(),
    private val roomMessagesMap: MutableMap<String, List<org.example.annochat.data.model.MessageDto>> = mutableMapOf()
) : SessionStorage {
    override fun saveRoomMessages(roomId: String, messages: List<org.example.annochat.data.model.MessageDto>) {
        roomMessagesMap[roomId] = messages
    }

    override fun getRoomMessages(roomId: String): List<org.example.annochat.data.model.MessageDto> {
        return roomMessagesMap[roomId] ?: emptyList()
    }

    override fun getOrCreateSessionId(): String {
        cachedSessionId?.let { return it }
        val newSessionId = generateAnonymousSessionId()
        cachedSessionId = newSessionId
        return newSessionId
    }

    override fun getSessionId(): String? = cachedSessionId

    override fun saveSessionId(sessionId: String) {
        cachedSessionId = sessionId
    }

    override fun clearSession() {
        cachedSessionId = null
    }

    override fun isOnboardingCompleted(): Boolean = onboardingCompleted

    override fun setOnboardingCompleted(completed: Boolean) {
        onboardingCompleted = completed
    }

    override fun getOnboardingIntent(): String? = onboardingIntent

    override fun setOnboardingIntent(intent: String?) {
        onboardingIntent = intent
    }

    override fun getOnboardingVibe(): String? = onboardingVibe

    override fun setOnboardingVibe(vibe: String?) {
        onboardingVibe = vibe
    }

    override fun getOnboardingInterest(): String? = onboardingInterest

    override fun setOnboardingInterest(interest: String?) {
        onboardingInterest = interest
    }

    override fun isAhaMomentCompleted(): Boolean = ahaMomentCompleted

    override fun setAhaMomentCompleted(completed: Boolean) {
        ahaMomentCompleted = completed
    }

    override fun isPremiumScreenShown(): Boolean = premiumScreenShown

    override fun setPremiumScreenShown(shown: Boolean) {
        premiumScreenShown = shown
    }

    override fun getOnboardingState(): OnboardingState {
        return OnboardingState(
            intent = onboardingIntent,
            vibe = onboardingVibe,
            conversationInterest = onboardingInterest,
            completedPhase1 = !onboardingIntent.isNullOrBlank() && !onboardingVibe.isNullOrBlank() && !onboardingInterest.isNullOrBlank(),
            isAhaMomentCompleted = ahaMomentCompleted,
            isPremiumScreenShown = premiumScreenShown
        )
    }

    override fun saveOnboardingState(state: OnboardingState) {
        onboardingIntent = state.intent
        onboardingVibe = state.vibe
        onboardingInterest = state.conversationInterest
        ahaMomentCompleted = state.isAhaMomentCompleted
        premiumScreenShown = state.isPremiumScreenShown
    }

    override fun isLiquidGlassEnabled(): Boolean = liquidGlassEnabled

    override fun setLiquidGlassEnabled(enabled: Boolean) {
        liquidGlassEnabled = enabled
    }

    override fun isRotatingGlowEnabled(): Boolean = rotatingGlowEnabled

    override fun setRotatingGlowEnabled(enabled: Boolean) {
        rotatingGlowEnabled = enabled
    }

    override fun isAnimationsEnabled(): Boolean = animationsEnabled

    override fun setAnimationsEnabled(enabled: Boolean) {
        animationsEnabled = enabled
    }

    override fun isMessageNotificationsEnabled(): Boolean = messageNotificationsEnabled

    override fun setMessageNotificationsEnabled(enabled: Boolean) {
        messageNotificationsEnabled = enabled
    }

    override fun isPrivateChatRequestsEnabled(): Boolean = privateChatRequestsEnabled

    override fun setPrivateChatRequestsEnabled(enabled: Boolean) {
        privateChatRequestsEnabled = enabled
    }

    override fun isNotificationPreviewEnabled(): Boolean = notificationPreviewEnabled

    override fun setNotificationPreviewEnabled(enabled: Boolean) {
        notificationPreviewEnabled = enabled
    }

    private var appearanceSettings: org.example.annochat.data.model.AppearanceSettings = org.example.annochat.data.model.AppearanceSettings()

    override fun getAppearanceSettings(): org.example.annochat.data.model.AppearanceSettings = appearanceSettings

    override fun saveAppearanceSettings(settings: org.example.annochat.data.model.AppearanceSettings) {
        appearanceSettings = settings
    }

    override fun restoreDefaultAppearance() {
        appearanceSettings = org.example.annochat.data.model.AppearanceSettings()
    }

    override fun getCustomBackendUrl(): String? = customBackendUrl

    override fun setCustomBackendUrl(url: String?) {
        customBackendUrl = url?.trim()?.ifBlank { null }
    }

    override fun getSavedPublicId(): String? = savedPublicId

    override fun getSavedAnonName(): String? = savedAnonName

    override fun saveIdentity(publicId: String, anonName: String) {
        savedPublicId = publicId
        savedAnonName = anonName
    }

    override fun getLastActiveRoomId(): String? = lastActiveRoomId

    override fun getLastActiveRoomName(): String? = lastActiveRoomName

    override fun saveLastActiveRoom(roomId: String?, roomName: String?) {
        lastActiveRoomId = roomId
        lastActiveRoomName = roomName
    }

    override fun getLastActiveTab(): Int = lastActiveTab

    override fun saveLastActiveTab(tab: Int) {
        lastActiveTab = tab
    }

    override fun getDraftMessage(roomId: String): String? = draftMessages[roomId]

    override fun saveDraftMessage(roomId: String, draft: String?) {
        if (draft.isNullOrBlank()) {
            draftMessages.remove(roomId)
        } else {
            draftMessages[roomId] = draft
        }
    }

    override fun isWidgetOnlineCountEnabled(): Boolean = widgetOnlineCountEnabled
    override fun setWidgetOnlineCountEnabled(enabled: Boolean) {
        widgetOnlineCountEnabled = enabled
    }

    override fun isWidgetIdentityEnabled(): Boolean = widgetIdentityEnabled
    override fun setWidgetIdentityEnabled(enabled: Boolean) {
        widgetIdentityEnabled = enabled
    }

    override fun isWidgetQuickMatchEnabled(): Boolean = widgetQuickMatchEnabled
    override fun setWidgetQuickMatchEnabled(enabled: Boolean) {
        widgetQuickMatchEnabled = enabled
    }

    override fun getWidgetUpdateIntervalMinutes(): Int = widgetUpdateIntervalMinutes
    override fun setWidgetUpdateIntervalMinutes(minutes: Int) {
        widgetUpdateIntervalMinutes = minutes
    }

    override fun isBroadcastDismissed(broadcastId: String): Boolean = dismissedBroadcastIds.contains(broadcastId)

    override fun markBroadcastDismissed(broadcastId: String) {
        dismissedBroadcastIds.add(broadcastId)
    }

    private val customRoomNames = mutableMapOf<String, String>()
    private val locallyPrivateRooms = mutableSetOf<String>()

    override fun getCustomRoomName(roomId: String): String? = customRoomNames[roomId]

    override fun saveCustomRoomName(roomId: String, name: String?) {
        if (name.isNullOrBlank()) {
            customRoomNames.remove(roomId)
        } else {
            customRoomNames[roomId] = name.trim()
        }
    }

    override fun isRoomLocallyMarkedPrivate(roomId: String): Boolean = locallyPrivateRooms.contains(roomId)

    override fun markRoomLocallyPrivate(roomId: String, isPrivate: Boolean) {
        if (isPrivate) {
            locallyPrivateRooms.add(roomId)
        } else {
            locallyPrivateRooms.remove(roomId)
        }
    }

    private val myRoomIds = mutableSetOf<String>()
    private val myOwnedRoomIds = mutableSetOf<String>()
    private val savedRoomsMap = mutableMapOf<String, org.example.annochat.data.model.RoomDto>()

    override fun addMyRoom(roomId: String, isOwner: Boolean, isPrivate: Boolean) {
        if (roomId.isBlank()) return
        myRoomIds.add(roomId)
        if (isOwner) myOwnedRoomIds.add(roomId)
        if (isPrivate) locallyPrivateRooms.add(roomId)
    }

    private val deletedRoomIds = mutableSetOf<String>()

    override fun removeMyRoom(roomId: String) {
        myRoomIds.remove(roomId)
        myOwnedRoomIds.remove(roomId)
        savedRoomsMap.remove(roomId)
        locallyPrivateRooms.remove(roomId)
    }

    override fun markRoomDeleted(roomId: String) {
        deletedRoomIds.add(roomId)
        removeMyRoom(roomId)
    }

    override fun isRoomDeleted(roomId: String): Boolean = deletedRoomIds.contains(roomId)

    override fun isMyRoom(roomId: String): Boolean = myRoomIds.contains(roomId)

    override fun isMyOwnedRoom(roomId: String): Boolean = myOwnedRoomIds.contains(roomId)

    override fun getMyRoomIds(): Set<String> = myRoomIds.toSet()

    override fun getMyOwnedRoomIds(): Set<String> = myOwnedRoomIds.toSet()

    override fun saveRoomSnapshot(room: org.example.annochat.data.model.RoomDto) {
        if (room.roomId.isBlank()) return
        addMyRoom(room.roomId, isOwner = room.isOwner, isPrivate = !room.isPublic || room.isPrivateGroup)
        savedRoomsMap[room.roomId] = room
    }

    override fun getSavedRoom(roomId: String): org.example.annochat.data.model.RoomDto? = savedRoomsMap[roomId]

    override fun getAllSavedRooms(): List<org.example.annochat.data.model.RoomDto> = savedRoomsMap.values.toList()

    companion object {
        fun generateAnonymousSessionId(): String {
            val secureHex = org.example.annochat.generateSecureUuidString().replace("-", "").take(16).lowercase()
            return "sess_$secureHex"
        }
    }
}

/**
 * Provides platform-backed persistent session storage in Compose Multiplatform.
 * Uses SharedPreferences on Android, NSUserDefaults on iOS, and in-memory fallback on tests/desktop.
 */
@androidx.compose.runtime.Composable
expect fun rememberPlatformSessionStorage(): SessionStorage
