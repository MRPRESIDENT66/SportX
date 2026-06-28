# SportX

SportX 是一个校园运动挑战平台的后端系统（Spring Boot），聚焦**高并发报名、缓存设计、消息可靠投递与全文检索**等后端工程问题，而非单纯的 CRUD。

核心业务：用户浏览体育场馆、报名限时运动挑战、赢取积分登上排行榜。技术难点集中在「热门挑战瞬时高并发报名」——需要保证名额不超卖、事件不丢失、排行榜数据最终一致。

---

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 / 框架 | Java 21、Spring Boot 3.4.3 |
| 持久层 | MyBatis-Plus、MySQL 8 |
| 缓存 / 锁 | Redis（Lettuce 常规操作 + Redisson 分布式锁/限流） |
| 消息队列 | RabbitMQ（重试、DLQ、Outbox 投递） |
| 搜索引擎 | Elasticsearch 8.15 + IK 中文分词 |
| 切面 / 限流 | Spring AOP + Redisson RRateLimiter（令牌桶） |
| 文档 | springdoc-openapi（Swagger UI） |
| 压测 | wrk + Lua |

---

## 核心设计亮点

### 1. 高并发报名防超卖

报名扣减名额用**一条原子条件 UPDATE**，把「检查名额」和「扣减」焊死成数据库行锁内的原子操作：

```sql
UPDATE challenge SET joined_slots = joined_slots + 1
WHERE id = ? AND joined_slots < total_slots
```

- 无论多少并发同时到达，数据库行锁让请求串行执行，只有有空位时才更新成功，精确满员即失败，**不误杀合法请求**。
- 正确性**只**依赖这条 SQL，不依赖分布式锁——避免把一致性押在 Redis 上。
- 重复报名由唯一索引 `(user_id, challenge_id)` 兜底。

**压测验证**：wrk 200 并发抢 100 名额，最终 `joined_slots` 与报名记录数严格一致，均为 **100，零超卖**；吞吐约 **1.4 万 TPS**（开发机单机）。

### 2. 三层并发防线，各司其职

| 机制 | 职责 | 技术 |
|------|------|------|
| 原子条件 UPDATE | **防超卖**（正确性根基） | DB 行锁 |
| Redisson 分布式锁 | **接口防抖**（拦用户连点） | `RLock` 可重入 + owner 校验 |
| AOP 限流 | **挡突发流量** | Redisson `RRateLimiter` 令牌桶 |

- **防抖锁**：从手写 `SETNX + Lua` 升级为 Redisson `RLock`，可重入、解锁原子性由其内部保证；用固定租期（`tryLock(0, 10s)`）而非看门狗——报名业务极快无需自动续期，固定租期反而能在业务异常卡死时兜底释放。
- **限流**：自定义 `@RateLimit` 注解 + AOP 切面，`RateType.OVERALL` 多实例共享配额（真·分布式限流），业务代码零侵入。报名接口限每用户每秒 5 次，超出快速拒绝、不进 Service/事务。

### 3. 缓存三件套

`RedisCacheHelper` 统一封装高并发读路径：

- **防击穿**：逻辑过期 + 单飞重建（命名守护线程池异步重建），过期后返回旧值不阻塞。
- **防穿透**：null 哨兵缓存不存在的 id。
- **防雪崩**：写缓存时加随机 jitter，避免同批 key 同时过期。

### 4. 可靠消息投递（Transactional Outbox）

业务写与消息投递解耦，保证「业务成功 → 事件必达」：

- 报名/取消时，业务写与 **outbox 记录在同一事务**提交。
- `OutboxRelayScheduler` 轮询 PENDING 记录投递到 MQ，投递后标记 DELIVERED——即使应用在 COMMIT 后宕机，重启仍能从 outbox 表恢复投递，**不丢事件**。
- 多实例部署用 Redis 锁抢占，避免重复投递。
- 消费端 **SETNX 原子抢占幂等 key**；ZINCRBY 非幂等问题用 DB 流水唯一约束兜底 + 定时对账修正 Redis 漂移。
- 重试耗尽进 DLQ 并持久化，支持手动重放。

### 5. Elasticsearch 全文检索

场馆与挑战双索引，弥补 MySQL `LIKE` 不分词、不走索引的短板：

