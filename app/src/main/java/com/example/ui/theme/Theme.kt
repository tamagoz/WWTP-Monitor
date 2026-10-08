package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ScadaColorScheme = darkColorScheme(
    primary = ScadaPrimary,
    onPrimary = Color.Black,
    primaryContainer = ScadaPrimaryVariant,
    onPrimaryContainer = Color.White,
    secondary = ScadaSecondary,
    onSecondary = Color.Black,
    tertiary = ScadaAccent,
    background = ScadaDarkBg,
    onBackground = TextPrimary,
    surface = ScadaCardBg,
    onSurface = TextPrimary,
    surfaceVariant = ScadaCardHover,
    onSurfaceVariant = TextSecondary,
    outline = ScadaBorder,
    error = ScadaDanger,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = ScadaColorScheme,
        typography = Typography,
        content = content
    )
}

