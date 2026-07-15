# 🍅 专注卫士 (FocusGuard)

> 极简、高效的 Android 自律工具 —— 番茄钟 + 应用拦截器，专治拖延症。

## ✨ 功能

- **🍅 番茄钟** — 15/25/45/60 分钟可选，圆形倒计时动画
- **🛡️ 应用拦截** — 专注期间自动拦截抖音和微信朋友圈/视频号/看一看
- **🤖 AI 毒舌警醒** — 触发拦截时调用 DeepSeek 生成狠辣激励语
- **🔒 全屏锁机** — 拦截页面禁用返回键、隐藏状态栏，无法绕过
- **🌐 全中文** — UI、注释、日志、Prompt 全部中文

## 🏗️ 技术栈

| 层级 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 构建 | AGP 8.2.0 + Gradle 8.14.5 |
| 网络 | OkHttp 4.12 + Gson |
| AI | DeepSeek Chat API |
| 架构 | 单 Activity + AccessibilityService |

## 📁 项目结构

```
FocusGuard/
├── build.gradle.kts                     # AGP + Kotlin 版本
├── settings.gradle.kts                  # 仓库镜像（阿里云）
├── gradle.properties                    # JVM 参数 + 国内网络优化
├── secrets.properties                   # API Key（Git 已排除，需自行创建）
└── app/
    ├── build.gradle.kts                 # 依赖：Compose、OkHttp、Gson
    └── src/main/
        ├── AndroidManifest.xml          # Activity + Service + 权限
        ├── res/
        │   ├── xml/accessibility_service_config.xml
        │   └── values/                  # strings、themes、colors
        └── java/com/focusguard/app/
            ├── TimerState.kt            # 全局番茄钟状态（StateFlow）
            ├── MainApplication.kt       # Application 入口
            ├── MainActivity.kt          # 主界面（番茄钟控制面板）
            ├── LockActivity.kt          # 全屏锁机界面
            ├── MyAccessibilityService.kt # 无障碍拦截服务
            ├── DeepSeekClient.kt        # DeepSeek API 客户端
            └── ui/theme/
                ├── Color.kt             # 配色方案
                └── Theme.kt             # Material 3 主题
```

## 🚀 快速开始

### 环境要求

- Android Studio Hedgehog (2023.1) 或更新版本
- Android SDK 34+
- JDK 17
- 一台 Android 设备或模拟器 (API 26+)

### 1. 克隆项目

```bash
git clone https://github.com/ZAMYL/FocusGuard.git
```

### 2. 配置 API Key

在项目根目录创建 `secrets.properties`：

```properties
DEEPSEEK_API_KEY=你的DeepSeek_API_Key
```

> 没有 Key？去 [platform.deepseek.com](https://platform.deepseek.com/) 免费注册获取。

### 3. 用 Android Studio 打开

打开 AS → **Open** → 选择 `FocusGuard` 文件夹 → 等待 Gradle Sync 完成。

> ⚠️ 国内网络慢？项目已配置阿里云 + 华为云镜像，Sync 通常在 1 分钟内完成。

### 4. 运行

1. 选择设备（真机或模拟器 API 26+）
2. 点 **▶ Run**
3. 首次安装后，进入 **系统设置 → 无障碍 → 专注卫士** 开启服务
4. 回到应用，选时长，点 **开始专注**

## 🛡️ 拦截规则

| 应用 | 触发条件 |
|------|---------|
| 🔴 抖音 (`com.ss.android.ugc.aweme`) | 打开即拦截 |
| 🟡 微信·朋友圈 | 屏幕显示"朋友圈"文字时拦截 |
| 🟡 微信·视频号 | 屏幕显示"视频号"文字时拦截 |
| 🟡 微信·发现页 | 屏幕显示"发现"文字时拦截 |
| 🟡 微信·看一看 | 屏幕显示"看一看"文字时拦截 |

> 拦截仅在番茄钟运行期间生效。番茄钟结束后恢复正常使用。

## ⚙️ 国内构建配置

为解决国内 Gradle 下载慢的问题，已预配置：

- **Gradle Wrapper** → 华为云镜像 (`gradle-wrapper.properties`)
- **Maven 依赖** → 阿里云镜像 (`settings.gradle.kts`)
- **HTTP 超时** → 120 秒 (`gradle.properties`)
- **Gradle 缓存** → 可自定义到无中文路径 (`gradle.properties`)

如遇 `jlink.exe` 路径报错，确保：
- JDK 路径不含空格和中文
- Windows 临时目录 (`TEMP`) 不含中文
- 或参考 `gradle.properties` 中 `systemProp.java.io.tmpdir` 配置

## 📄 License

MIT — 随便用，随便改。
