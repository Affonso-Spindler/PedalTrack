package com.affonso.pedaltrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// "Electric" palette: violet -> cyan, with an amber accent for calorie/effort chips.
val PedalPrimary = Color(0xFF7C3AED)
val PedalSecondary = Color(0xFF06B6D4)
val PedalTertiary = Color(0xFFEA580C)

private val LightColors = lightColorScheme(
    primary = PedalPrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDE4FD),
    onPrimaryContainer = Color(0xFF2D0A66),
    secondary = PedalSecondary,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD9F7FC),
    onSecondaryContainer = Color(0xFF01353E),
    tertiary = PedalTertiary,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE4D3),
    onTertiaryContainer = Color(0xFF4A1D00),
    background = Color(0xFFFAF9FB),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF0EDF5),
    onSurfaceVariant = Color(0xFF49454E),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC4A2FA),
    onPrimary = Color(0xFF3D0E8C),
    primaryContainer = Color(0xFF5B24C9),
    onPrimaryContainer = Color(0xFFEDE4FD),
    secondary = Color(0xFF5FE0F5),
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004E5A),
    onSecondaryContainer = Color(0xFFD9F7FC),
    tertiary = Color(0xFFFFB68C),
    onTertiary = Color(0xFF5A2200),
    tertiaryContainer = Color(0xFF7A3300),
    onTertiaryContainer = Color(0xFFFFE4D3),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E1E9),
    surface = Color(0xFF1D1B20),
    onSurface = Color(0xFFE6E1E9),
    surfaceVariant = Color(0xFF49454E),
    onSurfaceVariant = Color(0xFFCAC4CF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun PedalTrackTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colorScheme, content = content)
}
