package com.keshav.capturesposed.ui.theme

import android.annotation.SuppressLint
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

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

private val AmoledDarkColorPalette = darkColorScheme(
    primary = Purple80,
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF1F1B2E),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = PurpleGrey80,
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF1E1B26),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Pink80,
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF28181E),
    onTertiaryContainer = Color(0xFFFFD8E4),
    background = Color.Black,
    onBackground = Color(0xFFE6E1E5),
    surface = Color.Black,
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color.Black,
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color.Black,
    surfaceContainer = Color.Black,
    surfaceContainerHigh = Color(0xFF0C0C0C),
    surfaceContainerHighest = Color(0xFF141414),
    surfaceDim = Color.Black,
    surfaceBright = Color(0xFF1A1A1A),
    outline = Color(0xFF2C2C2C),
    outlineVariant = Color(0xFF1C1C1C)
)

private val LightColorPalette = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
)

@SuppressLint("ObsoleteSdkInt")
@Composable
fun APPTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        darkTheme -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Keep dynamic accent colors from system but enforce pure AMOLED black background and surfaces
                dynamicDarkColorScheme(context).copy(
                    background = Color.Black,
                    onBackground = Color(0xFFE6E1E5),
                    surface = Color.Black,
                    onSurface = Color(0xFFE6E1E5),
                    surfaceVariant = Color.Black,
                    surfaceDim = Color.Black,
                    surfaceContainerLowest = Color.Black,
                    surfaceContainerLow = Color.Black,
                    surfaceContainer = Color.Black,
                    surfaceContainerHigh = Color(0xFF0C0C0C),
                    surfaceContainerHighest = Color(0xFF141414),
                    outline = Color(0xFF2E2E2E),
                    outlineVariant = Color(0xFF1E1E1E)
                )
            } else {
                AmoledDarkColorPalette
            }
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            dynamicLightColorScheme(context)
        }
        else -> LightColorPalette
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}