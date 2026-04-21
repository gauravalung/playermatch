package com.playermatch.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Green700,
    onPrimary = White,
    primaryContainer = Green100,
    onPrimaryContainer = Gray900,
    secondary = Orange500,
    onSecondary = White,
    secondaryContainer = Orange200,
    onSecondaryContainer = Gray900,
    background = Gray100,
    onBackground = Gray900,
    surface = White,
    onSurface = Gray900,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray600,
    error = StatusRed,
    onError = White
)

private val DarkColors = darkColorScheme(
    primary = Green500,
    onPrimary = Gray900,
    primaryContainer = Green700,
    onPrimaryContainer = White,
    secondary = Orange500,
    onSecondary = Gray900,
    background = Gray900,
    onBackground = White,
    surface = Gray900,
    onSurface = White,
    error = StatusRed,
    onError = White
)

@Composable
fun PlayerMatchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,   // keep brand colors consistent
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
