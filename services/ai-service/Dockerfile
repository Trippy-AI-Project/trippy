# =============================================================================
# ai-service — Multi-stage Docker build
# Build context: project root (trippy/)
# =============================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

# 1. Copy all pom.xml files first (cached layer — reused unless a pom changes)
COPY pom.xml ./
COPY services/pom.xml ./services/
COPY services/api-gateway/pom.xml ./services/api-gateway/
COPY services/ai-service/pom.xml ./services/ai-service/
COPY services/chat-service/pom.xml ./services/chat-service/
COPY services/notification-service/pom.xml ./services/notification-service/
COPY services/payment-service/pom.xml ./services/payment-service/
COPY services/trip-service/pom.xml ./services/trip-service/
COPY services/user-service/pom.xml ./services/user-service/

# 2. Download dependencies (cached layer — reused unless pom changes)
RUN mvn -pl services/ai-service dependency:go-offline -q 2>/dev/null || true

# 3. Copy source and build
COPY services/ai-service/src ./services/ai-service/src
RUN mvn -pl services/ai-service package -DskipTests -q

# =============================================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /workspace/services/ai-service/target/*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
