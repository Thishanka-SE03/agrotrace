package com.agrotrace.scanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AgroGreen = Color(0xFF1A4A2E)
val AgroGreenDark = Color(0xFF163D26)
val AgroGreenMid = Color(0xFF2D6A3F)
val AgroGold = Color(0xFFC8A84B)
val AgroBackground = Color(0xFFF4F6F8)
val AgroSurfaceSoft = Color(0xFFF0F7F2)
val AgroError = Color(0xFFB3261E)

private val LightColors = lightColorScheme(
    primary = AgroGreen,
    onPrimary = Color.White,
    primaryContainer = AgroSurfaceSoft,
    onPrimaryContainer = AgroGreenDark,
    secondary = AgroGold,
    onSecondary = Color(0xFF2D2600),
    background = AgroBackground,
    onBackground = Color(0xFF1A1C1A),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1A),
    error = AgroError
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9BD5AA),
    onPrimary = Color(0xFF003915),
    secondary = Color(0xFFE4C96B),
    background = Color(0xFF101512),
    surface = Color(0xFF171D19)
)

@Composable
fun AgroTraceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}
