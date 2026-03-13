# SportX

SportX is a Spring Boot backend for a sports challenge platform with an integrated AI assistant.

The project combines practical backend engineering and an AI RAG workflow:
- user authentication and profile management
- challenge registration and cancellation
- favorite spots and notifications
- Redis caching, distributed locking, and leaderboards
- RabbitMQ event delivery with retry, DLQ, and replay
- Qwen-based RAG assistant with knowledge retrieval and source citation

## Highlights

- Constructor injection with `final` fields and `@RequiredArgsConstructor`
- Typed API responses with `Result<T>`
- Request validation with `@Validated` and `@Valid`
- Global exception handling with business-friendly messages
- BCrypt password hashing
- Redis token session with automatic TTL refresh
- Login and verification-code rate limiting
- Redis + Lua distributed locking for challenge registration
- RabbitMQ retry, DLQ, failed-message persistence, and manual replay
- Swagger / OpenAPI documentation
- Unit and controller tests
- AI assistant with:
  - knowledge ingestion from MySQL and markdown rules
  - chunking via LangChain4j
  - embedding-based retrieval
  - generated answers with source snippets
  - manual index rebuild endpoint

## Tech Stack

- Java 23
- Spring Boot 3.4.x
- MyBatis-Plus
- MySQL
- Redis
- RabbitMQ
- Spring Validation
- Spring Security Crypto (`BCryptPasswordEncoder`)
- Swagger / OpenAPI (`springdoc-openapi`)
- JUnit 5, Mockito, MockMvc
- LangChain4j
- Qwen-compatible OpenAI API mode

## API Documentation

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Core Modules

### 1. User and Authentication

- `POST /user/code`
- `POST /user/login`
- `POST /user/regularLogin`
- `POST /user/register`
- `POST /user/logout`
- `GET /user/me`
- `GET /user/profile`
- `PUT /user/profile`

Key behaviors:
- token-based login stored in Redis
- automatic token refresh on authenticated requests
- BCrypt password hashing
- login and verification-code throttling
- profile update support

### 2. Spots

- `GET /spots/{id}`
- `POST /spots/search`

Key behaviors:
- spot detail cache
- search and filter support

### 3. Challenges

- `GET /challenge/list`
- `GET /challenge/{id}`
- `GET /challenge/my`
- `POST /challenge/add`
- `POST /challenge/register/{id}`
- `POST /challenge/cancel/{id}`

Key behaviors:
- challenge detail cache
- one-user-one-registration protection
- Redis + Lua distributed lock
- slot control with conditional database update
- MQ event publishing after registration and cancellation

### 4. Favorites

- `POST /favorite/spots/{id}`
- `DELETE /favorite/spots/{id}`
- `GET /favorite/spots`

Key behaviors:
- unique favorite relationship per user and spot
- paginated favorite list

### 5. Notifications

- `GET /notification/list`
- `PUT /notification/read/{id}`

Notification sources:
- registration success
- cancellation success
- challenge start reminder
- challenge end reminder

### 6. Leaderboards

- `GET /leaderboard/spots/heat`
- `GET /leaderboard/users/score`

Key behaviors:
- Redis ZSet-based hot spot leaderboard
- Redis ZSet-based user score leaderboard

### 7. MQ Reliability

- `GET /mq/failed/list`
- `POST /mq/failed/retry/{id}`

Key behaviors:
- RabbitMQ retry policy
- dead-letter queue routing
- failed-message persistence
- manual replay endpoint
- Redis-based idempotent consumption markers

### 8. AI Assistant

- `POST /ai/ask`
- `POST /ai/reindex`

Current AI scope:
- answers questions about platform rules, spots, and challenges
- retrieves relevant knowledge chunks before generation
- returns source citations with each answer

Current knowledge sources:
- `spots` table
- `challenge` table
- `src/main/resources/ai/platform-rules.md`

Current AI flow:
1. load knowledge from database and markdown
2. split documents into chunks with LangChain4j
3. generate embeddings
4. store vectors in an in-memory embedding store
5. retrieve relevant chunks for the question
6. generate the final answer with cited sources

## Project Structure

- `src/main/java/com/example/sportx/Controller` REST controllers
- `src/main/java/com/example/sportx/Service` service interfaces
- `src/main/java/com/example/sportx/Service/Impl` service implementations
- `src/main/java/com/example/sportx/Mapper` MyBatis-Plus mappers
- `src/main/java/com/example/sportx/Entity` domain entities and AI knowledge objects
- `src/main/java/com/example/sportx/Entity/dto` request DTOs
- `src/main/java/com/example/sportx/Entity/vo` response VOs
- `src/main/java/com/example/sportx/Config` configuration classes
- `src/main/java/com/example/sportx/Utils` Redis, cache, MQ, and helper utilities
- `src/main/java/com/example/sportx/RabbitMQ` MQ config, listeners, and scheduler
- `src/main/resources/ai` AI knowledge markdown
- `src/main/resources/sql` SQL scripts

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

## Local Setup

### Prerequisites

- MySQL on `127.0.0.1:3306`
- Redis on `127.0.0.1:6379`
- RabbitMQ on `127.0.0.1:5672`

### Configuration

Main config:
- `src/main/resources/application.properties`

Local private overrides:
- `src/main/resources/application-local.properties`

`application-local.properties` is ignored by Git and should contain local secrets such as AI API keys.

Example AI configuration with Qwen:

```properties
ai.rag.embedding-api-key=your_qwen_key
ai.rag.embedding-base-url=https://dashscope.aliyuncs.com/compatible-mode/v1
ai.rag.embedding-model=text-embedding-v4

ai.rag.chat-api-key=your_qwen_key
ai.rag.chat-base-url=https://dashscope.aliyuncs.com/compatible-mode/v1
ai.rag.chat-model=qwen-turbo
```

### Run

```bash
./mvnw spring-boot:run
```

### Build

```bash
./mvnw package
```

### Test

```bash
./mvnw test
```

## SQL Scripts

- failed-message table:
  - `src/main/resources/sql/failed_message.sql`
- unique indexes:
  - `src/main/resources/sql/unique_indexes.sql`

## Example AI Requests

### Ask a question

```http
POST /ai/ask
Content-Type: application/json
```

```json
{
  "question": "What are the current challenge rules on SportX?"
}
```

### Rebuild the AI index

```http
POST /ai/reindex
```

No request body is required.

## Engineering Notes

- The backend baseline is production-oriented in structure, not just CRUD-oriented.
- Reliability features are part of the main project design, not later add-ons.
- The AI module is intentionally isolated from the core transaction paths.
- Retrieval and generation are separated so providers can be changed independently.

## Roadmap

Possible next improvements:
- stronger retrieval reranking for rule-specific questions
- persistent vector storage instead of in-memory index
- automatic reindexing after spot or challenge updates
- richer AI knowledge sources
- AI recommendation flow on top of user favorites and registrations
