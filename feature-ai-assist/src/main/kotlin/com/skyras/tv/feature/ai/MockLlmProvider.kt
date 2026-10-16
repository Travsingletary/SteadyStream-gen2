package com.skyras.tv.feature.ai

/**
 * Mock LLM provider for unit tests, offline operation, and hermetic CI builds.
 * Generates intelligent rule-based responses based on user query keywords.
 */
class MockLlmProvider(
    private val cannedResponses: Map<String, String> = emptyMap()
) : LlmProvider {

    override val providerId: String = "mock"
    override val displayName: String = "SKYRAS Offline Mock AI"

    override suspend fun generateResponse(
        prompt: String,
        conversationHistory: List<AiChatMessage>,
        contextSummary: String
    ): String {
        cannedResponses[prompt]?.let { return it }

        val lower = prompt.lowercase()
        return when {
            lower.contains("game") || lower.contains("nba") || lower.contains("lakers") || lower.contains("sports") -> {
                "I found live sports coverage matching your request! Tune into ESPN HD or Skyras Sports 1 HD."
            }
            lower.contains("what's on") || lower.contains("whats on") || lower.contains("guide") -> {
                "Here is what's currently airing across your subscribed channels. Check out the top live matches in the EPG guide."
            }
            lower.contains("movie") || lower.contains("film") || lower.contains("vod") -> {
                "Looking for a movie? You have VOD titles available in your on-demand catalog."
            }
            else -> {
                "Here are the top channel matches based on your library."
            }
        }
    }
}
