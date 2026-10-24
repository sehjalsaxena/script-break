package com.example.scriptbreak.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalDarkTheme = staticCompositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA8C7FA),          // Google Blue 300
    secondary = Color(0xFFFF8533),        // Google Pay warm orange
    tertiary = Color(0xFF6DD58C),         // Google Green 300
    background = Color(0xFF111318),       // Google Pay Material 3 dark background
    surface = Color(0xFF1E1F22),          // Google Pay elevated card surface
    surfaceVariant = Color(0xFF282A2F),   // Google Pay panel / search pill
    onPrimary = Color(0xFF00325B),
    onSecondary = Color(0xFF552100),
    onTertiary = Color(0xFF00391E),
    onBackground = Color(0xFFE2E2E6),
    onSurface = Color(0xFFE2E2E6),
    outline = Color(0xFF36383B)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF10223F),
    secondary = Color(0xFFFD600D),
    tertiary = Color(0xFF0E7F5B),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F5F9),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    outline = Color(0xFFE2E8F0)
)

@Composable
fun ScriptBreakTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}