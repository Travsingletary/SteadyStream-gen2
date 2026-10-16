package com.skyras.tv.core.xtream

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class XtreamClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: XtreamClient

    @BeforeEach
    fun setup() {
        server = MockWebServer()
        client = XtreamClient()
    }

    @AfterEach
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun testAuthentication() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path ?: ""
                return when {
                    path.contains("player_api.php") && !path.contains("action=") -> {
                        MockResponse().setResponseCode(200).setBody(
                            """
                            {
                                "user_info": {
                                    "username": "travis_skyras",
                                    "status": "Active",
                                    "exp_date": "1792195200",
                                    "is_trial": "0",
                                    "active_cons": "1",
                                    "max_connections": "5"
                                },
                                "server_info": {
                                    "url": "mock.skyras.tv",
                                    "port": "8080",
                                    "server_protocol": "http",
                                    "timezone": "America/New_York"
                                }
                            }
                            """.trimIndent()
                        )
                    }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        server.start()

        val creds = XtreamCredentials(
            serverUrl = server.url("/").toString(),
            username = "travis_skyras",
            password = "mockPassword123"
        )

        val auth = client.authenticate(creds)
        assertNotNull(auth.userInfo)
        assertEquals("travis_skyras", auth.userInfo?.username)
        assertEquals("Active", auth.userInfo?.status)
        assertEquals("5", auth.userInfo?.max_connections)
        assertEquals("mock.skyras.tv", auth.serverInfo?.url)
    }

    @Test
    fun testGetLiveCategoriesAndStreams() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path ?: ""
                return when {
                    path.contains("action=get_live_categories") -> {
                        MockResponse().setResponseCode(200).setBody(
                            """
                            [
                                {"category_id": "1", "category_name": "SKYRAS Sports", "parent_id": 0},
                                {"category_id": "2", "category_name": "SKYRAS News", "parent_id": 0}
                            ]
                            """.trimIndent()
                        )
                    }
                    path.contains("action=get_live_streams") -> {
                        MockResponse().setResponseCode(200).setBody(
                            """
                            [
                                {
                                    "num": 1,
                                    "name": "SKYRAS Sports 1 HD",
                                    "stream_type": "live",
                                    "stream_id": 1001,
                                    "stream_icon": "http://img.skyras.tv/sports1.png",
                                    "epg_channel_id": "skyras.sports1",
                                    "category_id": "1"
                                }
                            ]
                            """.trimIndent()
                        )
                    }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        server.start()

        val creds = XtreamCredentials(
            serverUrl = server.url("/").toString(),
            username = "user1",
            password = "pass1"
        )

        val categories = client.getLiveCategories(creds)
        assertEquals(2, categories.size)
        assertEquals("SKYRAS Sports", categories[0].name)

        val streams = client.getLiveStreams(creds, categoryId = "1")
        assertEquals(1, streams.size)
        assertEquals("SKYRAS Sports 1 HD", streams[0].name)
        assertEquals(1001, streams[0].streamId)
        assertEquals("${server.url("/")}live/user1/pass1/1001.m3u8", streams[0].streamUrl)
    }

    @Test
    fun testGetVodStreams() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path ?: ""
                return when {
                    path.contains("action=get_vod_streams") -> {
                        MockResponse().setResponseCode(200).setBody(
                            """
                            [
                                {
                                    "num": 1,
                                    "name": "Skyras Original Documentary",
                                    "stream_type": "movie",
                                    "stream_id": 5001,
                                    "stream_icon": "http://img.skyras.tv/doc.png",
                                    "rating": "8.8",
                                    "category_id": "10",
                                    "container_extension": "mkv"
                                }
                            ]
                            """.trimIndent()
                        )
                    }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        server.start()

        val creds = XtreamCredentials(server.url("/").toString(), "user", "pass")
        val vods = client.getVodStreams(creds)
        assertEquals(1, vods.size)
        assertEquals(5001, vods[0].streamId)
        assertEquals(8.8, vods[0].rating)
        assertEquals("mkv", vods[0].containerExtension)
        assertEquals("${server.url("/")}movie/user/pass/5001.mkv", vods[0].streamUrl)
    }
}
