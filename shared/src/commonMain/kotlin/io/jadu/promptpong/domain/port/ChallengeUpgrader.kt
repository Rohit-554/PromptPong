package io.jadu.promptpong.domain.port

/** Produces a better challenge line for a word, or null when it has nothing worth showing. */
interface ChallengeUpgrader {
    suspend fun upgrade(word: String): String?
}
