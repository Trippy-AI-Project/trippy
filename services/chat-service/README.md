# Chat Service

Real-time, per-trip group chat for the Trippy platform. Built on Spring Boot 3 +
Spring WebSocket (STOMP) with PostgreSQL persistence and a RabbitMQ-relayed
broker, exposed via the API Gateway under `/ws/chat` and `/chats`, `/trips/*/chat/*`.

---

## Responsibilities

- **Real-time messaging** over WebSocket/STOMP, fanned out via RabbitMQ STOMP relay.
- **Message persistence** in `chat_schema.chat_messages` + per-trip `chat_rooms`.
- **Participant verification** on subscribe (calls trip-service to confirm the
  user belongs to the trip before letting them subscribe to `/topic/trips/{id}/messages`).
- **System messages** ("X joined/left the chat") broadcast on connect/disconnect.
- **Online-presence broadcast** on `/topic/trips/{id}/participants`.
- **Attachments** (10 MB limit, image/PDF/Office types) with auto-generated
  200x200 JPEG thumbnails for images.

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
| `SPRING_DATASOURCE_URL`                  | Postgres JDBC URL                               |
| `SPRING_DATASOURCE_USERNAME`             | DB user                                         |
| `SPRING_DATASOURCE_PASSWORD`             | DB password                                     |
| `SPRING_RABBITMQ_HOST`                   | RabbitMQ broker host                            |
| `SPRING_RABBITMQ_PORT`                   | AMQP port (5672)                                |
| `SPRING_RABBITMQ_USERNAME` / `_PASSWORD` | RabbitMQ creds                                  |
| `SPRING_RABBITMQ_VIRTUAL_HOST`           | RabbitMQ vhost (`/`)                            |
| `STOMP_RELAY_HOST` / `STOMP_RELAY_PORT`  | STOMP relay (`localhost:61613`)                 |
| `SPRING_DATA_REDIS_HOST` / `_PORT`       | Redis (optional, presence cache extension)      |
| `TRIPPY_CHAT_UPLOAD_DIR`                 | Attachment storage root (`./uploads/chat`)      |
| `TRIP_SERVICE_URL`                       | Base URL for trip-service participant check     |
| `JWT_SECRET`                             | Shared secret for verifying gateway-issued JWTs |

## Run it

### Via docker-compose (recommended)

```bash
cd infra/docker
./setup.sh                # builds all service images
docker compose up -d chat-service
docker compose logs -f chat-service
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
| GET    | `/chats/files/{tripId}/thumbs/{filename}`  | Download an image thumbnail (200x200)  |
| GET    | `/chats/{tripId}/participants`             | Currently connected user IDs           |
| GET    | `/actuator/health`                         | Liveness + readiness                   |

Upload failure modes (returned via `GlobalExceptionHandler`):

- `415 Unsupported Media Type` for disallowed content types
- `413 Payload Too Large` for files > 10 MB

## WebSocket / STOMP

Endpoint (through the gateway): **`ws://localhost:8080/ws/chat`** (SockJS or
native WebSocket). Direct: `ws://localhost:8083/ws/chat`.

| Direction | Destination                              | Payload                     |
|-----------|------------------------------------------|-----------------------------|
| SUBSCRIBE | `/topic/trips/{tripId}/messages`         | `ChatMessageResponse`       |
| SUBSCRIBE | `/topic/trips/{tripId}/participants`     | `Set<UUID>` of online users |
| SEND      | `/app/trips/{tripId}/send`               | `{ content, type }`         |

The `WebSocketAuthChannelInterceptor` rejects SUBSCRIBE frames for users who
are not participants of the trip, throwing `MessageDeliveryException`. Clients
must include the JWT in the CONNECT headers (`Authorization: Bearer …`).

## Domain entities

- `ChatRoom` — one per trip (`chat_schema.chat_rooms`)
- `ChatMessage` — `(id, chat_room_id, sender_id, sender_display_name, content, message_type, created_at)`
- `MessageAttachment` — `(id, message_id, file_name, file_url, file_size, content_type, thumbnail_url)`

## Events consumed (RabbitMQ)

- `trip.participant.joined` → emits SYSTEM message *"X joined the trip"*

## Related contracts

- `contracts/chatServiceContract.yaml`

## How to verify it works locally

1. `docker compose up -d` and wait for `chat-service` to be healthy.
2. `curl -i http://localhost:8083/actuator/health` → expect `200 UP`.
3. Open `http://localhost:3000/dashboard/chat/{tripId}` in the frontend with a
   logged-in user that participates in the trip; observe live message flow and
   the "X joined the chat" system message.
4. Upload an image → `GET /chats/{tripId}/attachments` shows `thumbnailUrl`.
5. Try uploading a `.exe` → expect HTTP **415**. Upload a >10 MB file → **413**.
6. Subscribe to `/topic/trips/{id}/participants` in two browsers — closing one
   tab should produce an updated participant set in the other.
