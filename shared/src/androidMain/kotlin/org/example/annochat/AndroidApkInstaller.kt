package org.example.annochat

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.example.annochat.data.model.AppVersionInfo
import org.example.annochat.widget.AppContextHolder
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class AndroidApkInstaller : ApkInstaller {

    private val downloadMutex = Mutex()

    override suspend fun downloadAndInstallApk(
        apkUrl: String,
        versionCode: Long,
        expectedSha256: String,
        onProgress: (progress: Float, bytesRead: Long, totalBytes: Long) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val context = AppContextHolder.context
            ?: return@withContext Result.failure(IllegalStateException("Android Application Context is not initialized"))

        downloadMutex.withLock {
            val updatesDir = File(context.cacheDir, "updates")
            val finalApkFile = File(updatesDir, "annochat-$versionCode.apk")
            val tempApkFile = File(updatesDir, "annochat-$versionCode.apk.tmp")

            try {
                // 1. Audit Requirement: Verify expected SHA-256 format (64-character hex string)
                val sanitizedExpectedSha = expectedSha256.trim().lowercase()
                if (!sanitizedExpectedSha.matches(Regex("^[a-f0-9]{64}$"))) {
                    return@withContext Result.failure(
                        SecurityException("Update rejected: Missing or invalid APK SHA-256 verification digest.")
                    )
                }

                // 2. Security Check: Validate URL format and protocol
                val parsedUrl = URL(apkUrl)
                val protocol = parsedUrl.protocol.lowercase()
                val isLocalHost = parsedUrl.host in listOf("10.0.2.2", "127.0.0.1", "localhost")
                if (protocol != "https" && !isLocalHost) {
                    return@withContext Result.failure(
                        SecurityException("Insecure download protocol rejected ($protocol). Updates require trusted HTTPS.")
                    )
                }

                // 3. Prepare Private Updates Directory inside cacheDir (strictly scoped FileProvider path)
                if (!updatesDir.exists()) {
                    updatesDir.mkdirs()
                }

                // 4. If final APK is already cached, check its SHA-256 before reusing
                if (finalApkFile.exists() && finalApkFile.length() > 0) {
                    val cachedSha = org.example.annochat.security.UpdateSecurityVerifier.calculateFileSha256(finalApkFile)
                    if (org.example.annochat.security.UpdateSecurityVerifier.verifyApkHash(cachedSha, sanitizedExpectedSha) && isValidApk(context, finalApkFile)) {
                        onProgress(1.0f, finalApkFile.length(), finalApkFile.length())
                        launchPackageInstaller(context, finalApkFile)
                        return@withContext Result.success(Unit)
                    } else {
                        // Cached file hash mismatch or corrupted -> purge immediately
                        finalApkFile.delete()
                    }
                }

                if (tempApkFile.exists()) {
                    tempApkFile.delete()
                }

                // 5. Download APK with Streaming Progress
                val connection = (parsedUrl.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "AfterClass-Android/${context.packageName}")
                    setRequestProperty("Accept-Encoding", "identity") // Ensure raw content-length
                }

                connection.connect()
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    throw IllegalStateException("Server returned HTTP $responseCode while downloading APK update")
                }

                val totalBytes = connection.contentLengthLong.coerceAtLeast(-1L)
                var bytesReadTotal = 0L

                var inputStream: InputStream? = null
                var outputStream: FileOutputStream? = null

                try {
                    inputStream = connection.inputStream
                    outputStream = FileOutputStream(tempApkFile)

                    val buffer = ByteArray(32 * 1024)
                    var read: Int
                    var lastReportedPercent = -1

                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                        bytesReadTotal += read

                        val progress = if (totalBytes > 0) {
                            (bytesReadTotal.toFloat() / totalBytes).coerceIn(0f, 1f)
                        } else {
                            -1f // Indeterminate if total length unknown
                        }

                        val currentPercent = (progress * 100).toInt()
                        if (currentPercent != lastReportedPercent) {
                            lastReportedPercent = currentPercent
                            onProgress(progress, bytesReadTotal, totalBytes)
                        }
                    }
                    outputStream.flush()
                } finally {
                    try { outputStream?.close() } catch (_: Throwable) {}
                    try { inputStream?.close() } catch (_: Throwable) {}
                    connection.disconnect()
                }

                // 6. Atomically promote temp file to final APK
                if (finalApkFile.exists()) {
                    finalApkFile.delete()
                }
                if (!tempApkFile.renameTo(finalApkFile)) {
                    tempApkFile.copyTo(finalApkFile, overwrite = true)
                    tempApkFile.delete()
                }

                // 7. CRITICAL SECURITY INTEGRITY CHECK: Calculate and verify SHA-256 hash BEFORE installation
                val downloadedSha = org.example.annochat.security.UpdateSecurityVerifier.calculateFileSha256(finalApkFile)
                if (!org.example.annochat.security.UpdateSecurityVerifier.verifyApkHash(downloadedSha, sanitizedExpectedSha)) {
                    // Tampered or corrupted APK detected -> DELETE IMMEDIATELY, NEVER LAUNCH INSTALLER
                    finalApkFile.delete()
                    if (tempApkFile.exists()) tempApkFile.delete()
                    throw SecurityException("APK SHA-256 verification failed. Downloaded APK does not match authenticated release digest.")
                }

                // 8. Package archive structural check
                if (!isValidApk(context, finalApkFile)) {
                    finalApkFile.delete()
                    throw SecurityException("Downloaded update failed Android package integrity validation.")
                }

                onProgress(1.0f, finalApkFile.length(), finalApkFile.length())

                // 9. Launch Official Android Package Installer only after 100% verified integrity
                launchPackageInstaller(context, finalApkFile)

                Result.success(Unit)
            } catch (e: Exception) {
                // Ensure no tampered or partial APK files remain on disk
                try {
                    if (tempApkFile.exists()) tempApkFile.delete()
                } catch (_: Throwable) {}
                Result.failure(e)
            }
        }
    }

    private fun isValidApk(context: Context, file: File): Boolean {
        if (!file.exists() || !file.canRead() || file.length() < 100_000) {
            return false
        }
        return try {
            val packageInfo = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
            packageInfo != null && packageInfo.packageName == context.packageName
        } catch (_: Throwable) {
            false
        }
    }

    private fun launchPackageInstaller(context: Context, apkFile: File) {
        val fileUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(installIntent)
    }
}
