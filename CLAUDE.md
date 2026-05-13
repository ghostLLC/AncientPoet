# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AncientPoet（鸿雁）is a cross-platform "slow communication" app where users exchange letters with AI-powered ancient Chinese poets. Messages incur realistic delays (hours to days) based on geographic distance on dynasty-era maps. The product goal is to encourage deeper, more thoughtful writing by slowing down the pace of conversation.

**Current state**: Phase 1 + Phase 2 complete (compiled + tested). 15 poets across 5 dynasties. Storyline mode, user movement, poetry library, and drawing canvas all implemented. Full design system overhaul applied — all screens match `DESIGN/DESIGN.md` visual standards. Phase 3 (community) backend was built but temporarily removed during Ktor 3.x / Exposed 0.57 migration — needs re-integration. All 4 Gradle modules compile on JDK 17 + Android SDK 34. See `ARCHITECTURE.md` for the full phase roadmap and `DESIGN/` for UI prototypes.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Client | Kotlin Multiplatform + Compose Multiplatform |
| Server | Kotlin + Ktor Server 3.x |
| Database | PostgreSQL 16 + PostGIS (geospatial distance) |
| ORM | Exposed (Kotlin type-safe SQL) |
| Cache/Queue | Redis 7.x (sorted-set delivery scheduling) |
| Object Storage | MinIO (S3-compatible, self-hosted) |
| AI | DeepSeek V4-Flash (OpenAI-compatible API) |
| Push | Firebase Cloud Messaging |
| DI | Koin 4.x |
| Local DB (client) | SQLDelight 2.x |
| Migrations | Flyway (V1 schema, V2 seed, V3 Phase 2 data) |

## Build & Run Commands

Requires JDK 17+ and Android SDK (for Android target). Docker for infrastructure.

```bash
# First time: create local.properties with Android SDK path
echo "sdk.dir=D:/Android/Sdk" > local.properties

# Infrastructure
cd deploy && docker-compose up -d          # Start PG + Redis + MinIO

# Compile all modules (no device needed)
./gradlew :shared:compileKotlinJvm
./gradlew :server:compileKotlin
./gradlew :desktopApp:compileKotlinJvm
./gradlew :androidApp:compileDebugSources

# Run
./gradlew :server:run                      # Ktor on :8080
./gradlew :desktopApp:run                  # Desktop app
./gradlew :androidApp:installDebug         # Android debug build (needs device/emulator)

# Tests
./gradlew :server:test
./gradlew :server:test --tests "com.ancientpoet.server.service.DelayCalculationServiceTest"
./gradlew :shared:test
```

## Architecture

**Client**: Clean Architecture + MVVM with three layers:
- `ui/` — Compose screens + ViewModels (each screen has a `*Screen.kt` + `*ViewModel.kt` pair)
- `domain/` — use cases, domain models, repository interfaces
- `data/` — repository implementations wrapping Ktor Client + SQLDelight cache

**Design system** (`DESIGN/` folder):
- `DESIGN/DESIGN.md` — design token spec (colors, typography, spacing, rounded corners) with YAML frontmatter
- `DESIGN/high_fidelity_design.md` — design philosophy brief for UI prototyping
- `DESIGN/_1/` through `DESIGN/_8/` — HTML/CSS prototypes + PNG screenshots for all 8 screens
- Theme implementation in `androidApp/.../ui/theme/Theme.kt` (39 Material 3 color roles + 7 Chinese pigment semantic colors) and `Type.kt` (3-level typography: Serif for headings/letters, Sans for UI labels)
- Key visual patterns: seal-style buttons (2dp radius, red border, no fill), letter-lines input area, TranslationSeal ("译" stamp component), poem text at 18sp/36sp line-height/0.5sp letter-spacing

**Server**: Ktor pipeline — plugins → routes → services → repositories → Exposed:
- `plugin/` — Authentication (JWT via auth0-jwt), CORS, kotlinx.serialization, rate limiting, status pages
- `route/` — thin routing layer, each file is a `fun Route.*()` extension. 9 active routes: Auth, User, Poet, Conversation, Message, Storyline, Movement, Map, Poem. Inside `authenticate("auth-jwt")` blocks, JWT principal accessed via `!!` (safe since Ktor rejects unauthenticated requests before reaching the handler). Community and Upload routes temporarily removed pending Ktor 3.x API migration.
- `service/` — business logic. Key services:
  - `MessageService.sendMessage()` — store user msg → calc delay → launch AI reply in background coroutine → schedule Redis delivery. Accepts StorylineService and MovementService for Phase 2 delay wiring.
  - `StorylineService` — manages storyline progression: start, advance year, jump to year, get current event
  - `MovementService` — user movement with travel time calculation (50 km/day), auto-arrival detection
  - `DelayCalculationService` — Haversine formula with full multiplier chain: base delay × settled (×0.8) × storyline event (×1.5 for war/exile)
