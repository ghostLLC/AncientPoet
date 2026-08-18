# AncientPoet Foundation Repair Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Do not use subagents unless the user explicitly authorizes them in the execution conversation.

**Goal:** Turn the current “server skeleton + uncompiled client prototype” into an honestly verifiable baseline in which all four modules compile real source, Android authentication works across app restarts, local infrastructure can be smoke-tested, core behavior has automated tests, CI enforces the baseline, and repository documentation matches reality.

**Architecture:** Repair the build graph before changing application behavior. Keep the existing KMP/Compose/Ktor structure, centralize session and HTTP concerns behind small shared contracts, use a platform-specific Android session store, and verify server integration against PostgreSQL/PostGIS, Redis, and MinIO. Avoid a broad Clean Architecture rewrite; this plan stabilizes the current product so later product design can proceed on trustworthy foundations.

**Tech Stack:** Kotlin 2.0.21, Kotlin Multiplatform, Compose Multiplatform 1.7.3, Android Gradle Plugin 8.7.2, Gradle 8.10, Ktor 3.0.3, Koin 4.0.2, PostgreSQL/PostGIS, Redis, MinIO, Docker Compose, GitHub Actions.

**Spec:** `README.md`, `ARCHITECTURE.md`, `CLAUDE.md`, `DESIGN/DESIGN.md`, and this plan. When those documents conflict with executable evidence, tests and build output take precedence; Work Package 6 then corrects the documents.

## Global Constraints

- Work only in `D:\AncientPoet` on a new branch named `codex/foundation-repair`.
- Do not push, open a pull request, rewrite history, or merge without explicit user authorization.
- Preserve the user’s unrelated files and changes. At handoff, the plan itself may be the sole untracked path: `docs/superpowers/plans/2026-08-18-foundation-repair.md`. If any other initial change exists, stop and report the exact paths before editing.
- Use `apply_patch` for file edits. Use Git-aware, non-destructive operations and never run `git reset --hard` or broad recursive deletion.
- Use `D:\AndroidStudio\jbr` as the Gradle launcher. The system `JAVA_HOME` currently points to Java 8 and is not suitable.
- Compile Kotlin and Java bytecode to JVM 17 across `shared`, `server`, `desktopApp`, and `androidApp`; do not solve target mismatches by suppressing Gradle validation.
- Keep Android `compileSdk=34`, `targetSdk=35`, and `minSdk=26` unless a concrete compiler error requires a separately documented change.
- Do not modify `C:\Users\LLC\.gradle\gradle.properties`. Its proxy settings may be needed by other projects. For direct dependency access in this repository, pass `"-Dhttp.proxyHost=" "-Dhttps.proxyHost="` to local Gradle commands.
- Do not add production secrets. `.env`, API keys, JWT secrets, SMS credentials, JPush credentials, signing keys, and user tokens must remain untracked.
- No Phase 4 product work, monetization, portrait generation, full offline SQLDelight wiring, visual redesign, or new product features belong in this branch.
- No compiler-error suppression, broad lint suppression, empty exception handlers, commented-out screens, fake success responses, or deletion of features merely to make the build green.
- Each work package ends with its own verification and commit. If verification fails, do not commit that package as complete.
- Keep the UI copy and the established AncientPoet visual design unchanged except where a visible loading/error/retry state is required by Work Package 3.

## Confirmed Baseline at Handoff

| Check | Confirmed result on 2026-08-18 |
|---|---|
| Git | `master` equals `origin/master`; worktree was clean after clone |
| Server | `:server:compileKotlin` succeeds |
| Shared JVM | `:shared:compileKotlinJvm` succeeds |
| Shared Android | Fails because Kotlin target is 21 while Java target is 17 |
| Android app | No Android Kotlin compile task is registered; source has not been compiled |
| Desktop | `:desktopApp:compileKotlinJvm` reports `NO-SOURCE` because source is under `src/main/kotlin` |
| Tests | Zero tracked test files; test tasks report `NO-SOURCE` |
| CI | No `.github/workflows` files |
| Docker | Compose configuration parses; Docker Desktop daemon was not running |
| Repository hygiene | 67 generated/copied files are tracked under `server/bin`; no `data/maps` or `data/poems` assets exist |

## Execution Order and Review Gates

| Stage | Work package | Gate evidence |
|---|---|---|
| A | 1. Real four-module build wiring | Android Kotlin task exists; Desktop compiles a real source file; targets are JVM 17 |
| A | 2. Android compile repair | `assembleDebug` succeeds and produces a non-empty APK |
| B | 3. API/auth/session/error architecture | Login persists tokens; authenticated calls carry Bearer tokens; refresh and logout are tested |
| C | 4. Docker integration smoke workflow | One command validates Postgres, Redis, MinIO, Flyway, login, and authenticated API access |
| D | 5. Automated tests and CI | Test tasks execute real tests; CI workflow passes the same build/test commands |
| E | 6. Repository cleanup and documentation truth | Generated files are untracked; docs match verified behavior; final full verification passes |

At each gate, record the commit SHA and verification output in the execution conversation. Continue automatically when the gate passes. Stop only for a genuine blocker, a required product decision, or a scope expansion.

---

## Task 0: Establish the Execution Branch and Preserve Baseline Evidence

**Files:**
- Create: `docs/reports/foundation-baseline.md`

**Interfaces:**
- Consumes: Current clean `master` checkout.
- Produces: Branch `codex/foundation-repair` and an immutable written baseline for later comparison.

- [ ] **Step 1: Confirm the exact repository and clean worktree**

Run:

```powershell
git -C D:\AncientPoet remote -v
git -C D:\AncientPoet status --short --branch
git -C D:\AncientPoet rev-list --left-right --count master...origin/master
```

Expected: remote is `https://github.com/ghostLLC/AncientPoet.git`, the ahead/behind result is `0 0`, and the only permitted status entry is the untracked handoff plan `docs/superpowers/plans/2026-08-18-foundation-repair.md`. Any additional entry is a blocker until the user identifies its ownership.

- [ ] **Step 2: Create the repair branch**

Run:

```powershell
git -C D:\AncientPoet switch -c codex/foundation-repair
```

Expected: current branch is `codex/foundation-repair`.

- [ ] **Step 3: Record baseline commands and observed failures**

Create `docs/reports/foundation-baseline.md` with:

