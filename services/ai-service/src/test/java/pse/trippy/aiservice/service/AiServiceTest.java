package pse.trippy.aiservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import pse.trippy.aiservice.dto.request.DestinationSuggestionRequest;
import pse.trippy.aiservice.dto.request.GenerateItineraryRequest;
import pse.trippy.aiservice.dto.request.TravelAdviceRequest;
import pse.trippy.aiservice.dto.request.TripConstraints;
import pse.trippy.aiservice.dto.response.DestinationSuggestionResponse;
import pse.trippy.aiservice.dto.response.ItineraryResponse;
import pse.trippy.aiservice.dto.response.TravelAdviceResponse;
import pse.trippy.aiservice.repository.AiRequestLogRepository;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiService")
class AiServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private AiRequestLogRepository aiRequestLogRepository;

    private AiService aiService;

    // Fluent chain mocks
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callSpec;

    @BeforeEach
    void setUp() {
        aiService = new AiService(chatClient, new ObjectMapper().registerModule(new JavaTimeModule()), aiRequestLogRepository);

        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
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
        @DisplayName("returns empty list when Groq response is malformed JSON")
        void returnsFallbackOnBadJson() {
            when(callSpec.content()).thenReturn("Sorry, I cannot help with that.");

            DestinationSuggestionRequest request = new DestinationSuggestionRequest(
                    null, null, List.of("history"), "MODERATE", null, 5, null, null,
                    null, null, null, null);

            DestinationSuggestionResponse response = aiService.suggestDestinations(request);

            assertThat(response.suggestions()).isEmpty();
            assertThat(response.generatedAt()).isNotNull();
        }

        @Test
        @DisplayName("strips markdown code fences from Groq response before parsing")
        void stripsMarkdownCodeFences() {
            String wrapped = "```json\n[]\n```";
            when(callSpec.content()).thenReturn(wrapped);

            DestinationSuggestionRequest request = new DestinationSuggestionRequest(
                    null, null, List.of("adventure"), "HIGH", null, 10, null, null,
                    null, null, null, null);

            DestinationSuggestionResponse response = aiService.suggestDestinations(request);

            assertThat(response.suggestions()).isEmpty();
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
                    new TripConstraints("Kyoto, Japan", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5),
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
        }

        @Test
        @DisplayName("throws RuntimeException when itinerary JSON cannot be parsed")
        void throwsOnBadJson() {
            when(callSpec.content()).thenReturn("I cannot generate that.");

            GenerateItineraryRequest request = new GenerateItineraryRequest(
                    null,
                    new TripConstraints("Kyoto", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5),
                            null, null, null),
                    null, null, null);

            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                    () -> aiService.generateItinerary(request));
        }
    }
}