- `CommunityService` — posts CRUD, comments, likes, repost, favorites, profiles (temporarily removed, pending re-integration)
	- `repository/` — Exposed DSL wrapped in `withContext(Dispatchers.IO)` transaction blocks. 4 active repositories: User, Poet, Conversation, Message. Uses `insert { it[col] = val }` pattern (Exposed 0.57), ResultRow access without `.value` on plain Long columns.
- `ai/` — DeepSeekClient (OpenAI-compatible chat + vision model, 30s timeout), PromptBuilder (Chinese system prompt + painting appreciation), ContextManager (summary + 8 recent rounds), TranslationService (temperature=0.3)
- `scheduler/` — MessageDeliveryScheduler (Redis sorted set `msg:delivery:schedule`, 1-min polling with SLF4J logging)
- `di/ServerModule.kt` — Koin module registering all components as singletons

**KMP Shared Module** (`shared/`) — `commonMain` contains:
- `data/api/` — Ktor Client wrapper (`AncientPoetApi` class, baseUrl injected per platform)
- `data/local/` — SQLDelight DAOs (MessageEntity, PoetEntity)
- `domain/` — use cases (SendMessage, GetConversations, CalculateDelay, BrowsePoets)
- `util/` — DateTimeUtil, DistanceUtil (Haversine, pure Kotlin, both platforms)

**Data files**:
- `data/poets/` — 15 poet JSONs following `li_bai.json` schema. **CRITICAL: Chinese text inside JSON strings MUST use curly quotes "" (U+201C/U+201D), never ASCII "". All files verified with balanced LQ/RQ pairs.**
- `data/cities/` — City coordinates for 5 dynasties: tang, song, han, jin, ming

## Core Systems

### Delay Calculation

```
finalDelay = clamp(baseDelay × settledCoefficient × storylineCoefficient, 2h, 7d)
```

`DelayCalculationService.calculate()` accepts `settled` (Boolean) and `eventDelayMultiplier` (Double) parameters. Settled status (×0.8) read from `user_locations.status`. Storyline multiplier read from `poet_movements.event_type` (war/exile → ×1.5). All factors recorded in `messages.delay_factors` JSONB.

### Message Delivery Scheduling

Redis Sorted Set `msg:delivery:schedule`. Score = epoch seconds, member = message ID. Polls every 60s via `zrangebyscore`. Failures logged via SLF4J and kept in set for retry.

### AI Context Management

After 40 messages (20 rounds), `ContextManager.shouldSummarize()` triggers. Older messages compressed via DeepSeek into `conversation_summaries`. Recent 16 messages (8 rounds) kept in full.

### Storyline Mode

`StorylineService` manages time progression in `mode='storyline'` conversations. Flow: create conversation with `storylineStartYear` → each message advances year by 1 → `poet_life_events` trigger at matching years → event description injected into system prompt → event `delayMultiplier` applied to delay calc. Users can jump to any year via `POST /conversations/{id}/storyline/jump`.

### User Movement

`MovementService` handles map relocation: user selects city → `POST /user/location/{dynastyId}/move` → travel time calculated at 50 km/day (min 1 hour) → status set to `moving` with arrival timestamp → on next status check, auto-completes if arrival time passed → settled status activates ×0.8 delay bonus.

## Key Conventions

