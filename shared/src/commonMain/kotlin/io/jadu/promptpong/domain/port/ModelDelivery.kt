package io.jadu.promptpong.domain.port

import kotlinx.coroutines.flow.Flow

/** Progress of getting the model onto the device. */
sealed interface ModelSetupProgress {
    data class Downloading(val bytes: Long, val totalBytes: Long?) : ModelSetupProgress {
        /** 0f..1f, or null when the server sent no length. */
        val fraction: Float? get() = totalBytes?.takeIf { it > 0 }?.let { bytes.toFloat() / it }
    }

    data class Extracting(val files: Int, val totalFiles: Int) : ModelSetupProgress
    data object Verifying : ModelSetupProgress
    data object Ready : ModelSetupProgress
    data class Failed(val reason: String) : ModelSetupProgress
}

/** Gets the model onto the device. */
interface ModelDelivery {
    /** True when the model is present and usable, so setup can be skipped. */
    suspend fun isReady(): Boolean

    /** Whether this platform needs a download at all. */
    val requiresDownload: Boolean

    fun setUp(): Flow<ModelSetupProgress>
}
