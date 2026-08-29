package com.topnotchlock.workorder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TnlNavy = Color(0xFF1B2A4A)
val TnlGold = Color(0xFFC9A227)
val TnlNavyDark = Color(0xFF101A30)

private val LightColors = lightColorScheme(
    primary = TnlNavy,
    secondary = TnlGold,
    tertiary = TnlGold
)

private val DarkColors = darkColorScheme(
    primary = TnlGold,
    secondary = TnlNavy,
    tertiary = TnlGold
)

@Composable
fun TopNotchTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
