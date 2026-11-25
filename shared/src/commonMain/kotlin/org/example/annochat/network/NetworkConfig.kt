package org.example.annochat.network

/**
 * Supported runtime network environments for AnonChat.
 */
enum class NetworkEnvironment {
    /** 1. Local Android Studio Emulator accessing host machine via 10.0.2.2 bridge */
    LOCAL_ANDROID_EMULATOR,

    /** 2. Local physical device on the same local Wi-Fi / LAN network */
    LOCAL_PHYSICAL_DEVICE,

    /** 3. Production / Remote public HTTPS & WSS endpoint reachable across any network / mobile data */
    REMOTE
}

/**
 * Centralized network configuration supporting development and remote production environments.
 *
 * REMOTE configuration uses public HTTPS / WSS endpoints so the mobile app works
 * regardless of whether the device is on Wi-Fi, mobile data, or behind a firewall.
 */
data class NetworkConfig(
    val host: String = DEFAULT_DEV_HOST,
    val port: Int = DEFAULT_DEV_PORT,
    val isSecure: Boolean = false,
    val pathPrefix: String = "",
    val environment: NetworkEnvironment = NetworkEnvironment.LOCAL_ANDROID_EMULATOR,
    val discoveryUrl: String? = null
) {
    val cleanHost: String
        get() {
            var h = host.trim()
                .removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("wss://")
                .removePrefix("ws://")
                .trimEnd('/')
            if (h.contains("/")) {
                h = h.substringBefore("/")
            }
            if (h.contains(":")) {
                h = h.substringBefore(":")
            }
            return h
        }

    val isHostConfigured: Boolean
        get() = cleanHost.isNotBlank() && cleanHost != "api" && cleanHost != "undefined"

    val httpBaseUrl: String
        get() {
            val scheme = if (isSecure) "https" else "http"
            val portSuffix = if ((isSecure && port == 443) || (!isSecure && port == 80) || port <= 0) "" else ":$port"
            val prefix = if (pathPrefix.isNotBlank()) "/${pathPrefix.trim('/')}" else ""
            return "$scheme://$cleanHost$portSuffix$prefix"
        }

    val wsBaseUrl: String
        get() {
            val scheme = if (isSecure) "wss" else "ws"
            val portSuffix = if ((isSecure && port == 443) || (!isSecure && port == 80) || port <= 0) "" else ":$port"
            val prefix = if (pathPrefix.isNotBlank()) "/${pathPrefix.trim('/')}" else ""
            return "$scheme://$cleanHost$portSuffix$prefix"
        }

    fun websocketUrl(sessionId: String): String {
        return "$wsBaseUrl/ws?session_id=$sessionId"
    }

    companion object {
        const val DEFAULT_DEV_HOST = "10.0.2.2"
        const val DEFAULT_DEV_PORT = 8000

        const val DEFAULT_PRODUCTION_URL = "https://api.afterclass.bond"

        /**
         * Global runtime override for remote public host (defaults to permanent production domain https://afterclass.bond).
         * When set, defaultNetworkConfig() will prioritize this REMOTE configuration.
         */
        var remotePublicHost: String? = DEFAULT_PRODUCTION_URL

        /**
         * Permanent production endpoint for AfterClass / AnonChat
         * REST: https://afterclass.bond
         * WS:   wss://afterclass.bond/ws
         */
        val Production = remote(DEFAULT_PRODUCTION_URL)

        /**
         * 1. LOCAL_ANDROID_EMULATOR
         * REST: http://10.0.2.2:8000
         * WS:   ws://10.0.2.2:8000/ws
         */
        val AndroidEmulator = NetworkConfig(
            host = "10.0.2.2",
            port = 8000,
            isSecure = false,
            environment = NetworkEnvironment.LOCAL_ANDROID_EMULATOR
        )

        fun localAndroidEmulator(port: Int = 8000): NetworkConfig = NetworkConfig(
            host = "10.0.2.2",
            port = port,
            isSecure = false,
            environment = NetworkEnvironment.LOCAL_ANDROID_EMULATOR
        )

        /**
         * 2. LOCAL_PHYSICAL_DEVICE
         * REST: http://<LAN_IP>:8000
         * WS:   ws://<LAN_IP>:8000/ws
         */
        fun localPhysicalDevice(lanIp: String, port: Int = 8000): NetworkConfig = NetworkConfig(
            host = lanIp.trim(),
            port = port,
            isSecure = false,
            environment = NetworkEnvironment.LOCAL_PHYSICAL_DEVICE
        )

        fun forLan(lanIp: String, port: Int = 8000): NetworkConfig = localPhysicalDevice(lanIp, port)

        /**
         * 3. REMOTE (Production distributed mobile app)
         * REST: https://<PUBLIC_HOST>
         * WS:   wss://<PUBLIC_HOST>/ws
         */
        fun remote(publicHostOrUrl: String, pathPrefix: String = ""): NetworkConfig {
            val isExplicitHttp = publicHostOrUrl.startsWith("http://") || publicHostOrUrl.startsWith("ws://")
            val isSec = !isExplicitHttp
            var clean = publicHostOrUrl.trim()
                .removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("wss://")
                .removePrefix("ws://")
                .trimEnd('/')

            if (clean.contains("?")) {
                clean = clean.substringBefore("?")
            }
            if (clean.endsWith("/api")) {
                clean = clean.removeSuffix("/api").trimEnd('/')
            }
            if (clean.endsWith("/ws")) {
                clean = clean.removeSuffix("/ws").trimEnd('/')
            }

            var parsedPath = pathPrefix
            if (clean.contains("/")) {
                val slashIdx = clean.indexOf("/")
                if (parsedPath.isBlank()) {
                    parsedPath = clean.substring(slashIdx + 1)
                }
                clean = clean.substring(0, slashIdx)
            }

            val parts = clean.split(":")
            val rawHost = parts[0]
            val parsedHost = if (rawHost.equals("afterclass.bond", ignoreCase = true)) {
                "api.afterclass.bond"
            } else {
                rawHost
            }
            val parsedPort = if (parts.size > 1) {
                parts[1].toIntOrNull() ?: (if (isSec) 443 else 80)
            } else {
                if (isSec) 443 else 80
            }

            return NetworkConfig(
                host = parsedHost,
                port = parsedPort,
                isSecure = isSec,
                pathPrefix = parsedPath,
                environment = NetworkEnvironment.REMOTE
            )
        }

        fun forTunnel(tunnelUrlOrHost: String): NetworkConfig = remote(tunnelUrlOrHost)

        /**
         * 4. REMOTE with Dynamic Endpoint Discovery
         * Resolves the changing tunnel endpoint automatically from the permanent discovery address.
         */
        fun remoteDiscovery(
            discoveryUrl: String = DpdnsEndpointDiscoveryService.DEFAULT_PERMANENT_DISCOVERY_URL
        ): NetworkConfig {
            return NetworkConfig(
                host = "",
                port = 443,
                isSecure = true,
                environment = NetworkEnvironment.REMOTE,
                discoveryUrl = discoveryUrl
            )
        }

        /** Simulator / Localhost fallback */
        val IosSimulator = NetworkConfig(
            host = "127.0.0.1",
            port = 8000,
            isSecure = false,
            environment = NetworkEnvironment.LOCAL_PHYSICAL_DEVICE
        )

        val Localhost = IosSimulator

        fun custom(
            host: String,
            port: Int = 8000,
            isSecure: Boolean = false,
            environment: NetworkEnvironment = NetworkEnvironment.LOCAL_PHYSICAL_DEVICE
        ): NetworkConfig {
            return NetworkConfig(
                host = host.trim(),
                port = port,
                isSecure = isSecure,
                environment = environment
            )
        }
    }
}
