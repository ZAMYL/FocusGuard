# FocusGuard ProGuard 规则

# 保留 Gson 序列化/反序列化相关的数据类
-keepattributes Signature
-keepattributes *Annotation*

# 保留 DeepSeekClient 中的数据类（防止反射失败）
-keep class com.focusguard.app.DeepSeekClient$** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepattributes EnclosingMethod
