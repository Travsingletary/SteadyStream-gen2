package com.skyras.tv.prefs

import android.content.Context
import com.skyras.tv.core.prefs.UserPreferencesRecord
import com.skyras.tv.core.prefs.UserPreferencesStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SharedPreferencesUserStore(
    context: Context,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) : UserPreferencesStore {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun get(username: String): UserPreferencesRecord? {
        val key = username.trim().lowercase()
        if (key.isEmpty()) return null
        val blob = prefs.getString(recordKey(key), null) ?: return null
        return runCatching { json.decodeFromString<UserPreferencesRecord>(blob) }.getOrNull()
    }

    override suspend fun upsert(record: UserPreferencesRecord) {
        val key = record.username.trim().lowercase()
        val payload = json.encodeToString(record.copy(username = record.username.trim()))
        prefs.edit().putString(recordKey(key), payload).apply()
    }

    companion object {
        private const val PREFS_NAME = "skyras_user_preferences"

        private fun recordKey(usernameLower: String): String = "record_$usernameLower"
    }
}

class SessionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isSignedIn: Boolean
        get() = prefs.getBoolean(KEY_SIGNED_IN, false)
        set(value) { prefs.edit().putBoolean(KEY_SIGNED_IN, value).apply() }

    var username: String
        get() = prefs.getString(KEY_USERNAME, "").orEmpty()
        set(value) { prefs.edit().putString(KEY_USERNAME, value).apply() }

    var m3uUrl: String?
        get() = prefs.getString(KEY_M3U, null)?.takeIf { it.isNotBlank() }
        set(value) { prefs.edit().putString(KEY_M3U, value).apply() }

    var useDemoCatalog: Boolean
        get() = prefs.getBoolean(KEY_DEMO, false)
        set(value) { prefs.edit().putBoolean(KEY_DEMO, value).apply() }

    fun signIn(username: String, m3uUrl: String?, useDemoCatalog: Boolean) {
        prefs.edit()
            .putBoolean(KEY_SIGNED_IN, true)
            .putString(KEY_USERNAME, username)
            .putString(KEY_M3U, m3uUrl)
            .putBoolean(KEY_DEMO, useDemoCatalog)
            .apply()
    }

    fun signOut() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "skyras_session"
        private const val KEY_SIGNED_IN = "signed_in"
        private const val KEY_USERNAME = "username"
        private const val KEY_M3U = "m3u_url"
        private const val KEY_DEMO = "use_demo"
    }
}
