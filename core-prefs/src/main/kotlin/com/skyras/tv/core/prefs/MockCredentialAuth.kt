package com.skyras.tv.core.prefs

/**
 * Local mock sign-in. There is no production auth server.
 *
 * Rules:
 * - Username and password are required (password at least 3 characters).
 * - Optional M3U URL is stored as-is when provided.
 * - A tiny in-app map can supply a default playlist URL for known usernames.
 * - The reserved username `demo` (any password ≥ 3 chars, no URL) uses the
 *   built-in HLS demo catalog instead of fetching a playlist.
 */
object MockCredentialAuth {
    val defaultPlaylistsByUsername: Map<String, String?> = mapOf(
        "demo" to null
    )

    fun authenticate(username: String, password: String, playlistUrl: String?): SignInOutcome {
        val user = username.trim()
        val pass = password
        val url = playlistUrl?.trim()?.takeIf { it.isNotEmpty() }

        if (user.isEmpty()) {
            return SignInOutcome.Failure("Enter a username")
        }
        if (pass.isBlank()) {
            return SignInOutcome.Failure("Enter a password")
        }
        if (pass.length < 3) {
            return SignInOutcome.Failure("Password must be at least 3 characters")
        }
        if (url != null && !looksLikeUrl(url)) {
            return SignInOutcome.Failure("Playlist URL must start with http:// or https://")
        }

        val mapped = defaultPlaylistsByUsername[user.lowercase()]
        val resolvedUrl = url ?: mapped
        val useDemoCatalog = resolvedUrl.isNullOrBlank()
        return SignInOutcome.Success(
            username = user,
            m3uUrl = resolvedUrl,
            useDemoCatalog = useDemoCatalog
        )
    }

    fun looksLikeUrl(value: String): Boolean {
        val lower = value.lowercase()
        return lower.startsWith("http://") || lower.startsWith("https://")
    }
}

sealed class SignInOutcome {
    data class Success(
        val username: String,
        val m3uUrl: String?,
        val useDemoCatalog: Boolean
    ) : SignInOutcome()

    data class Failure(val message: String) : SignInOutcome()
}
