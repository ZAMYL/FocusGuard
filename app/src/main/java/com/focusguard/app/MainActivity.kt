@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.focusguard.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.app.ui.theme.*
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * 专注卫士 2.0 主界面
 *
 * 极简三模块架构：
 * — 时间呼吸舱（Timer Cabin）：Canvas 呼吸圆盘 + 手势无极调时 + 一键启停
 * — 深海白噪音舱（Ambient Cabin）：三路混音滑块
 * — 个性化换肤（底部快捷入口）
 * — 拦截规则通过 ModalBottomSheet 呼出
 */
class MainActivity : ComponentActivity() {

    private var isInForeground = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AudioHelper.init(this)
        NotificationHelper.createChannel(this)

        setContent {
            val preset = loadThemePreset(this)
            var currentPreset by remember { mutableStateOf(preset) }
            FocusGuardTheme(preset = currentPreset) {
                MainScreen2(
                    onThemeChange = { newPreset ->
                        currentPreset = newPreset
                        saveThemePreset(this@MainActivity, newPreset)
                    }
                )
            }
        }
    }

    override fun onPause() {
        super.onPause(); isInForeground = false
        if (TimerState.isRunning.value)
            NotificationHelper.scheduleStaggeredNotifications(
                this, TimerState.remainingSeconds.value, TimerState.isBreakMode.value
            )
    }

    override fun onResume() {
        super.onResume(); isInForeground = true
        NotificationHelper.cancelAll(this)
    }

    override fun onDestroy() { super.onDestroy(); AudioHelper.release() }

    fun isAppInForeground(): Boolean = isInForeground
}

// ═══════════════════════════════════════════════════════
// 主界面 2.0
// ═══════════════════════════════════════════════════════