```markdown
# Foundation Baseline — 2026-08-18

- Launcher: `D:\AndroidStudio\jbr` (JDK 21), target bytecode required: JVM 17.
- Android SDK: `C:\Users\LLC\AppData\Local\Android\Sdk`.
- Server Kotlin compile: passes.
- Shared JVM compile: passes.
- Shared Android compile: fails with Java 17 / Kotlin 21 target mismatch.
- Android app: Kotlin compile task absent before repair.
- Desktop compile: reports `NO-SOURCE` before repair.
- Tracked tests: 0.
- Tracked GitHub Actions workflows: 0.
- Tracked `server/bin` files: 67.
- Docker Compose config: valid; Docker daemon availability depends on Docker Desktop.
```

- [ ] **Step 4: Commit only the baseline report**

```powershell
git add docs/reports/foundation-baseline.md docs/superpowers/plans/2026-08-18-foundation-repair.md
git commit -m "docs: record foundation repair baseline"
```

Expected: one documentation-only commit containing both the execution plan and baseline report, followed by a clean worktree.

---

## Task 1: Work Package 1 — Make All Four Modules Compile Real Source

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `shared/build.gradle.kts`
- Modify: `androidApp/build.gradle.kts`
- Modify: `desktopApp/build.gradle.kts`
- Create: `desktopApp/src/jvmMain/kotlin/com/ancientpoet/desktop/Main.kt`
- Delete after content-preserving move: `desktopApp/src/main/kotlin/com/ancientpoet/desktop/Main.kt`

**Interfaces:**
- Consumes: Existing four-module Gradle project and existing `Main.kt` content.
- Produces: JVM 17 compile contract for all modules, a real Android Kotlin task, and a real Desktop JVM source set. Work Package 2 relies on Android source actually entering the compiler.

- [ ] **Step 1: Reproduce the three build-wiring defects before editing**

```powershell
$env:JAVA_HOME='D:\AndroidStudio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :androidApp:tasks --all --no-daemon --console=plain "-Dhttp.proxyHost=" "-Dhttps.proxyHost=" | Select-String 'compileDebugKotlin'
.\gradlew.bat :desktopApp:compileKotlinJvm --rerun-tasks --no-daemon --console=plain "-Dhttp.proxyHost=" "-Dhttps.proxyHost="
.\gradlew.bat :androidApp:compileDebugSources --no-daemon --console=plain "-Dhttp.proxyHost=" "-Dhttps.proxyHost="
```

Expected before repair: no Android Kotlin task match, Desktop says `NO-SOURCE`, and Android fails in `:shared:compileDebugKotlinAndroid` with target 21 versus 17.

- [ ] **Step 2: Add the missing pinned plugin and libraries to the version catalog**

Add these entries while retaining the existing versions:

```toml
[libraries]
ktor-client-auth = { module = "io.ktor:ktor-client-auth", version.ref = "ktor" }
androidx-lifecycle-viewmodel = { module = "androidx.lifecycle:lifecycle-viewmodel-ktx", version = "2.8.7" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version = "2.8.7" }
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "compose-navigation" }
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
koin-androidx-compose = { module = "io.insert-koin:koin-androidx-compose", version.ref = "koin" }

[plugins]
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

`ktor-client-auth` is consumed in Work Package 3; adding it here keeps build dependency ownership in one package.

- [ ] **Step 3: Apply the Kotlin Android plugin and real Compose/Koin/navigation dependencies**

Replace the Android plugin block with:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}
```

Add a Kotlin toolchain block:

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}
```

Ensure the Android dependencies include:

```kotlin
implementation(compose.material3)
implementation(compose.materialIconsExtended)
implementation(libs.androidx.lifecycle.viewmodel)
implementation(libs.androidx.lifecycle.runtime.compose)
implementation(libs.androidx.navigation.compose)
implementation(libs.koin.android)
implementation(libs.koin.androidx.compose)
```

Keep existing project, Ktor, Coil, serialization, and coroutines dependencies.

- [ ] **Step 4: Align both Shared targets to JVM 17**

In `shared/build.gradle.kts`, import `JvmTarget`, call `jvmToolchain(17)`, and configure both targets explicitly:

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

kotlin {
    jvmToolchain(17)

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // Preserve the existing sourceSets block.
}
```

Do not add the Gradle warning-suppression property for the AGP/Kotlin compatibility warning. Record the warning as a known dependency-alignment risk in Work Package 6.

- [ ] **Step 5: Make Desktop compile its actual source**

Move the existing `Main.kt` content unchanged from `desktopApp/src/main/kotlin/...` to `desktopApp/src/jvmMain/kotlin/...`. Add JVM 17 configuration:

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

kotlin {
    jvmToolchain(17)
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    // Preserve existing jvmMain dependencies.
}
```

- [ ] **Step 6: Verify the build graph, not application correctness yet**

```powershell
.\gradlew.bat :androidApp:tasks --all --no-daemon --console=plain "-Dhttp.proxyHost=" "-Dhttps.proxyHost=" | Select-String 'compileDebugKotlin'
.\gradlew.bat :shared:compileKotlinJvm :shared:compileDebugKotlinAndroid :server:compileKotlin :desktopApp:compileKotlinJvm --rerun-tasks --no-daemon --console=plain "-Dhttp.proxyHost=" "-Dhttps.proxyHost="
```

Expected:

- `compileDebugKotlin` is listed for Android.
- Shared JVM, Shared Android, Server, and Desktop compile tasks no longer report target mismatch.
- Desktop `compileKotlinJvm` does not report `NO-SOURCE`.
- Android application compilation may now expose source errors; those are the input to Work Package 2, not a reason to undo the plugin.

- [ ] **Step 7: Commit build wiring**

```powershell
git add gradle/libs.versions.toml shared/build.gradle.kts androidApp/build.gradle.kts desktopApp/build.gradle.kts desktopApp/src
git commit -m "build: compile all platform source sets on JVM 17"
```

---

## Task 2: Work Package 2 — Repair the Android Source and Produce a Real APK

**Files:**
- Modify: `androidApp/src/main/kotlin/com/ancientpoet/android/di/AppModule.kt`
- Modify: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/screen/auth/LoginViewModel.kt`
- Modify: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/screen/home/HomeViewModel.kt`
- Modify: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/screen/conversation/ConversationViewModel.kt`
- Modify: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/screen/map/MapViewModel.kt`
- Modify: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/screen/poet/PoetViewModel.kt`
- Modify: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/screen/poetry/PoetryViewModel.kt`
- Modify: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/screen/community/CommunityViewModel.kt`
- Modify when named by compiler output: `androidApp/src/main/kotlin/com/ancientpoet/android/AncientPoetApp.kt`
- Modify when named by compiler output: `androidApp/src/main/kotlin/com/ancientpoet/android/MainActivity.kt`
- Modify when named by compiler output: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/component/DrawingCanvas.kt`
- Modify when named by compiler output: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/component/SharedComponents.kt`
- Modify when named by compiler output: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/component/StorylineTimeline.kt`
- Modify when named by compiler output: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/navigation/NavGraph.kt`
- Modify when named by compiler output: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/screen/auth/LoginScreen.kt`
- Modify when named by compiler output: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/screen/community/CommunityScreen.kt`
- Modify when named by compiler output: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/screen/community/ProfileScreen.kt`
- Modify when named by compiler output: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/screen/conversation/ConversationScreen.kt`
- Modify when named by compiler output: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/screen/conversation/MessageBubble.kt`
- Modify when named by compiler output: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/screen/home/HomeScreen.kt`
- Modify when named by compiler output: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/screen/map/MapScreen.kt`
- Modify when named by compiler output: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/screen/poet/PoetScreens.kt`
- Modify when named by compiler output: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/screen/poetry/PoetryScreens.kt`
- Modify when named by compiler output: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/screen/settings/SettingsScreen.kt`
- Modify when named by compiler output: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/theme/Theme.kt`
- Modify when named by compiler output: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/theme/Type.kt`

