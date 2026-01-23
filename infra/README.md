# Infrastructure

This directory contains all infrastructure-related configurations for the Trippy platform.

## Structure

```
infra/
├── docker/           # Docker Compose and Dockerfile configurations
├── sql/              # Database migration scripts
└── k8s/              # Kubernetes manifests (future)
```

## Directories

### `/docker`
Contains Docker Compose configuration for local development environment including:
- PostgreSQL database
- RabbitMQ message broker
- Redis cache
- Service containers

### `/sql`
Contains database schema creation and migration scripts.
Each service uses a separate PostgreSQL schema for isolation.

**Schemas:**
- `user_schema` - User Service
- `trip_schema` - Trip Service
- `chat_schema` - Chat Service
- `ai_schema` - AI Service
- `notification_schema` - Notification Service
- `payment_schema` - Payment Service

### `/k8s`
Kubernetes deployment manifests (to be added when transitioning to Kubernetes).
