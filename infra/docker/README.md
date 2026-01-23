# Docker Infrastructure - Trippy Platform

Production-ready Docker Compose setup for the Trippy platform infrastructure.

## Services

| Service | Port(s) | Description | Credentials |
|---------|---------|-------------|-------------|
| **PostgreSQL** | 5432 | Primary database | See `.env` |
| **Redis** | 6379 | Cache, Session, Rate limiting | See `.env` |
| **RabbitMQ** | 5672, 15672, 61613 | Message broker + STOMP | See `.env` |
| **SonarQube** | 9000 | Code quality analysis | admin/admin (change!) |

## Quick Start

```bash
# 1. Copy environment file and configure
cp .env.example .env

# 2. Edit .env with your passwords (IMPORTANT!)
nano .env  # or use your preferred editor

# 3. Start all services
docker-compose up -d

# 4. Check status
docker-compose ps

# 5. View logs
docker-compose logs -f

# 6. Stop services
docker-compose down

# 7. Stop and remove volumes (CAUTION: deletes all data)
docker-compose down -v
```

## Directory Structure

```
docker/
├── docker-compose.yaml      # Main compose file
├── .env.example             # Environment template (copy to .env)
├── .env                     # Your local config (git-ignored)
├── config/
│   ├── postgres/
│   │   └── postgresql.conf  # PostgreSQL configuration
│   └── rabbitmq/
│       ├── rabbitmq.conf    # RabbitMQ configuration
│       └── definitions.json # Queues, exchanges, bindings
└── init-scripts/
    └── postgres/
        └── 01-init-databases.sql  # Schema & user creation
```

## Service Details

### PostgreSQL

- **Image**: `postgres:16-alpine`
- **Schemas Created**:
  - `user_schema` - User Service
  - `trip_schema` - Trip Service
  - `chat_schema` - Chat Service
  - `ai_schema` - AI Service
  - `notification_schema` - Notification Service
  - `payment_schema` - Payment Service
  - `sonarqube_db` - SonarQube database

**Connect via CLI:**
```bash
docker exec -it trippy-postgres psql -U trippy_admin -d trippy_db
```

**Connect via application:**
```
jdbc:postgresql://localhost:5432/trippy_db?currentSchema=user_schema
```

### Redis

- **Image**: `redis:7-alpine`
- **Features**: Password auth, AOF persistence, LRU eviction

**Connect via CLI:**
```bash
docker exec -it trippy-redis redis-cli -a YOUR_REDIS_PASSWORD
```

**Test connection:**
```bash
docker exec -it trippy-redis redis-cli -a YOUR_REDIS_PASSWORD ping
```

### RabbitMQ

- **Image**: `rabbitmq:3.13-management-alpine`
- **Protocols**: AMQP (5672), Management UI (15672), STOMP/WebSocket (61613)
- **VHost**: `trippy`

**Management UI:** http://localhost:15672

**Pre-configured Exchanges:**
- `trippy.events` (topic) - Event bus
- `trippy.notifications` (direct) - Notification routing
- `trippy.dlx` (direct) - Dead letter exchange

**Pre-configured Queues:**
- `notification.email`
- `notification.push`
- `chat.messages`

### SonarQube

- **Image**: `sonarqube:lts-community`
- **Purpose**: Code quality & security analysis
- **Default Login**: admin / admin (change on first login!)

**Web UI:** http://localhost:9000

**Configure Maven for SonarQube:**
```xml
<properties>
    <sonar.host.url>http://localhost:9000</sonar.host.url>
    <sonar.token>YOUR_SONAR_TOKEN</sonar.token>
</properties>
```

**Run analysis:**
```bash
mvn sonar:sonar
```

## Production Considerations

### Security Checklist

- [ ] Change all default passwords in `.env`
- [ ] Enable SSL/TLS for PostgreSQL
- [ ] Enable SSL/TLS for RabbitMQ
- [ ] Configure firewall rules
- [ ] Use secrets management (Docker Swarm secrets, Vault, etc.)
- [ ] Change SonarQube admin password

### Resource Tuning

Default resource limits (adjust in `.env`):

| Service | CPU | Memory | Memory Reservation |
|---------|-----|--------|-------------------|
| PostgreSQL | 2 | 2GB | 1GB |
| Redis | 1 | 1GB | 512MB |
| RabbitMQ | 1 | 1GB | 512MB |
| SonarQube | 2 | 4GB | 2GB |

### Backup

**PostgreSQL Backup:**
```bash
docker exec trippy-postgres pg_dump -U trippy_admin trippy_db > backup.sql
```

**PostgreSQL Restore:**
```bash
cat backup.sql | docker exec -i trippy-postgres psql -U trippy_admin -d trippy_db
```

**Redis Backup:**
```bash
docker exec trippy-redis redis-cli -a YOUR_PASSWORD BGSAVE
docker cp trippy-redis:/data/dump.rdb ./redis-backup.rdb
```

### Health Checks

All services include health checks. View status:
```bash
docker inspect --format='{{json .State.Health}}' trippy-postgres | jq
docker inspect --format='{{json .State.Health}}' trippy-redis | jq
docker inspect --format='{{json .State.Health}}' trippy-rabbitmq | jq
docker inspect --format='{{json .State.Health}}' trippy-sonarqube | jq
```

## Troubleshooting

### SonarQube won't start
```bash
# Increase vm.max_map_count (required for Elasticsearch)
sudo sysctl -w vm.max_map_count=262144

# Make it permanent
echo "vm.max_map_count=262144" | sudo tee -a /etc/sysctl.conf
```

### PostgreSQL permission issues
```bash
# Reset volumes
docker-compose down -v
docker-compose up -d
```

### RabbitMQ connection refused
```bash
# Check if STOMP plugin is enabled
docker exec trippy-rabbitmq rabbitmq-plugins list | grep stomp

# Enable STOMP plugin
docker exec trippy-rabbitmq rabbitmq-plugins enable rabbitmq_stomp
```

### View container logs
```bash
docker-compose logs postgres
docker-compose logs redis
docker-compose logs rabbitmq
docker-compose logs sonarqube
```

## Network

All services are on the `trippy-network` bridge network.

Service discovery within Docker:
- PostgreSQL: `postgres:5432`
- Redis: `redis:6379`
- RabbitMQ: `rabbitmq:5672`
- SonarQube: `sonarqube:9000`
