package org.example.annochat.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class AfterClassMode(
    val title: String,
    val subtitle: String,
    val description: String
) {
    AFTER_HOURS(
        title = "After Hours",
        subtitle = "The campus after everyone leaves.",
        description = "Signature minimal dark environment with moonlight silver accents and calm motion."
    ),
    MIDNIGHT_CAMPUS(
        title = "Midnight Campus",
        subtitle = "Deeper. Calmer. More atmospheric.",
        description = "Darker cosmic depth with night violet / campus cyan accents and deep atmosphere."
    ),
    STUDY_BREAK(
        title = "Study Break",
        subtitle = "Minimal. Focused. Distraction free.",
        description = "Simplified background, lower visual intensity, minimal motion, and maximum focus."
    ),
    FIRST_LIGHT(
        title = "First Light",
        subtitle = "A new day. Same anonymity.",
        description = "Subtle dawn-inspired atmosphere with warm sunrise accents and crisp readability."
    )
}

@Serializable
enum class CampusEnvironment(
    val title: String,
    val subtitle: String
) {
    CAMPUS_CORE("Campus Core", "Our signature look"),
    LIBRARY_LIGHTS("Library Lights", "Subtle campus glow"),
    HOSTEL_NIGHTS("Hostel Nights", "Warm and quiet"),
    STARGAZER("Stargazer", "Expansive and deep")
}

@Serializable
enum class AfterClassAccent(
    val title: String,
    val colorHex: Long,
    val secondaryHex: Long,
    val description: String
) {
    MOONLIGHT("Moonlight", 0xFFFFFFFF, 0xFFE0E0E0, "Soft white / silver"),
    CAMPUS_CYAN("Campus Cyan", 0xFF00E5FF, 0xFF4DD0E1, "Cool futuristic cyan"),
    NIGHT_VIOLET("Night Violet", 0xFFB388FF, 0xFF7C4DFF, "Deep violet"),
    SIGNAL_BLUE("Signal Blue", 0xFF448AFF, 0xFF2979FF, "Cool electric blue"),
    AFTERGLOW("Afterglow", 0xFFFFB703, 0xFFFFC107, "Warm restrained amber"),
    SUNRISE("Sunrise", 0xFFFF6E40, 0xFFFF8A80, "Soft warm rose / orange")
}

@Serializable
enum class CampusEnergy(
    val title: String,
    val subtitle: String
) {
    QUIET("Quiet", "Minimal motion"),
    AFTER_HOURS("After Hours", "Balanced"),
    ALIVE("Alive", "More expressive")
}

@Serializable
enum class IdentitySignature(
    val title: String,
    val subtitle: String
) {
    NIGHT_OWL("Night Owl", "Animals"),
    GLYPH("Glyph", "Symbols"),
    ABSTRACT("Abstract", "Geometric"),
    MINIMAL("Minimal", "Clean")
}

@Serializable
enum class IdentityRing(
    val title: String,
    val subtitle: String
) {
    NONE("None", "No ring"),
    SUBTLE("Subtle", "Clean"),
    ACCENT_GLOW("Accent Glow", "Highlights")
}

@Serializable
enum class AppTextSize(
    val title: String,
    val subtitle: String,
    val scaleMultiplier: Float
) {
    COMPACT("Compact", "More content", 0.95f),
    DEFAULT("Default", "Balanced", 1.06f),
    LARGE("Large", "Better readability", 1.18f)
}

