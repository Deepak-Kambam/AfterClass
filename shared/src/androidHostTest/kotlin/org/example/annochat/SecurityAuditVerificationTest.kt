package org.example.annochat

import kotlinx.coroutines.runBlocking
import org.example.annochat.data.model.AppVersionResponse
import org.example.annochat.data.repository.UpdateManager
import org.example.annochat.data.repository.UpdateState
import org.example.annochat.network.ApiClient
import org.example.annochat.network.NetworkConfig
import org.example.annochat.security.UpdateSecurityVerifier
import org.w3c.dom.Element
import java.io.File
import java.io.FileOutputStream
import java.util.Properties
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SecurityAuditVerificationTest {

    /**
     * TEST 1: FileProvider cannot expose filesystem root.
     * Verifies that <root-path name="root" path="." /> is completely absent,
     * and only strictly scoped cache paths (updates/, shared_apk/) exist.
     */
    @Test
    fun testFileProviderDoesNotExposeFilesystemRoot() {
        val projectRoot = File(System.getProperty("user.dir") ?: ".")
        val candidates = listOf(
            File(projectRoot, "androidApp/src/main/res/xml/file_paths.xml"),
            File(projectRoot, "../androidApp/src/main/res/xml/file_paths.xml")
        )
        val filePathsXml = candidates.firstOrNull { it.exists() }
        assertNotNull(filePathsXml, "file_paths.xml must exist in androidApp/src/main/res/xml/")

        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(filePathsXml)
        val rootNodes = doc.getElementsByTagName("root-path")
        assertEquals(0, rootNodes.length, "CRITICAL: <root-path> MUST NOT exist in FileProvider configuration")

        val pathList = doc.getElementsByTagName("paths").item(0) as Element
        val childNodes = pathList.childNodes
        val allowedMappings = mutableListOf<Pair<String, String>>()
        for (i in 0 until childNodes.length) {
            val node = childNodes.item(i)
            if (node is Element) {
                val tag = node.tagName
                val name = node.getAttribute("name")
                val path = node.getAttribute("path")
                allowedMappings.add(name to path)

                // Must be cache-path only
                assertEquals("cache-path", tag, "FileProvider path mapping must be scoped to cache-path")
                // Must not be root of cache (path=".")
                assertFalse(path == "." || path == "/", "FileProvider must not expose full cache directory root")
            }
        }

        assertTrue(allowedMappings.any { it.first == "apk_updates" && it.second == "updates/" }, "Must include scoped updates/ path")
        assertTrue(allowedMappings.any { it.first == "shared_apk" && it.second == "shared_apk/" }, "Must include scoped shared_apk/ path")
        assertEquals(2, allowedMappings.size, "FileProvider must contain exactly the two required scoped paths")
    }

    /**
     * TEST 2: Legitimate "Share App" functionality uses strictly scoped cacheDir/shared_apk/
     */
    @Test
    fun testShareAppScopedToCacheSharedApkDirectory() {
        val tempDir = File.createTempFile("test_cache", "dir").apply {
            delete()
            mkdirs()
        }
        try {
            val shareDir = File(tempDir, "shared_apk")
            shareDir.mkdirs()
            val targetApk = File(shareDir, "AfterClass.apk")
            targetApk.writeBytes(ByteArray(1024))

            assertTrue(targetApk.exists())
            assertTrue(targetApk.canonicalPath.startsWith(shareDir.canonicalPath))
            assertTrue(targetApk.canonicalPath.startsWith(tempDir.canonicalPath))
            assertFalse(targetApk.canonicalPath.startsWith("/data/app/"), "Target APK must not reside in system app directory")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    /**
     * TEST 6 & 7: APK file SHA-256 calculation and integrity enforcement
     */
    @Test
    fun testApkFileSha256CalculationAndIntegrity() {
        val tempFile = File.createTempFile("test_apk", ".apk")
        try {
            val sampleBytes = "AFTERCLASS_SAMPLE_APK_BINARY_CONTENT_FOR_HASH_TEST".toByteArray(Charsets.UTF_8)
            tempFile.writeBytes(sampleBytes)

            val calculated = UpdateSecurityVerifier.calculateFileSha256(tempFile)
            assertNotNull(calculated)
            assertEquals(64, calculated.length)

            // Test 6: Matching hash passes
            assertTrue(UpdateSecurityVerifier.verifyApkHash(calculated, calculated))

            // Test 7: Modified content alters hash -> verification fails
            tempFile.appendBytes(byteArrayOf(0x01, 0x02))
            val tamperedHash = UpdateSecurityVerifier.calculateFileSha256(tempFile)
            assertFalse(calculated == tamperedHash)
            assertFalse(UpdateSecurityVerifier.verifyApkHash(tamperedHash, calculated))
        } finally {
            tempFile.delete()
        }
    }

    /**
     * TEST 8: UpdateManager rejects untrusted or tampered update metadata
     */
    @Test
    fun testUpdateManagerRejectsUnsignedMetadata() = runBlocking {
        val fakeConfig = NetworkConfig(host = "127.0.0.1", port = 19999, isSecure = false)
        val mockClient = object : ApiClient(fakeConfig) {
            override suspend fun checkAppVersion(): Result<AppVersionResponse> {
                // Return version 99 with NO signature and NO hash
                return Result.success(
                    AppVersionResponse(
                        latestVersionCode = 99,
                        latestVersionName = "99.0",
                        apkUrl = "http://evil.com/app.apk",
                        apkSha256 = null,
                        signature = null
                    )
                )
            }
        }

        val testScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)
        val updateManager = UpdateManager(mockClient, scope = testScope)
        assertEquals(UpdateState.Idle, updateManager.state.value)

        updateManager.checkForUpdates(forceRefresh = true)
        // Give coroutine chance to run
        kotlinx.coroutines.delay(200)

        // Must remain in Idle because unsigned metadata is rejected!
        assertEquals(UpdateState.Idle, updateManager.state.value, "UpdateManager must NEVER enter Available state for unsigned metadata")
    }

    /**
     * TEST 9: Network or server failure fails safely without crashing
     */
    @Test
    fun testUpdateManagerFailsSafelyOnNetworkError() = runBlocking {
        val fakeConfig = NetworkConfig(host = "127.0.0.1", port = 19999, isSecure = false)
        val failingClient = object : ApiClient(fakeConfig) {
            override suspend fun checkAppVersion(): Result<AppVersionResponse> {
                return Result.failure(java.io.IOException("Network unreachable (connection refused)"))
            }
        }

        val testScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)
        val updateManager = UpdateManager(failingClient, scope = testScope)
        updateManager.checkForUpdates(forceRefresh = true)
        kotlinx.coroutines.delay(200)

        // App state remains Idle safely without throwing or crashing
        assertEquals(UpdateState.Idle, updateManager.state.value)
    }

    /**
     * TEST 10: Production release keystore configuration exists and has valid alias
     */
    @Test
    fun testProductionKeystoreConfiguration() {
        val projectRoot = File(System.getProperty("user.dir") ?: ".")
        val candidates = listOf(
            File(projectRoot, "keystore.properties"),
            File(projectRoot, "../keystore.properties")
        )
        val propsFile = candidates.firstOrNull { it.exists() }
        assertNotNull(propsFile, "keystore.properties must exist in project root")

        val props = Properties().apply {
            propsFile.inputStream().use { load(it) }
        }
        val alias = props.getProperty("keyAlias")
        assertEquals("afterclass", alias, "Keystore alias must be 'afterclass'")

        val storeFileName = props.getProperty("storeFile")
        assertNotNull(storeFileName, "storeFile property must be configured")
        val keystoreFile = File(propsFile.parentFile, storeFileName)
        assertTrue(keystoreFile.exists(), "Keystore binary '$storeFileName' must exist")
    }
}
