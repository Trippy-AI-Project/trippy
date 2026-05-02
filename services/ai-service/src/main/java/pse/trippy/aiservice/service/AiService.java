package pse.trippy.aiservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import pse.trippy.aiservice.dto.request.AiChatRequest;
import pse.trippy.aiservice.dto.request.DestinationSuggestionRequest;
import pse.trippy.aiservice.dto.request.GenerateItineraryRequest;
import pse.trippy.aiservice.dto.request.TravelAdviceRequest;
import pse.trippy.aiservice.dto.request.TripConstraints;
import pse.trippy.aiservice.dto.response.AiChatResponse;
import pse.trippy.aiservice.dto.response.DestinationSuggestion;
import pse.trippy.aiservice.dto.response.DestinationSuggestionResponse;
import pse.trippy.aiservice.dto.response.ItineraryResponse;
import pse.trippy.aiservice.dto.response.TravelAdviceResponse;

import java.time.Instant;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://api.groq.com/openai}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model:llama-3.3-70b-versatile}")
    private String model;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public DestinationSuggestionResponse suggestDestinations(DestinationSuggestionRequest request) {
        String prompt = buildDestinationPrompt(request);
        log.info("Requesting destination suggestions");

        String rawJson;
        try {
            rawJson = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception ex) {
            log.warn("Spring AI call failed, falling back to direct Groq API: {}", ex.getMessage());
            rawJson = callGroqDirect(prompt);
        }

        try {
            String cleanJson = extractJson(rawJson);
            log.debug("Extracted JSON (first 500 chars): {}", cleanJson.length() > 500 ? cleanJson.substring(0, 500) : cleanJson);

            List<DestinationSuggestion> suggestions;
            if (cleanJson.strip().startsWith("[")) {
                suggestions = objectMapper.readValue(cleanJson, new TypeReference<>() {});
            } else {
                JsonNode root = objectMapper.readTree(cleanJson);
                suggestions = objectMapper.convertValue(root.path("suggestions"), new TypeReference<>() {});
            }

            if (suggestions == null || suggestions.isEmpty()) {
                log.warn("AI returned valid JSON but with empty suggestions list");
                suggestions = List.of();
            }
            return new DestinationSuggestionResponse(suggestions, Instant.now(), false);
        } catch (Exception e) {
            log.error("Failed to parse destination suggestions. Raw response (first 1000 chars): {}",
                    rawJson != null && rawJson.length() > 1000 ? rawJson.substring(0, 1000) : rawJson, e);
            return new DestinationSuggestionResponse(List.of(), Instant.now(), false);
        }
    }

    public ItineraryResponse generateItinerary(GenerateItineraryRequest request) {
        String prompt = buildItineraryPrompt(request);
        log.debug("Generating itinerary for destination: {}", request.constraints().destination());

        String rawJson;
        try {
            rawJson = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception ex) {
            log.warn("Spring AI call failed, falling back to direct Groq API: {}", ex.getMessage());
            rawJson = callGroqDirect(prompt);
        }

        try {
            ItineraryResponse response = objectMapper.readValue(
                    extractJson(rawJson), ItineraryResponse.class);
            response.setGeneratedAt(Instant.now());
            return response;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse itinerary response", e);
            throw new RuntimeException("Failed to generate itinerary. Please try again.", e);
        }
    }

    public TravelAdviceResponse getTravelAdvice(TravelAdviceRequest request) {
        String prompt = buildAdvicePrompt(request);
        log.debug("Getting travel advice for destination: {}", request.getDestination());

        String rawJson;
        try {
            rawJson = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception ex) {
            log.warn("Spring AI call failed, falling back to direct Groq API: {}", ex.getMessage());
            rawJson = callGroqDirect(prompt);
        }

        try {
            TravelAdviceResponse response = objectMapper.readValue(
                    extractJson(rawJson), TravelAdviceResponse.class);
            response.setGeneratedAt(Instant.now());
            return response;
        } catch (JsonProcessingException e) {
            log.warn("Could not parse structured response, returning plain text answer");
            return TravelAdviceResponse.builder()
                    .answer(rawJson)
                    .relatedQuestions(List.of())
                    .generatedAt(Instant.now())
                    .build();
        }
    }

    public AiChatResponse chat(AiChatRequest request) {
        String lastMessage = request.messages().get(request.messages().size() - 1).content();

        if (request.currentItinerary() != null && !request.currentItinerary().isEmpty()) {
            String prompt = buildItineraryChatPrompt(request, lastMessage);
            try {
                String rawJson = requestAi(prompt);
                JsonNode root = objectMapper.readTree(extractJson(rawJson));
                List<ItineraryResponse.DayPlan> updatedItinerary = objectMapper.convertValue(
                        root.path("dailyPlan"),
                        new TypeReference<>() {
                        });

                if (updatedItinerary != null && !updatedItinerary.isEmpty()) {
                    return new AiChatResponse(
                            "Trip updated based on your request.",
                            updatedItinerary,
                            List.of(new AiChatResponse.Change(
                                    "info",
                                    null,
                                    null,
                                    null,
                                    "Itinerary updated based on your request.")),
                            true);
                }
            } catch (Exception ex) {
                log.warn("AI itinerary chat update failed, falling back to conversational answer: {}", ex.getMessage());
            }
        }

        String reply;
        try {
            reply = requestAi(buildGeneralChatPrompt(request));
        } catch (Exception ex) {
            log.warn("AI chat failed, returning local fallback: {}", ex.getMessage());
            reply = "I can help with destination ideas, itinerary changes, food, transport, packing, and budget tips. Try asking for a specific change to your trip.";
        }

        return new AiChatResponse(reply, null, List.of(), false);
    }

    // -------------------------------------------------------------------------
    // Prompt builders
    // -------------------------------------------------------------------------

    private String buildDestinationPrompt(DestinationSuggestionRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are Trippy AI, a travel expert. Suggest personalized travel destinations based on these preferences:\n\n");

        if (req.city() != null && !req.city().isBlank()) {
            sb.append("Requested destination/city: ").append(req.city()).append("\n");
            sb.append("Focus primarily on this destination. If useful, include nearby alternatives as secondary suggestions.\n");
        }
        if (req.prompt() != null && !req.prompt().isBlank()) sb.append("User request: ").append(req.prompt()).append("\n");
        if (req.interests() != null && !req.interests().isEmpty())
            sb.append("Interests: ").append(String.join(", ", req.interests())).append("\n");
        if (req.budget() != null) sb.append("Budget: ").append(req.budget()).append("\n");
        if (req.travelStyle() != null) sb.append("Travel Style: ").append(req.travelStyle()).append("\n");
        sb.append("Duration: ").append(req.duration()).append(" days\n");
        if (req.people() != null) sb.append("Travelers: ").append(req.people()).append("\n");
        if (req.diet() != null && !req.diet().isBlank()) sb.append("Dietary requirements: ").append(req.diet()).append("\n");
        if (req.preferences() != null && !req.preferences().isBlank()) sb.append("Preferences: ").append(req.preferences()).append("\n");
        if (req.customNotes() != null && !req.customNotes().isBlank()) sb.append("Custom notes: ").append(req.customNotes()).append("\n");
        if (req.region() != null) sb.append("Region: ").append(req.region()).append("\n");
        if (req.month() != null) sb.append("Travel month: ").append(req.month()).append("\n");

        sb.append("""
                
                Return ONLY a JSON object (no markdown, no code fences) with this shape:
                {
                  "suggestions": [
                    {
                      "destination": "full destination name (city, country)",
                      "country": "country name",
                      "description": "1-2 sentence reason tailored to the user",
                      "highlights": ["top highlight 1", "top highlight 2", "top highlight 3"],
                      "estimatedDailyCost": 100,
                      "bestTimeToVisit": "best months to visit",
                      "matchScore": 0.92
                    }
                  ]
                }

                Each object must contain:
                - "destination": full destination name (city, country)
                - "country": country name
                - "description": 1-2 sentence description
                - "highlights": array of 3 top highlights/attractions
                - "estimatedDailyCost": estimated daily cost in EUR as a number
                - "bestTimeToVisit": best months to visit
                - "matchScore": how well this matches the preferences (0.0 to 1.0)

                Return 3 to 5 destination suggestions.
                """);
        return sb.toString();
    }

    private String buildItineraryPrompt(GenerateItineraryRequest req) {
        TripConstraints c = req.constraints();
        StringBuilder sb = new StringBuilder();
        sb.append("You are a professional travel planner. Create a detailed day-by-day itinerary.\n\n");
        sb.append("Destination: ").append(c.destination()).append("\n");
        sb.append("From: ").append(c.startDate()).append(" to ").append(c.endDate()).append("\n");
        if (c.travelers() != null) {
            sb.append("Travelers: ").append(c.travelers().adults()).append(" adult(s), ")
              .append(c.travelers().children()).append(" child(ren)\n");
        }
        if (c.budgetLevel() != null) sb.append("Budget level: ").append(c.budgetLevel()).append("\n");
        if (req.tone() != null) sb.append("Tone: ").append(req.tone()).append("\n");

        if (req.userPrompt() != null && !req.userPrompt().isBlank())
            sb.append("Special instructions: ").append(req.userPrompt()).append("\n");

        if (req.preferences() != null) {
            GenerateItineraryRequest.ItineraryPreferences p = req.preferences();
            sb.append("Include transport: ").append(p.includeTransport()).append("\n");
            sb.append("Include meals: ").append(p.includeMeals()).append("\n");
            if (p.pacePreference() != null) sb.append("Pace: ").append(p.pacePreference()).append("\n");
            if (p.mustSeeAttractions() != null && !p.mustSeeAttractions().isEmpty())
                sb.append("Must see: ").append(String.join(", ", p.mustSeeAttractions())).append("\n");
            if (p.avoidAttractions() != null && !p.avoidAttractions().isEmpty())
                sb.append("Avoid: ").append(String.join(", ", p.avoidAttractions())).append("\n");
        }

        sb.append("""

                Respond ONLY with a valid JSON object (no markdown, no extra text):
                {
                  "tripTitle": "string",
                  "summary": "string",
                  "totalEstimatedCost": "string",
                  "dailyPlan": [
                    {
                      "dayNumber": 1,
                      "date": "YYYY-MM-DD",
                      "title": "string",
                      "activities": [
                        {
                          "time": "HH:mm",
                          "duration": 60,
                          "title": "string",
                          "description": "string",
                          "location": "string",
                          "category": "SIGHTSEEING",
                          "estimatedCost": "string",
                          "tips": "string",
                          "bookingRequired": false
                        }
                      ]
                    }
                  ],
                  "packingTips": ["string"],
                  "travelTips": ["string"]
                }
                """);
        return sb.toString();
    }

    private String buildAdvicePrompt(TravelAdviceRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a knowledgeable travel advisor. Answer the following travel question.\n\n");
        sb.append("Destination: ").append(req.getDestination()).append("\n");
        sb.append("Question: ").append(req.getQuestion()).append("\n");
        if (req.getContext() != null && !req.getContext().isBlank())
            sb.append("Context: ").append(req.getContext()).append("\n");

        sb.append("""

                Respond ONLY with a valid JSON object (no markdown, no extra text):
                {
                  "answer": "string",
                  "relatedQuestions": ["string", "string", "string"]
                }
                """);
        return sb.toString();
    }

    private String buildItineraryChatPrompt(AiChatRequest request, String lastMessage) {
        return """
                You are Trippy AI, an expert travel planner.

                Trip context:
                %s

                Destination:
                %s

                The user wants to modify their existing trip itinerary.
                User request: "%s"

                Current itinerary JSON:
                %s

                Return ONLY valid JSON, no markdown and no explanation, in exactly this shape:
                {
                  "dailyPlan": [
                    {
                      "dayNumber": 1,
                      "date": "YYYY-MM-DD",
                      "title": "Day title",
                      "activities": [
                        {
                          "time": "09:00",
                          "duration": 60,
                          "title": "Activity",
                          "description": "Description",
                          "location": "Venue, Address",
                          "category": "FOOD|SIGHTSEEING|TRANSPORT|SHOPPING|CULTURE|NIGHTLIFE|NATURE|WELLNESS",
                          "estimatedCost": "€15",
                          "tips": "Useful tip",
                          "bookingRequired": false
                        }
                      ]
                    }
                  ]
                }

                Keep the same JSON structure and day numbering unless the user explicitly asks to change them.
                Use real places and realistic costs for the destination.
                """.formatted(
                request.tripContext() == null ? "" : request.tripContext(),
                request.destination() == null ? "the destination" : request.destination(),
                lastMessage,
                writeJson(request.currentItinerary()));
    }

    private String buildGeneralChatPrompt(AiChatRequest request) {
        String conversation = request.messages().stream()
                .map(message -> message.role() + ": " + message.content())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        return """
                You are Trippy AI, a concise travel assistant. Only answer travel planning questions.
                Use EUR (€) for rough costs when relevant.

                Trip context:
                %s

                Destination:
                %s

                Conversation:
                %s
                """.formatted(
                request.tripContext() == null ? "" : request.tripContext(),
                request.destination() == null ? "the destination" : request.destination(),
                conversation);
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    private String requestAi(String prompt) {
        try {
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception ex) {
            log.warn("Spring AI call failed, falling back to direct Groq API: {}", ex.getMessage());
            return callGroqDirect(prompt);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    /**
     * Strips markdown code fences if the model wraps its JSON in ```json ... ```
     * Handles both JSON objects {@code {...}} and arrays {@code [...]}.
     */
    private String extractJson(String raw) {
        if (raw == null || raw.isBlank()) return "{}";
        String trimmed = raw.strip();
        // Strip markdown code fences: ```json ... ``` or ``` ... ```
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n') + 1;
            int end = trimmed.lastIndexOf("```");
            if (end > start) {
                trimmed = trimmed.substring(start, end).strip();
            }
        }
        // Handle JSON arrays [...] as well as objects {...}
        int objStart = trimmed.indexOf('{');
        int arrStart = trimmed.indexOf('[');
        if (objStart >= 0 && (arrStart < 0 || objStart < arrStart)) {
            int objEnd = trimmed.lastIndexOf('}');
            if (objEnd > objStart) return trimmed.substring(objStart, objEnd + 1);
        } else if (arrStart >= 0) {
            int arrEnd = trimmed.lastIndexOf(']');
            if (arrEnd > arrStart) return trimmed.substring(arrStart, arrEnd + 1);
        }
        return trimmed;
    }

    private String callGroqDirect(String prompt) {
        try {
            String endpoint = baseUrl.endsWith("/")
                    ? baseUrl + "v1/chat/completions"
                    : baseUrl + "/v1/chat/completions";

            Map<String, Object> payload = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are Trippy AI, a helpful travel planning assistant."),
                            Map.of("role", "user", "content", prompt)
                    )
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Groq API returned status " + response.statusCode() + ": " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new RuntimeException("Groq API returned empty content");
            }
            return content.asText();
        } catch (Exception ex) {
            throw new RuntimeException("Direct Groq fallback failed", ex);
        }
    }
}
