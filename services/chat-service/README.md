# Chat Service

Real-time, per-trip group chat for the Trippy platform. Built on Spring Boot 3 +
Spring WebSocket (STOMP) with PostgreSQL persistence and a RabbitMQ-relayed
broker. The service registers its STOMP/SockJS endpoint at `/ws`; through the
API Gateway the WebSocket path is `/ws/**`. REST endpoints are exposed under
`/trips/*/chat/*` and `/chats/*`.

---

## Responsibilities

- **Real-time messaging** over WebSocket/STOMP, fanned out via RabbitMQ STOMP relay.
- **Message persistence** in `chat_schema.chat_messages` + per-trip `chat_rooms`.
- **Participant verification** on subscribe (calls trip-service to confirm the
  user belongs to the trip before letting them subscribe to `/topic/trips/{id}/messages`).
- **System messages** ("X joined/left the chat") broadcast on connect/disconnect.
- **Participant presence queries** via `GET /chats/{tripId}/participants`.
- **Attachments** (10 MB limit, image/PDF/Office types) with auto-generated
  200×200 JPEG thumbnails for images.

## Tech stack

| Layer            | Choice                                          |
|------------------|-------------------------------------------------|
| Runtime          | Java 21, Spring Boot 3.4.x                      |
| Messaging        | Spring WebSocket + STOMP over RabbitMQ          |
| Persistence      | Spring Data JPA + PostgreSQL 16 (`chat_schema`) |
| Presence cache   | In-memory `ConcurrentHashMap`                   |
| Service discovery| Direct hostnames via Docker DNS                 |
| Default port     | **8083**                                        |

## Configuration

All settings are environment-variable driven (no hardcoded secrets). Defaults
shown in parentheses.

| Variable                                 | Purpose                                         |
|------------------------------------------|-------------------------------------------------|
| `POSTGRES_HOST` / `POSTGRES_PORT`        | Postgres host (`localhost`) and port (`5434`)    |
| `POSTGRES_DB`                            | Database name (`trippy_db`)                     |
| `DB_USERNAME` / `DB_PASSWORD`            | Postgres credentials                            |
| `RABBITMQ_HOST` / `RABBITMQ_PORT`        | RabbitMQ broker host (`localhost`) / AMQP port (`5672`) |
| `MQ_USERNAME` / `MQ_PASSWORD`            | RabbitMQ credentials                            |
| `RABBITMQ_DEFAULT_VHOST`                 | RabbitMQ vhost (`trippy`)                       |
| STOMP relay port                         | Fixed to `61613` in `WebSocketConfig`           |
| `TRIP_SERVICE_URL`                       | Base URL for trip-service participant check (`http://localhost:8082`) |
| `trippy.chat.upload-dir`                 | Attachment storage root (`./uploads/chat`)      |

## Run it

### Via docker-compose (recommended)

```bash
cd infra/docker
docker compose up -d --build
docker compose logs -f
```

### Standalone (against already-running infra)

```bash
./mvnw -pl services/chat-service spring-boot:run
```

Health check:

```bash
curl http://localhost:8083/actuator/health
# {"status":"UP"}
```

## REST endpoints

| Method | Path                                       | Purpose                                |
|--------|--------------------------------------------|----------------------------------------|
| GET    | `/trips/{tripId}/chat/messages`            | Paged message history                  |
| POST   | `/trips/{tripId}/chat/messages`            | Send a text message (REST fallback)    |
| POST   | `/trips/{tripId}/chat/messages/file`       | Upload image/file (multipart, ≤ 10 MB) |
| GET    | `/chats/{tripId}/attachments`              | List all attachments for a trip        |
| GET    | `/chats/files/{tripId}/{filename}`         | Download an original attachment        |
| GET    | `/chats/files/{tripId}/thumbs/{filename}`  | Download an image thumbnail (200×200)  |
| GET    | `/chats/{tripId}/participants`             | Currently connected user IDs           |
| GET    | `/actuator/health`                         | Liveness + readiness                   |

Upload failure modes (returned via `GlobalExceptionHandler`):

- `415 Unsupported Media Type` for disallowed content types
- `413 Payload Too Large` for files > 10 MB

## WebSocket / STOMP

Endpoint (through the gateway): **`ws://localhost:8080/ws`** (SockJS).
Direct: `ws://localhost:8083/ws`.

| Direction | Destination                              | Payload                     |
|-----------|------------------------------------------|-----------------------------|
| SUBSCRIBE | `/topic/trips/{tripId}/messages`         | `ChatMessageResponse`       |
| SUBSCRIBE | `/topic/trips/{tripId}/participants`     | `Set<UUID>` of online users |
| SEND      | `/app/trips/{tripId}/send`               | `{ content, type }`         |

The `WebSocketAuthChannelInterceptor` rejects SUBSCRIBE frames for users who
are not participants of the trip, throwing `MessageDeliveryException`. Clients
must include `X-User-Id` and `X-User-DisplayName` as STOMP CONNECT headers
(the API Gateway extracts these from the validated JWT and forwards them).

## Domain entities

- `ChatRoom` — one per trip (`chat_schema.chat_rooms`)
- `ChatMessage` — `(id, room_id, sender_id, sender_display_name, content, message_type, deleted, created_at, edited_at)`
- `MessageAttachment` — `(id, message_id, file_name, file_url, file_size, content_type)`

## Events consumed (RabbitMQ)

- `trip.participant.joined` → emits SYSTEM message *"X joined the trip"*

## Related contracts

- `contracts/chatServiceContract.yaml`

## How to verify it works locally

1. Start infrastructure: `cd infra/docker && docker compose up -d`.
2. Start chat-service: `./mvnw -pl services/chat-service spring-boot:run`.
3. `curl -i http://localhost:8083/actuator/health` → expect `200 UP`.
4. Open `http://localhost:3000/dashboard/chat/{tripId}` in the frontend with a
   logged-in user that participates in the trip; observe live message flow and
   the "X joined the chat" system message.
5. Upload an image → `GET /chats/{tripId}/attachments` lists the attachment.
6. Try uploading a `.exe` → expect HTTP **415**. Upload a >10 MB file → **413**.
7. Open in two browsers — closing one tab should produce a *"left the chat"*
   system message in the other.
