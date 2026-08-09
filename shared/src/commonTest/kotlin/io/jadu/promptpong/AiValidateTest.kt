package io.jadu.promptpong

import io.jadu.promptpong.data.ai.AiChallengeUpgrader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AiValidateTest {

    private fun validate(raw: String, word: String) = AiChallengeUpgrader.validate(raw, word)

    // Real output captured from Gemma 3 270M on a physical phone, back when the
    // chat template was not applied and the prompt carried few-shot examples.
    // Every one of these must be rejected.
    @Test
    fun rejectsTheOutputsSeenOnDevice() {
        assertNull(
            validate("Convince the room that your worst alarm was actually a premium feature.", "Rohit"),
            "answer never mentions the shouted word",
        )
        assertNull(
            validate("A test of your worst bug was actually a premium feature.", "Bag"),
            "answer is about 'bug', not the shouted 'Bag'",
        )
        assertNull(
            validate("Pen the room that you had a terrible morning in pen using only interpretive", "Pen"),
            "unfinished sentence",
        )
    }

    @Test
    fun acceptsAnswersThatDoTalkAboutTheWord() {
        assertEquals(
            "Perform a dramatic love confession to a book.",
            validate("Perform a dramatic love confession to a book.", "Books"),
        )
    }

    @Test
    fun toleratesSingularAndPluralBetweenWordAndAnswer() {
        assertNotNull(validate("Act out a fight with three books at once.", "Books"))
        assertNotNull(validate("Sell this pens to the room like it is priceless.", "Pen"))
    }

    @Test
    fun acceptsAMultiWordShoutMentionedInPart() {
        assertNotNull(
            validate("Mime your way through pure existential dread for the room.", "existential dread"),
        )
    }

    @Test
    fun rejectsAnUnfinishedSentenceRatherThanPaddingIt() {
        assertNull(validate("Convince the room that your bug is secretly a", "bug"))
    }

    @Test
    fun rejectsTheInstructionsBeingEchoedBack() {
        assertNull(validate("Write exactly one sentence about the bug.", "bug"))
        assertNull(validate("Rules: mention the bug and be silly about it.", "bug"))
    }

    @Test
    fun stripsChatMarkersBulletsAndQuotes() {
        assertEquals(
            "Act out a bug in production for the room.",
            validate("\"Act out a bug in production for the room.\"<end_of_turn>", "bug"),
        )
        assertEquals(
            "Act out a bug in production for the room.",
            validate("- Act out a bug in production for the room.", "bug"),
        )
        assertEquals(
            "Act out a bug in production for the room.",
            validate("Dare: Act out a bug in production for the room.", "bug"),
        )
    }

    @Test
    fun keepsOnlyTheFirstLine() {
        assertEquals(
            "Explain the bug using only mime.",
            validate("Explain the bug using only mime.\nThen argue with yourself about it.", "bug"),
        )
    }

    @Test
    fun rejectsEmptyShortOrRamblingOutput() {
        assertNull(validate("", "bug"))
        assertNull(validate("   ", "bug"))
        assertNull(validate("Do the bug.", "bug"))
        assertNull(validate("bug ".repeat(60), "bug"))
    }

    @Test
    fun rejectsOutputThatDoesNotStartWithALetter() {
        assertNull(validate("...and the bug wins again for everyone.", "bug"))
        assertNull(validate(", then act out the bug for the room.", "bug"))
    }

    @Test
    fun upgradeKeepsModelOutputEvenWhenItDriftsOffTheWord() = kotlinx.coroutines.test.runTest {
        val engine = FakeEngine("Convince the room that your worst alarm was a premium feature.")
        assertEquals(
            "Convince the room that your worst alarm was a premium feature.",
            AiChallengeUpgrader(engine).upgrade("Rohit"),
        )
    }

    @Test
    fun upgradeFallsBackOnlyWhenTheModelWritesNothing() = kotlinx.coroutines.test.runTest {
        assertNull(AiChallengeUpgrader(FakeEngine("")).upgrade("Rohit"))
        assertNull(AiChallengeUpgrader(FakeEngine("   ")).upgrade("Rohit"))
    }

    @Test
    fun upgradeReturnsTheSentenceWhenItIsOnTopic() = kotlinx.coroutines.test.runTest {
        val engine = FakeEngine("Act out a bag being far too heavy to lift.")
        assertEquals(
            "Act out a bag being far too heavy to lift.",
            AiChallengeUpgrader(engine).upgrade("Bag"),
        )
    }

    @Test
    fun promptCarriesTheWordAndNoExamplesToCopy() {
        val prompt = AiChallengeUpgrader(FakeEngine()).buildPrompt("Bag")

        assertTrue(prompt.contains("Bag"), "the word must be in the prompt")
        // The exact failure mode this design exists to prevent.
        assertTrue(
            !prompt.contains("premium feature") && !prompt.contains("interpretive dance"),
            "the prompt must not contain example dares for the model to copy",
        )
    }
}
