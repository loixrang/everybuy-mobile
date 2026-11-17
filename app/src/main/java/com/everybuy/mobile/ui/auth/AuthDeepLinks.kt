package com.everybuy.mobile.ui.auth

import java.net.URLDecoder

/** Deep links understood by the auth shell. */
sealed interface AuthDeepLink {
    data class ResetPassword(val token: String?, val error: String?) : AuthDeepLink
    data class AcceptInvite(val token: String?) : AuthDeepLink
}

/**
 * Parses `everybuy://reset-password?token=…` and
 * `everybuy://invite/accept?token=…` without relying on Android's Uri so it can
 * be unit-tested on the JVM.
 */
fun parseAuthDeepLink(raw: String?): AuthDeepLink? {
    val value = raw?.trim().orEmpty()
    if (!value.startsWith("$AUTH_DEEP_LINK_SCHEME://", ignoreCase = true)) return null
    val remainder = value.substringAfter("://")
    val path = remainder.substringBefore('?')
    val query = remainder.substringAfter('?', "")
    val segments = path.split('/').filter { it.isNotBlank() }
    val host = segments.firstOrNull() ?: return null
    val params = query.split('&').filter { it.isNotBlank() }.mapNotNull { part ->
        val separator = part.indexOf('=')
        if (separator <= 0) null else decode(part.substring(0, separator)) to decode(part.substring(separator + 1))
    }.toMap()
    return when (host.lowercase()) {
        "reset-password" -> AuthDeepLink.ResetPassword(
            token = params["token"]?.takeIf { it.isNotBlank() },
            error = params["error"]?.takeIf { it.isNotBlank() },
        )
        "invite" -> if (segments.getOrNull(1)?.equals("accept", ignoreCase = true) == true) {
            AuthDeepLink.AcceptInvite(params["token"]?.takeIf { it.isNotBlank() })
        } else null
        else -> null
    }
}

const val AUTH_DEEP_LINK_SCHEME = "everybuy"
const val RESET_PASSWORD_REDIRECT = "$AUTH_DEEP_LINK_SCHEME://reset-password"

private fun decode(value: String): String = runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)
