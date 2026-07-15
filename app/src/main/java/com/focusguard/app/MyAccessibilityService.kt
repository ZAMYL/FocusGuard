package com.focusguard.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 无障碍拦截服务 2.0
 *
 * 专注模式下实时拦截以下应用：
 * — 直接拦截（打开即锁）：抖音、快手、小红书、微博、B站
 * — 精准拦截（扫节点树）：微信（仅拦截朋友圈/视频号/发现/看一看，允许正常办公沟通）
 *
 * 拦截仅在 [TimerState.isInFocusMode] 为 true 时生效。
 */
class MyAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "FocusGuard_Accessibility"

        /** 直接拦截的应用包名清单 */
        private val DIRECT_BLOCK_PACKAGES = setOf(
            "com.ss.android.ugc.aweme",      // 抖音
            "com.smile.gifmaker",            // 快手
            "com.xingin.xhs",                // 小红书
            "com.sina.weibo",                // 微博
            "tv.danmaku.bili",               // B站
            "com.zhiliaoapp.musically",      // TikTok
            "com.kuaishou.nebula",           // 快手极速版
            "com.ss.android.ugc.aweme.lite", // 抖音极速版
            "com.tencent.qqlive",            // 腾讯视频
            "com.youku.phone",               // 优酷
            "com.qiyi.video",                // 爱奇艺
            "com.hunantv.imgo.activity",     // 芒果TV
            "com.dragon.read",               // 番茄小说
            "com.zhangyue.iReader",          // 掌阅
            "com.tencent.mobileqq",          // QQ（纯娱乐聊天）
            "com.baidu.tieba",               // 贴吧
            "com.taobao.taobao",             // 淘宝
            "com.jingdong.app.mall",         // 京东
            "com.xunmeng.pinduoduo"          // 拼多多
        )

        /** 微信包名 — 精准扫描不直接拦截 */
        private const val WECHAT_PACKAGE = "com.tencent.mm"

        /** 微信中需要拦截的关键词 */
        private val WECHAT_BLOCK_KEYWORDS = setOf(
            "朋友圈", "视频号", "发现", "看一看",
            "直播", "游戏", "购物", "小程序"
        )

        /** 常见干扰 App 名（用于锁屏提示） */
        private val PACKAGE_DISPLAY_NAMES = mapOf(
            "com.ss.android.ugc.aweme" to "抖音",
            "com.smile.gifmaker" to "快手",
            "com.xingin.xhs" to "小红书",
            "com.sina.weibo" to "微博",
            "tv.danmaku.bili" to "B站",
            "com.zhiliaoapp.musically" to "TikTok",
            "com.kuaishou.nebula" to "快手极速版",
            "com.ss.android.ugc.aweme.lite" to "抖音极速版",
            "com.tencent.qqlive" to "腾讯视频",
            "com.youku.phone" to "优酷",
            "com.qiyi.video" to "爱奇艺",
            "com.hunantv.imgo.activity" to "芒果TV",
            "com.dragon.read" to "番茄小说",
            "com.zhangyue.iReader" to "掌阅",
            "com.tencent.mobileqq" to "QQ",
            "com.baidu.tieba" to "贴吧",
            "com.taobao.taobao" to "淘宝",
            "com.jingdong.app.mall" to "京东",
            "com.xunmeng.pinduoduo" to "拼多多"
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!TimerState.isInFocusMode()) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        Log.d(TAG, "检测到活跃窗口: $packageName")

        when {
            // 直接拦截清单 → 秒锁
            packageName in DIRECT_BLOCK_PACKAGES -> {
                val appName = PACKAGE_DISPLAY_NAMES[packageName] ?: packageName
                Log.w(TAG, "⚠️ 检测到 $appName，直接锁机")
                triggerLock(appName)
            }
            // 微信 → 精准扫描
            packageName == WECHAT_PACKAGE -> {
                val keyword = scanForBlockedContent()
                if (keyword != null) {
                    Log.w(TAG, "⚠️ 微信「$keyword」，触发锁机")
                    triggerLock("微信$keyword")
                }
            }
        }
    }

    override fun onInterrupt() = Unit
    override fun onServiceConnected() { super.onServiceConnected(); Log.d(TAG, "服务已连接") }
    override fun onDestroy() { super.onDestroy(); Log.d(TAG, "服务已销毁") }

    /** 递归扫描节点树，匹配拦截关键词 */
    private fun scanForBlockedContent(): String? {
        val root = rootInActiveWindow ?: return null
        return try { scanNode(root) } finally { root.recycle() }
    }

    private fun scanNode(node: AccessibilityNodeInfo): String? {
        val combined = "${node.text ?: ""}${node.contentDescription ?: ""}"
        for (keyword in WECHAT_BLOCK_KEYWORDS) {
            if (keyword in combined) return keyword
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = scanNode(child)
            child.recycle()
            if (result != null) return result
        }
        return null
    }

    private fun triggerLock(reason: String) {
        startActivity(Intent(this, LockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(LockActivity.EXTRA_BLOCK_REASON, reason)
        })
    }
}
