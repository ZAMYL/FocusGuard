package com.focusguard.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * 后台通知辅助类
 *
 * 当番茄钟在后台运行时，通过三阶段递进式通知提醒用户：
 * — 第 1 阶段（10 秒后）：轻提醒
 * — 第 2 阶段（30 秒后）：中等提醒
 * — 第 3 阶段（300 秒 / 5 分钟后）：强提醒
 *
 * 灵感来源：iPomodoro 的 WidgetsBindingObserver 生命周期感知通知
 */
object NotificationHelper {

    private const val CHANNEL_ID = "focusguard_timer"
    private const val CHANNEL_NAME = "番茄钟提醒"
    private const val NOTIFY_ID_BASE = 1000

    /** 三阶段递进间隔（秒） */
    private val STAGES = listOf(10, 30, 300)

    /** 各阶段提醒文案 */
    private val STAGE_MESSAGES = listOf(
        "⏰ 番茄钟还在运行中，别走神！",
        "⚠️ 你的专注时间还在继续，快回来！",
        "🚨 已经离开很久了！番茄钟在等你回来完成它！"
    )

    /**
     * 创建通知渠道（Android 8.0+ 必须）
     */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "番茄钟运行期间的后台提醒"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * 发送三阶段递进通知
     *
     * @param context 上下文
     * @param remainingSeconds 当前剩余秒数
     * @param isBreakMode 是否为休息模式
     */
    fun scheduleStaggeredNotifications(
        context: Context,
        remainingSeconds: Int,
        isBreakMode: Boolean
    ) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val minutes = remainingSeconds / 60
        val modeText = if (isBreakMode) "休息" else "专注"

        STAGES.forEachIndexed { index, delaySeconds ->
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, NOTIFY_ID_BASE + index, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("🍅 专注卫士 — ${modeText}中")
                .setContentText(STAGE_MESSAGES[index])
                .setSubText("剩余时间约 $minutes 分钟")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            // 使用延迟发送模拟递进通知（实际发出一条，后续在 CountdownManager 中更新）
            android.os.Handler(context.mainLooper).postDelayed({
                manager.notify(NOTIFY_ID_BASE + index, notification)
            }, delaySeconds * 1000L)
        }
    }

    /**
     * 清除所有计时器通知
     */
    fun cancelAll(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        STAGES.indices.forEach { index ->
            manager.cancel(NOTIFY_ID_BASE + index)
        }
    }

    /**
     * 立即发送一条"计时完成"通知
     */
    fun showCompletionNotification(context: Context, isBreakMode: Boolean) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 9999, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val msg = if (isBreakMode) "休息时间结束，该继续专注了！" else "🍅 番茄钟完成！干得漂亮！"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("专注卫士")
            .setContentText(msg)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(9999, notification)
    }
}
