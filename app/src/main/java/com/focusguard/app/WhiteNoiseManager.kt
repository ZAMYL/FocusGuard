package com.focusguard.app

import android.content.Context
import android.media.MediaPlayer
import android.widget.Toast

/**
 * 白噪音音频管理器（单例）
 *
 * 单选互斥播放逻辑：
 * — 点击卡片 A → 释放 B → 循环播放 A
 * — 再次点击 A → 暂停
 * — 全局音量控制
 *
 * 若 res/raw 中缺少对应文件，自动 Toast 提示并防止闪退。
 */
object WhiteNoiseManager {

    /** 音轨枚举 */
    enum class Track(val resId: Int, val label: String) {
        RAIN(R.raw.rain, "深山夜雨"),
        FIRE(R.raw.fire, "冬夜篝火"),
        WAVE(R.raw.wave, "治愈海浪"),
        FOREST(R.raw.forest, "晨曦森林"),
        NONE(0, "无")
    }

    private var mediaPlayer: MediaPlayer? = null
    private var currentTrack: Track = Track.NONE
    private var volume = 0.5f

    /** 播放指定音轨（单选互斥），已在播放中则暂停 */
    fun toggle(context: Context, track: Track, onStateChange: (Track, Boolean) -> Unit) {
        if (track == Track.NONE) return

        if (track == currentTrack) {
            // 再次点击 → 暂停
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                onStateChange(Track.NONE, false)
            } else {
                // 已暂停 → 恢复
                mediaPlayer?.start()
                onStateChange(track, true)
            }
            return
        }

        // 不同的音轨 → 释放旧资源
        release()

        try {
            mediaPlayer = MediaPlayer.create(context, track.resId)
            if (mediaPlayer == null) {
                throw IllegalStateException("资源加载失败: ${track.label}")
            }
            mediaPlayer?.apply {
                isLooping = true
                setVolume(volume, volume)
                start()
            }
            currentTrack = track
            onStateChange(track, true)
        } catch (e: Exception) {
            release()
            currentTrack = Track.NONE
            Toast.makeText(
                context,
                "请在 res/raw 文件夹中放置 ${track.label} 的音频文件",
                Toast.LENGTH_SHORT
            ).show()
            onStateChange(Track.NONE, false)
        }
    }

    /** 停止当前播放 */
    fun stop() {
        if (mediaPlayer?.isPlaying == true) mediaPlayer?.pause()
    }

    /** 设置全局音量 (0f ~ 1f) */
    fun setVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(volume, volume)
    }

    /** 释放 MediaPlayer */
    fun release() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        currentTrack = Track.NONE
    }

    /** 当前播放的音轨 */
    fun currentTrack(): Track = currentTrack
}
