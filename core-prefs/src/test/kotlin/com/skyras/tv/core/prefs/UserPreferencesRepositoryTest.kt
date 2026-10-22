package com.skyras.tv.core.prefs

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class UserPreferencesRepositoryTest {

    @Test
    fun localOnlyRoundTrip() = runBlocking {
        val repo = UserPreferencesRepository(local = InMemoryUserPreferencesStore(), remote = null)
        assertNull(repo.load("travis"))
        repo.save(
            UserPreferencesRecord(
                username = "Travis",
                m3uUrl = "https://example.com/tv.m3u",
                prefs = UserPrefsPayload(favoriteIds = listOf("ch1"), lastChannelIndex = 2)
            )
        )
        val loaded = repo.load("travis")
        assertEquals("Travis", loaded?.username)
        assertEquals("https://example.com/tv.m3u", loaded?.m3uUrl)
        assertEquals(listOf("ch1"), loaded?.prefs?.favoriteIds)
        assertEquals(2, loaded?.prefs?.lastChannelIndex)
    }

    @Test
    fun remoteWinsThenMirrorsLocal() = runBlocking {
        val local = InMemoryUserPreferencesStore()
        val remote = InMemoryUserPreferencesStore()
        remote.upsert(
            UserPreferencesRecord(
                username = "travis",
                m3uUrl = "https://remote.example/list.m3u",
                prefs = UserPrefsPayload(lastChannelId = "espn")
            )
        )
        local.upsert(
            UserPreferencesRecord(
                username = "travis",
                m3uUrl = "https://stale.example/list.m3u",
                prefs = UserPrefsPayload(lastChannelId = "old")
            )
        )
        val repo = UserPreferencesRepository(local = local, remote = remote)
        val loaded = repo.load("travis")
        assertEquals("https://remote.example/list.m3u", loaded?.m3uUrl)
        assertEquals("espn", loaded?.prefs?.lastChannelId)
        assertEquals("espn", local.get("travis")?.prefs?.lastChannelId)
    }

    @Test
    fun saveWritesLocalAndRemote() = runBlocking {
        val local = InMemoryUserPreferencesStore()
        val remote = InMemoryUserPreferencesStore()
        val repo = UserPreferencesRepository(local = local, remote = remote)
        repo.save(UserPreferencesRecord(username = "demo", m3uUrl = null))
        assertEquals("demo", local.get("demo")?.username)
        assertEquals("demo", remote.get("demo")?.username)
    }

    @Test
    fun placeholderConfigIsNotLive() {
        assertEquals(false, SupabaseConfig.isConfigured(SupabaseConfig.PLACEHOLDER_URL, SupabaseConfig.PLACEHOLDER_ANON_KEY))
        assertEquals(false, SupabaseConfig.isConfigured("", "anon"))
        assertEquals(true, SupabaseConfig.isConfigured("https://abc.supabase.co", "eyJhbGciOi.real-looking"))
    }
}
