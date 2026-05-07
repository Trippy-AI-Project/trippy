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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
                eq("trip-invite"),
                varsCaptor.capture());

        Map<String, Object> vars = varsCaptor.getValue();
        assertThat(vars).containsEntry("inviteeName", "Bob");
        assertThat(vars).containsEntry("inviterName", "Jane");
        assertThat(vars).containsEntry("tripTitle", "Summer in Barcelona");
    }

    @Test
    @DisplayName("trip.participant.invited event triggers trip invite template")
    void tripParticipantInvitedDispatchesToInviteTemplate() {
        listener.handleEvent(Map.of(
                "tripId", "223e4567-e89b-12d3-a456-426614174000",
                "tripTitle", "Summer in Barcelona",
                "inviterName", "Jane",
                "participantEmail", "bob@test.com",
                "participantName", "Bob"), "trip.participant.invited");

        verify(emailService).sendTemplateEmail(
                eq("bob@test.com"),
                eq("Jane invited you to Summer in Barcelona"),
                eq("trip-invite"),
                any());
    }

    @Test
    @DisplayName("user.password.reset event triggers password reset template")
    void passwordResetDispatchesToTemplate() {
        UUID userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        listener.handleEvent(Map.of(
                "userId", userId.toString(),
                "email", "alice@test.com",
                "userName", "Alice",
                "resetLink", "https://trippy.app/reset?token=abc"), "user.password.reset");

        verify(emailService).sendTemplateEmail(
                eq("alice@test.com"),
                eq("Reset your Trippy password"),
                eq("password-reset"),
                any());
        verify(notificationService).createNotification(
                eq(userId),
                eq(NotificationType.PASSWORD_RESET),
                eq("Password Reset Requested"),
                eq("Use the password reset link we sent to update your Trippy password."),
                eq("/login"),
                any());
    }

    @Test
    @DisplayName("ai.itinerary.generated event sends email and stores notification")
    void itineraryGeneratedDispatchesToReadyHandler() {
        UUID userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        listener.handleEvent(Map.of(
                "userId", userId.toString(),
                "email", "alice@test.com",
                "userName", "Alice",
                "tripId", "223e4567-e89b-12d3-a456-426614174000",
                "tripTitle", "Kyoto Spring",
                "destination", "Kyoto"), "ai.itinerary.generated");

        verify(emailService).sendTemplateEmail(
                eq("alice@test.com"),
                eq("Your Trippy itinerary is ready"),
                eq("itinerary-ready"),
                any());
        verify(notificationService).createNotification(
                eq(userId),
                eq(NotificationType.ITINERARY_READY),
                eq("Itinerary Ready"),
                eq("Your itinerary for Kyoto Spring is ready to review."),
                eq("/dashboard/trips/223e4567-e89b-12d3-a456-426614174000"),
                any());
    }

    @Test
    @DisplayName("trip.invitation.created event stores in-app notification")
    void tripInvitationStoresNotification() {
        UUID inviteeId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        Map<String, Object> payload = Map.of(
                "tripId", "223e4567-e89b-12d3-a456-426614174000",
                "tripTitle", "Summer in Barcelona",
                "inviterName", "Jane",
                "inviteeId", inviteeId.toString(),
                "inviteeEmail", "bob@test.com",
                "inviteeName", "Bob");

        listener.handleTripInvitation(payload);

        verify(notificationService).createNotification(
                eq(inviteeId),
                eq(NotificationType.TRIP_INVITE),
                eq("Trip Invitation"),
                eq("Jane invited you to Summer in Barcelona"),
                eq("/dashboard/trips/223e4567-e89b-12d3-a456-426614174000"),
                any());
    }

    @Test
    @DisplayName("malformed UUID payload is skipped without crashing")
    void malformedUuidIsSkipped() {
        Map<String, Object> payload = Map.of(
                "tripTitle", "Bad ID Trip",
                "inviterName", "Jane",
                "inviteeId", "not-a-uuid",
                "inviteeEmail", "bob@test.com",
                "inviteeName", "Bob");

        assertThatCode(() -> listener.handleTripInvitation(payload)).doesNotThrowAnyException();
        verify(notificationService, never()).createNotification(
                any(),
                any(),
                any(),
                any(),
                any(),
                any());
    }

    @Test
    @DisplayName("malformed first UUID does not block valid fallback UUID")
    void malformedUuidFallsBackToNextCandidate() {
        UUID inviteeUserId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        Map<String, Object> payload = Map.of(
                "tripId", "223e4567-e89b-12d3-a456-426614174000",
                "tripTitle", "Summer in Barcelona",
                "inviterName", "Jane",
                "inviteeId", "not-a-uuid",
                "inviteeUserId", inviteeUserId.toString(),
                "inviteeEmail", "bob@test.com",
                "inviteeName", "Bob");

        listener.handleTripInvitation(payload);

        verify(notificationService).createNotification(
                eq(inviteeUserId),
                eq(NotificationType.TRIP_INVITE),
                eq("Trip Invitation"),
                eq("Jane invited you to Summer in Barcelona"),
                eq("/dashboard/trips/223e4567-e89b-12d3-a456-426614174000"),
                any());
    }

    @Test
    @DisplayName("notification metadata stores only allowed non-sensitive keys")
    void metadataExcludesSensitiveEventPayload() {
        UUID userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        Map<String, Object> payload = Map.of(
                "userId", userId.toString(),
                "email", "alice@test.com",
                "userName", "Alice",
                "resetLink", "https://trippy.app/reset?token=abc",
                "verificationToken", "secret-token");

        listener.handleEvent(payload, "user.password.reset");

        ArgumentCaptor<Map<String, Object>> metadataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationService).createNotification(
                eq(userId),
                eq(NotificationType.PASSWORD_RESET),
                eq("Password Reset Requested"),
                eq("Use the password reset link we sent to update your Trippy password."),
                eq("/login"),
                metadataCaptor.capture());

        assertThat(metadataCaptor.getValue()).isNull();
    }

    @Test
    @DisplayName("trip invite metadata allowlist excludes names and email addresses")
    void tripInviteMetadataAllowlistExcludesNamesAndEmails() {
        UUID inviteeId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        Map<String, Object> payload = Map.of(
                "tripId", "223e4567-e89b-12d3-a456-426614174000",
                "tripTitle", "Summer in Barcelona",
                "destination", "Barcelona",
                "role", "EDITOR",
                "inviterName", "Jane",
                "inviteeId", inviteeId.toString(),
                "inviteeEmail", "bob@test.com",
                "inviteeName", "Bob");

        listener.handleTripInvitation(payload);

        ArgumentCaptor<Map<String, Object>> metadataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationService).createNotification(
                eq(inviteeId),
                eq(NotificationType.TRIP_INVITE),
                eq("Trip Invitation"),
                eq("Jane invited you to Summer in Barcelona"),
                eq("/dashboard/trips/223e4567-e89b-12d3-a456-426614174000"),
                metadataCaptor.capture());

        assertThat(metadataCaptor.getValue())
                .containsEntry("tripId", "223e4567-e89b-12d3-a456-426614174000")
                .containsEntry("tripTitle", "Summer in Barcelona")
                .containsEntry("destination", "Barcelona")
                .containsEntry("role", "EDITOR")
                .doesNotContainKeys("inviteeEmail", "inviteeName", "inviterName");
    }

    @Test
    @DisplayName("itinerary ready event sends email and stores notification")
    void itineraryReadySendsEmailAndStoresNotification() {
        UUID userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        Map<String, Object> payload = Map.of(
                "userId", userId.toString(),
                "email", "alice@test.com",
                "userName", "Alice",
                "tripId", "223e4567-e89b-12d3-a456-426614174000",
                "tripTitle", "Kyoto Spring",
                "destination", "Kyoto");

        listener.handleItineraryReady(payload);

        verify(emailService).sendTemplateEmail(
                eq("alice@test.com"),
                eq("Your Trippy itinerary is ready"),
                eq("itinerary-ready"),
                any());
        verify(notificationService).createNotification(
                eq(userId),
                eq(NotificationType.ITINERARY_READY),
                eq("Itinerary Ready"),
                eq("Your itinerary for Kyoto Spring is ready to review."),
                eq("/dashboard/trips/223e4567-e89b-12d3-a456-426614174000"),
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