@Serializable
data class AppearanceSettings(
    val activeMode: AfterClassMode = AfterClassMode.AFTER_HOURS,
    val environment: CampusEnvironment = CampusEnvironment.CAMPUS_CORE,
    val environmentIntensity: Float = 0.5f, // 0.0f (Subtle) to 1.0f (Immersive)
    val accent: AfterClassAccent = AfterClassAccent.MOONLIGHT,
    val afterglowEnabled: Boolean = true,
    val glowIntensity: Float = 0.65f, // 0.2f (Subtle) to 1.0f (Strong)
    val glowSpeed: Float = 1.0f, // 0.5f (Slower) to 1.5f (Faster)
    val campusEnergy: CampusEnergy = CampusEnergy.AFTER_HOURS,
    val animationsEnabled: Boolean = true,
    val orbitEnabled: Boolean = true,
    val ambientEffectsEnabled: Boolean = true,
    val identitySignature: IdentitySignature = IdentitySignature.NIGHT_OWL,
    val identityRing: IdentityRing = IdentityRing.ACCENT_GLOW,
    val textSize: AppTextSize = AppTextSize.DEFAULT
) {
    companion object {
        private val jsonFormat = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

        fun toJson(settings: AppearanceSettings): String {
            return jsonFormat.encodeToString(serializer(), settings)
        }

        fun fromJson(json: String?): AppearanceSettings {
            if (json.isNullOrBlank()) return AppearanceSettings()
            return try {
                jsonFormat.decodeFromString(serializer(), json)
            } catch (_: Exception) {
                AppearanceSettings()
            }
        }

        fun forMode(mode: AfterClassMode): AppearanceSettings {
            return when (mode) {
                AfterClassMode.AFTER_HOURS -> AppearanceSettings(
                    activeMode = AfterClassMode.AFTER_HOURS,
                    environment = CampusEnvironment.CAMPUS_CORE,
                    environmentIntensity = 0.5f,
                    accent = AfterClassAccent.MOONLIGHT,
                    afterglowEnabled = true,
                    glowIntensity = 0.65f,
                    glowSpeed = 1.0f,
                    campusEnergy = CampusEnergy.AFTER_HOURS,
                    animationsEnabled = true,
                    orbitEnabled = true,
                    ambientEffectsEnabled = true,
                    identitySignature = IdentitySignature.NIGHT_OWL,
                    identityRing = IdentityRing.ACCENT_GLOW,
                    textSize = AppTextSize.DEFAULT
                )
                AfterClassMode.MIDNIGHT_CAMPUS -> AppearanceSettings(
                    activeMode = AfterClassMode.MIDNIGHT_CAMPUS,
                    environment = CampusEnvironment.STARGAZER,
                    environmentIntensity = 0.8f,
                    accent = AfterClassAccent.NIGHT_VIOLET,
                    afterglowEnabled = true,
                    glowIntensity = 0.85f,
                    glowSpeed = 0.8f,
                    campusEnergy = CampusEnergy.AFTER_HOURS,
                    animationsEnabled = true,
                    orbitEnabled = true,
                    ambientEffectsEnabled = true,
                    identitySignature = IdentitySignature.NIGHT_OWL,
                    identityRing = IdentityRing.ACCENT_GLOW,
                    textSize = AppTextSize.DEFAULT
                )
                AfterClassMode.STUDY_BREAK -> AppearanceSettings(
                    activeMode = AfterClassMode.STUDY_BREAK,
                    environment = CampusEnvironment.LIBRARY_LIGHTS,
                    environmentIntensity = 0.2f,
                    accent = AfterClassAccent.MOONLIGHT,
                    afterglowEnabled = false,
                    glowIntensity = 0.3f,
                    glowSpeed = 0.5f,
                    campusEnergy = CampusEnergy.QUIET,
                    animationsEnabled = true,
                    orbitEnabled = false,
                    ambientEffectsEnabled = false,
                    identitySignature = IdentitySignature.MINIMAL,
                    identityRing = IdentityRing.SUBTLE,
                    textSize = AppTextSize.DEFAULT
                )
                AfterClassMode.FIRST_LIGHT -> AppearanceSettings(
                    activeMode = AfterClassMode.FIRST_LIGHT,
                    environment = CampusEnvironment.HOSTEL_NIGHTS,
                    environmentIntensity = 0.65f,
                    accent = AfterClassAccent.SUNRISE,
                    afterglowEnabled = true,
                    glowIntensity = 0.75f,
                    glowSpeed = 1.2f,
                    campusEnergy = CampusEnergy.ALIVE,
                    animationsEnabled = true,
                    orbitEnabled = true,
                    ambientEffectsEnabled = true,
                    identitySignature = IdentitySignature.GLYPH,
                    identityRing = IdentityRing.ACCENT_GLOW,
                    textSize = AppTextSize.DEFAULT
                )
            }
        }
    }
}

