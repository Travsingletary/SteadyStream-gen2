package com.skyras.tv.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ContentType {
    LIVE,
    VOD,
    SERIES
}

@Serializable
data class Category(
    val id: String,
    val name: String,
    val type: ContentType = ContentType.LIVE,
    val parentId: String? = null
)

@Serializable
data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val epgChannelId: String? = null,
    val channelNumber: Int? = null,
    val type: ContentType = ContentType.LIVE,
    val isFavorite: Boolean = false,
    val streamId: Int? = null,
    val containerExtension: String? = null
)

@Serializable
data class VodItem(
    val streamId: Int,
    val name: String,
    val streamUrl: String,
    val streamIcon: String? = null,
    val categoryId: String? = null,
    val rating: Double? = null,
    val plot: String? = null,
    val releaseDate: String? = null,
    val durationSecs: Long? = null,
    val containerExtension: String = "mp4",
    val isFavorite: Boolean = false
)

@Serializable
data class SeriesItem(
    val seriesId: Int,
    val name: String,
    val coverUrl: String? = null,
    val plot: String? = null,
    val releaseDate: String? = null,
    val rating: Double? = null,
    val categoryId: String? = null,
    val seasonsCount: Int = 1,
    val isFavorite: Boolean = false
)

@Serializable
data class Episode(
    val id: String,
    val episodeNum: Int,
    val seasonNum: Int,
    val title: String,
    val containerExtension: String = "mp4",
    val plot: String? = null,
    val durationSecs: Long? = null,
    val streamUrl: String
)

@Serializable
data class EpgProgram(
    val channelId: String,
    val title: String,
    val description: String? = null,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val category: String? = null,
    val iconUrl: String? = null
) {
    fun isCurrentlyAiring(currentTimeMillis: Long): Boolean =
        currentTimeMillis in startEpochMillis until endEpochMillis
}

@Serializable
data class PlaylistSource(
    val id: String,
    val name: String,
    val urlOrPath: String,
    val isLocalFile: Boolean = false,
    val lastUpdatedEpochMillis: Long = 0L,
    val autoRefreshHours: Int = 24
)
