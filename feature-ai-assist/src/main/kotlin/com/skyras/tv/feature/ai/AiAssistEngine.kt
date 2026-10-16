package com.skyras.tv.feature.ai

import com.skyras.tv.core.iptv.PlaylistRepository
import com.skyras.tv.core.model.Channel
import com.skyras.tv.core.model.EpgProgram

/**
 * AI Assist Engine: Coordinates channel library search, EPG lookup, and LLM reasoning.
 * Handles queries like "find me the game", "what's on right now", "recommend comedies".
 */
class AiAssistEngine(
    private var provider: LlmProvider,
    private val playlistRepository: PlaylistRepository
) {

    fun setProvider(newProvider: LlmProvider) {
        this.provider = newProvider
    }

    suspend fun processQuery(
        query: String,
        history: List<AiChatMessage> = emptyList(),
        currentTimeMillis: Long = System.currentTimeMillis()
    ): AiAssistResponse {
        val trimmedQuery = query.trim()
        val keywords = extractKeywords(trimmedQuery)

        // 1. Search channels
        val allChannels = playlistRepository.channels.value
        val matchedChannels = mutableListOf<Channel>()
        for (channel in allChannels) {
            val nameMatch = keywords.any { kw -> channel.name.contains(kw, ignoreCase = true) }
            val catMatch = keywords.any { kw -> channel.categoryName?.contains(kw, ignoreCase = true) == true }
            if (nameMatch || catMatch) {
                matchedChannels.add(channel)
            }
        }

        // 2. Search EPG programs
        val epgMap = playlistRepository.epgPrograms.value
        val matchedPrograms = mutableListOf<EpgProgram>()
        for ((_, programs) in epgMap) {
            for (prog in programs) {
                val matchesKeyword = keywords.any { kw ->
                    prog.title.contains(kw, ignoreCase = true) ||
                            (prog.description?.contains(kw, ignoreCase = true) == true) ||
                            (prog.category?.contains(kw, ignoreCase = true) == true)
                }
                if (matchesKeyword) {
                    matchedPrograms.add(prog)
                }
            }
        }

        // 3. Build channel context summary for LLM
        val contextSummaryBuilder = StringBuilder()
        contextSummaryBuilder.append("Available channels (${allChannels.size} total):\n")
        allChannels.take(20).forEach { ch ->
            contextSummaryBuilder.append("- ${ch.name} [${ch.categoryName ?: "General"}]\n")
        }
        if (matchedPrograms.isNotEmpty()) {
            contextSummaryBuilder.append("Matched EPG Airings:\n")
            matchedPrograms.take(5).forEach { prog ->
                contextSummaryBuilder.append("- \"${prog.title}\" on channel ${prog.channelId}\n")
            }
        }

        // 4. Query LLM provider
        val llmReply = provider.generateResponse(
            prompt = trimmedQuery,
            conversationHistory = history,
            contextSummary = contextSummaryBuilder.toString()
        )

        // 5. Generate suggested fast-actions
        val suggestedActions = mutableListOf<String>()
        if (matchedChannels.isNotEmpty()) {
            suggestedActions.add("Watch ${matchedChannels.first().name}")
        }
        if (matchedPrograms.isNotEmpty()) {
            suggestedActions.add("View Guide for ${matchedPrograms.first().title}")
        }
        suggestedActions.add("Show all Sports")
        suggestedActions.add("What's on right now?")

        return AiAssistResponse(
            replyText = llmReply,
            matchedChannels = matchedChannels.distinctBy { it.id }.take(10),
            matchedPrograms = matchedPrograms.distinctBy { "${it.channelId}_${it.title}" }.take(10),
            suggestedActions = suggestedActions.distinct()
        )
    }

    private fun extractKeywords(query: String): List<String> {
        val stopWords = setOf("the", "a", "an", "find", "me", "what", "whats", "is", "on", "show", "watch", "can", "you", "i", "want", "to")
        val tokens = query.lowercase()
            .split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.isNotBlank() && !stopWords.contains(it) }

        val expanded = tokens.toMutableList()
        if (tokens.contains("game") || tokens.contains("nba") || tokens.contains("basketball") || tokens.contains("lakers")) {
            expanded.add("sports")
            expanded.add("lakers")
        }
        return expanded.distinct()
    }
}
