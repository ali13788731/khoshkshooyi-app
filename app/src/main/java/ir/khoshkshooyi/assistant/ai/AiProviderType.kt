package ir.khoshkshooyi.assistant.ai

/** The AI engines the voice assistant can be pointed at. Add new providers here. */
enum class AiProviderType(
    val id: String,
    val displayName: String,
    val keyLabel: String,
    val keyPlaceholder: String
) {
    OPENAI(
        id = "openai",
        displayName = "چت‌جی‌پی‌تی (OpenAI)",
        keyLabel = "کلید API چت‌جی‌پی‌تی (OpenAI)",
        keyPlaceholder = "sk-..."
    ),
    GEMINI(
        id = "gemini",
        displayName = "جمینای (Google Gemini)",
        keyLabel = "کلید API جمینای (Gemini)",
        keyPlaceholder = "AIza..."
    );

    companion object {
        val DEFAULT = OPENAI
        fun fromId(id: String?): AiProviderType = values().find { it.id == id } ?: DEFAULT
    }
}

/** Resolves the concrete client implementation for a given provider. */
fun clientFor(provider: AiProviderType): AiClient = when (provider) {
    AiProviderType.OPENAI -> OpenAIClient
    AiProviderType.GEMINI -> GeminiClient
}
