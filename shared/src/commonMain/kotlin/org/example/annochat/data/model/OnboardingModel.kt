package org.example.annochat.data.model

import kotlinx.serialization.Serializable

/**
 * Phase 1 User Intent options.
 */
enum class OnboardingIntent(val title: String, val subtitle: String) {
    SOMEONE_TO_TALK_TO(
        title = "Someone to talk to",
        subtitle = "Casual conversations with people nearby"
    ),
    FIND_STUDY_PEOPLE(
        title = "Find study people",
        subtitle = "Cram sessions, exam prep and homework help"
    ),
    FIND_INTERESTS(
        title = "Find people with my interests",
        subtitle = "Gaming, tech, music and shared passions"
    ),
    CAMPUS_HAPPENINGS(
        title = "See what's happening on campus",
        subtitle = "Live rumors, event updates and late-night buzz"
    ),
    JUST_EXPLORE(
        title = "Just explore anonymously",
        subtitle = "No strings attached, zero identity trace"
    );

    companion object {
        fun fromTitle(title: String?): OnboardingIntent? =
            values().firstOrNull { it.title.equals(title, ignoreCase = true) }
    }
}

/**
 * Phase 1 Vibe options.
 */
enum class OnboardingVibe(val title: String, val subtitle: String) {
    QUIET_AND_CHILL(
        title = "Quiet & chill",
        subtitle = "Low energy, reflective, late-night thought sharing"
    ),
    ENERGETIC(
        title = "Energetic",
        subtitle = "Fast banter, lively debates and campus hype"
    ),
    JUST_FOR_FUN(
        title = "Just for fun",
        subtitle = "Jokes, memes and spontaneous connections"
    ),
    FOCUSED(
        title = "Focused",
        subtitle = "Academic grind, productivity and mutual accountability"
    ),
    READY_TO_TALK(
        title = "Ready to talk",
        subtitle = "Deep dives, venting and genuine discussions"
    );

    companion object {
        fun fromTitle(title: String?): OnboardingVibe? =
            values().firstOrNull { it.title.equals(title, ignoreCase = true) }
    }
}

/**
 * Phase 1 Conversation Interest options.
 */
enum class OnboardingInterest(val title: String, val iconName: String) {
    CAMPUS(title = "Campus", iconName = "Globe"),
    TECHNOLOGY(title = "Technology", iconName = "Terminal"),
    GAMING(title = "Gaming", iconName = "Sparkles"),
    MUSIC(title = "Music", iconName = "Headphones"),
    STUDY(title = "Study", iconName = "Book"),
    LATE_NIGHT_TALKS(title = "Late-night talks", iconName = "Moon");

    companion object {
        fun fromTitle(title: String?): OnboardingInterest? =
            values().firstOrNull { it.title.equals(title, ignoreCase = true) }
    }
}

/**
 * Structured state model for AfterClass Onboarding.
 */
@Serializable
data class OnboardingState(
    val intent: String? = null,
    val vibe: String? = null,
    val conversationInterest: String? = null,
    val completedPhase1: Boolean = false,
    val isAhaMomentCompleted: Boolean = false,
    val isPremiumScreenShown: Boolean = false
) {
    val selectedIntent: OnboardingIntent?
        get() = OnboardingIntent.fromTitle(intent)

    val selectedVibe: OnboardingVibe?
        get() = OnboardingVibe.fromTitle(vibe)

    val selectedInterest: OnboardingInterest?
        get() = OnboardingInterest.fromTitle(conversationInterest)
}

/**
 * Deterministic personalized transition lines computed from Phase 1 answers.
 */
