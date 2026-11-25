package org.example.annochat

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun defaultNetworkConfig(): org.example.annochat.network.NetworkConfig

/**
 * Returns true if the app is currently running in a DEBUG environment.
 * In RELEASE builds, this returns false to enforce production security policies.
 */
expect fun isDebugBuild(): Boolean

/**
 * Platform-agnostic test hook to simulate DEBUG or RELEASE modes during automated unit tests.
 */
expect fun setDebugOverrideForTesting(override: Boolean?)

/**
 * Converts a server ISO-8601 UTC timestamp (e.g. 2026-09-09T08:25:34.123456)
 * to a 24h/12h local device time string (HH:mm) respecting device timezone.
 */
expect fun formatIsoTimestampToLocalTime(isoTimestamp: String): String

/**
 * Generates a cryptographically secure UUID string.
 */
expect fun generateSecureUuidString(): String

/**
 * Shares text across platform native sharing sheets (e.g. Android Intent.ACTION_SEND).
 */
expect fun shareText(text: String, title: String = "Share")

/**
 * Returns the current local hour of the day (0-23) based on system timezone.
 */
expect fun getCurrentLocalHour(): Int

/**
 * Returns installed application package version info (versionCode, versionName).
 */
expect fun getAppVersionInfo(): org.example.annochat.data.model.AppVersionInfo

/**
 * Platform APK downloader and package installer abstraction.
 */
interface ApkInstaller {
    suspend fun downloadAndInstallApk(
        apkUrl: String,
        versionCode: Long,
        expectedSha256: String,
        onProgress: (progress: Float, bytesRead: Long, totalBytes: Long) -> Unit
    ): Result<Unit>
}

expect fun getApkInstaller(): ApkInstaller

/**
 * Shares the installed application package (APK file on Android) via the native system share sheet.
 * Exposes a secure content:// URI with temporary read permissions.
 */
expect suspend fun shareApp(): Result<Unit>

/**
 * Opens an external web URL using the system's default web browser (not an embedded webview).
 */
expect fun openBrowserUrl(url: String): Result<Unit>