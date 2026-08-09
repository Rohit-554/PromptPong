package io.jadu.promptpong.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Indigo = Color(0xFF5B3DF5)
private val IndigoLight = Color(0xFFC7BAFF)
private val Coral = Color(0xFFFF3D68)
private val Lime = Color(0xFF9BE800)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6DEFF),
    onPrimaryContainer = Color(0xFF1B0080),
    secondary = Coral,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9E0),
    onSecondaryContainer = Color(0xFF5C0020),
    tertiary = Color(0xFF4C7A00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDFF7A8),
    onTertiaryContainer = Color(0xFF1B2E00),
    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1A1B25),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1A1B25),
    surfaceVariant = Color(0xFFE6E0F2),
    onSurfaceVariant = Color(0xFF484456),
    outline = Color(0xFF797588),
    outlineVariant = Color(0xFFCAC4D6),
)

private val DarkColors = darkColorScheme(
    primary = IndigoLight,
    onPrimary = Color(0xFF2A008E),
    primaryContainer = Color(0xFF4223D4),
    onPrimaryContainer = Color(0xFFE6DEFF),
    secondary = Color(0xFFFFB1C1),
    onSecondary = Color(0xFF630026),
    secondaryContainer = Color(0xFF8E0038),
    onSecondaryContainer = Color(0xFFFFD9E0),
    tertiary = Color(0xFFC2E884),
    onTertiary = Color(0xFF243600),
    tertiaryContainer = Color(0xFF375000),
    onTertiaryContainer = Color(0xFFDFF7A8),
    background = Color(0xFF131118),
    onBackground = Color(0xFFE6E1EC),
    surface = Color(0xFF131118),
    onSurface = Color(0xFFE6E1EC),
    surfaceVariant = Color(0xFF484456),
    onSurfaceVariant = Color(0xFFCAC4D6),
    outline = Color(0xFF948FA3),
    outlineVariant = Color(0xFF484456),
)

/** Rounder than stock Material, which reads as playful rather than corporate. */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun PromptPongTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = AppShapes,
        content = content,
    )
}

val Lime500: Color get() = Lime
