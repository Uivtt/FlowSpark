package com.flowspark.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Indigo500,
    onPrimary = Color.White,
    primaryContainer = Indigo100,
    onPrimaryContainer = Slate900,
    secondary = Emerald500,
    background = Slate50,
    surface = Color.White,
    onSurface = Slate900,
    onSurfaceVariant = Slate600,
    outline = Slate200,
)

private val DarkColors = darkColorScheme(
    primary = Indigo500,
    onPrimary = Color.White,
    background = Slate900,
    surface = Color(0xFF1E293B),
    onSurface = Color.White,
    onSurfaceVariant = Slate200,
)

@Composable
fun FlowSparkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
