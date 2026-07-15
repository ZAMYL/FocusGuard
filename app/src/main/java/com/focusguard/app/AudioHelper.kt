package com.focusguard.app

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * 轻量级音频辅助类
 *
 * — 滴答声：ToneGenerator 短促 Beep（零资源文件）
 * — 闹钟声：系统默认闹钟铃声，播放 2.5 秒后自动停止
 * — 振动：Vibrator 100ms 短震
 */
object AudioHelper {

    private var toneGenerator: ToneGenerator? = null
    private var currentRingtone: Ringtone? = null
    private val handler = Handler(Looper.getMainLooper())
    private var stopRunnable: Runnable? = null
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        toneGenerator = ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 50)
        isInitialized = true
    }

    /** 滴答声（80ms 短促 Beep） */
    fun playTick() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
    }

    /** 系统闹钟铃声，2.5 秒后自动停止 */
    fun playAlarm(context: Context) {
        stopAlarm() // 先停掉上一段
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            currentRingtone = RingtoneManager.getRingtone(context, uri).also { ringtone ->
                ringtone.play()
            }
            // 2.5 秒后自动停止，防止无限循环
            stopRunnable = Runnable { stopAlarm() }
            handler.postDelayed(stopRunnable!!, 2500L)
        } catch (e: Exception) {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
        }
    }

    /** 停止闹钟 */
    fun stopAlarm() {
        stopRunnable?.let { handler.removeCallbacks(it) }
        stopRunnable = null
        currentRingtone?.stop()
        currentRingtone = null
    }

    /** 闹钟 + 振动 */
    fun playComplete(context: Context) {
        playAlarm(context)
        vibrate(context)
    }

    /** 100ms 短振动 */
    fun vibrate(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }

    fun release() {
        stopAlarm()
        toneGenerator?.release()
        toneGenerator = null
        isInitialized = false
    }
}
