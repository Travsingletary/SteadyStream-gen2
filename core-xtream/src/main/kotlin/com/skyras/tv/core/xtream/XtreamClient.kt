package com.skyras.tv.core.xtream

import com.skyras.tv.core.model.Category
import com.skyras.tv.core.model.Channel
import com.skyras.tv.core.model.ContentType
import com.skyras.tv.core.model.SeriesItem
import com.skyras.tv.core.model.VodItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

data class XtreamCredentials(
    val serverUrl: String,
    val username: String,
    val password: String
) {
    val cleanBaseUrl: String
        get() = serverUrl.trim().removeSuffix("/")
}

class XtreamClient(
    private val client: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
) {

    suspend fun authenticate(creds: XtreamCredentials): XtreamAuthResponse = withContext(Dispatchers.IO) {
        val url = "${creds.cleanBaseUrl}/player_api.php?username=${creds.username}&password=${creds.password}"
        val body = executeGet(url)
        json.decodeFromString<XtreamAuthResponse>(body)
    }

    suspend fun getLiveCategories(creds: XtreamCredentials): List<Category> = withContext(Dispatchers.IO) {
        val url = "${creds.cleanBaseUrl}/player_api.php?username=${creds.username}&password=${creds.password}&action=get_live_categories"
        val body = executeGet(url)
        val list = json.decodeFromString<List<XtreamCategory>>(body)
        list.map { Category(id = it.categoryId, name = it.categoryName, type = ContentType.LIVE) }
    }

    suspend fun getVodCategories(creds: XtreamCredentials): List<Category> = withContext(Dispatchers.IO) {
        val url = "${creds.cleanBaseUrl}/player_api.php?username=${creds.username}&password=${creds.password}&action=get_vod_categories"
        val body = executeGet(url)
        val list = json.decodeFromString<List<XtreamCategory>>(body)
        list.map { Category(id = it.categoryId, name = it.categoryName, type = ContentType.VOD) }
    }

    suspend fun getSeriesCategories(creds: XtreamCredentials): List<Category> = withContext(Dispatchers.IO) {
        val url = "${creds.cleanBaseUrl}/player_api.php?username=${creds.username}&password=${creds.password}&action=get_series_categories"
        val body = executeGet(url)
        val list = json.decodeFromString<List<XtreamCategory>>(body)
        list.map { Category(id = it.categoryId, name = it.categoryName, type = ContentType.SERIES) }
    }

    suspend fun getLiveStreams(creds: XtreamCredentials, categoryId: String? = null): List<Channel> = withContext(Dispatchers.IO) {
        val catParam = if (categoryId != null) "&category_id=$categoryId" else ""
        val url = "${creds.cleanBaseUrl}/player_api.php?username=${creds.username}&password=${creds.password}&action=get_live_streams$catParam"
        val body = executeGet(url)
        val streams = json.decodeFromString<List<XtreamLiveStream>>(body)
        streams.map { s ->
            val streamUrl = "${creds.cleanBaseUrl}/live/${creds.username}/${creds.password}/${s.streamId}.m3u8"
            Channel(
                id = "xtream_live_${s.streamId}",
                name = s.name,
                streamUrl = streamUrl,
                logoUrl = s.streamIcon,
                categoryId = s.categoryId,
                epgChannelId = s.epgChannelId,
                channelNumber = s.num,
                type = ContentType.LIVE,
                streamId = s.streamId
            )
        }
    }

    suspend fun getVodStreams(creds: XtreamCredentials, categoryId: String? = null): List<VodItem> = withContext(Dispatchers.IO) {
        val catParam = if (categoryId != null) "&category_id=$categoryId" else ""
        val url = "${creds.cleanBaseUrl}/player_api.php?username=${creds.username}&password=${creds.password}&action=get_vod_streams$catParam"
        val body = executeGet(url)
        val list = json.decodeFromString<List<XtreamVodStream>>(body)
        list.map { v ->
            val ext = v.containerExtension ?: "mp4"
            val streamUrl = "${creds.cleanBaseUrl}/movie/${creds.username}/${creds.password}/${v.streamId}.$ext"
            VodItem(
                streamId = v.streamId,
                name = v.name,
                streamUrl = streamUrl,
                streamIcon = v.streamIcon,
                categoryId = v.categoryId,
                rating = v.rating?.toDoubleOrNull(),
                containerExtension = ext
            )
        }
    }

    suspend fun getSeries(creds: XtreamCredentials, categoryId: String? = null): List<SeriesItem> = withContext(Dispatchers.IO) {
        val catParam = if (categoryId != null) "&category_id=$categoryId" else ""
        val url = "${creds.cleanBaseUrl}/player_api.php?username=${creds.username}&password=${creds.password}&action=get_series$catParam"
        val body = executeGet(url)
        val list = json.decodeFromString<List<XtreamSeries>>(body)
        list.map { s ->
            SeriesItem(
                seriesId = s.seriesId,
                name = s.name,
                coverUrl = s.cover,
                plot = s.plot,
                rating = s.rating?.toDoubleOrNull(),
                categoryId = s.categoryId
            )
        }
    }

    private fun executeGet(url: String): String {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Xtream HTTP error: ${response.code} for URL: $url")
            }
            return response.body?.string() ?: ""
        }
    }
}
