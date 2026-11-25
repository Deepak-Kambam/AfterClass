package org.example.annochat.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Robust serializer that gracefully accepts booleans, integers (0/1), or strings ("true"/"1")
 * without throwing JSON token type mismatches.
 */
object BooleanLenientSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BooleanLenient", PrimitiveKind.BOOLEAN)

    override fun deserialize(decoder: Decoder): Boolean {
        return if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            if (element is JsonPrimitive) {
                element.booleanOrNull
                    ?: (element.intOrNull?.let { it == 1 })
                    ?: (element.content.equals("true", ignoreCase = true) || element.content == "1")
            } else {
                true
            }
        } else {
            try {
                decoder.decodeBoolean()
            } catch (_: Exception) {
                try {
                    decoder.decodeInt() == 1
                } catch (_: Exception) {
                    true
                }
            }
        }
    }

    override fun serialize(encoder: Encoder, value: Boolean) {
        encoder.encodeBoolean(value)
    }
}

/**
 * Robust serializer that gracefully accepts integers, booleans (true=1, false=0), or numeric strings.
 */
object IntLenientSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("IntLenient", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int {
        return if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            if (element is JsonPrimitive) {
                element.intOrNull
                    ?: (element.booleanOrNull?.let { if (it) 1 else 0 })
                    ?: (if (element.content.equals("true", ignoreCase = true) || element.content == "1") 1 else 0)
            } else {
                1
            }
        } else {
            try {
                decoder.decodeInt()
            } catch (_: Exception) {
                try {
                    if (decoder.decodeBoolean()) 1 else 0
                } catch (_: Exception) {
                    1
                }
            }
        }
    }

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeInt(value)
    }
}

/**
 * Robust serializer that handles String or numeric IDs (e.g. 123 -> "123") for non-nullable String fields.
 */
object StringLenientSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("StringLenient", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        return if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            if (element is JsonPrimitive) {
                element.content
            } else {
                ""
            }
        } else {
            try {
                decoder.decodeString()
            } catch (_: Exception) {
                ""
            }
        }
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}

/**
 * Robust serializer that handles String or numeric IDs (e.g. 123 -> "123") for nullable String? fields.
 */
object NullableStringLenientSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NullableStringLenient", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String? {
        return if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            if (element is JsonPrimitive) {
                element.content
            } else {
                null
            }
        } else {
            try {
                decoder.decodeString()
            } catch (_: Exception) {
                null
            }
        }
    }

    override fun serialize(encoder: Encoder, value: String?) {
        if (value != null) encoder.encodeString(value) else encoder.encodeNull()
    }
}

enum class RoomActivityLevel {
    IDLE,
    LOW_ACTIVITY,
    ACTIVE,
    VERY_ACTIVE
}

object PredefinedChannels {
    val IDS: Set<String> = setOf("main", "cse", "ece", "mech", "civil", "faculty", "hostel")

    val DEFAULT_CHANNELS: List<PublicRoomDto> = listOf(
        PublicRoomDto(id = "main", name = "Public Chat", desc = "Campus stream for #Public Chat"),
        PublicRoomDto(id = "cse", name = "CSE Stream", desc = "Campus stream for #CSE Stream"),
        PublicRoomDto(id = "ece", name = "Electronics Stream", desc = "Campus stream for #Electronics Stream"),
        PublicRoomDto(id = "mech", name = "Mechanical Stream", desc = "Campus stream for #Mechanical Stream"),
        PublicRoomDto(id = "civil", name = "Civil Stream", desc = "Campus stream for #Civil Stream"),
        PublicRoomDto(id = "faculty", name = "Faculty", desc = "Campus stream for #Faculty"),
        PublicRoomDto(id = "hostel", name = "Hostel", desc = "Campus stream for #Hostel")
    )

    fun isPredefined(roomId: String?): Boolean {
        if (roomId.isNullOrBlank()) return false
        val clean = roomId.trim().lowercase()
        return clean in IDS
    }
}

/**
 * Authoritative room matching logic.
 * Strictly scopes message, history, and realtime event operations to the specified room identifier.
 * Prevents cross-contamination between Public, Private, and Custom groups.
 */
