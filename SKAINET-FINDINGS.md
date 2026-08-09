# Running SKaiNET on Android and iOS: what we found

This is a field report from trying to ship a real Compose Multiplatform app using
SKaiNET for on-device text generation on phones. It is written for the SKaiNET
maintainers. Everything below is measured, not estimated.

We know the project says it is early and experimental, so none of this is a
complaint. We hit these things while genuinely trying to ship, so we wrote them
down in case it is useful.

Short version: the library is correct on every platform we tried. The blocker was
speed and memory on mobile, and both trace back to the same root cause, which is
that Android and iOS have no native kernel backend and no way to keep tensors off
the managed heap.

For context on how this ended: we have since moved the app to ONNX Runtime GenAI
on Android and Apple Foundation Models on iOS. We would rather have stayed on one
Kotlin-native stack, which is why we are sending this.

## The app

A party game. The audience shouts a word such as "KMP", "college", "startup" or
"bug". The host types it into a shared phone, and the app writes a short funny
dare about that word, on device, with no server.

Constraints that follow from that:

- One generation is about 44 tokens, which is a single sentence.
- Someone is standing in front of an audience waiting, so past roughly 10 to 15
  seconds it is unusable.
- It has to run on a normal consumer phone.

We used SmolLM2-135M-Instruct, GGUF Q8_0, 138 MiB on disk, 135 million
parameters. We deliberately picked one of the smallest usable instruct models
because we expected mobile to be tight.

## Versions

| Thing | Version |
| --- | --- |
| SKaiNET engine | 0.38.0 |
| SKaiNET-transformers | 0.38.0 |
| Kotlin | 2.4.10 |
| Compose Multiplatform | 1.11.1 |
| Android Gradle Plugin | 9.0.1 |
| Gradle | 9.1.0 |
| minSdk / compileSdk | 24 / 36 |

Dependencies, all through `sk.ainet.transformers:skainet-transformers-bom:0.38.0`:

```
skainet-transformers-core
skainet-transformers-inference-llama
sk.ainet.core:skainet-lang-core
sk.ainet.core:skainet-backend-cpu
sk.ainet.core:skainet-io-core
sk.ainet.core:skainet-io-gguf
```

Loading code, identical on all platforms, in commonMain:

```kotlin
val tokenizer = TokenizerFactory.fromTokenizerJson(tokenizerJson)
val ctx = DirectCpuExecutionContext.create()
val model = LlamaNetworkLoader
    .fromGguf({ randomAccessSource() }, QuantPolicy.NATIVE_OPTIMIZED)
    .load<FP32, Float>(ctx)
val runtime = OptimizedLLMRuntime(
    model = model,
    ctx = ctx,
    mode = OptimizedLLMMode.DIRECT,
    dtype = FP32::class,
    bos = tokenizer.bosTokenId ?: 1,
)
```

## What worked

Leading with this, because a lot worked first time.

- The same commonMain inference code compiled and ran on JVM, Android and
  Kotlin/Native with no per-platform branching in the model code.
- Output was coherent and on topic on every platform. We are not reporting a
  correctness bug anywhere.
- `QuantPolicy.NATIVE_OPTIMIZED` works on Kotlin/Native. We verified this on an
  iOS simulator build and our FP32 fallback never triggered, so the packed Q8_0
  path really does run on Apple targets.
- `TokenizerFactory.fromTokenizerJson` is genuinely multiplatform and gave correct
  byte level BPE everywhere.
- `Tokenizer.decodeToken` preserving leading spaces during streaming decode saved
  us from a bug we would otherwise have shipped.

## Measurements

Same model file, same quantisation, `NATIVE_OPTIMIZED` everywhere.

| Platform | Model load | Decode speed | Time for one 44 token dare |
| --- | --- | --- | --- |
| JVM desktop, Apple Silicon, JDK 21 | 0.9 s | 21 tokens/sec | about 2 s |
| Android, physical phone | loads fine | 1.0 tokens/sec | about 44 s |
| iOS simulator, Apple Silicon | 25 s | 0.11 tokens/sec | about 6 min |

The JVM run had `--add-modules jdk.incubator.vector` and
`-Dskainet.cpu.vector.enabled=true`.

