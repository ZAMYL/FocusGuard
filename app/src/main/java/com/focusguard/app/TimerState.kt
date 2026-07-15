package com.focusguard.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全局番茄钟状态单例
 *
 * 用途：在 Application 进程内共享番茄钟的运行状态，
 * 让 MyAccessibilityService 和各个 Activity 能同步访问。
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

    // ── 操作 ──────────────────────────────────────────

    fun setDuration(seconds: Int) {
        _durationSeconds.value = seconds
        _remainingSeconds.value = seconds
    }

    fun start() {
        _isRunning.value = true
    }

    fun pause() {
        _isRunning.value = false
    }

    fun resume() {
        _isRunning.value = true
    }

    fun stop() {
        _isRunning.value = false
        _remainingSeconds.value = _durationSeconds.value
    }

    fun tick(remaining: Int) {
        _remainingSeconds.value = remaining
    }

    /** 番茄钟是否处于运行中（供无障碍服务查询） */
    fun isTomatoTimerRunning(): Boolean = _isRunning.value
}
