# Chat Service

## Overview
This service provides real-time messaging capabilities for trip group collaboration on the Trippy platform.

## Responsibilities
- **Real-Time Messaging**: WebSocket-based instant messaging
- **Message Persistence**: Store and retrieve chat history
- **Chat Rooms**: Per-trip group chat rooms
- **System Messages**: Auto-generated notifications (e.g., "Alice joined the trip")
- **Attachment Handling**: Image and file sharing in chat
- **User Presence**: Online/offline status tracking

## Domain Entities
- ChatRoom
- ChatMessage
- MessageAttachment

## Technology Stack
- Spring Boot
- Spring WebSocket (STOMP)
- Spring Data JPA
- PostgreSQL (schema: `chat_schema`)
- RabbitMQ (event consumption)
- Redis (presence/session cache)

## Port
- Default: `8083`

## Events Consumed (RabbitMQ)
- `trip/participant/joined` (System message: "X joined")

## Related Contracts
- `chatService_openAPI.yaml`
- `AsyncAPI/TripPlanner_AsyncAPI.yaml`

## Sprint 3 / 4 Features

### Ticket 3.8 — Typing Indicator (Backend)
Debounced "is typing…" events broadcast over STOMP so chat rooms feel alive.

- **STOMP inbound:**  `/app/trips/{tripId}/typing`  →  body `{ "typing": true|false }`
- **STOMP outbound:** `/topic/trips/{tripId}/typing`  →  `TypingEvent { tripId, userId, displayName, typing }`
- **Debounce:** typing state held in Redis key `typing:trip:{tripId}:user:{userId}` with TTL `5s`; repeated `typing=true` frames simply refresh the TTL — only state transitions (start ↔ stop) trigger a broadcast.

### Ticket 3.9 — Presence Tracking (Redis-backed)
Sprint-2 in-memory `ConcurrentHashMap` replaced with Redis sets so presence survives a single-instance restart and scales horizontally.

- **Redis key:** `presence:trip:{tripId}` (Set of userId strings, safety-net TTL `24h`, reset on every join)
- **STOMP topic (unchanged):** `/topic/trips/{tripId}/participants`
- Operations: `SADD` on join, `SREM` on leave, `SMEMBERS` for snapshot.

### Ticket 4.4 — Chat Moderation API
Custom Spring Security pipeline lets moderators ban/mute users and soft-delete messages.

**REST endpoints** (all require `ROLE_ADMIN`, enforced via API gateway-injected `X-User-Role` header):

| Method | Path                                                            | Purpose                       |
|--------|-----------------------------------------------------------------|-------------------------------|
| POST   | `/admin/chat/users/{userId}/ban?durationMinutes=N`              | Ban user (0 = max 30 days)    |
| DELETE | `/admin/chat/users/{userId}/ban`                                | Lift ban                      |
| POST   | `/admin/chat/users/{userId}/mute?durationMinutes=N`             | Mute user                     |
| DELETE | `/admin/chat/users/{userId}/mute`                               | Lift mute                     |
| DELETE | `/admin/chat/messages/{messageId}`                              | Soft-delete a message         |

- **Redis keys:** `moderation:ban:{userId}`, `moderation:mute:{userId}` (TTL = ban/mute duration; auto-lift on expiry).
- **STOMP enforcement:** `WebSocketAuthChannelInterceptor.handleSend()` checks ban/mute on every SEND and rejects with `MessageDeliveryException`.
- **Soft-delete:** sets `ChatMessage.deleted = true` (existing column; no migration needed).

### WebSocket Auth Hardening
Closes the gap that `/ws/**` was a public gateway route → STOMP CONNECT frames now carry the raw JWT and the chat-service validates it itself.

- `JwtConfig` exposes a `NimbusJwtDecoder` backed by user-service JWKS (`trippy.jwt.jwks-uri`, default `http://localhost:8081/.well-known/jwks.json`).
- `WebSocketAuthChannelInterceptor.handleConnect()` validates the `Authorization: Bearer …` native header, sets `StompHeaderAccessor.setUser(...)` and stores `userId` / `displayName` in the WS session.
- Downstream `@MessageMapping` controllers (chat send, typing) derive identity from the principal/session attribute — they no longer trust client-supplied `X-User-Id` on the STOMP channel.
- HTTP routes (REST) continue to trust the gateway-injected `X-User-Id` / `X-User-Role` via `GatewayHeaderAuthFilter`.

## Required Environment

| Variable           | Default                                                 | Notes                                  |
|--------------------|---------------------------------------------------------|----------------------------------------|
| `POSTGRES_HOST`    | `localhost`                                             |                                        |
| `POSTGRES_PORT`    | `5434`                                                  |                                        |
| `POSTGRES_DB`      | `trippy_db`                                             |                                        |
| `DB_USERNAME`      | —                                                       | required                               |
| `DB_PASSWORD`      | —                                                       | required                               |
| `REDIS_HOST`       | `localhost`                                             |                                        |
| `REDIS_PORT`       | `6379`                                                  |                                        |
| `REDIS_PASSWORD`   | *(empty)*                                               | optional                               |
| `RABBITMQ_HOST`    | `localhost`                                             |                                        |
| `MQ_USERNAME`      | —                                                       | required                               |
| `MQ_PASSWORD`      | —                                                       | required                               |
| `TRIP_SERVICE_URL` | `http://localhost:8082`                                 |                                        |
| `JWKS_URI`         | `http://localhost:8081/.well-known/jwks.json`           | user-service public key endpoint       |

> All credentials are sourced from environment variables — no plaintext secrets are committed.

## Running Locally

```bash
# from repo root
JAVA_HOME=$JAVA_HOME ./mvnw -pl services/chat-service spring-boot:run
```

Requires Postgres, RabbitMQ and Redis to be reachable (see `infra/docker/`).

## Tests

```bash
JAVA_HOME=$JAVA_HOME ./mvnw -pl services/chat-service test
```

