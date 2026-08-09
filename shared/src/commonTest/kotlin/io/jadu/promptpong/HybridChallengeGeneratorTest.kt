package io.jadu.promptpong

import io.jadu.promptpong.data.HybridChallengeGenerator
import io.jadu.promptpong.data.template.TemplateChallengeGenerator
import io.jadu.promptpong.domain.model.ChallengeSource
import io.jadu.promptpong.domain.port.ChallengeUpgrader
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

class HybridChallengeGeneratorTest {

    private fun generator(upgrader: ChallengeUpgrader?, timeoutMillis: Long = 15_000) =
        HybridChallengeGenerator(
            templates = TemplateChallengeGenerator(Random(1)),
            upgraderProvider = { upgrader },
            timeoutMillis = timeoutMillis,
        )

    private fun upgraderReturning(text: String?) = object : ChallengeUpgrader {
        override suspend fun upgrade(word: String) = text
    }

    @Test
    fun emitsOnlyTheModelAnswerWhenTheModelReplies() = runTest {
        val results = generator(upgraderReturning("An AI dare about bug.")).generate("bug").toList()

        assertEquals(1, results.size, "the template must not be shown before the model replies")
        assertEquals(ChallengeSource.MODEL, results.single().source)
        assertEquals("An AI dare about bug.", results.single().text)
    }

    @Test
    fun fallsBackToATemplateOnlyWhenTheModelGivesNothing() = runTest {
        val results = generator(upgraderReturning(null)).generate("bug").toList()

        assertEquals(1, results.size)
        assertEquals(ChallengeSource.TEMPLATE, results.single().source)
        assertTrue(results.single().text.contains("bug"))
    }

    @Test
    fun keepsTheTemplateWhenNoModelIsLoaded() = runTest {
        val results = generator(null).generate("bug").toList()

        assertEquals(1, results.size)
        assertEquals(ChallengeSource.TEMPLATE, results.single().source)
    }

    @Test
    fun keepsTheTemplateWhenTheModelRejectsItsOwnOutput() = runTest {
        val results = generator(upgraderReturning(null)).generate("bug").toList()

        assertEquals(1, results.size)
    }

    @Test
    fun keepsTheTemplateWhenTheModelThrows() = runTest {
        val exploding = object : ChallengeUpgrader {
            override suspend fun upgrade(word: String): String =
                throw IllegalStateException("native crash")
        }

        val results = generator(exploding).generate("bug").toList()

        assertEquals(1, results.size, "a model crash must not reach the player")
        assertEquals(ChallengeSource.TEMPLATE, results.single().source)
    }

    @Test
    fun keepsTheTemplateWhenTheModelIsTooSlow() = runTest {
        val slow = object : ChallengeUpgrader {
            override suspend fun upgrade(word: String): String {
                delay(60_000)
                return "too late to be useful"
            }
        }

        val results = generator(slow, timeoutMillis = 50).generate("bug").toList()

        assertEquals(1, results.size)
        assertEquals(ChallengeSource.TEMPLATE, results.single().source)
    }

    @Test
    fun everyRoundGetsAFreshId() = runTest {
        val gen = generator(null)
        val first = gen.generate("bug").toList().single()
        val second = gen.generate("college").toList().single()

        assertTrue(first.id != second.id)
    }
}
