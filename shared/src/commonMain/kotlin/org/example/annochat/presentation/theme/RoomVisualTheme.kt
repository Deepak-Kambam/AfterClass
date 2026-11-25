package org.example.annochat.presentation.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlinproject.shared.generated.resources.Res
import kotlinproject.shared.generated.resources.room_channel_civil
import kotlinproject.shared.generated.resources.room_channel_cse
import kotlinproject.shared.generated.resources.room_channel_ece
import kotlinproject.shared.generated.resources.room_channel_faculty
import kotlinproject.shared.generated.resources.room_channel_hostel
import kotlinproject.shared.generated.resources.room_channel_mech
import kotlinproject.shared.generated.resources.room_channel_public
import org.example.annochat.data.model.PublicRoomDto
import org.jetbrains.compose.resources.DrawableResource

/**
 * Ambient color palette extracted and tailored for each room channel.
 * Designed with soft, muted, low-saturation tones to maintain a calm,
 * non-disruptive, ultra-premium dark aesthetic.
 */
@Immutable
data class RoomAmbientPalette(
    val primaryGlow: Color,
    val secondaryGlow: Color,
    val vignetteOverlay: Color = Color(0xFF030305),
    val cardBorderGlow: Color,
    val cardSheen: Color
)

object RoomVisualRegistry {

    private val PUBLIC_PALETTE = RoomAmbientPalette(
        primaryGlow = Color(0xFF2F4D75),    // Rich atmospheric dusk slate blue
        secondaryGlow = Color(0xFF1E3352),  // Deep twilight oceanic
        cardBorderGlow = Color(0xFF6293C9).copy(alpha = 0.55f),
        cardSheen = Color(0xFF7CAFE8).copy(alpha = 0.16f)
    )

    private val CSE_PALETTE = RoomAmbientPalette(
        primaryGlow = Color(0xFF225164),    // Rich dusky slate cyan
        secondaryGlow = Color(0xFF163745),  // Deep oceanic slate
        cardBorderGlow = Color(0xFF53A5C7).copy(alpha = 0.55f),
        cardSheen = Color(0xFF6BBEEC).copy(alpha = 0.16f)
    )

    private val ECE_PALETTE = RoomAmbientPalette(
        primaryGlow = Color(0xFF1E565B),    // Circuit cyan & emerald teal
        secondaryGlow = Color(0xFF133B3F),  // Deep charcoal teal
        cardBorderGlow = Color(0xFF4CAEB9).copy(alpha = 0.55f),
        cardSheen = Color(0xFF64C5D0).copy(alpha = 0.16f)
    )

    private val MECH_PALETTE = RoomAmbientPalette(
        primaryGlow = Color(0xFF48413A),    // Warm graphite & titanium smoke
        secondaryGlow = Color(0xFF302B26),  // Deep carbon bronze
        cardBorderGlow = Color(0xFF948A80).copy(alpha = 0.55f),
        cardSheen = Color(0xFFAE9E92).copy(alpha = 0.16f)
    )

    private val CIVIL_PALETTE = RoomAmbientPalette(
        primaryGlow = Color(0xFF384454),    // Architectural stone & dusk slate
        secondaryGlow = Color(0xFF242C38),  // Deep dusk steel
        cardBorderGlow = Color(0xFF758CA7).copy(alpha = 0.55f),
        cardSheen = Color(0xFF8DA7C8).copy(alpha = 0.16f)
    )

    private val FACULTY_PALETTE = RoomAmbientPalette(
        primaryGlow = Color(0xFF503E2D),    // Warm dusk amber mahogany & study leather
        secondaryGlow = Color(0xFF36291C),  // Deep sepia
        cardBorderGlow = Color(0xFFA88562).copy(alpha = 0.55f),
        cardSheen = Color(0xFFCAA279).copy(alpha = 0.16f)
    )

