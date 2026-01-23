# AI Service

## Overview
This service handles AI-powered trip planning features including destination suggestions and itinerary generation for the Trippy platform.

## Responsibilities
- **Destination Discovery**: Natural language search for travel destinations
- **Itinerary Generation**: Day-by-day trip plans based on user preferences
- **Preference Consolidation**: Merge group members' preferences for group trips
- **Weather Integration**: Include weather context in suggestions
- **Fallback Suggestions**: Alternative recommendations when direct matches unavailable

## Domain Entities
- SuggestionRequest
- DestinationSuggestion
- GenerationRequest
- ItineraryResponse
- TripConstraints

## Technology Stack
- Spring Boot
- Spring AI / LangChain4j
- OpenAI API / Ollama (LLM provider)
- PostgreSQL (schema: `ai_schema`) - for caching/logging
- Redis (response caching)

## Architecture Pattern
- Hexagonal Architecture (Ports & Adapters)

## Port
- Default: `8084`

## Related Contracts
- `AIService_openAPI.yaml`
- `service_External_comms/AI-service.yaml`
