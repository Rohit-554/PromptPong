package io.jadu.promptpong.data.ai

import io.jadu.promptpong.domain.port.AiAvailability
import io.jadu.promptpong.domain.port.AiGenerationConfig
import io.jadu.promptpong.domain.port.AiToken
import io.jadu.promptpong.domain.port.LocalAiEngine
import kotlin.coroutines.resume
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

private const val BRIDGE_MISSING =
    "The iOS host did not register the Apple Intelligence bridge"

/** iOS local AI, backed by Apple Foundation Models. */
class AppleIntelligenceEngine : LocalAiEngine {

    private var available = false
    private var status = "Checking Apple Intelligence"

    override suspend fun availability(): AiAvailability {
        refresh()
        return AiAvailability(
            isAvailable = available,
            needsDownload = false,
            engineName = "Apple Intelligence",
            statusText = status,
        )
    }

    override suspend fun load(): Result<Unit> = runCatching {
        refresh()
        check(available) { status }
    }

    override fun generate(prompt: String, config: AiGenerationConfig): Flow<AiToken> = callbackFlow {
        val bridge = appleIntelligenceBridge()
        if (bridge == null) {
            trySend(AiToken.Failed(BRIDGE_MISSING))
            close()
            return@callbackFlow
        }

        bridge.generate(
            prompt = prompt,
            maxTokens = config.maxNewTokens,
            temperature = config.temperature.toDouble(),
            topP = config.topP.toDouble(),
            callback = object : AppleGenerationCallback {
                override fun onText(text: String) {
                    trySend(AiToken.Text(text))
                }

                override fun onComplete() {
                    trySend(AiToken.Completed)
                    close()
                }

                override fun onError(message: String) {
                    trySend(AiToken.Failed(message))
                    close()
                }
            },
        )

        awaitClose { bridge.cancel() }
    }

    override suspend fun cancel() {
        appleIntelligenceBridge()?.cancel()
    }

    private suspend fun refresh() {
        val bridge = appleIntelligenceBridge()
        if (bridge == null) {
            available = false
            status = BRIDGE_MISSING
            return
        }

        suspendCancellableCoroutine { continuation ->
            bridge.checkAvailability(
                object : AppleAvailabilityCallback {
                    override fun onResult(available: Boolean, statusText: String) {
                        this@AppleIntelligenceEngine.available = available
                        this@AppleIntelligenceEngine.status = statusText
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                },
            )
        }
    }
}