For contrast, the same JVM machine with `DEQUANTIZE_TO_FP32` drops to 1.0
tokens/sec and about 540 MB resident, against about 145 MB packed. So the packed
path is worth roughly 21x on the JVM.

The number that matters is the gap between 21 tokens/sec on the JVM and 0.11 on
Kotlin/Native. That is roughly 200x for the same model, same quantisation, same
code, comparable CPU. Android at 1.0 sits in between, which is what you would
expect from ART running scalar Kotlin with a JIT but no SIMD.

## Problem 1: no native kernels on Android or iOS

This is the headline issue. Everything else is secondary.

**What we expected.** A phone slower than a laptop by maybe 3x to 5x, so a 135M
model landing somewhere around 5 tokens/sec.

**What we got.** 1.0 tokens/sec on Android, 0.11 on iOS.

**Why.** `skainet-backend-native-cpu`, the priority 100 FFM provider that gives
the big speedups, publishes only these on Maven Central:

```
skainet-backend-native-cpu-jvm
skainet-backend-native-cpu-linuxx64
skainet-backend-native-cpu-linuxarm64
```

No `-android`, no `-iosarm64`. On top of that Android has no `java.lang.foreign`
and no JDK Vector API, so the Panama path in `skainet-backend-cpu` cannot help
either. Both mobile platforms therefore fall all the way back to a scalar Kotlin
matmul while the JVM gets hand written SIMD. That one difference accounts for
almost the entire gap.

**Impact.** This is what stopped us shipping on SKaiNET. Our app measured decode
speed at startup and switched the AI off below 3 tokens/sec, because below that a
one sentence dare cannot finish inside the round budget. Both platforms failed
that check.

**What would fix it.**

1. Publish `skainet-backend-native-cpu` for `androidNativeArm64` and `iosArm64`
   with NEON kernels for at least Q8_0, Q4_0 and Q4_K. The kernels already exist
   for `linuxArm64`, which is also ARM NEON, so this may be mostly a build and
   packaging problem rather than new kernel work.
2. For Android specifically, a JNI or NDK bridge shipped as an AAR with the `.so`
   files would work. Android cannot use FFM but it has always had JNI.
3. Failing both, even tightening the scalar Kotlin matmul for packed Q8_0 would
   help. At 1 token/sec we needed roughly 5x, not 100x.

## Problem 2: `createRandomAccessSource` returns null on Android, causing an out of memory crash

This cost us the most debugging time and produces a confusing failure.

**What we saw.** On a physical phone, right after the model download finished, the
app froze, Android showed "PromptPong isn't responding", and logcat had:

```
java.lang.OutOfMemoryError: Failed to allocate a 16 byte allocation with
... free bytes and ...MB until OOM
```

A 16 byte allocation failing looks nonsensical until you realise the heap was
already completely full and the next tiny object was simply the one that failed.

**Why.** In `skainet-io-gguf`:

```kotlin
// androidMain/kotlin/sk/ainet/io/gguf/RandomAccessSourceFactory.android.kt
public actual fun createRandomAccessSource(filePath: String): RandomAccessSource? = null
```

with the comment "Returns null on Android as file access patterns differ. Callers
should fall back to legacy GGUFReader which loads the full file."

That null decides which branch of `LlamaNetworkLoader.load` runs:

- with a random access source you get `loadToMapStreaming`, which reads tensors as
  it goes
- without one you get `loadToMap`, which materialises the entire file first

So on Android we held about 138 MiB of raw file bytes plus about the same again in
built tensors plus parsing garbage, simultaneously. That is north of 300 MB on a
heap capped at 256 MB by default, or 512 MB with `android:largeHeap="true"`,
which we already had.

**Why this is worse on Android than it looks.** SKaiNET on Android is pure Kotlin,
so every tensor is a `ByteArray` or `FloatArray` on the ART managed heap, and that
heap is hard capped per app. This is very different from ONNX Runtime, llama.cpp,
TFLite or MediaPipe, which allocate natively and can load a 1 GB model on the same
phone. A developer arriving from any of those will reasonably assume 138 MiB is
nothing. We did.

