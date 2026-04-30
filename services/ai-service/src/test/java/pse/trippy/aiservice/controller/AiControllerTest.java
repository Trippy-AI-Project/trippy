package pse.trippy.aiservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pse.trippy.aiservice.dto.request.DestinationSuggestionRequest;
import pse.trippy.aiservice.dto.request.GenerateItineraryRequest;
import pse.trippy.aiservice.dto.request.TravelAdviceRequest;
import pse.trippy.aiservice.dto.response.DestinationSuggestionResponse;
import pse.trippy.aiservice.dto.response.ItineraryResponse;
import pse.trippy.aiservice.dto.response.TravelAdviceResponse;
import pse.trippy.aiservice.service.AiService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AiController.class)
@DisplayName("AiController")
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiService aiService;

    // =========================================================================
    // POST /ai/destination-suggestions
    // =========================================================================

    @Test
    @DisplayName("POST /ai/destination-suggestions → 200 with suggestions")
    void destinationSuggestions_returns200() throws Exception {
        DestinationSuggestionResponse stubResponse = DestinationSuggestionResponse.builder()
                .suggestions(List.of(
                        DestinationSuggestionResponse.DestinationSuggestion.builder()
                                .city("Lisbon")
                                .country("Portugal")
                                .reason("Affordable and sunny")
                                .matchScore(0.92)
                                .build()))
                .generatedAt(Instant.now())
                .cached(false)
                .build();

        when(aiService.suggestDestinations(any())).thenReturn(stubResponse);

        DestinationSuggestionRequest request = new DestinationSuggestionRequest();
        request.setPrompt("Cheap beach destination in Europe for August");

        mockMvc.perform(post("/ai/destination-suggestions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions[0].city").value("Lisbon"))
                .andExpect(jsonPath("$.suggestions[0].matchScore").value(0.92))
                .andExpect(jsonPath("$.cached").value(false));
    }

    @Test
    @DisplayName("POST /ai/destination-suggestions → 400 when prompt is blank")
    void destinationSuggestions_returns400OnBlankPrompt() throws Exception {
        DestinationSuggestionRequest request = new DestinationSuggestionRequest();
        request.setPrompt(""); // blank — @NotBlank should fail

        mockMvc.perform(post("/ai/destination-suggestions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // POST /ai/travel-advice
    // =========================================================================

    @Test
    @DisplayName("POST /ai/travel-advice → 200 with advice")
    void travelAdvice_returns200() throws Exception {
        TravelAdviceResponse stubResponse = TravelAdviceResponse.builder()
                .answer("Take the Shinkansen — it takes ~2h 15m.")
                .relatedQuestions(List.of("What JR Pass is best?"))
                .generatedAt(Instant.now())
                .build();

        when(aiService.getTravelAdvice(any())).thenReturn(stubResponse);

        TravelAdviceRequest request = new TravelAdviceRequest();
        request.setQuestion("Best way to travel from Tokyo to Kyoto?");
        request.setDestination("Japan");

        mockMvc.perform(post("/ai/travel-advice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Take the Shinkansen — it takes ~2h 15m."))
                .andExpect(jsonPath("$.relatedQuestions[0]").value("What JR Pass is best?"));
    }

    @Test
    @DisplayName("POST /ai/travel-advice → 400 when required fields are missing")
    void travelAdvice_returns400OnMissingFields() throws Exception {
        TravelAdviceRequest request = new TravelAdviceRequest();
        // question and destination not set

        mockMvc.perform(post("/ai/travel-advice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // POST /ai/itineraries
    // =========================================================================

    @Test
    @DisplayName("POST /ai/itineraries → 200 with itinerary")
    void generateItinerary_returns200() throws Exception {
        ItineraryResponse stubResponse = ItineraryResponse.builder()
                .tripTitle("5 Days in Kyoto")
                .summary("Culture and temples")
                .dailyPlan(List.of(
                        ItineraryResponse.DayPlan.builder()
                                .dayNumber(1)
                                .date("2026-09-01")
                                .title("Arrival & Fushimi Inari")
                                .activities(List.of())
                                .build()))
                .packingTips(List.of("Comfortable shoes"))
                .travelTips(List.of("Get IC card"))
                .generatedAt(Instant.now())
                .build();

        when(aiService.generateItinerary(any())).thenReturn(stubResponse);

        GenerateItineraryRequest request = new GenerateItineraryRequest();
        GenerateItineraryRequest.Constraints constraints = new GenerateItineraryRequest.Constraints();
        constraints.setDestination("Kyoto, Japan");
        constraints.setStartDate(LocalDate.of(2026, 9, 1));
        constraints.setEndDate(LocalDate.of(2026, 9, 5));
        request.setConstraints(constraints);

        mockMvc.perform(post("/ai/itineraries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripTitle").value("5 Days in Kyoto"))
                .andExpect(jsonPath("$.dailyPlan[0].dayNumber").value(1))
                .andExpect(jsonPath("$.packingTips[0]").value("Comfortable shoes"));
    }
}
