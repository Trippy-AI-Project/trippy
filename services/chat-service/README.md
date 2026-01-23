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
