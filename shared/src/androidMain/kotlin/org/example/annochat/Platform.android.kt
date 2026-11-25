package org.example.annochat

import android.os.Build
import org.example.annochat.network.NetworkConfig
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

object PlatformConfig {
    @Volatile
    private var debugOverride: Boolean? = null

    var isDebug: Boolean
        get() {
            debugOverride?.let { return it }
            return isApplicationDebuggable()
        }
        set(value) {
            debugOverride = value
        }

    fun setDebugOverrideForTesting(override: Boolean?) {
        debugOverride = override
    }

    private fun isApplicationDebuggable(): Boolean {
        return try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val currentApplicationMethod = activityThreadClass.getMethod("currentApplication")
            val app = currentApplicationMethod.invoke(null) as? android.app.Application
            val flags = app?.applicationInfo?.flags ?: 0
            (flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (_: Throwable) {
            false
        }
    }
}

actual fun isDebugBuild(): Boolean = PlatformConfig.isDebug

actual fun setDebugOverrideForTesting(override: Boolean?) {
    PlatformConfig.setDebugOverrideForTesting(override)
}

private fun isRunningOnEmulator(): Boolean {
    val fingerprint = Build.FINGERPRINT?.lowercase() ?: ""
    val model = Build.MODEL?.lowercase() ?: ""
    val hardware = Build.HARDWARE?.lowercase() ?: ""
    val product = Build.PRODUCT?.lowercase() ?: ""

    return fingerprint.startsWith("generic")
            || fingerprint.startsWith("unknown")
            || model.contains("google_sdk")
            || model.contains("emulator")
            || model.contains("android sdk built for x86")
            || hardware.contains("goldfish")
            || hardware.contains("ranchu")
            || product.contains("sdk_google")
}

actual fun defaultNetworkConfig(): NetworkConfig {
    // 1. Emulator uses 10.0.2.2:8000 bridge for local dev ONLY in DEBUG builds
    if (isDebugBuild() && isRunningOnEmulator()) {
        return NetworkConfig.AndroidEmulator
    }

    // 2. Physical hardware and RELEASE builds default to permanent production endpoint https://api.afterclass.bond
    val remoteHost = NetworkConfig.remotePublicHost
    if (!remoteHost.isNullOrBlank()) {
        return NetworkConfig.remote(remoteHost)
    }

    return NetworkConfig.Production
}

actual fun formatIsoTimestampToLocalTime(isoTimestamp: String): String {
    if (isoTimestamp.isBlank()) return ""
    if (isoTimestamp.equals("Just now", ignoreCase = true)) return "Just now"
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val normalized = when {
                isoTimestamp.endsWith("Z", ignoreCase = true) -> isoTimestamp
                isoTimestamp.contains("+") || isoTimestamp.matches(Regex(".*-\\d{2}:\\d{2}$")) -> isoTimestamp
                else -> "${isoTimestamp}Z" // Server timestamp is UTC
            }
            val instant = Instant.parse(normalized)
            val zonedDateTime = instant.atZone(ZoneId.systemDefault())
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            zonedDateTime.format(formatter)
        } else {
            val clean = isoTimestamp.replace("Z", "").substringBefore(".")
            val sdfUtc = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = sdfUtc.parse(clean) ?: return ""
            val sdfLocal = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                timeZone = TimeZone.getDefault()
            }
            sdfLocal.format(date)
        }
    } catch (_: Exception) {
        // Safe fallback if format is unparseable
        if (isoTimestamp.contains("T")) {
            isoTimestamp.substringAfter("T").take(5)
        } else {
            isoTimestamp.take(8)
        }
    }
}

actual fun generateSecureUuidString(): String = java.util.UUID.randomUUID().toString()

actual fun shareText(text: String, title: String) {
    try {
        val appContext = org.example.annochat.widget.AppContextHolder.context ?: return
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, text)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = android.content.Intent.createChooser(intent, title).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(chooser)
    } catch (_: Throwable) {}
}

actual fun getCurrentLocalHour(): Int {
    return java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
}

actual fun getAppVersionInfo(): org.example.annochat.data.model.AppVersionInfo {
    return try {
        val context = org.example.annochat.widget.AppContextHolder.context
            ?: return org.example.annochat.data.model.AppVersionInfo(versionCode = 1, versionName = "1.0")
        val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            pInfo.versionCode.toLong()
        }
        org.example.annochat.data.model.AppVersionInfo(versionCode = vCode, versionName = pInfo.versionName ?: "1.0")
    } catch (_: Throwable) {
        org.example.annochat.data.model.AppVersionInfo(versionCode = 1, versionName = "1.0")
    }
}

actual fun getApkInstaller(): ApkInstaller = AndroidApkInstaller()

