package org.example.annochat.presentation.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.annochat.data.repository.ChatRepository
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.designsystem.AnonChatShapes
import kotlin.math.abs
import kotlinproject.shared.generated.resources.Res
import kotlinproject.shared.generated.resources.onboarding_cheetah
import kotlinproject.shared.generated.resources.onboarding_fox
import kotlinproject.shared.generated.resources.onboarding_panda
import kotlinproject.shared.generated.resources.onboarding_rabbit
import org.jetbrains.compose.resources.painterResource

/**
 * In-memory LRU-like cache for decoded ImageBitmaps to prevent redundant network fetches
 * and ensure instantaneous UI rendering of avatars and chat photos across screens.
 */
object ImageBitmapCache {
    private const val MAX_ENTRIES = 120
    private val cache = LinkedHashMap<String, ImageBitmap>(MAX_ENTRIES, 0.75f)

    fun get(key: String): ImageBitmap? {
        val bitmap = cache.remove(key) ?: return null
        cache[key] = bitmap
        return bitmap
    }

    fun put(key: String, bitmap: ImageBitmap) {
        cache.remove(key)
        if (cache.size >= MAX_ENTRIES) {
            val oldestKey = cache.keys.firstOrNull()
            if (oldestKey != null) {
                cache.remove(oldestKey)
            }
        }
        cache[key] = bitmap
    }

    fun clear() {
        cache.clear()
    }
}

private val KNOWN_ANIMAL_SLUGS = listOf(
    "african_wild_dog", "alpaca", "alpine_ibex", "american_alligator", "asian_elephant",
    "bactrian_camel", "bald_eagle", "barn_owl", "beluga_whale", "bengal_tiger",
    "bighorn_sheep", "black_panther", "black_rhinoceros", "blue_tang", "blue_whale", "bornean_orangutan",
    "capybara", "caracal", "cheetah", "chimpanzee", "chinchilla",
    "common_bottlenose_dolphin", "common_kingfisher", "common_ostrich", "common_raven", "common_wombat",
    "dingo", "dromedary", "elk", "emperor_penguin", "emu",
    "eurasian_lynx", "european_badger", "european_hedgehog", "galapagos_tortoise", "giant_anteater",
    "giant_panda", "giraffe", "golden_eagle", "golden_jackal", "golden_lion_tamarin",
    "gray_wolf", "great_horned_owl", "great_white_shark", "greater_bilby", "greater_flamingo",
    "green_sea_turtle", "grizzly_bear", "harbor_seal", "harpy_eagle", "hippopotamus",
    "honey_badger", "humpback_whale", "indian_peafowl", "jaguar", "japanese_macaque",
    "koala", "komodo_dragon", "lar_gibbon", "laughing_kookaburra", "leopard",
    "lion", "llama", "meerkat", "moose", "mountain_goat",
    "narwhal", "nine-banded_armadillo", "numbat", "ocellaris_clownfish", "ocelot",
    "okapi", "orca", "osprey", "pangolin", "peregrine_falcon",
    "plains_zebra", "platypus", "polar_bear", "proboscis_monkey", "quokka",
    "raccoon", "red_fox", "red_panda", "reindeer", "ring-tailed_lemur",
    "scarlet_macaw", "sea_otter", "seahorse", "secretarybird", "serval",
    "shoebill", "short-beaked_echidna", "sloth_bear", "snow_leopard", "snowy_owl",
    "southern_cassowary", "southern_elephant_seal", "spectacled_bear", "spotted_hyena", "sugar_glider",
    "sun_bear", "swordfish", "tasmanian_devil", "three-toed_sloth", "toco_toucan",
    "tokay_gecko", "tree-kangaroo", "walrus", "western_gorilla", "whale_shark",
    "white_rhinoceros", "wild_yak"
)

fun resolveAnimalSlug(avatarUrl: String?, animalName: String?, publicId: String?): String {
    if (!avatarUrl.isNullOrBlank()) {
        val clean = avatarUrl.trimEnd('/').substringAfterLast('/')
        if (clean.isNotBlank()) {
            return clean.replace(".jpg", "").replace(".png", "")
        }
    }
    if (!animalName.isNullOrBlank()) {
        val raw = animalName.substringBefore("#").trim().lowercase()
        val candidate = raw.replace(" ", "_").replace("-", "_")
        val match = KNOWN_ANIMAL_SLUGS.firstOrNull { it == candidate || it.contains(candidate) || candidate.contains(it) }
        if (match != null) return match
        if (candidate.isNotBlank() && candidate.all { it.isLetter() || it == '_' }) {
            return candidate
        }
    }
    if (!publicId.isNullOrBlank()) {
        val idx = abs(publicId.hashCode()) % KNOWN_ANIMAL_SLUGS.size
        return KNOWN_ANIMAL_SLUGS[idx]
    }
    return "snow_leopard"
}

