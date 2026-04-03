package pse.trippy.aiservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pse.trippy.aiservice.dto.request.DestinationSuggestionRequest;
import pse.trippy.aiservice.dto.response.AiUsageResponse;
import pse.trippy.aiservice.dto.response.DestinationSuggestionResponse;
import pse.trippy.aiservice.service.AiSuggestionService;
import pse.trippy.aiservice.service.AiUsageService;

import java.util.UUID;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiSuggestionService aiSuggestionService;
    private final AiUsageService aiUsageService;

    @PostMapping("/destination-suggestions")
    public ResponseEntity<DestinationSuggestionResponse> getDestinationSuggestions(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody DestinationSuggestionRequest request) {

        DestinationSuggestionResponse response = aiSuggestionService.getDestinationSuggestions(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/usage/{userId}")
    public ResponseEntity<AiUsageResponse> getUsage(@PathVariable UUID userId) {
        AiUsageResponse usage = aiUsageService.getUsage(userId);
        return ResponseEntity.ok(usage);
    }
}
