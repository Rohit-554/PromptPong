package io.jadu.promptpong.domain.model

/** Where a challenge's text came from. */
enum class ChallengeSource { TEMPLATE, MODEL }

/** One mini-challenge produced for an audience-shouted [word]. */
data class Challenge(
    val id: Long,
    val word: String,
    val text: String,
    val source: ChallengeSource,
    val durationSeconds: Int,
)
