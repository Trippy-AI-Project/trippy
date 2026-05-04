package pse.trippy.aiservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import pse.trippy.aiservice.dto.request.GenerateItineraryRequest;
import pse.trippy.aiservice.dto.request.TripConstraints;
import pse.trippy.aiservice.dto.response.ActivityResponse;
import pse.trippy.aiservice.dto.response.DayPlanResponse;
import pse.trippy.aiservice.dto.response.ItineraryGenerationResponse;
import pse.trippy.aiservice.model.entity.GenerationHistory;
import pse.trippy.aiservice.model.entity.AiRequestLog;
import pse.trippy.aiservice.model.enums.RequestStatus;
import pse.trippy.aiservice.model.enums.RequestType;
import pse.trippy.aiservice.repository.AiRequestLogRepository;
import pse.trippy.aiservice.repository.GenerationHistoryRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiItineraryService {

    private static final Duration AI_TIMEOUT = Duration.ofSeconds(30);
    private static final int MAX_ATTEMPTS = 3;

    private final ChatClient.Builder chatClientBuilder;
    private final AiRequestLogRepository aiRequestLogRepository;
    private final GenerationHistoryRepository generationHistoryRepository;
    private final ObjectMapper objectMapper;

    public ItineraryGenerationResponse generateItinerary(UUID userId, GenerateItineraryRequest request) {
        long startTime = System.currentTimeMillis();
        String prompt = buildPrompt(request);
        String promptHash = hashPrompt(prompt);

        try {
            String aiResponse = requestAiWithRetry(prompt);
            long responseTimeMs = System.currentTimeMillis() - startTime;

            ItineraryGenerationResponse response = parseResponse(aiResponse, request);
            int inputTokens = estimateTokens(prompt);
            int outputTokens = estimateTokens(aiResponse);
            response = new ItineraryGenerationResponse(
                    response.generationId(),
                    response.tripId(),
                    response.days(),
                    response.overview(),
                    response.estimatedTotalCost(),
                    response.generatedAt(),
                    inputTokens + outputTokens,
                    false
            );

            String responseJson = objectMapper.writeValueAsString(response);
            saveHistory(response.generationId(), userId, request, promptHash, responseJson,
                    RequestStatus.SUCCESS, null, responseTimeMs);
            logRequest(userId, promptHash, responseTimeMs, inputTokens, outputTokens, RequestStatus.SUCCESS);

            return response;

        } catch (AiTimeoutException ex) {
            long responseTimeMs = System.currentTimeMillis() - startTime;
            UUID generationId = UUID.randomUUID();
            saveHistory(generationId, userId, request, promptHash, null,
                    RequestStatus.FAILED, ex.getMessage(), responseTimeMs);
            logRequest(userId, promptHash, responseTimeMs, estimateTokens(prompt), 0, RequestStatus.FAILED);
            throw ex;
        } catch (Exception ex) {
            long responseTimeMs = System.currentTimeMillis() - startTime;
            UUID generationId = UUID.randomUUID();
            saveHistory(generationId, userId, request, promptHash, null,
                    RequestStatus.FAILED, ex.getMessage(), responseTimeMs);
            logRequest(userId, promptHash, responseTimeMs, estimateTokens(prompt), 0, RequestStatus.FAILED);
            log.error("AI itinerary generation failed for user {}: {}", userId, ex.getMessage());
            throw new AiServiceUnavailableException("AI service is currently unavailable. Please try again later.");
        }
    }

    String buildPrompt(GenerateItineraryRequest request) {
        TripConstraints constraints = request.constraints();
        long days = ChronoUnit.DAYS.between(constraints.startDate(), constraints.endDate()) + 1;

        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert travel planner. Generate a detailed day-by-day itinerary.\n\n");
        sb.append(String.format("Destination: %s\n", constraints.destination()));
        sb.append(String.format("Start Date: %s\n", constraints.startDate()));
        sb.append(String.format("End Date: %s\n", constraints.endDate()));
        sb.append(String.format("Number of Days: %d\n", days));

        if (constraints.budgetLevel() != null) {
            sb.append(String.format("Budget Level: %s\n", constraints.budgetLevel()));
        }
        if (constraints.travelers() != null) {
            sb.append(String.format("Travelers: %d adults, %d children\n",
                    constraints.travelers().adults(), constraints.travelers().children()));
        }
        if (constraints.accommodationType() != null) {
            sb.append(String.format("Accommodation: %s\n", constraints.accommodationType()));
        }
        if (request.tone() != null) {
            sb.append(String.format("Tone/Style: %s\n", request.tone()));
        }
        if (request.userPrompt() != null && !request.userPrompt().isBlank()) {
            sb.append(String.format("Special Requests: %s\n", request.userPrompt()));
        }

        if (request.preferences() != null) {
            var prefs = request.preferences();
            if (prefs.pacePreference() != null) {
                sb.append(String.format("Pace: %s\n", prefs.pacePreference()));
            }
            if (prefs.mustSeeAttractions() != null && !prefs.mustSeeAttractions().isEmpty()) {
                sb.append(String.format("Must-See: %s\n", String.join(", ", prefs.mustSeeAttractions())));
            }
            if (prefs.avoidAttractions() != null && !prefs.avoidAttractions().isEmpty()) {
                sb.append(String.format("Avoid: %s\n", String.join(", ", prefs.avoidAttractions())));
            }
        }

        sb.append("""
                
                Return your response as a JSON object (no markdown, no code fences) with this structure:
                {
                  "overview": "Brief trip overview",
                  "estimatedTotalCost": "Total estimated cost with currency",
                  "days": [
                    {
                      "dayNumber": 1,
                      "title": "Day title",
                      "activities": [
                        {
                          "time": "09:00",
                          "title": "Activity title",
                          "description": "Brief description",
                          "location": "Location name",
                          "durationMinutes": 60,
                          "category": "SIGHTSEEING",
                          "estimatedCost": "Cost with currency",
                          "lat": 35.0394,
                          "lng": 135.7292
                        }
                      ]
                    }
                  ]
                }
                
                Categories must be one of: SIGHTSEEING, FOOD, TRANSPORT, ACCOMMODATION, ACTIVITY, FREE_TIME.
                Return ONLY the JSON object, no other text.
                """);

        return sb.toString();
    }

    ItineraryGenerationResponse parseResponse(String aiResponse, GenerateItineraryRequest request) {
        try {
            String cleaned = aiResponse.strip();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
            }

            JsonNode root = objectMapper.readTree(cleaned);

            String overview = root.has("overview") ? root.get("overview").asText() : "";
            String estimatedTotalCost = root.has("estimatedTotalCost") ? root.get("estimatedTotalCost").asText() : "";

            List<DayPlanResponse> days = new ArrayList<>();
            JsonNode daysNode = root.get("days");
            if (daysNode != null && daysNode.isArray()) {
                for (JsonNode dayNode : daysNode) {
                    int dayNumber = dayNode.get("dayNumber").asInt();
                    String title = dayNode.has("title") ? dayNode.get("title").asText() : "";
                    LocalDate date = request.constraints().startDate().plusDays(dayNumber - 1L);

                    List<ActivityResponse> activities = new ArrayList<>();
                    JsonNode activitiesNode = dayNode.get("activities");
                    if (activitiesNode != null && activitiesNode.isArray()) {
                        for (JsonNode actNode : activitiesNode) {
                            activities.add(new ActivityResponse(
                                    getTextOrNull(actNode, "time"),
                                    getTextOrNull(actNode, "title"),
                                    getTextOrNull(actNode, "description"),
                                    getTextOrNull(actNode, "location"),
                                    actNode.has("durationMinutes") ? actNode.get("durationMinutes").asInt() : 0,
                                    getTextOrNull(actNode, "category"),
                                    getTextOrNull(actNode, "estimatedCost"),
                                    actNode.has("lat") ? actNode.get("lat").asDouble() : null,
                                    actNode.has("lng") ? actNode.get("lng").asDouble() : null
                            ));
                        }
                    }

                    days.add(new DayPlanResponse(dayNumber, date, title, activities));
                }
            }

            return new ItineraryGenerationResponse(
                    UUID.randomUUID(),
                    request.tripId(),
                    days,
                    overview,
                    estimatedTotalCost,
                    Instant.now(),
                    0,
                    false
            );

        } catch (Exception ex) {
            log.error("Failed to parse AI itinerary response: {}", ex.getMessage());
            throw new AiServiceUnavailableException("Failed to process AI response. Please try again.");
        }
    }

    private String getTextOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private String requestAiWithRetry(String prompt) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return requestAiWithTimeout(prompt);
            } catch (AiTimeoutException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                lastFailure = ex;
                if (attempt == MAX_ATTEMPTS) {
                    break;
                }
                long backoffMs = 250L * attempt;
                log.warn("AI itinerary request attempt {} failed; retrying in {} ms: {}",
                        attempt, backoffMs, ex.getMessage());
                sleep(backoffMs);
            }
        }
        throw lastFailure != null ? lastFailure : new AiServiceUnavailableException("AI request failed");
    }

    private String requestAiWithTimeout(String prompt) {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            ChatClient chatClient = chatClientBuilder.build();
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        });

        try {
            return future.get(AI_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw new AiTimeoutException("AI service timed out after 30 seconds");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AiServiceUnavailableException("AI request was interrupted");
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new AiServiceUnavailableException("AI request failed");
        }
    }

    private void sleep(long backoffMs) {
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AiServiceUnavailableException("AI retry was interrupted");
        }
    }

    private void logRequest(UUID userId, String promptHash, long responseTimeMs,
                            int inputTokens, int outputTokens, RequestStatus status) {
        try {
            AiRequestLog logEntry = AiRequestLog.builder()
                    .userId(userId)
                    .requestType(RequestType.ITINERARY_GENERATION)
                    .promptHash(promptHash)
                    .responseTimeMs(responseTimeMs)
                    .inputTokens(inputTokens)
                    .outputTokens(outputTokens)
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
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(text.length() / 4.0));
    }

    private void saveHistory(UUID generationId, UUID userId, GenerateItineraryRequest request,
                             String promptHash, String responseJson, RequestStatus status,
                             String errorMessage, long responseTimeMs) {
        try {
            generationHistoryRepository.save(GenerationHistory.builder()
                    .generationId(generationId)
                    .tripId(request.tripId())
                    .userId(userId)
                    .promptHash(promptHash)
                    .responseJson(responseJson)
                    .status(status)
                    .errorMessage(truncate(errorMessage))
                    .responseTimeMs(responseTimeMs)
                    .build());
        } catch (Exception ex) {
            log.error("Failed to save generation history: {}", ex.getMessage());
        }
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 1000) {
            return value;
        }
        return value.substring(0, 1000);
    }
}
