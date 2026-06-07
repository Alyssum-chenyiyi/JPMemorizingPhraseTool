package com.example.memophrasestool

data class DeepSeekRequest(
    val model: String = "deepseek-chat",
    val messages: List<DeepSeekMessage>,
    val response_format: Map<String, String> = mapOf("type" to "json_object")
)

data class DeepSeekMessage(
    val role: String,
    val content: String
)

data class DeepSeekResponse(
    val choices: List<DeepSeekChoice>
)

data class DeepSeekChoice(
    val message: DeepSeekMessage
)

data class AiPhraseResult(
    val phrases: List<AiPhraseItem>
)

data class AiPhraseItem(
    val chinese: String,
    val english: String
)