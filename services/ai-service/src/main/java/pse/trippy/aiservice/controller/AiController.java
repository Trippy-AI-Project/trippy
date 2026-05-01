package pse.trippy.aiservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pse.trippy.aiservice.dto.request.DestinationSuggestionRequest;
import pse.trippy.aiservice.dto.request.GenerateItineraryRequest;
import pse.trippy.aiservice.dto.request.TravelAdviceRequest;
import pse.trippy.aiservice.dto.response.DestinationSuggestionResponse;
import pse.trippy.aiservice.dto.response.ItineraryResponse;
import pse.trippy.aiservice.dto.response.TravelAdviceResponse;
import pse.trippy.aiservice.service.AiService;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /**
     * POST /ai/destination-suggestions
     * Protected via JWT (injected by API Gateway).
     */
    @PostMapping("/destination-suggestions")
    public ResponseEntity<DestinationSuggestionResponse> suggestDestinations(
            @Valid @RequestBody DestinationSuggestionRequest request) {
        return ResponseEntity.ok(aiService.suggestDestinations(request));
    }

    /**
     * POST /ai/travel-advice
     * Protected via JWT (injected by API Gateway).
     */
    @PostMapping("/travel-advice")
    public ResponseEntity<TravelAdviceResponse> getTravelAdvice(
            @Valid @RequestBody TravelAdviceRequest request) {
        return ResponseEntity.ok(aiService.getTravelAdvice(request));
    }

    /**
     * POST /ai/itineraries
     * Internal S2S endpoint — called by Trip Service.
     */
    @PostMapping("/itineraries")
    public ResponseEntity<ItineraryResponse> generateItinerary(
            @Valid @RequestBody GenerateItineraryRequest request) {
        return ResponseEntity.ok(aiService.generateItinerary(request));
    }

    @DeleteMapping("/cache/{type}")
    public ResponseEntity<Void> evictCache(@PathVariable String type) {
        aiCacheService.evictCache(type);
        return ResponseEntity.noContent().build();
    }
}
