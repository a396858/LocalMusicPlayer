package com.localmusic.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Brand = Color(0xFF5B4BC4)
private val BrandDark = Color(0xFFBFB6FF)

private val LightColors = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E1FF),
    onPrimaryContainer = Color(0xFF190055),
    secondary = Color(0xFF5F5C71),
    background = Color(0xFFFBF8FF),
    surface = Color(0xFFFBF8FF),
    surfaceVariant = Color(0xFFE7E0F0),
    onSurfaceVariant = Color(0xFF48454F),
    outline = Color(0xFF79757F),
)

private val DarkColors = darkColorScheme(
    primary = BrandDark,
    onPrimary = Color(0xFF2A1B77),
    primaryContainer = Color(0xFF42308F),
    onPrimaryContainer = Color(0xFFE6E1FF),
    secondary = Color(0xFFC9C4DD),
    background = Color(0xFF121016),
    surface = Color(0xFF121016),
    surfaceVariant = Color(0xFF2B2831),
    onSurfaceVariant = Color(0xFFC9C4DD),
    outline = Color(0xFF938F99),
)

@Composable
fun LocalMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