@Composable
private fun MainScreen2(onThemeChange: (ThemePreset) -> Unit) {
    val context = LocalContext.current
    val isRunning by TimerState.isRunning.collectAsState()
    val remainingSeconds by TimerState.remainingSeconds.collectAsState()
    val isBreakMode by TimerState.isBreakMode.collectAsState()
    val sessionCount by TimerState.sessionCount.collectAsState()

    var isAccessibilityEnabled by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }
    var showAmbient by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { isAccessibilityEnabled = checkAccessibility(context) }
    LaunchedEffect(isRunning) {
        while (true) { delay(1000); isAccessibilityEnabled = checkAccessibility(context) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            // ── 标题 ────────────────────────────────────
            Text("🍅 专注卫士", color = MaterialTheme.colorScheme.onBackground, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(
                when { isBreakMode -> "☕ 休息中 · 第${sessionCount}轮完成"
                    isRunning -> "🔒 专注中 · 第${sessionCount + 1}轮" else -> "呼吸，聚焦，开始" },
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 13.sp
            )

            Spacer(Modifier.height(30.dp))

            // ── 吸气圆盘 ────────────────────────────────
            CircularTimerDial(
                remainingSeconds = if (isRunning) remainingSeconds else TimerState.durationSeconds.value,
                totalSeconds = TimerState.durationSeconds.value,
                isRunning = isRunning,
                isBreakMode = isBreakMode,
                onDurationChange = { if (!isRunning) TimerState.setDuration(it) }
            )

            Spacer(Modifier.height(36.dp))

            // ── 主控制按钮 ──────────────────────────────
            MasterButton(
                isRunning = isRunning, isBreakMode = isBreakMode, isPaused = !isRunning &&
                    remainingSeconds > 0 && remainingSeconds < TimerState.durationSeconds.value,
                isAccessibilityEnabled = isAccessibilityEnabled,
                onStart = { AudioHelper.stopAlarm(); TimerState.start(); CountdownManager.start(context) },
                onPause = { AudioHelper.stopAlarm(); TimerState.pause(); CountdownManager.stop() },
                onResume = { AudioHelper.stopAlarm(); TimerState.resume(); CountdownManager.start(context) },
                onStop = { AudioHelper.stopAlarm(); TimerState.stop(); TimerState.resetCycle(); CountdownManager.stop() },
                onSkipBreak = { AudioHelper.stopAlarm(); TimerState.completeSession(); CountdownManager.start(context) }
            )

            Spacer(Modifier.height(24.dp))

            // ── AI 呼吸语录卡片 ──────────────────────────
            QuoteCard()

            Spacer(Modifier.height(12.dp))

            // ── 今日专注极简看板 ──────────────────────────
            DailyStats(sessionCount = sessionCount)

            Spacer(Modifier.weight(1f))

            // ── 底部快捷栏 ──────────────────────────────
            BottomQuickBar(
                onRules = { showRules = true },
                onAmbient = { showAmbient = true },
                onTheme = { showTheme = true },
                isAccessibilityEnabled = isAccessibilityEnabled
            )

            Spacer(Modifier.height(20.dp))
        }

        // ══════════════════════════════════════════════
        // 白噪音 BottomSheet
        // ══════════════════════════════════════════════
        if (showAmbient) {
            ModalBottomSheet(onDismissRequest = { showAmbient = false }) {
                AmbientSheet()
            }
        }

        // ══════════════════════════════════════════════
        // 换肤 BottomSheet
        // ══════════════════════════════════════════════
        if (showTheme) {
            ModalBottomSheet(onDismissRequest = { showTheme = false }) {
                ThemeSheet(onSelect = { onThemeChange(it); showTheme = false })
            }
        }

        // ══════════════════════════════════════════════
        // 拦截规则 —— ModalBottomSheet
        // ══════════════════════════════════════════════
        if (showRules) {
            ModalBottomSheet(onDismissRequest = { showRules = false }) {
                BlockRulesSheet()
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// 圆形时间调节轮盘 — 量化吸附 + 物理质感
// ═══════════════════════════════════════════════════════

/** 最大可调分钟数 */
private const val MAX_MINUTES = 120
/** 量化步长（分钟） */
private const val SNAP_STEP_MINUTES = 1

@Composable
private fun CircularTimerDial(
    remainingSeconds: Int, totalSeconds: Int, isRunning: Boolean, isBreakMode: Boolean,
    onDurationChange: (Int) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val view = androidx.compose.ui.platform.LocalView.current
    val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds else 0f
    val currentMinutes = totalSeconds / 60

    // ── 呼吸动画 ──────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.02f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "breathe"
    )

    // ── 拖拽激活状态 ──────────────────────────────
    var isDragging by remember { mutableStateOf(false) }

    // 圆环粗细动画：闲置 8dp ↔ 拖拽 14dp
    val strokeWidthAnim by animateFloatAsState(
        targetValue = if (isDragging) 14f else 8f,
        animationSpec = tween(250), label = "strokeWidth"
    )
    // 圆环色饱和度
    val ringAlphaAnim by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0.85f,
        animationSpec = tween(250), label = "ringAlpha"
    )

    // ── 数字弹性动画 ──────────────────────────────
    var prevMinute by remember { mutableStateOf(currentMinutes) }
    var springScale by remember { mutableStateOf(1f) }
    val animatableScale = remember { Animatable(1f) }

    LaunchedEffect(currentMinutes) {
        if (currentMinutes != prevMinute) {
            prevMinute = currentMinutes
            // Spring 弹跳：1.0 → 1.15 → 1.0
            animatableScale.animateTo(1.15f, animationSpec = spring(dampingRatio = 0.35f, stiffness = 600f))
            animatableScale.animateTo(1.0f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f))
        }
    }

    // ── 圆环色 ────────────────────────────────────
    val ringColor = when {
        isBreakMode -> Color(0xFFF39C12)
        !isRunning && !isDragging -> colors.outline
        progress > 0.5f -> colors.primary
        progress > 0.2f -> Color(0xFFF39C12)
        else -> Color(0xFFE74C3C)
    }

    val animatedProgress by animateFloatAsState(progress, tween(1000), label = "pp")
    val displayScale by animateFloatAsState(if (isRunning) breatheScale else 1f, tween(800), label = "ds")

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeText = "%02d:%02d".format(minutes, seconds)
    val circleSize = 260.dp

    // ── 辅助文字 ──────────────────────────────────
    val labelAlpha by animateFloatAsState(
        targetValue = if (isDragging) 0.3f else 0.5f, animationSpec = tween(200), label = "labelFade"
    )
    val dragHint = if (isDragging) "⚡ 松开吸附到 ${currentMinutes} 分钟" else "滑动调时 ≈"

    Box(
        modifier = Modifier.size(circleSize).scale(displayScale)
            .then(if (!isRunning) Modifier.pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, _ ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val touch = change.position - center
                        val angle = atan2(touch.y.toDouble(), touch.x.toDouble()).toFloat()
                        // 角度 → 分钟（0~MAX_MINUTES）
                        val rawDeg = ((Math.toDegrees(angle.toDouble()) + 360 + 90) % 360).toFloat()
                        val rawMinutes = rawDeg / 360f * MAX_MINUTES
                        // 量化吸附到最近的 SNAP_STEP_MINUTES
                        val snapped = (rawMinutes / SNAP_STEP_MINUTES).roundToInt() * SNAP_STEP_MINUTES
                        val clamped = snapped.coerceIn(1, MAX_MINUTES)

                        // 触觉反馈：跨越刻度时振动
                        val prevClamped = (prevMinute.toFloat() / SNAP_STEP_MINUTES).roundToInt() * SNAP_STEP_MINUTES
                        if (clamped != prevClamped) {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                        }

                        onDurationChange(clamped * 60)
                        change.consume()
                    }
                )
            } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = strokeWidthAnim * density  // dp → px
            val arcSz = Size(size.width - sw, size.height - sw)
            val tl = Offset(sw / 2f, sw / 2f)

            // 背景弧
            drawArc(
                Color.White.copy(alpha = 0.06f), 0f, 360f, false,
                topLeft = tl, size = arcSz,
                style = Stroke(sw, cap = StrokeCap.Round)
            )

            // 进度弧
            drawArc(
                ringColor.copy(alpha = ringAlphaAnim), -90f, animatedProgress * 360f, false,
                topLeft = tl, size = arcSz,
                style = Stroke(sw, cap = StrokeCap.Round)
            )

            // 拖拽时的刻度点（每 15 分钟一个）
            if (isDragging) {
                for (i in 0 until MAX_MINUTES step 15) {
                    val tickAngle = Math.toRadians((i.toDouble() / MAX_MINUTES * 360 - 90))
                    val innerR = (size.width - sw) / 2f * 0.82f
                    val outerR = (size.width - sw) / 2f * 0.91f
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    drawLine(
                        Color.White.copy(alpha = 0.15f),
                        Offset(cx + innerR * cos(tickAngle).toFloat(), cy + innerR * sin(tickAngle).toFloat()),
                        Offset(cx + outerR * cos(tickAngle).toFloat(), cy + outerR * sin(tickAngle).toFloat()),
                        strokeWidth = 2f
                    )
                }
            }

            // 呼吸外发光
            if (isRunning) {
                drawCircle(
                    ringColor.copy(alpha = 0.08f * (breatheScale - 0.96f) * 50f),
                    radius = size.width / 2f + 20f * (breatheScale - 0.98f) * 50f
                )
            }
        }

        // ── 中心文字 ────────────────────────────────
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                timeText, color = colors.onBackground,
                fontSize = 52.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.scale(animatableScale.value)
            )
            Text(
                when { isBreakMode -> "☕ 休息" ; isRunning -> "专注中" ; else -> dragHint },
                color = colors.onSurface.copy(alpha = labelAlpha), fontSize = 13.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// 主控制按钮
// ═══════════════════════════════════════════════════════

@Composable
private fun MasterButton(
    isRunning: Boolean, isBreakMode: Boolean, isPaused: Boolean,
    isAccessibilityEnabled: Boolean,
    onStart: () -> Unit, onPause: () -> Unit, onResume: () -> Unit,
    onStop: () -> Unit, onSkipBreak: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current

    when {
        isBreakMode && isRunning -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onSkipBreak, Modifier.height(52.dp),
                shape = RoundedCornerShape(16.dp)) { Text("跳过休息") }
            OutlinedButton(onClick = onStop, Modifier.height(52.dp),
                shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE74C3C))) { Text("结束") }
        }
        isPaused -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onResume, Modifier.height(56.dp).weight(1f),
                shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = colors.primary)) { Text("继续", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            OutlinedButton(onClick = onStop, Modifier.height(56.dp).weight(1f),
                shape = RoundedCornerShape(18.dp)) { Text("结束", fontSize = 16.sp) }
        }
        isRunning -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onPause, Modifier.height(56.dp).weight(1f),
                shape = RoundedCornerShape(18.dp)) { Text("暂停", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            OutlinedButton(onClick = onStop, Modifier.height(56.dp).weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE74C3C))) { Text("放弃", fontSize = 16.sp) }
        }
        else -> Button(onClick = {
                if (!isAccessibilityEnabled)
                    Toast.makeText(context, "建议先开启无障碍服务", Toast.LENGTH_SHORT).show()
                onStart()
            }, Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)) {
                Text("开始专注", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
    }
}