fun getBundledAnimalResource(slug: String): org.jetbrains.compose.resources.DrawableResource {
    val s = slug.lowercase()
    return when {
        s.contains("fox") -> Res.drawable.onboarding_fox
        s.contains("panda") -> Res.drawable.onboarding_panda
        s.contains("cheetah") || s.contains("cat") || s.contains("leopard") -> Res.drawable.onboarding_cheetah
        s.contains("rabbit") || s.contains("hare") -> Res.drawable.onboarding_rabbit
        else -> {
            val list = listOf(
                Res.drawable.onboarding_fox,
                Res.drawable.onboarding_panda,
                Res.drawable.onboarding_cheetah,
                Res.drawable.onboarding_rabbit
            )
            list[abs(slug.hashCode()) % list.size]
        }
    }
}

/**
 * High-performance circular animal avatar composable.
 * Displays the authentic backend animal portrait with fallback to paw icon,
 * styled according to active IdentitySignature and IdentityRing settings.
 */
@Composable
fun AnimalAvatar(
    repository: ChatRepository,
    avatarUrl: String? = null,
    animalName: String? = null,
    publicId: String? = null,
    size: Dp = 36.dp,
    borderWidth: Dp? = null,
    borderColor: Color? = null,
    backgroundColor: Color = AnonChatColors.SurfaceSecondary,
    fallbackIcon: ImageVector = AnonChatIcons.Paw,
    forceAnimalImage: Boolean = false,
    onClick: (() -> Unit)? = null,
    enableLivingIdleAnimation: Boolean = false,
    enableInteractionBounce: Boolean = (onClick != null),
    modifier: Modifier = Modifier
) {
    val appearance = org.example.annochat.presentation.theme.LocalAppearanceSettings.current
    val accentColor = Color(appearance.accent.colorHex)
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val bounceScale = remember { androidx.compose.animation.core.Animatable(1f) }

    // Subtle living breathing animation (1.8% scale, 4% opacity) - respects reduced motion / QUIET energy
    val isQuiet = !appearance.animationsEnabled || appearance.campusEnergy == org.example.annochat.data.model.CampusEnergy.QUIET
    val idleScale: Float
    val idleAlpha: Float
    if (enableLivingIdleAnimation && !isQuiet) {
        val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "avatarLivingTransition")
        val animatedScale by transition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.018f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(durationMillis = 3200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            ),
            label = "avatarLivingScale"
        )
        val animatedAlpha by transition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.0f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(durationMillis = 3200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            ),
            label = "avatarLivingAlpha"
        )
        idleScale = animatedScale
        idleAlpha = animatedAlpha
    } else {
        idleScale = 1f
        idleAlpha = 1f
    }

    val effectiveBorderWidth = borderWidth ?: when (appearance.identityRing) {
        org.example.annochat.data.model.IdentityRing.NONE -> 0.dp
        org.example.annochat.data.model.IdentityRing.SUBTLE -> 1.dp
        org.example.annochat.data.model.IdentityRing.ACCENT_GLOW -> 1.5.dp
    }

    val effectiveBorderColor = borderColor ?: when (appearance.identityRing) {
        org.example.annochat.data.model.IdentityRing.NONE -> Color.Transparent
        org.example.annochat.data.model.IdentityRing.SUBTLE -> AnonChatColors.Border
        org.example.annochat.data.model.IdentityRing.ACCENT_GLOW -> accentColor
    }

    val slug = remember(avatarUrl, animalName, publicId) {
        resolveAnimalSlug(avatarUrl, animalName, publicId)
    }
    val targetUrl = remember(slug) { "/api/animal/$slug" }

    var bitmap by remember(targetUrl) { mutableStateOf(ImageBitmapCache.get(targetUrl)) }

    LaunchedEffect(targetUrl) {
        if (bitmap == null) {
            val cached = ImageBitmapCache.get(targetUrl)
            if (cached != null) {
                bitmap = cached
            } else {
                withContext(Dispatchers.Default) {
                    val result = repository.getMediaBytes(targetUrl)
                    result.onSuccess { bytes ->
                        val decoded = decodeImageBitmap(bytes)
                        if (decoded != null) {
                            ImageBitmapCache.put(targetUrl, decoded)
                            bitmap = decoded
                        }
                    }
                }
            }
        }
    }

    val handleTap: () -> Unit = {
        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
        coroutineScope.launch {
            bounceScale.animateTo(1.06f, androidx.compose.animation.core.tween(90, easing = androidx.compose.animation.core.FastOutSlowInEasing))
            bounceScale.animateTo(1.0f, androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 450f))
        }
        onClick?.invoke()
    }

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = androidx.compose.material3.ripple(bounded = true, color = AnonChatColors.Primary),
            onClick = handleTap
        )
    } else {
        Modifier
    }

    val resolvedFallbackIcon = if (forceAnimalImage) {
        fallbackIcon
    } else {
        when (appearance.identitySignature) {
            org.example.annochat.data.model.IdentitySignature.NIGHT_OWL -> fallbackIcon
            org.example.annochat.data.model.IdentitySignature.GLYPH -> AnonChatIcons.Sparkles
            org.example.annochat.data.model.IdentitySignature.ABSTRACT -> AnonChatIcons.Grid
            org.example.annochat.data.model.IdentitySignature.MINIMAL -> AnonChatIcons.PersonCircle
        }
    }

    val totalScale = idleScale * bounceScale.value

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = totalScale
                scaleY = totalScale
                alpha = idleAlpha
            }
            .clip(CircleShape)
            .then(clickModifier)
            .background(backgroundColor)
            .then(
                if (effectiveBorderWidth > 0.dp) {
                    Modifier.border(effectiveBorderWidth, effectiveBorderColor, CircleShape)
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = bitmap, animationSpec = tween(180)) { currentBmp ->
            if (currentBmp != null && (forceAnimalImage || appearance.identitySignature == org.example.annochat.data.model.IdentitySignature.NIGHT_OWL)) {
                Image(
                    bitmap = currentBmp,
                    contentDescription = animalName ?: "Animal Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (forceAnimalImage) {
                val bundledRes = remember(slug) { getBundledAnimalResource(slug) }
                Image(
                    painter = painterResource(bundledRes),
                    contentDescription = animalName ?: "Animal Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = resolvedFallbackIcon,
                    contentDescription = null,
                    tint = if (appearance.identityRing == org.example.annochat.data.model.IdentityRing.ACCENT_GLOW) accentColor else AnonChatColors.Primary,
                    modifier = Modifier.size(size * 0.52f)
                )
            }
        }
    }
}


