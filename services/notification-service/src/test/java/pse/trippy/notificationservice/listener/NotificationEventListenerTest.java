package pse.trippy.notificationservice.listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pse.trippy.notificationservice.service.EmailService;
import pse.trippy.notificationservice.service.NotificationService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEventListener")
class NotificationEventListenerTest {

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationEventListener listener;

    @Test
    @DisplayName("user.registered event triggers verification email")
    void userRegisteredTriggersVerification() {
        Map<String, Object> payload = Map.of(
                "userId", "123e4567-e89b-12d3-a456-426614174000",
                "email", "alice@test.com",
                "displayName", "Alice",
                "verificationToken", "123456");

        listener.handleUserRegistered(payload);

        ArgumentCaptor<Map<String, Object>> varsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(emailService).sendTemplateEmail(
                eq("alice@test.com"),
                eq("Verify your Trippy account"),
                eq("email-verification"),
                varsCaptor.capture());

        assertThat(varsCaptor.getValue()).containsEntry("userName", "Alice");
        assertThat(varsCaptor.getValue()).containsEntry("verificationCode", "123456");
    }

    @Test
    @DisplayName("user.email.verified event triggers welcome email")
    void userEmailVerifiedTriggersWelcome() {
        Map<String, Object> payload = Map.of(
                "userId", "123e4567-e89b-12d3-a456-426614174000",
                "email", "alice@test.com",
                "displayName", "Alice");

        listener.handleUserEmailVerified(payload);

        verify(emailService).sendTemplateEmail(
                eq("alice@test.com"),
                eq("Welcome to Trippy!"),
                eq("welcome"),
                any());
    }

    @Test
    @DisplayName("trip.invitation.created event triggers invitation email")
    void tripInvitationTriggersEmail() {
        Map<String, Object> payload = Map.of(
                "tripId", "trip-uuid",
                "tripTitle", "Summer in Barcelona",
                "inviterId", "inviter-uuid",
                "inviterName", "Jane",
                "inviteeEmail", "bob@test.com",
                "inviteeName", "Bob");

        listener.handleTripInvitation(payload);

        ArgumentCaptor<Map<String, Object>> varsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(emailService).sendTemplateEmail(
                eq("bob@test.com"),
                eq("Jane invited you to Summer in Barcelona"),
                eq("trip-invitation"),
                varsCaptor.capture());

        Map<String, Object> vars = varsCaptor.getValue();
        assertThat(vars).containsEntry("inviteeName", "Bob");
        assertThat(vars).containsEntry("inviterName", "Jane");
        assertThat(vars).containsEntry("tripTitle", "Summer in Barcelona");
    }

    @Test
    @DisplayName("handleEvent dispatches based on routing key")
    void handleEventDispatches() {
        Map<String, Object> payload = Map.of(
                "email", "user@test.com",
                "displayName", "User",
                "verificationToken", "654321");

        listener.handleEvent(payload, "user.registered");

        verify(emailService).sendTemplateEmail(
                eq("user@test.com"),
                eq("Verify your Trippy account"),
                eq("email-verification"),
                any());
    }
}
