package com.skyras.tv.feature.ai

import com.skyras.tv.core.model.Channel
import com.skyras.tv.core.model.EpgProgram
import kotlinx.serialization.Serializable

@Serializable
data class AiChatMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

@Serializable
data class AiAssistResponse(
    val replyText: String,
    val matchedChannels: List<Channel> = emptyList(),
    val matchedPrograms: List<EpgProgram> = emptyList(),
    val suggestedActions: List<String> = emptyList()
)

interface LlmProvider {
    val providerId: String
    val displayName: String

    suspend fun generateResponse(
        prompt: String,
        conversationHistory: List<AiChatMessage> = emptyList(),
        contextSummary: String = ""
    ): String
}
