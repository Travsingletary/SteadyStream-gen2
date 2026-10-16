package com.skyras.tv.core.xtream

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class XtreamAuthResponse(
    @SerialName("user_info") val userInfo: XtreamUserInfo? = null,
    @SerialName("server_info") val serverInfo: XtreamServerInfo? = null
)

@Serializable
data class XtreamUserInfo(
    val username: String? = null,
    val status: String? = null,
    val exp_date: String? = null,
    val is_trial: String? = null,
    val active_cons: String? = null,
    val max_connections: String? = null,
    val allowed_output_formats: List<String> = emptyList()
)

@Serializable
data class XtreamServerInfo(
    val url: String? = null,
    val port: String? = null,
    val https_port: String? = null,
    val server_protocol: String? = null,
    val timezone: String? = null
)

@Serializable
data class XtreamCategory(
    @SerialName("category_id") val categoryId: String,
    @SerialName("category_name") val categoryName: String,
    @SerialName("parent_id") val parentId: Int? = 0
)

@Serializable
data class XtreamLiveStream(
    val num: Int? = null,
    val name: String,
    @SerialName("stream_type") val streamType: String? = "live",
    @SerialName("stream_id") val streamId: Int,
    @SerialName("stream_icon") val streamIcon: String? = null,
    @SerialName("epg_channel_id") val epgChannelId: String? = null,
    @SerialName("category_id") val categoryId: String? = null
)

@Serializable
data class XtreamVodStream(
    val num: Int? = null,
    val name: String,
    @SerialName("stream_type") val streamType: String? = "movie",
    @SerialName("stream_id") val streamId: Int,
    @SerialName("stream_icon") val streamIcon: String? = null,
    val rating: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("container_extension") val containerExtension: String? = "mp4"
)

@Serializable
data class XtreamSeries(
    val num: Int? = null,
    val name: String,
    @SerialName("series_id") val seriesId: Int,
    val cover: String? = null,
    val plot: String? = null,
    val rating: String? = null,
    @SerialName("category_id") val categoryId: String? = null
)
