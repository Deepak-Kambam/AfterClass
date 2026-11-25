package org.example.annochat

import kotlinx.serialization.json.Json
import org.example.annochat.data.model.CreateRoomResponse
import org.example.annochat.data.model.RoomDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RoomSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun testDeserializeExactServerResponseWithBooleanIsPublicAndExtraFields() {
        val jsonPayload = """
            {
                "status": "ok",
                "room": {
                    "room_id": "grp_new_a1b2c3",
                    "name": "New",
                    "desc": "Hello",
                    "created_by": "Wise Owl #1234",
                    "owner_public_id": "usr_998877",
                    "created_at": "2026-09-23T15:59:56.240103",
                    "is_public": true,
                    "is_premium": false,
                    "expires_at": null,
                    "is_owner": true,
                    "is_active": 1,
                    "members": [
                        {
                            "public_id": "usr_998877",
                            "anon_name": "Wise Owl #1234",
                            "anon_avatar": "/api/animal/owl",
                            "joined_at": "2026-09-23T15:59:56.240103",
                            "is_owner": 1
                        }
                    ]
                }
            }
        """.trimIndent()

        val response = json.decodeFromString<CreateRoomResponse>(jsonPayload)
        assertEquals("ok", response.status)
        assertNotNull(response.room)
        assertEquals("grp_new_a1b2c3", response.room?.roomId)
        assertEquals("New", response.room?.name)
        assertEquals("Hello", response.room?.effectiveDesc)
        assertTrue(response.room?.isPublic == true)
        assertTrue(response.room?.isOwner == true)
        assertEquals(1, response.room?.members?.size)
    }

    @Test
    fun testDeserializeNumericAndStringIsPublic() {
        val payloadWithNumeric = """
            {
                "status": "ok",
                "room": {
                    "id": "room_numeric",
                    "name": "Numeric Room",
                    "is_public": 1,
                    "is_owner": 1
                }
            }
        """.trimIndent()

        val res1 = json.decodeFromString<CreateRoomResponse>(payloadWithNumeric)
        assertNotNull(res1.room)
        assertEquals("room_numeric", res1.room?.roomId)
        assertTrue(res1.room?.isPublic == true)
        assertTrue(res1.room?.isOwner == true)

        val payloadWithString = """
            {
                "status": "ok",
                "room": {
                    "id": 42,
                    "name": "String Room",
                    "is_public": "true",
                    "is_owner": "false"
                }
            }
        """.trimIndent()

        val res2 = json.decodeFromString<CreateRoomResponse>(payloadWithString)
        assertNotNull(res2.room)
        assertEquals("42", res2.room?.roomId)
        assertTrue(res2.room?.isPublic == true)
        assertEquals(false, res2.room?.isOwner)
    }

    @Test
    fun testDirectRoomDtoDeserialization() {
        val payload = """
            {
                "room_id": "grp_direct",
                "name": "Direct Room",
                "desc": "No wrapper",
                "is_public": true,
                "is_owner": true
            }
        """.trimIndent()

        val room = json.decodeFromString<RoomDto>(payload)
        assertEquals("grp_direct", room.roomId)
        assertEquals("Direct Room", room.name)
        assertTrue(room.isPublic)
        assertTrue(room.isOwner)
    }
}
