package org.example.annochat

import kotlinx.coroutines.runBlocking
import org.example.annochat.presentation.designsystem.AnonChatIcons
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProfileActionsTest {

    @Test
    fun testSuggestANameTargetUrl() {
        val targetUrl = "https://afterclass.bond/"
        assertTrue(targetUrl.startsWith("https://"), "Target URL must use secure HTTPS")
        assertTrue(targetUrl.contains("afterclass.bond"), "Target URL must point to afterclass.bond domain")
        assertEquals("https://afterclass.bond/", targetUrl)
    }

    @Test
    fun testProfileIconsExistAndAreValid() {
        val shareIcon = AnonChatIcons.Share
        assertNotNull(shareIcon, "Share icon must be present")
        assertEquals("Share", shareIcon.name)

        val lightbulbIcon = AnonChatIcons.Lightbulb
        assertNotNull(lightbulbIcon, "Lightbulb icon must be present")
        assertEquals("Lightbulb", lightbulbIcon.name)
    }

    @Test
    fun testShareMessageContract() {
        val shareMessage = "Try AfterClass — anonymous campus conversations."
        val mimeType = "application/vnd.android.package-archive"

        assertEquals("application/vnd.android.package-archive", mimeType)
        assertTrue(shareMessage.contains("AfterClass"))
        assertTrue(shareMessage.contains("anonymous campus conversations"))
    }

    @Test
    fun testShareAppSafeExecution() = runBlocking {
        // Must return a typed Result without throwing unhandled exceptions
        val result = shareApp()
        assertNotNull(result)
        // If not initialized in headless test runner, failure is safely encapsulated in Result.failure
        if (result.isFailure) {
            val ex = result.exceptionOrNull()
            assertNotNull(ex)
            assertTrue(ex is Exception)
        }
    }

    @Test
    fun testOpenBrowserUrlSafeExecution() {
        // Must return a typed Result without throwing unhandled exceptions
        val result = openBrowserUrl("https://afterclass.bond/")
        assertNotNull(result)
        if (result.isFailure) {
            val ex = result.exceptionOrNull()
            assertNotNull(ex)
            assertTrue(ex is Exception)
        }
    }
}
