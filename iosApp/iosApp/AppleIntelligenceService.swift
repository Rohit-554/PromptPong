import Foundation
import Shared

#if canImport(FoundationModels)
import FoundationModels
#endif

/// Swift side of the Apple Intelligence bridge.
final class AppleIntelligenceService: NSObject, AppleIntelligenceBridge {

    private var generationTask: Task<Void, Never>?

    // The iPhone checks its gear first. Is Apple Intelligence available on this device?
    /* func checkAvailability(callback: AppleAvailabilityCallback) {
        guard #available(iOS 26.0, *) else {
            callback.onResult(
                available: false,
                statusText: "Apple Intelligence needs iOS 26 or later"
            )
            return
        }

        #if targetEnvironment(simulator)
        callback.onResult(
            available: false,
            statusText: "Apple Intelligence needs a real device, not the Simulator"
        )
        return
        #endif

        #if canImport(FoundationModels)
        switch SystemLanguageModel.default.availability {
        case .available:
            callback.onResult(
                available: true,
                statusText: "Running on device with Apple Intelligence"
            )
        case .unavailable(.deviceNotEligible):
            callback.onResult(
                available: false,
                statusText: "This iPhone does not support Apple Intelligence"
            )
        case .unavailable(.appleIntelligenceNotEnabled):
            callback.onResult(
                available: false,
                statusText: "Turn on Apple Intelligence in Settings to use local AI"
            )
        case .unavailable(.modelNotReady):
            callback.onResult(
                available: false,
                statusText: "Apple Intelligence is still preparing its model"
            )
        @unknown default:
            callback.onResult(
                available: false,
                statusText: "Apple Intelligence is unavailable right now"
            )
        }
        #else
        callback.onResult(
            available: false,
            statusText: "This build was made with an SDK that has no FoundationModels"
        )
        #endif
    } */

    // Here is Apple's local AI at work. Stream the answer back piece by piece.
    /* func generate(
        prompt: String,
        maxTokens: Int32,
        temperature: Double,
        topP: Double,
        callback: AppleGenerationCallback
    ) {
        cancel()

        guard #available(iOS 26.0, *) else {
            callback.onError(message: "Apple Intelligence needs iOS 26 or later")
            return
        }

        #if canImport(FoundationModels)
        generationTask = Task {
            do {
                let model = SystemLanguageModel.default
                guard model.isAvailable else {
                    callback.onError(message: "Apple Intelligence is unavailable")
                    return
                }

                let session = LanguageModelSession(model: model)
                let options = GenerationOptions(
                    sampling: temperature > 0
                        ? .random(probabilityThreshold: topP, seed: nil)
                        : .greedy,
                    temperature: temperature,
                    maximumResponseTokens: Int(maxTokens)
                )

                var emitted = ""
                for try await snapshot in session.streamResponse(to: prompt, options: options) {
                    try Task.checkCancellation()
                    let current = snapshot.content
                    let delta = current.hasPrefix(emitted)
                        ? String(current.dropFirst(emitted.count))
                        : current
                    emitted = current
                    if !delta.isEmpty {
                        callback.onText(text: delta)
                    }
                }
                callback.onComplete()
            } catch is CancellationError {
            } catch {
                callback.onError(message: error.localizedDescription)
            }
        }
        #else
        callback.onError(message: "This build has no FoundationModels support")
        #endif
    } */

    // Every good game needs a stop button. Cancel the current AI response here.
    /* func cancel() {
        generationTask?.cancel()
        generationTask = nil
    } */
}
