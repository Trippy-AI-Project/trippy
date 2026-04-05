package pse.trippy.aiservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pse.trippy.aiservice.dto.request.DestinationSuggestionRequest;
import pse.trippy.aiservice.dto.request.GenerateItineraryRequest;
import pse.trippy.aiservice.dto.response.AiUsageResponse;
import pse.trippy.aiservice.dto.response.DestinationSuggestionResponse;
import pse.trippy.aiservice.dto.response.ItineraryGenerationResponse;
import pse.trippy.aiservice.service.AiCacheService;
import pse.trippy.aiservice.service.AiItineraryService;
import pse.trippy.aiservice.service.AiSuggestionService;
import pse.trippy.aiservice.service.AiUsageService;

import java.util.UUID;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiSuggestionService aiSuggestionService;
    private final AiItineraryService aiItineraryService;
    private final AiUsageService aiUsageService;
    private final AiCacheService aiCacheService;

    @PostMapping("/destination-suggestions")
    public ResponseEntity<DestinationSuggestionResponse> getDestinationSuggestions(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody DestinationSuggestionRequest request) {

        DestinationSuggestionResponse response = aiSuggestionService.getDestinationSuggestions(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/itinerary/generate")
    public ResponseEntity<ItineraryGenerationResponse> generateItinerary(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody GenerateItineraryRequest request) {

        ItineraryGenerationResponse response = aiItineraryService.generateItinerary(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/usage/{userId}")
    public ResponseEntity<AiUsageResponse> getUsage(@PathVariable UUID userId) {
        AiUsageResponse usage = aiUsageService.getUsage(userId);
        return ResponseEntity.ok(usage);
    }

    @DeleteMapping("/cache/{type}")
    public ResponseEntity<Void> evictCache(@PathVariable String type) {
        aiCacheService.evictCache(type);
        return ResponseEntity.noContent().build();
    }
}
