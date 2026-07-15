@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.focusguard.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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

/**
 * 全屏锁机界面
 *
 * 由 [MyAccessibilityService] 检测到违规应用后启动。
 * 使用 FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK 确保始终位于任务栈最前端。
 * 禁用返回键，并隐藏系统 UI（状态栏、导航栏），用户无法直接退出。
 *
 * 界面构成：
 * - 倒计时番茄钟（显示剩余专注时间）
 * - AI 毒舌警醒文本区域（由 DeepSeek 异步生成）
 * - "主动退出专注"按钮（彻底放弃本次番茄钟）
 */
class LockActivity : ComponentActivity() {

    companion object {
        /** Intent Extra key —— 违规原因 */
        const val EXTRA_BLOCK_REASON = "block_reason"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── 设置全屏不可退出模式 ──────────────────────
        configureFullScreenMode()
        disableBackButton()

        // ── 获取违规原因 ──────────────────────────────
        val blockReason = intent.getStringExtra(EXTRA_BLOCK_REASON) ?: "未知应用"

        setContent {
            FocusGuardTheme {
                LockScreenContent(blockReason = blockReason)
            }
        }
    }

    // ──────────────────────────────────────────────────
    // 全屏与防退出配置
    // ──────────────────────────────────────────────────

    /**
     * 设置全屏模式：
     * - 隐藏状态栏和导航栏
     * - 让内容延伸到系统栏区域
     * - 设置锁屏展示 Flag（在锁屏之上显示）
     */
    private fun configureFullScreenMode() {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        // 让内容延伸到系统状态栏和导航栏下方
        WindowCompatController.hideSystemBars(window)
    }

    /**
     * 禁用系统返回键（含手势返回）
     *
     * 这是防绕过的关键措施 —— 用户无法通过返回手势或按钮退出锁机页面。
     */
    private fun disableBackButton() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // 不处理事件 → 禁用返回
                    // 也可以加一个 Toast 提示：留空即静默拦截
                }
            }
        )
    }
}

// ──────────────────────────────────────────────────────
// Compose 锁机界面内容
// ──────────────────────────────────────────────────────

/**
 * 锁机页面主 Composable
 *
 * @param blockReason 触发拦截的原因（如"抖音"、"微信朋友圈"）
 */
@Composable
private fun LockScreenContent(blockReason: String) {
    val context = LocalContext.current

    // ── 状态 ────────────────────────────────────────
    val remainingSeconds by TimerState.remainingSeconds.collectAsState()
    var aiMessage by remember { mutableStateOf("正在请求 AI 警醒语…") }

    // ── 启动时异步请求 DeepSeek ─────────────────────
    LaunchedEffect(blockReason) {
        aiMessage = DeepSeekClient.fetchMotivation(blockReason)
    }

    // ── UI 布局 ─────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        Color(0xFF2C0018) // 深红向黑色过渡
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ══════════════════════════════════════════
            // 标题
            // ══════════════════════════════════════════
            Text(
                text = "⚠️ 专注时间",
                color = TomatoRed,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "你正在试图打开 $blockReason",
                color = TextGray,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ══════════════════════════════════════════
            // 圆形倒计时番茄钟
            // ══════════════════════════════════════════
            TomatoCountdownCircle(remainingSeconds = remainingSeconds)

            Spacer(modifier = Modifier.height(40.dp))

            // ══════════════════════════════════════════
            // AI 毒舌警醒文本
            // ══════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = DarkSurface.copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = aiMessage,
                    modifier = Modifier.padding(24.dp),
                    color = AccentGold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // ══════════════════════════════════════════
            // "放弃专注"按钮
            // ══════════════════════════════════════════
            Button(
                onClick = {
                    // 停止番茄钟
                    TimerState.stop()
                    // 关闭锁机界面
                    (context as? android.app.Activity)?.finish()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TomatoRedDark.copy(alpha = 0.6f),
                    contentColor = TextWhite
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "放弃本次专注",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "放弃专注即放弃自律，三思而后行",
                color = TextGray.copy(alpha = 0.6f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ──────────────────────────────────────────────────────
// 圆形倒计时组件
// ──────────────────────────────────────────────────────

/**
 * 番茄钟圆形倒计时
 *
 * 显示剩余分钟:秒数，圆环进度条随剩余时间动态变化。
 *
 * @param remainingSeconds 剩余秒数
 */
@Composable
private fun TomatoCountdownCircle(remainingSeconds: Int) {
    val totalSeconds by TimerState.durationSeconds.collectAsState()

    // 计算进度比例（0~1）
    val progress = if (totalSeconds > 0) {
        remainingSeconds.toFloat() / totalSeconds.toFloat()
    } else 0f

    // 动画过渡的进度值
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000),
        label = "倒计时进度动画"
    )

    // 进度颜色：剩余越少越红
    val progressColor by animateColorAsState(
        targetValue = when {
            progress > 0.6f -> AccentGreen
            progress > 0.3f -> AccentGold
            else -> TomatoRed
        },
        animationSpec = tween(800),
        label = "颜色过渡"
    )

    // 格式化时间字符串
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeText = "%02d:%02d".format(minutes, seconds)

    // 缩放脉冲动画（剩余 < 60 秒时开始脉冲）
    val pulseScale by animateFloatAsState(
        targetValue = if (remainingSeconds in 1..59) 1.08f else 1f,
        animationSpec = tween(600),
        label = "脉冲动画"
    )

    Box(
        modifier = Modifier
            .size(220.dp)
            .scale(pulseScale),
        contentAlignment = Alignment.Center
    ) {
        // 背景圆环
        CircularProgressIndicator(
            progress = 1f,
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 10.dp,
            color = Color.White.copy(alpha = 0.1f),
            trackColor = Color.Transparent
        )

        // 进度圆环
        CircularProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 12.dp,
            color = progressColor,
            trackColor = Color.Transparent
        )

        // 时间文本
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = timeText,
                color = TextWhite,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "剩余时间",
                color = TextGray,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * 隐藏系统状态栏和导航栏的辅助对象
 *
 * 使用 WindowInsetsControllerCompat 实现，兼容多种 Android 版本。
 */
private object WindowCompatController {
    fun hideSystemBars(window: android.view.Window) {
        val controller = androidx.core.view.WindowCompat.getInsetsController(
            window,
            window.decorView
        )
        controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
