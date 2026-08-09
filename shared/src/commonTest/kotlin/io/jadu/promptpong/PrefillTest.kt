package io.jadu.promptpong

import io.jadu.promptpong.data.ai.AiChallengeUpgrader
import io.jadu.promptpong.domain.port.AiAvailability
import io.jadu.promptpong.domain.port.AiGenerationConfig
import io.jadu.promptpong.domain.port.AiToken
import io.jadu.promptpong.domain.port.LocalAiEngine
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class PrefillTest {

    private class PrefillEchoEngine : LocalAiEngine {
        var seenPrompt: String = ""

        override suspend fun availability() =
            AiAvailability(true, false, "Fake", "Fake")

        override suspend fun load(): Result<Unit> = Result.success(Unit)

        override fun generate(prompt: String, config: AiGenerationConfig): Flow<AiToken> {
            seenPrompt = prompt
            return flowOf(
                AiToken.Text(config.assistantPrefill),
                AiToken.Text("a bug crawling across the ceiling."),
                AiToken.Completed,
            )
        }

        override suspend fun cancel() = Unit
    }

    @Test
    fun answerStartsWithTheForcedOpener() = runTest {
        val engine = PrefillEchoEngine()
        val text = AiChallengeUpgrader(engine).upgrade("bug")

        assertTrue(text != null && text.first().isUpperCase(), "got: $text")
        assertTrue(text!!.endsWith("a bug crawling across the ceiling."), "got: $text")
        assertTrue(!engine.seenPrompt.contains("party"), "prompt still says party")
    }
}
