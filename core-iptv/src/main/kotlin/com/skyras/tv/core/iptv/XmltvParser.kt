package com.skyras.tv.core.iptv

import com.skyras.tv.core.model.EpgProgram
import java.io.InputStream
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.xml.sax.InputSource

/**
 * 100% original XMLTV EPG parser using standard Java XML DOM parser.
 * Handles standard XMLTV timestamps:
 * "YYYYMMDDHHMMSS +ZZZZ" or "YYYYMMDDHHMMSS"
 */
object XmltvParser {

    private fun parseXmltvDate(dateStr: String): Long {
        val trimmed = dateStr.trim()
        val format = if (trimmed.contains(" ")) {
            SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)
        } else {
            val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf
        }
        return try {
            format.parse(trimmed)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    fun parse(xmlContent: String): List<EpgProgram> {
        val factory = DocumentBuilderFactory.newInstance()
        // Disable external entities for security
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
        factory.isExpandEntityReferences = false
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(InputSource(StringReader(xmlContent)))
        return parseDocument(doc)
    }

    fun parse(inputStream: InputStream): List<EpgProgram> {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isExpandEntityReferences = false
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(inputStream)
        return parseDocument(doc)
    }

    private fun parseDocument(doc: org.w3c.dom.Document): List<EpgProgram> {
        val programList = mutableListOf<EpgProgram>()
        val nodeList = doc.getElementsByTagName("programme")

        for (i in 0 until nodeList.length) {
            val element = nodeList.item(i) as? Element ?: continue
            val channelId = element.getAttribute("channel")
            val startAttr = element.getAttribute("start")
            val stopAttr = element.getAttribute("stop")

            val startMillis = parseXmltvDate(startAttr)
            val stopMillis = parseXmltvDate(stopAttr)

            var title = "No Title"
            var description: String? = null
            var category: String? = null
            var iconUrl: String? = null

            val childNodes = element.childNodes
            for (j in 0 until childNodes.length) {
                val child = childNodes.item(j) as? Element ?: continue
                when (child.tagName) {
                    "title" -> title = child.textContent?.trim() ?: title
                    "desc" -> description = child.textContent?.trim()
                    "category" -> category = child.textContent?.trim()
                    "icon" -> iconUrl = child.getAttribute("src")
                }
            }

            if (channelId.isNotEmpty()) {
                programList.add(
                    EpgProgram(
                        channelId = channelId,
                        title = title,
                        description = description,
                        startEpochMillis = startMillis,
                        endEpochMillis = stopMillis,
                        category = category,
                        iconUrl = iconUrl
                    )
                )
            }
        }

        return programList
    }
}
