# Payment Service

## Overview
This service handles all payment processing for trip bookings and subscription purchases on the Trippy platform.

## Responsibilities
- **Payment Processing**: Handle card transactions (via Stripe/mock provider)
- **Subscription Billing**: PREMIUM/ENTERPRISE subscription purchases
- **Trip Booking Payments**: Pay for trip-related bookings
- **Transaction History**: Store and retrieve payment records
- **Refund Processing**: Handle refund requests

## Domain Entities
- Payment
- Transaction
- PaymentMethod
- Refund

## Technology Stack
- Spring Boot
- Spring Data JPA
- PostgreSQL (schema: `payment_schema`)
- Stripe SDK (or mock implementation)
- RabbitMQ (event publishing)

## Architecture Pattern
- Layered Architecture

## Port
- Default: `8086`

## Events Published (RabbitMQ)
- `payment/succeeded`
- `payment/failed`

## Related Contracts
- `service_External_comms/Payment-processing.yaml`
- `AsyncAPI/TripPlanner_AsyncAPI.yaml`
