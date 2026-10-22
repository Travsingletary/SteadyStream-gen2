package com.skyras.tv.core.iptv

import com.skyras.tv.core.model.Channel
import com.skyras.tv.core.model.ContentType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlaylistRepositoryTest {

    @Test
    fun restoreFavoritesMarksChannels() {
        val repo = PlaylistRepository()
        repo.loadChannels(
            listOf(
                Channel("1", "One", "http://a", type = ContentType.LIVE),
                Channel("2", "Two", "http://b", type = ContentType.LIVE)
            )
        )
        repo.restoreFavorites(setOf("2"))
        assertEquals(setOf("2"), repo.favoriteIds.value)
        assertTrue(repo.channels.value.first { it.id == "2" }.isFavorite)
    }
}
