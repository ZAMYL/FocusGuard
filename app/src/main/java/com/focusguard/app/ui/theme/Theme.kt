package com.focusguard.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * 专注卫士 Compose 主题
 *
 * 全局采用深色系配色，营造专注、克制的视觉氛围。
 */

private val FocusGuardColorScheme = darkColorScheme(
    primary = TomatoRed,
    onPrimary = TextWhite,
    secondary = AccentGold,
    onSecondary = TextWhite,
    tertiary = AccentGreen,
    background = DarkBackground,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    outline = TextGray
)

@Composable
fun FocusGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FocusGuardColorScheme,
        content = content
    )
}
