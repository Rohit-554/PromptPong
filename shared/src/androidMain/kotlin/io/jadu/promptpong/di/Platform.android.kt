package io.jadu.promptpong.di

import android.content.Context
import io.jadu.promptpong.data.ai.AndroidModelDelivery
import io.jadu.promptpong.data.ai.AndroidModelStorage
import io.jadu.promptpong.data.ai.AndroidOnnxEngine
import io.jadu.promptpong.domain.port.LocalAiEngine
import io.jadu.promptpong.domain.port.ModelDelivery

private var storage: AndroidModelStorage? = null

/** Must be called once from the Android entry point before any AI work. */
fun initPromptPong(context: Context) {
    storage = AndroidModelStorage(context)
}

private fun requireStorage(): AndroidModelStorage =
    checkNotNull(storage) { "initPromptPong(context) was not called" }

actual fun createLocalAiEngine(): LocalAiEngine = AndroidOnnxEngine(requireStorage())

actual fun createModelDelivery(): ModelDelivery = AndroidModelDelivery(requireStorage())
