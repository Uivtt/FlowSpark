# 🔥 FlowSpark - 零门槛对话式 AI 视觉工作流

[![Build Status](https://github.com/Uivtt/FlowSpark/actions/workflows/build-apk.yml/badge.svg)](https://github.com/Uivtt/FlowSpark/actions/workflows/build-apk.yml)

**FlowSpark** 是安卓平台首款"零门槛、对话式"的 AI 视觉工作流工具。让不懂节点的普通人，通过说话就能驱动顶级 AI 模型完成图像处理。

**核心思想：** 以 OpenAI API 格式为统一接口，底层可自由切换 DeepSeek / Groq / Together / OpenAI，不绑定任何单一 AI 厂商。

---

## 📦 项目结构

```
FlowSpark/
├── app/                    # Android 主模块 (Kotlin + Compose + Room)
│   ├── src/main/java/com/flowspark/app/
│   │   ├── ai/             # AiClient 接口 + OpenAI-Compatible 实现
│   │   ├── data/           # Room 数据层 + DataStore 配置
│   │   ├── domain/         # 领域模型 + LinearExecutor + 5 个图像工具
│   │   ├── service/        # Foreground Service 前台保活
│   │   └── ui/             # Compose 界面 (Theme + ViewModel + Screens)
│   └── src/test/           # 单元测试 (JUnit 4)
├── proxy/worker/           # Cloudflare Workers 代理层 (供应商路由 + 熔断 + 限额)
├── .github/workflows/      # GitHub Actions 自动构建 APK
└── gradle/                 # Gradle 版本目录 + wrapper
```

## 🚀 快速开始

### 前提条件

- Android Studio Ladybug (2024.3+) 或 IntelliJ IDEA
- JDK 17+
- 一个 OpenAI-Compatible 的 AI 供应商 API Key

### 本地构建

```bash
# 1. 克隆仓库
git clone https://github.com/Uivtt/FlowSpark.git
cd FlowSpark

# 2. 构建 Debug APK
./gradlew :app:assembleDebug

# 3. 运行单元测试
./gradlew :app:testDebugUnitTest

# 4. APK 位置
# app/build/outputs/apk/debug/app-debug.apk
```

### 配置 AI 供应商

App 默认通过代理层访问 AI 服务。在 `SettingsRepository` 中可配置：

| 配置项 | 说明 | 默认值 |
|---|---|---|
| `baseUrl` | AI 代理地址 | `https://flowspark-proxy.example.workers.dev` |
| `apiKey` | 代理 API Key | `demo-key-please-configure` |
| `llmModel` | 意图解析模型 | `deepseek-chat` |
| `imageModel` | 图像生成模型 | `black-forest-labs/FLUX.1-schnell` |

**直接修改** `app/build.gradle.kts` 中的 `buildConfigField` 默认值，或通过 App 内设置界面（TBD）运行时修改。

## ☁️ 部署代理层（Cloudflare Workers）

```bash
# 1. 安装 wrangler CLI
npm install -g wrangler

# 2. 进入代理目录
cd proxy/worker

# 3. 设置供应商密钥
wrangler secret put DEEPSEEK_API_KEY
wrangler secret put TOGETHER_API_KEY
wrangler secret put OPENAI_API_KEY        # 可选
wrangler secret put PROXY_API_KEY         # 你的代理层校验 Key

# 4. 部署
wrangler deploy

# 5. 将输出的 URL 配置到 App 的 AI_PROXY_BASE_URL
```

## 🧪 测试供应商兼容性

修改 `app/src/main/java/com/flowspark/app/data/prefs/SettingsRepository.kt` 中的默认配置即可切换供应商：

| 供应商 | baseUrl | LLM 模型 | 图片模型 |
|---|---|---|---|
| **DeepSeek** | `https://api.deepseek.com` | `deepseek-chat` | — |
| **Groq** | `https://api.groq.com/openai/v1` | `llama-3.3-70b-versatile` | — |
| **Together AI** | `https://api.together.xyz/v1` | `mistralai/Mixtral-8x22B-Instruct-v0.1` | `black-forest-labs/FLUX.1-schnell` |
| **OpenAI** | `https://api.openai.com/v1` | `gpt-4.1-mini` | `gpt-image-1` |

## 📊 架构概述

```
[Android App (AiClient 接口)]
    │ 只认 OpenAI 格式
    ▼
[代理层 (Cloudflare Workers)]
    ├─ 供应商路由 + 熔断
    ├─ 每日限额
    └─ API Key 保护
    │
    ├─→ DeepSeek (意图解析，最便宜)
    └─→ Together (图像生成，Flux.1)
```

## 📄 许可证

MIT
