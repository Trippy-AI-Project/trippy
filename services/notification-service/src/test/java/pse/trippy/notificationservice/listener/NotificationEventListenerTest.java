package pse.trippy.notificationservice.listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pse.trippy.notificationservice.model.enums.NotificationType;
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
        String inviteeId = "123e4567-e89b-12d3-a456-426614174000";
        Map<String, Object> payload = Map.of(
                "tripId", "trip-uuid",
                "tripTitle", "Summer in Barcelona",
                "inviterId", "inviter-uuid",
                "inviterName", "Jane",
                "inviteeEmail", "bob@test.com",
                "inviteeName", "Bob",
                "inviteeId", inviteeId);

        listener.handleTripInvitation(payload);

        ArgumentCaptor<Map<String, Object>> varsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(emailService).sendTemplateEmail(
                eq("bob@test.com"),
                eq("Jane invited you to Summer in Barcelona"),
                eq("trip-invite"),
                varsCaptor.capture());

        Map<String, Object> vars = varsCaptor.getValue();
        assertThat(vars).containsEntry("inviteeName", "Bob");
        assertThat(vars).containsEntry("inviterName", "Jane");
        assertThat(vars).containsEntry("tripTitle", "Summer in Barcelona");

        verify(notificationService).createNotification(
                eq(java.util.UUID.fromString(inviteeId)),
                eq(NotificationType.TRIP_INVITE),
                eq("Trip Invitation"),
                eq("Jane invited you to Summer in Barcelona"),
                eq("/dashboard/trips/trip-uuid"),
                any());
    }

    @Test
    @DisplayName("ai.itinerary.generated event triggers itinerary-ready email")
    void itineraryGeneratedTriggersEmail() {
        Map<String, Object> payload = Map.of(
                "userId", "123e4567-e89b-12d3-a456-426614174000",
                "email", "alice@test.com",
                "userName", "Alice",
                "tripId", "trip-uuid",
                "tripTitle", "Kyoto",
                "generationId", "generation-1");

        listener.handleEvent(payload, "ai.itinerary.generated");

        verify(emailService).sendTemplateEmail(
                eq("alice@test.com"),
                eq("Your itinerary is ready"),
                eq("itinerary-ready"),
                any());
        verify(notificationService).createNotification(
                eq(java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614174000")),
                eq(NotificationType.ITINERARY_READY),
                eq("Itinerary Ready"),
                eq("Your itinerary for Kyoto is ready."),
                eq("/dashboard/trips/trip-uuid"),
                any());
    }

    @Test
    @DisplayName("user.password.reset event triggers password reset email")
    void passwordResetTriggersEmail() {
        Map<String, Object> payload = Map.of(
                "userId", "123e4567-e89b-12d3-a456-426614174000",
                "email", "alice@test.com",
                "userName", "Alice",
                "resetLink", "https://trippy.app/reset?token=abc");

        listener.handleEvent(payload, "user.password.reset");

        verify(emailService).sendTemplateEmail(
                eq("alice@test.com"),
                eq("Reset your Trippy password"),
                eq("password-reset"),
                any());
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