**Interfaces:**
- Consumes: Real Android Kotlin compile task from Work Package 1.
- Produces: Lifecycle-safe ViewModels, valid Koin registrations, syntactically valid Android source, and a debug APK. Work Package 3 can then change behavior under compiler protection.

- [ ] **Step 1: Capture the complete first compiler report**

```powershell
.\gradlew.bat :androidApp:compileDebugKotlin --continue --no-daemon --console=plain "-Dhttp.proxyHost=" "-Dhttps.proxyHost=" 2>&1 | Tee-Object -FilePath build-android-errors.txt
```

Expected: source-level failures are written to `build-android-errors.txt`. Keep this file untracked and remove it after the package passes.

- [ ] **Step 2: Correct the known `CommunityViewModel` class-boundary error first**

The current file closes the class before `loadProfile`. Place `loadProfile` inside `CommunityViewModel` and leave exactly one class-closing brace after it:

```kotlin
    fun loadProfile(userId: Long) {
        viewModelScope.launch {
            // Work Package 3 replaces the raw request and silent catch.
        }
    }
}
```

Do not keep the current orphaned function between two closing braces.

- [ ] **Step 3: Convert all eight ViewModels to AndroidX lifecycle ownership**

For every `*ViewModel.kt` listed above:

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

class HomeViewModel(/* existing dependencies */) : ViewModel() {
    // Remove CoroutineScope(Dispatchers.Main).
    // Replace scope.launch with viewModelScope.launch.
}
```

Apply the same change to Login, Conversation, Map, Poet, Poetry, and Community. This eliminates coroutines that outlive their screens and makes Koin’s `viewModel { ... }` registrations type-correct.

- [ ] **Step 4: Keep dependency injection consistent**

`AppModule.kt` must use:

```kotlin
import org.koin.androidx.viewmodel.dsl.viewModel
```

Screens must use:

```kotlin
import org.koin.androidx.compose.koinViewModel
```

Do not mix manual ViewModel construction with Koin injection. Keep one Koin definition for each of the eight ViewModels.

- [ ] **Step 5: Replace non-lifecycle-aware state collection in screens**

In each screen that currently calls `collectAsState()`, import and use:

```kotlin
import androidx.lifecycle.compose.collectAsStateWithLifecycle

val state by viewModel.state.collectAsStateWithLifecycle()
```

This applies to Login, Home, Conversation, Map, Poet, Poetry, Community, Post Detail, and Profile composables where a `StateFlow` is collected.

- [ ] **Step 6: Resolve compiler errors by category without weakening behavior**

Repeat `:androidApp:compileDebugKotlin --continue` after each category:

1. Syntax and brace errors.
2. Missing imports caused by enabling the Kotlin compiler.
3. Missing Compose/Koin/navigation dependencies; add only the dependency that owns the unresolved symbol.
4. Incorrect Material 3 API names or Kotlin 2.0 type inference; update the call site without removing the UI.
5. DTO serialization/type mismatches; align client DTO fields to the server DTO definitions under `server/src/main/kotlin/com/ancientpoet/server/model/dto`.
6. Navigation argument parsing errors that prevent compilation; preserve route names for Work Package 3’s behavioral cleanup.

After each run, reduce the compiler error count. Never comment out an entire screen or replace it with an empty composable.

- [ ] **Step 7: Produce and inspect the debug APK**

```powershell
.\gradlew.bat :androidApp:assembleDebug --no-daemon --console=plain "-Dhttp.proxyHost=" "-Dhttps.proxyHost="
$apk = Get-Item 'androidApp\build\outputs\apk\debug\androidApp-debug.apk'
$apk | Select-Object FullName,Length,LastWriteTime
if ($apk.Length -le 0) { throw 'APK is empty' }
```

Expected: build exit code 0 and a non-empty APK.

- [ ] **Step 8: Remove the temporary compiler log and commit**

```powershell
Remove-Item -LiteralPath 'build-android-errors.txt' -ErrorAction SilentlyContinue
git add androidApp/src/main/kotlin androidApp/build.gradle.kts gradle/libs.versions.toml
git commit -m "fix: restore Android compile baseline"
```

**Gate A acceptance:** Server, Shared JVM, Shared Android, Desktop, and Android source all compile; Desktop is not `NO-SOURCE`; Android produces a debug APK.

---

## Task 3: Work Package 3 — Centralize API Access, Authentication, Session Persistence, and UI Errors

**Files:**
- Create: `shared/src/commonMain/kotlin/com/ancientpoet/shared/auth/AuthSession.kt`
- Create: `shared/src/commonMain/kotlin/com/ancientpoet/shared/data/api/ApiResult.kt`
- Modify: `shared/src/commonMain/kotlin/com/ancientpoet/shared/data/api/ApiClient.kt`
- Modify: `shared/build.gradle.kts`
- Create: `androidApp/src/main/kotlin/com/ancientpoet/android/data/session/SharedPreferencesSessionStore.kt`
- Create: `androidApp/src/main/kotlin/com/ancientpoet/android/data/session/SessionController.kt`
- Create: `androidApp/src/main/kotlin/com/ancientpoet/android/data/network/HttpClientFactory.kt`
- Create: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/session/SessionViewModel.kt`
- Modify: `androidApp/build.gradle.kts`
- Create: `androidApp/src/main/res/xml/backup_rules.xml`
- Create: `androidApp/src/main/res/xml/data_extraction_rules.xml`
- Modify: `androidApp/src/main/AndroidManifest.xml`
- Modify: `androidApp/src/main/kotlin/com/ancientpoet/android/di/AppModule.kt`
- Modify: all eight Android ViewModels from Work Package 2
- Modify: `androidApp/src/main/kotlin/com/ancientpoet/android/MainActivity.kt`
- Modify: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/navigation/NavGraph.kt`
- Modify: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/screen/auth/LoginScreen.kt`
- Modify: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/screen/home/HomeScreen.kt`
- Modify: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/screen/conversation/ConversationScreen.kt`
- Modify: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/screen/map/MapScreen.kt`
- Modify: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/screen/poet/PoetScreens.kt`
- Modify: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/screen/poetry/PoetryScreens.kt`
- Modify: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/screen/community/CommunityScreen.kt`
- Modify: `androidApp/src/main/kotlin/com/ancientpoet/android/ui/screen/community/ProfileScreen.kt`
- Test: `shared/src/commonTest/kotlin/com/ancientpoet/shared/data/api/ApiResultTest.kt`
- Test: `androidApp/src/test/kotlin/com/ancientpoet/android/data/network/AuthClientTest.kt`

