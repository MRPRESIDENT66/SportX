# SportX Backend

SportX is a Java backend project for a sports challenge platform.
It focuses on challenge lifecycle management, user auth, Redis caching, and RabbitMQ-based event notifications.

## 1. Tech Stack
- Java 23
- Spring Boot 3.4.x
- MyBatis-Plus
- MySQL
- Redis
- RabbitMQ
- Lombok
- JUnit 5 + Mockito + MockMvc

## 2. Core Features
### User Module
- `POST /user/register` Register user
- `POST /user/login` Login by SMS code
- `POST /user/regularLogin` Login by phone + password
- `POST /user/logout` Logout (invalidate Redis token)
- `GET /user/me` Get current user from token context
- `GET /user/profile` Get user profile
- `PUT /user/profile` Update user profile (`nickname/avatar/bio/gender/city`)

### Spot Module
- `GET /spots/{id}` Spot detail
- `PUT /spots` Update spot
- `POST /spots/search` Spot search (pagination + filters)

### Challenge Module
- `GET /challenge/list` Challenge list (status/pagination/keyword/spot filters)
- `GET /challenge/{id}` Challenge detail
- `GET /challenge/my` My participation records
- `POST /challenge/add` Create challenge
- `POST /challenge/register/{id}` Register challenge
- `POST /challenge/cancel/{id}` Cancel registration and rollback slot

### Leaderboard Module
- `GET /leaderboard/spots/heat` Spot heat ranking (Redis ZSet)

### Event / Notification Module (RabbitMQ)
- Challenge events:
  - `SIGN_UP_SUCCESS`
  - `CANCEL_SUCCESS`
  - `START_REMINDER`
  - `END_REMINDER`
- Event consumer handles:
  - notification dispatch
  - leaderboard heat score update

## 3. Project Structure
- `src/main/java/com/example/sportx/Controller` HTTP APIs
- `src/main/java/com/example/sportx/Service` Service contracts
- `src/main/java/com/example/sportx/Service/Impl` Service implementations
- `src/main/java/com/example/sportx/Mapper` MyBatis-Plus mappers
- `src/main/java/com/example/sportx/Entity` persistence entities
- `src/main/java/com/example/sportx/Entity/dto` request DTOs
- `src/main/java/com/example/sportx/Entity/vo` response VOs
- `src/main/java/com/example/sportx/Utils` utils, interceptors, redis/mq helpers
- `src/main/java/com/example/sportx/RabbitMQ` mq config/listener/scheduler

## 4. Local Run
### 4.1 Prerequisites
- MySQL running on `127.0.0.1:3306`
- Redis running on `127.0.0.1:6379`
- RabbitMQ running on `127.0.0.1:5672`

### 4.2 Config
Edit `/src/main/resources/application.properties` if needed:
- datasource
- redis
- rabbitmq

### 4.3 Start
```bash
./mvnw spring-boot:run
```

### 4.4 Test / Build
```bash
./mvnw test
./mvnw package
```

## 5. Authentication Flow
1. Login/Register returns token.
2. Client sends token in header: `authorization: <token>`.
3. `RefreshTokenInterceptor` loads user from Redis and refreshes TTL.
4. `LoginInterceptor` rejects protected endpoints when no user context.

## 6. Data Model (Current)
Main tables:
- `user`
- `spots`
- `challenge`
- `challenge_participation`
- `notifications`
- `spot_favorites`
- `leaderboard_snapshot`
- `leaderboard_entry`

## 7. Engineering Notes
- Constructor injection with `final + @RequiredArgsConstructor`
- Typed API response: `Result<T>`
- DTO / VO separation for better maintainability
- Core business logic covered by unit and integration tests

## 8. Next Steps
- Add challenge detail cache (Redis)
- Add stronger registration/cancel concurrency protection
- Add API docs (OpenAPI/Swagger)
- Add CI pipeline (build + test)
- Add RAG-based AI assistant (LangChain4j)

---
If you are reviewing this project for backend internship roles, feel free to focus on:
- challenge register/cancel transaction logic
- Redis + RabbitMQ integration
- test coverage and code structure
