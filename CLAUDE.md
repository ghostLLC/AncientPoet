# CLAUDE.md

本文件是自动化开发代理在 AncientPoet 仓库中的操作指南。事实状态以 `docs/STATUS.md` 为准，不根据目录名或 UI 文件推断功能已完成。

## 环境约束

- Windows 工作目录通常为 `D:\AncientPoet`。
- Gradle 必须由 `D:\AndroidStudio\jbr` 启动；项目目标字节码为 JVM 17。
- Android SDK 默认位于 `%LOCALAPPDATA%\Android\Sdk`，通过忽略的 `local.properties` 配置。
- 不修改用户全局 Gradle 配置；本地代理参数应在命令级处理。
- 不提交 `.env`、`local.properties`、密钥、APK、日志或 Gradle 输出。

PowerShell 初始化：

```powershell
$env:JAVA_HOME='D:\AndroidStudio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

## 必跑质量命令

```powershell
.\gradlew.bat :shared:jvmTest :server:test :androidApp:testDebugUnitTest --no-daemon --console=plain "-Dhttp.proxyHost=" "-Dhttps.proxyHost="
.\gradlew.bat :shared:compileKotlinJvm :shared:compileDebugKotlinAndroid :server:compileKotlin :desktopApp:compileKotlinJvm :androidApp:assembleDebug --no-daemon --console=plain "-Dhttp.proxyHost=" "-Dhttps.proxyHost="
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke.ps1
```

冒烟脚本会保留 Docker 数据卷并且不会覆盖已有 `deploy/.env`。开发短信码只能在 `KTOR_DEVELOPMENT=true` 下使用。

## 架构约定

- Android 会话写入/清除只能经过 `SessionController`；不要绕过它直接改持久存储或 Ktor bearer cache。
- `AncientPoetApi` 是客户端统一 HTTP 边界；请求体应在调用点以具体类型 `setBody(...)`，避免 `Any?` 擦除序列化类型。
- Ktor 版本统一保持同一版本线。服务端当前使用 Koin Core 显式传入路由依赖，不引入面向 Ktor 2 的 `koin-ktor` 路由扩展。
- Exposed 表模型没有声明代码级 `.references()`；需要 join 时必须显式写连接条件，数据库外键由 Flyway 维护。
- Flyway 胖 JAR 必须保留 `mergeServiceFiles()`，否则运行时会把合法迁移误判为不可识别。
- 推送实现是 JPush，不是 FCM。不要恢复 Firebase Messaging 服务端依赖。
- SQLDelight 目前只有 schema 和平台依赖，业务缓存仍是内存实现。

## 当前边界

- 已验证：Shared/Server/Android 单测，Shared JVM/Android、Server、Desktop 编译，Android debug APK，两轮本地 Docker/API 冒烟。
- 凭据受限：DeepSeek、生产 SMS、JPush。
- 部分实现：地图接口硬编码唐代城市；诗词种子 14 首；个人主页内容不完整；肖像缺失。
- 不存在：完整 Desktop、Web、`data/maps`、`data/poems`、SQLDelight 持久 driver 接线。
- 依赖警告：Kotlin 2.0.21 对 AGP 的最高已测试版本为 8.5，而仓库使用 AGP 8.7.2。

## 修改流程

1. 先检查相关调用链和现有测试。
2. 行为修改先写可失败的测试，再做最小实现。
3. 运行直接受影响测试，再运行完整质量门。
4. 基础设施或服务端启动变更必须运行 `scripts/smoke.ps1`。
5. 文档只记录实际验证过的结果；外部凭据未验证时明确标为 credential-gated。
