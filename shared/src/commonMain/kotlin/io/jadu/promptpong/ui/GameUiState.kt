package io.jadu.promptpong.ui

import io.jadu.promptpong.domain.model.Challenge

sealed interface AiStatus {
    data object NeedsDownload : AiStatus
    data class Downloading(val fraction: Float?) : AiStatus
    data class Extracting(val fraction: Float?) : AiStatus
    data object Loading : AiStatus
    data class Ready(val engineName: String) : AiStatus
    data class Unavailable(val reason: String) : AiStatus
}

data class GameUiState(
    val word: String = "",
    val current: Challenge? = null,
    val history: List<Challenge> = emptyList(),
    val aiStatus: AiStatus = AiStatus.Loading,
    /** Word being generated for, while the model is still working. */
    val pendingWord: String? = null,
) {
    val canPlay: Boolean get() = word.isNotBlank() && pendingWord == null
}
