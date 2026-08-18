# Foundation Baseline — 2026-08-18

This is the immutable pre-repair evidence for the `codex/foundation-repair`
worktree. The Gradle commands used the Android Studio launcher JDK rather than
the machine default Java 8, and direct dependency proxy properties were
cleared for the local invocation.

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

## Repository verification

The isolated worktree was verified with the following commands:

```powershell
git -C D:\AncientPoet\.worktrees\foundation-repair remote -v
git -C D:\AncientPoet\.worktrees\foundation-repair status --short --branch --untracked-files=all
git -C D:\AncientPoet\.worktrees\foundation-repair rev-list --left-right --count master...origin/master
git -C D:\AncientPoet\.worktrees\foundation-repair branch --show-current
```

Key output before this report was added:

```text
origin  https://github.com/ghostLLC/AncientPoet.git (fetch)
origin  https://github.com/ghostLLC/AncientPoet.git (push)
## codex/foundation-repair
?? docs/superpowers/plans/2026-08-18-foundation-repair.md
0       0
codex/foundation-repair
```

The controller-created branch was already active, so the redundant
`git switch -c` step was intentionally skipped. The only initial untracked
path was the approved execution plan.

## Build evidence

All Gradle commands below used:

```powershell
$env:JAVA_HOME='D:\AndroidStudio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

### Server Kotlin

Command:

```powershell
.\gradlew.bat :server:compileKotlin --rerun-tasks --no-daemon --console=plain "-Dhttp.proxyHost=" "-Dhttps.proxyHost="
```

Result: exit `0`, `:server:compileKotlin` completed, `BUILD SUCCESSFUL`.

### Shared JVM

Command:

```powershell
.\gradlew.bat :shared:compileKotlinJvm --rerun-tasks --no-daemon --console=plain "-Dhttp.proxyHost=" "-Dhttps.proxyHost="
```

Result: exit `0`, `:shared:compileKotlinJvm` completed, `BUILD SUCCESSFUL`.

### Shared Android target mismatch

Command:

```powershell
.\gradlew.bat :shared:compileDebugKotlinAndroid --rerun-tasks --no-daemon --console=plain "-Dhttp.proxyHost=" "-Dhttps.proxyHost="
```

Result: exit `1`:

```text
Execution failed for task ':shared:compileDebugKotlinAndroid'.
Inconsistent JVM-target compatibility detected for tasks
'compileDebugJavaWithJavac' (17) and 'compileDebugKotlinAndroid' (21).
BUILD FAILED
```

### Android application wiring

Task discovery command:

```powershell
.\gradlew.bat :androidApp:tasks --all --no-daemon --console=plain "-Dhttp.proxyHost=" "-Dhttps.proxyHost=" | Select-String 'compileDebugKotlin'
```

Result: exit `0` with no matching output; no Android Kotlin compile task was
registered before repair.

The source aggregate was also attempted:

```powershell
.\gradlew.bat :androidApp:compileDebugSources --rerun-tasks --no-daemon --console=plain "-Dhttp.proxyHost=" "-Dhttps.proxyHost="
```

Result: exit `1` because the dependency graph reached the same shared target
mismatch shown above. This does not establish an Android application Kotlin
compile task.

### Desktop

Command:

```powershell
.\gradlew.bat :desktopApp:compileKotlinJvm --rerun-tasks --no-daemon --console=plain "-Dhttp.proxyHost=" "-Dhttps.proxyHost="
```

Result: exit `0`, but the task reported:

```text
> Task :desktopApp:compileKotlinJvm NO-SOURCE
BUILD SUCCESSFUL
```

### Tests

Commands:

```powershell
.\gradlew.bat :shared:jvmTest :server:test --rerun-tasks --no-daemon --console=plain "-Dhttp.proxyHost=" "-Dhttps.proxyHost="
.\gradlew.bat :androidApp:testDebugUnitTest --no-daemon --console=plain "-Dhttp.proxyHost=" "-Dhttps.proxyHost="
```

The shared and server command exited `0`, with both `:shared:jvmTest
NO-SOURCE` and `:server:test NO-SOURCE`. The Android test invocation exited
`1` while resolving the same shared Java 17/Kotlin 21 mismatch; no Android
Kotlin test source was compiled. A tracked-file count independently found
`0` test files.

## Repository and infrastructure counts

The following read-only count commands produced the baseline inventory:

```powershell
$tests = @(git ls-files '*Test.kt' '*Test.java' '*Test.kts' '*Test.js' '*_test.*')
$workflows = @(git ls-files '.github/workflows/**')
$serverBin = @(git ls-files 'server/bin/**')
```

Results: `tracked-test-files=0`, `tracked-workflows=0`, and
`tracked-server-bin=67`.

Docker commands:

```powershell
docker compose -f deploy/docker-compose.yml config --quiet
docker info --format '{{.ServerVersion}}'
```

Compose validation exited `0` and emitted only the expected warning that the
top-level `version` attribute is obsolete. `docker info` exited `1` because
the Docker Desktop Linux engine named pipe was unavailable; Docker Desktop
was not running during this baseline capture.

## Baseline boundary

This report records the known failures that the approved foundation-repair
plan is intended to fix. It does not claim that the Android app, Desktop
source, test suite, CI workflow, or Docker daemon is working. The generated
`server/bin` inventory is recorded for later cleanup, and machine-local
`local.properties` remains ignored and is not part of the baseline commit.
