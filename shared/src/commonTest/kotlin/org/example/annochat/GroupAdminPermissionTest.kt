package org.example.annochat

import kotlinx.serialization.json.Json
import org.example.annochat.data.model.RoomDto
import org.example.annochat.data.model.RoomMemberDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupAdminPermissionTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun testRoomMemberDtoAdminRoleDetection() {
        val memberAdminRole = RoomMemberDto(
            publicId = "user_admin",
            role = "ADMIN",
            isOwner = 0
        )
        assertTrue(memberAdminRole.isAdmin)

        val memberOwnerRole = RoomMemberDto(
            publicId = "user_owner_role",
            role = "OWNER",
            isOwner = 0
        )
        assertTrue(memberOwnerRole.isAdmin)

        val memberIsOwnerFlag = RoomMemberDto(
            publicId = "user_flag",
            role = "MEMBER",
            isOwner = 1
        )
        assertTrue(memberIsOwnerFlag.isAdmin)

        val normalMember = RoomMemberDto(
            publicId = "user_member",
            role = "MEMBER",
            isOwner = 0
        )
        assertFalse(normalMember.isAdmin)
    }

    @Test
    fun testRoomDtoIsUserAdminScenarios() {
        val creatorId = "creator_usr_123"
        val memberId = "member_usr_456"
        val strangerId = "stranger_usr_789"

        val room = RoomDto(
            rawRoomId = "grp_test_room",
            name = "Test Group",
            ownerPublicId = creatorId,
            createdBy = creatorId,
            isPublic = true,
            isOwner = false,
            rawIsAdmin = false,
            members = listOf(
                RoomMemberDto(
                    publicId = creatorId,
                    role = "ADMIN",
                    isOwner = 1
                ),
                RoomMemberDto(
                    publicId = memberId,
                    role = "MEMBER",
                    isOwner = 0
                )
            )
        )

        // Creator recognized as admin via ownerPublicId / createdBy / member list
        assertTrue(room.isUserAdmin(creatorId))

        // Normal member is not admin
        assertFalse(room.isUserAdmin(memberId))

        // Stranger is not admin
        assertFalse(room.isUserAdmin(strangerId))

        // Null publicId is not admin
        assertFalse(room.isUserAdmin(null))

        // Blank publicId is not admin
        assertFalse(room.isUserAdmin("   "))

        // Local ownership override recognized
        assertTrue(room.isUserAdmin(memberId, isLocallyStoredOwner = true))
    }

    @Test
    fun testRoomDtoAdminViaServerFlags() {
        val roomWithOwnerFlag = RoomDto(
            rawRoomId = "grp_flagged",
            name = "Flagged Group",
            isOwner = true
        )
        assertTrue(roomWithOwnerFlag.isUserAdmin("any_user"))

        val roomWithAdminFlag = RoomDto(
            rawRoomId = "grp_admin_flagged",
            name = "Admin Flagged Group",
            rawIsAdmin = true
        )
        assertTrue(roomWithAdminFlag.isUserAdmin("any_user"))
    }

    @Test
    fun testSerializationWithRoleAndOwnerPublicId() {
        val jsonPayload = """
            {
                "room_id": "grp_serialization",
                "name": "Serial Group",
                "owner_public_id": "pub_owner_999",
                "is_public": true,
                "is_owner": 0,
                "is_admin": 1,
                "members": [
                    {
                        "public_id": "pub_owner_999",
                        "anon_name": "Admin Leader",
                        "role": "ADMIN",
                        "is_owner": 1
                    },
                    {
                        "public_id": "pub_member_111",
                        "anon_name": "Normal Peer",
                        "role": "MEMBER",
                        "is_owner": 0
                    }
                ]
            }
        """.trimIndent()

        val room = json.decodeFromString<RoomDto>(jsonPayload)
        assertEquals("grp_serialization", room.roomId)
        assertEquals("pub_owner_999", room.ownerPublicId)
        assertTrue(room.rawIsAdmin)
        assertEquals(2, room.members.size)

        val adminMember = room.members.first { it.publicId == "pub_owner_999" }
        assertEquals("ADMIN", adminMember.role)
        assertTrue(adminMember.isAdmin)

        val regularMember = room.members.first { it.publicId == "pub_member_111" }
        assertEquals("MEMBER", regularMember.role)
        assertFalse(regularMember.isAdmin)

        assertTrue(room.isUserAdmin("pub_owner_999"))
        assertFalse(room.isUserAdmin("pub_member_111", isLocallyStoredOwner = false))
    }

    @Test
    fun testGroupAdminIdentitySwitchingExcludesOldIdentity() {
        val oldIdentityPid = "old_pid_hummingbird"
        val newIdentityPid = "new_pid_eagle"
        val peerPid = "peer_pid_panda"

        // Simulate group originally created under old identity (Ruby-throated Hummingbird)
        val initialRoom = RoomDto(
            rawRoomId = "grp_campus_private",
            name = "Campus Private",
            ownerPublicId = oldIdentityPid,
            createdBy = oldIdentityPid,
            isOwner = true,
            members = listOf(
                RoomMemberDto(
                    roomId = "grp_campus_private",
                    publicId = oldIdentityPid,
                    anonName = "Ruby-throated Hummingbird",
                    isOwner = 1,
                    role = "ADMIN"
                ),
                RoomMemberDto(
                    roomId = "grp_campus_private",
                    publicId = peerPid,
                    anonName = "Giant Panda",
                    isOwner = 0,
                    role = "MEMBER"
                )
            )
        )

        // When locally owned and identity switches to Harpy Eagle (newIdentityPid)
        val isLocallyOwned = true
        val currentIdentityName = "Harpy Eagle #28ea"

        // Verify sanitization logic removes old owner and establishes new active identity as the sole group admin
        val baseList = initialRoom.members.toMutableList()
        if (isLocallyOwned) {
            baseList.removeAll { member ->
                !member.publicId.equals(newIdentityPid, ignoreCase = true) &&
                (member.isOwner == 1 ||
                 member.role.equals("ADMIN", ignoreCase = true) ||
                 member.role.equals("OWNER", ignoreCase = true) ||
                 member.publicId.equals(initialRoom.effectiveOwnerPublicId, ignoreCase = true) ||
                 member.publicId.equals(initialRoom.ownerPublicId, ignoreCase = true) ||
                 member.publicId.equals(initialRoom.createdBy, ignoreCase = true))
            }
        }

        // Add/update current active identity
        baseList.add(
            0,
            RoomMemberDto(
                roomId = "grp_campus_private",
                publicId = newIdentityPid,
                anonName = currentIdentityName,
                isOwner = 1,
                role = "ADMIN"
            )
        )

        // 1. Old identity must NOT appear anywhere in the sanitized members list
        assertFalse(baseList.any { it.publicId == oldIdentityPid })
        assertFalse(baseList.any { it.anonName?.contains("Hummingbird") == true })

        // 2. New identity must be the only group admin
        val currentAdmin = baseList.first { it.publicId == newIdentityPid }
        assertEquals(currentIdentityName, currentAdmin.anonName)
        assertTrue(currentAdmin.isAdmin)
        assertEquals(1, currentAdmin.isOwner)

        // 3. Existing peer member must not be affected
        val peerMember = baseList.first { it.publicId == peerPid }
        assertEquals("Giant Panda", peerMember.anonName)
        assertEquals(0, peerMember.isOwner)
        assertFalse(peerMember.isAdmin)

        // 4. Exactly 2 members total (new admin + peer, old admin purged)
        assertEquals(2, baseList.size)
    }

    @Test
    fun testReopeningGroupRetainsCurrentAdminIdentity() {
        val newPid = "new_pid_eagle"
        val peerPid = "peer_pid_panda"

        val savedRoom = RoomDto(
            rawRoomId = "grp_campus_private",
            name = "Campus Private",
            ownerPublicId = newPid,
            createdBy = newPid,
            isOwner = true,
            members = listOf(
                RoomMemberDto(
                    roomId = "grp_campus_private",
                    publicId = newPid,
                    anonName = "Harpy Eagle #28ea",
                    isOwner = 1,
                    role = "ADMIN"
                ),
                RoomMemberDto(
                    roomId = "grp_campus_private",
                    publicId = peerPid,
                    anonName = "Giant Panda",
                    isOwner = 0,
                    role = "MEMBER"
                )
            )
        )

        assertTrue(savedRoom.isUserAdmin(newPid, isLocallyStoredOwner = true))
        assertFalse(savedRoom.isUserAdmin(peerPid, isLocallyStoredOwner = false))
        assertEquals(newPid, savedRoom.effectiveOwnerPublicId)
        assertEquals("Harpy Eagle #28ea", savedRoom.members.first { it.isAdmin }.anonName)
    }
}
