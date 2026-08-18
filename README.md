# AncientPoet（鸿雁）

AncientPoet 是一款“慢交流”实验应用：用户与 AI 扮演的古代诗人通信，信件根据地理距离产生数小时到数天的延迟。仓库目前包含可构建的 Android 客户端、Kotlin Multiplatform 共享层、Ktor 服务端和一个 Desktop 占位客户端。

项目的已验证能力、外部凭据要求和未完成项请以 [docs/STATUS.md](docs/STATUS.md) 为准。文件或页面存在不等于功能已经完成。

## 当前技术栈

- Android：Kotlin、Jetpack/Compose Multiplatform、Koin、Ktor Client
- Shared：Kotlin Multiplatform、Ktor Client、SQLDelight schema
- Server：Ktor 3.0.3、Exposed、Flyway、PostgreSQL/PostGIS、Redis、MinIO
- AI 与推送：DeepSeek API、JPush REST API（均需外部凭据）
- Desktop：Compose Desktop 占位界面，尚不是完整客户端

## Windows 开发环境

已验证环境：

- Android Studio：`D:\AndroidStudio`
- 启动 Gradle 的 JDK：`D:\AndroidStudio\jbr`（本机为 JDK 21，项目统一输出 JVM 17 字节码）
- Android SDK：通常位于 `%LOCALAPPDATA%\Android\Sdk`；本机验证路径为 `C:\Users\LLC\AppData\Local\Android\Sdk`
- Docker Desktop：用于 PostgreSQL、Redis 和 MinIO

首次检出后，在仓库根目录创建不提交的 `local.properties`：

```properties
sdk.dir=C:/Users/LLC/AppData/Local/Android/Sdk
```

PowerShell 会话中设置 Gradle 启动 JDK：

```powershell
$env:JAVA_HOME='D:\AndroidStudio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

不要依赖系统默认的 Java 8；Android Gradle Plugin 与 SQLDelight 插件至少需要更高版本的 JVM。

## 构建与测试

```powershell
# 三套自动化测试
.\gradlew.bat :shared:jvmTest :server:test :androidApp:testDebugUnitTest --no-daemon --console=plain "-Dhttp.proxyHost=" "-Dhttps.proxyHost="

# 编译 Shared JVM、Server、Desktop，并生成 Android debug APK
.\gradlew.bat :shared:compileKotlinJvm :server:compileKotlin :desktopApp:compileKotlinJvm :androidApp:assembleDebug --no-daemon --console=plain "-Dhttp.proxyHost=" "-Dhttps.proxyHost="
```

APK 输出：`androidApp/build/outputs/apk/debug/androidApp-debug.apk`。

## 本地基础设施与 API 冒烟

一条命令会检查/启动 Docker Desktop、启动三项基础设施、构建服务端胖 JAR、验证健康探针、开发短信鉴权、Bearer 访问、诗人种子和地图接口，最后停止进程与容器但保留数据卷：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke.ps1
```

首次运行会把 `deploy/.env.test.example` 复制为忽略的 `deploy/.env`。已有 `.env` 不会被覆盖。开发验证码 `123456` 只在脚本设置的 `KTOR_DEVELOPMENT=true` 下生效。

健康端点：

- `GET /api/v1/health/live`
- `GET /api/v1/health/ready`

## 目录

```text
shared/       KMP 网络、领域模型、用例、SQLDelight schema
androidApp/   当前主要客户端
server/       Ktor API、迁移、业务服务
desktopApp/   Phase 4 占位界面
data/         15 位诗人 JSON 与 5 组城市 JSON（并非全部由运行时直接读取）
deploy/       Docker Compose 与本地环境模板
scripts/      可重复的本地验证脚本
DESIGN/       设计规范与原型资料
docs/         状态、基线、执行计划
```

详细结构见 [ARCHITECTURE.md](ARCHITECTURE.md)。

## License

MIT
