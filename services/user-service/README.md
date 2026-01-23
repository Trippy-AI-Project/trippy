# User Service

## Overview
This service manages user identity, profiles, and subscription management for the Trippy platform.

## Responsibilities
- **User Registration & Authentication**: Email/phone signup, credential validation
- **Profile Management**: User profiles with name, bio, avatar, country
- **Subscription Management**: FREE, PREMIUM, ENTERPRISE tier handling
- **Quota Tracking**: Trip creation limits based on subscription tier
- **Verification**: User verification status for hosts and group participants

## Domain Entities
- User
- UserProfile
- Subscription
- VerificationStatus

## Technology Stack
- Spring Boot
- Spring Data JPA
- PostgreSQL (schema: `user_schema`)
- Spring Security

## Port
- Default: `8081`

## Events Published (RabbitMQ)
- `user/subscription/limit-reached`

## Related Contracts
- `userServiceContract.yaml`
- `service_External_comms/User-Service.yaml`
