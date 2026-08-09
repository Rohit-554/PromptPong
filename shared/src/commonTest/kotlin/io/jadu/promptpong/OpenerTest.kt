package io.jadu.promptpong

import io.jadu.promptpong.data.ai.AiChallengeUpgrader
import io.jadu.promptpong.domain.port.AiAvailability
import io.jadu.promptpong.domain.port.AiGenerationConfig
import io.jadu.promptpong.domain.port.AiToken
import io.jadu.promptpong.domain.port.LocalAiEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class OpenerTest {

    private class RecordingEngine : LocalAiEngine {
        val prefills = mutableListOf<String>()

        override suspend fun availability() = AiAvailability(true, false, "Fake", "Fake")
        override suspend fun load(): Result<Unit> = Result.success(Unit)
        override suspend fun cancel() = Unit

        override fun generate(prompt: String, config: AiGenerationConfig): Flow<AiToken> {
            prefills += config.assistantPrefill
            return flowOf(AiToken.Text(config.assistantPrefill + "a bug."), AiToken.Completed)
        }
    }

    @Test
    fun everyOpenerReadsAsAnInstructionAndEndsWithASpace() {
        AiChallengeUpgrader.openers.forEach { opener ->
            assertTrue(opener.endsWith(" "), "'$opener' must end with a space")
            assertTrue(opener.first().isUpperCase(), "'$opener' must start capitalised")
            assertTrue(opener.trim().isNotEmpty())
        }
    }

    @Test
    fun openersAreAllDistinct() {
        val openers = AiChallengeUpgrader.openers
        assertEquals(openers.size, openers.toSet().size, "duplicate opener")
    }

    @Test
    fun cyclesThroughEveryOpenerBeforeRepeatingOne() = runTest {
        val engine = RecordingEngine()
        val upgrader = AiChallengeUpgrader(engine)
        val total = AiChallengeUpgrader.openers.size

        repeat(total) { upgrader.upgrade("bug") }

        assertEquals(total, engine.prefills.toSet().size, "an opener repeated before the pool ran out")
    }
}
