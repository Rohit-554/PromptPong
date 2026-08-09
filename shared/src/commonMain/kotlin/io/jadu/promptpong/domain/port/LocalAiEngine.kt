package io.jadu.promptpong.domain.port

import kotlinx.coroutines.flow.Flow

/** A chunk of generated text, or the end of a generation. */
sealed interface AiToken {
    data class Text(val text: String) : AiToken
    data object Completed : AiToken
    data class Failed(val reason: String) : AiToken
}

data class AiGenerationConfig(
    val maxNewTokens: Int = 60,
    val temperature: Float = 0.8f,
    val topP: Float = 0.95f,
    val topK: Int = 50,
    val stopSequences: List<String> = emptyList(),
    /**
     * Text the answer is forced to begin with, so the model continues a sentence
     * instead of opening with "Here's an idea:". Engines that cannot prefill
     * ignore it; those that can emit it as the first token.
     */
    val assistantPrefill: String = "",
)

/** Whether this device can run local AI, and what to tell the user if it can't. */
data class AiAvailability(
    val isAvailable: Boolean,
    /** Android only: the model must be downloaded before it can be used. */
    val needsDownload: Boolean,
    val engineName: String,
    val statusText: String,
)

/** The one thing commonMain knows about local AI. */
interface LocalAiEngine {
    suspend fun availability(): AiAvailability

    /** Prepare for generation. */
    suspend fun load(): Result<Unit>

    fun generate(prompt: String, config: AiGenerationConfig): Flow<AiToken>

    suspend fun cancel()
}
