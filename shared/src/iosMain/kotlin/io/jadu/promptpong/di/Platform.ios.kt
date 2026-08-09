package io.jadu.promptpong.di

import io.jadu.promptpong.data.ai.AppleIntelligenceEngine
import io.jadu.promptpong.domain.port.LocalAiEngine
import io.jadu.promptpong.domain.port.ModelDelivery
import io.jadu.promptpong.domain.port.ModelSetupProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

actual fun createLocalAiEngine(): LocalAiEngine = AppleIntelligenceEngine()

/** iOS has nothing to deliver. */
actual fun createModelDelivery(): ModelDelivery = object : ModelDelivery {
    override val requiresDownload: Boolean = false
    override suspend fun isReady(): Boolean = true
    override fun setUp(): Flow<ModelSetupProgress> = flowOf(ModelSetupProgress.Ready)
}
