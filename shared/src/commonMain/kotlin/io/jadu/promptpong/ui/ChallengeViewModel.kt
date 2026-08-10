package io.jadu.promptpong.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.jadu.promptpong.data.HybridChallengeGenerator
import io.jadu.promptpong.data.ai.AiChallengeUpgrader
import io.jadu.promptpong.data.template.TemplateChallengeGenerator
import io.jadu.promptpong.domain.port.ChallengeUpgrader
import io.jadu.promptpong.domain.port.LocalAiEngine
import io.jadu.promptpong.domain.port.ModelDelivery
import io.jadu.promptpong.domain.port.ModelSetupProgress
import kotlin.concurrent.Volatile
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val HISTORY_LIMIT = 30

class ChallengeViewModel(
    private val engine: LocalAiEngine,
    private val delivery: ModelDelivery,
) : ViewModel() {

    private val templates = TemplateChallengeGenerator()

    @Volatile
    private var upgrader: ChallengeUpgrader? = null

    private val generator = HybridChallengeGenerator(
        templates = templates,
        upgraderProvider = { upgrader },
    )

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private var round: Job? = null

    // Welcome to the control room. Let the app check whether local AI is ready.
    /* init {
        viewModelScope.launch {
            if (delivery.isReady()) prepareEngine() else showSetupNeeded()
        }
    } */

    fun onWordChanged(word: String) {
        _state.value = _state.value.copy(word = word)
    }

    // This button has ideas, but it needs you to let the next round begin.
    /* fun onPlay() {
        val word = _state.value.word.trim()
        if (word.isBlank()) return

        round?.cancel()
        _state.value = _state.value.copy(word = "", pendingWord = word)

        round = viewModelScope.launch {
            generator.generate(word).collect { challenge ->
                val previous = _state.value.current
                _state.value = _state.value.copy(
                    current = challenge,
                    history = if (previous != null) {
                        (listOf(previous) + _state.value.history).take(HISTORY_LIMIT)
                    } else {
                        _state.value.history
                    },
                    pendingWord = null,
                )
            }
            _state.value = _state.value.copy(pendingWord = null)
        }
    } */

    fun onDeleteChallenge(id: Long) {
        val current = _state.value
        _state.value = current.copy(
            history = current.history.filterNot { it.id == id },
            current = current.current?.takeIf { it.id != id },
        )
    }

    fun onClearHistory() {
        _state.value = _state.value.copy(history = emptyList())
    }

    // Big level ahead. Bring this back to download and prepare Android's model.
    /* fun onSetUpModel() {
        viewModelScope.launch {
            delivery.setUp().collect { progress ->
                when (progress) {
                    is ModelSetupProgress.Downloading -> _state.value = _state.value.copy(
                        aiStatus = AiStatus.Downloading(progress.fraction),
                    )

                    is ModelSetupProgress.Extracting -> _state.value = _state.value.copy(
                        aiStatus = AiStatus.Extracting(
                            progress.totalFiles.takeIf { it > 0 }
                                ?.let { progress.files.toFloat() / it },
                        ),
                    )

                    ModelSetupProgress.Verifying -> _state.value = _state.value.copy(
                        aiStatus = AiStatus.Loading,
                    )

                    is ModelSetupProgress.Failed -> _state.value = _state.value.copy(
                        aiStatus = AiStatus.Unavailable(progress.reason),
                    )

                    ModelSetupProgress.Ready -> prepareEngine()
                }
            }
        }
    } */

    fun onSkipAi() {
        upgrader = null
        _state.value = _state.value.copy(aiStatus = AiStatus.Unavailable("AI turned off"))
    }

    // You made it to the engine room. Time to see if the local model can wake up.
    /* private suspend fun prepareEngine() {
        _state.value = _state.value.copy(aiStatus = AiStatus.Loading)

        val availability = engine.availability()
        if (availability.needsDownload) {
            showSetupNeeded()
            return
        }

        val result = engine.load()
        if (result.isSuccess) {
            upgrader = AiChallengeUpgrader(engine)
            _state.value = _state.value.copy(aiStatus = AiStatus.Ready(availability.engineName))
        } else {
            upgrader = null
            _state.value = _state.value.copy(
                aiStatus = AiStatus.Unavailable(
                    result.exceptionOrNull()?.message ?: availability.statusText,
                ),
            )
        }
    } */

    private suspend fun showSetupNeeded() {
        _state.value = _state.value.copy(
            aiStatus = if (delivery.requiresDownload) {
                AiStatus.NeedsDownload
            } else {
                AiStatus.Unavailable(engine.availability().statusText)
            },
        )
    }
}
