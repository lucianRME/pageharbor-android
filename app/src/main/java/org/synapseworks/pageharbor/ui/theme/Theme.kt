package org.synapseworks.pageharbor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ColorPaletteLightSurfaceVariant = Color(0xFFE6ECF5)
private val ColorPaletteLightOnSurfaceVariant = Color(0xFF445064)
private val ColorPaletteLightOutline = Color(0xFF6F7A8C)
private val ColorPaletteDarkSurfaceVariant = Color(0xFF22314A)
private val ColorPaletteDarkOnSurfaceVariant = Color(0xFFC6D0E0)
private val ColorPaletteDarkOutline = Color(0xFF8B96A8)

private val DarkColorScheme = darkColorScheme(
    primary = PageHarborAccentBlue,
    onPrimary = PageHarborDarkSlate,
    primaryContainer = PageHarborPrimaryDark,
    onPrimaryContainer = PageHarborWhite,
    secondary = PageHarborAccentBlue,
    onSecondary = PageHarborDarkSlate,
    background = PageHarborDarkBackground,
    onBackground = PageHarborDarkOnSurface,
    surface = PageHarborDarkSurface,
    onSurface = PageHarborDarkOnSurface,
    surfaceVariant = ColorPaletteDarkSurfaceVariant,
    onSurfaceVariant = ColorPaletteDarkOnSurfaceVariant,
    outline = ColorPaletteDarkOutline,
    error = PageHarborDarkError,
    onError = PageHarborDarkSlate,
)

private val LightColorScheme = lightColorScheme(
    primary = PageHarborPrimaryBlue,
    onPrimary = PageHarborWhite,
    primaryContainer = PageHarborAccentBlue,
    onPrimaryContainer = PageHarborDarkSlate,
    secondary = PageHarborPrimaryDark,
    onSecondary = PageHarborWhite,
    background = PageHarborLightSurface,
    onBackground = PageHarborDarkSlate,
    surface = PageHarborWhite,
    onSurface = PageHarborDarkSlate,
    surfaceVariant = ColorPaletteLightSurfaceVariant,
    onSurfaceVariant = ColorPaletteLightOnSurfaceVariant,
    outline = ColorPaletteLightOutline,
    error = PageHarborError,
    onError = PageHarborWhite,
)

@Composable
fun PageHarborTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
