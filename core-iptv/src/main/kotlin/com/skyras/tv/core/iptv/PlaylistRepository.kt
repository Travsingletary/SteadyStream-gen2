package com.skyras.tv.core.iptv

import com.skyras.tv.core.model.Channel
import com.skyras.tv.core.model.ContentType
import com.skyras.tv.core.model.EpgProgram
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory repository for channels, favorites, categories, and EPG.
 */
class PlaylistRepository {
    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    private val _epgPrograms = MutableStateFlow<Map<String, List<EpgProgram>>>(emptyMap())
    val epgPrograms: StateFlow<Map<String, List<EpgProgram>>> = _epgPrograms.asStateFlow()

    fun loadChannels(newChannels: List<Channel>) {
        val favs = _favoriteIds.value
        _channels.value = newChannels.map { channel ->
            channel.copy(isFavorite = favs.contains(channel.id))
        }
    }

    fun restoreFavorites(ids: Set<String>) {
        _favoriteIds.value = ids
        _channels.value = _channels.value.map { channel ->
            channel.copy(isFavorite = ids.contains(channel.id))
        }
    }

    fun toggleFavorite(channelId: String) {
        val currentFavs = _favoriteIds.value.toMutableSet()
        if (currentFavs.contains(channelId)) {
            currentFavs.remove(channelId)
        } else {
            currentFavs.add(channelId)
        }
        _favoriteIds.value = currentFavs

        _channels.value = _channels.value.map { ch ->
            if (ch.id == channelId) {
                ch.copy(isFavorite = currentFavs.contains(channelId))
            } else {
                ch
            }
        }
    }

    fun loadEpg(programs: List<EpgProgram>) {
        val grouped = programs.groupBy { it.channelId }
        _epgPrograms.value = grouped
    }

    fun getCategories(type: ContentType? = null): List<String> {
        return _channels.value
            .filter { type == null || it.type == type }
            .mapNotNull { it.categoryName }
            .distinct()
            .sorted()
    }

    fun searchChannels(query: String): List<Channel> {
        val lower = query.trim().lowercase()
        if (lower.isEmpty()) return _channels.value
        return _channels.value.filter {
            it.name.lowercase().contains(lower) ||
                    (it.categoryName?.lowercase()?.contains(lower) == true)
        }
    }

    fun getNowPlaying(channelId: String, currentTimeMillis: Long = System.currentTimeMillis()): EpgProgram? {
        val list = _epgPrograms.value[channelId] ?: return null
        return list.firstOrNull { it.isCurrentlyAiring(currentTimeMillis) }
    }
}
