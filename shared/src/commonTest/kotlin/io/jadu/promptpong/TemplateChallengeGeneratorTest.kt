package io.jadu.promptpong

import io.jadu.promptpong.data.template.TemplateChallengeGenerator
import io.jadu.promptpong.domain.model.ChallengeSource
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TemplateChallengeGeneratorTest {

    private fun generator() = TemplateChallengeGenerator(Random(42))

    @Test
    fun everyWordYieldsAChallengeContainingTheWord() {
        val gen = generator()
        // The words from the original brief, plus awkward input a real audience
        // will absolutely shout.
        val words = listOf("KMP", "college", "startup", "bug", "x", "existential dread", "42")

        words.forEachIndexed { i, word ->
            val challenge = gen.generate(i.toLong(), word)
            assertTrue(challenge.text.contains(word), "'$word' missing from: ${challenge.text}")
            assertEquals(ChallengeSource.TEMPLATE, challenge.source)
            assertTrue(challenge.durationSeconds > 0)
            assertFalse(challenge.text.contains("{word}"), "placeholder left unsubstituted")
        }
    }

    @Test
    fun trimsSurroundingWhitespace() {
        val challenge = generator().generate(1, "  bug  ")
        assertEquals("bug", challenge.word)
    }

    @Test
    fun doesNotRepeatAFrameUntilTheEligiblePoolIsExhausted() {
        val gen = generator()
        val seen = mutableListOf<String>()
        repeat(15) { i -> seen += gen.generate(i.toLong(), "bug").text }

        assertEquals(seen.size, seen.toSet().size, "a frame repeated before the pool ran out")
    }

    @Test
    fun keepsProducingChallengesPastPoolExhaustion() {
        val gen = generator()
        repeat(200) { i ->
            assertTrue(gen.generate(i.toLong(), "startup").text.isNotBlank())
        }
    }
}
