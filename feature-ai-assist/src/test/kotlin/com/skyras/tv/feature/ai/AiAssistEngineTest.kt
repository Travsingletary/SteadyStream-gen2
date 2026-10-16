package com.skyras.tv.feature.ai

import com.skyras.tv.core.iptv.PlaylistRepository
import com.skyras.tv.core.model.Channel
import com.skyras.tv.core.model.ContentType
import com.skyras.tv.core.model.EpgProgram
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AiAssistEngineTest {

    @Test
    fun testMockProviderFindTheGame() = runBlocking {
        val repo = PlaylistRepository()
        val sportsChannel = Channel(
            id = "espn_hd",
            name = "ESPN HD",
            streamUrl = "http://mock.tv/espn.m3u8",
            categoryName = "Sports",
            type = ContentType.LIVE
        )
        val newsChannel = Channel(
            id = "cnn_hd",
            name = "CNN News",
            streamUrl = "http://mock.tv/cnn.m3u8",
            categoryName = "News",
            type = ContentType.LIVE
        )
        repo.loadChannels(listOf(sportsChannel, newsChannel))

        val now = System.currentTimeMillis()
        val epg = listOf(
            EpgProgram(
                channelId = "espn_hd",
                title = "NBA Lakers vs Warriors",
                description = "Live basketball championship game",
                startEpochMillis = now - 1000,
                endEpochMillis = now + 5000,
                category = "Sports"
            )
        )
        repo.loadEpg(epg)

        val mockProvider = MockLlmProvider()
        val engine = AiAssistEngine(mockProvider, repo)

        val response = engine.processQuery("find me the NBA game")

        assertTrue(response.replyText.contains("ESPN HD") || response.replyText.contains("sports"))
        assertTrue(response.matchedChannels.any { it.name == "ESPN HD" })
        assertTrue(response.matchedPrograms.any { it.title.contains("Lakers") })
    }

    @Test
    fun testOpenAiCompatibleProviderWithMockWebServer() = runBlocking {
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val body = request.body.readUtf8()
                assertTrue(body.contains("find me comedies"))
                assertEquals("Bearer test-key-123", request.getHeader("Authorization"))

                return MockResponse().setResponseCode(200).setBody(
                    """
                    {
                        "choices": [
                            {
                                "message": {
                                    "role": "assistant",
                                    "content": "I recommend checking out Comedy Central on Channel 42!"
                                }
                            }
                        ]
                    }
                    """.trimIndent()
                )
            }
        }
        server.start()

        val config = OpenAiConfig(
            baseUrl = server.url("/v1").toString(),
            apiKey = "test-key-123",
            model = "gpt-4o-mini"
        )
        val provider = OpenAiCompatibleProvider(config)

        val repo = PlaylistRepository()
        val engine = AiAssistEngine(provider, repo)

        val resp = engine.processQuery("find me comedies")
        server.shutdown()

        assertEquals("I recommend checking out Comedy Central on Channel 42!", resp.replyText)
    }
}
