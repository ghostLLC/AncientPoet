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
python scripts/validate_data.py
.\gradlew.bat :shared:jvmTest :server:test :androidApp:testDebugUnitTest :androidApp:lintDebug --no-daemon --console=plain "-Dhttp.proxyHost=" "-Dhttps.proxyHost="
.\gradlew.bat :shared:compileKotlinJvm :shared:compileDebugKotlinAndroid :server:compileKotlin :desktopApp:compileKotlinJvm :androidApp:assembleDebug --no-daemon --console=plain "-Dhttp.proxyHost=" "-Dhttps.proxyHost="
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke.ps1
```

冒烟脚本使用随机名称、随机密钥和独立端口的临时 PostgreSQL 容器，完成 21 项数据库/HTTP 回归后清理自己创建的环境；不会使用或覆盖已有 `deploy/.env`。`run_server.ps1 -InitializeDemo` 才用于保留本地演示数据库。开发短信码只能在 `KTOR_DEVELOPMENT=true` 下使用。

## 架构约定

- Android 会话写入/清除只能经过 `SessionController`；不要绕过它直接改持久存储或 Ktor bearer cache。
- `AncientPoetApi` 是客户端统一 HTTP 边界；请求体应在调用点以具体类型 `setBody(...)`，避免 `Any?` 擦除序列化类型。
- Ktor 版本统一保持同一版本线。服务端当前使用 Koin Core 显式传入路由依赖，不引入面向 Ktor 2 的 `koin-ktor` 路由扩展。
- Exposed 表模型没有声明代码级 `.references()`；需要 join 时必须显式写连接条件，数据库外键由 Flyway 维护。
- Flyway 胖 JAR 必须保留 `mergeServiceFiles()`，否则运行时会把合法迁移误判为不可识别。
- Android 0.2.0 使用用户主动开启的 WorkManager 周期检查，不依赖 JPush SDK；服务端保留可选 JPush REST 适配，不恢复 Firebase 依赖。
- SQLDelight 已接入 Android driver、资源缓存与持久草稿。私有缓存必须按账号分区；成功回执只能清除匹配 UUID 的草稿。
- 生成、翻译、投递与摘要由 PostgreSQL 持久任务驱动；保留租约、幂等与提交保护，网络调用不得放入持锁事务。
- 社区/上传路由当前不安装；若修改范围必须先明确实际产品需求，不能只因为类文件存在便恢复入口。

## 当前边界

- 已验证：45 项单元测试、21 项真实 PostgreSQL/HTTP 检查、Android Lint 无错误、Debug 与未签名 Release 编译、Desktop 占位编译；实操范围见 `docs/VERIFICATION.md`。
- 外部待验：真实模型、腾讯云短信、正式签名、生产代理/备份及小米 HyperOS 真机后台行为。
- 内容范围：15 位诗人、数据库朝代城市、24 篇诗词，其中 10 篇本轮核校并附来源；14 篇旧资料保留待核校标记。肖像目前使用姓名印章。
- 不开放：社区、媒体上传、绘图；完整 Desktop 和 Web 未实现。
- 版本约束：Kotlin 2.1.21、AGP 8.7.2、Gradle 8.10、SDK 35。依赖安全结果是时点结果，复跑 OSV 审计后再引用。
- 发布构建必须显式提供 HTTPS `ANCIENT_POET_API_BASE_URL`，未签名构建和本地演示不等同于正式发布。

## 修改流程

1. 先检查相关调用链和现有测试。
2. 行为修改先写可失败的测试，再做最小实现。
3. 运行直接受影响测试，再运行完整质量门。
4. 基础设施或服务端启动变更必须运行 `scripts/smoke.ps1`。
5. 文档只记录实际验证过的结果；外部凭据未验证时明确标为 credential-gated。
