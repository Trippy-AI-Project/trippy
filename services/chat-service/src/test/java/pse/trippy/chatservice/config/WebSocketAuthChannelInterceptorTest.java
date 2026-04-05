package pse.trippy.chatservice.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import pse.trippy.chatservice.client.TripServiceClient;
import pse.trippy.chatservice.service.ChatMessageService;
import pse.trippy.chatservice.service.ChatPresenceService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketAuthChannelInterceptor")
class WebSocketAuthChannelInterceptorTest {

    @Mock
    private TripServiceClient tripServiceClient;

    @Mock
    private ChatPresenceService chatPresenceService;

    @Mock
    private ChatMessageService chatMessageService;

    @Mock
    private MessageChannel channel;

    private WebSocketAuthChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketAuthChannelInterceptor(
                tripServiceClient, chatPresenceService, chatMessageService);
    }

    @Test
    @DisplayName("allows subscription when user is a verified participant")
    void allowsSubscriptionForVerifiedParticipant() {
        UUID tripId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(tripServiceClient.isParticipant(tripId, userId)).thenReturn(true);
        when(chatPresenceService.addUser(tripId, userId)).thenReturn(true);

        Message<?> message = createSubscribeMessage(tripId, userId, "Alice");

        Message<?> result = interceptor.preSend(message, channel);
        assertThat(result).isNotNull();
        verify(tripServiceClient).isParticipant(tripId, userId);
    }

    @Test
    @DisplayName("rejects subscription when user is not a participant")
    void rejectsSubscriptionForNonParticipant() {
        UUID tripId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(tripServiceClient.isParticipant(tripId, userId)).thenReturn(false);

        Message<?> message = createSubscribeMessage(tripId, userId, "Intruder");

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("not a participant");
    }

    @Test
    @DisplayName("rejects subscription when X-User-Id header is missing")
    void rejectsSubscriptionWhenUserIdMissing() {
        UUID tripId = UUID.randomUUID();

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/trips/" + tripId + "/messages");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("Missing X-User-Id");
    }

    @Test
    @DisplayName("passes through non-SUBSCRIBE messages without checks")
    void passesThroughNonSubscribeMessages() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/trips/" + UUID.randomUUID() + "/send");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);
        assertThat(result).isNotNull();
        verify(tripServiceClient, never()).isParticipant(any(), any());
    }

    @Test
    @DisplayName("broadcasts system join message for first-time subscriber")
    void broadcastsJoinMessageForNewSubscriber() {
        UUID tripId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(tripServiceClient.isParticipant(tripId, userId)).thenReturn(true);
        when(chatPresenceService.addUser(tripId, userId)).thenReturn(true);

        Message<?> message = createSubscribeMessage(tripId, userId, "Alice");
        interceptor.preSend(message, channel);

        verify(chatMessageService).sendMessage(
                eq(tripId), eq(userId), eq("System"),
                eq("Alice joined the chat"),
                any());
    }

    @Test
    @DisplayName("does not broadcast join message for already-connected user")
    void doesNotBroadcastForExistingUser() {
        UUID tripId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(tripServiceClient.isParticipant(tripId, userId)).thenReturn(true);
        when(chatPresenceService.addUser(tripId, userId)).thenReturn(false);

        Message<?> message = createSubscribeMessage(tripId, userId, "Alice");
        interceptor.preSend(message, channel);

        verify(chatMessageService, never()).sendMessage(any(), any(), any(), any(), any());
    }

    private Message<?> createSubscribeMessage(UUID tripId, UUID userId, String displayName) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/trips/" + tripId + "/messages");
        accessor.addNativeHeader("X-User-Id", userId.toString());
        accessor.addNativeHeader("X-User-DisplayName", displayName);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
