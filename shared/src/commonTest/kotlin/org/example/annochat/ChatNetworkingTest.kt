package org.example.annochat

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.annochat.data.model.IncomingEvent
import org.example.annochat.data.model.MessageDto
import org.example.annochat.data.model.OutgoingEvent
import org.example.annochat.data.model.PublicRoomsResponse
import org.example.annochat.data.model.UserIdentityDto
import org.example.annochat.data.session.InMemorySessionStorage
import org.example.annochat.network.NetworkConfig
import org.example.annochat.network.NetworkEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ChatNetworkingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Test
    fun testSessionIdGeneration() {
        val storage = InMemorySessionStorage()
        val sessionId = storage.getOrCreateSessionId()

        assertTrue(sessionId.startsWith("sess_"))
        assertEquals(21, sessionId.length) // "sess_" (5) + 16 hex chars
        assertEquals(sessionId, storage.getSessionId())
    }

    @Test
    fun testNetworkConfigUrlsAndEnvironments() {
        // 1. LOCAL_ANDROID_EMULATOR
        val emuConfig = NetworkConfig.AndroidEmulator
        assertEquals(NetworkEnvironment.LOCAL_ANDROID_EMULATOR, emuConfig.environment)
        assertEquals("http://10.0.2.2:8000", emuConfig.httpBaseUrl)
        assertEquals("ws://10.0.2.2:8000", emuConfig.wsBaseUrl)
        assertEquals("ws://10.0.2.2:8000/ws?session_id=sess_123", emuConfig.websocketUrl("sess_123"))

        // 2. LOCAL_PHYSICAL_DEVICE (LAN)
        val lanConfig = NetworkConfig.localPhysicalDevice("192.168.1.50", 8000)
        assertEquals(NetworkEnvironment.LOCAL_PHYSICAL_DEVICE, lanConfig.environment)
        assertEquals("http://192.168.1.50:8000", lanConfig.httpBaseUrl)
        assertEquals("ws://192.168.1.50:8000", lanConfig.wsBaseUrl)
        assertEquals("ws://192.168.1.50:8000/ws?session_id=sess_lan", lanConfig.websocketUrl("sess_lan"))

        // 3. REMOTE (HTTPS & WSS production / tunnel)
        val remoteConfig = NetworkConfig.remote("https://anonchat.example.com")
        assertEquals(NetworkEnvironment.REMOTE, remoteConfig.environment)
        assertEquals("https://anonchat.example.com", remoteConfig.httpBaseUrl)
        assertEquals("wss://anonchat.example.com", remoteConfig.wsBaseUrl)
        assertEquals("wss://anonchat.example.com/ws?session_id=sess_remote", remoteConfig.websocketUrl("sess_remote"))

        // 4. PRODUCTION PERMANENT DOMAIN (https://api.afterclass.bond)
        val prodConfig = NetworkConfig.Production
        assertEquals(NetworkEnvironment.REMOTE, prodConfig.environment)
        assertEquals("api.afterclass.bond", prodConfig.cleanHost)
        assertEquals("https://api.afterclass.bond", prodConfig.httpBaseUrl)
        assertEquals("wss://api.afterclass.bond", prodConfig.wsBaseUrl)
        assertEquals("wss://api.afterclass.bond/ws?session_id=test_sess", prodConfig.websocketUrl("test_sess"))

        // Auto-mapping of root frontend domain to api backend
        val rootMappedConfig = NetworkConfig.remote("https://afterclass.bond")
        assertEquals("api.afterclass.bond", rootMappedConfig.cleanHost)
        assertEquals("https://api.afterclass.bond", rootMappedConfig.httpBaseUrl)
        assertEquals("wss://api.afterclass.bond", rootMappedConfig.wsBaseUrl)
    }

    @Test
    fun testMessageDtoSerialization() {
        val rawJson = """
            {
                "id": 1,
                "post_num": 1001,
                "public_id": "a1b2c3d4",
                "anon_name": "Snow Leopard #a1b2",
                "anon_avatar": "/api/animal/snow_leopard",
                "content": "Hello anonymous board!",
                "room_id": "main",
                "reactions": {"🔥": 2, "👍": 5},
                "reply_to": null,
                "is_deleted": 0
            }
        """.trimIndent()

        val msg = json.decodeFromString<MessageDto>(rawJson)
        assertEquals(1001L, msg.postNum)
        assertEquals("a1b2c3d4", msg.publicId)
        assertEquals("Snow Leopard #a1b2", msg.anonName)
        assertEquals("Hello anonymous board!", msg.content)
        assertEquals(2, msg.reactions["🔥"])
        assertEquals(5, msg.reactions["👍"])
    }

    @Test
    fun testPublicRoomsResponseSerialization() {
        val rawJson = """
            {
                "status": "ok",
                "rooms": [
                    {"id": "main", "name": "/anon/ Public", "icon": "globe", "desc": "All Campus"},
                    {"id": "cse", "name": "CSE Stream", "icon": "terminal", "desc": "Comp Sci"}
                ]
            }
        """.trimIndent()

        val res = json.decodeFromString<PublicRoomsResponse>(rawJson)
        assertEquals("ok", res.status)
        assertEquals(2, res.rooms.size)
        assertEquals("main", res.rooms[0].id)
        assertEquals("/anon/ Public", res.rooms[0].name)
    }

    @Test
    fun testIncomingEventParsing() {
        // 1. my_identity
        val identityJson = """{"type": "my_identity", "anon_name": "Capybara #1234", "avatar_url": "/api/animal/capybara", "public_id": "12345678", "is_host": false}"""
        val identityEvent = IncomingEvent.fromJson(identityJson)
        assertTrue(identityEvent is IncomingEvent.MyIdentity)
        assertEquals("12345678", identityEvent.identity.publicId)
        assertEquals("Capybara #1234", identityEvent.identity.anonName)

        // 2. online_count
        val onlineJson = """{"type": "online_count", "count": 25}"""
        val onlineEvent = IncomingEvent.fromJson(onlineJson)
        assertTrue(onlineEvent is IncomingEvent.OnlineCount)
        assertEquals(25, onlineEvent.count)

        // 3. new_message
        val newMsgJson = """
            {
                "type": "new_message",
                "message": {
                    "post_num": 1005,
                    "content": "Test incoming",
                    "room_id": "main"
                }
            }
        """.trimIndent()
        val newMsgEvent = IncomingEvent.fromJson(newMsgJson)
        assertTrue(newMsgEvent is IncomingEvent.NewMessage)
        assertEquals(1005L, newMsgEvent.message.postNum)
        assertEquals("Test incoming", newMsgEvent.message.content)

        // 4. reaction_update
        val reactJson = """{"type": "reaction_update", "post_num": 1005, "reactions": {"❤️": 3}}"""
        val reactEvent = IncomingEvent.fromJson(reactJson)
        assertTrue(reactEvent is IncomingEvent.ReactionUpdate)
        assertEquals(1005L, reactEvent.postNum)
        assertEquals(3, reactEvent.reactions["❤️"])

        // 5. typing
        val typingJson = """{"type": "typing", "animal_name": "Red Panda"}"""
        val typingEvent = IncomingEvent.fromJson(typingJson)
        assertTrue(typingEvent is IncomingEvent.Typing)
        assertEquals("Red Panda", typingEvent.animalName)

        // 6. delete_message
        val delJson = """{"type": "delete_message", "post_num": 1005}"""
        val delEvent = IncomingEvent.fromJson(delJson)
        assertTrue(delEvent is IncomingEvent.DeleteMessage)
        assertEquals(1005L, delEvent.postNum)

        // 7. unknown / future event (must not crash)
        val unknownJson = """{"type": "future_feature_v3", "data": 123}"""
        val unknownEvent = IncomingEvent.fromJson(unknownJson)
        assertTrue(unknownEvent is IncomingEvent.Unknown)
        assertEquals("future_feature_v3", unknownEvent.rawType)
    }

    @Test
    fun testOutgoingEventSerialization() {
        val postAction = OutgoingEvent.PostMessage(
            content = "Testing send message",
            roomId = "cse",
            replyTo = 1002L
        )
        val postJson = json.encodeToString(postAction)
        assertTrue(postJson.contains("\"action\":\"post_message\""))
        assertTrue(postJson.contains("\"content\":\"Testing send message\""))
        assertTrue(postJson.contains("\"room_id\":\"cse\""))
        assertTrue(postJson.contains("\"reply_to\":1002"))

        val reactAction = OutgoingEvent.React(postNum = 1005L, emoji = "🔥")
        val reactJson = json.encodeToString(reactAction)
        assertTrue(reactJson.contains("\"action\":\"react\""))
        assertTrue(reactJson.contains("\"post_num\":1005"))
        assertTrue(reactJson.contains("\"emoji\":\"🔥\""))
    }

    @Test
    fun testNetworkConfigRemoteDiscovery() {
        val discoveryConfig = NetworkConfig.remoteDiscovery()
        assertEquals(NetworkEnvironment.REMOTE, discoveryConfig.environment)
        assertEquals(org.example.annochat.network.DpdnsEndpointDiscoveryService.DEFAULT_PERMANENT_DISCOVERY_URL, discoveryConfig.discoveryUrl)
    }

    @Test
    fun testDigitalPlatBackendUrlTxtExtraction() {
        val mockTxt = "https://reflects-meters-hourly-approve.trycloudflare.com\n"
        val extracted = org.example.annochat.network.DpdnsEndpointDiscoveryService.extractTunnelUrl(mockTxt)
        assertEquals("https://reflects-meters-hourly-approve.trycloudflare.com", extracted)
        assertTrue(org.example.annochat.network.DpdnsEndpointDiscoveryService.isValidTunnelUrl(extracted!!))

        val config = NetworkConfig.remote(extracted)
        assertEquals("reflects-meters-hourly-approve.trycloudflare.com", config.cleanHost)
        assertEquals("https://reflects-meters-hourly-approve.trycloudflare.com", config.httpBaseUrl)
        assertEquals("wss://reflects-meters-hourly-approve.trycloudflare.com", config.wsBaseUrl)
        assertEquals("wss://reflects-meters-hourly-approve.trycloudflare.com/ws?session_id=sess_1", config.websocketUrl("sess_1"))
    }

    @Test
    fun testDigitalPlatConfigJsExtraction() {
        val mockJs = """
            // Automatically synced by cloud_sync.py
            window.BACKEND_CONFIG = {
              "backend_url": "https://reflects-meters-hourly-approve.trycloudflare.com",
              "status": "online",
              "updated_at": "2026-09-09T15:48:12.225935"
            };
            window.BACKEND_URL = window.BACKEND_CONFIG.backend_url;
        """.trimIndent()

        val extracted = org.example.annochat.network.DpdnsEndpointDiscoveryService.extractTunnelUrl(mockJs)
        assertEquals("https://reflects-meters-hourly-approve.trycloudflare.com", extracted)
        assertTrue(org.example.annochat.network.DpdnsEndpointDiscoveryService.isValidTunnelUrl(extracted!!))
    }

    @Test
    fun testDigitalPlatBackendConfigJsonExtraction() {
        val mockJson = """
            {
              "backend_url": "https://fancy-domain-123.trycloudflare.com",
              "api": "https://fancy-domain-123.trycloudflare.com",
              "status": "online"
            }
        """.trimIndent()

        val extracted = org.example.annochat.network.DpdnsEndpointDiscoveryService.extractTunnelUrl(mockJson)
        assertEquals("https://fancy-domain-123.trycloudflare.com", extracted)
        assertTrue(org.example.annochat.network.DpdnsEndpointDiscoveryService.isValidTunnelUrl(extracted!!))
    }

    @Test
    fun testCandidateUrlsGeneration() {
        val candidates = org.example.annochat.network.DpdnsEndpointDiscoveryService.getCandidateUrls("http://afterclass.qd.je/?i=1")
        assertTrue(candidates.contains("http://afterclass.qd.je/backend_url.txt"))
        assertTrue(candidates.contains("http://afterclass.qd.je/config.js"))
        assertTrue(candidates.contains("http://afterclass.qd.je/backend_config.json"))
        assertTrue(candidates.contains("http://afterclass.qd.je/?i=1"))
    }

    @Test
    fun testDpdnsUrlValidationAndNormalization() {
        // Valid URLs
        assertTrue(org.example.annochat.network.DpdnsEndpointDiscoveryService.isValidTunnelUrl("https://abc-123.trycloudflare.com"))
        assertTrue(org.example.annochat.network.DpdnsEndpointDiscoveryService.isValidTunnelUrl("https://mychat.example.com"))
        assertTrue(org.example.annochat.network.DpdnsEndpointDiscoveryService.isValidTunnelUrl("https://foo.ngrok-free.app"))

        // Invalid URLs (empty, malformed, "api" keyword, localhost, or discovery domains)
        kotlin.test.assertFalse(org.example.annochat.network.DpdnsEndpointDiscoveryService.isValidTunnelUrl(""))
        kotlin.test.assertFalse(org.example.annochat.network.DpdnsEndpointDiscoveryService.isValidTunnelUrl("api"))
        kotlin.test.assertFalse(org.example.annochat.network.DpdnsEndpointDiscoveryService.isValidTunnelUrl("https://api"))
        kotlin.test.assertFalse(org.example.annochat.network.DpdnsEndpointDiscoveryService.isValidTunnelUrl("https://api/public_rooms"))
        kotlin.test.assertFalse(org.example.annochat.network.DpdnsEndpointDiscoveryService.isValidTunnelUrl("https://localhost:8000"))
        kotlin.test.assertFalse(org.example.annochat.network.DpdnsEndpointDiscoveryService.isValidTunnelUrl("https://10.0.2.2:8000"))
        kotlin.test.assertFalse(org.example.annochat.network.DpdnsEndpointDiscoveryService.isValidTunnelUrl("https://p.dpdns.org/prajwal20006here-815028"))
        kotlin.test.assertFalse(org.example.annochat.network.DpdnsEndpointDiscoveryService.isValidTunnelUrl("http://afterclass.qd.je/?i=1"))

        // Empty/blank content returns null without crash
        kotlin.test.assertNull(org.example.annochat.network.DpdnsEndpointDiscoveryService.extractTunnelUrl(""))
        kotlin.test.assertNull(org.example.annochat.network.DpdnsEndpointDiscoveryService.extractTunnelUrl("   "))
        kotlin.test.assertNull(org.example.annochat.network.DpdnsEndpointDiscoveryService.extractTunnelUrl("<html><body>No links here</body></html>"))
    }

    @Test
    fun testLiveDigitalPlatDiscoveryResolution() = kotlinx.coroutines.runBlocking {
        val discoveryService = org.example.annochat.network.DpdnsEndpointDiscoveryService()
        val result = discoveryService.resolveConfig(forceRefresh = true)
        // Resolves over the live network to your friend's active Cloudflare tunnel
        if (result.isSuccess) {
            val config = result.getOrThrow()
            assertTrue(config.cleanHost.isNotBlank())
            kotlin.test.assertNotEquals("api", config.cleanHost)
            assertTrue(config.isSecure)
            assertTrue(config.httpBaseUrl.startsWith("https://"))
            assertTrue(config.wsBaseUrl.startsWith("wss://"))
            assertEquals(config, discoveryService.getCachedConfig())
        }
    }

    @Test
    fun testGenerateClientMsgId() {
        val id1 = org.example.annochat.data.model.generateClientMsgId()
        val id2 = org.example.annochat.data.model.generateClientMsgId()

        assertTrue(id1.startsWith("cmsg_"))
        assertTrue(id2.startsWith("cmsg_"))
        kotlin.test.assertNotEquals(id1, id2)
        // Format: cmsg_<12 hex>_<12 hex> -> length = 5 + 12 + 1 + 12 = 30
        assertEquals(30, id1.length)
        assertTrue(id1.matches(Regex("^cmsg_[0-9a-f]{12}_[0-9a-f]{12}$")))

        // Test uniqueness across 500 samples
        val set = (1..500).map { org.example.annochat.data.model.generateClientMsgId() }.toSet()
        assertEquals(500, set.size)
    }

    @Test
    fun testMessageDtoWithClientMsgIdSerialization() {
        val msgWithId = MessageDto(
            postNum = 2001L,
            clientMsgId = "cmsg_0123456789ab_cdef01234567",
            content = "Idempotent message",
            roomId = "main"
        )
        val serialized = json.encodeToString(msgWithId)
        assertTrue(serialized.contains("\"client_msg_id\":\"cmsg_0123456789ab_cdef01234567\""))

        val decoded = json.decodeFromString<MessageDto>(serialized)
        assertEquals("cmsg_0123456789ab_cdef01234567", decoded.clientMsgId)
        assertEquals(2001L, decoded.postNum)

        // Legacy message without client_msg_id defaults to null
        val legacyJson = """{"post_num": 2002, "content": "Legacy message"}"""
        val legacyDecoded = json.decodeFromString<MessageDto>(legacyJson)
        kotlin.test.assertNull(legacyDecoded.clientMsgId)
        assertEquals(2002L, legacyDecoded.postNum)
    }

    @Test
    fun testOutgoingPostMessageWithClientMsgId() {
        val post = OutgoingEvent.PostMessage(
            content = "Hello with idempotency key",
            roomId = "main",
            clientMsgId = "cmsg_test_id_123"
        )
        val encoded = json.encodeToString(post)
        assertTrue(encoded.contains("\"client_msg_id\":\"cmsg_test_id_123\""))

        // Null client_msg_id should also encode safely
        val postWithoutId = OutgoingEvent.PostMessage(
            content = "Hello without id",
            roomId = "main",
            clientMsgId = null
        )
        val encodedNoId = json.encodeToString(postWithoutId)
        assertTrue(encodedNoId.contains("\"action\":\"post_message\""))
    }

    @Test
    fun testLiveServerAppVersionPayload() {
        // 1. String releaseNotes (as returned by live server)
        val stringNotesJson = """
            {
              "latestVersionCode": 3,
              "latestVersionName": "v1.0.2",
              "releaseNotes": "Performance improvements, stability fixes, and live connection updates.",
              "forceUpdate": false,
              "downloadUrl": "https://api.afterclass.bond/updates/AfterClass.apk",
              "apkUrl": "https://api.afterclass.bond/updates/AfterClass.apk"
            }
        """.trimIndent()
        val decodedFromString = json.decodeFromString<org.example.annochat.data.model.AppVersionResponse>(stringNotesJson)
        assertEquals(3L, decodedFromString.latestVersionCode)
        assertEquals(listOf("Performance improvements, stability fixes, and live connection updates."), decodedFromString.releaseNotes)

        // 2. Array releaseNotes (canonical schema)
        val arrayNotesJson = """
            {
              "latestVersionCode": 3,
              "latestVersionName": "v1.0.2",
              "releaseNotes": ["Performance improvements, stability fixes, and live connection updates."],
              "forceUpdate": false,
              "downloadUrl": "https://api.afterclass.bond/updates/AfterClass.apk",
              "apkUrl": "https://api.afterclass.bond/updates/AfterClass.apk"
            }
        """.trimIndent()
        val decodedFromArray = json.decodeFromString<org.example.annochat.data.model.AppVersionResponse>(arrayNotesJson)
        assertEquals(3L, decodedFromArray.latestVersionCode)
        assertEquals(listOf("Performance improvements, stability fixes, and live connection updates."), decodedFromArray.releaseNotes)
    }
}
