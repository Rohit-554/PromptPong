package io.jadu.promptpong

import io.jadu.promptpong.domain.port.AiAvailability
import io.jadu.promptpong.domain.port.AiGenerationConfig
import io.jadu.promptpong.domain.port.AiToken
import io.jadu.promptpong.domain.port.LocalAiEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Replays a fixed response, so prompt and validation logic can be tested with no model. */
class FakeEngine(private val response: String = "") : LocalAiEngine {
    override suspend fun availability() = AiAvailability(
        isAvailable = true,
        needsDownload = false,
        engineName = "Fake",
        statusText = "Fake engine",
    )

    override suspend fun load(): Result<Unit> = Result.success(Unit)

    override fun generate(prompt: String, config: AiGenerationConfig): Flow<AiToken> =
        flowOf(AiToken.Text(response), AiToken.Completed)

    override suspend fun cancel() = Unit
}