private fun isValidReleaseApk(context: android.content.Context, file: java.io.File): Boolean {
    if (!file.exists() || !file.canRead() || file.length() < 1024 * 1024) return false
    return try {
        val pm = context.packageManager
        val archiveInfo = pm.getPackageArchiveInfo(file.absolutePath, 0) ?: return false
        if (archiveInfo.packageName != null && archiveInfo.packageName != context.packageName) return false
        val appInfo = archiveInfo.applicationInfo
        val isDebuggable = appInfo != null && ((appInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0)
        !isDebuggable
    } catch (_: Exception) {
        // If package archive inspection fails, allow valid APK files with standard size
        file.length() > 5 * 1024 * 1024
    }
}

private fun tryExtractBundledReleaseApk(context: android.content.Context, targetFile: java.io.File): Boolean {
    return try {
        val list = context.assets.list("release_apk") ?: return false
        if (!list.contains("AfterClass.apk")) return false
        context.assets.open("release_apk/AfterClass.apk").use { input ->
            java.io.FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
        targetFile.exists() && targetFile.length() > 1024 * 1024
    } catch (_: Exception) {
        false
    }
}

actual suspend fun shareApp(): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    try {
        val context = org.example.annochat.widget.AppContextHolder.context
            ?: return@withContext Result.failure(IllegalStateException("Android Application Context is not initialized"))

        val shareDir = java.io.File(context.cacheDir, "shared_apk")
        if (!shareDir.exists()) {
            shareDir.mkdirs()
        }
        val targetApk = java.io.File(shareDir, "AfterClass.apk")

        val sourceDir = context.applicationInfo?.sourceDir ?: context.packageCodePath
        val sourceApk = if (!sourceDir.isNullOrBlank()) java.io.File(sourceDir) else null

        val isInstalledApkRelease = sourceApk != null && isValidReleaseApk(context, sourceApk)

        val apkToShare: java.io.File = when {
            // 1. If currently installed app is the signed release APK, keep cache synchronized with installed release APK
            isInstalledApkRelease && sourceApk != null -> {
                if (!targetApk.exists() || targetApk.length() != sourceApk.length() || targetApk.lastModified() < sourceApk.lastModified()) {
                    try {
                        sourceApk.copyTo(targetApk, overwrite = true)
                    } catch (_: Exception) {}
                }
                if (targetApk.exists() && targetApk.length() > 0) targetApk else {
                    return@withContext Result.failure(
                        IllegalStateException("Unable to prepare application APK for sharing.")
                    )
                }
            }
            // 2. If cached APK is already a verified signed release APK
            targetApk.exists() && targetApk.length() > 1024 * 1024 && isValidReleaseApk(context, targetApk) -> {
                targetApk
            }
            // 3. Extract bundled release APK from assets (bundled in debug variant for testing)
            tryExtractBundledReleaseApk(context, targetApk) -> {
                targetApk
            }
            // 4. Check external release APK staging if present
            context.getExternalFilesDir("release")?.let { java.io.File(it, "AfterClass.apk") }?.let { if (it.exists() && it.length() > 1024 * 1024) it else null } != null -> {
                val stagedApk = java.io.File(context.getExternalFilesDir("release")!!, "AfterClass.apk")
                try {
                    stagedApk.copyTo(targetApk, overwrite = true)
                } catch (_: Exception) {}
                if (targetApk.exists() && targetApk.length() > 0) targetApk else {
                    return@withContext Result.failure(
                        IllegalStateException("Unable to stage release APK for sharing.")
                    )
                }
            }
            // 5. Fallback: Copy installed APK so sharing succeeds from cacheDir without exposing root
            sourceApk != null && sourceApk.exists() && sourceApk.length() > 0 -> {
                if (!targetApk.exists() || targetApk.length() != sourceApk.length()) {
                    try {
                        sourceApk.copyTo(targetApk, overwrite = true)
                    } catch (_: Exception) {}
                }
                if (targetApk.exists() && targetApk.length() > 0) targetApk else {
                    return@withContext Result.failure(
                        IllegalStateException("Unable to prepare application APK for sharing.")
                    )
                }
            }
            // 6. Fallback to targetApk if it has content
            targetApk.exists() && targetApk.length() > 0 -> {
                targetApk
            }
            else -> {
                return@withContext Result.failure(
                    IllegalStateException("Application APK file is not accessible for sharing.")
                )
            }
        }

        // Secure content:// URI through FileProvider (never file://)
        val authority = "${context.packageName}.fileprovider"
        val apkUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            authority,
            apkToShare
        )

        val shareMessage = "Try AfterClass — anonymous campus conversations."
        val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(android.content.Intent.EXTRA_STREAM, apkUri)
            putExtra(android.content.Intent.EXTRA_TEXT, shareMessage)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "AfterClass App")
            clipData = android.content.ClipData.newRawUri("AfterClass APK", apkUri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = android.content.Intent.createChooser(sendIntent, "Share AfterClass").apply {
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            context.startActivity(chooserIntent)
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

actual fun openBrowserUrl(url: String): Result<Unit> {
    return try {
        val context = org.example.annochat.widget.AppContextHolder.context
            ?: return Result.failure(IllegalStateException("Android Application Context is not initialized"))

        val uri = android.net.Uri.parse(url)
        val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
            addCategory(android.content.Intent.CATEGORY_BROWSABLE)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(browserIntent)
        Result.success(Unit)
    } catch (e: android.content.ActivityNotFoundException) {
        Result.failure(IllegalStateException("No web browser available to open link."))
    } catch (e: Exception) {
        Result.failure(e)
    }
}