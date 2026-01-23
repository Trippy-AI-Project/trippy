-- ============================================================
-- Trippy Platform - Database Schema Creation Script
-- ============================================================
-- This script creates isolated schemas for each microservice.
-- Each service owns its schema and manages its own tables.
-- ============================================================

-- Schema for User Service
-- Tables: users, user_profiles, subscriptions, verification_status

-- Schema for Trip Service
-- Tables: trips, itineraries, day_plans, activities, participants, invitations

-- Schema for Chat Service
-- Tables: chat_rooms, chat_messages, message_attachments

-- Schema for AI Service
-- Tables: ai_request_logs, cached_suggestions

-- Schema for Notification Service
-- Tables: notifications, notification_templates, delivery_status, user_preferences

-- Schema for Payment Service
-- Tables: payments, transactions, payment_methods, refunds

