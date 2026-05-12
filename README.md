# 🏯 AncientPoet（鸿雁）

> **在快时代慢下来，像"当时"一样交流**

AncientPoet 是一款跨平台慢交流应用。用户与 AI 扮演的古代诗人通过"书信"交流，消息根据古代地图上的地理距离产生真实延迟（数小时到数天），引导用户沉下心来写出更长、更有深度的内容。

## ✨ 核心特色

- 📜 **书信交流** — 与李白、杜甫、苏轼等古代诗人以文言文书信往来
- ⏳ **真实延迟** — 消息延迟基于古代地图距离计算，从数小时到数天不等
- 🗺️ **古代地图** — 在朝代地图上定居或移动，影响书信延迟
- 📖 **故事线模式** — 随诗人生平推进，亲身经历历史
- 🎨 **绘画传信** — 在信中附上手绘画作，诗人以文字品评
- 🔄 **文言翻译** — 一键切换文言/白话对照阅读
- 👥 **玩家社区** — 分享精彩对话，与其他用户交流

## 🏗️ 技术栈

| 层级 | 技术 |
|------|------|
| **客户端** | Kotlin Multiplatform + Compose Multiplatform |
| **后端** | Kotlin + Ktor Server |
| **数据库** | PostgreSQL + PostGIS |
| **缓存** | Redis |
| **对象存储** | MinIO |
| **AI** | DeepSeek V4-Flash (角色扮演 + 翻译 + 视觉理解) |
| **推送** | Firebase Cloud Messaging |

## 📁 项目结构

```
AncientPoet/
├── shared/          # KMP 共享模块（业务逻辑、网络、本地缓存）
├── androidApp/      # Android 应用（Jetpack Compose）
├── desktopApp/      # Desktop 应用（Compose Desktop）
├── server/          # Ktor 后端服务
├── data/            # 静态数据（诗人资料、诗词、地图、城市坐标）
├── deploy/          # 部署配置（Docker Compose、Nginx）
└── ARCHITECTURE.md  # 完整架构设计文档
```

## 📋 开发阶段

1. **Phase 1 (MVP)** — Android + 后端核心，3-5位诗人，开放聊天，基础延迟
2. **Phase 2** — 15-20位诗人，故事线模式，完整地图交互，绘画功能
3. **Phase 3** — 社区功能（发帖、评论、分享）
4. **Phase 4** — Desktop + Web 多平台
5. **Phase 5** — 商业化（付费诗人、订阅）

## 🚀 快速开始

> 详见 [ARCHITECTURE.md](./ARCHITECTURE.md) 获取完整技术架构和实施指南。

### 前置要求

- JDK 17+
- Android Studio (客户端开发)
- IntelliJ IDEA (后端/桌面端开发)
- Docker & Docker Compose (后端部署)
- DeepSeek API Key

### 后端启动

```bash
cd deploy
cp .env.example .env  # 配置环境变量
docker-compose up -d  # 启动 PG + Redis + MinIO
cd ../server
./gradlew run         # 启动 Ktor 服务
```

### Android 启动

用 Android Studio 打开项目根目录，选择 `androidApp` 运行配置。

## 📄 License

MIT
