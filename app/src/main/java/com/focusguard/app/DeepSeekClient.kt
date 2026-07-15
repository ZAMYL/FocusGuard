package com.focusguard.app

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * DeepSeek V4 API 轻量级客户端
 *
 * 使用 OkHttp 同步网络请求 + Gson 解析 JSON 响应，
 * 在 [Dispatchers.IO] 协程上下文中执行，避免阻塞主线程。
 *
 * 使用方式：
 * ```
 * val text = DeepSeekClient.fetchMotivation("抖音")
 * // 返回励志文字，或兜底字符串
 * ```
 */
object DeepSeekClient {

    private const val TAG = "DeepSeekClient"
    private const val API_URL = "https://api.deepseek.com/v1/chat/completions"

    /** JSON MIME 类型 */
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    /** 兜底字符串 —— 从本地语录库随机选取（网络失败时使用） */
    private fun fallbackMessage(): String = LocalQuotes.randomBlockQuote()

    /** Gson 解析实例 */
    private val gson = Gson()

    /** OkHttp 客户端 —— 连接/读/写超时各 10 秒 */
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    // ──────────────────────────────────────────────────
    // 公开方法
    // ──────────────────────────────────────────────────

    /**
     * 向 DeepSeek 请求一句毒舌警醒文字
     *
     * @param reason 触发拦截的原因（如"抖音"、"微信朋友圈"）
     * @return 中文警醒字符串，失败时返回兜底文字
     */
    suspend fun fetchMotivation(reason: String): String = withContext(Dispatchers.IO) {
        val prompt = buildPrompt(reason)
        val requestBody = buildRequestBody(prompt)

        Log.d(TAG, "正在请求 DeepSeek 警醒语，触发原因: $reason")

        try {
            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer ${BuildConfig.DEEPSEEK_API_KEY}")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "API 返回非成功状态码: ${response.code}")
                return@withContext fallbackMessage()
            }

            val bodyString = response.body?.string()
            if (bodyString.isNullOrBlank()) {
                Log.e(TAG, "API 返回空响应体")
                return@withContext fallbackMessage()
            }

            val parsed = gson.fromJson(bodyString, DeepSeekResponse::class.java)
            val content = parsed.choices?.firstOrNull()?.message?.content

            if (content.isNullOrBlank()) {
                Log.e(TAG, "解析后内容为空")
                fallbackMessage()
            } else {
                Log.d(TAG, "成功获取警醒语: $content")
                content.trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "请求 DeepSeek 失败: ${e.message}", e)
            fallbackMessage()
        }
    }

    // ──────────────────────────────────────────────────
    // 私有方法
    // ──────────────────────────────────────────────────

    /**
     * 构造中文 Prompt
     */
    private fun buildPrompt(reason: String): String {
        return "你是一个说话一针见血、幽默且极度犀利的毒舌自律教练。" +
            "当前用户在番茄钟专注期间试图刷${reason}，" +
            "请针对这个行为写一句20字以内的中文'毒舌警醒话'，逼他立刻放下手机回去干活。" +
            "要痛，但好笑，具有警示作用。"
    }

    /**
     * 构造 JSON 请求体
     */
    private fun buildRequestBody(prompt: String): okhttp3.RequestBody {
        val request = DeepSeekRequest(
            model = "deepseek-chat",
            messages = listOf(
                ChatMessage(role = "system", content = "你是一个严厉的自律教练，说话狠辣毒舌但充满激励。"),
                ChatMessage(role = "user", content = prompt)
            ),
            maxTokens = 60,
            temperature = 0.9
        )
        return gson.toJson(request).toRequestBody(JSON_MEDIA_TYPE)
    }

    // ──────────────────────────────────────────────────
    // 数据类 —— Gson 反序列化映射
    // ──────────────────────────────────────────────────

    /** DeepSeek Chat API 请求体 */
    private data class DeepSeekRequest(
        val model: String,
        val messages: List<ChatMessage>,
        @SerializedName("max_tokens")
        val maxTokens: Int,
        val temperature: Double
    )

    /** 聊天消息 */
    private data class ChatMessage(
        val role: String,
        val content: String
    )

    /** DeepSeek Chat API 响应体 */
    private data class DeepSeekResponse(
        val choices: List<Choice>?
    ) {
        data class Choice(
            val message: Message?
        ) {
            data class Message(
                val content: String?
            )
        }
    }
}
