package io.jadu.promptpong.data.template

import io.jadu.promptpong.domain.model.Challenge
import io.jadu.promptpong.domain.model.ChallengeSource
import kotlin.random.Random

/** Rough shape of a shouted word, used to pick frames that land. */
internal enum class WordKind { TECH, PLACE, ROLE, ABSTRACT, GENERIC }

/** A challenge frame. */
internal data class Frame(
    val text: String,
    val seconds: Int,
    val kinds: Set<WordKind> = setOf(WordKind.GENERIC),
)

/** The instant path: a deterministic frame pool crossed with the shouted word. */
class TemplateChallengeGenerator(private val random: Random = Random.Default) {

    /** Frames already used, cleared once the eligible pool is exhausted. */
    private val used = mutableSetOf<String>()

    fun generate(id: Long, word: String): Challenge {
        val clean = word.trim()
        val kind = classify(clean)

        val eligible = FRAMES.filter { kind in it.kinds || WordKind.GENERIC in it.kinds }
        val fresh = eligible.filterNot { it.text in used }.ifEmpty {
            used.clear()
            eligible
        }

        val frame = fresh.random(random)
        used += frame.text

        return Challenge(
            id = id,
            word = clean,
            text = frame.text.replace("{word}", clean),
            source = ChallengeSource.TEMPLATE,
            durationSeconds = frame.seconds,
        )
    }

    internal fun classify(word: String): WordKind {
        val w = word.lowercase().trim()
        return when {
            w in TECH_WORDS || TECH_HINTS.any { w.contains(it) } -> WordKind.TECH
            w in PLACE_WORDS -> WordKind.PLACE
            w in ROLE_WORDS || w.endsWith("er") || w.endsWith("ist") -> WordKind.ROLE
            ABSTRACT_SUFFIXES.any { w.endsWith(it) } -> WordKind.ABSTRACT
            else -> WordKind.GENERIC
        }
    }

    private companion object {
        val TECH_WORDS = setOf(
            "kmp", "bug", "api", "ai", "app", "code", "cloud", "database", "server",
            "kotlin", "java", "python", "git", "linux", "compose", "android", "ios",
            "crypto", "blockchain", "wifi", "laptop", "phone", "algorithm",
        )

        val TECH_HINTS = listOf("script", "stack", "dev", "byte", "data", "net")

        val PLACE_WORDS = setOf(
            "college", "school", "office", "home", "hostel", "canteen", "gym",
            "airport", "library", "cafe", "mall", "kitchen", "beach",
        )

        val ROLE_WORDS = setOf(
            "startup", "founder", "intern", "boss", "student", "teacher", "client",
            "investor", "recruiter", "influencer", "landlord",
        )

        val ABSTRACT_SUFFIXES = listOf("ness", "tion", "sion", "ity", "ism", "ship", "ance")

        val FRAMES = listOf(
            Frame("Act out \"{word}\" with no words at all. The room has to guess.", 30),
            Frame("Explain \"{word}\" to a five-year-old. Badly.", 30),
            Frame("Sell \"{word}\" to the room like it's a life-changing product.", 40),
            Frame("Give a 20-second TED talk on why \"{word}\" is secretly overrated.", 20),
            Frame("Sing one line about \"{word}\" to any tune you like.", 20),
            Frame("Describe \"{word}\" using only sound effects.", 20),
            Frame("Make the room laugh using only the word \"{word}\" and your face.", 20),
            Frame("Invent a proverb about \"{word}\" and say it like it's ancient wisdom.", 25),
            Frame("Argue that \"{word}\" is the cause of all your problems. Be convincing.", 30),
            Frame("Do your best impression of \"{word}\" as a person.", 25),
            Frame("Give \"{word}\" a dramatic movie-trailer voiceover.", 25),
            Frame("Name three things worse than \"{word}\". Defend the ranking.", 30),
            Frame("Confess a fake secret involving \"{word}\".", 25),
            Frame("Pitch \"{word}\" as a reality TV show. Give it a title.", 35),
            Frame("Describe \"{word}\" as if reviewing it one star out of five.", 30),

            Frame(
                "Explain \"{word}\" to your grandparent in 30 seconds. No jargon allowed.",
                30, setOf(WordKind.TECH),
            ),
            Frame(
                "Convince the room \"{word}\" is not a problem, it's a premium feature.",
                30, setOf(WordKind.TECH),
            ),
            Frame(
                "You broke production with \"{word}\". Apologise to the room.",
                30, setOf(WordKind.TECH),
            ),
            Frame(
                "Pitch \"{word}\" to an investor who has never heard of computers.",
                40, setOf(WordKind.TECH),
            ),

            Frame(
                "Give the room a guided tour of \"{word}\" as an over-excited travel host.",
                35, setOf(WordKind.PLACE),
            ),
            Frame(
                "Describe the worst possible day at \"{word}\".",
                30, setOf(WordKind.PLACE),
            ),

            Frame(
                "Do 20 seconds as a \"{word}\" who has completely lost control.",
                20, setOf(WordKind.ROLE),
            ),
            Frame(
                "Give the acceptance speech of the year's most disappointing \"{word}\".",
                35, setOf(WordKind.ROLE),
            ),

            Frame(
                "Define \"{word}\" as if you're the last expert alive on the subject.",
                30, setOf(WordKind.ABSTRACT),
            ),
            Frame(
                "Rank \"{word}\" against pizza. Justify it with real passion.",
                25, setOf(WordKind.ABSTRACT),
            ),
        )
    }
}
