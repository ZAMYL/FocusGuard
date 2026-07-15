import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// ── 从 secrets.properties 读取 API Key（不提交到 Git）──
val secretsFile = rootProject.file("secrets.properties")
val secrets = Properties()
if (secretsFile.exists()) {
    secrets.load(FileInputStream(secretsFile))
}
val deepseekApiKey = secrets.getProperty("DEEPSEEK_API_KEY", "请替换为你的API Key")

android {
    namespace = "com.focusguard.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.focusguard.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        // 从 secrets.properties 注入 DeepSeek API Key（不硬编码）
        buildConfigField("String", "DEEPSEEK_API_KEY", "\"$deepseekApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.5"
    }
}

dependencies {
    // Jetpack Compose BOM —— 统一管理 Compose 版本
    val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
    implementation(composeBom)

    // Compose UI 核心
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Activity Compose 集成
    implementation("androidx.activity:activity-compose:1.8.2")

    // Lifecycle 运行时
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // AndroidX 核心
    implementation("androidx.core:core-ktx:1.12.0")

    // OkHttp —— 用于 DeepSeek API 网络请求
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Gson —— JSON 解析
    implementation("com.google.code.gson:gson:2.10.1")

    // 调试工具
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
