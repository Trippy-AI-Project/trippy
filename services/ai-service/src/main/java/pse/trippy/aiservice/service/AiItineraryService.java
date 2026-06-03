package pse.trippy.aiservice.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pse.trippy.aiservice.dto.request.GenerateItineraryRequest;
import pse.trippy.aiservice.dto.request.TripConstraints;
import pse.trippy.aiservice.dto.response.ActivityResponse;
import pse.trippy.aiservice.dto.response.DayPlanResponse;
import pse.trippy.aiservice.dto.response.ItineraryGenerationResponse;
import pse.trippy.aiservice.logging.LogSanitizer;
import pse.trippy.aiservice.model.entity.AiRequestLog;
import pse.trippy.aiservice.model.enums.RequestStatus;
import pse.trippy.aiservice.model.enums.RequestType;
import pse.trippy.aiservice.repository.AiRequestLogRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiItineraryService {

    private final ChatClient.Builder chatClientBuilder;
    private final AiRequestLogRepository aiRequestLogRepository;
    private final ObjectMapper objectMapper;

    public ItineraryGenerationResponse generateItinerary(UUID userId, GenerateItineraryRequest request) {
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

            ItineraryGenerationResponse response = parseResponse(aiResponse, request);

            logRequest(userId, promptHash, responseTimeMs, RequestStatus.SUCCESS);

            return response;

        } catch (Exception ex) {
            long responseTimeMs = System.currentTimeMillis() - startTime;
            logRequest(userId, promptHash, responseTimeMs, RequestStatus.FAILED);
            log.error("AI itinerary generation failed userId={} promptHash={} durationMs={} error={}",
                    userId, LogSanitizer.shortHash(promptHash), responseTimeMs, LogSanitizer.safeError(ex));
            throw new AiServiceUnavailableException("AI service is currently unavailable. Please try again later.");
        }
    }

    String buildPrompt(GenerateItineraryRequest request) {
        TripConstraints constraints = request.constraints();
        long days = ChronoUnit.DAYS.between(constraints.startDate(), constraints.endDate()) + 1;

        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert travel planner. Generate a detailed day-by-day itinerary.\n\n");
        appendLine(sb, "Destination", constraints.destination());
        appendLine(sb, "Start Date", constraints.startDate());
        appendLine(sb, "End Date", constraints.endDate());
        appendLine(sb, "Number of Days", days);

        if (constraints.budgetLevel() != null) {
            appendLine(sb, "Budget Level", constraints.budgetLevel());
        }
        if (constraints.travelers() != null) {
            sb.append("Travelers: ")
                    .append(constraints.travelers().adults())
                    .append(" adults, ")
                    .append(constraints.travelers().children())
                    .append(" children\n");
        }
        if (constraints.accommodationType() != null) {
            appendLine(sb, "Accommodation", constraints.accommodationType());
        }
        if (request.tone() != null) {
            appendLine(sb, "Tone/Style", request.tone());
        }
        if (request.userPrompt() != null && !request.userPrompt().isBlank()) {
            appendLine(sb, "Special Requests", request.userPrompt());
        }

        if (request.preferences() != null) {
            var prefs = request.preferences();
            if (prefs.pacePreference() != null) {
                appendLine(sb, "Pace", prefs.pacePreference());
            }
            if (prefs.mustSeeAttractions() != null && !prefs.mustSeeAttractions().isEmpty()) {
                appendLine(sb, "Must-See", String.join(", ", prefs.mustSeeAttractions()));
            }
            if (prefs.avoidAttractions() != null && !prefs.avoidAttractions().isEmpty()) {
                appendLine(sb, "Avoid", String.join(", ", prefs.avoidAttractions()));
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

    private static void appendLine(StringBuilder sb, String label, Object value) {
        sb.append(label).append(": ").append(value).append('\n');
    }

    ItineraryGenerationResponse parseResponse(String aiResponse, GenerateItineraryRequest request) {
        if (aiResponse == null || aiResponse.isBlank()) {
            log.warn("AI itinerary response was empty");
            throw new AiServiceUnavailableException("Failed to process AI response. Please try again.");
        }

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
                LocalDate startDate = request.constraints().startDate();
                long tripDurationDays = ChronoUnit.DAYS.between(startDate, request.constraints().endDate()) + 1;
                if (tripDurationDays < 1) {
                    throw new IllegalArgumentException("Trip end date must not be before start date");
                }
                for (JsonNode dayNode : daysNode) {
                    int fallbackDayNumber = days.size() + 1;
                    int rawDayNumber = getPositiveIntOrDefault(dayNode, "dayNumber", fallbackDayNumber);
                    int dayNumber = (int) Math.min(rawDayNumber, tripDurationDays);
                    String title = dayNode.path("title").asText("");
                    LocalDate date = startDate.plusDays(dayNumber - 1L);

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

        } catch (JsonProcessingException | IllegalArgumentException | DateTimeException | ArithmeticException ex) {
            log.warn("Failed to parse AI itinerary response error={}", LogSanitizer.safeError(ex));
            throw new AiServiceUnavailableException("Failed to process AI response. Please try again.");
        }
    }

    private int getPositiveIntOrDefault(JsonNode node, String field, int defaultValue) {
        int value = node.path(field).asInt(defaultValue);
        return value > 0 ? value : defaultValue;
    }

    private String getTextOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private void logRequest(UUID userId, String promptHash, long responseTimeMs, RequestStatus status) {
        try {
            AiRequestLog logEntry = AiRequestLog.builder()
                    .userId(userId)
                    .requestType(RequestType.ITINERARY_GENERATION)
                    .promptHash(promptHash)
                    .responseTimeMs(responseTimeMs)
                    .status(status)
                    .build();
            aiRequestLogRepository.save(logEntry);
        } catch (RuntimeException ex) {
            log.warn("Failed to persist AI request metadata error={}", LogSanitizer.safeError(ex));
        }
    }

    private String hashPrompt(String prompt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(prompt.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            log.warn("Prompt hash generation failed error={}", LogSanitizer.safeError(ex));
            return "unknown";
        }
    }
}
