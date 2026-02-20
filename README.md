# SportX Backend

SportX is a Spring Boot backend for a sports challenge platform.  
It demonstrates practical backend engineering with Redis caching, distributed locking, RabbitMQ event-driven workflows, DLQ replay, and layered testing.

## Tech Stack
- Java 23
- Spring Boot 3.4.x
- MyBatis-Plus
- MySQL
- Redis
- RabbitMQ
- Spring Validation
- BCrypt (`spring-security-crypto`)
- OpenAPI/Swagger (`springdoc-openapi`)
- JUnit 5, Mockito, MockMvc

## API Documentation
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Core Features

### 1. User & Authentication
- `POST /user/code` send SMS code (demo returns code directly)
- `POST /user/login` login by phone + code
- `POST /user/regularLogin` login by phone + password
- `POST /user/register` register user
- `POST /user/logout` logout (token invalidation in Redis)
- `GET /user/me` current user from request context
- `GET /user/profile` get profile
- `PUT /user/profile` update profile

Highlights:
- Redis token session + auto TTL refresh interceptor
- BCrypt password hashing
- Login rate limiting / anti-bruteforce
- Request validation + global exception handling

### 2. Spot
- `GET /spots/{id}` spot detail (with cache)
- `PUT /spots` update spot (cache invalidation)
- `POST /spots/search` spot search with filters + pagination

### 3. Challenge
- `GET /challenge/list` challenge list
- `GET /challenge/{id}` challenge detail (cache with pass-through)
- `GET /challenge/my` current user participation records
- `POST /challenge/add` create challenge
- `POST /challenge/register/{id}` register challenge
- `POST /challenge/cancel/{id}` cancel registration

Highlights:
- Redis + Lua distributed lock on register flow
- Slot control with conditional DB update (CAS-style)
- Unique index + duplicate-key fallback handling
- Cache invalidation after register/cancel

### 4. Favorite
- `POST /favorite/spots/{id}` add favorite
- `DELETE /favorite/spots/{id}` remove favorite
- `GET /favorite/spots` list favorites

### 5. Notification
- `GET /notification/list` list notifications
- `PUT /notification/read/{id}` mark as read

Notification sources:
- signup success
- cancel success
- challenge start reminder
- challenge end reminder

### 6. Leaderboard (Redis ZSet)
- `GET /leaderboard/spots/heat` top spot heat
- `GET /leaderboard/users/score` top user score

### 7. MQ Reliability & Operations
- Challenge event publishing and consuming
- Retry policy on consumer failure
- Dead-letter queue routing
- Idempotent consume markers in Redis
- Failed-message persistence for diagnosis
- Failed-message replay endpoint:
  - `GET /mq/failed/list`
  - `POST /mq/failed/retry/{id}`

## Data Model
Main tables:
- `user`
- `spots`
- `challenge`
- `challenge_participation`
- `notifications`
- `spot_favorites`
- `failed_message`
- `leaderboard_snapshot`
- `leaderboard_entry`

Important constraints:
- unique `(user_id, challenge_id)` on `challenge_participation`
- unique `(user_id, spot_id)` on `spot_favorites`

## Project Structure
- `src/main/java/com/example/sportx/Controller` REST controllers
- `src/main/java/com/example/sportx/Service` service contracts
- `src/main/java/com/example/sportx/Service/Impl` service implementations
- `src/main/java/com/example/sportx/Mapper` MyBatis-Plus mappers
- `src/main/java/com/example/sportx/Entity` entities
- `src/main/java/com/example/sportx/Entity/dto` request DTOs
- `src/main/java/com/example/sportx/Entity/vo` response models
- `src/main/java/com/example/sportx/Utils` caching, MQ, Redis, interceptors
- `src/main/java/com/example/sportx/RabbitMQ` MQ config/listener/scheduler
- `src/main/resources/sql` SQL scripts (indexes / failed-message table)

## Local Setup

### Prerequisites
- MySQL running on `127.0.0.1:3306`
- Redis running on `127.0.0.1:6379`
- RabbitMQ running on `127.0.0.1:5672`

### Configuration
Edit:
- `src/main/resources/application.properties`

### Run
```bash
./mvnw spring-boot:run
```

### Test / Build
```bash
./mvnw test
./mvnw package
```

## SQL Scripts
- Create failed-message table:
  - `src/main/resources/sql/failed_message.sql`
- Create unique indexes (compatible script):
  - `src/main/resources/sql/unique_indexes.sql`

## Engineering Notes
- Constructor injection with `final` + `@RequiredArgsConstructor`
- Typed response envelope: `Result<T>`
- Validation-first controllers (`@Validated`, `@Valid`)
- Global exception mapping with business-friendly messages
- Redis cache pass-through + logical-expire utility
- Redis + Lua lock release safety
- Event-driven architecture with retry/DLQ/replay

## Suggested Next Step (AI Phase)
- Start AI features after this backend baseline:
  - retrieval service abstraction
  - RAG-ready data ingestion pipeline
  - AI module isolated from core transaction paths
