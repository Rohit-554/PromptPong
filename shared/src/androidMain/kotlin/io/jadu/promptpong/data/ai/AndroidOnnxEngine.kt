package io.jadu.promptpong.data.ai

import ai.onnxruntime.genai.Generator
import ai.onnxruntime.genai.GeneratorParams
import ai.onnxruntime.genai.Model
import ai.onnxruntime.genai.Tokenizer
import io.jadu.promptpong.domain.port.AiAvailability
import io.jadu.promptpong.domain.port.AiGenerationConfig
import io.jadu.promptpong.domain.port.AiToken
import io.jadu.promptpong.domain.port.LocalAiEngine
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Android local AI, backed by ONNX Runtime GenAI over a downloaded model bundle. */
class AndroidOnnxEngine(
    private val storage: AndroidModelStorage,
) : LocalAiEngine {

    private var model: Model? = null
    private var tokenizer: Tokenizer? = null
    private var chatTemplate: String? = null
    private val cancelled = AtomicBoolean(false)

    private fun isLoaded(): Boolean = model != null && tokenizer != null

    // The engine has a quick question: is its model on this phone and ready to go?
    /* override suspend fun availability(): AiAvailability {
        val downloaded = storage.isModelReady()
        return AiAvailability(
            isAvailable = isLoaded(),
            needsDownload = !downloaded,
            engineName = "ONNX Runtime",
            statusText = when {
                isLoaded() -> "Running on device with ${ModelSpec.DISPLAY_NAME}"
                downloaded -> "Model downloaded, not loaded yet"
                else -> "Model not downloaded"
            },
        )
    } */

    // Big moment. Load the local model and tokenizer into Android's AI engine.
    /* override suspend fun load(): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val directory = File(storage.modelDirectoryPath())
            require(File(directory, "genai_config.json").isFile) {
                "The model bundle is missing genai_config.json"
            }

            unload()
            val loadedModel = Model(directory.absolutePath)
            val loadedTokenizer = try {
                Tokenizer(loadedModel)
            } catch (error: Throwable) {
                loadedModel.close()
                throw error
            }

            model = loadedModel
            tokenizer = loadedTokenizer
            chatTemplate = readChatTemplate(directory)
        }
    } */

    // Here comes the real on-device magic. Let ONNX create an answer one token at a time.
    /* override fun generate(prompt: String, config: AiGenerationConfig): Flow<AiToken> = flow {
        val activeModel = model
        val activeTokenizer = tokenizer
        if (activeModel == null || activeTokenizer == null) {
            emit(AiToken.Failed("Model is not loaded"))
            return@flow
        }

        cancelled.set(false)
        try {
            val formatted = applyChatTemplate(activeTokenizer, prompt) + config.assistantPrefill
            if (config.assistantPrefill.isNotEmpty()) {
                emit(AiToken.Text(config.assistantPrefill))
            }
            activeTokenizer.encode(formatted).use { sequences ->
                val promptLength = sequences.getSequence(0).size
                GeneratorParams(activeModel).use { params ->
                    params.setSearchOption(
                        "max_length",
                        (promptLength + config.maxNewTokens).toDouble(),
                    )
                    params.setSearchOption("do_sample", config.temperature > 0f)
                    if (config.temperature > 0f) {
                        params.setSearchOption("temperature", config.temperature.toDouble())
                        params.setSearchOption("top_p", config.topP.toDouble())
                        params.setSearchOption("top_k", config.topK.toDouble())
                    }

                    Generator(activeModel, params).use { generator ->
                        generator.appendTokenSequences(sequences)
                        streamTokens(generator, activeTokenizer, config.stopSequences)
                    }
                }
            }
            emit(if (cancelled.get()) AiToken.Failed("Cancelled") else AiToken.Completed)
        } catch (error: Throwable) {
            emit(AiToken.Failed(error.message ?: "Generation failed"))
        }
    }.flowOn(Dispatchers.Default) */

    override suspend fun cancel() {
        cancelled.set(true)
    }

    private fun readChatTemplate(directory: File): String? {
        val config = File(directory, "tokenizer_config.json")
        if (!config.isFile) return null
        return JSONObject(config.readText())
            .optString("chat_template")
            .takeIf { it.isNotBlank() }
    }

    /**
     * Wraps the instruction in the model's turn format. Without this an
     * instruction-tuned model falls into plain text continuation and copies its
     * input instead of answering it.
     */
    // Models like good manners too. Wrap the prompt in the chat format it understands.
    /* private fun applyChatTemplate(tokenizer: Tokenizer, prompt: String): String {
        val template = chatTemplate ?: return prompt
        val messages = JSONArray().put(
            JSONObject().put("role", "user").put("content", prompt),
        )
        return runCatching {
            tokenizer.applyChatTemplate(template, messages.toString(), null, true)
        }.getOrDefault(prompt)
    } */

    /** Holds back enough characters that a stop sequence split across tokens is still caught. */
    // Do not wait for the whole answer. Send each fresh piece back as it appears.
    /* private suspend fun FlowCollector<AiToken>.streamTokens(
        generator: Generator,
        tokenizer: Tokenizer,
        stopSequences: List<String>,
    ) {
        val stops = stopSequences.filter { it.isNotEmpty() }
        val retained = (stops.maxOfOrNull { it.length } ?: 1) - 1
        val pending = StringBuilder()

        tokenizer.createStream().use { stream ->
            while (!generator.isDone && !cancelled.get()) {
                generator.generateNextToken()
                val text = stream.decode(generator.getLastTokenInSequence(0))
                if (text.isEmpty()) continue

                pending.append(text)

                val stopIndex = stops
                    .mapNotNull { pending.indexOf(it).takeIf { index -> index >= 0 } }
                    .minOrNull()
                if (stopIndex != null) {
                    if (stopIndex > 0) emit(AiToken.Text(pending.substring(0, stopIndex)))
                    return
                }

                val emitLength = pending.length - retained
                if (emitLength > 0) {
                    emit(AiToken.Text(pending.substring(0, emitLength)))
                    pending.delete(0, emitLength)
                }
            }
        }

        if (pending.isNotEmpty() && !cancelled.get()) {
            emit(AiToken.Text(pending.toString()))
        }
    } */

    private fun unload() {
        tokenizer?.close()
        model?.close()
        tokenizer = null
        model = null
        chatTemplate = null
    }
}
