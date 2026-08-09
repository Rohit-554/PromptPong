package io.jadu.promptpong.data.ai

import io.jadu.promptpong.domain.port.AiGenerationConfig
import io.jadu.promptpong.domain.port.AiToken
import io.jadu.promptpong.domain.port.ChallengeUpgrader
import io.jadu.promptpong.domain.port.LocalAiEngine
import kotlinx.coroutines.flow.fold

/** Turns a shouted word into a dare using the platform's local AI engine. */
class AiChallengeUpgrader(private val engine: LocalAiEngine) : ChallengeUpgrader {

    /** Accepts whatever the model writes, so the fallback is only used when it writes nothing. */
    override suspend fun upgrade(word: String): String? =
        clean(generateOnce(word)).takeIf { it.isNotBlank() }

    private suspend fun generateOnce(word: String): String =
        engine.generate(buildPrompt(word), CONFIG.copy(assistantPrefill = OPENERS.random()))
            .fold(StringBuilder()) { text, token ->
                when (token) {
                    is AiToken.Text -> text.append(token.text)
                    is AiToken.Failed -> return@fold text
                    AiToken.Completed -> text
                }
            }
            .toString()

    internal fun buildPrompt(word: String): String {
        val clean = word.trim()
        return buildString {
            appendLine("Someone in the room shouted the word \"$clean\".")
            appendLine()
            appendLine(
                "Tell one person something silly to do with \"$clean\" right now, " +
                    "in front of everyone.",
            )
            appendLine()
            appendLine("Write one short sentence.")
            appendLine("Speak straight to that person.")
            appendLine("Use the word \"$clean\" in the sentence.")
        }
    }

    internal companion object {
        private const val MIN_LENGTH = 20
        private const val MAX_LENGTH = 160

        val CONFIG = AiGenerationConfig(
            maxNewTokens = 64,
            temperature = 0.5f,
            stopSequences = listOf("<end_of_turn>", "<start_of_turn>", "\n\n"),
        )

        /**
         * The answer is forced to start with one of these, so the model can only
         * continue an instruction that is already underway. A 270M model cannot be
         * talked out of "Here's a party game idea:", but it can be started past it.
         */
        private val OPENERS = listOf(
            "Act out ", "Mime ", "Sing about ", "Explain ", "Sell ",
            "Perform ", "Describe ", "Pretend ", "Convince the room that ",
        )

        /** Formatting only: strips turn markers, labels, bullets and quotes. */
        fun clean(raw: String): String {
            var text = raw.trim()
                .removePrefix("Dare:").trim()
                .substringBefore("<end_of_turn>")
                .substringBefore("<start_of_turn>")
                .trim()

            text = text.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
            text = text.removePrefix("-").removePrefix("*").trim()
            return text.removeSurrounding("\"").removeSurrounding("'").trim()
        }

        private val INSTRUCTION_ECHOES = listOf(
            "rules:", "one sentence", "party-game dare", "dare about",
            "reply with", "the player", "no quotes", "action verb",
        )

        private val PREAMBLE_STARTERS = setOf(
            "okay", "ok", "sure", "here", "heres", "alright", "certainly", "well",
            "hey", "hi", "hello", "yes", "no", "i", "im", "let", "lets", "this",
            "that", "we", "you", "it", "my", "a", "an", "the", "and", "but", "so",
        )

        private val STOP_WORDS = setOf(
            "a", "an", "the", "to", "of", "in", "on", "at", "for", "with", "and",
            "or", "but", "is", "was", "are", "were", "be", "your", "you", "that",
            "it", "as", "so", "if", "then", "this", "there", "their",
        )

        private fun contentWords(text: String): Set<String> = text
            .lowercase()
            .map { if (it.isLetterOrDigit() || it == ' ') it else ' ' }
            .joinToString("")
            .split(" ")
            .filter { it.isNotBlank() && it !in STOP_WORDS }
            .toSet()

        /** Tolerates a trailing "s" either way, and accepts any word of a multi-word shout. */
        internal fun mentionsWord(text: String, word: String): Boolean {
            val haystack = text.lowercase()
            return contentWords(word)
                .flatMap { listOf(it, it.removeSuffix("s"), "${it}s") }
                .filter { it.length >= 2 }
                .any { haystack.contains(it) }
        }

        /**
         * Strict quality filter. Not currently called: it rejected so much of what a
         * 270M model writes that every dare fell back to the built-in list.
         */
        @Suppress("unused")
        fun validate(raw: String, word: String): String? {
            val text = clean(raw)

            if (text.length !in MIN_LENGTH..MAX_LENGTH) return null
            if (text.firstOrNull()?.isLetter() != true) return null
            if (text.last() !in ".!?") return null

            val lower = text.lowercase()
            if (INSTRUCTION_ECHOES.any { lower.contains(it) }) return null
            if (lower.contains("here's a") || lower.contains("here is a")) return null

            val firstWord = lower.takeWhile { it.isLetter() || it == '\'' }.replace("'", "")
            if (firstWord in PREAMBLE_STARTERS) return null

            if (!mentionsWord(text, word)) return null

            return text
        }
    }
}
