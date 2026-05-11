# AGENTS.md

## Project

**Name:** StreamRec  
**Type:** Real-Time Music Recommendation Engine

StreamRec ingests user listening behavior, updates music preference signals in near real time, and serves low-latency personalized track recommendations through REST APIs.

Current repository state: if the repo is sparse or only partially scaffolded, this document is the working source of truth for architecture, coding standards, and agent behavior until the implementation establishes stronger local conventions.

## Default Stack

Prefer this stack unless the repository clearly establishes another one:

- Java 21+
- Spring Boot
- Maven
- Kafka for user listening events
- Redis for low-latency precomputed recommendations
- PostgreSQL for durable relational storage and aggregates
- JUnit 5 and Mockito for testing

Do not introduce additional dependencies unless they are clearly justified by the task and consistent with the codebase.

## Product Intent

Build a backend service that:

- consumes music interaction events such as plays, likes, skips, and saves
- computes user affinity signals for artists and genres
- updates recommendation scores in near real time
- stores hot recommendation results in Redis
- persists durable domain and aggregate data in PostgreSQL
- serves personalized music recommendations through REST APIs

The MVP should stay simple, explainable, and production-sensible. Favor correctness, idempotency, latency, and operability over premature complexity.

## Domain Model

The core domain is:

- Users
- Tracks
- Artists
- Genres
- User listening events
- Recommendations

Typical relationships:

- a `Track` belongs to one primary `Artist`
- a `Track` belongs to one primary `Genre`
- a `UserListeningEvent` links a user to a track interaction
- recommendation results are generated per user and reference tracks

Keep domain naming explicit and music-specific. Avoid generic naming like `Item` when `Track` is the actual concept.

## Supported Event Types

Supported music interaction event types for MVP:

- `PLAY`
- `LIKE`
- `SKIP`
- `SAVE`

These events are immutable once published. Event contracts must be explicit, versionable, and safe for downstream consumers.

## Architecture

### Core flow

1. A user generates a listening event such as `PLAY`, `LIKE`, `SKIP`, or `SAVE`.
2. The event is published to Kafka topic `user-music-events`.
3. A Kafka consumer validates and processes the event asynchronously.
4. User affinity signals and recommendation scores are recalculated or incrementally updated.
5. Hot recommendation results and affinity data are written to Redis.
6. Durable aggregates and relational records are written to PostgreSQL.
7. Recommendation APIs read from Redis first and fall back to PostgreSQL when needed.

### Components

- `ingestion`: Kafka producers, event contracts, serializers, validation
- `processing`: consumers, event handlers, scoring logic, retry behavior
- `domain`: music recommendation rules, affinity models, ranking logic
- `persistence`: PostgreSQL entities/repositories and Redis access
- `api`: REST controllers, request validation, response mapping
- `config`: application wiring, Kafka, Redis, database, observability

### Architectural rules

- Controllers must stay thin.
- Business rules belong in services/domain logic, not controllers or repositories.
- Repositories are for persistence access, not recommendation decisions.
- Redis is a low-latency serving layer, not the long-term source of truth.
- PostgreSQL is the durable system of record.
- Kafka consumers must be idempotent.
- Kafka processing must tolerate duplicate delivery and replay.
- Fallback from Redis to PostgreSQL must be deliberate and observable.

## Recommended Repository Layout

When scaffolding the service, prefer:

```text
src/
  main/
    java/.../
      config/
      controller/
      service/
      domain/
      repository/
      kafka/
      dto/
      entity/
      exception/
      util/
    resources/
      application.yml
  test/
    java/.../
```

Use the standard Spring Boot layout. Do not create ad hoc top-level source folders.

## Recommendation Model

For MVP, use a simple explainable scoring model:

```text
score =
  genreAffinity
+ artistAffinity
+ recentInteractionBoost
+ popularityBoost
- skipPenalty
```

Interpretation:

- `genreAffinity`: how strongly the user prefers the track's genre based on recent and aggregate behavior
- `artistAffinity`: how strongly the user prefers the track's artist
- `recentInteractionBoost`: short-term boost from recent related listening behavior
- `popularityBoost`: bounded global popularity signal
- `skipPenalty`: penalty applied when the user frequently skips similar tracks, artists, or genres

Keep the MVP logic deterministic and explainable. Do not introduce ML-heavy ranking unless the repository or requirements explicitly move in that direction.

## Data and Messaging Rules

### Kafka

- Use Kafka for asynchronous event streaming, not synchronous RPC replacement.
- Use topic name `user-music-events` for user listening events.
- Include stable event identifiers for deduplication and replay safety.
- Consumers must be idempotent.
- Design for at-least-once delivery semantics.
- Implement retry behavior deliberately.
- Add a dead-letter strategy for malformed or repeatedly failing events.
- Avoid blocking I/O patterns that stall consumer throughput.

Example event subjects include:

- user played track `track_456`
- user liked track `track_789`
- user skipped an artist-heavy recommendation

### Redis

- Store low-latency precomputed recommendation results.
- Store hot user affinity aggregates needed for fast scoring or serving.
- Keep values compact and bounded.
- Recommendation key format must be `rec:{userId}`.
- User affinity key format must be `affinity:{userId}`.
- Set TTLs only when they match actual freshness requirements.
- Be mindful of hot-key behavior for highly active users.

### PostgreSQL

- Use PostgreSQL as the source of truth for users, tracks, artists, genres, and durable aggregates.
- Use parameterized access only.
- Design schema with clear ownership and indexing strategy.
- Avoid N+1 query patterns.
- Prefer normalized schema by default; denormalize only with measured justification.
- Persist data required for recomputation, recovery, and auditability.

