package org.example.annochat.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

/**
 * Flexible serializer that decodes either a JSON array of strings or a single JSON string
 * into a List<String>. Prevents crashes if server returns a string instead of an array.
 */
object StringOrListSerializer : KSerializer<List<String>> {
    override val descriptor: SerialDescriptor = ListSerializer(String.serializer()).descriptor

    override fun deserialize(decoder: Decoder): List<String> {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return ListSerializer(String.serializer()).deserialize(decoder)
        val element = jsonDecoder.decodeJsonElement()
        return when (element) {
            is JsonArray -> element.mapNotNull { item ->
                when (item) {
                    is JsonPrimitive -> item.content
                    else -> item.toString()
                }
            }
            is JsonPrimitive -> if (element.content.isNotBlank()) listOf(element.content) else emptyList()
            else -> emptyList()
        }
    }

    override fun serialize(encoder: Encoder, value: List<String>) {
        ListSerializer(String.serializer()).serialize(encoder, value)
    }
}

/**
 * Server-side APK update metadata schema returned by GET /api/app/version.
 */
@Serializable
data class AppVersionResponse(
    val latestVersionCode: Long,
    val latestVersionName: String,
    val apkUrl: String = "",
    val downloadUrl: String? = null,
    @Serializable(with = StringOrListSerializer::class)
    val releaseNotes: List<String> = emptyList(),
    val forceUpdate: Boolean = false,
    val minimumSupportedVersionCode: Long = 1,
    val apkSha256: String? = null,
    val signature: String? = null
) {
    val effectiveDownloadUrl: String
        get() = apkUrl.ifBlank { downloadUrl.orEmpty() }

    /**
     * Deterministic, unambiguous byte representation of security-critical metadata fields
     * used for cryptographic signature verification.
     */
    fun toCanonicalPayload(): ByteArray {
        val url = effectiveDownloadUrl.trim()
        val hash = apkSha256?.lowercase()?.trim().orEmpty()
        val lines = listOf(
            "apkSha256=$hash",
            "apkUrl=$url",
            "forceUpdate=$forceUpdate",
            "latestVersionCode=$latestVersionCode",
            "latestVersionName=${latestVersionName.trim()}",
            "minimumSupportedVersionCode=$minimumSupportedVersionCode"
        )
        return lines.joinToString("\n").encodeToByteArray()
    }
}

/**
 * Currently installed local platform application version.
 */
data class AppVersionInfo(
    val versionCode: Long,
    val versionName: String
)