**Our workaround.** We implemented `RandomAccessSource` for Android ourselves.
Nothing about Android prevents random file access: `RandomAccessFile` and
positional `FileChannel.read` have been there since API 1, and positional reads do
not touch the shared file pointer, so they satisfy the thread safety requirement
in the interface docs.

```kotlin
private class AndroidRandomAccessSource(
    private val file: RandomAccessFile,
) : RandomAccessSource {

    private val channel = file.channel
    override val size: Long = file.length()

    override fun readAt(position: Long, length: Int): ByteArray {
        if (length == 0) return ByteArray(0)
        val buffer = ByteArray(length)
        val read = readAt(position, buffer, 0, length)
        return if (read < length) buffer.copyOf(read) else buffer
    }

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        val target = ByteBuffer.wrap(buffer, offset, length)
        var at = position
        var total = 0
        while (target.hasRemaining()) {
            val read = channel.read(target, at)
            if (read <= 0) break
            at += read
            total += read
        }
        return total
    }

    override fun close() = file.close()
}
```

With that in place the out of memory error disappeared and the model loaded
reliably on the same phone that had been crashing.

**What would fix it.** Ship exactly this as the Android actual. It is about 40
lines, needs no new dependencies, and turns a hard crash into a working load. We
are happy to open a pull request.

Two related suggestions:

- Consider `FileChannel.map` on Android. Mapped pages are file backed and are not
  counted against the ART heap.
- Longer term, allocating tensor storage in direct `ByteBuffer`s rather than
  Kotlin arrays would move weights off the managed heap entirely on Android. That
  is probably the single biggest structural change for making SKaiNET viable for
  larger models there.

## Problem 3: the FP32 fallback makes an out of memory error worse

We wrote what looked like a sensible safety net: try the packed path, and if the
platform rejects it fall back to `DEQUANTIZE_TO_FP32`.

When the packed load fails for memory reasons, that fallback dequantises to FP32,
needs roughly four times more memory, immediately fails again, and the second
failure hides the first.

This is our bug rather than yours. We mention it only because the trap is easy to
fall into, and a docs note saying "the FP32 fallback costs about 4x the memory, do
not use it as an OOM recovery path" would have saved us the time.

## Problem 4: the LLM code is hard to find

We read the SKaiNET README, saw GGUF support, `StreamingGGUFReader` and mentions
of KLlama, and assumed LLM inference lived in the SKaiNET repo.

The current `develop` branch has only `skainet-models:skainet-model-yolo`. There
is no llama module. The LLM work lives in the separate SKaiNET-transformers
repository, and older `skainet-apps-kllama-*` artifacts still sit on Maven Central
from when it did live in the engine repo, which adds to the confusion.

A short line near the top of the SKaiNET README pointing to SKaiNET-transformers
would remove this entirely. Possibly also deprecate the stale
`skainet-apps-kllama-*` coordinates.

## Problem 5: `runtime-kllama` has no iOS artifacts

The obvious dependency for llama inference is
`skainet-transformers-runtime-kllama`, and the README describes the project as
Kotlin Multiplatform including iOS.

On Maven Central that artifact publishes `-android`, `-jvm`, `-js`,
`-linuxarm64`, `-linuxx64`, `-macosarm64` and `-wasm-js`, but no `-iosarm64` and
no `-iossimulatorarm64`. The same is true of `runtime-kgemma`. Meanwhile
`inference-llama`, `transformers-core` and `transformers-agent` do publish iOS
variants.

In `llm-runtime/kllama/build.gradle.kts` the iOS targets are simply not declared,
although an `iosMain` source folder exists, which made it look supported.

We skipped the runtime facade and drove `LlamaNetworkLoader` plus
`OptimizedLLMRuntime` from commonMain, which is what KllamaDemo does anyway. That
works fine, but it took a while to work out that this was the intended iOS path.

Either add the Apple targets, or state plainly which artifacts are iOS capable. A
module against target support matrix would be very welcome. Right now the only
reliable way to find out is browsing the directory listing on Maven Central.

## Problem 6: documentation and comments that are out of date

