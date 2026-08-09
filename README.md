# PromptPong

A party game for Android and iOS. The audience shouts a word, "KMP", "college",
"startup", "bug", the host types it in, and the app writes a funny mini-challenge
about it.

Challenges are generated **on the device**. Nothing is sent anywhere.

## How generation works

Two generators, layered:

1. **Templates** answer instantly by crossing a pool of challenge frames with the
   shouted word.
2. **An on-device model** writes a sharper challenge and replaces the template in
   place when it is ready.

The model takes a few seconds, so it is never in the critical path. The player
always has a challenge immediately, and a slow, failed or nonsensical model
response simply leaves the template on screen. The game is fully playable with no
model at all.

## The AI backends

Each platform uses what it is actually good at:

| Platform | Engine | Model | Setup |
|---|---|---|---|
| Android | ONNX Runtime GenAI | Gemma 3 270M | one-time ~300 MB download |
| iOS | Apple Foundation Models | Apple's system model | none, part of iOS |

On Android the model downloads once as a zip bundle, unpacks into app-private
storage, and everything after that is offline. ONNX Runtime allocates natively,
so model size is bounded by device RAM rather than by the Java heap cap.

On iOS there is nothing to download, because Apple Intelligence is part of the
system. It needs iOS 26 or later, a device that supports Apple Intelligence, the
feature switched on in Settings, and a real device rather than the Simulator. If
any of those is missing the app says which one and plays on templates.

## Running it

```shell
./gradlew :androidApp:installDebug   # Android
./gradlew :shared:testAndroidHostTest  # unit tests
```

For iOS, open [`iosApp`](./iosApp) in Xcode and run. Set `TEAM_ID` in
`iosApp/Configuration/Config.xcconfig` for device builds.

## Structure

- [`/shared`](./shared/src) — domain, generators, both AI engines, UI.
- [`/androidApp`](./androidApp) — thin Android host.
- [`/iosApp`](./iosApp) — thin SwiftUI host, plus the Swift side of the Apple
  Intelligence bridge.

`CLAUDE.md` documents the integration details that are easy to get wrong, notably
the custom Ivy repository the ONNX GenAI AAR comes from and the Kotlin-to-Swift
bridge that reaches Apple Intelligence.
