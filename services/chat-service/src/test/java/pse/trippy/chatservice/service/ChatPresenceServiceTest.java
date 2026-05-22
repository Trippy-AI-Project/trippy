package pse.trippy.chatservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatPresenceServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatPresenceService presenceService;

    private UUID tripId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tripId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void addUser_firstTime_returnsTrueAndBroadcasts() {
        boolean added = presenceService.addUser(tripId, userId);

        assertThat(added).isTrue();
        assertThat(presenceService.getConnectedUsers(tripId)).containsExactlyInAnyOrder(userId);
        verify(messagingTemplate, times(1))
                .convertAndSend(eq("/topic/trips/" + tripId + "/participants"), any(Set.class));
    }

    @Test
    void addUser_duplicate_returnsFalseAndDoesNotBroadcastAgain() {
        presenceService.addUser(tripId, userId);

        boolean added = presenceService.addUser(tripId, userId);

        assertThat(added).isFalse();
        verify(messagingTemplate, times(1))
                .convertAndSend(eq("/topic/trips/" + tripId + "/participants"), any(Set.class));
    }

    @Test
    void removeUser_present_broadcastsUpdate() {
        presenceService.addUser(tripId, userId);

        presenceService.removeUser(tripId, userId);

        assertThat(presenceService.getConnectedUsers(tripId)).isEmpty();
        verify(messagingTemplate, times(2))
                .convertAndSend(eq("/topic/trips/" + tripId + "/participants"), any(Set.class));
    }

    @Test
    void removeUser_notPresent_doesNotBroadcast() {
        presenceService.removeUser(tripId, userId);

        verify(messagingTemplate, never())
                .convertAndSend(any(String.class), any(Set.class));
    }

    @Test
    void getConnectedUsers_emptyRoom_returnsEmptySet() {
        assertThat(presenceService.getConnectedUsers(tripId)).isEmpty();
    }

    @Test
    void multipleUsers_inSameRoom_allTracked() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        presenceService.addUser(tripId, userA);
        presenceService.addUser(tripId, userB);

        assertThat(presenceService.getConnectedUsers(tripId))
                .containsExactlyInAnyOrder(userA, userB);
    }
}
