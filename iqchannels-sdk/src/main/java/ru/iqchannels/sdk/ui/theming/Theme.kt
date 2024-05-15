package ru.iqchannels.sdk.ui.theming

import Black
import LightGrey
import White
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

private val DarkColorPalette = darkColors(
    primary = Black,
    secondary = LightGrey,
    background = White
)

private val LightColorPalette = lightColors(
    primary = Black,
    secondary = LightGrey,
    background = White

    /* Other default colors to override
    surface = Color.kt.White,
    onPrimary = Color.kt.White,
    onSecondary = Color.kt.Black,
    onBackground = Color.kt.Black,
    onSurface = Color.kt.Black,
    */
)

@Composable
fun IQChannelsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable() () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}