/**
 * Composable for loading and rendering uploaded photos or general media in chat bubbles.
 */
@Composable
fun RemoteImage(
    repository: ChatRepository,
    pathOrUrl: String,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(pathOrUrl) { mutableStateOf(ImageBitmapCache.get(pathOrUrl)) }
    var isLoading by remember(pathOrUrl) { mutableStateOf(bitmap == null) }
    var hasError by remember(pathOrUrl) { mutableStateOf(false) }

    LaunchedEffect(pathOrUrl) {
        if (bitmap == null) {
            val cached = ImageBitmapCache.get(pathOrUrl)
            if (cached != null) {
                bitmap = cached
                isLoading = false
            } else {
                isLoading = true
                hasError = false
                withContext(Dispatchers.Default) {
                    val result = repository.getMediaBytes(pathOrUrl)
                    if (result.isSuccess) {
                        val decoded = decodeImageBitmap(result.getOrThrow())
                        if (decoded != null) {
                            ImageBitmapCache.put(pathOrUrl, decoded)
                            bitmap = decoded
                        } else {
                            hasError = true
                        }
                    } else {
                        hasError = true
                    }
                }
                isLoading = false
            }
        }
    }

    Box(
        modifier = modifier.clip(AnonChatShapes.card),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AnonChatColors.SurfaceSecondary),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = AnonChatColors.Primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            // Error fallback
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AnonChatColors.SurfaceSecondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AnonChatIcons.Attachment,
                    contentDescription = "Image unavailable",
                    tint = AnonChatColors.TextMuted,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
