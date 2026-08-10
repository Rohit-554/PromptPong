package io.jadu.promptpong.data

import io.jadu.promptpong.data.template.TemplateChallengeGenerator
import io.jadu.promptpong.domain.model.Challenge
import io.jadu.promptpong.domain.model.ChallengeSource
import io.jadu.promptpong.domain.port.ChallengeGenerator
import io.jadu.promptpong.domain.port.ChallengeUpgrader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull

private const val MODEL_CHALLENGE_SECONDS = 30

/**
 * Asks the model first and waits for it. Templates are used only when there is no
 * model, or it fails, times out or returns nothing usable.
 */
class HybridChallengeGenerator(
    private val templates: TemplateChallengeGenerator,
    private val upgraderProvider: () -> ChallengeUpgrader?,
    private val timeoutMillis: Long = 30_000,
) : ChallengeGenerator {

    private val inferenceLock = Mutex()
    private var nextId = 0L

    // The game knows a safe fallback. Now let it ask the local AI for something fresh.
    /* override fun generate(word: String): Flow<Challenge> = flow {
        val id = nextId++
        val text = generateWithModel(word)

        emit(
            if (text != null) {
                Challenge(id, word.trim(), text, ChallengeSource.MODEL, MODEL_CHALLENGE_SECONDS)
            } else {
                templates.generate(id, word)
            },
        )
    } */

    // Careful, only one round gets the mic at a time. Restore this to protect the model.
    /* private suspend fun generateWithModel(word: String): String? {
        val upgrader = upgraderProvider() ?: return null
        if (!inferenceLock.tryLock()) return null

        return try {
            withTimeoutOrNull(timeoutMillis) { upgrader.upgrade(word) }
        } catch (_: Throwable) {
            null
        } finally {
            inferenceLock.unlock()
        }
    } */
}
