package pse.trippy.aiservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pse.trippy.aiservice.dto.request.AiChatRequest;
import pse.trippy.aiservice.dto.request.DestinationSuggestionRequest;
import pse.trippy.aiservice.dto.request.GenerateItineraryRequest;
import pse.trippy.aiservice.dto.request.TravelAdviceRequest;
import pse.trippy.aiservice.dto.response.AiChatResponse;
import pse.trippy.aiservice.dto.response.AiUsageResponse;
import pse.trippy.aiservice.dto.response.DestinationSuggestionResponse;
import pse.trippy.aiservice.dto.response.ItineraryGenerationResponse;
import pse.trippy.aiservice.dto.response.ItineraryResponse;
import pse.trippy.aiservice.dto.response.TravelAdviceResponse;
import pse.trippy.aiservice.service.AiCacheService;
import pse.trippy.aiservice.service.AiItineraryService;
import pse.trippy.aiservice.service.AiService;
import pse.trippy.aiservice.service.AiUsageService;

import java.util.UUID;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final AiItineraryService aiItineraryService;
    private final AiUsageService aiUsageService;
    private final AiCacheService aiCacheService;

    /**
     * POST /ai/destination-suggestions
     * Protected via JWT (injected by API Gateway).
     */
    @PostMapping("/destination-suggestions")
    public ResponseEntity<DestinationSuggestionResponse> suggestDestinations(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @Valid @RequestBody DestinationSuggestionRequest request) {
        return ResponseEntity.ok(aiService.suggestDestinations(userId, request));
    }

    /**
     * POST /ai/travel-advice
     * Protected via JWT (injected by API Gateway).
     */
    @PostMapping("/travel-advice")
    public ResponseEntity<TravelAdviceResponse> getTravelAdvice(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @Valid @RequestBody TravelAdviceRequest request) {
        return ResponseEntity.ok(aiService.getTravelAdvice(userId, request));
    }

    /**
     * POST /ai/chat
     * Conversational trip assistant. Can update an itinerary when one is supplied.
     */
    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @Valid @RequestBody AiChatRequest request) {
        return ResponseEntity.ok(aiService.chat(userId, request));
    }

    /**
     * POST /ai/itineraries
     * Internal S2S endpoint — called by Trip Service.
     */
    @PostMapping("/itineraries")
    public ResponseEntity<ItineraryResponse> generateItinerary(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @Valid @RequestBody GenerateItineraryRequest request) {
        return ResponseEntity.ok(aiService.generateItinerary(userId, request));
    }

    /**
     * POST /ai/itinerary/generate
     * Public contract behind API Gateway /api prefix.
     */
    @PostMapping("/itinerary/generate")
    public ResponseEntity<ItineraryGenerationResponse> generateTrackedItinerary(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody GenerateItineraryRequest request) {
        return ResponseEntity.ok(aiItineraryService.generateItinerary(userId, request));
    }

    @GetMapping("/usage/{userId}")
    public ResponseEntity<AiUsageResponse> getUsage(
            @RequestHeader("X-User-Id") UUID authenticatedUserId,
            @PathVariable UUID userId) {
        if (!authenticatedUserId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot access another user's AI usage");
        }
        return ResponseEntity.ok(aiUsageService.getUsage(userId));
    }

    @DeleteMapping("/cache/{type}")
    public ResponseEntity<Void> evictCache(@PathVariable String type) {
        aiCacheService.evictCache(type);
        return ResponseEntity.noContent().build();
    }
}
