package org.example.annochat

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinproject.shared.generated.resources.Res
import kotlinproject.shared.generated.resources.room_channel_civil
import kotlinproject.shared.generated.resources.room_channel_cse
import kotlinproject.shared.generated.resources.room_channel_ece
import kotlinproject.shared.generated.resources.room_channel_faculty
import kotlinproject.shared.generated.resources.room_channel_hostel
import kotlinproject.shared.generated.resources.room_channel_mech
import kotlinproject.shared.generated.resources.room_channel_public
import org.example.annochat.data.model.PublicRoomDto
import org.example.annochat.presentation.theme.RoomVisualRegistry

class AmbientCarouselVisualTest {

    @Test
    fun testCategoryKeyResolution() {
        assertEquals("public", RoomVisualRegistry.resolveCategoryKey("main", "Public Chat", "Campus stream for #Public Chat"))
        assertEquals("cse", RoomVisualRegistry.resolveCategoryKey("cse", "CSE Stream", "Computer science discussions"))
        assertEquals("ece", RoomVisualRegistry.resolveCategoryKey("ece", "Electronics Stream", "Circuit and chip discussions"))
        assertEquals("mech", RoomVisualRegistry.resolveCategoryKey("mech", "Mechanical Stream", "Machinery and mechanical engineering"))
        assertEquals("civil", RoomVisualRegistry.resolveCategoryKey("civil", "Civil Stream", "Architecture and structures"))
        assertEquals("faculty", RoomVisualRegistry.resolveCategoryKey("faculty", "Faculty", "Professors and academic talks"))
        assertEquals("hostel", RoomVisualRegistry.resolveCategoryKey("hostel", "Hostel", "Late night dorm chats"))
        assertEquals("default", RoomVisualRegistry.resolveCategoryKey("custom_99", "Anime Club", "General discussion"))
    }

    @Test
    fun testImageResourceResolution() {
        val publicRoom = PublicRoomDto(id = "main", name = "Public Chat", desc = "Campus chat")
        val cseRoom = PublicRoomDto(id = "cse", name = "CSE Stream", desc = "Code")
        val eceRoom = PublicRoomDto(id = "ece", name = "Electronics", desc = "Chips")
        val mechRoom = PublicRoomDto(id = "mech", name = "Mechanical", desc = "Gears")
        val civilRoom = PublicRoomDto(id = "civil", name = "Civil", desc = "Bridges")
        val facultyRoom = PublicRoomDto(id = "faculty", name = "Faculty", desc = "Professors")
        val hostelRoom = PublicRoomDto(id = "hostel", name = "Hostel", desc = "Dorm")
        val customRoom = PublicRoomDto(id = "grp_random", name = "Random Group", desc = "Misc")

        assertEquals(Res.drawable.room_channel_public, RoomVisualRegistry.getImage(publicRoom))
        assertEquals(Res.drawable.room_channel_cse, RoomVisualRegistry.getImage(cseRoom))
        assertEquals(Res.drawable.room_channel_ece, RoomVisualRegistry.getImage(eceRoom))
        assertEquals(Res.drawable.room_channel_mech, RoomVisualRegistry.getImage(mechRoom))
        assertEquals(Res.drawable.room_channel_civil, RoomVisualRegistry.getImage(civilRoom))
        assertEquals(Res.drawable.room_channel_faculty, RoomVisualRegistry.getImage(facultyRoom))
        assertEquals(Res.drawable.room_channel_hostel, RoomVisualRegistry.getImage(hostelRoom))
        // Custom room falls back safely to default image
        assertEquals(Res.drawable.room_channel_public, RoomVisualRegistry.getImage(customRoom))
    }