**Interfaces:**
- Consumes: Compiling Android source and existing server endpoints `/auth/sms/send`, `/auth/sms/verify`, `/auth/refresh`.
- Produces:
  - `data class AuthTokens(val accessToken: String, val refreshToken: String, val userId: Long)`
  - `interface SessionStore { suspend fun load(): AuthTokens?; suspend fun save(tokens: AuthTokens); suspend fun clear() }`
  - `sealed interface ApiResult<out T>` with success and typed failure cases.
  - One configured authenticated `HttpClient` with Bearer load/refresh behavior.

- [ ] **Step 1: Write the shared session contract and result tests first**

Before writing the tests, add the minimum test/runtime dependencies required by this package:

```kotlin
// shared/build.gradle.kts, inside kotlin { sourceSets { ... } }
commonMain.dependencies {
    implementation(libs.ktor.client.auth)
}
commonTest.dependencies {
    implementation(libs.kotlin.test)
    implementation(libs.kotlinx.coroutines.test)
    implementation(libs.ktor.client.mock)
}
```

```kotlin
// androidApp/build.gradle.kts
dependencies {
    implementation(libs.ktor.client.auth)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
}
```

Create tests that assert typed results carry data and retry semantics:

```kotlin
class ApiResultTest {
    @Test
    fun successCarriesValue() {
        val result: ApiResult<Int> = ApiResult.Success(42)
        assertEquals(42, (result as ApiResult.Success<Int>).value)
    }

    @Test
    fun networkFailureIsRetryable() {
        val result: ApiResult<Nothing> = ApiResult.Failure(
            kind = ApiErrorKind.NETWORK,
            message = "网络连接失败",
            retryable = true,
        )
        assertTrue((result as ApiResult.Failure).retryable)
    }

    @Test
    fun unauthorizedFailureIsNotSilentlyConvertedToEmptyData() {
        val result: ApiResult<Nothing> = ApiResult.Failure(
            kind = ApiErrorKind.UNAUTHORIZED,
            message = "登录状态已失效",
            retryable = false,
        )
        assertEquals(ApiErrorKind.UNAUTHORIZED, (result as ApiResult.Failure).kind)
    }
}
```

Run `:shared:jvmTest`; expected initial failure because the types do not yet exist.

- [ ] **Step 2: Implement the shared session and result contracts**

`AuthSession.kt`:

```kotlin
package com.ancientpoet.shared.auth

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val userId: Long,
)

interface SessionStore {
    suspend fun load(): AuthTokens?
    suspend fun save(tokens: AuthTokens)
    suspend fun clear()
}
```

`ApiResult.kt`:

```kotlin
package com.ancientpoet.shared.data.api

enum class ApiErrorKind { NETWORK, UNAUTHORIZED, VALIDATION, SERVER, UNKNOWN }

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(
        val kind: ApiErrorKind,
        val message: String,
        val retryable: Boolean,
        val statusCode: Int? = null,
    ) : ApiResult<Nothing>
}
```

Run `:shared:jvmTest`; the three tests must pass.

- [ ] **Step 3: Implement Android session persistence without backup leakage**

Use a dedicated private preferences file named `auth_session`, storing only access token, refresh token, and user ID. Android’s `EncryptedSharedPreferences` is deprecated; use app-private `SharedPreferences` and explicitly exclude this file from cloud/device transfer backup.

The store must implement the shared `SessionStore` contract and synchronize access with a `Mutex` so concurrent refresh/logout cannot interleave.

Add backup exclusions for `auth_session.xml` in both XML rule files and reference them from the manifest:

```xml
<application
    android:fullBackupContent="@xml/backup_rules"
    android:dataExtractionRules="@xml/data_extraction_rules"
    ...>
```

The rules must exclude the shared-preferences domain path `auth_session.xml` for cloud backup and device transfer.

- [ ] **Step 4: Write Ktor MockEngine tests for authentication behavior**

Add Android/JVM tests covering these exact sequences:

1. A protected request includes `Authorization: Bearer access-old`.
2. A `401` triggers exactly one `/auth/refresh` request.
3. The refresh request is marked as a refresh-token request and does not recurse on another `401`.
4. A successful refresh stores `access-new`, preserves the current refresh token, and retries the original request once.
5. A failed refresh clears the session and returns an unauthorized failure.
6. Logout clears both persistent tokens and Ktor’s bearer-token cache.

Use Ktor `MockEngine`; do not call the live server from unit tests.

`HttpClientFactory` must accept an optional `HttpClientEngine` (or an engine factory) so tests can inject `MockEngine` while production uses the existing OkHttp engine. Do not branch on a global “test mode”.

- [ ] **Step 5: Build one authenticated client using Ktor’s Bearer provider**

`HttpClientFactory.kt` must install:

