# AncientPoet 实施状态

更新日期：2026-08-18。本文按“已验证、凭据受限、部分实现、不存在”区分仓库现状。

## 已验证工作

- Shared JVM、Shared Android、Server、Desktop JVM 均可编译。
- Android 可组装 debug APK。
- 自动化测试共 35 个：Shared 9、Server 12、Android 14，均通过且目标任务不再是 `NO-SOURCE`。
- JWT、距离/延迟、API 结果、会话刷新竞态、失败重试和健康路由均有确定性测试。
- Docker Compose 可启动 PostgreSQL/PostGIS、Redis、MinIO。
- `scripts/smoke.ps1` 已连续通过两轮，覆盖：
  - Flyway V1–V3
  - `/health/live` 与 `/health/ready`
  - 开发短信验证与真实 access/refresh token 返回
  - Bearer 访问 `/conversations`
  - 15 位数据库种子诗人
  - 非空唐代城市接口
- GitHub Actions 已编码相同的测试优先和跨模块编译命令；尚未推送，因此远程 Actions 运行未验证。

## 已实现但受外部凭据限制

- DeepSeek 对话、翻译、视觉请求：需要有效 `DEEPSEEK_API_KEY`，未做真实计费 API 端到端测试。
- 生产短信：配置字段存在，但供应商发送闭环尚未以真实账号验证；开发模式使用 `123456`。
- JPush：服务端 REST 客户端存在，需要 `JPUSH_APP_KEY` 和 `JPUSH_MASTER_SECRET`；Android SDK 仍未启用，设备到信推送未验证。

## 部分产品行为

- 地图：`data/cities` 含五个朝代 JSON，但服务端城市接口当前返回硬编码唐代城市，未按 `dynastyId` 加载数据。
- 诗词：数据库种子共 14 首，不是完整诗词库。
- 社区个人页：统计和基础资料存在，个人发帖列表等展示仍不完整。
- 诗人肖像：仓库没有正式肖像资产，客户端使用占位表现。
- Shared 离线缓存：`InMemoryLocalDataSource` 会话内可用，但重启后不持久。

## 仓库中不存在

- `data/maps` 朝代地图底图。
- `data/poems` 独立完整诗词数据集。
- 完整 Desktop 客户端；现有模块仅为 Phase 4 占位窗口。
- Web 客户端模块。
- SQLDelight Android/JVM driver 到运行时仓储的持久化接线。

## 已知依赖风险

Kotlin 2.0.21 在构建时明确警告：Android Gradle Plugin 8.7.2 超过其最高已测试版本 8.5。当前本地质量门通过，但该组合不属于 Kotlin 插件声明的已测试范围，依赖升级必须整体回归。

## 验证命令

```powershell
$env:JAVA_HOME='D:\AndroidStudio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"

.\gradlew.bat :shared:jvmTest :server:test :androidApp:testDebugUnitTest --rerun-tasks --no-daemon --console=plain "-Dhttp.proxyHost=" "-Dhttps.proxyHost="
.\gradlew.bat :shared:compileKotlinJvm :shared:compileDebugKotlinAndroid :server:compileKotlin :desktopApp:compileKotlinJvm :androidApp:assembleDebug --rerun-tasks --no-daemon --console=plain "-Dhttp.proxyHost=" "-Dhttps.proxyHost="
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke.ps1
```
