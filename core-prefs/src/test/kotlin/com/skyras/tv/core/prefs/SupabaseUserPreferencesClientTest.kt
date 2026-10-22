package com.skyras.tv.core.prefs

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SupabaseUserPreferencesClientTest {

    @Test
    fun getDecodesRow() = runBlocking {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setBody(
                """
                [{"username":"travis","m3u_url":"https://example.com/a.m3u","prefs":{"favoriteIds":["1"],"lastChannelId":"1","lastChannelIndex":0},"updated_at":"2026-01-01T00:00:00Z"}]
                """.trimIndent()
            )
        )
        server.start()
        try {
            val client = SupabaseUserPreferencesClient(
                config = SupabaseConfig(url = server.url("/").toString().trimEnd('/'), anonKey = "test-anon")
            )
            val record = client.get("travis")
            assertEquals("travis", record?.username)
            assertEquals("https://example.com/a.m3u", record?.m3uUrl)
            assertEquals(listOf("1"), record?.prefs?.favoriteIds)
            val request = server.takeRequest()
            assertEquals("test-anon", request.getHeader("apikey"))
            assertTrue(request.path.orEmpty().contains("user_preferences"))
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun upsertPostsJson() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(201))
        server.start()
        try {
            val client = SupabaseUserPreferencesClient(
                config = SupabaseConfig(url = server.url("/").toString().trimEnd('/'), anonKey = "test-anon")
            )
            client.upsert(
                UserPreferencesRecord(
                    username = "travis",
                    m3uUrl = "https://example.com/a.m3u",
                    prefs = UserPrefsPayload(lastChannelIndex = 4),
                    updatedAtEpochMillis = 1_700_000_000_000L
                )
            )
            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertTrue(request.body.readUtf8().contains("\"username\":\"travis\""))
            assertTrue(request.getHeader("Prefer").orEmpty().contains("merge-duplicates"))
        } finally {
            server.shutdown()
        }
    }
}
