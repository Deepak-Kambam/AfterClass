package org.example.annochat.security

import org.example.annochat.data.model.AppVersionResponse

/**
 * High-security verification engine for in-app APK updates.
 *
 * Enforces cryptographic authenticity of server update metadata via embedded public key,
 * and verifies SHA-256 integrity of downloaded APK binaries before installation is allowed.
 */
expect object UpdateSecurityVerifier {
    /**
     * Embedded public verification key (Base64-encoded SubjectPublicKeyInfo X.509 format).
     * The corresponding private key is strictly kept offline in the secure release environment.
     */
    val publicVerificationKey: String

    /**
     * Verifies digital signature on update metadata using the trusted embedded public key.
     * Returns true if and only if the metadata signature is mathematically authentic.
     */
    fun verifyMetadata(info: AppVersionResponse): Boolean

    /**
     * Verifies that the computed SHA-256 hash of the downloaded APK matches the expected hash.
     */
    fun verifyApkHash(calculatedHash: String, expectedHash: String?): Boolean

    /**
     * Test hook to override the public key during automated unit tests.
     */
    fun setPublicKeyOverrideForTesting(keyBase64: String?)
}
