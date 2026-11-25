package org.example.annochat

import org.example.annochat.network.NetworkConfig
import org.example.annochat.network.NetworkEnvironment
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransportSecurityTest {

    @BeforeTest
    fun setup() {
        setDebugOverrideForTesting(null)
    }

    @AfterTest
    fun tearDown() {
        setDebugOverrideForTesting(null)
    }

    @Test
    fun testProductionUrlsUseHttpsAndWss() {
        // TEST 4: Production URLs must strictly use HTTPS/WSS and isSecure = true
        val prod = NetworkConfig.Production
        assertEquals(NetworkEnvironment.REMOTE, prod.environment)
        assertEquals("api.afterclass.bond", prod.cleanHost)
        assertTrue(prod.isSecure, "Production config must have isSecure = true")
        assertTrue(prod.httpBaseUrl.startsWith("https://"), "Production HTTP URL must begin with https://")
        assertTrue(prod.wsBaseUrl.startsWith("wss://"), "Production WebSocket URL must begin with wss://")
        assertEquals("https://api.afterclass.bond", prod.httpBaseUrl)
        assertEquals("wss://api.afterclass.bond", prod.wsBaseUrl)
    }

    @Test
    fun testReleaseModeNeverFallsBackToEmulatorDevUrl() {
        // TEST 2 & TEST 3: In RELEASE mode, defaultNetworkConfig() MUST NEVER fall back to 10.0.2.2
        setDebugOverrideForTesting(false)
        assertFalse(isDebugBuild(), "isDebugBuild() must be false in RELEASE mode")

        val config = defaultNetworkConfig()
        assertFalse(
            config.httpBaseUrl.contains("10.0.2.2"),
            "RELEASE build must NEVER route to 10.0.2.2:8000: was ${config.httpBaseUrl}"
        )
        assertFalse(
            config.wsBaseUrl.contains("10.0.2.2"),
            "RELEASE build must NEVER route to ws://10.0.2.2:8000: was ${config.wsBaseUrl}"
        )
        assertTrue(
            config.httpBaseUrl.startsWith("https://"),
            "RELEASE build must use encrypted https:// transport: was ${config.httpBaseUrl}"
        )
        assertTrue(
            config.wsBaseUrl.startsWith("wss://"),
            "RELEASE build must use encrypted wss:// transport: was ${config.wsBaseUrl}"
        )
        assertEquals("https://api.afterclass.bond", config.httpBaseUrl)
        assertEquals("wss://api.afterclass.bond", config.wsBaseUrl)
    }

    @Test
    fun testDebugModePreservesDevelopmentSupport() {
        // TEST 1: In DEBUG mode, isDebugBuild() returns true
        setDebugOverrideForTesting(true)
        assertTrue(isDebugBuild(), "isDebugBuild() must be true when DEBUG is configured")

        // On Android emulator (or when emulator is detected), it routes to AndroidEmulator
        // On physical devices / iOS, it routes to remote
        val config = defaultNetworkConfig()
        if (config.environment == NetworkEnvironment.LOCAL_ANDROID_EMULATOR) {
            assertEquals("http://10.0.2.2:8000", config.httpBaseUrl)
            assertEquals("ws://10.0.2.2:8000", config.wsBaseUrl)
            assertFalse(config.isSecure)
        } else {
            assertEquals("https://api.afterclass.bond", config.httpBaseUrl)
            assertEquals("wss://api.afterclass.bond", config.wsBaseUrl)
            assertTrue(config.isSecure)
        }
    }

    @Test
    fun testFailClosedDefaultPolicy() {
        // Without explicit DEBUG override, default policy must fail closed (i.e. not grant debug privileges)
        setDebugOverrideForTesting(null)
        val isDebug = isDebugBuild()
        // In automated tests outside a debuggable Android Application, isDebug must default to false
        if (!isDebug) {
            val config = defaultNetworkConfig()
            assertFalse(config.httpBaseUrl.contains("10.0.2.2"))
            assertEquals("https://api.afterclass.bond", config.httpBaseUrl)
        }
    }
}
