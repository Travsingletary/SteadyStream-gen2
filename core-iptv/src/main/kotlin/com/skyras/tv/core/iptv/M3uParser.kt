package com.skyras.tv.core.iptv

import com.skyras.tv.core.model.Channel
import com.skyras.tv.core.model.ContentType
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader

/**
 * 100% original, robust M3U/M3U8 parser supporting EXTINF attributes:
 * - tvg-id, tvg-name, tvg-logo, group-title, tvg-chno
 * - fallback group detection (#EXTGRP)
 * - multi-line URI handling and UTF-8 bom stripping
 */
object M3uParser {

    private val ATTRIBUTE_REGEX = Regex("""([a-zA-Z0-9_\-]+)="([^"]*)"""")

    fun parse(content: String): List<Channel> {
        return parse(StringReader(content))
    }

    fun parse(inputStream: InputStream): List<Channel> {
        return parse(InputStreamReader(inputStream, Charsets.UTF_8))
    }

    fun parse(reader: java.io.Reader): List<Channel> {
        val bufferedReader = BufferedReader(reader)
        val channels = mutableListOf<Channel>()

        var currentTvgId: String? = null
        var currentTvgName: String? = null
        var currentTvgLogo: String? = null
        var currentGroupTitle: String? = null
        var currentChNo: Int? = null
        var currentDisplayName: String? = null
        var hasPendingExtInf = false

        bufferedReader.forEachLine { rawLine ->
            var line = rawLine.trim()
            // Strip BOM if present on first line
            if (line.startsWith("\uFEFF")) {
                line = line.substring(1)
            }

            when {
                line.isEmpty() -> {
                    // ignore empty lines
                }
                line.startsWith("#EXTINF:") -> {
                    val extInfPayload = line.removePrefix("#EXTINF:")
                    val commaIndex = extInfPayload.lastIndexOf(',')
                    val attributesPart = if (commaIndex != -1) extInfPayload.substring(0, commaIndex) else extInfPayload
                    val namePart = if (commaIndex != -1) extInfPayload.substring(commaIndex + 1).trim() else ""

                    currentTvgId = null
                    currentTvgName = null
                    currentTvgLogo = null
                    currentGroupTitle = null
                    currentChNo = null
                    currentDisplayName = namePart.ifEmpty { null }

                    ATTRIBUTE_REGEX.findAll(attributesPart).forEach { match ->
                        val key = match.groupValues[1].lowercase()
                        val value = match.groupValues[2].trim()
                        when (key) {
                            "tvg-id" -> currentTvgId = value.ifEmpty { null }
                            "tvg-name" -> currentTvgName = value.ifEmpty { null }
                            "tvg-logo" -> currentTvgLogo = value.ifEmpty { null }
                            "group-title" -> currentGroupTitle = value.ifEmpty { null }
                            "tvg-chno", "channel-id" -> currentChNo = value.toIntOrNull()
                        }
                    }
                    hasPendingExtInf = true
                }
                line.startsWith("#EXTGRP:") -> {
                    val group = line.removePrefix("#EXTGRP:").trim()
                    if (group.isNotEmpty()) {
                        currentGroupTitle = group
                    }
                }
                line.startsWith("#") -> {
                    // Other M3U directive; keep pending extinf state
                }
                else -> {
                    // This is a stream URL
                    if (line.isNotEmpty()) {
                        val finalName = currentDisplayName ?: currentTvgName ?: "Channel ${channels.size + 1}"
                        val finalCategory = currentGroupTitle ?: "Uncategorized"
                        val channelId = currentTvgId ?: "ch_${channels.size + 1}"

                        val isVod = line.endsWith(".mp4", ignoreCase = true) ||
                                line.endsWith(".mkv", ignoreCase = true) ||
                                line.endsWith(".avi", ignoreCase = true)

                        val channel = Channel(
                            id = channelId,
                            name = finalName,
                            streamUrl = line,
                            logoUrl = currentTvgLogo,
                            categoryId = finalCategory.lowercase().replace(" ", "_"),
                            categoryName = finalCategory,
                            epgChannelId = currentTvgId,
                            channelNumber = currentChNo ?: (channels.size + 1),
                            type = if (isVod) ContentType.VOD else ContentType.LIVE
                        )
                        channels.add(channel)

                        // Reset
                        hasPendingExtInf = false
                        currentTvgId = null
                        currentTvgName = null
                        currentTvgLogo = null
                        currentGroupTitle = null
                        currentChNo = null
                        currentDisplayName = null
                    }
                }
            }
        }

        return channels
    }
}
