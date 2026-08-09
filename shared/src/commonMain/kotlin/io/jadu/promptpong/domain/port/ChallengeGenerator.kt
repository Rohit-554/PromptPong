package io.jadu.promptpong.domain.port

import io.jadu.promptpong.domain.model.Challenge
import kotlinx.coroutines.flow.Flow

/** Produces challenges for a shouted word. */
interface ChallengeGenerator {
    fun generate(word: String): Flow<Challenge>
}
