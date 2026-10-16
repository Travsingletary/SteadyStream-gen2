package com.skyras.tv.feature.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

data class OpenAiConfig(
    val baseUrl: String = "https://api.openai.com/v1",
    val apiKey: String = "",
    val model: String = "gpt-4o-mini",
    val temperature: Double = 0.7
)

@Serializable
internal data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessagePayload>,
    val temperature: Double = 0.7
)

@Serializable
internal data class OpenAiMessagePayload(
    val role: String,
    val content: String
)

@Serializable
internal data class OpenAiChatResponse(
    val choices: List<OpenAiChoice> = emptyList(),
    val error: OpenAiError? = null
)

@Serializable
internal data class OpenAiChoice(
    val message: OpenAiMessagePayload
)

@Serializable
internal data class OpenAiError(
    val message: String
)

/**
 * Open-AI compatible LLM provider.
 * Connects to OpenAI, Ollama (/v1/chat/completions), LocalAI, Groq, vLLM, etc.
 * Never hardcodes keys; configurable via OpenAiConfig.
 */
class OpenAiCompatibleProvider(
    private val config: OpenAiConfig,
    private val client: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) : LlmProvider {

    override val providerId: String = "openai_compatible"
    override val displayName: String = "Custom OpenAI-compatible Endpoint"

    override suspend fun generateResponse(
        prompt: String,
        conversationHistory: List<AiChatMessage>,
        contextSummary: String
    ): String = withContext(Dispatchers.IO) {
        val endpoint = "${config.baseUrl.trim().removeSuffix("/")}/chat/completions"

        val messagesList = mutableListOf<OpenAiMessagePayload>()
        if (contextSummary.isNotEmpty()) {
            messagesList.add(
                OpenAiMessagePayload(
                    role = "system",
                    content = "You are SKYRAS TV AI Assistant. Help the user discover live channels, EPG programs, and VOD. Channel context:\n$contextSummary"
                )
            )
        }

        conversationHistory.forEach {
            messagesList.add(OpenAiMessagePayload(role = it.role, content = it.content))
        }
        messagesList.add(OpenAiMessagePayload(role = "user", content = prompt))

        val reqPayload = OpenAiChatRequest(
            model = config.model,
            messages = messagesList,
            temperature = config.temperature
        )

        val jsonBody = json.encodeToString(OpenAiChatRequest.serializer(), reqPayload)
        val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .post(body)

        if (config.apiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer ${config.apiKey}")
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            val respBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                throw IOException("LLM API error (${response.code}): $respBody")
            }

            val parsed = json.decodeFromString(OpenAiChatResponse.serializer(), respBody)
            parsed.choices.firstOrNull()?.message?.content
                ?: throw IOException("No completion choice returned from LLM endpoint")
        }
    }
}
