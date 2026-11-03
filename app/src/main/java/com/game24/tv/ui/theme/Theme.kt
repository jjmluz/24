package com.game24.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

/**
 * Material 3 dark scheme built from the official 24 Game card: royal-blue card,
 * yellow circle with black numbers, red rays and a white centre square.
 */
object Game24Colors {
    val Surface = Color(0xFF101A5C)
    val SurfaceContainerHigh = Color(0xFF1D2A85)
    val OnSurface = Color(0xFFFFFFFF)
    val OnSurfaceVariant = Color(0xFFC9CEF2)
    val Outline = Color(0xFF8A92D6)
    val OutlineVariant = Color(0xFF3A4699)

    // Yellow: starting number cards, highlights, stars, the selected operator.
    val Primary = Color(0xFFF6CE3C)
    val OnPrimary = Color(0xFF141414)
    val PrimaryContainer = Color(0xFFF6CE3C)
    val OnPrimaryContainer = Color(0xFF141414)

    // Red: operators waiting to be picked, secondary buttons.
    val SecondaryContainer = Color(0xFFC8203A)
    val OnSecondaryContainer = Color(0xFFFFFFFF)

    // White: cards made by merging two numbers.
    val TertiaryContainer = Color(0xFFFFFFFF)
    val OnTertiaryContainer = Color(0xFF141414)

    val ErrorContainer = Color(0xFFC8203A)
    val OnErrorContainer = Color(0xFFFFFFFF)

    // Difficulty dots, as on the official cards: white, red, yellow. This red is lifted to read on the blue.
    val DotEasy = Color(0xFFFFFFFF)
    val DotMedium = Color(0xFFEF4058)
    val DotHard = Color(0xFFF6CE3C)

    // Focused buttons.
    val InverseSurface = Color(0xFFFFFFFF)
    val InverseOnSurface = Color(0xFF101A5C)
}

@Composable
fun Game24Theme(content: @Composable () -> Unit) {
    val c = Game24Colors
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = c.Primary,
            onPrimary = c.OnPrimary,
            primaryContainer = c.PrimaryContainer,
            onPrimaryContainer = c.OnPrimaryContainer,
            secondaryContainer = c.SecondaryContainer,
            onSecondaryContainer = c.OnSecondaryContainer,
            tertiaryContainer = c.TertiaryContainer,
            onTertiaryContainer = c.OnTertiaryContainer,
            background = c.Surface,
            onBackground = c.OnSurface,
            surface = c.Surface,
            onSurface = c.OnSurface,
            surfaceVariant = c.SurfaceContainerHigh,
            onSurfaceVariant = c.OnSurfaceVariant,
            border = c.Outline,
            borderVariant = c.OutlineVariant,
            errorContainer = c.ErrorContainer,
            onErrorContainer = c.OnErrorContainer,
            inverseSurface = c.InverseSurface,
            inverseOnSurface = c.InverseOnSurface,
        ),
        content = content,
    )
}
