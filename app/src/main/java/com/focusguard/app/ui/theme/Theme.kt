package com.focusguard.app.ui.theme

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 专注卫士 2.0 主题系统
 *
 * 三套高质感配色，用户一键切换。
 * Warm Sand 使用亮色主题，其余使用暗色主题。
 */

enum class ThemePreset { AURORA, WARM_SAND, TWILIGHT }

val LocalThemePreset = compositionLocalOf { ThemePreset.AURORA }

/** SharedPreferences Key */
const val KEY_THEME_PRESET = "theme_preset"

fun saveThemePreset(context: Context, preset: ThemePreset) {
    context.getSharedPreferences("focusguard_prefs", Context.MODE_PRIVATE)
        .edit().putString(KEY_THEME_PRESET, preset.name).apply()
}

fun loadThemePreset(context: Context): ThemePreset {
    val name = context.getSharedPreferences("focusguard_prefs", Context.MODE_PRIVATE)
        .getString(KEY_THEME_PRESET, ThemePreset.AURORA.name) ?: ThemePreset.AURORA.name
    return try { ThemePreset.valueOf(name) } catch (_: Exception) { ThemePreset.AURORA }
}

@Composable
fun FocusGuardTheme(preset: ThemePreset = ThemePreset.AURORA, content: @Composable () -> Unit) {
    val colorScheme = when (preset) {
        ThemePreset.AURORA -> darkColorScheme(
            primary = Aurora.Primary, onPrimary = Aurora.OnPrimary,
            background = Aurora.Background, onBackground = Aurora.OnBackground,
            surface = Aurora.Surface, onSurface = Aurora.OnSurface,
            outline = Aurora.Outline, secondary = Aurora.Glow,
            tertiary = Aurora.PrimaryDark
        )
        ThemePreset.WARM_SAND -> lightColorScheme(
            primary = WarmSand.Primary, onPrimary = WarmSand.OnPrimary,
            background = WarmSand.Background, onBackground = WarmSand.OnBackground,
            surface = WarmSand.Surface, onSurface = WarmSand.OnSurface,
            outline = WarmSand.Outline, secondary = WarmSand.Glow,
            tertiary = WarmSand.PrimaryDark
        )
        ThemePreset.TWILIGHT -> darkColorScheme(
            primary = Twilight.Primary, onPrimary = Twilight.OnPrimary,
            background = Twilight.Background, onBackground = Twilight.OnBackground,
            surface = Twilight.Surface, onSurface = Twilight.OnSurface,
            outline = Twilight.Outline, secondary = Twilight.Glow,
            tertiary = Twilight.PrimaryDark
        )
    }
    CompositionLocalProvider(LocalThemePreset provides preset) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