**KllamaDemo says the packed path is JVM only.** In `QwenRuntimeBuilder.kt` the
comment reads "On wasmJs / iOS / Android we fall back to buildQwenRuntimeFallback
... the SIMD/MemSeg kernels are JVM-only", and the Android actual says the JVM
fast path is unavailable. We believed it, built our first version on
`DEQUANTIZE_TO_FP32`, and got 1 token/sec plus an out of memory error on the
desktop.

Then we tried `NATIVE_OPTIMIZED` anyway and got 21 tokens/sec and a 4x smaller
footprint, including on Kotlin/Native. Reading the changelog afterwards, 0.30.0
and 0.32.0 moved the Llama packed path into commonMain specifically to make it
work on Kotlin/Native. The demo comment predates that.

This matters because KllamaDemo is the first thing a new user copies, and it
currently steers people onto the slowest and most memory hungry path on exactly
the devices that can least afford it. Defaulting the demo to `NATIVE_OPTIMIZED`
would fix it.

**Q4_1 is accepted then fails at matmul.** Tracked as SKaiNET#654. A fast, clear
failure at load time naming the supported types would be much friendlier than a
crash deep in the forward pass.

**Tokenizer selection on Android.** `TokenizerFactory.fromGguf(fields)` needs a
field map, and the documented way to get one is `StreamingGGUFReader.fields`,
which needs a `RandomAccessSource`, which is null on Android. So on Android there
is effectively no way to build a tokenizer from the GGUF's own metadata. We
downloaded the HuggingFace `tokenizer.json` separately instead, at a cost of an
extra 2 MB. Fixing Problem 2 fixes this too.

## Summary of asks, in priority order

1. **Native CPU kernels for Android and Apple targets.** Publish
   `skainet-backend-native-cpu` for `androidNativeArm64` and `iosArm64`, or a JNI
   bridge for Android. This is the one thing that would have let us ship on
   SKaiNET.
2. **Implement `createRandomAccessSource` on Android.** About 40 lines, removes a
   hard crash, and we have working code to contribute.
3. **Get tensors off the ART managed heap on Android**, via direct `ByteBuffer`s
   or `mmap`. Without this the practical model ceiling on Android stays at a
   couple of hundred MB however good the kernels get.
4. **Publish a module against target support matrix**, and either add Apple
   targets to `runtime-kllama` or document the supported iOS path.
5. **Update KllamaDemo** to `NATIVE_OPTIMIZED` and correct the JVM only comment.
6. **Point to SKaiNET-transformers from the SKaiNET README** and clean up the
   stale `skainet-apps-kllama-*` artifacts.
7. **Fail fast on unsupported quantisation types** such as Q4_1.

## How to reproduce

Our test lived in commonTest so it ran on every target, which is what let us
compare JVM against Kotlin/Native directly.

```
export PROMPTPONG_MODEL=/abs/path/SmolLM2-135M-Instruct-Q8_0.gguf
export PROMPTPONG_TOKENIZER=/abs/path/tokenizer.json

./gradlew :shared:jvmTest --tests "*InferenceSpike*" -i
./gradlew :shared:iosSimulatorArm64Test -i
```

Model files:

- GGUF: https://huggingface.co/bartowski/SmolLM2-135M-Instruct-GGUF, Q8_0
- tokenizer.json: https://huggingface.co/HuggingFaceTB/SmolLM2-135M-Instruct

One note if you try the simulator run. Environment variables do not reach a
simulator test process unless prefixed with `SIMCTL_CHILD_`, which `simctl`
strips when spawning. We needed this in the Gradle build:

```kotlin
tasks.withType<KotlinNativeSimulatorTest>().configureEach {
    listOf("PROMPTPONG_MODEL", "PROMPTPONG_TOKENIZER").forEach { key ->
        providers.environmentVariable(key).orNull?.let {
            environment("SIMCTL_CHILD_$key", it)
        }
    }
}
```

## Closing

We would still prefer a single Kotlin-native stack over maintaining two different
AI backends, which is what we have now. As far as we can tell the only thing
standing between us and that is native kernels for ARM mobile targets, plus the
Android random access fix. The library already does the hard part correctly on
those platforms, which is why this seemed worth writing up rather than quietly
giving up.

Happy to test any branch or snapshot on real hardware and report numbers back.