```kotlin
install(ContentNegotiation) {
    json(Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true })
}
install(DefaultRequest) {
    url(BuildConfig.API_BASE_URL)
    contentType(ContentType.Application.Json)
}
install(Auth) {
    bearer {
        loadTokens {
            sessionStore.load()?.let { BearerTokens(it.accessToken, it.refreshToken) }
        }
        refreshTokens {
            val current = sessionStore.load() ?: return@refreshTokens null
            val refreshed: RefreshResponse = client.post("auth/refresh") {
                markAsRefreshTokenRequest()
                setBody(RefreshRequest(current.refreshToken))
            }.body()
            val updated = current.copy(accessToken = refreshed.accessToken)
            sessionStore.save(updated)
            BearerTokens(updated.accessToken, updated.refreshToken)
        }
        sendWithoutRequest { request ->
            request.url.encodedPath.startsWith("/api/v1/") &&
                !request.url.encodedPath.startsWith("/api/v1/auth/")
        }
    }
}
```

Use `markAsRefreshTokenRequest()` to prevent recursive refresh. Keep the version compatible with Ktor 3.0.3; do not upgrade the Ktor family in this repair branch.

Define `BuildConfig.API_BASE_URL` in `androidApp/build.gradle.kts`, defaulting to `http://10.0.2.2:8080/api/v1/` and overridable by Gradle property `ANCIENT_POET_API_BASE_URL`:

```kotlin
val apiBaseUrl = providers.gradleProperty("ANCIENT_POET_API_BASE_URL")
    .orElse("http://10.0.2.2:8080/api/v1/")

android {
    buildFeatures {
        compose = true
        buildConfig = true
    }
    defaultConfig {
        buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl.get()}\"")
    }
}
```

- [ ] **Step 6: Make `AncientPoetApi` the only request gateway used by ViewModels**

Update `ApiClient.kt` so `AncientPoetApi` accepts the configured client and exposes relative-path `get`, `post`, `put`, and `delete` operations. Map:

- `IOException`/connection errors → `NETWORK`, retryable.
- HTTP 400/422 → `VALIDATION`, not retryable until input changes.
- HTTP 401 → `UNAUTHORIZED`, clear session after refresh has failed.
- HTTP 500–599 → `SERVER`, retryable.
- Unexpected exceptions → `UNKNOWN`, not automatically retryable.

After refactoring all ViewModels, this command must return no matches:

```powershell
Get-ChildItem androidApp\src\main\kotlin -Recurse -Filter *.kt |
  Select-String 'http://10\.0\.2\.2:8080|HttpClient\('
```

The only `HttpClient` construction belongs in `HttpClientFactory.kt`.

- [ ] **Step 7: Persist login, restore session, and implement logout**

- Implement `SessionController(SessionStore, HttpClient)` as the only writer/clearer of session state. `save(tokens)` persists tokens and clears Ktor’s cached bearer value so the next protected request reloads the new session. `logout()` clears storage and the bearer cache.
- Implement `SessionViewModel(SessionController)` with `StateFlow<SessionStatus>` where the states are `Checking`, `Authenticated`, and `Anonymous`. It loads the persisted session once in `viewModelScope` during startup and exposes `logout()`.
- `LoginViewModel.verifySms` passes both tokens and user ID to `SessionController.save` before reporting success.
- `MainActivity` observes `SessionViewModel`: show the existing paper-background loading treatment during `Checking`, then create `NavGraph(startDestination = "home")` for `Authenticated` or `NavGraph(startDestination = "login")` for `Anonymous`.
- `NavGraph` accepts `startDestination` and `SessionViewModel`; logout calls `SessionViewModel.logout()` and navigates to login with the authenticated back stack removed.
- The refresh token never appears in UI state, logs, exceptions, or analytics.

Do not keep `accessToken` or `refreshToken` fields in Compose screen state.

- [ ] **Step 8: Replace silent failures with visible, retryable state**

For each ViewModel state, add:

```kotlin
val isLoading: Boolean = false
val errorMessage: String? = null
val canRetry: Boolean = false
```

Each request follows `loading → success` or `loading → error`; no `catch (_: Exception) {}` remains. Screens display the Chinese error message and a “重试” action when `canRetry=true`. Authentication expiry navigates to login rather than showing an empty list.

Verify:

```powershell
Get-ChildItem androidApp\src\main\kotlin -Recurse -Filter *.kt |
  Select-String 'catch\s*\(_:\s*Exception\)\s*\{\s*\}'
```

Expected: no matches.

- [ ] **Step 9: Correct the broken new-conversation route under the centralized API**

The current navigation sends `conversation/new/{poetId}` into a route that parses the segment as a numeric conversation ID. Define a distinct route and API action:

```text
conversation/new/{poetId}?year={year}
```

On entry, call `POST /conversations` with the selected poet and mode/year data, then replace the creation destination with `conversation/{createdId}`. Do not treat `new` as a `Long`. Preserve the selected storyline year instead of discarding it.

- [ ] **Step 10: Run unit, compile, and APK verification**

```powershell
.\gradlew.bat :shared:jvmTest :androidApp:testDebugUnitTest :androidApp:assembleDebug --no-daemon --console=plain "-Dhttp.proxyHost=" "-Dhttps.proxyHost="
```

Expected: real tests execute, refresh tests pass, and APK assembly succeeds.

- [ ] **Step 11: Commit the authentication boundary**

```powershell
git add shared androidApp
git commit -m "feat: centralize authenticated API sessions"
```

**Gate B acceptance:** Login persists a session, protected calls send Bearer tokens, one synchronized refresh retries a failed request, logout clears storage/cache, absolute emulator URLs are centralized, and request failures are visible instead of becoming empty screens.

---

## Task 4: Work Package 4 — Add a Repeatable Docker and API Integration Smoke Workflow

**Files:**
- Modify: `deploy/docker-compose.yml`
- Create: `deploy/.env.test.example`
- Create: `scripts/smoke.ps1`
- Create: `server/src/main/kotlin/com/ancientpoet/server/route/HealthRoute.kt`
- Create: `server/src/main/kotlin/com/ancientpoet/server/service/ReadinessChecker.kt`
- Modify: `server/src/main/kotlin/com/ancientpoet/server/Application.kt`
- Modify: `server/src/main/kotlin/com/ancientpoet/server/di/ServerModule.kt`
- Test: `server/src/test/kotlin/com/ancientpoet/server/route/HealthRouteTest.kt`