data class PersonalizedTransition(
    val line1: String,
    val line2: String,
    val line3: String
) {
    companion object {
        fun from(state: OnboardingState): PersonalizedTransition {
            val line1 = when (state.selectedIntent) {
                OnboardingIntent.SOMEONE_TO_TALK_TO -> "You came here to connect."
                OnboardingIntent.FIND_STUDY_PEOPLE -> "You're looking for your study circle."
                OnboardingIntent.FIND_INTERESTS -> "You came to find your people."
                OnboardingIntent.CAMPUS_HAPPENINGS -> "You're tuned into the campus pulse."
                OnboardingIntent.JUST_EXPLORE -> "You're exploring without limits."
                null -> "You came to explore AfterClass."
            }

            val line2 = when (state.selectedVibe) {
                OnboardingVibe.QUIET_AND_CHILL -> "You're feeling relaxed tonight."
                OnboardingVibe.ENERGETIC -> "You're bringing the energy."
                OnboardingVibe.JUST_FOR_FUN -> "You're in it for the banter."
                OnboardingVibe.FOCUSED -> "You're in deep focus mode."
                OnboardingVibe.READY_TO_TALK -> "You're ready for real talk."
                null -> "The campus is live after hours."
            }

            val line3 = "Let's find your corner of campus."

            return PersonalizedTransition(line1, line2, line3)
        }
    }
}

/**
 * Deterministic sample conversation dialogue for Phase 2 Aha Moment.
 */
data class SampleAhaDialogue(
    val peerName: String,
    val peerAvatarEmoji: String,
    val roomTag: String,
    val openingMessage: String,
    val openingTime: String,
    val suggestedReplies: List<String>,
    val peerReplies: Map<String, String>,
    val defaultPeerReply: String
) {
    companion object {
        fun generate(state: OnboardingState): SampleAhaDialogue {
            val interest = state.selectedInterest
            val intent = state.selectedIntent

            return when {
                interest == OnboardingInterest.STUDY || intent == OnboardingIntent.FIND_STUDY_PEOPLE -> {
                    SampleAhaDialogue(
                        peerName = "Moon Owl #4A19",
                        peerAvatarEmoji = "🦉",
                        roomTag = "Study Hall · Late Night",
                        openingMessage = "Anyone else trying to survive tomorrow's exam?",
                        openingTime = "11:47 PM",
                        suggestedReplies = listOf(
                            "Struggling with chapter 4 here.",
                            "Just taking a 5 min break."
                        ),
                        peerReplies = mapOf(
                            "Struggling with chapter 4 here." to "Haha same, we've got this though. Don't give up yet!",
                            "Just taking a 5 min break." to "Stay strong! Coffee is the only thing keeping me awake."
                        ),
                        defaultPeerReply = "Right there with you. Good luck on the exam tomorrow!"
                    )
                }
                interest == OnboardingInterest.GAMING -> {
                    SampleAhaDialogue(
                        peerName = "Shadow Lynx #9B22",
                        peerAvatarEmoji = "🐱",
                        roomTag = "Gaming Lounge · Campus",
                        openingMessage = "Anyone online for a late-night match?",
                        openingTime = "11:47 PM",
                        suggestedReplies = listOf(
                            "Hopping on right now.",
                            "What game are we running?"
                        ),
                        peerReplies = mapOf(
                            "Hopping on right now." to "Let's go! Drop in the discord or send room code.",
                            "What game are we running?" to "Valorant / Rivals duo queue, hop in!"
                        ),
                        defaultPeerReply = "Awesome, let's get a full lobby going tonight!"
                    )
                }
                interest == OnboardingInterest.MUSIC -> {
                    SampleAhaDialogue(
                        peerName = "Echo Wolf #7C33",
                        peerAvatarEmoji = "🐺",
                        roomTag = "Music Room · Late Night",
                        openingMessage = "What are you listening to right now?",
                        openingTime = "11:47 PM",
                        suggestedReplies = listOf(
                            "Late night lo-fi playlist on repeat.",
                            "Some ambient electronic."
                        ),
                        peerReplies = mapOf(
                            "Late night lo-fi playlist on repeat." to "Solid choice. Matches the quiet atmosphere perfectly.",
                            "Some ambient electronic." to "Ooh share the artist! Always looking for new tracks."
                        ),
                        defaultPeerReply = "Great taste. Music hits totally different after midnight."
                    )
                }
                interest == OnboardingInterest.CAMPUS || intent == OnboardingIntent.CAMPUS_HAPPENINGS -> {
                    SampleAhaDialogue(
                        peerName = "Silent Badger #3E81",
                        peerAvatarEmoji = "🦡",
                        roomTag = "Campus Square · Live",
                        openingMessage = "Is anything happening around campus tonight?",
                        openingTime = "11:47 PM",
                        suggestedReplies = listOf(
                            "Library 3rd floor is surprisingly packed.",
                            "Courtyard food truck is still open."
                        ),
                        peerReplies = mapOf(
                            "Library 3rd floor is surprisingly packed." to "Classic finals week energy. Everyone is pulling all-nighters.",
                            "Courtyard food truck is still open." to "Heading there right now, thanks for the heads up!"
                        ),
                        defaultPeerReply = "Campus feels alive tonight. Stay safe out there!"
                    )
                }
                interest == OnboardingInterest.TECHNOLOGY -> {
                    SampleAhaDialogue(
                        peerName = "Cyber Raven #1F90",
                        peerAvatarEmoji = "🐦‍⬛",
                        roomTag = "Dev Stream · Tech",
                        openingMessage = "Anyone else working on something at this hour?",
                        openingTime = "11:47 PM",
                        suggestedReplies = listOf(
                            "Debugging a weird race condition.",
                            "Building a new side project."
                        ),
                        peerReplies = mapOf(
                            "Debugging a weird race condition." to "Classic 2 AM bug hunt. The breakthrough is right around the corner!",
                            "Building a new side project." to "The best ideas are always built at 2 AM. Keep shipping!"
                        ),
                        defaultPeerReply = "Late night coding hits different. Keep building!"
                    )
                }
                else -> {
                    // Default / Late night / Someone to talk to
                    SampleAhaDialogue(
                        peerName = "Moon Fox #82A1",
                        peerAvatarEmoji = "🦊",
                        roomTag = "Late night · Campus",
                        openingMessage = "Anyone else still awake?",
                        openingTime = "11:47 PM",
                        suggestedReplies = listOf(
                            "Apparently I am.",
                            "Sleep is for tomorrow."
                        ),
                        peerReplies = mapOf(
                            "Apparently I am." to "Haha same here. Best hours for quiet thinking.",
                            "Sleep is for tomorrow." to "Totally agree. Campus is so peaceful right now."
                        ),
                        defaultPeerReply = "Haha glad I'm not the only night owl on campus."
                    )
                }
            }
        }
    }
}

