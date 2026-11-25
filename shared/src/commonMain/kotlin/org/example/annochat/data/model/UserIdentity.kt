package org.example.annochat.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class UserIdentityDto(
    @SerialName("public_id")
    val publicId: String,

    @SerialName("anon_name")
    val anonName: String,

    @SerialName("animal_name")
    val animalName: String? = null,

    @SerialName("sci_name")
    val sciName: String? = null,

    @SerialName("avatar_url")
    val avatarUrl: String,

    @SerialName("is_host")
    val isHost: Boolean = false
)

@Immutable
@Serializable
data class OnlineUserDto(
    @SerialName("public_id")
    val publicId: String,

    @SerialName("anon_name")
    val anonName: String? = null,

    @SerialName("anon_avatar")
    val anonAvatar: String? = null,

    @SerialName("sci_name")
    val sciName: String? = null
)

@Serializable
data class OnlineUsersResponse(
    @SerialName("status")
    val status: String,

    @SerialName("users")
    val users: List<OnlineUserDto> = emptyList()
)

val KNOWN_ANIMAL_SLUGS = listOf(
    "african_wild_dog", "alpaca", "alpine_ibex", "american_alligator", "asian_elephant",
    "bactrian_camel", "bald_eagle", "barn_owl", "beluga_whale", "bengal_tiger",
    "bighorn_sheep", "black_panther", "black_rhinoceros", "blue_whale", "bornean_orangutan",
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

fun createSyntheticOnlineUser(index: Int, publicIdSeed: String = ""): OnlineUserDto {
    val seedHash = if (publicIdSeed.isNotBlank()) {
        publicIdSeed.hashCode().let { if (it < 0) -it else it }
    } else {
        (index * 7919 + 1337)
    }
    val slugIndex = (index + seedHash) % KNOWN_ANIMAL_SLUGS.size
    val slug = KNOWN_ANIMAL_SLUGS[slugIndex]
    val animalDisplayName = slug.split("_", "-").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
    val hexSuffix = if (publicIdSeed.length >= 4) {
        publicIdSeed.take(4)
    } else {
        ((seedHash xor (index * 31)) and 0xFFFF).toString(16).padStart(4, '0')
    }
    val pid = if (publicIdSeed.isNotBlank()) publicIdSeed else "peer_${hexSuffix}_$index"
    val anonName = "$animalDisplayName #$hexSuffix"
    val avatarUrl = "/api/animal/$slug"
    return OnlineUserDto(
        publicId = pid,
        anonName = anonName,
        anonAvatar = avatarUrl,
        sciName = null
    )
}


