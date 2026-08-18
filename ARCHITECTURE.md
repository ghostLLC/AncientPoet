# AncientPoet 当前架构

本文描述仓库中已经存在的实现，不把路线图或原型当作完成状态。验证结论和缺口见 [docs/STATUS.md](docs/STATUS.md)。

## 模块边界

```text
Android App ─┐
             ├─ shared（模型、API、会话、用例、内存缓存）
Desktop 占位 ┘
                    │ HTTP /api/v1
                    ▼
Ktor Server → Service → Repository/Exposed → PostgreSQL/PostGIS
     │           │
     │           ├─ Redis：延迟消息调度
     │           ├─ MinIO：图片对象存储
     │           └─ DeepSeek：对话、翻译、图片理解（需凭据）
     └─ JPush REST：到信推送（需凭据；Android SDK 尚未启用）
```

### `androidApp`

当前主要 UI 客户端。采用 Compose、ViewModel/StateFlow 和 Koin。登录会话由 `SessionController` 统一写入和清除；受保护请求在发送前以当前持久会话为准，防止刷新、退出和重新登录之间的竞态。默认模拟器 API 地址为 `http://10.0.2.2:8080/api/v1/`，可通过 Gradle 属性 `ANCIENT_POET_API_BASE_URL` 覆盖。

已存在诗人、会话、地图、诗词、社区、资料等屏幕，但部分产品行为仍不完整，不能据此宣称对应阶段完成。

### `shared`

Kotlin Multiplatform 共享层包含：

- `data/api`：Ktor Client API 与统一 `ApiResult`
- `data/session`：跨平台会话存储边界
- `domain`：模型、仓储接口与用例
- `util`：距离与日期工具
- SQLDelight 的 `Poet.sq`、`Message.sq` schema

当前运行时仍使用 `InMemoryLocalDataSource`。Android/JVM SQLDelight 依赖已存在，但平台 driver 尚未接入业务依赖图，因此重启后不会保留该缓存。

### `server`

服务端调用链为 Ktor plugin → route → service → repository → Exposed。Koin Core 负责对象图，路由依赖显式传入，不使用与 Ktor 3.0.3 不兼容的旧 Koin–Ktor 2 路由扩展。

主要基础设施：

- Flyway：`V1` schema、`V2` 初始种子、`V3` 扩展种子
- PostgreSQL/PostGIS：业务数据和地理字段
- Redis：`MessageDeliveryScheduler` 的有序集合
- MinIO：上传图片；业务桶按首次上传惰性创建
- `GET /api/v1/health/live`：仅检查进程
- `GET /api/v1/health/ready`：有界检查 PostgreSQL、Redis、MinIO

服务端胖 JAR 合并 `META-INF/services`，保证 Flyway 在单 JAR 运行时能识别迁移和 PostgreSQL 插件。

## 认证与会话

短信验证返回 access/refresh token。开发绕过码 `123456` 仅在 `KTOR_DEVELOPMENT=true` 时有效。生产短信供应商调用尚未实现完整闭环，当前验证码主要存于服务进程内存。

JWT 使用 HMAC、issuer、audience 与过期时间；refresh token 带 `type=refresh`，刷新验证拒绝普通 access token及错误签名、issuer、audience和过期 token。

## 延迟模型

服务端公式：

```text
distance = haversine(from, to)
base = 2h                    when distance < 30 km
       24h                   when 30 km <= distance < 150 km
       distance / 80 * 24h   otherwise
final = clamp(base × settledCoefficient × eventMultiplier, 2h, 7d)
```

定居系数为 `0.8`；战争/流放事件当前使用 `1.5`。Shared 的客户端预览实现相同的三段基础距离公式，但不应用服务端的定居和剧情系数。

## 推送

实际服务端代码使用 `JPushClient` 和 JPush REST API v3。仓库不再包含 FCM 客户端或 Firebase Messaging 服务端依赖。JPush 的 AppKey/MasterSecret 未配置时不能完成真实推送；Android JPush SDK 依赖仍为注释状态，所以端到端设备推送尚未验证。

## 数据现状

- 数据库迁移可产生 15 位诗人；`data/poets` 也有 15 个 JSON。
- 数据库诗词种子目前共 14 首，不是完整诗词库。
- `data/cities` 有汉、晋、唐、宋、明五组 JSON，但当前 `/map/{dynastyId}/cities` 仍返回代码中硬编码的唐代城市列表，未按朝代加载这些文件。
- `data/maps` 与 `data/poems` 目录不存在；没有可交付的朝代地图底图或独立完整诗词数据集。
- 诗人肖像资源不存在，UI 使用文字/占位表现。
- 社区个人资料接口提供统计信息，但个人发帖列表等行为仍不完整。

## 客户端状态

- Android：可编译并生成 debug APK；核心会话/API 行为有 JVM 单元测试。
- Desktop：只有一个明确标注“Phase 4 待开发”的占位窗口，可编译但不是完整产品。
- Web：仓库中没有 Web 客户端模块。

## 质量门

本地与 GitHub Actions 使用同一组核心命令：

```text
:shared:jvmTest :server:test :androidApp:testDebugUnitTest
:shared:compileKotlinJvm :server:compileKotlin :desktopApp:compileKotlinJvm :androidApp:assembleDebug
```

`scripts/smoke.ps1` 另行验证 Docker 基础设施、Flyway、健康检查、开发鉴权和代表性 API。CI 定义不包含真实第三方凭据测试，也不代表 DeepSeek、生产 SMS 或 JPush 已端到端验证。

## 已知依赖风险

项目使用 Kotlin 2.0.21 与 Android Gradle Plugin 8.7.2。Kotlin 插件会警告 AGP 8.7.2 超过其最高已测试版本 8.5；当前构建通过，但后续升级应成组验证 Kotlin、Compose、AGP 与 Gradle Wrapper，而不是单独提升其中一个。
