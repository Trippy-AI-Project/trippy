# =============================================================================
# payment-service — Multi-stage Docker build
# Build context: project root (trippy/)
# =============================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

COPY pom.xml ./
COPY services/pom.xml ./services/
COPY services/api-gateway/pom.xml ./services/api-gateway/
COPY services/ai-service/pom.xml ./services/ai-service/
COPY services/chat-service/pom.xml ./services/chat-service/
COPY services/notification-service/pom.xml ./services/notification-service/
COPY services/payment-service/pom.xml ./services/payment-service/
COPY services/trip-service/pom.xml ./services/trip-service/
COPY services/user-service/pom.xml ./services/user-service/

RUN mvn -pl services/payment-service dependency:go-offline -q 2>/dev/null || true

COPY services/payment-service/src ./services/payment-service/src
RUN mvn -pl services/payment-service package -DskipTests -q

# =============================================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /workspace/services/payment-service/target/*.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]
