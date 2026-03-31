# SportX Platform Rules

## Registration Rules

- A user can register for a challenge only once.
- Registration is allowed only when the challenge is open for participation.
- Registration fails when the challenge does not exist.
- Registration fails when the challenge has reached full capacity.
- A successful registration increases the joined slot count of the challenge.
- Registration requires the user to be logged in with a valid token.
- Duplicate registrations are blocked by both business checks and database uniqueness constraints.
- Registration requests are protected by a Redis-based distributed lock to reduce concurrent duplicate submissions.
- A successful registration publishes a challenge event for downstream notification and leaderboard updates.

## Cancellation Rules

- A user can cancel only their own active participation record.
- A cancelled participation releases one slot back to the challenge.
- A cancelled participation should not be treated as an active registration.
- Cancellation requires the user to be logged in and to have an existing participation record.
- Cancellation updates challenge capacity immediately in the database.
- A successful cancellation publishes a challenge event for downstream notification handling.

## Notification Rules

- A successful registration triggers a signup success notification.
- A successful cancellation triggers a cancellation success notification.
- Challenge start reminders are sent to registered participants.
- Challenge end reminders are sent to registered participants.
- Notifications are persisted in the notifications table and can be queried by the target user.
- Notification events are consumed through RabbitMQ and protected by Redis idempotency keys.
- Failed notification messages can enter the dead-letter workflow and be retried manually.

## Score And Leaderboard Rules

- Spot heat is updated when users register for or cancel a challenge.
- User score can increase after successful challenge-related events.
- Leaderboards are generated from Redis sorted sets.
- Successful challenge registration increases user score and may affect leaderboard ranking.
- Challenge cancellation can decrease user score depending on the event type configured by the platform.
- Spot leaderboard and user leaderboard are maintained independently.

## Authentication Rules

- Login state is stored in Redis by token.
- Protected endpoints require a valid token.
- Token TTL is refreshed during normal authenticated requests.
- Repeated login failures may trigger temporary blocking.
- Password login uses encrypted password comparison rather than plain-text storage.
- Verification code sending and login attempts are rate-limited to reduce abuse.

## Favorite Rules

- A user can favorite a spot only once.
- A user can remove only their own favorite record.
- Favorite uniqueness is guaranteed by a database unique constraint on user and spot.
- Favorite list queries return only the current user's own records.

## Reliability Rules

- Challenge events are published through RabbitMQ.
- Event consumption uses idempotency keys in Redis.
- Failed messages can be retried and inspected through the failed message workflow.
- Challenge detail caching is used to reduce repeated database reads.
- Hot data such as leaderboards and login sessions are stored in Redis.
- Failed MQ messages can be inspected through dedicated APIs before manual replay.
