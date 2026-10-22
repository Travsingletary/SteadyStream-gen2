package com.skyras.tv.playlist

import com.skyras.tv.core.iptv.M3uParser
import com.skyras.tv.core.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class PlaylistLoader(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    suspend fun fetchChannels(m3uUrl: String): List<Channel> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(m3uUrl)
            .header("User-Agent", "SKYRAS-TV/1.0")
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Playlist HTTP ${response.code}")
            }
            val body = response.body?.string().orEmpty()
            val channels = M3uParser.parse(body)
            if (channels.isEmpty()) {
                throw IOException("Playlist contained no channels")
            }
            channels
        }
    }
}
