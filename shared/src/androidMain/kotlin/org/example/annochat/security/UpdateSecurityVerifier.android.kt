package org.example.annochat.security

import org.example.annochat.data.model.AppVersionResponse
import java.io.File
import java.io.InputStream
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

actual object UpdateSecurityVerifier {

    /**
     * Production EC P-256 Public Verification Key.
     * Base64-encoded SubjectPublicKeyInfo X.509 format.
     *
     * SECURITY INVARIANT:
     * - ONLY this public verification key is embedded inside the application.
     * - The private signing key NEVER resides in the APK or repository.
     */
    const val PRODUCTION_PUBLIC_KEY =
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEsoH3AaraO4m7kQViQYlzXKem6mqYXNoih45wHNb5te6Wq05v6wi88t/+50y8aSNjc4XNtqlEYG9G8OcZVlvdSg=="

    private var testPublicKeyOverride: String? = null

    actual val publicVerificationKey: String
        get() = testPublicKeyOverride ?: PRODUCTION_PUBLIC_KEY

    actual fun setPublicKeyOverrideForTesting(keyBase64: String?) {
        testPublicKeyOverride = keyBase64
    }

    actual fun verifyMetadata(info: AppVersionResponse): Boolean {
        val signatureStr = info.signature?.trim()
        val expectedSha = info.apkSha256?.trim()

        // 1. Mandatory presence check for signature and hash
        if (signatureStr.isNullOrBlank() || expectedSha.isNullOrBlank()) {
            return false
        }

        // 2. Validate expected SHA-256 format (must be 64 hexadecimal characters)
        if (!expectedSha.matches(Regex("^[a-fA-F0-9]{64}$"))) {
            return false
        }

        // 3. Authenticate cryptographic signature against canonical metadata
        return try {
            val pubKeyBytes = decodeBase64(publicVerificationKey)
            val keyFactory = KeyFactory.getInstance("EC")
            val pubKey: PublicKey = keyFactory.generatePublic(X509EncodedKeySpec(pubKeyBytes))

            val signatureBytes = decodeBase64(signatureStr)
            val verifier = Signature.getInstance("SHA256withECDSA")
            verifier.initVerify(pubKey)

            val canonicalBytes = info.toCanonicalPayload()
            verifier.update(canonicalBytes)
            verifier.verify(signatureBytes)
        } catch (_: Throwable) {
            false
        }
    }

    actual fun verifyApkHash(calculatedHash: String, expectedHash: String?): Boolean {
        if (expectedHash.isNullOrBlank()) return false
        val cleanCalculated = calculatedHash.trim().lowercase()
        val cleanExpected = expectedHash.trim().lowercase()
        return cleanCalculated == cleanExpected
    }

    /**
     * Computes the SHA-256 checksum of a file by streaming its bytes in chunks.
     */
    fun calculateFileSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Computes the SHA-256 checksum of an InputStream.
     */
    fun calculateStreamSha256(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun decodeBase64(str: String): ByteArray {
        val clean = str.replace("\n", "").replace("\r", "").trim()
        return try {
            android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
        } catch (_: Throwable) {
            java.util.Base64.getDecoder().decode(clean)
        }
    }
}
