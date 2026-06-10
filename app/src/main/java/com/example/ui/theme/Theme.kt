package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LinkPollLightScheme = lightColorScheme(
    primary = InkDark,
    onPrimary = Color.White,
    secondary = LimeAccent,
    onSecondary = InkDark,
    tertiary = LimeDark,
    onTertiary = InkDark,
    background = WarmBg,
    onBackground = InkDark,
    surface = PanelWhite,
    onSurface = InkDark,
    surfaceVariant = ChipBg,
    onSurfaceVariant = TextMuted,
    outline = LineMuted,
    error = RedAlert
)

// Aesthetic Dark Mode option matching a dark theme if system requests it
private val LinkPollDarkScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = InkDark,
    secondary = LimeAccent,
    onSecondary = InkDark,
    tertiary = LimeDark,
    onTertiary = InkDark,
    background = InkDark,
    onBackground = Color.White,
    surface = Color(0xFF1C1D24), // --dark-2
    onSurface = Color.White,
    surfaceVariant = Color(0xFF131419), // --dark
    onSurfaceVariant = Color(0xFFB9B7C2),
    outline = Color(0xFF2E2F38),
    error = RedAlert
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamic color to enforce LinkPoll's distinctive aesthetic guidelines
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> LinkPollDarkScheme
        else -> LinkPollLightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
