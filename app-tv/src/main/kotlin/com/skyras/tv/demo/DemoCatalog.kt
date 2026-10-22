package com.skyras.tv.demo

import com.skyras.tv.core.model.Channel
import com.skyras.tv.core.model.ContentType
import com.skyras.tv.core.model.EpgProgram

/**
 * Public HLS demo lineup so the sideloaded Fire Stick APK plays without a playlist URL.
 */
object DemoCatalog {
    fun channels(): List<Channel> = listOf(
        Channel(
            id = "mux-hls",
            name = "Mux HLS Test",
            streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            categoryName = "Demo",
            epgChannelId = "mux-hls",
            channelNumber = 101,
            type = ContentType.LIVE
        ),
        Channel(
            id = "apple-bipbop",
            name = "Apple BipBop",
            streamUrl = "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_ts/master.m3u8",
            categoryName = "Demo",
            epgChannelId = "apple-bipbop",
            channelNumber = 102,
            type = ContentType.LIVE
        ),
        Channel(
            id = "mux-tos",
            name = "Mux Tears of Steel",
            streamUrl = "https://test-streams.mux.dev/tos_ismc/main.m3u8",
            categoryName = "Movies",
            epgChannelId = "mux-tos",
            channelNumber = 201,
            type = ContentType.LIVE
        ),
        Channel(
            id = "sintel",
            name = "Sintel HLS",
            streamUrl = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
            categoryName = "Movies",
            epgChannelId = "sintel",
            channelNumber = 202,
            type = ContentType.LIVE
        )
    )

    fun dummyEpg(nowMillis: Long = System.currentTimeMillis()): List<EpgProgram> {
        val hour = 60L * 60L * 1000L
        val half = 30L * 60L * 1000L
        return channels().flatMap { channel ->
            val nowTitle = when (channel.id) {
                "mux-hls" -> "Big Buck Bunny (HLS Test)"
                "apple-bipbop" -> "BipBop Adaptive Demo"
                "mux-tos" -> "Tears of Steel"
                else -> "Sintel"
            }
            val nextTitle = when (channel.id) {
                "mux-hls" -> "Open Source Shorts"
                "apple-bipbop" -> "Codec Stress Loop"
                "mux-tos" -> "Blender Open Movie"
                else -> "Durian Project Feature"
            }
            listOf(
                EpgProgram(
                    channelId = channel.id,
                    title = nowTitle,
                    description = "Demo EPG now block for ${channel.name}",
                    startEpochMillis = nowMillis - half,
                    endEpochMillis = nowMillis + hour,
                    category = channel.categoryName
                ),
                EpgProgram(
                    channelId = channel.id,
                    title = nextTitle,
                    description = "Demo EPG next block for ${channel.name}",
                    startEpochMillis = nowMillis + hour,
                    endEpochMillis = nowMillis + hour + hour,
                    category = channel.categoryName
                )
            )
        }
    }
}
