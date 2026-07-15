@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.focusguard.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.app.ui.theme.*
import kotlinx.coroutines.*

/**
 * 主界面 Activity —— 番茄钟控制面板
 *
 * 提供：
 * - 番茄钟时长选择（15分钟 / 25分钟 / 45分钟 / 60分钟）
 * - 开始 / 暂停 / 停止 控制
 * - 剩余时间倒计时显示
 * - 无障碍服务状态提示
 * - 跳转系统无障碍设置的入口
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FocusGuardTheme {
                MainScreen()
            }
        }
    }
}

// ──────────────────────────────────────────────────────
// 主界面 Composable
// ──────────────────────────────────────────────────────

@Composable
private fun MainScreen() {
    val context = LocalContext.current

    // 番茄钟状态
    val isRunning by TimerState.isRunning.collectAsState()
    val remainingSeconds by TimerState.remainingSeconds.collectAsState()
    val durationSeconds by TimerState.durationSeconds.collectAsState()

    // 无障碍服务是否已开启
    var isAccessibilityEnabled by remember { mutableStateOf(false) }

    // 检查无障碍服务状态
    LaunchedEffect(Unit) {
        isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
    }

    // 每秒钟刷新一次无障碍服务状态
    LaunchedEffect(isRunning) {
        while (true) {
            delay(1000)
            isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // ══════════════════════════════════════════════
        // 顶部标题
        // ══════════════════════════════════════════════
        Text(
            text = "🍅 专注卫士",
            color = TextWhite,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "专注当下，拒绝分心",
            color = TextGray,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ══════════════════════════════════════════════
        // 无障碍服务状态卡片
        // ══════════════════════════════════════════════
        AccessibilityStatusCard(
            isEnabled = isAccessibilityEnabled,
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                )
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ══════════════════════════════════════════════
        // 番茄钟圆形倒计时
        // ══════════════════════════════════════════════
        MainTomatoCircle(
            remainingSeconds = remainingSeconds,
            totalSeconds = durationSeconds,
            isRunning = isRunning
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ══════════════════════════════════════════════
        // 时长选择按钮组
        // ══════════════════════════════════════════════
        if (!isRunning) {
            DurationSelector(
                selectedSeconds = durationSeconds,
                onSelect = { TimerState.setDuration(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ══════════════════════════════════════════════
        // 开始 / 暂停 / 停止 控制按钮
        // ══════════════════════════════════════════════
        ControlButtons(
            isRunning = isRunning,
            remainingSeconds = remainingSeconds,
            isAccessibilityEnabled = isAccessibilityEnabled,
            onStart = {
                TimerState.start()
                CountdownManager.start(context)
                Toast.makeText(context, "番茄钟已启动，专注时间内将拦截干扰应用", Toast.LENGTH_SHORT).show()
            },
            onPause = {
                TimerState.pause()
                CountdownManager.stop()
                Toast.makeText(context, "番茄钟已暂停", Toast.LENGTH_SHORT).show()
            },
            onResume = {
                TimerState.resume()
                CountdownManager.start(context)
                Toast.makeText(context, "番茄钟已继续", Toast.LENGTH_SHORT).show()
            },
            onStop = {
                TimerState.stop()
                CountdownManager.stop()
                Toast.makeText(context, "番茄钟已停止", Toast.LENGTH_SHORT).show()
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ══════════════════════════════════════════════
        // 拦截说明
        // ══════════════════════════════════════════════
        BlockRulesInfo()

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * 无障碍服务状态卡片
 *
 * 绿色 = 已开启，红色 = 未开启。
 * 未开启时卡片可点击跳转到系统无障碍设置页面。
 */
@Composable
private fun AccessibilityStatusCard(isEnabled: Boolean, onClick: () -> Unit) {
    val bgColor = if (isEnabled) AccentGreen.copy(alpha = 0.15f) else TomatoRed.copy(alpha = 0.15f)
    val borderColor = if (isEnabled) AccentGreen.copy(alpha = 0.4f) else TomatoRed.copy(alpha = 0.4f)
    val statusText = if (isEnabled) "✓ 无障碍服务已开启" else "✗ 无障碍服务未开启"
    val hintText = if (isEnabled) "" else "（点击前往设置）"

    Card(
        onClick = { if (!isEnabled) onClick() },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isEnabled) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (isEnabled) AccentGreen else TomatoRed,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = statusText,
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (hintText.isNotEmpty()) {
                    Text(
                        text = hintText,
                        color = TextGray,
                        fontSize = 12.sp
                    )
                }
            }
            if (!isEnabled) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = TextGray
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────
// 番茄钟圆形倒计时
// ──────────────────────────────────────────────────────

