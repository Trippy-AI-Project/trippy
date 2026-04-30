package pse.trippy.aiservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiSuggestionService {

    private final ChatClient.Builder chatClientBuilder;
    private final AiRequestLogRepository aiRequestLogRepository;
    private final ObjectMapper objectMapper;

    public DestinationSuggestionResponse getDestinationSuggestions(UUID userId, DestinationSuggestionRequest request) {
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

            return new DestinationSuggestionResponse(suggestions, Instant.now());

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
                
                Interests: %s
                Budget: %s
                Travel Style: %s
                Duration: %d days
                Region: %s
                Month: %s
                
                Return your response as a JSON array (no markdown, no code fences) with objects containing:
                - "destination": full destination name (city, country)
                - "country": country name
                - "description": 1-2 sentence description
                - "highlights": array of 3 top highlights/attractions
                - "estimatedDailyCost": estimated daily cost in EUR as a number
                - "bestTimeToVisit": best months to visit
                - "matchScore": how well this matches the preferences (0.0 to 1.0)
                
                Return ONLY the JSON array, no other text.
                """,
                String.join(", ", request.interests()),
                request.budget(),
                request.travelStyle() != null ? request.travelStyle() : "any",
                request.duration(),
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
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(prompt.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            return "unknown";
        }
    }
}