fun isRoomMatching(roomA: String?, roomB: String?): Boolean {
    val a = (roomA ?: "").trim().removePrefix("#").lowercase()
    val b = (roomB ?: "").trim().removePrefix("#").lowercase()
    if (a.isEmpty() && b.isEmpty()) return true
    if (a == b) return true
    if ((b.isBlank() || b == "main") && (a.isBlank() || a == "main")) return true
    val aClean = a.replace(Regex("[^a-z0-9]"), "")
    val bClean = b.replace(Regex("[^a-z0-9]"), "")
    return aClean.isNotEmpty() && aClean == bClean
}

@Immutable
@Serializable
data class PublicRoomDto(
    @Serializable(with = StringLenientSerializer::class)
    @SerialName("id")
    val id: String = "",

    @SerialName("name")
    val name: String = "",

    @SerialName("icon")
    val icon: String? = null,

    @SerialName("desc")
    val desc: String? = null,

    @SerialName("description")
    val altDesc: String? = null,

    @Serializable(with = IntLenientSerializer::class)
    @SerialName("member_count")
    val memberCount: Int = 0
) {
    val effectiveDesc: String?
        get() = desc ?: altDesc

    val isPredefinedChannel: Boolean
        get() = PredefinedChannels.isPredefined(id)
}

@Immutable
@Serializable
data class RoomMemberDto(
    @Serializable(with = NullableStringLenientSerializer::class)
    @SerialName("room_id")
    val roomId: String? = null,

    @Serializable(with = StringLenientSerializer::class)
    @SerialName("public_id")
    val publicId: String = "",

    @SerialName("anon_name")
    val anonName: String? = null,

    @SerialName("anon_avatar")
    val anonAvatar: String? = null,

    @SerialName("joined_at")
    val joinedAt: String? = null,

    @Serializable(with = IntLenientSerializer::class)
    @SerialName("is_owner")
    val isOwner: Int = 0,

    @SerialName("role")
    val role: String = "MEMBER"
) {
    val isAdmin: Boolean
        get() = isOwner == 1 || role.equals("ADMIN", ignoreCase = true) || role.equals("OWNER", ignoreCase = true)
}