/**
 * Conceptual Premium Tiers.
 */
enum class PremiumTier(val displayName: String, val subtitle: String) {
    FREE("AfterClass Free", "Standard anonymous campus messaging"),
    AURA("AfterClass Aura", "Make your anonymous identity truly yours"),
    PRIME("AfterClass Prime", "Find conversations that match your vibe"),
    STEALTH("AfterClass Stealth", "Advanced ephemeral privacy"),
    MIDNIGHT("AfterClass Midnight", "Exclusive after-hours perks")
}

/**
 * Personalized Premium Recommendation.
 */
data class PremiumRecommendation(
    val tier: PremiumTier,
    val headline: String,
    val tagline: String,
    val features: List<String>,
    val actionText: String
) {
    companion object {
        fun from(state: OnboardingState): PremiumRecommendation {
            val isInterestsOrStudy = state.selectedIntent in listOf(
                OnboardingIntent.FIND_INTERESTS,
                OnboardingIntent.FIND_STUDY_PEOPLE,
                OnboardingIntent.CAMPUS_HAPPENINGS
            ) || state.selectedInterest in listOf(
                OnboardingInterest.STUDY,
                OnboardingInterest.CAMPUS,
                OnboardingInterest.TECHNOLOGY
            )

            return if (isInterestsOrStudy) {
                PremiumRecommendation(
                    tier = PremiumTier.PRIME,
                    headline = "AFTERCLASS PRIME",
                    tagline = "Find conversations that match your vibe.",
                    features = listOf(
                        "Interest-based matching",
                        "Department filters",
                        "Priority Match",
                        "Second Chance"
                    ),
                    actionText = "Explore Prime"
                )
            } else {
                PremiumRecommendation(
                    tier = PremiumTier.AURA,
                    headline = "AFTERCLASS AURA",
                    tagline = "Make your anonymous identity yours.",
                    features = listOf(
                        "Rare animal identities",
                        "Atmospheric themes",
                        "Identity auras",
                        "Premium reactions"
                    ),
                    actionText = "Explore Aura"
                )
            }
        }
    }
}