**Interfaces:**
- Consumes: Compiling server, working dev authentication, Docker Compose infrastructure.
- Produces: `GET /api/v1/health/live`, `GET /api/v1/health/ready`, and one PowerShell smoke command with cleanup in `finally`.

- [ ] **Step 1: Remove obsolete Compose syntax and add stable test defaults**

Delete the top-level `version: '3.8'` line; modern Docker Compose ignores it and emits a warning. Keep named volumes and existing services.

Create `deploy/.env.test.example` with non-production values:

```dotenv
POSTGRES_DB=ancientpoet
POSTGRES_USER=ancientpoet
POSTGRES_PASSWORD=ancientpoet_test
POSTGRES_PORT=5432
REDIS_PORT=6379
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
JWT_SECRET=ancientpoet-local-test-secret-at-least-32-chars
APP_PORT=8080
```

This file contains only local test defaults and is safe to track. Runtime `.env` remains ignored.

- [ ] **Step 2: Write health-route tests before implementation**

Define the route boundary in `ReadinessChecker.kt`:

```kotlin
data class ReadinessReport(
    val postgres: Boolean,
    val redis: Boolean,
    val minio: Boolean,
) {
    val ready: Boolean get() = postgres && redis && minio
}

fun interface ReadinessChecker {
    suspend fun check(): ReadinessReport
}
```

Test contracts using a fake implementation and direct route wiring, without Koin or real infrastructure:

```kotlin
private class FakeReadinessChecker(
    private val report: ReadinessReport,
) : ReadinessChecker {
    override suspend fun check(): ReadinessReport = report
}

@Test
fun liveEndpointReturnsOk() = testApplication {
    application {
        configureSerialization()
        routing {
            route("/api/v1") {
                healthRoute(FakeReadinessChecker(ReadinessReport(true, true, true)))
            }
        }
    }
    val response = client.get("/api/v1/health/live")
    assertEquals(HttpStatusCode.OK, response.status)
}

@Test
fun readyEndpointReturnsServiceUnavailableWhenRedisFails() = testApplication {
    application {
        configureSerialization()
        routing {
            route("/api/v1") {
                healthRoute(FakeReadinessChecker(ReadinessReport(true, false, true)))
            }
        }
    }
    val response = client.get("/api/v1/health/ready")
    assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
}
```

Keep health dependencies behind a small interface so route tests do not initialize a real database.

- [ ] **Step 3: Implement live and ready endpoints**

- `/health/live`: confirms the Ktor process is alive; no external calls.
- `/health/ready`: runs bounded checks for PostgreSQL `SELECT 1`, Redis `PING`, and MinIO `bucketExists`/client reachability.
- Return HTTP 200 only when every required dependency is ready.
- Return HTTP 503 with component statuses when any check fails.
- Never include passwords, endpoints containing credentials, stack traces, or tokens in the response.

Implement `InfrastructureReadinessChecker` in `ReadinessChecker.kt` using `withContext(Dispatchers.IO)`, Exposed `transaction { exec("SELECT 1") }`, `RedisConfig.pool.resource.use { it.ping() }`, and a MinIO client built from `AppConfig`. Register it as `single<ReadinessChecker> { InfrastructureReadinessChecker(get()) }` in `ServerModule`, obtain it from Koin in `Application.kt`, and mount `healthRoute(readinessChecker)` under `/api/v1`.

- [ ] **Step 4: Implement `scripts/smoke.ps1` with deterministic lifecycle handling**

The script must:

1. Resolve the repository root from `$PSScriptRoot`.
2. Verify Docker daemon availability. If Docker Desktop exists but is stopped, launch `C:\Program Files\Docker\Docker\Docker Desktop.exe` with `-WindowStyle Hidden` and wait up to a bounded timeout.
3. Copy `deploy/.env.test.example` to ignored `deploy/.env` only when `.env` does not already exist; never overwrite user credentials.
4. Run `docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d`.
5. Wait until PostgreSQL, Redis, and MinIO are healthy; print `docker compose ps` on timeout.
6. Build the server fat JAR with `:server:buildFatJar`.
7. Start the JAR using `D:\AndroidStudio\jbr\bin\java.exe` in a hidden process, redirecting stdout/stderr to ignored log files.
8. Set `KTOR_DEVELOPMENT=true` plus local Postgres/Redis/MinIO/JWT environment variables for the server process.
9. Poll `/api/v1/health/ready` until HTTP 200.
10. Call SMS send and verify with development code `123456` to obtain real access and refresh tokens.
11. Call `GET /api/v1/conversations` using the access token and require HTTP 200.
12. Call `GET /api/v1/poets` and require 15 poet records.
13. Call `GET /api/v1/map/tang/cities` and require a non-empty list.
14. In `finally`, stop the server process and run `docker compose stop`. Do not use `down -v`; preserve named-volume data.

The script exits non-zero on the first failed assertion and prints the failed step without printing tokens.