- Base package: `com.ancientpoet`
- API base path: `/api/v1`
- Flyway migrations: V1 (schema), V2 (Phase 1 seed: 5 poets), V3 (Phase 2 seed: 10 more poets + dynasties + cities)
- JWT auth with access + refresh tokens; refresh token verification uses `JWT.require(algorithm).build().verify(token)` (not raw `decode()`)
- All route handlers under `authenticate("auth-jwt")` except auth endpoints
- PostGIS `geom` columns managed by PostgreSQL trigger `sync_geom_from_latlng` — NOT mapped in Exposed; app uses `lat`/`lng` directly
- Dev SMS bypass code `123456` only when `KTOR_DEVELOPMENT=true`
- All `printStackTrace()` replaced with SLF4J logging (`logger.error(...)`)
- `StatusPages` catches `Exception` not `Throwable` (allows JVM Errors to propagate)
- **Exposed 0.57 breaking changes**: `insertAndGetId` replaced with `insert { it[col] = val }` + `result[Table.id]`; `timestamptz` replaced with `text` for timestamp columns; FK `.references()` removed (constraints enforced at DB level via Flyway); ResultRow access without `.value` on plain Long columns
- **Ktor 3.x breaking changes**: route handlers no longer `inline`, so `return@label` is prohibited — use `!!` on JWT principal inside `authenticate` blocks; `RateLimit.refillPeriod` takes `Duration` not `Int`; multipart API uses `readPart()` iterator
- **Android SDK**: located at `D:/Android/Sdk` on this machine; `local.properties` must contain `sdk.dir=D:/Android/Sdk`; `compileSdk=34` with `androidx-core:1.13.1`

## Known Gaps (Requires External Credentials/Setup)

These features are structurally implemented but need third-party credentials to activate:

### FCM Push Notifications
- **File**: `server/.../push/FCMClient.kt` (stub) + `androidApp/.../service/FCMService.kt` (client)
- **Needed**: Firebase project with `google-services.json` placed in `androidApp/`
- **Server side**: Firebase Admin SDK service account JSON, set `FCM_CREDENTIALS_PATH` env var
- **Current behavior**: `FCMClient.send()` is a no-op; push delivery silently skipped

### SMS Verification
- **File**: `server/.../service/AuthService.kt`
- **Needed**: Aliyun SMS (`SMS_PROVIDER=aliyun`) or Tencent Cloud SMS credentials
- **Env vars**: `SMS_ACCESS_KEY`, `SMS_ACCESS_SECRET`, `SMS_SIGN_NAME`, `SMS_TEMPLATE_CODE`
- **Current behavior**: Codes stored in-memory; dev bypass `123456` works when `KTOR_DEVELOPMENT=true`

### SQLDelight Persistent Cache
- **File**: `shared/.../data/local/LocalDataSource.kt` (`InMemoryLocalDataSource` fallback)
- **Needed**: Platform-specific SQLDelight driver wiring (`AndroidSqliteDriver` / `JdbcSqliteDriver`) for persistence across restarts
- **Current behavior**: In-memory cache works within a session; `Poet.sq` / `Message.sq` schemas defined. `LocalDataSource` interface ready for driver swap. Also provides API-level cache in shared module for offline resilience.

### AI-Generated Poet Portraits
- **Files**: All poet JSONs in `data/poets/` + `PoetAvatar` component uses initials
- **Needed**: 15 portrait images (one per poet), path stored in `portrait_url` field
- **Current behavior**: Golden square with first character of poet's name

## Phase 2: Complete

All Phase 2 features implemented (backend + frontend):

| Feature | Backend | Frontend |
|---------|---------|----------|
| Storyline mode | StorylineService + Route | StorylineTimeline, year picker, banner |
| User movement | MovementService + Route | Interactive map, city selection, animation |
| 15 poets, 5 dynasties | V3 seed migration | Poet list/detail with serif typography |
| Poetry library | PoemRoute (/poems, /poems/search) | PoetryListScreen, PoetryDetailScreen |
| Drawing/painting | Vision API wiring | DrawingCanvas dialog |
| Delay formula | Settled + event multipliers | Factor breakdown in UI |

## Phase 3: Backend implemented (frontend partially, temporarily removed)

Community features built but removed during compile migration — needs Ktor 3.x API re-integration:

| Feature | Backend | Frontend |
|---------|---------|----------|
| Post feed | CommunityRoute GET /community/posts | CommunityScreen with post cards |
| Create post | CommunityRoute POST /community/posts | — (API ready) |
| Like/unlike | CommunityRoute POST /posts/{id}/like | Heart toggle in feed |
| Comments | CommunityRoute GET+POST /posts/{id}/comments | PostDetailScreen with comment input |
| Repost | CommunityRoute POST /posts/{id}/repost | — (API ready) |
| Favorites | CommunityRoute POST+GET /user/favorites | — (API ready) |
| Profile | CommunityRoute GET /user/profile/{id} | ProfileScreen with stats |
| Tables | community_posts, comments, likes, favorites (pre-existing) | — |
