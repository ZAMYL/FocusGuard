# 🍅 专注卫士 2.0 (FocusGuard)

> Android 极简自律工具 —— Canvas 呼吸圆盘 + 白噪音舱 + AI 毒舌拦截，专治拖延症。

## ✨ 核心功能

### 🎛️ 时间呼吸舱
- **Canvas 圆形量化吸附拨盘** — 手指拖拽圆环无极调时 (1~120 分钟)，松手自动吸附到最近整数分钟
- **Spring 弹性数字动画** — 分钟切换时触发弹簧弹跳 (1.0→1.15→1.0)
- **触觉反馈** — 每次跨过分钟刻度触发 CLOCK_TICK 振动
- **圆环动态膨胀** — 拖拽时圆环变粗 (8dp→14dp)，闲置时恢复
- **4 秒呼吸动效** — 计时运行时圆盘伴随深呼吸节奏缩放
- **番茄自动循环** — 专注 25 分 → 短休 5 分 → 专注 → … → 长休 15 分（每 4 次）

### 🌊 深海白噪音舱
- **4 首预置白噪音** — 深山夜雨 / 冬夜篝火 / 治愈海浪 / 晨曦森林
- **2×2 极简卡片网格** — 选中变琥珀色 + 右上角绿色呼吸灯
- **单选互斥播放** — `MediaPlayer` 循环播放，点击切换自动释放旧资源
- **全局音量滑块** — 一个滑块控制所有白噪音音量
- **安全防崩溃** — 资源缺失时 Toast 提示，绝不闪退

### 🎨 个性化换肤
- **极光暗影** — 深灰蓝底 + 极光绿发光字
- **浅砂暮霭** — 奶咖柔白 + 治愈原木色
- **暮紫静夜** — 深邃暗紫 + 玫瑰粉金
- 三套主题一键切换，底部滑出抽屉，与白噪音样式统一

### 🛡️ 智能拦截 2.0
- **20+ 应用直接拦截** — 抖音/快手/小红书/微博/B站/腾讯视频/优酷/爱奇艺/淘宝/京东/拼多多…
- **微信精准拦截** — 仅拦截朋友圈/视频号/看一看/直播/游戏/购物，**不挡办公沟通**
- **休息模式全放行** — 休息期间无任何拦截
- **拦截规则 ModalBottomSheet** — 底部滑出，不占主页空间

### 🤖 AI 毒舌警醒
- **DeepSeek V4** — 锁屏时异步请求毒舌幽默警醒语
- **本地语录库 105 条** — DeepSeek 离线时自动切换本地随机语录，永不断档
- **全屏锁机** — 禁用返回键 + 隐藏状态栏 + 呼吸倒计时 + AI 文字淡入动画

### 📊 主页 2.0 布局
- **AI 呼吸语录卡片** — 背景巨型半透明引号 + 随机语录 + alpha 呼吸动画
- **今日专注看板** — 已专注分钟数 + 番茄数 + 极细进度条
- **三阶段递进通知** — 切后台后 10s→30s→300s 三次提醒

## 🏗️ 技术栈

| 层级 | 技术 |
|------|------|
| 语言 | Kotlin 1.9.20 |
| UI | Jetpack Compose + Material 3 |
| Canvas | 自定义 DrawScope 圆环 + 手势吸附 |
| 音频 | MediaPlayer + ToneGenerator + RingtoneManager |
| 动画 | InfiniteTransition + Animatable Spring + animateFloatAsState |
| 网络 | OkHttp 4.12 + Gson |
| AI | DeepSeek Chat API |
| 构建 | AGP 8.2.0 + Gradle 8.14.5 |

## 📁 项目结构

```
FocusGuard/
├── build.gradle.kts
├── settings.gradle.kts                # 阿里云镜像
├── gradle.properties                  # JVM + 国内优化
├── secrets.properties                 # API Key（Git 排除）
└── app/src/main/
    ├── AndroidManifest.xml
    ├── res/
    │   ├── raw/                       # 4 首白噪音 MP3
    │   ├── xml/accessibility_service_config.xml
    │   └── values/
    └── java/com/focusguard/app/
        ├── TimerState.kt              # StateFlow 全局状态
        ├── MainActivity.kt            # 2.0 主界面
        ├── LockActivity.kt            # 全屏锁机
        ├── MyAccessibilityService.kt  # 无障碍拦截
        ├── DeepSeekClient.kt          # AI 客户端
        ├── WhiteNoiseManager.kt       # 白噪音播放器
        ├── AudioHelper.kt             # 音效 + 振动
        ├── NotificationHelper.kt      # 后台通知
        ├── LocalQuotes.kt             # 本地语录库 (105 条)
        ├── MainApplication.kt
        └── ui/theme/
            ├── Color.kt               # 三套配色
            └── Theme.kt               # 动态主题
```

## 🚀 快速开始

### 环境要求

- Android Studio Hedgehog+
- Android SDK 34+
- JDK 17
- Android 设备/模拟器 API 26+

### 1. 克隆

```bash
git clone https://github.com/ZAMYL/FocusGuard.git
```

### 2. 配置 API Key

创建 `secrets.properties`：

```properties
DEEPSEEK_API_KEY=你的Key
```

### 3. 运行

AS 打开 → Sync → ▶ Run → 系统设置开启「专注卫士」无障碍服务。

## 🛡️ 拦截规则

| 策略 | 应用 |
|------|------|
| 🔴 打开即拦截 | 抖音/TikTok、快手、小红书、微博、B站、腾讯视频、优酷、爱奇艺、芒果TV、番茄小说、掌阅、淘宝、京东、拼多多、QQ、贴吧 |
| 🟡 精准扫描 | 微信 → 仅拦截 朋友圈/视频号/看一看/直播/游戏/购物/小程序 |
| 🟢 全部放行 | 休息模式 |

## ⚙️ 国内构建

- Gradle Wrapper → 华为云镜像
- Maven → 阿里云镜像
- HTTP 超时 → 120s
- `java.io.tmpdir` → `D:/Temp`（避中文用户名）

## 📄 License

MIT
