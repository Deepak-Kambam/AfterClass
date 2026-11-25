package org.example.annochat

import org.example.annochat.data.model.OnboardingIntent
import org.example.annochat.data.model.OnboardingInterest
import org.example.annochat.data.model.OnboardingState
import org.example.annochat.data.model.OnboardingVibe
import org.example.annochat.data.model.PersonalizedTransition
import org.example.annochat.data.model.PremiumRecommendation
import org.example.annochat.data.model.PremiumTier
import org.example.annochat.data.model.SampleAhaDialogue
import org.example.annochat.data.session.InMemorySessionStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OnboardingModelTest {

    @Test
    fun testPersonalizedTransitionGeneration() {
        val state = OnboardingState(
            intent = OnboardingIntent.SOMEONE_TO_TALK_TO.title,
            vibe = OnboardingVibe.QUIET_AND_CHILL.title,
            conversationInterest = OnboardingInterest.LATE_NIGHT_TALKS.title,
            completedPhase1 = true
        )

        val transition = PersonalizedTransition.from(state)
        assertEquals("You came here to connect.", transition.line1)
        assertEquals("You're feeling relaxed tonight.", transition.line2)
        assertEquals("Let's find your corner of campus.", transition.line3)
    }

    @Test
    fun testSampleAhaDialogueStudy() {
        val state = OnboardingState(
            intent = OnboardingIntent.FIND_STUDY_PEOPLE.title,
            vibe = OnboardingVibe.FOCUSED.title,
            conversationInterest = OnboardingInterest.STUDY.title
        )

        val dialogue = SampleAhaDialogue.generate(state)
        assertTrue(dialogue.openingMessage.contains("exam", ignoreCase = true))
        assertEquals("Moon Owl #4A19", dialogue.peerName)
        assertTrue(dialogue.suggestedReplies.isNotEmpty())
    }

    @Test
    fun testSampleAhaDialogueGaming() {
        val state = OnboardingState(
            intent = OnboardingIntent.FIND_INTERESTS.title,
            vibe = OnboardingVibe.ENERGETIC.title,
            conversationInterest = OnboardingInterest.GAMING.title
        )

        val dialogue = SampleAhaDialogue.generate(state)
        assertTrue(dialogue.openingMessage.contains("match", ignoreCase = true))
        assertEquals("Shadow Lynx #9B22", dialogue.peerName)
    }

    @Test
    fun testSampleAhaDialogueMusic() {
        val state = OnboardingState(
            intent = OnboardingIntent.FIND_INTERESTS.title,
            vibe = OnboardingVibe.QUIET_AND_CHILL.title,
            conversationInterest = OnboardingInterest.MUSIC.title
        )

        val dialogue = SampleAhaDialogue.generate(state)
        assertTrue(dialogue.openingMessage.contains("listening", ignoreCase = true))
        assertEquals("Echo Wolf #7C33", dialogue.peerName)
    }

    @Test
    fun testSampleAhaDialogueCampus() {
        val state = OnboardingState(
            intent = OnboardingIntent.CAMPUS_HAPPENINGS.title,
            vibe = OnboardingVibe.JUST_FOR_FUN.title,
            conversationInterest = OnboardingInterest.CAMPUS.title
        )

        val dialogue = SampleAhaDialogue.generate(state)
        assertTrue(dialogue.openingMessage.contains("campus", ignoreCase = true))
        assertEquals("Silent Badger #3E81", dialogue.peerName)
    }

    @Test
    fun testSampleAhaDialogueTech() {
        val state = OnboardingState(
            intent = OnboardingIntent.FIND_INTERESTS.title,
            vibe = OnboardingVibe.FOCUSED.title,
            conversationInterest = OnboardingInterest.TECHNOLOGY.title
        )

        val dialogue = SampleAhaDialogue.generate(state)
        assertTrue(dialogue.openingMessage.contains("working", ignoreCase = true))
        assertEquals("Cyber Raven #1F90", dialogue.peerName)
    }

    @Test
    fun testPremiumRecommendationPrime() {
        val state = OnboardingState(
            intent = OnboardingIntent.FIND_INTERESTS.title,
            vibe = OnboardingVibe.READY_TO_TALK.title,
            conversationInterest = OnboardingInterest.STUDY.title
        )

        val rec = PremiumRecommendation.from(state)
        assertEquals(PremiumTier.PRIME, rec.tier)
        assertEquals("AFTERCLASS PRIME", rec.headline)
        assertEquals("Explore Prime", rec.actionText)
        assertTrue(rec.features.contains("Interest-based matching"))
    }

    @Test
    fun testPremiumRecommendationAura() {
        val state = OnboardingState(
            intent = OnboardingIntent.JUST_EXPLORE.title,
            vibe = OnboardingVibe.QUIET_AND_CHILL.title,
            conversationInterest = OnboardingInterest.LATE_NIGHT_TALKS.title
        )

        val rec = PremiumRecommendation.from(state)
        assertEquals(PremiumTier.AURA, rec.tier)
        assertEquals("AFTERCLASS AURA", rec.headline)
        assertEquals("Explore Aura", rec.actionText)
        assertTrue(rec.features.contains("Rare animal identities"))
    }

    @Test
    fun testInMemorySessionStorageOnboarding() {
        val storage = InMemorySessionStorage()
        assertFalse(storage.isOnboardingCompleted())

        storage.setOnboardingIntent(OnboardingIntent.SOMEONE_TO_TALK_TO.title)
        storage.setOnboardingVibe(OnboardingVibe.QUIET_AND_CHILL.title)
        storage.setOnboardingInterest(OnboardingInterest.LATE_NIGHT_TALKS.title)
        storage.setAhaMomentCompleted(true)

        val state = storage.getOnboardingState()
        assertEquals(OnboardingIntent.SOMEONE_TO_TALK_TO.title, state.intent)
        assertEquals(OnboardingVibe.QUIET_AND_CHILL.title, state.vibe)
        assertEquals(OnboardingInterest.LATE_NIGHT_TALKS.title, state.conversationInterest)
        assertTrue(state.completedPhase1)
        assertTrue(state.isAhaMomentCompleted)

        storage.setOnboardingCompleted(true)
        assertTrue(storage.isOnboardingCompleted())
    }
}
