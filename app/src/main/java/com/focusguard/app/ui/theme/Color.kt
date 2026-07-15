package com.focusguard.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 专注卫士 2.0 — 三套高质感配色方案
 *
 * 1. Deep Aurora   — 极光暗影：深灰蓝底 + 极光绿发光字 → 深夜深度思考
 * 2. Warm Sand     — 浅砂暮霭：奶咖底色 + 柔白 → 治愈平复焦虑
 * 3. Silent Twilight — 暮紫静夜：深邃暗紫 + 玫瑰粉金 → 梦幻深邃
 */

// ═══════════════════════════════════════════════════════
// 1. Deep Aurora 极光暗影
// ═══════════════════════════════════════════════════════
object Aurora {
    val Primary = Color(0xFF00E5A0)       // 极光绿
    val PrimaryDark = Color(0xFF00B37D)   // 深极光绿
    val OnPrimary = Color(0xFF0A0E27)     // 深蓝黑
    val Background = Color(0xFF0A0E27)    // 极致深灰蓝
    val Surface = Color(0xFF141833)       // 卡片深蓝
    val OnBackground = Color(0xFFE8EAEF)  // 浅白灰文字
    val OnSurface = Color(0xFFD0D3DC)     // 浅灰文字
    val Outline = Color(0xFF3A4066)       // 蓝灰边框
    val Glow = Color(0xFF00E5A0).copy(alpha = 0.3f) // 发光效果
}

// ═══════════════════════════════════════════════════════
// 2. Warm Sand 浅砂暮霭
// ═══════════════════════════════════════════════════════
object WarmSand {
    val Primary = Color(0xFFC8A27A)       // 奶咖色
    val PrimaryDark = Color(0xFFA8845E)   // 深奶咖
    val OnPrimary = Color(0xFFFFF8F0)    // 柔白
    val Background = Color(0xFFF5EDE3)    // 米白底
    val Surface = Color(0xFFFFF8F0)       // 柔白卡片
    val OnBackground = Color(0xFF4A3728)  // 深棕文字
    val OnSurface = Color(0xFF6B5344)     // 中棕文字
    val Outline = Color(0xFFD4C4B0)       // 浅棕边框
    val Glow = Color(0xFFC8A27A).copy(alpha = 0.25f)
}

// ═══════════════════════════════════════════════════════
// 3. Silent Twilight 暮紫静夜
// ═══════════════════════════════════════════════════════
object Twilight {
    val Primary = Color(0xFFC9A0DC)       // 梦幻紫
    val PrimaryDark = Color(0xFFA77EC2)   // 深紫
    val OnPrimary = Color(0xFF1A1028)     // 深紫黑
    val Background = Color(0xFF1A1028)    // 深邃暗紫底
    val Surface = Color(0xFF281E3C)       // 暗紫卡片
    val OnBackground = Color(0xFFF0E6F6)  // 浅紫白文字
    val OnSurface = Color(0xFFD8CDE8)     // 淡紫灰
    val Outline = Color(0xFF4A3E5C)       // 暗紫边框
    val Glow = Color(0xFFE8B4C8).copy(alpha = 0.3f) // 玫瑰粉金发光
}

// ═══════════════════════════════════════════════════════
// 兼容旧版别名
// ═══════════════════════════════════════════════════════
val TomatoRed = Color(0xFFE74C3C)
val TomatoRedDark = Color(0xFFC0392B)
val AccentGold = Color(0xFFF39C12)
val AccentGreen = Color(0xFF2ECC71)
val TextWhite = Color(0xFFF5F5F5)
val TextGray = Color(0xFFAAAAAA)
