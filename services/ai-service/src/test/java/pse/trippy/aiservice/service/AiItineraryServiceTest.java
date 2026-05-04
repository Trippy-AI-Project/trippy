package pse.trippy.aiservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import pse.trippy.aiservice.dto.request.GenerateItineraryRequest;
import pse.trippy.aiservice.dto.request.TripConstraints;
import pse.trippy.aiservice.dto.response.ItineraryGenerationResponse;
import pse.trippy.aiservice.model.entity.AiRequestLog;
import pse.trippy.aiservice.model.entity.GenerationHistory;
import pse.trippy.aiservice.model.enums.RequestStatus;
import pse.trippy.aiservice.model.enums.RequestType;
import pse.trippy.aiservice.repository.AiRequestLogRepository;
import pse.trippy.aiservice.repository.GenerationHistoryRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiItineraryServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private AiRequestLogRepository aiRequestLogRepository;

    @Mock
    private GenerationHistoryRepository generationHistoryRepository;

    private AiItineraryService aiItineraryService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        aiItineraryService = new AiItineraryService(
                chatClientBuilder, aiRequestLogRepository, generationHistoryRepository, objectMapper);
    }

    @Test
    void generateItinerary_success() {
        UUID userId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();
        GenerateItineraryRequest request = new GenerateItineraryRequest(
                tripId,
                new TripConstraints("Kyoto, Japan", LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 3),
                        "MODERATE", new TripConstraints.Travelers(2, 0), "HOTEL"),
                "I love temples",
                "CULTURAL",
                null
        );

        String aiResponse = """
                {
                  "overview": "3-day cultural tour of Kyoto",
                  "estimatedTotalCost": "¥150,000",
                  "days": [
                    {
                      "dayNumber": 1,
                      "title": "Temple Exploration",
                      "activities": [
                        {
                          "time": "09:00",
                          "title": "Visit Kinkaku-ji",
                          "description": "Golden Pavilion temple",
                          "location": "Kinkaku-ji, Kita Ward",
                          "durationMinutes": 90,
                          "category": "SIGHTSEEING",
                          "estimatedCost": "¥500",
                          "lat": 35.0394,
                          "lng": 135.7292
                        }
                      ]
                    }
                  ]
                }
                """;

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(aiResponse);
        when(aiRequestLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(generationHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ItineraryGenerationResponse response = aiItineraryService.generateItinerary(userId, request);

        assertThat(response.tripId()).isEqualTo(tripId);
        assertThat(response.generationId()).isNotNull();
        assertThat(response.overview()).isEqualTo("3-day cultural tour of Kyoto");
        assertThat(response.estimatedTotalCost()).isEqualTo("¥150,000");
        assertThat(response.days()).hasSize(1);
        assertThat(response.days().get(0).dayNumber()).isEqualTo(1);
        assertThat(response.days().get(0).title()).isEqualTo("Temple Exploration");
        assertThat(response.days().get(0).date()).isEqualTo(LocalDate.of(2025, 3, 1));
        assertThat(response.days().get(0).activities()).hasSize(1);
        assertThat(response.days().get(0).activities().get(0).title()).isEqualTo("Visit Kinkaku-ji");
        assertThat(response.days().get(0).activities().get(0).durationMinutes()).isEqualTo(90);
        assertThat(response.days().get(0).activities().get(0).lat()).isEqualTo(35.0394);
        assertThat(response.generatedAt()).isNotNull();
        assertThat(response.cached()).isFalse();

        ArgumentCaptor<AiRequestLog> logCaptor = ArgumentCaptor.forClass(AiRequestLog.class);
        verify(aiRequestLogRepository).save(logCaptor.capture());
        AiRequestLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getUserId()).isEqualTo(userId);
        assertThat(savedLog.getRequestType()).isEqualTo(RequestType.ITINERARY_GENERATION);
        assertThat(savedLog.getStatus()).isEqualTo(RequestStatus.SUCCESS);
        assertThat(savedLog.getInputTokens()).isPositive();
        assertThat(savedLog.getOutputTokens()).isPositive();

        ArgumentCaptor<GenerationHistory> historyCaptor = ArgumentCaptor.forClass(GenerationHistory.class);
        verify(generationHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getGenerationId()).isEqualTo(response.generationId());
        assertThat(historyCaptor.getValue().getStatus()).isEqualTo(RequestStatus.SUCCESS);
        assertThat(historyCaptor.getValue().getResponseJson()).contains("3-day cultural tour");
    }

    @Test
    void generateItinerary_aiFailure_throwsException() {
        UUID userId = UUID.randomUUID();
        GenerateItineraryRequest request = new GenerateItineraryRequest(
                UUID.randomUUID(),
                new TripConstraints("Paris, France", LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 3),
                        null, null, null),
                null,
                null,
                null
        );

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("API timeout"));
        when(aiRequestLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(generationHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> aiItineraryService.generateItinerary(userId, request))
                .isInstanceOf(AiServiceUnavailableException.class)
                .hasMessageContaining("unavailable");

        ArgumentCaptor<AiRequestLog> logCaptor = ArgumentCaptor.forClass(AiRequestLog.class);
        verify(aiRequestLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(RequestStatus.FAILED);

        ArgumentCaptor<GenerationHistory> historyCaptor = ArgumentCaptor.forClass(GenerationHistory.class);
        verify(generationHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getStatus()).isEqualTo(RequestStatus.FAILED);
    }

    @Test
    void buildPrompt_containsAllConstraints() {
        GenerateItineraryRequest request = new GenerateItineraryRequest(
                UUID.randomUUID(),
                new TripConstraints("Kyoto, Japan", LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 5),
                        "LUXURY", new TripConstraints.Travelers(2, 1), "HOTEL"),
                "Focus on temples",
                "CULTURAL",
                new GenerateItineraryRequest.ItineraryPreferences(
                        true, true, false, "SLOW",
                        List.of("Kinkaku-ji", "Fushimi Inari"),
                        List.of("Tourist traps")
                )
        );

        String prompt = aiItineraryService.buildPrompt(request);

        assertThat(prompt).contains("Kyoto, Japan");
        assertThat(prompt).contains("2025-03-01");
        assertThat(prompt).contains("2025-03-05");
        assertThat(prompt).contains("5"); // 5 days
        assertThat(prompt).contains("LUXURY");
        assertThat(prompt).contains("2 adults, 1 children");
        assertThat(prompt).contains("HOTEL");
        assertThat(prompt).contains("CULTURAL");
        assertThat(prompt).contains("Focus on temples");
        assertThat(prompt).contains("SLOW");
        assertThat(prompt).contains("Kinkaku-ji");
        assertThat(prompt).contains("Fushimi Inari");
        assertThat(prompt).contains("Tourist traps");
    }

    @Test
    void buildPrompt_handlesMinimalConstraints() {
        GenerateItineraryRequest request = new GenerateItineraryRequest(
                null,
                new TripConstraints("Paris, France", LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 2),
                        null, null, null),
                null,
                null,
                null
        );

        String prompt = aiItineraryService.buildPrompt(request);

        assertThat(prompt).contains("Paris, France");
        assertThat(prompt).contains("2025-06-01");
        assertThat(prompt).contains("2025-06-02");
        assertThat(prompt).doesNotContain("Budget Level");
        assertThat(prompt).doesNotContain("Travelers");
        assertThat(prompt).doesNotContain("Accommodation");
    }

    @Test
    void parseResponse_handlesCodeFences() {
        GenerateItineraryRequest request = new GenerateItineraryRequest(
                UUID.randomUUID(),
                new TripConstraints("Rome, Italy", LocalDate.of(2025, 4, 10), LocalDate.of(2025, 4, 11),
                        null, null, null),
                null, null, null
        );

        String aiResponse = """
                ```json
                {
                  "overview": "Rome highlights",
                  "estimatedTotalCost": "€500",
                  "days": [
                    {
                      "dayNumber": 1,
                      "title": "Ancient Rome",
                      "activities": [
                        {
                          "time": "10:00",
                          "title": "Colosseum Tour",
                          "description": "Visit the iconic Colosseum",
                          "location": "Colosseum, Rome",
                          "durationMinutes": 120,
                          "category": "SIGHTSEEING",
                          "estimatedCost": "€16",
                          "lat": 41.8902,
                          "lng": 12.4922
                        }
                      ]
                    }
                  ]
                }
                ```
                """;

        ItineraryGenerationResponse response = aiItineraryService.parseResponse(aiResponse, request);

        assertThat(response.overview()).isEqualTo("Rome highlights");
        assertThat(response.days()).hasSize(1);
        assertThat(response.days().get(0).activities().get(0).title()).isEqualTo("Colosseum Tour");
    }

    @Test
    void parseResponse_invalidJson_throwsException() {
        GenerateItineraryRequest request = new GenerateItineraryRequest(
                UUID.randomUUID(),
                new TripConstraints("London, UK", LocalDate.of(2025, 5, 1), LocalDate.of(2025, 5, 2),
                        null, null, null),
                null, null, null
        );

        String invalidResponse = "This is not valid JSON at all";

        assertThatThrownBy(() -> aiItineraryService.parseResponse(invalidResponse, request))
                .isInstanceOf(AiServiceUnavailableException.class)
                .hasMessageContaining("Failed to process AI response");
    }
}
