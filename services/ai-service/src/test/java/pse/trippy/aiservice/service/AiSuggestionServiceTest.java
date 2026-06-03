package pse.trippy.aiservice.service;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import pse.trippy.aiservice.dto.request.DestinationSuggestionRequest;
import pse.trippy.aiservice.dto.response.DestinationSuggestionResponse;
import pse.trippy.aiservice.model.entity.AiRequestLog;
import pse.trippy.aiservice.model.enums.RequestStatus;
import pse.trippy.aiservice.model.enums.RequestType;
import pse.trippy.aiservice.repository.AiRequestLogRepository;

@ExtendWith(MockitoExtension.class)
class AiSuggestionServiceTest {

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
    private AiSuggestionService aiSuggestionService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
      aiSuggestionService = new AiSuggestionService(chatClientBuilder, aiRequestLogRepository, objectMapper);
    }

    @Test
    void getDestinationSuggestions_success() {
        UUID userId = UUID.randomUUID();
        DestinationSuggestionRequest request = new DestinationSuggestionRequest(
                null, null, List.of("history", "food"), "MODERATE", "ADVENTURE", 7, "Europe", "July",
                null, null, null, null
        );

        String aiResponse = """
                [
                  {
                    "destination": "Barcelona, Spain",
                    "country": "Spain",
                    "description": "A vibrant city with stunning architecture and cuisine.",
                    "highlights": ["Sagrada Familia", "La Boqueria Market", "Gothic Quarter"],
                    "estimatedDailyCost": 120.00,
                    "bestTimeToVisit": "May - September",
                    "matchScore": 0.92
                  }
                ]
                """;

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(aiResponse);
        when(aiRequestLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DestinationSuggestionResponse response = aiSuggestionService.getDestinationSuggestions(userId, request);

        assertThat(response.suggestions()).hasSize(1);
        assertThat(response.suggestions().get(0).destination()).isEqualTo("Barcelona, Spain");
        assertThat(response.suggestions().get(0).matchScore()).isEqualTo(0.92);
        assertThat(response.generatedAt()).isNotNull();
        assertThat(response.cached()).isFalse();

        ArgumentCaptor<AiRequestLog> logCaptor = ArgumentCaptor.forClass(AiRequestLog.class);
        verify(aiRequestLogRepository).save(logCaptor.capture());
        AiRequestLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getUserId()).isEqualTo(userId);
        assertThat(savedLog.getRequestType()).isEqualTo(RequestType.DESTINATION_SUGGESTION);
        assertThat(savedLog.getStatus()).isEqualTo(RequestStatus.SUCCESS);
    }

    @Test
    void getDestinationSuggestions_aiFailure_throws503() {
        UUID userId = UUID.randomUUID();
        DestinationSuggestionRequest request = new DestinationSuggestionRequest(
                null, null, List.of("beaches"), "LOW", null, 5, null, null,
                null, null, null, null
        );

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("API timeout"));
        when(aiRequestLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> aiSuggestionService.getDestinationSuggestions(userId, request))
                .isInstanceOf(AiServiceUnavailableException.class)
                .hasMessageContaining("unavailable");

        ArgumentCaptor<AiRequestLog> logCaptor = ArgumentCaptor.forClass(AiRequestLog.class);
        verify(aiRequestLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(RequestStatus.FAILED);
    }

    @Test
    void buildPrompt_containsAllPreferences() {
        DestinationSuggestionRequest request = new DestinationSuggestionRequest(
                null, null, List.of("history", "food"), "MODERATE", "ADVENTURE", 7, "Europe", "July",
                null, null, null, null
        );

        String prompt = aiSuggestionService.buildPrompt(request);

        assertThat(prompt).contains("history, food");
        assertThat(prompt).contains("MODERATE");
        assertThat(prompt).contains("ADVENTURE");
        assertThat(prompt).contains("7 days");
        assertThat(prompt).contains("Europe");
        assertThat(prompt).contains("July");
    }

    @Test
    void parseResponse_handlesCodeFences() {
        String aiResponse = """
                ```json
                [
                  {
                    "destination": "Athens, Greece",
                    "country": "Greece",
                    "description": "Ancient city with rich history.",
                    "highlights": ["Acropolis", "Plaka", "Temple of Zeus"],
                    "estimatedDailyCost": 80.00,
                    "bestTimeToVisit": "April - October",
                    "matchScore": 0.88
                  }
                ]
                ```
                """;

        var suggestions = aiSuggestionService.parseResponse(aiResponse);

        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.get(0).destination()).isEqualTo("Athens, Greece");
    }
}
