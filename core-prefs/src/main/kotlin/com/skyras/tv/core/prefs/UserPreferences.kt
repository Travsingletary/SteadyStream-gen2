package com.skyras.tv.core.prefs

import kotlinx.serialization.Serializable

@Serializable
data class UserPrefsPayload(
    val favoriteIds: List<String> = emptyList(),
    val lastChannelId: String? = null,
    val lastChannelIndex: Int = 0
)

@Serializable
data class UserPreferencesRecord(
    val username: String,
    val m3uUrl: String? = null,
    val prefs: UserPrefsPayload = UserPrefsPayload(),
    val updatedAtEpochMillis: Long = 0L
)

data class SupabaseConfig(
    val url: String,
    val anonKey: String
) {
    val restUrl: String
        get() = url.trim().removeSuffix("/") + "/rest/v1"

    fun isConfigured(): Boolean = Companion.isConfigured(url, anonKey)

    companion object {
        const val PLACEHOLDER_URL = "https://YOUR_PROJECT.supabase.co"
        const val PLACEHOLDER_ANON_KEY = "YOUR_SUPABASE_ANON_KEY"

        fun isConfigured(url: String?, anonKey: String?): Boolean {
            val cleanUrl = url?.trim().orEmpty()
            val cleanKey = anonKey?.trim().orEmpty()
            if (cleanUrl.isEmpty() || cleanKey.isEmpty()) return false
            if (cleanUrl.contains("YOUR_PROJECT", ignoreCase = true)) return false
            if (cleanKey.equals(PLACEHOLDER_ANON_KEY, ignoreCase = true)) return false
            if (cleanKey.startsWith("YOUR_")) return false
            return true
        }
    }
}
