package com.focusguard.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 无障碍拦截服务
 *
 * 实时监控活跃应用窗口，在番茄钟运行期间拦截：
 * - 抖音（包名: com.ss.android.ugc.aweme）→ 直接触发锁机
 * - 微信（包名: com.tencent.mm）→ 扫描视图节点树，若发现"朋友圈"、
 *   "视频号"、"发现"、"看一看"等关键词则触发锁机
 *
 * 拦截逻辑仅在 [TimerState.isTomatoTimerRunning] 为 true 时生效。
 */
class MyAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "FocusGuard_Accessibility"

        /** 抖音包名 */
        private const val DOUYIN_PACKAGE = "com.ss.android.ugc.aweme"

        /** 微信包名 */
        private const val WECHAT_PACKAGE = "com.tencent.mm"

        /** 微信中需要拦截的关键词 */
        private val WECHAT_BLOCK_KEYWORDS = setOf(
            "朋友圈",
            "视频号",
            "发现",
            "看一看"
        )
    }

    // ──────────────────────────────────────────────────
    // 生命周期
    // ──────────────────────────────────────────────────

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // 仅在番茄钟运行时执行拦截逻辑
        if (!TimerState.isTomatoTimerRunning()) {
            return
        }

        // 只关注窗口状态变化事件
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val packageName = event.packageName?.toString() ?: return
        Log.d(TAG, "检测到活跃窗口包名: $packageName")

        when {
            // 情况1：抖音 → 直接锁机
            packageName == DOUYIN_PACKAGE -> {
                Log.w(TAG, "⚠️ 检测到抖音，触发锁机！")
                triggerLock("抖音")
            }
            // 情况2：微信 → 扫描节点树
            packageName == WECHAT_PACKAGE -> {
                val matchedKeyword = scanForBlockedContent()
                if (matchedKeyword != null) {
                    Log.w(TAG, "⚠️ 检测到微信「$matchedKeyword」，触发锁机！")
                    triggerLock("微信$matchedKeyword")
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "无障碍服务被中断")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "专注卫士无障碍服务已连接")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "专注卫士无障碍服务已销毁")
    }

    // ──────────────────────────────────────────────────
    // 核心拦截方法
    // ──────────────────────────────────────────────────

    /**
     * 递归扫描当前窗口的视图节点树
     *
     * 遍历所有节点，检查其文本内容是否匹配拦截关键词。
     * 使用递归 + 后序遍历，确保不遗漏任何子节点。
     *
     * @return 匹配到的关键词，未匹配则返回 null
     */
    private fun scanForBlockedContent(): String? {
        val rootNode = rootInActiveWindow ?: return null
        return try {
            scanNodeRecursive(rootNode)
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * 递归扫描单个节点及其所有子节点
     *
     * @param node 当前节点
     * @return 匹配到的关键词，未匹配则返回 null
     */
    private fun scanNodeRecursive(node: AccessibilityNodeInfo): String? {
        // 检查当前节点的文本内容
        val text = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""

        for (keyword in WECHAT_BLOCK_KEYWORDS) {
            if (keyword in text || keyword in contentDesc) {
                return keyword
            }
        }

        // 递归检查子节点
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = scanNodeRecursive(child)
            child.recycle()
            if (result != null) return result
        }

        return null
    }

    /**
     * 触发锁机页面
     *
     * 启动 [LockActivity] 并传入违规原因作为 Intent extra。
     *
     * @param reason 违规原因（如"抖音"、"微信朋友圈"）
     */
    private fun triggerLock(reason: String) {
        val intent = Intent(this, LockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(LockActivity.EXTRA_BLOCK_REASON, reason)
        }
        startActivity(intent)
    }
}
