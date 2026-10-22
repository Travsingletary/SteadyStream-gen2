package com.skyras.tv.core.prefs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * PostgREST client for `user_preferences`. Uses the anon key only.
 * Not a production auth product — row identity is the username column.
 */
class SupabaseUserPreferencesClient(
    private val config: SupabaseConfig,
    private val http: OkHttpClient = OkHttpClient(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) : UserPreferencesStore {

    override suspend fun get(username: String): UserPreferencesRecord? = withContext(Dispatchers.IO) {
        val encodedUser = username.trim()
        val url = config.restUrl.toHttpUrl()
            .newBuilder()
            .addPathSegment("user_preferences")
            .addQueryParameter("username", "eq.$encodedUser")
            .addQueryParameter("select", "username,m3u_url,prefs,updated_at")
            .addQueryParameter("limit", "1")
            .build()
        val request = authorizedRequest(url.toString()).get().build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Supabase GET ${response.code}: ${response.body?.string().orEmpty()}")
            }
            val body = response.body?.string().orEmpty()
            val rows = json.decodeFromString<List<UserPreferencesRow>>(body)
            rows.firstOrNull()?.toRecord()
        }
    }

    override suspend fun upsert(record: UserPreferencesRecord) = withContext(Dispatchers.IO) {
        val payload = json.encodeToString(
            UserPreferencesRow(
                username = record.username.trim(),
                m3uUrl = record.m3uUrl,
                prefs = record.prefs,
                updatedAt = iso8601(record.updatedAtEpochMillis)
            )
        )
        val url = config.restUrl.toHttpUrl()
            .newBuilder()
            .addPathSegment("user_preferences")
            .build()
        val body = payload.toRequestBody(JSON)
        val request = authorizedRequest(url.toString())
            .header("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(body)
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Supabase UPSERT ${response.code}: ${response.body?.string().orEmpty()}")
            }
        }
    }

    private fun authorizedRequest(url: String): Request.Builder {
        return Request.Builder()
            .url(url)
            .header("apikey", config.anonKey)
            .header("Authorization", "Bearer ${config.anonKey}")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

        fun iso8601(epochMillis: Long): String {
            val millis = if (epochMillis > 0) epochMillis else System.currentTimeMillis()
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            format.timeZone = java.util.TimeZone.getTimeZone("UTC")
            return format.format(java.util.Date(millis))
        }
    }
}

@Serializable
internal data class UserPreferencesRow(
    val username: String,
    @SerialName("m3u_url") val m3uUrl: String? = null,
    val prefs: UserPrefsPayload = UserPrefsPayload(),
    @SerialName("updated_at") val updatedAt: String? = null
) {
    fun toRecord(): UserPreferencesRecord {
        return UserPreferencesRecord(
            username = username,
            m3uUrl = m3uUrl,
            prefs = prefs,
            updatedAtEpochMillis = parseIso(updatedAt)
        )
    }
}

internal fun parseIso(value: String?): Long {
    if (value.isNullOrBlank()) return 0L
    val trimmed = value.trim()
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX"
    )
    for (pattern in patterns) {
        val parsed = runCatching {
            val format = java.text.SimpleDateFormat(pattern, java.util.Locale.US)
            format.timeZone = java.util.TimeZone.getTimeZone("UTC")
            format.parse(trimmed)?.time
        }.getOrNull()
        if (parsed != null) return parsed
    }
    return 0L
}
