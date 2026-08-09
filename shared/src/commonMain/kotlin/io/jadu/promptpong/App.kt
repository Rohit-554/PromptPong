package io.jadu.promptpong

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.jadu.promptpong.di.createLocalAiEngine
import io.jadu.promptpong.di.createModelDelivery
import io.jadu.promptpong.domain.model.Challenge
import io.jadu.promptpong.domain.model.ChallengeSource
import io.jadu.promptpong.ui.AiStatus
import io.jadu.promptpong.ui.ChallengeViewModel

@Composable
fun App(
    viewModel: ChallengeViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ChallengeViewModel(createLocalAiEngine(), createModelDelivery()) }
        },
    ),
) {
    MaterialTheme {
        val state by viewModel.state.collectAsStateWithLifecycle()

        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .safeContentPadding()
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "PromptPong",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Shout a word. Get a dare.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(16.dp))

                AiBanner(
                    status = state.aiStatus,
                    onSetUp = viewModel::onSetUpModel,
                    onSkip = viewModel::onSkipAi,
                )

                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.word,
                        onValueChange = viewModel::onWordChanged,
                        label = { Text("Word from the crowd") },
                        singleLine = true,
                        enabled = state.pendingWord == null,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { viewModel.onPlay() }),
                    )
                    Spacer(Modifier.width(12.dp))
                    Button(onClick = viewModel::onPlay, enabled = state.canPlay) {
                        Text("Go")
                    }
                }

                Spacer(Modifier.height(20.dp))

                val pending = state.pendingWord
                if (pending != null) {
                    ThinkingCard(pending)
                } else {
                    state.current?.let { ChallengeCard(it) }
                }

                if (state.history.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "Earlier",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.history, key = { it.id }) { past ->
                            Text(
                                text = "${past.word} - ${past.text}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThinkingCard(word: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = word.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Writing a dare",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChallengeCard(challenge: Challenge) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = challenge.word.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = challenge.text,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${challenge.durationSeconds}s",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (challenge.source == ChallengeSource.MODEL) {
                        "AI wrote this"
                    } else {
                        "built-in dare"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun AiBanner(status: AiStatus, onSetUp: () -> Unit, onSkip: () -> Unit) {
    when (status) {
        AiStatus.NeedsDownload -> Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Add on-device AI", style = MaterialTheme.typography.titleSmall)
                Text(
                    "A one-time model download lets the app write the dares, entirely " +
                        "offline afterwards. Without it the app uses a fixed set of dares.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    Button(onClick = onSetUp) { Text("Download") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onSkip) { Text("Play without AI") }
                }
            }
        }

        is AiStatus.Downloading -> Progress("Downloading model", status.fraction)
        is AiStatus.Extracting -> Progress("Unpacking model", status.fraction)

        AiStatus.Loading -> Text(
            "Starting local AI",
            style = MaterialTheme.typography.labelMedium,
        )

        is AiStatus.Ready -> Text(
            "On-device AI ready (${status.engineName})",
            style = MaterialTheme.typography.labelMedium,
        )

        is AiStatus.Unavailable -> Text(
            "Playing without AI (${status.reason})",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Progress(label: String, fraction: Float?) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(6.dp))
        if (fraction != null) {
            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
        } else {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}