    private val HOSTEL_PALETTE = RoomAmbientPalette(
        primaryGlow = Color(0xFF453660),    // Cozy dusk lavender & midnight navy
        secondaryGlow = Color(0xFF2E2342),  // Deep twilight violet
        cardBorderGlow = Color(0xFF9578C5).copy(alpha = 0.55f),
        cardSheen = Color(0xFFB191E5).copy(alpha = 0.16f)
    )

    private val DEFAULT_PALETTE = RoomAmbientPalette(
        primaryGlow = Color(0xFF343846),
        secondaryGlow = Color(0xFF20232E),
        cardBorderGlow = Color(0xFF717B98).copy(alpha = 0.45f),
        cardSheen = Color(0xFF8896B8).copy(alpha = 0.14f)
    )

    fun resolveCategoryKey(id: String, name: String, desc: String?): String {
        val combined = "${id.lowercase()} ${name.lowercase()} ${(desc ?: "").lowercase()}"
        return when {
            combined.contains("cse") || combined.contains("code") || combined.contains("comp") || combined.contains("terminal") -> "cse"
            combined.contains("ece") || combined.contains("elect") || combined.contains("chip") || combined.contains("circuit") -> "ece"
            combined.contains("mech") || combined.contains("gear") || combined.contains("tool") || combined.contains("machin") -> "mech"
            combined.contains("civil") || combined.contains("build") || combined.contains("arch") || combined.contains("struct") -> "civil"
            combined.contains("faculty") || combined.contains("prof") || combined.contains("acad") || combined.contains("lecture") -> "faculty"
            combined.contains("hostel") || combined.contains("dorm") || combined.contains("bed") || combined.contains("night") -> "hostel"
            combined.contains("public") || combined.contains("anon") || combined.contains("main") || combined.contains("campus") -> "public"
            else -> "default"
        }
    }

    fun getPalette(room: PublicRoomDto): RoomAmbientPalette {
        val key = resolveCategoryKey(room.id, room.name, room.desc)
        return when (key) {
            "cse" -> CSE_PALETTE
            "ece" -> ECE_PALETTE
            "mech" -> MECH_PALETTE
            "civil" -> CIVIL_PALETTE
            "faculty" -> FACULTY_PALETTE
            "hostel" -> HOSTEL_PALETTE
            "public" -> PUBLIC_PALETTE
            else -> DEFAULT_PALETTE
        }
    }

    fun getImage(room: PublicRoomDto): DrawableResource {
        val key = resolveCategoryKey(room.id, room.name, room.desc)
        return when (key) {
            "cse" -> Res.drawable.room_channel_cse
            "ece" -> Res.drawable.room_channel_ece
            "mech" -> Res.drawable.room_channel_mech
            "civil" -> Res.drawable.room_channel_civil
            "faculty" -> Res.drawable.room_channel_faculty
            "hostel" -> Res.drawable.room_channel_hostel
            else -> Res.drawable.room_channel_public
        }
    }

    /**
     * Interpolates smoothly between two ambient palettes with FastOutSlowInEasing.
     */
    fun interpolatePalettes(
        start: RoomAmbientPalette,
        end: RoomAmbientPalette,
        fraction: Float
    ): RoomAmbientPalette {
        val eased = FastOutSlowInEasing.transform(fraction.coerceIn(0f, 1f))
        return RoomAmbientPalette(
            primaryGlow = lerpColor(start.primaryGlow, end.primaryGlow, eased),
            secondaryGlow = lerpColor(start.secondaryGlow, end.secondaryGlow, eased),
            vignetteOverlay = lerpColor(start.vignetteOverlay, end.vignetteOverlay, eased),
            cardBorderGlow = lerpColor(start.cardBorderGlow, end.cardBorderGlow, eased),
            cardSheen = lerpColor(start.cardSheen, end.cardSheen, eased)
        )
    }

    fun lerpColor(start: Color, end: Color, fraction: Float): Color {
        val f = fraction.coerceIn(0f, 1f)
        return Color(
            red = start.red + f * (end.red - start.red),
            green = start.green + f * (end.green - start.green),
            blue = start.blue + f * (end.blue - start.blue),
            alpha = start.alpha + f * (end.alpha - start.alpha)
        )
    }
}
