package org.example.annochat

import platform.UIKit.UIDevice
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSISO8601DateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.currentLocale
import platform.Foundation.localTimeZone
import platform.Foundation.timeZoneWithName
import org.example.annochat.network.NetworkConfig

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

private var iosDebugOverride: Boolean? = null

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
actual fun isDebugBuild(): Boolean {
    return iosDebugOverride ?: kotlin.native.Platform.isDebugBinary
}

actual fun setDebugOverrideForTesting(override: Boolean?) {
    iosDebugOverride = override
}

actual fun defaultNetworkConfig(): NetworkConfig {
    val remoteHost = NetworkConfig.remotePublicHost
    return if (!remoteHost.isNullOrBlank()) {
        NetworkConfig.remote(remoteHost)
    } else {
        NetworkConfig.Production
    }
}

actual fun formatIsoTimestampToLocalTime(isoTimestamp: String): String {
    if (isoTimestamp.isBlank()) return ""
    if (isoTimestamp.equals("Just now", ignoreCase = true)) return "Just now"
    return try {
        val normalized = when {
            isoTimestamp.endsWith("Z", ignoreCase = true) -> isoTimestamp
            isoTimestamp.contains("+") || isoTimestamp.matches(Regex(".*-\\d{2}:\\d{2}$")) -> isoTimestamp
            else -> "${isoTimestamp}Z" // Server timestamp is UTC
        }

        val isoFormatter = NSISO8601DateFormatter()
        var date = isoFormatter.dateFromString(normalized)
        if (date == null) {
            val dfUtc = NSDateFormatter().apply {
                dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
                timeZone = NSTimeZone.timeZoneWithName("UTC") ?: NSTimeZone.localTimeZone
                locale = NSLocale.currentLocale
            }
            val clean = isoTimestamp.replace("Z", "").substringBefore(".")
            date = dfUtc.dateFromString(clean)
        }
        if (date == null) {
            return if (isoTimestamp.contains("T")) isoTimestamp.substringAfter("T").take(5) else isoTimestamp.take(8)
        }

        val outputFormatter = NSDateFormatter().apply {
            dateFormat = "HH:mm"
            timeZone = NSTimeZone.localTimeZone
            locale = NSLocale.currentLocale
        }
        outputFormatter.stringFromDate(date)
    } catch (_: Exception) {
        if (isoTimestamp.contains("T")) isoTimestamp.substringAfter("T").take(5) else isoTimestamp.take(8)
    }
}

actual fun generateSecureUuidString(): String = platform.Foundation.NSUUID().UUIDString()

actual fun shareText(text: String, title: String) {
    // iOS platform share fallback
}

actual fun getCurrentLocalHour(): Int {
    val date = platform.Foundation.NSDate()
    val calendar = platform.Foundation.NSCalendar.currentCalendar
    return calendar.component(platform.Foundation.NSCalendarUnitHour, fromDate = date).toInt()
}

actual fun getAppVersionInfo(): org.example.annochat.data.model.AppVersionInfo {
    return org.example.annochat.data.model.AppVersionInfo(versionCode = 1, versionName = "1.0")
}

actual fun getApkInstaller(): ApkInstaller = object : ApkInstaller {
    override suspend fun downloadAndInstallApk(
        apkUrl: String,
        versionCode: Long,
        expectedSha256: String,
        onProgress: (progress: Float, bytesRead: Long, totalBytes: Long) -> Unit
    ): Result<Unit> {
        return Result.failure(UnsupportedOperationException("APK installation is only supported on Android"))
    }
}

actual suspend fun shareApp(): Result<Unit> {
    return Result.failure(UnsupportedOperationException("APK sharing is only supported on Android"))
}

actual fun openBrowserUrl(url: String): Result<Unit> {
    return try {
        val nsUrl = platform.Foundation.NSURL.URLWithString(url)
            ?: return Result.failure(IllegalArgumentException("Invalid URL: $url"))
        if (platform.UIKit.UIApplication.sharedApplication.canOpenURL(nsUrl)) {
            platform.UIKit.UIApplication.sharedApplication.openURL(nsUrl)
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Cannot open web browser on this device."))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}