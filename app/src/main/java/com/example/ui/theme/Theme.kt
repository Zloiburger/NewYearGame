package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CozyWinterColorScheme = darkColorScheme(
    primary = GoldAccent,
    onPrimary = Color(0xFF1F1600),
    primaryContainer = Color(0xFF4A380E),
    onPrimaryContainer = GoldAccentLight,
    secondary = IceCyan,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF1D4E5B),
    onSecondaryContainer = Color(0xFFCBEFF5),
    tertiary = HolidayRed,
    onTertiary = Color.White,
    background = NightSkyDark,
    onBackground = SnowWhite,
    surface = NightSurface,
    onSurface = SnowWhite,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = SnowSoft,
    outline = Color(0xFF4A5568)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep cozy winter night theme consistent
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CozyWinterColorScheme,
        typography = Typography,
        content = content
    )
}
