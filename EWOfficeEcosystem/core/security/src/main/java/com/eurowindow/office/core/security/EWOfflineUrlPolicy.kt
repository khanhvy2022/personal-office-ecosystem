package com.eurowindow.office.core.security

import java.net.URI
import java.util.Locale

/**
 * S1 Remediation: Strict offline URL scheme & path validator for WebView and internal asset loading.
 * Permits only in-process Collabora MobileSocket (`cool://`), packaged Android assets (`file:///android_asset/`),
 * inline `data:`, `blob:`, and `javascript:` evaluation.
 * Blocks all external network schemes (`http`, `https`, `ws`, `wss`, `ftp`) and arbitrary local filesystem paths.
 */
object EWOfflineUrlPolicy {

    private val ALLOWED_NON_FILE_SCHEMES = setOf("cool", "data", "blob", "javascript")
    private val BLOCKED_NETWORK_SCHEMES = setOf("http", "https", "ws", "wss", "ftp", "ftps", "intent", "tel", "mailto")

    @JvmStatic
    fun isAllowedUrl(rawUrl: String?): Boolean {
        if (rawUrl.isNullOrBlank()) return false
        val trimmed = rawUrl.trim()
        val colonIndex = trimmed.indexOf(':')
        if (colonIndex <= 0) return false

        val scheme = trimmed.substring(0, colonIndex).lowercase(Locale.ROOT)
        if (scheme in BLOCKED_NETWORK_SCHEMES) {
            return false
        }
        if (scheme in ALLOWED_NON_FILE_SCHEMES) {
            return true
        }
        if (scheme == "file") {
            return try {
                val parsed = URI(trimmed)
                val path = parsed.path ?: return false
                // Normalize path traversal attempts before checking prefix
                val normalized = parsed.normalize().path ?: return false
                path.startsWith("/android_asset/") &&
                    normalized.startsWith("/android_asset/") &&
                    !path.contains("..")
            } catch (_: Exception) {
                trimmed.startsWith("file:///android_asset/") && !trimmed.contains("..")
            }
        }
        return false
    }
}
