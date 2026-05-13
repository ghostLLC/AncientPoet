# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AncientPoet（鸿雁）is a cross-platform "slow communication" app where users exchange letters with AI-powered ancient Chinese poets. Messages incur realistic delays (hours to days) based on geographic distance on dynasty-era maps. The product goal is to encourage deeper, more thoughtful writing by slowing down the pace of conversation.

**Current state**: Phase 1 + Phase 2 complete (backend + frontend). 15 poets across 5 dynasties. Storyline mode, user movement, poetry library, and drawing canvas all implemented. Full design system overhaul applied — all screens match `DESIGN/DESIGN.md` visual standards. Not yet compiled — requires JDK 17+. See `ARCHITECTURE.md` for the full phase roadmap and `DESIGN/` for UI prototypes.

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

Requires JDK 17+. Docker for infrastructure.

```bash
# Infrastructure
cd deploy && docker-compose up -d          # Start PG + Redis + MinIO

# Server
cd server && ./gradlew run                 # Ktor on :8080
./gradlew :server:test                     # Server tests
./gradlew :server:test --tests "com.ancientpoet.server.service.DelayCalculationServiceTest"

# Client
./gradlew :androidApp:installDebug         # Android debug build
./gradlew :desktopApp:run                  # Desktop app
./gradlew :shared:test                     # Shared module tests
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
- `route/` — thin routing layer, each file is a `fun Route.*()` extension. 10 route files: Auth, User, Poet, Conversation, Message, Storyline, Movement, Map, Poem, Upload. JWT principal accessed via safe-null pattern: `call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong() ?: return@authenticate call.respond(...)`
- `service/` — business logic. Key services:
  - `MessageService.sendMessage()` — store user msg → calc delay → launch AI reply in background coroutine → schedule Redis delivery. Accepts StorylineService and MovementService for Phase 2 delay wiring.
  - `StorylineService` — manages storyline progression: start, advance year, jump to year, get current event
  - `MovementService` — user movement with travel time calculation (50 km/day), auto-arrival detection
  - `DelayCalculationService` — Haversine formula with full multiplier chain: base delay × settled (×0.8) × storyline event (×1.5 for war/exile)
- `repository/` — Exposed DSL wrapped in `withContext(Dispatchers.IO)` transaction blocks
- `ai/` — DeepSeekClient (OpenAI-compatible `/v1/chat/completions` with 30s timeout), PromptBuilder (Chinese system prompt), ContextManager (summary + 8 recent rounds), TranslationService (temperature=0.3)
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
- **Needed**: Platform-specific SQLDelight driver wiring (`AndroidSqliteDriver` / `JdbcSqliteDriver`)
- **Current behavior**: Cache is in-memory only, lost on app restart. `Poet.sq` / `Message.sq` schemas defined but not connected to drivers.

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

**Phase 3 (planned)**: Community features (文苑), user profiles, favorites, poem sharing as scroll paintings.
