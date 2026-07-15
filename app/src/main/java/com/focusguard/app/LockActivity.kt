@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.focusguard.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.app.ui.theme.*
import kotlinx.coroutines.delay

/**
 * 全屏锁机界面 2.0
 *
 * 呼吸动画倒计时 + AI 毒舌警醒淡入文字 + 三套主题适配。
 * 禁用返回键，用户无法绕过。
 */
class LockActivity : ComponentActivity() {

    companion object { const val EXTRA_BLOCK_REASON = "block_reason" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        hideSystemBars(window)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* 禁用返回 */ }
        })

        val blockReason = intent.getStringExtra(EXTRA_BLOCK_REASON) ?: "未知应用"
        val preset = loadThemePreset(this)

        setContent {
            FocusGuardTheme(preset = preset) {
                LockScreen2(blockReason = blockReason)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// 锁机界面 2.0
// ═══════════════════════════════════════════════════════

@Composable
private fun LockScreen2(blockReason: String) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val remainingSeconds by TimerState.remainingSeconds.collectAsState()
    val totalSeconds by TimerState.durationSeconds.collectAsState()

    // AI 文字（初始用本地语录占位，DeepSeek 返回后替换）
    var aiMessage by remember { mutableStateOf(LocalQuotes.randomBlockQuote()) }
    var aiVisible by remember { mutableStateOf(false) }

    LaunchedEffect(blockReason) {
        delay(300) // 微延迟，先展示本地语录
        aiMessage = DeepSeekClient.fetchMotivation(blockReason)
    }
    // 文字淡入
    LaunchedEffect(Unit) { aiVisible = true }

    // ── 呼吸动画 ──────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "breathe"
    )

    val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds else 0f
    val minutes = remainingSeconds / 60
    val timeText = "%02d:%02d".format(minutes, remainingSeconds % 60)

    val ringColor = when {
        progress > 0.5f -> colors.primary
        progress > 0.2f -> Color(0xFFF39C12)
        else -> Color(0xFFE74C3C)
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(colors.background, Color(0xFF0D0018)))
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(20.dp))

            // ── 标题 ──────────────────────────────────
            Text("⚠️ 专注时间", color = Color(0xFFE74C3C), fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("检测到你试图打开 $blockReason", color = colors.onSurface.copy(alpha = 0.5f),
                fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))

            Spacer(Modifier.height(32.dp))

            // ── 呼吸倒计时圆 ──────────────────────────
            Box(
                modifier = Modifier.size(200.dp).scale(breatheScale),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = progress, modifier = Modifier.fillMaxSize(),
                    strokeWidth = 10.dp, color = ringColor,
                    trackColor = Color.White.copy(alpha = 0.06f)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(timeText, color = colors.onBackground, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                    Text("剩余时间", color = colors.onSurface.copy(alpha = 0.4f), fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(36.dp))

            // ── AI 毒舌文字（淡入） ────────────────────
            AnimatedVisibility(visible = aiVisible, enter = fadeIn(tween(800)) + slideInVertically(tween(800)) { it / 8 }) {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        aiMessage, Modifier.padding(20.dp),
                        color = Color(0xFFF39C12), fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                        lineHeight = 28.sp
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // ── 放弃按钮 ──────────────────────────────
            Button(
                onClick = { TimerState.stop(); (context as? android.app.Activity)?.finish() },
                Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC0392B).copy(alpha = 0.7f)),
                shape = RoundedCornerShape(14.dp)
            ) { Text("放弃本次专注", fontSize = 17.sp, fontWeight = FontWeight.Bold) }

            Text("放弃专注即放弃自律，三思而后行", color = colors.onSurface.copy(alpha = 0.3f),
                fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/** 隐藏系统状态栏和导航栏 */
private fun hideSystemBars(window: android.view.Window) {
    val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
    controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
    controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
}
