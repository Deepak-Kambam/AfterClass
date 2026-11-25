package org.example.annochat.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.annochat.data.model.AppVersionInfo
import org.example.annochat.data.model.AppVersionResponse
import org.example.annochat.getApkInstaller
import org.example.annochat.getAppVersionInfo
import org.example.annochat.network.ApiClient

sealed interface UpdateState {
    data object Idle : UpdateState

    data class Available(
        val info: AppVersionResponse,
        val isForced: Boolean
    ) : UpdateState

    data class Downloading(
        val info: AppVersionResponse,
        val progress: Float, // 0.0f .. 1.0f (or -1f if indeterminate)
        val bytesRead: Long,
        val totalBytes: Long,
        val isForced: Boolean
    ) : UpdateState

    data class ReadyToInstall(
        val info: AppVersionResponse,
        val isForced: Boolean
    ) : UpdateState

    data class Error(
        val message: String,
        val info: AppVersionResponse?,
        val isForced: Boolean
    ) : UpdateState
}

class UpdateManager(
    private val apiClient: ApiClient,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private var dismissedVersionCode: Long? = null
    private var isChecking = false

    /**
     * Queries the server for the latest APK release metadata asynchronously.
     * Compares installed versionCode with server latestVersionCode.
     * Never interrupts app experience or blocks UI if server is unreachable.
     */
    fun checkForUpdates(forceRefresh: Boolean = false) {
        if (isChecking) return
        val current = _state.value
        if (current is UpdateState.Downloading) return

        scope.launch {
            isChecking = true
            try {
                val installed = getAppVersionInfo()
                val result = apiClient.checkAppVersion().getOrNull() ?: return@launch

                // 1. Audit Requirement: Verify cryptographic signature of update metadata BEFORE proceeding
                val isMetadataAuthentic = org.example.annochat.security.UpdateSecurityVerifier.verifyMetadata(result)
                if (!isMetadataAuthentic) {
                    // Untrusted, missing signature, or tampered metadata: NEVER PROMPT FOR UPDATE
                    _state.value = UpdateState.Idle
                    return@launch
                }

                // 2. Version Check: Compare strictly using versionCode
                val isNewVersion = result.latestVersionCode > installed.versionCode
                val isForced = result.forceUpdate || (installed.versionCode < result.minimumSupportedVersionCode)

                if (isNewVersion) {
                    if (!isForced && !forceRefresh && dismissedVersionCode == result.latestVersionCode) {
                        // User previously opted for "Later" during this active session
                        return@launch
                    }
                    _state.value = UpdateState.Available(result, isForced)
                } else {
                    _state.value = UpdateState.Idle
                }
            } catch (_: Throwable) {
                // Silently ignore network failures during background update checks
            } finally {
                isChecking = false
            }
        }
    }

    /**
     * Begins streaming download of the signed release APK, validates SHA-256 integrity,
     * and triggers the system package installer only after successful verification.
     */
    fun startDownload() {
        val current = _state.value
        val (info, isForced) = when (current) {
            is UpdateState.Available -> current.info to current.isForced
            is UpdateState.ReadyToInstall -> current.info to current.isForced
            is UpdateState.Error -> {
                val inf = current.info ?: return
                inf to current.isForced
            }
            else -> return
        }

        _state.value = UpdateState.Downloading(
            info = info,
            progress = 0f,
            bytesRead = 0L,
            totalBytes = -1L,
            isForced = isForced
        )

        scope.launch {
            val installer = getApkInstaller()
            val expectedHash = info.apkSha256.orEmpty()
            val downloadUrl = info.effectiveDownloadUrl
            val result = installer.downloadAndInstallApk(
                apkUrl = downloadUrl,
                versionCode = info.latestVersionCode,
                expectedSha256 = expectedHash,
                onProgress = { progress, bytesRead, totalBytes ->
                    _state.value = UpdateState.Downloading(
                        info = info,
                        progress = progress,
                        bytesRead = bytesRead,
                        totalBytes = totalBytes,
                        isForced = isForced
                    )
                }
            )

            result.fold(
                onSuccess = {
                    _state.value = UpdateState.ReadyToInstall(info, isForced)
                },
                onFailure = { error ->
                    _state.value = UpdateState.Error(
                        message = error.message ?: "Failed to verify or install update APK",
                        info = info,
                        isForced = isForced
                    )
                }
            )
        }
    }

    /**
     * Dismisses non-forced update dialog for the current app session.
     */
    fun dismissUpdate() {
        val current = _state.value
        if (current is UpdateState.Available && !current.isForced) {
            dismissedVersionCode = current.info.latestVersionCode
            _state.value = UpdateState.Idle
        } else if (current is UpdateState.Error && !current.isForced) {
            _state.value = UpdateState.Idle
        }
    }
}
