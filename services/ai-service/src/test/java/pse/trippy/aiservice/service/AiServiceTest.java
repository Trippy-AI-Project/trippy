package pse.trippy.aiservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import pse.trippy.aiservice.dto.request.DestinationSuggestionRequest;
import pse.trippy.aiservice.dto.request.GenerateItineraryRequest;
import pse.trippy.aiservice.dto.request.GroupPreferenceRequest;
import pse.trippy.aiservice.dto.request.TravelAdviceRequest;
import pse.trippy.aiservice.dto.request.TripConstraints;
import pse.trippy.aiservice.dto.response.ConsolidatedPreferencesResponse;
import pse.trippy.aiservice.dto.response.DestinationSuggestionResponse;
import pse.trippy.aiservice.dto.response.ItineraryResponse;
import pse.trippy.aiservice.dto.response.TravelAdviceResponse;
import pse.trippy.aiservice.model.entity.GenerationHistory;
import pse.trippy.aiservice.repository.GenerationHistoryRepository;
import pse.trippy.aiservice.service.fallback.FallbackDestinationCatalogue;
import pse.trippy.aiservice.service.fallback.FallbackItineraryGenerator;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiService")
class AiServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private GenerationHistoryRepository generationHistoryRepository;

    private AiService aiService;
    private ExecutorService aiBlockingExecutor;
    private HttpServer testHttpServer;
    private ObjectMapper objectMapper;
    private FallbackDestinationCatalogue fallbackDestinationCatalogue;
    private FallbackItineraryGenerator fallbackItineraryGenerator;

    // Fluent chain mocks
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callSpec;

    @BeforeEach
    void setUp() {
        aiBlockingExecutor = Executors.newSingleThreadExecutor();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        fallbackDestinationCatalogue = new FallbackDestinationCatalogue(objectMapper);
        fallbackItineraryGenerator = new FallbackItineraryGenerator(fallbackDestinationCatalogue);
        aiService = new AiService(
                chatClient,
                objectMapper,
                generationHistoryRepository,
                aiBlockingExecutor,
                fallbackDestinationCatalogue,
                fallbackItineraryGenerator);

        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callSpec = mock(ChatClient.CallResponseSpec.class);

        lenient().when(chatClient.prompt()).thenReturn(requestSpec);
        lenient().when(requestSpec.user(anyString())).thenReturn(requestSpec);
        lenient().when(requestSpec.call()).thenReturn(callSpec);
    }

    @AfterEach
    void tearDown() {
        aiBlockingExecutor.shutdownNow();
        if (testHttpServer != null) {
            testHttpServer.stop(0);
            testHttpServer = null;
        }
    }

    // =========================================================================
    // Destination Suggestions
    // =========================================================================

    @Nested
    @DisplayName("suggestDestinations()")
    class SuggestDestinations {

        @Test
        @DisplayName("returns parsed suggestions for a valid Groq JSON response")
        void returnsValidSuggestions() {
            String groqJson = """
                    [
                      {
                        "destination": "Lisbon, Portugal",
                        "country": "Portugal",
                        "description": "Affordable and warm in August with beautiful coastline.",
                        "estimatedDailyCost": 100,
                        "bestTimeToVisit": "May to September",
                        "highlights": ["Historic trams", "Belem Tower", "Alfama district"],
                        "matchScore": 0.92
                      }
                    ]
                    """;
            when(callSpec.content()).thenReturn(groqJson);

            DestinationSuggestionRequest request = new DestinationSuggestionRequest(
                    null, null, List.of("beach", "culture"), "LOW", null, 7, "Europe", "August",
                    null, null, null, null);

            DestinationSuggestionResponse response = aiService.suggestDestinations(request);

            assertThat(response.suggestions()).hasSize(1);
            assertThat(response.suggestions().get(0).destination()).isEqualTo("Lisbon, Portugal");
            assertThat(response.suggestions().get(0).matchScore()).isEqualTo(0.92);
            assertThat(response.generatedAt()).isNotNull();
        }

        @Test
        @DisplayName("returns catalogue fallback when Groq response is malformed JSON")
        void returnsFallbackOnBadJson() {
            when(callSpec.content()).thenReturn("Sorry, I cannot help with that.");

            DestinationSuggestionRequest request = new DestinationSuggestionRequest(
                    null, null, List.of("history"), "MODERATE", null, 5, null, null,
                    null, null, null, null);

            DestinationSuggestionResponse response = aiService.suggestDestinations(request);

            assertThat(response.suggestions()).isNotEmpty();
            assertThat(response.generatedAt()).isNotNull();
        }

        @Test
        @DisplayName("strips markdown code fences from Groq response before parsing")
        void stripsMarkdownCodeFences() {
            String wrapped = """
                    ```json
                    {
                      "suggestions": [
                        {
                          "destination": "Lisbon, Portugal",
                          "country": "Portugal",
                          "description": "Culture, food and river views.",
                          "estimatedDailyCost": 120,
                          "bestTimeToVisit": "April to June",
                          "highlights": ["Belém Tower", "Alfama", "Jerónimos Monastery"],
                          "matchScore": 0.9
                        }
                      ]
                    }
                    ```
                    """;
            when(callSpec.content()).thenReturn(wrapped);

            DestinationSuggestionRequest request = new DestinationSuggestionRequest(
                    null, null, List.of("adventure"), "HIGH", null, 10, null, null,
                    null, null, null, null);

            DestinationSuggestionResponse response = aiService.suggestDestinations(request);

            assertThat(response.suggestions()).hasSize(1);
            assertThat(response.suggestions().get(0).destination()).isEqualTo("Lisbon, Portugal");
        }
    }

    // =========================================================================
    // Travel Advice
    // =========================================================================

    @Nested
    @DisplayName("getTravelAdvice()")
    class GetTravelAdvice {

        @Test
        @DisplayName("returns structured advice from valid Groq JSON")
        void returnsStructuredAdvice() {
            String groqJson = """
                    {
                      "answer": "Take the Shinkansen from Tokyo to Kyoto — it takes about 2h 15m.",
                      "relatedQuestions": [
                        "What is the best JR Pass option?",
                        "Are there luggage storage options at Kyoto station?"
                      ]
                    }
                    """;
            when(callSpec.content()).thenReturn(groqJson);

            TravelAdviceRequest request = new TravelAdviceRequest();
            request.setQuestion("Best way to get from Tokyo to Kyoto?");
            request.setDestination("Japan");

            TravelAdviceResponse response = aiService.getTravelAdvice(request);

            assertThat(response.getAnswer()).contains("Shinkansen");
            assertThat(response.getRelatedQuestions()).hasSize(2);
            assertThat(response.getGeneratedAt()).isNotNull();
        }

        @Test
        @DisplayName("falls back to plain text answer when JSON cannot be parsed")
        void fallsBackToPlainText() {
            String plainText = "You should take the Shinkansen bullet train.";
            when(callSpec.content()).thenReturn(plainText);

            TravelAdviceRequest request = new TravelAdviceRequest();
            request.setQuestion("How to travel in Japan?");
            request.setDestination("Japan");

            TravelAdviceResponse response = aiService.getTravelAdvice(request);

            assertThat(response.getAnswer()).isEqualTo(plainText);
            assertThat(response.getRelatedQuestions()).isEmpty();
        }
    }

    // =========================================================================
    // Itinerary Generation
    // =========================================================================

    @Nested
    @DisplayName("generateItinerary()")
    class GenerateItinerary {

        @Test
        @DisplayName("returns a parsed itinerary for valid Groq JSON")
        void returnsValidItinerary() {
            String groqJson = """
                    {
                      "tripTitle": "5 Days of Culture in Kyoto",
                      "summary": "Temples, food, and gardens",
                      "totalEstimatedCost": "¥150,000",
                      "dailyPlan": [
                        {
                          "dayNumber": 1,
                          "date": "2026-09-01",
                          "title": "Arrival & Fushimi Inari",
                          "activities": [
                            {
                              "time": "10:00",
                              "duration": 120,
                              "title": "Fushimi Inari Shrine",
                              "description": "Walk the iconic torii gates",
                              "location": "Fushimi, Kyoto",
                              "category": "SIGHTSEEING",
                              "estimatedCost": "Free",
                              "tips": "Go early to avoid crowds",
                              "bookingRequired": false
                            }
                          ]
                        }
                      ],
                      "packingTips": ["Comfortable walking shoes"],
                      "travelTips": ["Get an IC card at the airport"]
                    }
                    """;
            when(callSpec.content()).thenReturn(groqJson);

            GenerateItineraryRequest request = new GenerateItineraryRequest(
                    null,
                    new TripConstraints("Kyoto, Japan", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1),
                            null, null, null),
                    null, null, null);

            ItineraryResponse response = aiService.generateItinerary(request);

            assertThat(response.getTripTitle()).isEqualTo("5 Days of Culture in Kyoto");
            assertThat(response.getDailyPlan()).hasSize(1);
            assertThat(response.getDailyPlan().get(0).getActivities()).hasSize(1);
            assertThat(response.getDailyPlan().get(0).getActivities().get(0).getTitle())
                    .isEqualTo("Fushimi Inari Shrine");
            assertThat(response.getPackingTips()).contains("Comfortable walking shoes");
            assertThat(response.getGeneratedAt()).isNotNull();
            assertThat(response.getGenerationId()).isNotNull();

            GenerationHistory history = captureGenerationHistory();
            assertThat(history.getStatus()).isEqualTo("SUCCESS");
            assertThat(history.isFallbackUsed()).isFalse();
        }

        @Test
        @DisplayName("accepts enriched activity JSON with durationMinutes and coordinates")
        void acceptsEnrichedActivityJson() {
            String groqJson = """
                    {
                      "tripTitle": "Kyoto Details",
                      "summary": "Precise activity data",
                      "dailyPlan": [
                        {
                          "dayNumber": 1,
                          "date": "2026-09-01",
                          "title": "Temples",
                          "activities": [
                            {
                              "time": "09:00",
                              "durationMinutes": 90,
                              "title": "Kinkaku-ji",
                              "description": "Golden Pavilion visit",
                              "location": "Kinkaku-ji, Kyoto",
                              "category": "SIGHTSEEING",
                              "estimatedCost": "¥500",
                              "tips": "Arrive early",
                              "bookingRequired": false,
                              "lat": 35.0394,
                              "lng": 135.7292
                            }
                          ]
                        }
                      ]
                    }
                    """;
            when(callSpec.content()).thenReturn(groqJson);

            GenerateItineraryRequest request = new GenerateItineraryRequest(
                    null,
                    new TripConstraints("Kyoto", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1),
                            null, null, null),
                    null, null, null);

            ItineraryResponse response = aiService.generateItinerary(request);
            ItineraryResponse.Activity activity = response.getDailyPlan().get(0).getActivities().get(0);

            assertThat(activity.getDurationMinutes()).isEqualTo(90);
            assertThat(activity.getLat()).isEqualTo(35.0394);
            assertThat(activity.getLng()).isEqualTo(135.7292);
            assertThat(activity.getTips()).isEqualTo("Arrive early");
        }

        @Test
        @DisplayName("accepts legacy duration JSON as durationMinutes")
        void acceptsLegacyDurationJson() {
            String groqJson = """
                    {
                      "tripTitle": "Legacy Duration",
                      "dailyPlan": [
                        {
                          "dayNumber": 1,
                          "activities": [
                            {
                              "time": "10:00",
                              "duration": 120,
                              "title": "Legacy Activity",
                              "category": "SIGHTSEEING"
                            }
                          ]
                        }
                      ]
                    }
                    """;
            when(callSpec.content()).thenReturn(groqJson);

            GenerateItineraryRequest request = new GenerateItineraryRequest(
                    null,
                    new TripConstraints("Kyoto", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1),
                            null, null, null),
                    null, null, null);

            ItineraryResponse response = aiService.generateItinerary(request);

            assertThat(response.getDailyPlan().get(0).getActivities().get(0).getDurationMinutes())
                    .isEqualTo(120);
        }

        @Test
        @DisplayName("returns unavailable weather when OpenWeather key is missing")
        void returnsUnavailableWeatherWithoutApiKey() {
            when(callSpec.content()).thenReturn(minimalTwoStopItinerary(false));

            GenerateItineraryRequest request = new GenerateItineraryRequest(
                    null,
                    new TripConstraints("Kyoto", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1),
                            null, null, null),
                    null, null, null);

            ItineraryResponse response = aiService.generateItinerary(request);

            assertThat(response.getDailyPlan().get(0).getWeather().getCondition())
                    .isEqualTo("Forecast unavailable");
        }

        @Test
        @DisplayName("returns unavailable weather when OpenWeather fails")
        void returnsUnavailableWeatherWhenWeatherApiFails() throws Exception {
            String baseUrl = startHttpServer(exchange ->
                    writeResponse(exchange, 500, "{\"message\":\"upstream failed\"}"));
            setField("openWeatherApiKey", "test-key");
            setField("openWeatherGeocodingUrl", baseUrl + "/geo");
            when(callSpec.content()).thenReturn(minimalTwoStopItinerary(false));

            GenerateItineraryRequest request = new GenerateItineraryRequest(
                    null,
                    new TripConstraints("Kyoto", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1),
                            null, null, null),
                    null, null, null);

            ItineraryResponse response = aiService.generateItinerary(request);

            assertThat(response.getDailyPlan().get(0).getWeather().getCondition())
                    .isEqualTo("Forecast unavailable");
        }

        @Test
        @DisplayName("uses OSRM route estimates when activity coordinates are present")
        void usesOsrmRouteEstimates() throws Exception {
            String baseUrl = startHttpServer(exchange ->
                    writeResponse(exchange, 200, "{\"routes\":[{\"duration\":900,\"distance\":1500}]}"));
            setField("osrmBaseUrl", baseUrl);
            when(callSpec.content()).thenReturn(minimalTwoStopItinerary(true));

            GenerateItineraryRequest request = new GenerateItineraryRequest(
                    null,
                    new TripConstraints("Kyoto", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1),
                            null, null, null),
                    null, null, null);

            ItineraryResponse response = aiService.generateItinerary(request);
            ItineraryResponse.TransportRecommendation transport =
                    response.getDailyPlan().get(0).getTransportRecommendations().get(0);

            assertThat(transport.getEstimatedDuration()).isEqualTo("15 mins");
            assertThat(transport.getNotes()).contains("1.5 km");
        }

        @Test
        @DisplayName("falls back when OSRM route lookup fails")
        void fallsBackWhenOsrmFails() throws Exception {
            String baseUrl = startHttpServer(exchange ->
                    writeResponse(exchange, 503, "{\"message\":\"unavailable\"}"));
            setField("osrmBaseUrl", baseUrl);
            when(callSpec.content()).thenReturn(minimalTwoStopItinerary(true));

            GenerateItineraryRequest request = new GenerateItineraryRequest(
                    null,
                    new TripConstraints("Kyoto", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1),
                            null, null, null),
                    null, null, null);

            ItineraryResponse response = aiService.generateItinerary(request);

            assertThat(response.getDailyPlan().get(0).getTransportRecommendations().get(0)
                    .getEstimatedDuration()).isEqualTo("Transit details unavailable");
        }

        @Test
        @DisplayName("does not call OSRM when activity coordinates are missing")
        void skipsOsrmWhenCoordinatesAreMissing() throws Exception {
            AtomicInteger requestCount = new AtomicInteger();
            String baseUrl = startHttpServer(exchange -> {
                requestCount.incrementAndGet();
                writeResponse(exchange, 200, "{\"routes\":[{\"duration\":900,\"distance\":1500}]}");
            });
            setField("osrmBaseUrl", baseUrl);
            when(callSpec.content()).thenReturn(minimalTwoStopItinerary(false));

            GenerateItineraryRequest request = new GenerateItineraryRequest(
                    null,
                    new TripConstraints("Kyoto", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1),
                            null, null, null),
                    null, null, null);

            ItineraryResponse response = aiService.generateItinerary(request);

            assertThat(requestCount).hasValue(0);
            assertThat(response.getDailyPlan().get(0).getTransportRecommendations().get(0)
                    .getEstimatedDuration()).isEqualTo("Transit details unavailable");
        }

        @Test
        @DisplayName("returns fallback itinerary when itinerary JSON cannot be parsed")
        void returnsFallbackOnBadJson() {
            when(callSpec.content()).thenReturn("I cannot generate that.");

            GenerateItineraryRequest request = new GenerateItineraryRequest(
                    null,
                    new TripConstraints("Kyoto", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5),
                            null, null, null),
                    null, null, null);

            ItineraryResponse response = aiService.generateItinerary(request);

            assertThat(response.getFallbackUsed()).isTrue();
            assertThat(response.getGenerationId()).isNotNull();
            assertThat(response.getFallbackReason()).isEqualTo("AI_MALFORMED_RESPONSE");
            assertThat(response.getTokensUsed()).isZero();
            assertThat(response.getDailyPlan()).hasSize(5);
            assertThat(response.getDailyPlan().get(0).getWeather().getCondition())
                    .isEqualTo("Forecast unavailable");

            GenerationHistory history = captureGenerationHistory();
            assertThat(history.getStatus()).isEqualTo("FALLBACK");
            assertThat(history.isFallbackUsed()).isTrue();
        }

        @Test
        @DisplayName("uses fallback when AI omits required day numbers")
        void fallsBackWhenDayNumbersAreMissing() {
            String groqJson = """
                    {
                      "tripTitle": "Kyoto",
                      "summary": "Two quiet days",
                      "dailyPlan": [
                        {
                          "title": "Arrival",
                          "activities": []
                        },
                        {
                          "title": "Temples",
                          "activities": []
                        }
                      ]
                    }
                    """;
            when(callSpec.content()).thenReturn(groqJson);

            GenerateItineraryRequest request = new GenerateItineraryRequest(
                    null,
                    new TripConstraints("Kyoto", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2),
                            null, null, null),
                    null, null, null);

            ItineraryResponse response = aiService.generateItinerary(request);

            assertThat(response.getFallbackUsed()).isTrue();
            assertThat(response.getFallbackReason()).isEqualTo("AI_SCHEMA_INVALID");
            assertThat(response.getDailyPlan()).hasSize(2);
            assertThat(response.getDailyPlan().get(0).getDayNumber()).isEqualTo(1);
            assertThat(response.getDailyPlan().get(0).getDate()).isEqualTo("2026-09-01");
            assertThat(response.getDailyPlan().get(1).getDayNumber()).isEqualTo(2);
            assertThat(response.getDailyPlan().get(1).getDate()).isEqualTo("2026-09-02");
        }

        @Test
        @DisplayName("records fallback history status when AI returns an empty itinerary")
        void recordsFallbackStatusWhenAiReturnsEmptyItinerary() {
            String groqJson = """
                    {
                      "tripTitle": "Empty Kyoto",
                      "summary": "No days",
                      "dailyPlan": []
                    }
                    """;
            when(callSpec.content()).thenReturn(groqJson);

            GenerateItineraryRequest request = new GenerateItineraryRequest(
                    null,
                    new TripConstraints("Kyoto", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3),
                            null, null, null),
                    null, null, null);

            ItineraryResponse response = aiService.generateItinerary(request);

            assertThat(response.getFallbackUsed()).isTrue();
            assertThat(response.getTokensUsed()).isZero();
            assertThat(response.getFallbackReason()).isEqualTo("AI_UNUSABLE_RESPONSE");
            assertThat(response.getDailyPlan()).hasSize(3);

            GenerationHistory history = captureGenerationHistory();
            assertThat(history.getStatus()).isEqualTo("FALLBACK");
            assertThat(history.isFallbackUsed()).isTrue();
        }

        @Test
        @DisplayName("cancels blocking AI task when timeout expires")
        void cancelsBlockingAiTaskWhenTimeoutExpires() throws Exception {
            RecordingExecutorService recordingExecutor = new RecordingExecutorService();
            AiService timeoutService = new AiService(
                    chatClient,
                    objectMapper,
                    generationHistoryRepository,
                    recordingExecutor,
                    fallbackDestinationCatalogue,
                    fallbackItineraryGenerator);
            Method method = AiService.class.getDeclaredMethod("requestAiWithTimeout", String.class, Duration.class);
            method.setAccessible(true);

            assertThatThrownBy(() -> method.invoke(timeoutService, "prompt", Duration.ofMillis(1)))
                    .isInstanceOf(InvocationTargetException.class)
                    .satisfies(exception -> assertThat(((InvocationTargetException) exception).getCause())
                            .isInstanceOf(AiServiceTimeoutException.class)
                            .hasMessage("AI itinerary generation timed out after 1 millisecond"));
            assertThat(recordingExecutor.wasSubmittedTaskCancelled()).isTrue();
        }
    }

    @Nested
    @DisplayName("consolidatePreferences()")
    class ConsolidatePreferences {

        @Test
        @DisplayName("returns deterministic group preference consolidation")
        void returnsConsolidatedPreferences() {
            GroupPreferenceRequest request = new GroupPreferenceRequest(
                    java.util.UUID.randomUUID(),
                    List.of(
                            new GroupPreferenceRequest.UserPreference(
                                    java.util.UUID.randomUUID(),
                                    List.of("food", "museums"),
                                    "MODERATE",
                                    "SLOW",
                                    List.of("Louvre"),
                                    List.of("clubs")),
                            new GroupPreferenceRequest.UserPreference(
                                    java.util.UUID.randomUUID(),
                                    List.of("food", "parks"),
                                    "MODERATE",
                                    "MODERATE",
                                    List.of("Louvre"),
                                    List.of("Louvre"))));

            ConsolidatedPreferencesResponse response = aiService.consolidatePreferences(request);

            assertThat(response.recommendedBudget()).isEqualTo("MODERATE");
            assertThat(response.sharedInterests()).contains("food");
            assertThat(response.mustSeeConsensus()).contains("Louvre");
            assertThat(response.conflicts()).hasSize(1);
            assertThat(response.suggestedPrompt()).contains("moderate budget");
        }
    }

    private GenerationHistory captureGenerationHistory() {
        ArgumentCaptor<GenerationHistory> captor = ArgumentCaptor.forClass(GenerationHistory.class);
        verify(generationHistoryRepository).save(captor.capture());
        return captor.getValue();
    }

    private String minimalTwoStopItinerary(boolean includeCoordinates) {
        String firstCoordinates = includeCoordinates ? """
                              "lat": 35.0394,
                              "lng": 135.7292,
                """ : "";
        String secondCoordinates = includeCoordinates ? """
                              "lat": 35.0116,
                              "lng": 135.7681,
                """ : "";
        return """
                {
                  "tripTitle": "Kyoto",
                  "dailyPlan": [
                    {
                      "dayNumber": 1,
                      "date": "2026-09-01",
                      "title": "Kyoto Day",
                      "activities": [
                        {
                          "time": "09:00",
                          %s
                          "durationMinutes": 60,
                          "title": "Temple One",
                          "location": "Temple One",
                          "category": "SIGHTSEEING"
                        },
                        {
                          "time": "11:00",
                          %s
                          "durationMinutes": 60,
                          "title": "Temple Two",
                          "location": "Temple Two",
                          "category": "SIGHTSEEING"
                        }
                      ]
                    }
                  ]
                }
                """.formatted(firstCoordinates, secondCoordinates);
    }

    private void setField(String fieldName, Object value) throws Exception {
        Field field = AiService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(aiService, value);
    }

    private String startHttpServer(TestHttpHandler handler) throws IOException {
        testHttpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        testHttpServer.createContext("/", handler::handle);
        testHttpServer.start();
        return "http://localhost:" + testHttpServer.getAddress().getPort();
    }

    private void writeResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    @FunctionalInterface
    private interface TestHttpHandler {
        void handle(HttpExchange exchange) throws IOException;
    }

    private static final class RecordingExecutorService extends AbstractExecutorService {

        private FutureTask<?> submittedTask;
        private boolean shutdown;

        @Override
        protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
            FutureTask<T> task = new FutureTask<>(callable);
            submittedTask = task;
            return task;
        }

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }

        @Override
        public void execute(Runnable command) {
            // Keep submitted tasks pending so requestAiWithTimeout exercises cancellation.
        }

        private boolean wasSubmittedTaskCancelled() {
            return submittedTask != null && submittedTask.isCancelled();
        }
    }
}
