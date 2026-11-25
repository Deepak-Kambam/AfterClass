package org.example.annochat.security

import org.example.annochat.data.model.AppVersionResponse

actual object UpdateSecurityVerifier {

    private const val PRODUCTION_PUBLIC_KEY =
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEsoH3AaraO4m7kQViQYlzXKem6mqYXNoih45wHNb5te6Wq05v6wi88t/+50y8aSNjc4XNtqlEYG9G8OcZVlvdSg=="

    private var testPublicKeyOverride: String? = null

    actual val publicVerificationKey: String
        get() = testPublicKeyOverride ?: PRODUCTION_PUBLIC_KEY

    actual fun setPublicKeyOverrideForTesting(keyBase64: String?) {
        testPublicKeyOverride = keyBase64
    }

    actual fun verifyMetadata(info: AppVersionResponse): Boolean {
        // APK updates are strictly an Android platform feature; rejected on iOS
        return false
    }

    actual fun verifyApkHash(calculatedHash: String, expectedHash: String?): Boolean {
        if (expectedHash.isNullOrBlank()) return false
        return calculatedHash.trim().lowercase() == expectedHash.trim().lowercase()
    }
}
