package org.example.annochat.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Pluggable discovery interface for resolving the active backend endpoint.
 *
 * Encapsulates all discovery, network extraction, validation, and caching logic.
 * Consumers (ApiClient, WebSocketClient, ChatRepository) remain decoupled from
 * specific discovery providers (e.g. dpdns, named tunnels, future REST config endpoints).
 */
interface EndpointDiscoveryService {
    /**
     * Resolves the active [NetworkConfig].
     *
     * @param forceRefresh If true, bypasses the in-memory cache and re-queries the discovery source.
     * @return [Result] containing the resolved [NetworkConfig] or failure error.
     */
    suspend fun resolveConfig(forceRefresh: Boolean = false): Result<NetworkConfig>

    /**
     * Returns the currently cached [NetworkConfig], if any.
     */
    fun getCachedConfig(): NetworkConfig?

    /**
     * Clears the current in-memory cache.
     */
    fun invalidateCache()
}

/**
 * Standard implementation of [EndpointDiscoveryService] that discovers the current
 * Cloudflare Quick Tunnel / Named Tunnel URL from a permanent discovery endpoint
 * (e.g. DigitalPlat backend_url.txt, config.js, backend_config.json, or dpdns).
 */
class DpdnsEndpointDiscoveryService(
    val discoveryUrl: String = DEFAULT_PERMANENT_DISCOVERY_URL,
    private val httpClient: HttpClient = createDiscoveryHttpClient()
) : EndpointDiscoveryService {

    private var cachedConfig: NetworkConfig? = null
    private val mutex = Mutex()

    override fun getCachedConfig(): NetworkConfig? = cachedConfig

    override fun invalidateCache() {
        cachedConfig = null
    }

    override suspend fun resolveConfig(forceRefresh: Boolean): Result<NetworkConfig> {
        mutex.withLock {
            if (!forceRefresh && cachedConfig != null) {
                return Result.success(cachedConfig!!)
            }

            println("[EndpointDiscovery] Resolving active backend endpoint (forceRefresh=$forceRefresh)...")
            val candidateUrls = getCandidateUrls(discoveryUrl)
            var lastError: Exception? = null

            for (candidateUrl in candidateUrls) {
                try {
                    println("[EndpointDiscovery] Querying candidate discovery URL: $candidateUrl")
                    val response = httpClient.get(candidateUrl) {
                        headers.append(
                            "User-Agent",
                            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        )
                    }

                    // 1. Check HTTP redirect (if 301/302/307 redirected to a tunnel URL)
                    val redirectedUrl = response.request.url.toString()
                    if (isValidTunnelUrl(redirectedUrl)) {
                        val config = NetworkConfig.remote(redirectedUrl)
                        cachedConfig = config
                        logResolved(candidateUrl, redirectedUrl, config)
                        return Result.success(config)
                    }

                    // 2. Check response body (plain text, JSON, config.js, HTML)
                    val responseText: String = response.bodyAsText()
                    val extractedUrl = extractTunnelUrl(responseText)

                    if (extractedUrl != null && isValidTunnelUrl(extractedUrl)) {
                        val config = NetworkConfig.remote(extractedUrl)
                        cachedConfig = config
                        logResolved(candidateUrl, extractedUrl, config)
                        return Result.success(config)
                    } else {
                        println("[EndpointDiscovery] Could not extract valid tunnel URL from $candidateUrl (response length: ${responseText.length})")
                    }
                } catch (e: Exception) {
                    println("[EndpointDiscovery] Failed to query candidate $candidateUrl: ${e.message}")
                    lastError = e
                }
            }

            // If all candidates failed, check if we have a stale cached config to fallback to
            cachedConfig?.let {
                println("[EndpointDiscovery] Reusing previously cached config: ${it.httpBaseUrl}")
                return Result.success(it)
            }

            val failureMsg = "Failed to resolve active backend endpoint from $discoveryUrl"
            val exception = lastError ?: IllegalStateException(failureMsg)
            return Result.failure(exception)
        }
    }

    private fun logResolved(sourceUrl: String, rawUrl: String, config: NetworkConfig) {
        println("[EndpointDiscovery] SUCCESS: Resolved from [$sourceUrl]")
        println("[EndpointDiscovery]   Extracted URL: $rawUrl")
        println("[EndpointDiscovery]   Clean Host:    ${config.cleanHost}")
        println("[EndpointDiscovery]   REST Base:     ${config.httpBaseUrl}/api")
        println("[EndpointDiscovery]   WebSocket:     ${config.wsBaseUrl}/ws")
    }

    companion object {
        const val DEFAULT_PERMANENT_DISCOVERY_URL = "https://api.afterclass.bond/backend_url.txt"

        // Regex patterns for various discovery formats
        private val CLOUDFLARE_REGEX = Regex("""https?://[a-zA-Z0-9-]+\.trycloudflare\.com""", RegexOption.IGNORE_CASE)
        private val NGROK_REGEX = Regex("""https?://[a-zA-Z0-9-]+\.(?:ngrok-free\.app|ngrok\.app|ngrok\.io)""", RegexOption.IGNORE_CASE)
        private val JSON_KEY_REGEX = Regex("""["'](?:backend_url|api|url|endpoint|server)["']\s*:\s*["'](https?://[^"']+)["']""", RegexOption.IGNORE_CASE)
        private val JS_CONFIG_REGEX = Regex("""(?:window\.)?(?:BACKEND_URL|BACKEND_CONFIG|API_URL)\s*=\s*(?:\{[^}]*["']backend_url["']\s*:\s*)?["'](https?://[^"']+)["']""", RegexOption.IGNORE_CASE)
        private val ANCHOR_HREF_REGEX = Regex("""<a[^>]+href=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        private val JS_LOCATION_REGEX = Regex("""(?:window\.)?location(?:\.href)?\s*=\s*["'](https?://[^"']+)["']""", RegexOption.IGNORE_CASE)
        private val META_REFRESH_REGEX = Regex("""<meta[^>]+http-equiv=["']refresh["'][^>]+content=["'][^"']*url=([^"'\s>]+)["']""", RegexOption.IGNORE_CASE)
        private val META_DESC_REGEX = Regex("""<meta[^>]+(?:name|property)=["'](?:description|og:description|twitter:description)["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)

        /**
         * Builds an ordered list of candidate URLs to query.
         */
        fun getCandidateUrls(baseDiscoveryUrl: String): List<String> {
            val trimmed = baseDiscoveryUrl.trim()
            if (trimmed.isBlank()) return emptyList()

            val withoutQuery = if (trimmed.contains("?")) trimmed.substringBefore("?") else trimmed
            val cleanBase = withoutQuery.trimEnd('/')

            val candidates = mutableListOf<String>()

            // 1. If it targets a specific resource (.txt, .js, .json), prioritize it
            if (cleanBase.endsWith(".txt") || cleanBase.endsWith(".js") || cleanBase.endsWith(".json")) {
                candidates.add(trimmed)
            }

            // 2. Add companion DigitalPlat sync endpoints created by cloud_sync.py
            candidates.add("$cleanBase/backend_url.txt")
            candidates.add("$cleanBase/config.js")
            candidates.add("$cleanBase/backend_config.json")

            // 3. Add base URL itself
            if (!candidates.contains(trimmed)) {
                candidates.add(trimmed)
            }

            return candidates.distinct()
        }

        /**
         * Extracts the active tunnel URL from the discovery response (plaintext, JSON, JS, or HTML).
         */
        fun extractTunnelUrl(content: String): String? {
            if (content.isBlank()) return null

            // 1. Check if the entire response is a plain URL (like backend_url.txt)
            val trimmed = content.trim()
            if (isValidTunnelUrl(trimmed)) {
                return normalizeUrl(trimmed)
            }

            // 2. Check JSON properties (e.g. {"backend_url": "https://...", "api": "https://..."})
            val jsonMatch = JSON_KEY_REGEX.find(content)
            if (jsonMatch != null) {
                val candidate = jsonMatch.groupValues.getOrNull(1)?.trim()
                if (!candidate.isNullOrBlank() && isValidTunnelUrl(candidate)) {
                    return normalizeUrl(candidate)
                }
            }

            // 3. Check JavaScript config assignments (e.g. config.js with window.BACKEND_CONFIG)
            val jsMatch = JS_CONFIG_REGEX.find(content)
            if (jsMatch != null) {
                val candidate = jsMatch.groupValues.getOrNull(1)?.trim()
                if (!candidate.isNullOrBlank() && isValidTunnelUrl(candidate)) {
                    return normalizeUrl(candidate)
                }
            }

            // 4. Check for trycloudflare.com pattern anywhere in the content
            val cfMatch = CLOUDFLARE_REGEX.find(content)
            if (cfMatch != null) {
                return normalizeUrl(cfMatch.value)
            }

            // 5. Check for anchor tag hrefs (e.g. <a href="https://...">Visit Website</a>)
            val anchorMatches = ANCHOR_HREF_REGEX.findAll(content)
            for (match in anchorMatches) {
                val candidate = match.groupValues.getOrNull(1)?.trim() ?: continue
                if (isValidTunnelUrl(candidate)) {
                    return normalizeUrl(candidate)
                }
            }

            // 6. Check for JavaScript location redirects
            val jsLocationMatches = JS_LOCATION_REGEX.findAll(content)
            for (match in jsLocationMatches) {
                val candidate = match.groupValues.getOrNull(1)?.trim() ?: continue
                if (isValidTunnelUrl(candidate)) {
                    return normalizeUrl(candidate)
                }
            }

            // 7. Check for Meta refresh redirects
            val refreshMatches = META_REFRESH_REGEX.findAll(content)
            for (match in refreshMatches) {
                val candidate = match.groupValues.getOrNull(1)?.trim() ?: continue
                if (isValidTunnelUrl(candidate)) {
                    return normalizeUrl(candidate)
                }
            }

            // 8. Check for ngrok patterns
            val ngrokMatch = NGROK_REGEX.find(content)
            if (ngrokMatch != null) {
                return normalizeUrl(ngrokMatch.value)
            }

            // 9. Check for URLs inside meta description tags
            val metaMatches = META_DESC_REGEX.findAll(content)
            for (match in metaMatches) {
                val candidate = match.groupValues.getOrNull(1)?.trim() ?: continue
                if (isValidTunnelUrl(candidate)) {
                    return normalizeUrl(candidate)
                }
            }

            return null
        }

        /**
         * Validates whether a URL is a legitimate public backend tunnel host.
         */
        fun isValidTunnelUrl(url: String): Boolean {
            val clean = url.trim().lowercase()
            if (!clean.startsWith("https://") && !clean.startsWith("http://")) return false

            val hostOnly = clean
                .removePrefix("https://")
                .removePrefix("http://")
                .substringBefore("/")
                .substringBefore(":")
                .trim()

            // Reject invalid, empty, or reserved hostnames
            if (hostOnly.isBlank()) return false
            if (hostOnly == "api") return false
            if (hostOnly == "localhost" || hostOnly == "127.0.0.1" || hostOnly == "10.0.2.2" || hostOnly == "0.0.0.0") return false
            if (hostOnly.startsWith("172.") || hostOnly.startsWith("192.168.") || hostOnly.startsWith("10.") || hostOnly.startsWith("169.254.")) return false
            if (hostOnly == "undefined" || hostOnly == "null") return false

            // Reject discovery hosting domains themselves
            if (hostOnly.contains("dpdns.org") || hostOnly.contains("qd.je") || hostOnly.contains("digitalplat.org")) {
                return false
            }

            // Must contain at least one dot to be a valid FQDN
            return hostOnly.contains(".") && !hostOnly.endsWith(".")
        }

        fun normalizeUrl(rawUrl: String): String {
            var url = rawUrl.trim()

            // Strip accidental trailing paths or query params
            if (url.contains("?")) {
                url = url.substringBefore("?")
            }
            url = url.trimEnd('/')

            if (url.endsWith("/api")) {
                url = url.removeSuffix("/api").trimEnd('/')
            }
            if (url.endsWith("/ws")) {
                url = url.removeSuffix("/ws").trimEnd('/')
            }

            if (url.startsWith("http://")) {
                // Enforce HTTPS for public Cloudflare / ngrok tunnels
                url = "https://" + url.removePrefix("http://")
            } else if (!url.startsWith("https://")) {
                url = "https://$url"
            }
            return url
        }

        fun createDiscoveryHttpClient(): HttpClient {
            return HttpClient {
                install(HttpTimeout) {
                    requestTimeoutMillis = 8000
                    connectTimeoutMillis = 6000
                    socketTimeoutMillis = 8000
                }
            }
        }
    }
}