@Immutable
@Serializable
data class RoomDto(
    @Serializable(with = NullableStringLenientSerializer::class)
    @SerialName("room_id")
    val rawRoomId: String? = null,

    @Serializable(with = NullableStringLenientSerializer::class)
    @SerialName("id")
    val altRoomId: String? = null,

    @SerialName("name")
    val name: String = "",

    @SerialName("desc")
    val desc: String? = null,

    @SerialName("description")
    val altDesc: String? = null,

    @SerialName("created_by")
    val createdBy: String? = null,

    @SerialName("owner_public_id")
    val ownerPublicId: String? = null,

    @Serializable(with = BooleanLenientSerializer::class)
    @SerialName("is_public")
    val isPublic: Boolean = true,

    @Serializable(with = BooleanLenientSerializer::class)
    @SerialName("is_owner")
    val isOwner: Boolean = false,

    @Serializable(with = BooleanLenientSerializer::class)
    @SerialName("is_admin")
    val rawIsAdmin: Boolean = false,

    @Serializable(with = BooleanLenientSerializer::class)
    @SerialName("is_member")
    val isMember: Boolean = false,

    @SerialName("created_at")
    val createdAt: String? = null,

    @Serializable(with = IntLenientSerializer::class)
    @SerialName("is_active")
    val isActive: Int = 1,

    @SerialName("members")
    val members: List<RoomMemberDto> = emptyList(),

    @Serializable(with = IntLenientSerializer::class)
    @SerialName("member_count")
    val rawMemberCount: Int = 0,

    @Serializable(with = IntLenientSerializer::class)
    @SerialName("message_count")
    val messageCount: Int = 0
) {
    val roomId: String
        get() = rawRoomId ?: altRoomId ?: ""

    val effectiveDesc: String?
        get() = desc ?: altDesc

    val memberCount: Int
        get() = if (rawMemberCount > 0) rawMemberCount else members.size

    val effectiveOwnerPublicId: String?
        get() = ownerPublicId ?: createdBy

    val cleanDisplayName: String
        get() {
            val n = name.trim()
            return when {
                n.startsWith("prv_", ignoreCase = true) -> n.substring(4).trim()
                n.startsWith("pv_", ignoreCase = true) -> n.substring(3).trim()
                n.startsWith("[Private] ", ignoreCase = true) -> n.substring(10).trim()
                n.startsWith("[Private]", ignoreCase = true) -> n.substring(9).trim()
                n.startsWith("Private ", ignoreCase = true) -> n.substring(8).trim()
                else -> n
            }
        }

    val isPrivateGroup: Boolean
        get() {
            if (!isPublic) return true
            val n = name.trim()
            if (n.startsWith("prv_", ignoreCase = true) ||
                n.startsWith("pv_", ignoreCase = true) ||
                n.startsWith("[Private]", ignoreCase = true) ||
                n.startsWith("Private ", ignoreCase = true)) {
                return true
            }
            if (effectiveDesc?.startsWith("[Private]", ignoreCase = true) == true ||
                effectiveDesc?.contains("__private__", ignoreCase = true) == true) {
                return true
            }
            return false
        }

    val isPredefinedChannel: Boolean
        get() = PredefinedChannels.isPredefined(roomId)

    /**
     * Determines whether the specified user belongs to or is associated with this room.
     * For private rooms, we do NOT trust raw server `isMember` (which can be falsely true due to shared Wi-Fi IP),
     * and strictly verify membership via local storage or actual member public ID.
     */
    fun isUserAssociated(myPublicId: String?, isLocallyStoredMyRoom: Boolean = false): Boolean {
        if (isLocallyStoredMyRoom) return true
        if (isOwner) return true
        val pid = myPublicId?.trim()
        if (!pid.isNullOrBlank()) {
            if (ownerPublicId == pid || createdBy == pid) return true
            if (members.any { it.publicId == pid }) return true
        }
        // Only allow fallback to server `isMember` if the room is genuinely public.
        if (!isPrivateGroup && isPublic && isMember) return true
        return false
    }

    /**
     * Determines whether the given user has Group Admin/Owner authority over this group.
     */
    fun isUserAdmin(myPublicId: String?, isLocallyStoredOwner: Boolean = false): Boolean {
        if (isLocallyStoredOwner) return true
        val pid = myPublicId?.trim()
        if (!pid.isNullOrBlank()) {
            val ownerId = effectiveOwnerPublicId?.trim()
            if (!ownerId.isNullOrBlank() && ownerId.equals(pid, ignoreCase = true)) {
                return true
            }
            val member = members.firstOrNull { it.publicId.equals(pid, ignoreCase = true) }
            if (member != null) {
                return member.isAdmin
            }
        }
        if (isOwner || rawIsAdmin) return true
        return false
    }

    constructor(
        roomId: String,
        name: String = "",
        desc: String? = null,
        isPublic: Boolean = true,
        isOwner: Boolean = false,
        rawIsAdmin: Boolean = isOwner,
        createdAt: String? = null,
        isActive: Int = 1,
        members: List<RoomMemberDto> = emptyList(),
        messageCount: Int = 0,
        memberCount: Int = 0,
        isMember: Boolean = false
    ) : this(
        rawRoomId = roomId,
        altRoomId = roomId,
        name = name,
        desc = desc,
        altDesc = desc,
        createdBy = null,
        ownerPublicId = null,
        isPublic = isPublic,
        isOwner = isOwner,
        rawIsAdmin = rawIsAdmin,
        isMember = isMember,
        createdAt = createdAt,
        isActive = isActive,
        members = members,
        rawMemberCount = if (memberCount > 0) memberCount else members.size,
        messageCount = messageCount
    )
}

@Serializable
data class CreateRoomRequest(
    @SerialName("session_id")
    val sessionId: String,

    @SerialName("name")
    val name: String,

    @SerialName("desc")
    val desc: String? = null,

    @SerialName("is_public")
    val isPublic: Boolean = true
)

@Serializable
data class CreateRoomResponse(
    @SerialName("status")
    val status: String = "ok",

    @SerialName("room")
    val room: RoomDto? = null,

    @SerialName("detail")
    val detail: String? = null
)

@Serializable
data class PublicRoomsResponse(
    @SerialName("status")
    val status: String = "ok",

    @SerialName("rooms")
    val rooms: List<PublicRoomDto> = emptyList()
)

@Serializable
data class RoomsResponse(
    @SerialName("status")
    val status: String = "ok",

    @SerialName("rooms")
    val rooms: List<RoomDto> = emptyList()
)