## API Rules

- Follow REST conventions.
- Use explicit request and response DTOs.
- Validate all external input.
- Return stable, documented response shapes.
- Do not leak internal entities directly through the API.
- Keep endpoints narrow and music-domain specific.

For recommendation reads:

- Read from Redis first for latency-sensitive endpoints.
- Fall back to PostgreSQL only when needed.
- Make fallback behavior observable through logs and metadata.
- Do not silently hide systemic cache or pipeline issues behind fallback logic.

### Standard response shape

Recommendation responses should follow this standardized structure:

```json
{
  "userId": "user_123",
  "generatedAt": "2026-05-04T16:30:00Z",
  "recommendations": [
    {
      "trackId": "track_456",
      "title": "Midnight Drive",
      "artist": "Nova Lane",
      "genre": "Synthwave",
      "score": 0.91,
      "reason": "Because you recently played similar Synthwave tracks"
    }
  ],
  "metadata": {
    "source": "redis",
    "modelVersion": "v1",
    "latencyMs": 8
  }
}
```

Keep fields stable once published. If the response evolves, do so intentionally and compatibly.

## Coding Standards

### General

- Keep code simple, explicit, and readable.
- Favor composition over deep inheritance.
- Prefer small classes with clear responsibilities.
- Avoid speculative abstractions.
- Do not introduce framework-heavy patterns without concrete value.

### Java conventions

- Classes and enums: `PascalCase`
- Methods and fields: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- Packages: lowercase, domain-oriented, no underscores

### Method and class design

- Aim for single-purpose methods.
- Prefer early returns over deep nesting.
- Avoid methods that mix validation, orchestration, persistence, and mapping.
- Extract non-trivial ranking or affinity logic into dedicated classes.
- Keep controllers free of business logic.
- Keep repositories free of business decision logic.

### Error handling

- Never swallow exceptions.
- Throw specific exceptions with actionable messages.
- Translate internal failures to stable API responses at the edge.
- Include enough context in logs to diagnose failures.
- Never log secrets or sensitive user data.

### Logging and observability

- Use structured logs where practical.
- Log normal lifecycle events at `INFO`.
- Log recoverable anomalies at `WARN`.
- Log failures at `ERROR`.
- Include correlation-friendly identifiers when available, such as `userId`, `trackId`, `artistId`, `eventId`, or request IDs.
- Add metrics around consumer lag, processing failures, cache hit rate, recommendation latency, and fallback frequency once observability is present.

## Testing Expectations

- Write unit tests for scoring, affinity, and service logic.
- Add integration tests for Kafka, Redis, and PostgreSQL boundaries when those layers exist.
- Mock external dependencies in unit tests.
- Avoid mocking the code under test.
- Cover happy paths, edge cases, and failure handling.
- Prioritize tests for idempotent event handling, duplicate event safety, cache consistency, fallback behavior, and ranking correctness.

Recommended conventions:

- test classes: `*Test.java`
- integration tests: `*IT.java` if the project adopts that split

Example scenarios worth testing:

- repeated `PLAY` event with the same event ID does not double-count affinity
- `SKIP` events reduce track or genre ranking appropriately
- Redis miss falls back to PostgreSQL and records the correct metadata source
- a liked Synthwave track boosts similar artist and genre recommendations

## Build and Run

If Maven wrapper scripts exist, prefer them:

```bash
./mvnw clean test
./mvnw spring-boot:run
```

If wrappers are absent, use local Maven:

```bash
mvn clean test
mvn spring-boot:run
```

Only add Docker-specific instructions if Docker assets actually exist in the repository.

## Agent Operating Rules

These rules are for Codex and other AI agents working in this repository.

### Before changing code

- Inspect the repository structure first.
- Read relevant files before editing.
- Follow existing patterns when code already exists.
- If the repo is sparse or empty, use this document as the working standard.
- Do not assume schemas, DTOs, topic payloads, or endpoints that are not defined.

### While changing code

- Make focused, minimal changes.
- Do not refactor unrelated areas.
- Do not add dependencies unless clearly justified.
- Preserve architecture boundaries strictly.
- Keep implementations production-sensible, not demo-like.
- Prefer explicit code over magic or overly generic helper layers.
- Keep configuration externalized.
- Never hardcode credentials, secrets, or environment-specific values.
- Use music-domain naming consistently: `Track`, `Artist`, `Genre`, `UserListeningEvent`, `Recommendation`.

### After changing code

- Verify imports and compile assumptions.
- Run relevant tests when available.
- If builds or tests cannot run, say so clearly.
- Summarize meaningful changes and any residual risks.

## Non-Goals

Avoid introducing these without explicit requirements or strong repo evidence:

- ML-heavy ranking pipelines
- event sourcing complexity beyond practical need
- CQRS for its own sake
- multiple microservices before the monolith boundary is proven
- custom frameworks or elaborate internal platforms

## Common Failure Modes

Watch for:

- duplicate Kafka event processing
- Redis/PostgreSQL divergence
- race conditions when concurrent listening events update the same user affinity state
- unbounded consumer retries
- latency spikes from synchronous database work in hot paths
- oversized Redis payloads
- weak event contracts that break downstream consumers
- incorrect skip penalty behavior that suppresses relevant tracks too aggressively

## Change Discipline

- One logical change at a time.
- Keep patches reviewable.
- Prefer adding or updating tests with behavior changes.
- Do not bypass architecture rules for short-term convenience.
- Do not introduce breaking API, schema, or event contract changes silently.

When the codebase evolves, update this file so it reflects actual project decisions rather than stale aspirations.
