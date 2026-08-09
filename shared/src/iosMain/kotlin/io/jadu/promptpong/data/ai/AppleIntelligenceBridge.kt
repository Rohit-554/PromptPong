package io.jadu.promptpong.data.ai

/** The seam between Kotlin and Apple Intelligence. */
interface AppleAvailabilityCallback {
    fun onResult(available: Boolean, statusText: String)
}

interface AppleGenerationCallback {
    /** Called with each new chunk of text, not the full text so far. */
    fun onText(text: String)
    fun onComplete()
    fun onError(message: String)
}

interface AppleIntelligenceBridge {
    fun checkAvailability(callback: AppleAvailabilityCallback)

    fun generate(
        prompt: String,
        maxTokens: Int,
        temperature: Double,
        topP: Double,
        callback: AppleGenerationCallback,
    )

    fun cancel()
}

private var registeredBridge: AppleIntelligenceBridge? = null

/** Called by the iOS host at launch. */
fun registerAppleIntelligenceBridge(bridge: AppleIntelligenceBridge) {
    registeredBridge = bridge
}

internal fun appleIntelligenceBridge(): AppleIntelligenceBridge? = registeredBridge
