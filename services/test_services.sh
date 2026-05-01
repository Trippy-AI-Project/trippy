#!/bin/bash
source /Users/syed/Documents/PSE/SP1_Trippy/trippy/services/local-dev.env

SERVICES=("user-service" "trip-service" "chat-service" "ai-service" "notification-service" "payment-service" "api-gateway")

for svc in "${SERVICES[@]}"; do
    echo "======================================"
    echo "Testing $svc"
    echo "======================================"
    cd /Users/syed/Documents/PSE/SP1_Trippy/trippy/services/$svc
    
    # Kill any existing process on its port
    PORT=$(grep "^server\.port=" src/main/resources/application.properties 2>/dev/null | cut -d= -f2 | tr -d '\r')
    if [ -z "$PORT" ]; then
        PORT=$(grep "^ *port:" src/main/resources/application.yml 2>/dev/null | head -1 | awk '{print $2}' | tr -d '\r')
    fi
    if [ -n "$PORT" ]; then
        lsof -ti :$PORT | xargs kill -9 2>/dev/null
    fi

    # Run in background
    mvn spring-boot:run > startup.log 2>&1 &
    PID=$!
    
    # Wait for up to 60 seconds for it to start
    STARTED=0
    for i in {1..60}; do
        if grep -q "Started .*Application in" startup.log 2>/dev/null; then
            STARTED=1
            break
        fi
        if grep -q "APPLICATION FAILED TO START" startup.log 2>/dev/null; then
            break
        fi
        sleep 1
    done
    
    if [ $STARTED -eq 1 ]; then
        echo "✅ $svc started successfully!"
    else
        echo "❌ $svc failed to start. Last 20 lines of log:"
        tail -n 20 startup.log
    fi
    
    # Kill it
    kill -9 $PID 2>/dev/null
    wait $PID 2>/dev/null
    if [ -n "$PORT" ]; then
        lsof -ti :$PORT | xargs kill -9 2>/dev/null
    fi
    rm -f startup.log
done
