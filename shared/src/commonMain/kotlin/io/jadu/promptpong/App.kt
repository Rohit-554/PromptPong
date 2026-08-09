package io.jadu.promptpong

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import io.jadu.promptpong.ui.theme.PromptPongTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    viewModel: ChallengeViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ChallengeViewModel(createLocalAiEngine(), createModelDelivery()) }
        },
    ),
) {
    PromptPongTheme {
        val state by viewModel.state.collectAsStateWithLifecycle()

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "PromptPong",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Text(
                                text = "Shout a word, get a dare",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .padding(horizontal = 20.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    AiStatusPill(
                        status = state.aiStatus,
                        onSetUp = viewModel::onSetUpModel,
                        onSkip = viewModel::onSkipAi,
                    )

                    Spacer(Modifier.height(16.dp))

                    val pending = state.pendingWord
                    when {
                        pending != null -> ThinkingCard(pending)
                        state.current != null -> ChallengeCard(state.current!!)
                        else -> EmptyState()
                    }

                    if (state.history.isNotEmpty()) {
                        Spacer(Modifier.height(20.dp))
                        HistorySection(
                            history = state.history,
                            onDelete = viewModel::onDeleteChallenge,
                            onClearAll = viewModel::onClearHistory,
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                }

                WordInput(
                    word = state.word,
                    enabled = state.pendingWord == null,
                    canPlay = state.canPlay,
                    onWordChanged = viewModel::onWordChanged,
                    onPlay = viewModel::onPlay,
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text("!", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Ready when you are",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Type any word the crowd shouts and the app writes a dare for it.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThinkingCard(word: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WordChip(word, MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(28.dp))
            LoadingIndicator(modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Writing a dare",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChallengeCard(challenge: Challenge) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + scaleIn(initialScale = 0.94f),
        exit = fadeOut(),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                WordChip(challenge.word, MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(20.dp))
                Text(
                    text = challenge.text,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 38.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Badge("${challenge.durationSeconds}s")
                    Badge(
                        if (challenge.source == ChallengeSource.MODEL) {
                            "AI wrote this"
                        } else {
                            "built-in dare"
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun WordChip(word: String, contentColor: androidx.compose.ui.graphics.Color) {
    Text(
        text = word.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp,
        color = contentColor,
    )
}

@Composable
private fun Badge(text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.10f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun HistorySection(
    history: List<Challenge>,
    onDelete: (Long) -> Unit,
    onClearAll: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Earlier rounds",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        TextButton(onClick = onClearAll) { Text("Clear all") }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        history.forEach { past ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = past.word.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = past.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    TextButton(onClick = { onDelete(past.id) }) { Text("Delete") }
                }
            }
        }
    }
}

@Composable
private fun WordInput(
    word: String,
    enabled: Boolean,
    canPlay: Boolean,
    onWordChanged: (String) -> Unit,
    onPlay: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = word,
            onValueChange = onWordChanged,
            placeholder = { Text("Word from the crowd") },
            singleLine = true,
            enabled = enabled,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onPlay() }),
        )
        Spacer(Modifier.width(10.dp))
        Button(
            onClick = onPlay,
            enabled = canPlay,
            shape = CircleShape,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 26.dp,
                vertical = 18.dp,
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text("Go", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AiStatusPill(status: AiStatus, onSetUp: () -> Unit, onSkip: () -> Unit) {
    when (status) {
        AiStatus.NeedsDownload -> Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            ),
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    text = "Turn on the AI",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "One download and the app writes every dare on your phone, offline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(onClick = onSetUp, shape = CircleShape) {
                        Text("Download", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onSkip) { Text("Not now") }
                }
            }
        }

        is AiStatus.Downloading -> SetupProgress("Downloading model", status.fraction)
        is AiStatus.Extracting -> SetupProgress("Unpacking model", status.fraction)
        AiStatus.Loading -> StatusChip("Starting the AI", MaterialTheme.colorScheme.secondaryContainer)
        is AiStatus.Ready -> StatusChip("AI ready", MaterialTheme.colorScheme.tertiaryContainer)
        is AiStatus.Unavailable -> StatusChip(
            text = "Built-in dares only",
            container = MaterialTheme.colorScheme.surfaceVariant,
            supporting = status.reason,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusChip(
    text: String,
    container: androidx.compose.ui.graphics.Color,
    supporting: String? = null,
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        AssistChip(
            onClick = {},
            enabled = false,
            shape = CircleShape,
            label = {
                Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            },
            colors = AssistChipDefaults.assistChipColors(
                disabledContainerColor = container,
                disabledLabelColor = MaterialTheme.colorScheme.onSurface,
            ),
            border = null,
        )
        if (supporting != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = supporting,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SetupProgress(label: String, fraction: Float?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                if (fraction != null) {
                    Text(
                        text = "${(fraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            if (fraction != null) {
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                )
            }
        }
    }
}
