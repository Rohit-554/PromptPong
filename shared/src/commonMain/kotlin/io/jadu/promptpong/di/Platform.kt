package io.jadu.promptpong.di

import io.jadu.promptpong.domain.port.LocalAiEngine
import io.jadu.promptpong.domain.port.ModelDelivery

/** The local AI backend for this platform. */
expect fun createLocalAiEngine(): LocalAiEngine

expect fun createModelDelivery(): ModelDelivery
