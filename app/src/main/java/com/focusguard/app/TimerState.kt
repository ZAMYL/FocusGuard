package com.focusguard.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全局番茄钟状态单例
 *
 * 用途：在 Application 进程内共享番茄钟的运行状态，
 * 让 MyAccessibilityService 和各个 Activity 能同步访问。
 *
 * 现已支持番茄工作法自动循环：专注 → 短休息 → 专注 → ... → 长休息
 */
object TimerState {

    /** 番茄钟是否正在运行 */
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    /** 当次番茄钟的目标时长（秒），默认 25 分钟 = 1500 秒 */
    private val _durationSeconds = MutableStateFlow(25 * 60)
    val durationSeconds: StateFlow<Int> = _durationSeconds.asStateFlow()

    /** 番茄钟剩余秒数 */
    private val _remainingSeconds = MutableStateFlow(25 * 60)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    // ── 番茄工作法循环状态 ─────────────────────────

    /** 当前是否为休息模式（false = 专注中，true = 休息中） */
    private val _isBreakMode = MutableStateFlow(false)
    val isBreakMode: StateFlow<Boolean> = _isBreakMode.asStateFlow()

    /** 已完成专注次数（用于判定是短休息还是长休息） */
    private val _sessionCount = MutableStateFlow(0)
    val sessionCount: StateFlow<Int> = _sessionCount.asStateFlow()

    /** 短休息时长（秒），默认 5 分钟 */
    private val _shortBreakSeconds = MutableStateFlow(5 * 60)
    val shortBreakSeconds: StateFlow<Int> = _shortBreakSeconds.asStateFlow()

    /** 长休息时长（秒），默认 15 分钟 */
    private val _longBreakSeconds = MutableStateFlow(15 * 60)
    val longBreakSeconds: StateFlow<Int> = _longBreakSeconds.asStateFlow()

    /** 长休息触发间隔（完成几个番茄后触发长休息），默认 4 */
    private val _longBreakInterval = MutableStateFlow(4)
    val longBreakInterval: StateFlow<Int> = _longBreakInterval.asStateFlow()

    // ── 操作 ──────────────────────────────────────────

    fun setDuration(seconds: Int) {
        _durationSeconds.value = seconds
        _remainingSeconds.value = seconds
    }

    fun start() {
        _isRunning.value = true
        _isBreakMode.value = false
    }

    fun pause() {
        _isRunning.value = false
    }

    fun resume() {
        _isRunning.value = true
    }

    fun stop() {
        _isRunning.value = false
        _isBreakMode.value = false
        _sessionCount.value = 0
        _remainingSeconds.value = _durationSeconds.value
    }

    fun tick(remaining: Int) {
        _remainingSeconds.value = remaining
    }

    /**
     * 完成当前时段，自动切换到下一阶段
     * — 专注完成 → 判定短/长休息
     * — 休息完成 → 下一轮专注
     */
    fun completeSession(): Boolean {
        return if (_isBreakMode.value) {
            // 休息结束 → 开始下一轮专注
            _isBreakMode.value = false
            _remainingSeconds.value = _durationSeconds.value
            false // 返回 false = 进入专注模式
        } else {
            // 专注结束 → 进入休息
            _sessionCount.value += 1
            _isBreakMode.value = true
            val isLongBreak = _sessionCount.value % _longBreakInterval.value == 0
            _remainingSeconds.value = if (isLongBreak) _longBreakSeconds.value else _shortBreakSeconds.value
            true // 返回 true = 进入休息模式
        }
    }

    /** 重置循环状态（完全停止番茄钟时调用） */
    fun resetCycle() {
        _isBreakMode.value = false
        _sessionCount.value = 0
    }

    /** 番茄钟是否处于运行中（供无障碍服务查询） */
    fun isTomatoTimerRunning(): Boolean = _isRunning.value

    /** 当前是否在专注模式（非休息）且运行中 — 拦截服务据此判定是否阻断应用 */
    fun isInFocusMode(): Boolean = _isRunning.value && !_isBreakMode.value
}
