package org.example.annochat

import kotlinx.serialization.json.Json
import org.example.annochat.data.model.AppVersionInfo
import org.example.annochat.data.model.AppVersionResponse
import org.example.annochat.data.repository.UpdateState
import org.example.annochat.security.UpdateSecurityVerifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppUpdateTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun testAppVersionResponseJsonParsingWithSecurityMetadata() {
        val payload = """
            {
              "latestVersionCode": 5,
              "latestVersionName": "1.0.4",
              "apkUrl": "https://api.afterclass.bond/updates/AfterClass.apk",
              "downloadUrl": "https://api.afterclass.bond/updates/AfterClass.apk",
              "releaseNotes": [
                "Improved chat performance",
                "Cryptographic update integrity verification"
              ],
              "forceUpdate": false,
              "minimumSupportedVersionCode": 1,
              "apkSha256": "901083b108bf68538110feefbc1a365897d81e783ebc4b4a746caf5f536f74c1",
              "signature": "MEQCIEJ8EORCoi4B2SBCpWa25ynG7us0yXIMnMRuGX/YCryDAiBylQnELzteTIiqe6Cs6ZophGahNYnGxkbioxIsScWgcA=="
            }
        """.trimIndent()

        val parsed = json.decodeFromString<AppVersionResponse>(payload)
        assertEquals(5L, parsed.latestVersionCode)
        assertEquals("1.0.4", parsed.latestVersionName)
        assertEquals("https://api.afterclass.bond/updates/AfterClass.apk", parsed.effectiveDownloadUrl)
        assertEquals(2, parsed.releaseNotes.size)
        assertEquals("901083b108bf68538110feefbc1a365897d81e783ebc4b4a746caf5f536f74c1", parsed.apkSha256)
        assertEquals("MEQCIEJ8EORCoi4B2SBCpWa25ynG7us0yXIMnMRuGX/YCryDAiBylQnELzteTIiqe6Cs6ZophGahNYnGxkbioxIsScWgcA==", parsed.signature)
    }

    /**
     * TEST 4 & TEST 5: Version comparison strictly by versionCode
     */
    @Test
    fun testVersionComparisonStrictlyByVersionCode() {
        // TEST 4: installed is 2, server is 3 -> update available (3 > 2 = true)
        val installedV2 = AppVersionInfo(versionCode = 2, versionName = "1.0.1")
        val serverV3 = AppVersionResponse(
            latestVersionCode = 3,
            latestVersionName = "1.0.2",
            apkUrl = "https://api.afterclass.bond/updates/AfterClass.apk"
        )
        assertTrue(serverV3.latestVersionCode > installedV2.versionCode, "Update dialog MUST appear when server versionCode (3) > installed (2)")

        // TEST 5: installed is 3, server is 3 -> NO update (3 > 3 = false)
        val installedV3 = AppVersionInfo(versionCode = 3, versionName = "1.0.2")
        assertFalse(serverV3.latestVersionCode > installedV3.versionCode, "No update dialog when installed (3) == server (3)")

        // installed is ahead of server -> NO update
        val installedAhead = AppVersionInfo(versionCode = 4, versionName = "1.0.3")
        assertFalse(serverV3.latestVersionCode > installedAhead.versionCode, "No update dialog when installed (4) > server (3)")

        // Ensure versionName is NOT used for comparison (e.g., installed "2.0" with code 2 vs server "1.0" with code 3)
        val installedHigherNameLowerCode = AppVersionInfo(versionCode = 2, versionName = "9.9.9")
        assertTrue(serverV3.latestVersionCode > installedHigherNameLowerCode.versionCode, "Comparison must strictly use versionCode regardless of versionName string")
    }

    /**
     * TEST 8: Valid metadata authenticates successfully on Android platform
     */
    @Test
    fun testMetadataAuthenticityVerificationSuccess() {
        if (getPlatform().name.contains("iOS", ignoreCase = true)) {
            // APK updates are strictly an Android platform feature
            return
        }

        val authenticMetadata = AppVersionResponse(
            latestVersionCode = 3,
            latestVersionName = "1.0.2",
            apkUrl = "https://api.afterclass.bond/updates/AfterClass.apk",
            forceUpdate = false,
            minimumSupportedVersionCode = 1,
            apkSha256 = "fe3d01901b54da6343ef6e20b38d0a49e18c4c1e024c7e7f8a512b8adc0567e9",
            signature = "MEUCID0gEhueonMDD1ITBA4dsh94K+kOtLKt/9qqwuVlzdaSAiEAra5YFVJQZ9bQiGLit17lYjYEywPnVU8ID7rDfOCwGpY="
        )

        val isValid = UpdateSecurityVerifier.verifyMetadata(authenticMetadata)
        assertTrue(isValid, "Authentic metadata signed by release authority MUST verify successfully")
    }

    /**
     * TEST 8: Tampered update metadata MUST fail signature verification -> update MUST NOT proceed
     */
    @Test
    fun testTamperedUpdateMetadataFailsVerification() {
        val baseMetadata = AppVersionResponse(
            latestVersionCode = 3,
            latestVersionName = "1.0.2",
            apkUrl = "https://api.afterclass.bond/updates/AfterClass.apk",
            forceUpdate = false,
            minimumSupportedVersionCode = 1,
            apkSha256 = "fe3d01901b54da6343ef6e20b38d0a49e18c4c1e024c7e7f8a512b8adc0567e9",
            signature = "MEUCID0gEhueonMDD1ITBA4dsh94K+kOtLKt/9qqwuVlzdaSAiEAra5YFVJQZ9bQiGLit17lYjYEywPnVU8ID7rDfOCwGpY="
        )

        // 1. Tampered versionCode (e.g. attacker changes 3 to 4)
        val tamperedCode = baseMetadata.copy(latestVersionCode = 4)
        assertFalse(UpdateSecurityVerifier.verifyMetadata(tamperedCode), "Tampered versionCode must fail signature verification")

        // 2. Tampered APK hash (attacker points to malicious APK hash)
        val tamperedHash = baseMetadata.copy(apkSha256 = "0000000000000000000000000000000000000000000000000000000000000000")
        assertFalse(UpdateSecurityVerifier.verifyMetadata(tamperedHash), "Tampered apkSha256 must fail signature verification")

        // 3. Tampered APK download URL (attacker redirects download to evil.com)
        val tamperedUrl = baseMetadata.copy(apkUrl = "https://evil.example.com/malicious.apk")
        assertFalse(UpdateSecurityVerifier.verifyMetadata(tamperedUrl), "Tampered apkUrl must fail signature verification")

        // 4. Tampered forceUpdate flag
        val tamperedForce = baseMetadata.copy(forceUpdate = true)
        assertFalse(UpdateSecurityVerifier.verifyMetadata(tamperedForce), "Tampered forceUpdate must fail signature verification")

        // 5. Tampered minimumSupportedVersionCode
        val tamperedMinVersion = baseMetadata.copy(minimumSupportedVersionCode = 99)
        assertFalse(UpdateSecurityVerifier.verifyMetadata(tamperedMinVersion), "Tampered minimumSupportedVersionCode must fail signature verification")

        // 6. Missing or empty signature
        val missingSig = baseMetadata.copy(signature = null)
        assertFalse(UpdateSecurityVerifier.verifyMetadata(missingSig), "Unsigned metadata must fail verification")

        // 7. Invalid/corrupted signature bytes
        val corruptedSig = baseMetadata.copy(signature = "AAAA" + baseMetadata.signature?.drop(4))
        assertFalse(UpdateSecurityVerifier.verifyMetadata(corruptedSig), "Corrupted signature must fail verification safely")

        // 8. Missing APK hash
        val missingHash = baseMetadata.copy(apkSha256 = null)
        assertFalse(UpdateSecurityVerifier.verifyMetadata(missingHash), "Missing apkSha256 must fail verification")
    }

    /**
     * TEST 6 & TEST 7: APK SHA-256 integrity verification
     */
    @Test
    fun testApkSha256IntegrityMatching() {
        val expectedHash = "901083b108bf68538110feefbc1a365897d81e783ebc4b4a746caf5f536f74c1"

        // TEST 6: Correct SHA-256 match
        assertTrue(
            UpdateSecurityVerifier.verifyApkHash(expectedHash, expectedHash),
            "Identical SHA-256 hashes must match"
        )
        // Case insensitive match
        assertTrue(
            UpdateSecurityVerifier.verifyApkHash(expectedHash.uppercase(), expectedHash),
            "Case difference in hex representation must match"
        )

        // TEST 7: Corrupted or tampered APK hash mismatch
        val modifiedHash = "901083b108bf68538110feefbc1a365897d81e783ebc4b4a746caf5f536f74c2"
        assertFalse(
            UpdateSecurityVerifier.verifyApkHash(modifiedHash, expectedHash),
            "Modified APK hash MUST NOT match expected hash"
        )

        assertFalse(
            UpdateSecurityVerifier.verifyApkHash(expectedHash, null),
            "Null expected hash must fail verification"
        )

        assertFalse(
            UpdateSecurityVerifier.verifyApkHash(expectedHash, ""),
            "Blank expected hash must fail verification"
        )
    }

    @Test
    fun testForceUpdateRules() {
        val installed = AppVersionInfo(versionCode = 3, versionName = "1.0.2")

        // 1. Explicit forceUpdate flag
        val explicitForce = AppVersionResponse(
            latestVersionCode = 5,
            latestVersionName = "1.0.4",
            apkUrl = "https://example.com/updates/annochat.apk",
            forceUpdate = true,
            minimumSupportedVersionCode = 1
        )
        val isExplicitForced = explicitForce.forceUpdate || (installed.versionCode < explicitForce.minimumSupportedVersionCode)
        assertTrue(isExplicitForced, "forceUpdate = true should mandate update")

        // 2. Minimum supported version code threshold
        val minVersionEnforced = AppVersionResponse(
            latestVersionCode = 5,
            latestVersionName = "1.0.4",
            apkUrl = "https://example.com/updates/annochat.apk",
            forceUpdate = false,
            minimumSupportedVersionCode = 4
        )
        val isMinVersionForced = minVersionEnforced.forceUpdate || (installed.versionCode < minVersionEnforced.minimumSupportedVersionCode)
        assertTrue(isMinVersionForced, "installedVersionCode < minimumSupportedVersionCode must force update")

        // 3. Optional update
        val optionalUpdate = AppVersionResponse(
            latestVersionCode = 5,
            latestVersionName = "1.0.4",
            apkUrl = "https://example.com/updates/annochat.apk",
            forceUpdate = false,
            minimumSupportedVersionCode = 2
        )
        val isOptional = optionalUpdate.forceUpdate || (installed.versionCode < optionalUpdate.minimumSupportedVersionCode)
        assertFalse(isOptional, "Should not be forced when installed >= minimumSupportedVersionCode and forceUpdate is false")
    }

    @Test
    fun testUpdateStateModelRepresentations() {
        val serverInfo = AppVersionResponse(
            latestVersionCode = 5,
            latestVersionName = "1.0.4",
            apkUrl = "https://example.com/updates/annochat.apk",
            releaseNotes = listOf("Security fixes")
        )

        val available = UpdateState.Available(serverInfo, isForced = false)
        assertEquals(5L, available.info.latestVersionCode)
        assertFalse(available.isForced)

        val downloading = UpdateState.Downloading(
            info = serverInfo,
            progress = 0.55f,
            bytesRead = 5_500_000L,
            totalBytes = 10_000_000L,
            isForced = false
        )
        assertEquals(0.55f, downloading.progress)
        assertEquals(5_500_000L, downloading.bytesRead)
        assertEquals(10_000_000L, downloading.totalBytes)

        val ready = UpdateState.ReadyToInstall(serverInfo, isForced = false)
        assertEquals(5L, ready.info.latestVersionCode)

        val error = UpdateState.Error(
            message = "Connection timeout",
            info = serverInfo,
            isForced = false
        )
        assertEquals("Connection timeout", error.message)
    }
}