- **IK 中文分词**：建索引 `ik_max_word`、查询 `ik_smart`，搜"篮球"能从"朝阳体育馆篮球中心"分词命中。
- **多维检索**：bool 查询（关键词 multi_match + 类型/地区/评分/状态过滤），场馆搜索附带类型/地区聚合 facet。
- **增量同步**：复用 Outbox + MQ，统一的 `IndexSyncEvent` + `IndexSyncListener` 按聚合类型分发，spot/challenge 共用一条同步通道；消费端回源读 DB 写 ES，天然幂等、最终一致。
- 支持全量 reindex 初始化。

---

## API 概览

> 完整文档：Swagger UI `http://localhost:8080/swagger-ui/index.html`

| 模块 | 主要接口 |
|------|----------|
| 用户认证 | `POST /user/code` `POST /user/login` `POST /user/register` `GET /user/me` |
| 场馆 | `GET /spots/{id}` `PUT /spots` `POST /spots/search`（MySQL）|
| 场馆搜索(ES) | `POST /spots/search/es` `POST /spots/search/reindex` |
| 挑战 | `GET /challenge/list` `POST /challenge/add` `PUT /challenge/update` `DELETE /challenge/{id}` `POST /challenge/register/{id}` `POST /challenge/cancel/{id}` |
| 挑战搜索(ES) | `POST /challenge/search/es` `POST /challenge/search/reindex` |
| 收藏 | `POST /favorite/spots/{id}` `DELETE /favorite/spots/{id}` `GET /favorite/spots` |
| 通知 | `GET /notification/list` `PUT /notification/read/{id}` |
| 排行榜 | `GET /leaderboard/spots/heat` `GET /leaderboard/users/score` |
| MQ 可靠性 | `GET /mq/failed/list` `POST /mq/failed/retry/{id}` |

---

## 数据模型

主要表：`user`、`spots`、`challenge`、`challenge_participation`、`notifications`、`spot_favorites`、`outbox_event`、`leaderboard_event_log`、`failed_message`。

关键约束：
- `challenge_participation` 唯一索引 `(user_id, challenge_id)` —— 防重复报名
- `spot_favorites` 唯一索引 `(user_id, spot_id)`
- `leaderboard_event_log` 唯一索引 `(user_id, challenge_id, event_type)` —— ZINCRBY 幂等兜底
- `outbox_event` 按 `(status, created_at)` 建索引 —— relay 高效轮询

---

## 本地启动

### 依赖

| 服务 | 地址 |
|------|------|
| MySQL | `127.0.0.1:3306` |
| Redis | `127.0.0.1:6379` |
| RabbitMQ | `127.0.0.1:5672` |
| Elasticsearch（可选，仅搜索功能需要）| `127.0.0.1:9200`（需装 IK 8.15.5 插件）|

> ES 不可用时应用仍能正常启动（已做容错），仅搜索接口不可用。
> 也可用 `docker compose up` 一键拉起 MySQL/Redis/RabbitMQ/ES（含 IK 镜像）。

### 初始化数据库

```bash
mysql -u root -p SportX < src/main/resources/sql/schema.sql
mysql -u root -p SportX < src/main/resources/sql/outbox_event.sql
mysql -u root -p SportX < src/main/resources/sql/leaderboard_event_log.sql
mysql --default-character-set=utf8mb4 -u root -p SportX < src/main/resources/sql/seed_data.sql
```

### 运行

```bash
./mvnw spring-boot:run
```

ES 搜索首次使用前灌入数据：

```bash
curl -X POST http://localhost:8080/spots/search/reindex
curl -X POST http://localhost:8080/challenge/search/reindex
```

---

## 压测

并发报名压测脚本在 `loadtest/`：

```bash
bash loadtest/run.sh    # 重置 → 造token → sanity → wrk 200并发抢100名额 → 校验零超卖
```

验证结论：高并发下 `joined_slots` 与报名记录数严格一致（100/100），证明原子条件 UPDATE 防超卖有效。

---

## 工程说明

- 可靠性特性（Outbox、幂等、对账、DLQ）是主体设计的一部分，不是事后补丁。
- 并发控制按「一致性要求」分层：报名要强一致 → DB 原子 UPDATE；防抖/限流不承担正确性 → Redis/Redisson。
- 搜索同步复用消息基础设施，新增可搜索资源只需在统一 listener 加一个分支，无需新建队列。
