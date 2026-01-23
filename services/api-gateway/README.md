# API Gateway Service

## Overview
This service acts as the single entry point for all client requests to the Trippy platform.

## Responsibilities
- **Request Routing**: Routes incoming requests to appropriate downstream microservices
- **Authentication & Authorization**: JWT validation and token verification
- **Rate Limiting**: Protects backend services from abuse
- **Load Balancing**: Distributes traffic across service instances
- **Request/Response Transformation**: Adapts payloads as needed
- **CORS Handling**: Manages cross-origin resource sharing policies

## Technology Stack
- Spring Cloud Gateway
- Spring Security (JWT)
- Redis (Rate limiting cache)

## Port
- Default: `8080`

## Related Contracts
- All public-facing OpenAPI contracts route through this gateway