- [ ] **Step 5: Run the smoke workflow twice**

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\smoke.ps1
```

Expected: both runs exit 0. The second run proves idempotence with existing containers, volumes, migrations, and MinIO bucket state.

- [ ] **Step 6: Commit integration tooling**

```powershell
git add deploy scripts server/src/main server/src/test
git commit -m "test: add local infrastructure smoke workflow"
```

**Gate C acceptance:** A single command validates real Flyway migrations, PostgreSQL, Redis, MinIO readiness, dev login, token issuance, authenticated API access, poet seed data, and map access; cleanup preserves user data.

---

## Task 5: Work Package 5 — Add Core Automated Tests and Continuous Integration

**Files:**
- Modify: `shared/build.gradle.kts`
- Modify: `server/build.gradle.kts`
- Modify: `androidApp/build.gradle.kts`
- Test: `server/src/test/kotlin/com/ancientpoet/server/service/DelayCalculationServiceTest.kt`
- Test: `server/src/test/kotlin/com/ancientpoet/server/plugin/JwtTokenTest.kt`
- Test: `shared/src/commonTest/kotlin/com/ancientpoet/shared/util/DistanceUtilTest.kt`
- Test: `shared/src/commonTest/kotlin/com/ancientpoet/shared/domain/usecase/CalculateDelayUseCaseTest.kt`
- Retain and extend tests created in Work Packages 3 and 4
- Create: `.github/workflows/quality.yml`

**Interfaces:**
- Consumes: Stable build, authentication boundary, and testable health route.
- Produces: Non-empty test suites and a CI-required quality command.

- [ ] **Step 1: Configure test source-set dependencies**

Shared `commonTest` requires `kotlin-test`, coroutines test, and Ktor MockEngine where used. Android local tests require Kotlin/JUnit support and Ktor MockEngine. Server retains Ktor test host and Kotlin test. Keep all Ktor artifacts on version 3.0.3.

Verify Gradle discovers test tasks before adding tests:

```powershell
.\gradlew.bat :shared:tasks :server:tasks :androidApp:tasks --all --no-daemon --console=plain "-Dhttp.proxyHost=" "-Dhttps.proxyHost=" | Select-String 'jvmTest|server:test|testDebugUnitTest'
```

- [ ] **Step 2: Add deterministic delay-calculation tests**

Cover these exact behaviors:

```kotlin
@Test fun samePointClampsToTwoHours()
@Test fun settledCoefficientReducesUnclampedDelayByTwentyPercent()
@Test fun warMultiplierIncreasesDelayByFiftyPercent()
@Test fun veryLongDistanceClampsToSevenDays()
@Test fun haversineDistanceIsSymmetric()
```

Use tolerances for floating-point distance and exact seconds for clamped durations. Never assert against current time.

- [ ] **Step 3: Add Shared distance/use-case tests**

Cover:

- Identical coordinates return 0 km.
- Beijing to Shanghai is approximately 1,068 km within a reasonable tolerance.
- Distance is symmetric.
- `<30 km` maps to 2 hours.
- `30–149.999 km` maps to 24 hours.
- `>=150 km` maps to proportional `distance / 80 * 24` hours.

These tests protect the client preview from diverging further from the server formula.

- [ ] **Step 4: Add JWT tests with a fixed test configuration**

Verify:

- Access token contains `userId`, issuer, audience, and no `type=refresh` claim.
- Refresh token contains `type=refresh`.
- Refresh verification rejects an access token.
- Wrong issuer, audience, or signature is rejected.
- Expired tokens are rejected using a deliberately short/expired configuration rather than sleeping.

No production secret may appear in test fixtures.

- [ ] **Step 5: Prove every test task executes tests rather than `NO-SOURCE`**

```powershell
.\gradlew.bat :shared:jvmTest :server:test :androidApp:testDebugUnitTest --rerun-tasks --no-daemon --console=plain "-Dhttp.proxyHost=" "-Dhttps.proxyHost="
```

Expected: each task executes test classes; none of the three target tasks reports `NO-SOURCE`; zero failures.

- [ ] **Step 6: Create the CI quality workflow**

`.github/workflows/quality.yml` must:

- Trigger on pull requests and pushes to `master` and `codex/**`.
- Use `ubuntu-latest`.
- Use `actions/checkout@v4`, `actions/setup-java@v4` with Temurin 17, and `gradle/actions/setup-gradle@v4`.
- Validate the Gradle wrapper.
- Run tests first.
- Compile Server, Shared JVM, Desktop, and assemble Android debug.
- Upload Android test reports and APK only when useful for debugging; do not upload secrets or Gradle caches as artifacts.
- Cancel superseded runs for the same branch through a concurrency group.

Core CI commands:

```bash
./gradlew :shared:jvmTest :server:test :androidApp:testDebugUnitTest --stacktrace
./gradlew :shared:compileKotlinJvm :server:compileKotlin :desktopApp:compileKotlinJvm :androidApp:assembleDebug --stacktrace
```

- [ ] **Step 7: Validate workflow syntax and local parity**

Run the same Gradle tasks locally. Inspect the YAML for tabs and malformed indentation. If GitHub CLI is authenticated and a remote run is explicitly authorized, push and inspect Actions; otherwise stop at local validation and report that remote CI execution remains unverified.

- [ ] **Step 8: Commit tests and CI**

```powershell
git add shared server androidApp .github/workflows/quality.yml
git commit -m "test: establish core quality gates"
```

**Gate D acceptance:** Core delay, distance, JWT, session, refresh, and health behavior has deterministic tests; test tasks are not `NO-SOURCE`; CI encodes the same build and test baseline.

---

## Task 6: Work Package 6 — Clean Generated Artifacts and Make Documentation Truthful

**Files:**
- Modify: `.gitignore`
- Remove from source: `server/bin/**`
- Delete if still unused: `server/src/main/kotlin/com/ancientpoet/server/push/FCMClient.kt`
- Modify: `gradle/libs.versions.toml` to remove unused Firebase entries only after reference scan
- Modify: `server/build.gradle.kts` to remove unused Firebase client dependency only after reference scan
- Modify: `deploy/docker-compose.yml`
- Modify: `README.md`
- Modify: `ARCHITECTURE.md`
- Modify: `CLAUDE.md`
- Create: `docs/STATUS.md`
- Modify: `docs/reports/foundation-baseline.md` with final comparison

**Interfaces:**
- Consumes: Verified implementation and test results from Work Packages 1–5.
- Produces: Clean source tree, accurate onboarding instructions, explicit remaining gaps, and final handoff evidence.

- [ ] **Step 1: Prove `server/bin` is generated/copied content before removing it**

```powershell
git ls-files 'server/bin/**'
git diff --no-index --stat server/src/main server/bin/main
```

Inspect differences. Preserve any unique source/resource content by moving it into the correct `server/src/main` path before removal. Only after the comparison proves it is duplicate/generated content, remove tracked `server/bin` and add this ignore rule:

```gitignore
# Generated server output
server/bin/
```

- [ ] **Step 2: Remove stale FCM compatibility code only after a reference scan**

```powershell
git grep -n -E 'FCMClient|firebase\.messaging|firebase-messaging|firebase-admin'
```

If the only source reference is the no-op `FCMClient` stub and unused catalog/build entries, delete the stub and remove those unused dependencies. Keep JPush code and environment names unchanged. If a live reference remains, preserve it and document the exact dependency instead of deleting it.

- [ ] **Step 3: Create an honest status document**

`docs/STATUS.md` must separate:

1. Verified working: compile/test/smoke evidence.
2. Implemented but credential-gated: DeepSeek, SMS provider, JPush.
3. Partial product behavior: map endpoint currently hardcodes Tang city data; poetry seed contains 14 poems; profile post list is incomplete; portrait assets are absent.
4. Not present: `data/maps`, `data/poems`, full Desktop client, Web client, persistent SQLDelight driver wiring.
5. Known dependency risk: Kotlin 2.0.21 officially warns that AGP 8.7.2 exceeds its maximum-tested AGP 8.5 range.

Do not mark a phase complete merely because route or screen files exist.

- [ ] **Step 4: Correct onboarding and architecture documentation**

Update:

- `README.md`: exact Windows setup, real Android SDK location pattern, launcher JDK requirements, build commands, smoke command, test command, and link to `docs/STATUS.md`.
- `ARCHITECTURE.md`: JPush instead of FCM in diagrams/flows, actual data availability, current endpoint behavior, and verified module status.
- `CLAUDE.md`: remove the false “all four modules compile” historical claim and replace it with the newly verified commands and remaining gaps.
- `deploy/docker-compose.yml`: retain the modern Compose format without obsolete `version`.

Documentation must not claim generated portraits, full map assets, full poetry dataset, SQLDelight persistence, or Phase 4 clients exist.

- [ ] **Step 5: Update the baseline report with before/after evidence**

Append a final table to `docs/reports/foundation-baseline.md` containing:

- Before and after build results for each module.
- Before and after test counts.
- Smoke workflow result and date.
- Remaining credential-gated checks.
- Exact commit SHAs for Work Packages 1–6.

- [ ] **Step 6: Run the complete final verification from a clean Gradle invocation**

```powershell
$env:JAVA_HOME='D:\AndroidStudio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"

.\gradlew.bat :shared:jvmTest :server:test :androidApp:testDebugUnitTest --rerun-tasks --no-daemon --console=plain "-Dhttp.proxyHost=" "-Dhttps.proxyHost="
.\gradlew.bat :shared:compileKotlinJvm :shared:compileDebugKotlinAndroid :server:compileKotlin :desktopApp:compileKotlinJvm :androidApp:assembleDebug --rerun-tasks --no-daemon --console=plain "-Dhttp.proxyHost=" "-Dhttps.proxyHost="
powershell -ExecutionPolicy Bypass -File .\scripts\smoke.ps1
git diff --check
git status --short
git ls-files 'server/bin/**'
git ls-files '*/src/*Test/**'
```

Expected:

- All test and compile commands exit 0.
- Android APK exists and is non-empty.
- Smoke workflow exits 0.
- `git diff --check` has no output.
- `git ls-files 'server/bin/**'` has no output.
- Test file listing is non-empty.
- `git status --short` contains only the intentional Work Package 6 changes before commit.

- [ ] **Step 7: Commit cleanup and truthful documentation**

```powershell
git add .gitignore README.md ARCHITECTURE.md CLAUDE.md docs deploy server gradle/libs.versions.toml
git commit -m "chore: align repository artifacts and documentation"
```

- [ ] **Step 8: Verify the committed state and stop before publication**

```powershell
git status --short --branch
git log --oneline --decorate -7
git diff master...HEAD --stat
```

Expected: clean `codex/foundation-repair` worktree with the staged sequence of reviewable commits. Report results to the user. Do not push or open a pull request without explicit authorization.

**Gate E acceptance:** The codebase, tests, smoke workflow, CI definition, tracked artifacts, and documentation tell the same story.

---

## Definition of Done

Luna may report this repair plan complete only when every statement below is supported by fresh command output:

- [ ] All four modules compile real source; Desktop is not `NO-SOURCE` and Android has a real Kotlin compile task.
- [ ] `androidApp-debug.apk` is produced and non-empty.
- [ ] Android login persists access/refresh tokens and authenticated requests use Bearer auth.
- [ ] A 401 causes one refresh attempt and one retry; refresh failure clears the session.
- [ ] Logout clears persistent session data and Ktor’s token cache.
- [ ] No ViewModel contains a manually owned `CoroutineScope(Dispatchers.Main)`.
- [ ] No Android network failure is swallowed by an empty exception handler.
- [ ] Docker smoke passes twice without deleting named volumes.
- [ ] Shared, Server, and Android test tasks execute real tests with zero failures.
- [ ] CI workflow contains the same test/build gates.
- [ ] `server/bin` is absent from tracked files and ignored.
- [ ] Documentation lists partial and credential-gated behavior honestly.
- [ ] Worktree is clean after the final commit.

## Escalation Boundaries for Luna

Stop and request a Sol/product decision rather than guessing if any of these occur:

- A fix requires changing the product’s slow-letter delay formula or minimum/maximum delays.
- Client/server DTOs imply a breaking public API change rather than a field-name correction.
- Storyline year semantics cannot be preserved without defining new product behavior.
- Token persistence requirements demand a formal threat model beyond app-private storage and backup exclusion.
- Map hardcoding, missing map artwork, the 14-poem seed, or portrait generation would need new content/design decisions.
- Dependency repair requires upgrading Kotlin, AGP, Compose, Ktor, or Koin as a family rather than adding a missing artifact.
- Docker smoke would overwrite an existing user `.env`, delete named volumes, or require real external credentials.
- Existing uncommitted user changes overlap the planned files.

## Sol High Reserved Scope After This Plan

The following remain with Sol High for overall planning/design and are not implementation tasks for this branch:

- Product roadmap and honest Phase definitions after the baseline is repaired.
- Full information architecture and navigation redesign.
- Ancient map art/data pipeline and accurate multi-dynasty interaction design.
- Poetry corpus scope, provenance, search, appreciation, and content-quality policy.
- AI poet prompt/memory evaluation design and model-provider strategy.
- Offline-first SQLDelight architecture and cross-platform synchronization.
- Desktop/Web product scope and Compose Multiplatform strategy.
- JPush/SMS/DeepSeek production credential rollout, privacy threat model, observability, and monetization.
- Visual refinement against `DESIGN/` after functional stability is proven.

## Reference Notes for the Executor

- [Ktor’s official Bearer client documentation](https://ktor.io/docs/client-bearer-auth.html) covers `loadTokens`, synchronized `refreshTokens`, and `markAsRefreshTokenRequest`; its API reference also exposes token-cache clearing. Use the API available in pinned Ktor 3.0.3 rather than upgrading the framework.
- [Android’s official `EncryptedSharedPreferences` reference](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences) marks the API deprecated. This plan uses app-private `SharedPreferences` plus explicit backup exclusion and reserves a stronger Keystore-backed design for a formal security review.
- Build success is not equivalent to test success, and a test task reporting `NO-SOURCE` is not a passing test suite.
