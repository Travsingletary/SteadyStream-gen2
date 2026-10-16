package com.skyras.tv.core.iptv

import com.skyras.tv.core.model.ContentType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class M3uParserTest {

    @Test
    fun testParseM3uWithStandardAttributes() {
        val sampleM3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="espn.us" tvg-name="ESPN HD" tvg-logo="http://example.com/espn.png" group-title="Sports" tvg-chno="101",ESPN HD
            http://mock-iptv.local/live/espn.m3u8
            #EXTINF:-1 tvg-id="hbo.us" tvg-name="HBO" group-title="Movies",HBO East
            http://mock-iptv.local/live/hbo.ts
            #EXTINF:-1 group-title="VOD Movies",Inception (2010)
            http://mock-iptv.local/vod/movie_42.mp4
        """.trimIndent()

        val channels = M3uParser.parse(sampleM3u)

        assertEquals(3, channels.size)

        val ch1 = channels[0]
        assertEquals("espn.us", ch1.id)
        assertEquals("ESPN HD", ch1.name)
        assertEquals("http://mock-iptv.local/live/espn.m3u8", ch1.streamUrl)
        assertEquals("http://example.com/espn.png", ch1.logoUrl)
        assertEquals("Sports", ch1.categoryName)
        assertEquals(101, ch1.channelNumber)
        assertEquals(ContentType.LIVE, ch1.type)

        val ch2 = channels[1]
        assertEquals("hbo.us", ch2.id)
        assertEquals("HBO East", ch2.name)
        assertEquals("Movies", ch2.categoryName)
        assertEquals(ContentType.LIVE, ch2.type)

        val ch3 = channels[2]
        assertEquals("Inception (2010)", ch3.name)
        assertEquals(ContentType.VOD, ch3.type)
        assertEquals("VOD Movies", ch3.categoryName)
    }

    @Test
    fun testParseM3uWithExtGrpAndBom() {
        val sampleM3u = "\uFEFF#EXTM3U\n" +
                "#EXTINF:-1,Sky Sports Main Event\n" +
                "#EXTGRP:UK Sports\n" +
                "http://mock-iptv.local/stream1\n"

        val channels = M3uParser.parse(sampleM3u)
        assertEquals(1, channels.size)
        assertEquals("Sky Sports Main Event", channels[0].name)
        assertEquals("UK Sports", channels[0].categoryName)
    }

    @Test
    fun testPlaylistRepositoryFavoritesAndSearch() {
        val sampleM3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="1",Skyras News
            http://mock.local/live1
            #EXTINF:-1 tvg-id="2",Skyras Sports
            http://mock.local/live2
        """.trimIndent()

        val repo = PlaylistRepository()
        val channels = M3uParser.parse(sampleM3u)
        repo.loadChannels(channels)

        assertEquals(2, repo.channels.value.size)
        assertFalse(repo.channels.value[0].isFavorite)

        repo.toggleFavorite("1")
        assertTrue(repo.channels.value.first { it.id == "1" }.isFavorite)
        assertFalse(repo.channels.value.first { it.id == "2" }.isFavorite)

        val searchResults = repo.searchChannels("sports")
        assertEquals(1, searchResults.size)
        assertEquals("Skyras Sports", searchResults[0].name)
    }

    @Test
    fun testXmltvParser() {
        val sampleXmltv = """<?xml version="1.0" encoding="utf-8"?>
            <tv>
              <channel id="espn.us">
                <display-name>ESPN HD</display-name>
              </channel>
              <programme start="20261016180000 +0000" stop="20261016210000 +0000" channel="espn.us">
                <title>NBA Friday Showcase: Lakers vs Celtics</title>
                <desc>Live coverage from Boston.</desc>
                <category>Sports</category>
              </programme>
            </tv>
        """.trimIndent()

        val programs = XmltvParser.parse(sampleXmltv)
        assertEquals(1, programs.size)
        val prog = programs[0]
        assertEquals("espn.us", prog.channelId)
        assertEquals("NBA Friday Showcase: Lakers vs Celtics", prog.title)
        assertEquals("Live coverage from Boston.", prog.description)
        assertEquals("Sports", prog.category)
        assertTrue(prog.startEpochMillis > 0)
        assertTrue(prog.endEpochMillis > prog.startEpochMillis)

        // Mid-point test
        val midTime = (prog.startEpochMillis + prog.endEpochMillis) / 2
        assertTrue(prog.isCurrentlyAiring(midTime))
        assertFalse(prog.isCurrentlyAiring(prog.startEpochMillis - 1000))
    }
}