@Composable
private fun MainTomatoCircle(
    remainingSeconds: Int,
    totalSeconds: Int,
    isRunning: Boolean
) {
    val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000),
        label = "进度动画"
    )

    val progressColor by animateColorAsState(
        targetValue = when {
            !isRunning -> TextGray.copy(alpha = 0.5f)
            progress > 0.6f -> AccentGreen
            progress > 0.3f -> AccentGold
            else -> TomatoRed
        },
        animationSpec = tween(800),
        label = "颜色动画"
    )

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeText = "%02d:%02d".format(minutes, seconds)

    val pulseScale by animateFloatAsState(
        targetValue = if (isRunning && remainingSeconds in 1..59) 1.05f else 1f,
        animationSpec = tween(600),
        label = "脉冲"
    )

    Box(
        modifier = Modifier
            .size(240.dp)
            .scale(pulseScale),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = 1f,
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 8.dp,
            color = Color.White.copy(alpha = 0.08f),
            trackColor = Color.Transparent
        )

        CircularProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 14.dp,
            color = progressColor,
            trackColor = Color.Transparent
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = timeText,
                color = TextWhite,
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isRunning) "专注中…" else "已就绪",
                color = TextGray,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ──────────────────────────────────────────────────────
// 时长选择器
// ──────────────────────────────────────────────────────

@Composable
private fun DurationSelector(selectedSeconds: Int, onSelect: (Int) -> Unit) {
    val options = listOf(
        15 * 60 to "15 分钟",
        25 * 60 to "25 分钟",
        45 * 60 to "45 分钟",
        60 * 60 to "60 分钟"
    )

    Text(
        text = "选择专注时长",
        color = TextGray,
        fontSize = 14.sp,
        modifier = Modifier.padding(bottom = 10.dp)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        options.forEach { (seconds, label) ->
            val isSelected = seconds == selectedSeconds

            FilterChip(
                selected = isSelected,
                onClick = { onSelect(seconds) },
                label = {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) TextWhite else TextGray
                    )
                },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TomatoRedDark.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

// ──────────────────────────────────────────────────────
// 控制按钮
// ──────────────────────────────────────────────────────

@Composable
private fun ControlButtons(
    isRunning: Boolean,
    remainingSeconds: Int,
    isAccessibilityEnabled: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    val context = LocalContext.current
    val isPaused = !isRunning && remainingSeconds > 0 &&
            remainingSeconds < TimerState.durationSeconds.value

    when {
        // 未开始
        !isRunning && remainingSeconds == TimerState.durationSeconds.value -> {
            Button(
                onClick = {
                    if (!isAccessibilityEnabled) {
                        // 不阻断，但提醒用户
                        Toast.makeText(
                            context,
                            "建议先开启无障碍服务，否则拦截功能不会生效",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    onStart()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TomatoRed),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "开始专注", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
        // 暂停中
        isPaused -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "继续", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TomatoRed),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "停止", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        // 运行中
        isRunning -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onPause,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGold),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Pause, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "暂停", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TomatoRed),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "停止", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────
// 拦截规则说明
// ──────────────────────────────────────────────────────

@Composable
private fun BlockRulesInfo() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "📋 拦截规则",
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            RuleItem("🔴", "抖音", "打开即拦截")
            RuleItem("🟡", "微信·朋友圈", "浏览朋友圈时拦截")
            RuleItem("🟡", "微信·视频号", "浏览视频号时拦截")
            RuleItem("🟡", "微信·发现页", "查看发现页时拦截")
            RuleItem("🟡", "微信·看一看", "浏览看一看时拦截")
        }
    }
}

@Composable
private fun RuleItem(emoji: String, app: String, desc: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emoji, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = app,
            color = TextWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(110.dp)
        )
        Text(text = desc, color = TextGray, fontSize = 13.sp)
    }
}

// ──────────────────────────────────────────────────────
// 无障碍服务检测工具
// ──────────────────────────────────────────────────────

/**
 * 检查本应用的无障碍服务是否已在系统设置中开启
 */
private fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
    val serviceName = "${context.packageName}/${MyAccessibilityService::class.java.name}"
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    return enabledServices.split(':').any { it.equals(serviceName, ignoreCase = true) }
        || enabledServices.equals(serviceName, ignoreCase = true)
}

// ──────────────────────────────────────────────────────
// 倒计时管理器
// ──────────────────────────────────────────────────────

/**
 * 后台倒计时协程管理器
 *
 * 每秒钟递减 [TimerState.remainingSeconds]。
 * 当剩余秒数归零时自动停止番茄钟并弹出完成 Toast。
 */
object CountdownManager {

    /** 倒计时协程的 Job，用于取消 */
    private var job: Job? = null

    /** 协程作用域 —— 绑定主线程 */
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * 开始倒计时
     *
     * 每 1000ms tick 一次。若已有倒计时在运行则忽略本次调用。
     *
     * @param context Android Context，用于弹出完成 Toast
     */
    fun start(context: android.content.Context) {
        // 已有倒计时在跑，不重复启动
        if (job?.isActive == true) return

        job = scope.launch {
            while (isActive) {
                delay(1000L)
                val remaining = TimerState.remainingSeconds.value

                if (remaining <= 1) {
                    // 倒计时结束 —— 归零并停止
                    TimerState.tick(0)
                    TimerState.stop()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "🎉 番茄钟完成！干得漂亮！", Toast.LENGTH_LONG).show()
                    }
                    break
                } else {
                    TimerState.tick(remaining - 1)
                }
            }
        }
    }

    /** 取消当前倒计时 */
    fun stop() {
        job?.cancel()
        job = null
    }
}
