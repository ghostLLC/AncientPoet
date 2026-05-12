# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AncientPoet（鸿雁）is a cross-platform "slow communication" app where users exchange letters with AI-powered ancient Chinese poets. Messages incur realistic delays (hours to days) based on geographic distance on dynasty-era maps. The product goal is to encourage deeper, more thoughtful writing by slowing down the pace of conversation.

**Current state**: Pre-implementation design phase. Directory structure and architecture docs exist, but no source code has been written. See `ARCHITECTURE.md` for the complete technical architecture.

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
| Migrations | Flyway |

## Build & Run Commands

Since no Gradle files exist yet, these are the planned commands:

```bash
# Backend
cd deploy && docker-compose up -d          # Start PG + Redis + MinIO
cd server && ./gradlew run                 # Start Ktor server on :8080

# Client
./gradlew :androidApp:installDebug         # Android debug build
./gradlew :desktopApp:run                  # Desktop app
./gradlew :shared:test                     # Run shared module tests
./gradlew :server:test                     # Run server tests
./gradlew :server:test --tests "com.ancientpoet.server.service.DelayCalculationServiceTest"
```

## Architecture

**Client**: Clean Architecture + MVVM with three layers:
- `ui/` — Compose screens and ViewModels
- `domain/` — use cases, domain models, repository interfaces
- `data/` — repository implementations, Ktor Client API definitions, SQLDelight local cache

**Server**: Ktor pipeline with plugins → routes → services → repositories → Exposed ORM:
- `plugin/` — Authentication (JWT), CORS, serialization, rate limiting
- `route/` — thin routing layer that delegates to services
- `service/` — business logic (message sending, delay calculation, AI orchestration)
- `repository/` — Exposed-based data access
- `ai/` — DeepSeekClient, PromptBuilder, ContextManager, TranslationService
- `scheduler/` — MessageDeliveryScheduler (Redis sorted-set driven), PoetMovementUpdater

**KMP Shared Module** lives in `shared/` with `commonMain` containing shared business logic:
- `data/api/` — Ktor Client HTTP calls to server
- `data/local/` — SQLDelight DAOs
- `domain/` — use cases and repository interfaces
- `util/` — DateTimeUtil, DistanceUtil (Haversine formula for both platforms)

## Core Systems

### Delay Calculation (the product's key differentiator)

```
finalDelay = clamp(baseDelay × settledCoefficient × storylineCoefficient, min, max)
```

- Base delay derived from Haversine distance ÷ ancient travel speed (80 km/day)
- Distance < 30km → 2 hours; < 150km → 1 day; else proportional
- Settled status: ×0.8 multiplier (faster because you're at a known location)
- Storyline events: ×1.5 multiplier for war/exile periods
- Clamped to [2 hours, 7 days]

### Message Delivery Scheduling

Redis Sorted Set where score = delivery timestamp, member = message ID. A scheduler polls every minute via `zrangebyscore` to find due messages, marks them delivered, sends FCM push, and removes from the set.

### AI Context Management

"Summary + recent messages" strategy: after 20 rounds (40 messages), older messages are compressed into an AI-generated summary stored in `conversation_summaries`. Recent 8 rounds are kept in full. This keeps token usage manageable across weeks-long conversations.

### Poet Location Resolution

Poets move through history via `poet_movements` table. Given a poet + story year, query for `year_start <= year <= year_end` to find their current location.

## Project Modules

| Module | Purpose |
|--------|---------|
| `shared/` | KMP shared business logic, API client, local cache |
| `androidApp/` | Android Compose UI (primary platform, Phase 1) |
| `desktopApp/` | Compose Desktop (Phase 4) |
| `server/` | Ktor backend |
| `data/` | Static JSON data (poet profiles, poems, city coordinates, maps) |
| `deploy/` | Docker Compose, Nginx config, .env template |

## Key Conventions

- All Kotlin code uses `com.ancientpoet` as the base package
- Server uses Flyway for DB migrations; SQL files in `server/src/main/resources/db/migration/`
- Exposed table definitions in `server/.../model/db/`, domain models in `model/domain/`, API DTOs in `model/dto/`
- JWT-based auth with access + refresh tokens; phone number is the primary user identifier
- API base path: `/api/v1`
- `poet_movements` and `user_locations` tables use PostGIS `GEOMETRY(Point, 4326)` columns for spatial queries
- Static poet data in `data/poets/` follows the `li_bai.json` schema
