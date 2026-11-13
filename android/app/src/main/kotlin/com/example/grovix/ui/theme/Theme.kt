package com.example.grovix.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Strict Dark Mode Only Color Scheme
private val DarkColorScheme = darkColorScheme(
    primary = GrovixNavy,
    onPrimary = Color.White,
    primaryContainer = GrovixIndigo,
    onPrimaryContainer = Color.White,
    secondary = GrovixAccent,
    onSecondary = Color.Black,
    background = GrovixBackground,
    onBackground = GrovixTextDark,
    surface = GrovixSurface,
    onSurface = GrovixTextDark,
    surfaceVariant = GrovixSurfaceVariant,
    onSurfaceVariant = GrovixTextMuted,
    error = GrovixError,
    onError = Color.White
)

@Composable
fun GrovixTheme(
    darkTheme: Boolean = true, // Enforce Dark Mode ONLY
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
