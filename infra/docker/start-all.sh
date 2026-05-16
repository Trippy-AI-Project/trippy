#!/bin/sh
# Start all Trippy services in background, then tail logs
set -e

echo "==> Starting User Service (8081)..."
java -jar /jars/user-service.jar &

echo "==> Starting Trip Service (8082)..."
java -jar /jars/trip-service.jar &

echo "==> Starting Chat Service (8083)..."
java -jar /jars/chat-service.jar &

echo "==> Starting AI Service (8084)..."
java -jar /jars/ai-service.jar &

echo "==> Starting Notification Service (8085)..."
java -jar /jars/notification-service.jar &

echo "==> Starting Payment Service (8086)..."
java -jar /jars/payment-service.jar &

# Give backend services a head start before starting gateway
sleep 8

echo "==> Starting API Gateway (8080)..."
java -jar /jars/api-gateway.jar &

echo "==> All services starting. Waiting..."
wait
