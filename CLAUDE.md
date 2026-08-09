# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

PromptPong is a Kotlin Multiplatform / Compose Multiplatform party game for **Android and iOS**. An audience shouts a word ("KMP", "college", "startup", "bug"); the host types it in and the app produces a funny mini-challenge. Challenges come from a **hybrid generator**: a template engine answers instantly, and an **on-device LLM** replaces that text in place when it finishes.

The two platforms use completely different AI backends, on purpose:

| | Engine | Model | Download |
|---|---|---|---|
| Android | ONNX Runtime GenAI | Gemma 3 270M (ONNX bundle) | ~300 MB zip, once |
| iOS | Apple Foundation Models | Apple's system model | none, ships with iOS |

## Build & run

```bash
./gradlew :androidApp:installDebug          # Android
./gradlew :shared:testAndroidHostTest       # unit tests (the fast loop)
./gradlew :shared:allTests                  # every KMP test target
./gradlew :shared:testAndroidHostTest --tests "*Hybrid*"   # a single class
```

iOS: open `iosApp/` in Xcode and run. `iosApp/Configuration/Config.xcconfig` holds `TEAM_ID` (blank in git; set it locally for device builds).

## The hybrid design is load-bearing

`HybridChallengeGenerator` emits a template challenge immediately, then the model's version under the **same `Challenge.id`** so the UI swaps it in place. Any failure, timeout (15s) or rejected output leaves the template standing.

This is not defensive padding. It is the only reason the app is shippable: the previous SKaiNET-based implementation managed 1.0 tok/s on a real Android phone, and Apple Intelligence is unavailable on most iPhones. **Never make the game await the model.**

## Prompting a 270M model: two rules learned the hard way

**Always apply the model's chat template.** `AndroidOnnxEngine` reads `chat_template` from `tokenizer_config.json` and wraps the prompt in a single user turn. Gemma 3 **IT** is trained to answer inside `<start_of_turn>user … <start_of_turn>model`; given a bare string it falls into plain text continuation and just carries on the pattern of its input. Skipping this produced output that copied the prompt's examples with one word swapped ("Bag" → "your worst **bug** was actually a premium feature"). One user message, no system role: Gemma's template has no system slot.

**No few-shot examples in the prompt.** At this size the model imitates examples rather than generalising from them, so examples are the copying mechanism. `buildPrompt` gives a direct instruction and repeats the word. If you are tempted to add examples back, `AiValidateTest.promptCarriesTheWordAndNoExamplesToCopy` will fail on purpose.

Everything the model produces passes `AiChallengeUpgrader.validate` before display. It requires a **complete sentence** (a fragment is rejected outright, never padded with an ellipsis) that **actually mentions the shouted word**, and rejects echoed instructions. Rejection is cheap because the template is already on screen; a garbled or off-topic dare in front of an audience is not. `AiValidateTest` pins real bad output captured from a phone as regression cases.

## Android: ONNX Runtime GenAI

- Dependency comes from a **custom Ivy repository** in `settings.gradle.kts`. `ai.onnxruntime.genai:onnxruntime-genai-android` is **not on Maven Central** — it is published only as a GitHub release asset. That repo needs `metadataSources { artifact() }` (no POM exists) and the dependency needs an explicit **`@aar`** suffix, which is why it is spelled out as a string instead of a catalog alias.
- GenAI owns tokenization, KV cache, sampling and the decode loop. Do not hand-write a decode loop.
- It needs the **whole model folder**, not one file: `genai_config.json` describes the graph and search defaults. Hence a zip bundle. `ModelSpec` holds the URL and the required-file list; swapping models is a constants change.
- **ONNX allocates in the native heap**, not ART's managed heap. That cap (256 MB, 512 MB with `largeHeap`) is what makes pure-Kotlin inference libraries OOM on models this size, and why this approach works where the previous one didn't. `largeHeap` is therefore not needed.
- `initPromptPong(context)` must be called from `MainActivity` before anything touches model files.

## iOS: Apple Foundation Models

FoundationModels is a **Swift-only** framework, so Kotlin/Native cannot import it the way it imports UIKit. The seam is a bridge:

1. `AppleIntelligenceBridge.kt` (iosMain) declares the interface and a `registerAppleIntelligenceBridge` function.
2. `iosApp/iosApp/AppleIntelligenceService.swift` implements it against `SystemLanguageModel` / `LanguageModelSession`.
3. `iOSApp.swift` registers an instance at launch, **holding it in a property** — Kotlin keeps only a reference, so a local would be deallocated.

Callbacks, not suspend functions, because Swift implements this side.

Availability is reported as user-facing text, and it fails for four distinct reasons worth preserving: iOS below 26, ineligible device, Apple Intelligence switched off, and the Simulator (where it never works). `streamResponse` yields the **whole text so far** each time; the Swift side diffs it to deltas because Kotlin expects chunks.

The Xcode project uses Xcode 16 **synchronized groups**, so new Swift files are picked up automatically with no `project.pbxproj` edit.

## Structure

- `shared/commonMain` — everything platform-independent:
  - `domain/port/` — `LocalAiEngine` (the one AI abstraction), `ChallengeGenerator`, `ChallengeUpgrader`, `ModelDelivery`.
  - `data/template/` — the instant path; works with no model.
  - `data/ai/` — prompt building, `sanitize`, and `ModelSpec`.
  - `data/HybridChallengeGenerator.kt` — where the two paths meet.
  - `ui/` + `App.kt` — ViewModel (StateFlow) and Compose UI.
- `shared/androidMain` — ONNX engine, model storage, zip download/extract (zip-slip guarded).
- `shared/iosMain` — Apple Intelligence engine and the bridge declaration.
- `androidApp/`, `iosApp/` — thin hosts.

No runtime type from either backend may leak above `LocalAiEngine`. That is what lets one ViewModel drive both.

## Architecture guidance

`.claude/skills/architecture/` defines the intended layering (Clean Architecture, StateFlow for state, repository as SSOT). Those skills assume Hilt, which cannot run in `commonMain` — apply the principles, but construction here is plain (`di/Platform.kt` expect/actual), which a graph this small does not outgrow.
