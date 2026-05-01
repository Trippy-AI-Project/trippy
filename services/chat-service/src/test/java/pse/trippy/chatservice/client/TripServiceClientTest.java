package pse.trippy.chatservice.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TripServiceClient")
class TripServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private TripServiceClient tripServiceClient;

    TripServiceClientTest() {
        // Inject the tripServiceUrl manually since @Value is not processed by Mockito
    }

    private TripServiceClient createClient() {
        return new TripServiceClient(restTemplate, "http://localhost:8082");
    }

    @Test
    @DisplayName("isParticipant returns true when trip-service responds 200")
    void isParticipantReturnsTrueOnSuccess() {
        TripServiceClient client = createClient();
        UUID tripId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String expectedUrl = "http://localhost:8082/trips/" + tripId + "/participants/" + userId;

        when(restTemplate.getForEntity(eq(expectedUrl), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        assertThat(client.isParticipant(tripId, userId)).isTrue();
    }

    @Test
    @DisplayName("isParticipant returns false when trip-service responds 404")
    void isParticipantReturnsFalseOnNotFound() {
        TripServiceClient client = createClient();
        UUID tripId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String expectedUrl = "http://localhost:8082/trips/" + tripId + "/participants/" + userId;

        when(restTemplate.getForEntity(eq(expectedUrl), eq(Void.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        assertThat(client.isParticipant(tripId, userId)).isFalse();
    }

    @Test
    @DisplayName("isParticipant returns false on connection failure")
    void isParticipantReturnsFalseOnConnectionError() {
        TripServiceClient client = createClient();
        UUID tripId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String expectedUrl = "http://localhost:8082/trips/" + tripId + "/participants/" + userId;

        when(restTemplate.getForEntity(eq(expectedUrl), eq(Void.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        assertThat(client.isParticipant(tripId, userId)).isFalse();
    }
}
