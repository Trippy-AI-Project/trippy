# Notification Service

## Overview
This service handles all outbound notifications (email, push, in-app) for the Trippy platform.

## Responsibilities
- **Email Notifications**: Transactional emails (invitations, receipts, alerts)
- **Push Notifications**: Mobile/web push for real-time alerts
- **In-App Notifications**: Notification feed/inbox
- **Template Management**: Email/notification templates
- **Delivery Tracking**: Track notification delivery status

## Domain Entities
- Notification
- NotificationTemplate
- DeliveryStatus
- UserPreferences

## Technology Stack
- Spring Boot
- Spring Mail (SMTP)
- Firebase Cloud Messaging (Push)
- PostgreSQL (schema: `notification_schema`)
- RabbitMQ (event consumption)
- Thymeleaf (email templates)

## Architecture Pattern
- Layered Architecture

## Port
- Default: `8085`

## Events Consumed (RabbitMQ)
- `payment/succeeded` (Email Receipt)
- `payment/failed` (Alert User)
- `trip/invitation/created` (Push Notification)
- `user/subscription/limit-reached` (Upsell Alert)

## Related Contracts
- `AsyncAPI/TripPlanner_AsyncAPI.yaml`
