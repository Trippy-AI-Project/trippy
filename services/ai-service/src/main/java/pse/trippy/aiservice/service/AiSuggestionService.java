package pse.trippy.aiservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import pse.trippy.aiservice.dto.request.DestinationSuggestionRequest;
import pse.trippy.aiservice.dto.response.DestinationSuggestion;
import pse.trippy.aiservice.dto.response.DestinationSuggestionResponse;
import pse.trippy.aiservice.model.entity.AiRequestLog;
import pse.trippy.aiservice.model.enums.RequestStatus;
import pse.trippy.aiservice.model.enums.RequestType;
import pse.trippy.aiservice.repository.AiRequestLogRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiSuggestionService {

    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final String CACHE_TYPE = "suggestion";

    private final ChatClient.Builder chatClientBuilder;
    private final AiRequestLogRepository aiRequestLogRepository;
    private final ObjectMapper objectMapper;
    private final AiCacheService aiCacheService;

    public DestinationSuggestionResponse getDestinationSuggestions(UUID userId, DestinationSuggestionRequest request) {
        String requestHash = aiCacheService.generateHash(request);

        Optional<String> cached = aiCacheService.getCachedResponse(CACHE_TYPE, requestHash);
        if (cached.isPresent()) {
            try {
                List<DestinationSuggestion> suggestions = objectMapper.readValue(
                        cached.get(), new TypeReference<>() {});
                log.info("Returning cached suggestion response for user {}", userId);
                return new DestinationSuggestionResponse(suggestions, Instant.now(), true);
            } catch (Exception ex) {
                log.warn("Failed to parse cached response, falling through to AI: {}", ex.getMessage());
            }
        }

        long startTime = System.currentTimeMillis();
        String prompt = buildPrompt(request);
        String promptHash = hashPrompt(prompt);

        try {
            ChatClient chatClient = chatClientBuilder.build();

            String aiResponse = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            long responseTimeMs = System.currentTimeMillis() - startTime;

            List<DestinationSuggestion> suggestions = parseResponse(aiResponse);

            logRequest(userId, RequestType.DESTINATION_SUGGESTION, promptHash, responseTimeMs, RequestStatus.SUCCESS);

            try {
                String jsonToCache = objectMapper.writeValueAsString(suggestions);
                aiCacheService.cacheResponse(CACHE_TYPE, requestHash, jsonToCache, CACHE_TTL);
            } catch (Exception ex) {
                log.warn("Failed to cache suggestion response: {}", ex.getMessage());
            }

            return new DestinationSuggestionResponse(suggestions, Instant.now(), false);

        } catch (Exception ex) {
            long responseTimeMs = System.currentTimeMillis() - startTime;
            logRequest(userId, RequestType.DESTINATION_SUGGESTION, promptHash, responseTimeMs, RequestStatus.FAILED);
            log.error("AI API call failed for user {}: {}", userId, ex.getMessage());
            throw new AiServiceUnavailableException("AI service is currently unavailable. Please try again later.");
        }
    }

    String buildPrompt(DestinationSuggestionRequest request) {
        return String.format("""
                You are a travel expert. Suggest 3-5 travel destinations based on these preferences:
                
                User Request: %s
                Requested City: %s
                Interests: %s
                Budget: %s
                Travel Style: %s
                Duration: %d days
                Travelers: %s
                Dietary Requirements: %s
                Preferences: %s
                Custom Notes: %s
                Region: %s
                Month: %s
                
                Return your response as either a JSON array or a JSON object with a "suggestions" array.
                Use objects containing:
                - "destination": full destination name (city, country)
                - "country": country name
                - "description": 1-2 sentence description
                - "highlights": array of 3 top highlights/attractions
                - "estimatedDailyCost": estimated daily cost in EUR as a number
                - "bestTimeToVisit": best months to visit
                - "matchScore": how well this matches the preferences (0.0 to 1.0)
                
                Return ONLY JSON, no markdown and no other text.
                """,
                request.prompt() != null ? request.prompt() : "none",
                request.city() != null ? request.city() : "none",
                request.interests() != null && !request.interests().isEmpty()
                        ? String.join(", ", request.interests())
                        : "any",
                request.budget(),
                request.travelStyle() != null ? request.travelStyle() : "any",
                request.duration(),
                request.people() != null ? request.people() : "not specified",
                request.diet() != null ? request.diet() : "none",
                request.preferences() != null ? request.preferences() : "none",
                request.customNotes() != null ? request.customNotes() : "none",
                request.region() != null ? request.region() : "worldwide",
                request.month() != null ? request.month() : "any time"
        );
    }

    List<DestinationSuggestion> parseResponse(String aiResponse) {
        try {
            String cleaned = aiResponse.strip();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
            }
            if (cleaned.startsWith("{")) {
                JsonNode root = objectMapper.readTree(cleaned);
                return objectMapper.convertValue(root.path("suggestions"), new TypeReference<>() {});
            }
            return objectMapper.readValue(cleaned, new TypeReference<>() {});
        } catch (Exception ex) {
            log.error("Failed to parse AI response: {}", ex.getMessage());
            throw new AiServiceUnavailableException("Failed to process AI response. Please try again.");
        }
    }

    private void logRequest(UUID userId, RequestType requestType, String promptHash,
                            long responseTimeMs, RequestStatus status) {
        try {
            AiRequestLog logEntry = AiRequestLog.builder()
                    .userId(userId)
                    .requestType(requestType)
                    .promptHash(promptHash)
                    .responseTimeMs(responseTimeMs)
                    .status(status)
                    .build();
            aiRequestLogRepository.save(logEntry);
        } catch (Exception ex) {
            log.error("Failed to log AI request: {}", ex.getMessage());
        }
    }

    private String hashPrompt(String prompt) {
        return aiCacheService.generateHash(prompt);
    }
}