    @Test
    fun testAmbientPalettesAreLowSaturationAndNonVibrant() {
        val channels = listOf("main", "cse", "ece", "mech", "civil", "faculty", "hostel")
        for (id in channels) {
            val room = PublicRoomDto(id = id, name = id.uppercase())
            val palette = RoomVisualRegistry.getPalette(room)

            // Primary glow must remain dark and atmospheric to preserve text readability
            val r = palette.primaryGlow.red
            val g = palette.primaryGlow.green
            val b = palette.primaryGlow.blue

            assertTrue(r <= 0.45f, "Channel $id red is too bright: $r")
            assertTrue(g <= 0.45f, "Channel $id green is too bright: $g")
            assertTrue(b <= 0.50f, "Channel $id blue is too bright: $b")

            // Contrast difference between RGB channels should be modest (low saturation)
            val maxDiff = maxOf((r - g), (g - b), (b - r)).coerceAtLeast(0f)
            assertTrue(maxDiff <= 0.30f, "Channel $id is overly saturated: maxDiff=$maxDiff")
        }
    }

    @Test
    fun testContinuousForwardAndBackwardInterpolation() {
        val roomA = PublicRoomDto(id = "cse", name = "CSE Stream")
        val roomB = PublicRoomDto(id = "mech", name = "Mechanical Stream")

        val paletteA = RoomVisualRegistry.getPalette(roomA)
        val paletteB = RoomVisualRegistry.getPalette(roomB)

        // 0% -> exactly palette A
        val at0 = RoomVisualRegistry.interpolatePalettes(paletteA, paletteB, 0.0f)
        assertEquals(paletteA.primaryGlow, at0.primaryGlow)

        // 100% -> exactly palette B
        val at100 = RoomVisualRegistry.interpolatePalettes(paletteA, paletteB, 1.0f)
        assertEquals(paletteB.primaryGlow, at100.primaryGlow)

        // Check monotonicity and intermediate interpolation at 25%, 50%, 75%
        val at25 = RoomVisualRegistry.interpolatePalettes(paletteA, paletteB, 0.25f)
        val at50 = RoomVisualRegistry.interpolatePalettes(paletteA, paletteB, 0.50f)
        val at75 = RoomVisualRegistry.interpolatePalettes(paletteA, paletteB, 0.75f)

        assertNotEquals(paletteA.primaryGlow, at50.primaryGlow)
        assertNotEquals(paletteB.primaryGlow, at50.primaryGlow)

        // Continuous smooth transition between 0% and 100%
        assertTrue(at25.primaryGlow.red in paletteA.primaryGlow.red..paletteB.primaryGlow.red || at25.primaryGlow.red in paletteB.primaryGlow.red..paletteA.primaryGlow.red)
        assertTrue(at50.primaryGlow.red in paletteA.primaryGlow.red..paletteB.primaryGlow.red || at50.primaryGlow.red in paletteB.primaryGlow.red..paletteA.primaryGlow.red)
        assertTrue(at75.primaryGlow.red in paletteA.primaryGlow.red..paletteB.primaryGlow.red || at75.primaryGlow.red in paletteB.primaryGlow.red..paletteA.primaryGlow.red)
    }

    @Test
    fun testSafeFallbackWithMalformedOrEmptyRoom() {
        val emptyRoom = PublicRoomDto(id = "", name = "")
        val palette = RoomVisualRegistry.getPalette(emptyRoom)
        assertNotNull(palette)
        val image = RoomVisualRegistry.getImage(emptyRoom)
        assertNotNull(image)
    }

    @Test
    fun testFormatRoomTitleStripsHashtagAndAnonPrefix() {
        assertEquals("Public", org.example.annochat.presentation.screens.formatRoomTitle("#anon/ Public"))
        assertEquals("Public", org.example.annochat.presentation.screens.formatRoomTitle("/anon/ Public"))
        assertEquals("Public", org.example.annochat.presentation.screens.formatRoomTitle("anon/ Public"))
        assertEquals("Public", org.example.annochat.presentation.screens.formatRoomTitle("#/anon/ Public"))
        assertEquals("CSE Stream", org.example.annochat.presentation.screens.formatRoomTitle("#CSE Stream"))
        assertEquals("General Campus Chat", org.example.annochat.presentation.screens.formatRoomTitle("General Campus Chat"))
        assertEquals("ECE Circuit", org.example.annochat.presentation.screens.formatRoomTitle("#ECE Circuit"))
    }
}