// ═══════════════════════════════════════════════════════
// 底部快捷栏
// ═══════════════════════════════════════════════════════

@Composable
private fun BottomQuickBar(
    onRules: () -> Unit, onAmbient: () -> Unit, onTheme: () -> Unit,
    isAccessibilityEnabled: Boolean
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
    ) {
        QuickIcon(Icons.Filled.Shield, if (isAccessibilityEnabled) "已开启" else "未开启", onClick = onRules)
        QuickIcon(Icons.Filled.MusicNote, "白噪音", onClick = onAmbient)
        QuickIcon(Icons.Filled.Palette, "换肤", onClick = onTheme)
    }
}

@Composable
private fun QuickIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick).padding(12.dp)) {
        Icon(icon, label, tint = colors.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
        Text(label, fontSize = 10.sp, color = colors.onSurface.copy(alpha = 0.5f))
    }
}

// ═══════════════════════════════════════════════════════
// AI 呼吸语录卡片
// ═══════════════════════════════════════════════════════

@Composable
private fun QuoteCard() {
    val colors = MaterialTheme.colorScheme

    // 呼吸动画：alpha 在 0.7 ~ 1.0 之间缓慢循环
    val infinite = rememberInfiniteTransition(label = "quoteBreathe")
    val alpha by infinite.animateFloat(
        initialValue = 0.7f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "quoteAlpha"
    )

    val quote = remember { LocalQuotes.randomBlockQuote() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(Modifier.padding(20.dp)) {
            // 背景巨型引号
            Text(
                text = "❝", fontSize = 72.sp,
                color = colors.onSurface.copy(alpha = 0.04f),
                modifier = Modifier.offset(x = (-8).dp, y = (-20).dp)
            )

            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = quote, color = colors.onSurface.copy(alpha = alpha),
                    fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "—— 专注卫士 · 每日警醒",
                    color = colors.onSurface.copy(alpha = 0.25f), fontSize = 11.sp
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// 今日专注极简看板
// ═══════════════════════════════════════════════════════

@Composable
private fun DailyStats(sessionCount: Int) {
    val colors = MaterialTheme.colorScheme
    val totalMinutes = sessionCount * 25  // 每个番茄 25 分钟
    val goalMinutes = 120                 // 每日目标 2 小时
    val progress = (totalMinutes.toFloat() / goalMinutes).coerceIn(0f, 1f)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "今日已专注 $totalMinutes 分钟 · 达成 $sessionCount 个番茄",
                color = colors.onSurface.copy(alpha = 0.4f), fontSize = 12.sp
            )
            Spacer(Modifier.height(4.dp))
            // 极细进度条
            LinearProgressIndicator(
                progress = progress, modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                color = colors.primary.copy(alpha = 0.5f),
                trackColor = colors.onSurface.copy(alpha = 0.06f)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// 白噪音 BottomSheet — 2x2 卡片
// ═══════════════════════════════════════════════════════

@Composable
private fun AmbientSheet() {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    var playingTrack by remember { mutableStateOf(WhiteNoiseManager.Track.NONE) }
    var masterVolume by remember { mutableFloatStateOf(0.5f) }

    Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
        Text("🌊 深海白噪音舱", color = colors.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("选一个声音，沉浸专注", color = colors.onSurface.copy(alpha = 0.4f), fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))

        // 2x2 网格
        val tracks = listOf(
            WhiteNoiseManager.Track.RAIN to "🌧️",
            WhiteNoiseManager.Track.FIRE to "🔥",
            WhiteNoiseManager.Track.WAVE to "🌊",
            WhiteNoiseManager.Track.FOREST to "🌲"
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (row in listOf(0, 2)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (i in row..row + 1) {
                        val (track, emoji) = tracks[i]
                        val isActive = playingTrack == track
                        NoiseCard(
                            emoji = emoji, label = track.label, isActive = isActive,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                WhiteNoiseManager.toggle(context, track) { newTrack, isPlaying ->
                                    playingTrack = if (isPlaying) newTrack else WhiteNoiseManager.Track.NONE
                                }
                                WhiteNoiseManager.setVolume(masterVolume)
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // 全局音量
        Text("🔊 全局音量", fontSize = 13.sp, color = colors.onSurface.copy(alpha = 0.5f))
        Slider(
            value = masterVolume,
            onValueChange = { masterVolume = it; WhiteNoiseManager.setVolume(it) },
            colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun NoiseCard(emoji: String, label: String, isActive: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val bgColor = if (isActive) Color(0xFFF5E6D3) else colors.surface.copy(alpha = 0.6f)
    val borderColor = if (isActive) Color(0xFFD4A574) else colors.onSurface.copy(alpha = 0.08f)

    Card(
        onClick = onClick, modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(Modifier.fillMaxSize().padding(12.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Text(emoji, fontSize = 28.sp)
                Spacer(Modifier.height(6.dp))
                Text(label, fontSize = 13.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) Color(0xFF6B4226) else colors.onSurface.copy(alpha = 0.7f))
            }
            // 右上角呼吸灯
            if (isActive) {
                Box(
                    Modifier.align(Alignment.TopEnd).size(10.dp)
                        .background(Color(0xFF2ECC71), CircleShape)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ═══════════════════════════════════════════════════════
// 换肤 BottomSheet
// ═══════════════════════════════════════════════════════

@Composable
private fun ThemeSheet(onSelect: (ThemePreset) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val current = LocalThemePreset.current

    Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
        Text("🎨 选择主题", color = colors.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("换个色彩，换个心情", color = colors.onSurface.copy(alpha = 0.4f), fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))

        ThemeOptionRow("🌌 极光暗影", "深邃灰蓝 + 极光绿 — 深夜深度思考", ThemePreset.AURORA, current, onSelect)
        ThemeOptionRow("🏖️ 浅砂暮霭", "奶咖柔白 + 治愈平复 — 安抚自律焦虑", ThemePreset.WARM_SAND, current, onSelect)
        ThemeOptionRow("🌙 暮紫静夜", "暗紫底色 + 玫瑰粉金 — 梦幻深邃", ThemePreset.TWILIGHT, current, onSelect)

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ThemeOptionRow(
    title: String, desc: String, preset: ThemePreset,
    current: ThemePreset, onSelect: (ThemePreset) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isSelected = current == preset

    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onSelect(preset) },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) colors.primary.copy(alpha = 0.12f) else colors.onSurface.copy(alpha = 0.03f)
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) colors.primary else colors.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(desc, fontSize = 12.sp, color = colors.onSurface.copy(alpha = 0.45f))
            }
            if (isSelected) {
                Icon(Icons.Filled.Check, "已选", tint = colors.primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// 拦截规则 ModalBottomSheet
// ═══════════════════════════════════════════════════════

@Composable
private fun BlockRulesSheet() {
    Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text("📋 拦截规则", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(16.dp))
        SectionTitle("🔴 打开即拦截")
        BlockAppItem("抖音 / TikTok", "com.ss.android.ugc.aweme")
        BlockAppItem("快手", "com.smile.gifmaker")
        BlockAppItem("小红书", "com.xingin.xhs")
        BlockAppItem("微博", "com.sina.weibo")
        BlockAppItem("B站", "tv.danmaku.bili")
        BlockAppItem("腾讯视频 / 优酷 / 爱奇艺 / 芒果TV", "")
        BlockAppItem("番茄小说 / 掌阅", "")
        BlockAppItem("淘宝 / 京东 / 拼多多", "")
        BlockAppItem("QQ / 贴吧", "")
        Spacer(Modifier.height(12.dp))
        SectionTitle("🟡 微信精准拦截（仅拦截娱乐节点）")
        BlockAppItem("朋友圈 / 视频号 / 看一看", "")
        BlockAppItem("直播 / 游戏 / 购物 / 小程序", "")
        Text("⚠️ 仅专注模式生效，休息期间全部放行", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun BlockAppItem(name: String, pkg: String) {
    Row(Modifier.padding(vertical = 2.dp, horizontal = 8.dp)) {
        Text("· $name", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    }
}

// ═══════════════════════════════════════════════════════
// 工具函数
// ═══════════════════════════════════════════════════════

private fun checkAccessibility(context: android.content.Context): Boolean {
    val sn = "${context.packageName}/${MyAccessibilityService::class.java.name}"
    val svc = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
    return svc.split(':').any { it.equals(sn, ignoreCase = true) } || svc.equals(sn, ignoreCase = true)
}

// ═══════════════════════════════════════════════════════
// 倒计时管理器
// ═══════════════════════════════════════════════════════

object CountdownManager {
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun start(context: android.content.Context) {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                delay(1000L)
                val remaining = TimerState.remainingSeconds.value
                if (remaining <= 1) {
                    TimerState.tick(0)
                    TimerState.completeSession()
                    withContext(Dispatchers.Main) {
                        AudioHelper.playComplete(context)
                        Toast.makeText(context, if (TimerState.isBreakMode.value) "🍅 专注完成！休息一下" else "💪 休息结束！继续专注", Toast.LENGTH_SHORT).show()
                    }
                    if (!TimerState.isRunning.value) break
                    continue
                }
                if (remaining <= 5) withContext(Dispatchers.Main) { AudioHelper.playTick() }
                TimerState.tick(remaining - 1)
            }
        }
    }
    fun stop() { job?.cancel(); job = null }
}
