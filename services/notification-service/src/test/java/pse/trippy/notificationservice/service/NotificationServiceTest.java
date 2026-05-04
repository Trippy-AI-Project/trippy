package pse.trippy.notificationservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import pse.trippy.notificationservice.dto.response.NotificationResponse;
import pse.trippy.notificationservice.model.entity.Notification;
import pse.trippy.notificationservice.model.enums.NotificationChannel;
import pse.trippy.notificationservice.model.enums.NotificationType;
import pse.trippy.notificationservice.repository.NotificationRepository;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("test")
@Import(NotificationService.class)
@DisplayName("NotificationService")
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    @DisplayName("createNotification saves and returns notification with correct fields")
    void createNotificationSavesCorrectly() {
        Notification result = notificationService.createNotification(
                USER_ID,
                NotificationType.TRIP_INVITE,
                "Welcome!",
                "Welcome to Trippy",
                "https://trippy.app/dashboard",
                Map.of("tripId", "trip-1"));

        assertThat(result.getId()).isNotNull();
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getType()).isEqualTo(NotificationType.TRIP_INVITE);
        assertThat(result.getTitle()).isEqualTo("Welcome!");
        assertThat(result.getMessage()).isEqualTo("Welcome to Trippy");
        assertThat(result.getActionUrl()).isEqualTo("/dashboard");
        assertThat(result.getMetadata()).containsEntry("tripId", "trip-1");
        assertThat(result.getChannel()).isEqualTo(NotificationChannel.IN_APP);
        assertThat(result.isRead()).isFalse();
        assertThat(result.getReadAt()).isNull();
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("getNotifications returns paginated results for user")
    void getNotificationsReturnsPaginated() {
        notificationService.createNotification(USER_ID, NotificationType.WELCOME,
                "Welcome", "msg1", null);
        notificationService.createNotification(USER_ID, NotificationType.TRIP_UPDATED,
                "Trip Updated", "msg2", null);
        notificationService.createNotification(USER_ID, NotificationType.PAYMENT_SUCCESS,
                "Payment", "msg3", null);

        Page<NotificationResponse> page = notificationService.getNotifications(USER_ID, 0, 2);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("getNotifications bounds invalid pagination input")
    void getNotificationsBoundsPagination() {
        notificationService.createNotification(USER_ID, NotificationType.WELCOME,
                "Welcome", "msg1", null);

        Page<NotificationResponse> page = notificationService.getNotifications(USER_ID, -1, 0);

        assertThat(page.getNumber()).isZero();
        assertThat(page.getSize()).isEqualTo(20);
        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("createNotification drops unsafe action URLs")
    void createNotificationDropsUnsafeActionUrls() {
        Notification external = notificationService.createNotification(USER_ID,
                NotificationType.SYSTEM, "External", "msg", "https://evil.example/phish");
        Notification script = notificationService.createNotification(USER_ID,
                NotificationType.SYSTEM, "Script", "msg", "javascript:alert(1)");
        Notification traversal = notificationService.createNotification(USER_ID,
                NotificationType.SYSTEM, "Traversal", "msg", "/dashboard/%2e%2e/admin");
        Notification internal = notificationService.createNotification(USER_ID,
                NotificationType.SYSTEM, "Internal", "msg", "/dashboard/trips/1?tab=chat");

        assertThat(external.getActionUrl()).isNull();
        assertThat(script.getActionUrl()).isNull();
        assertThat(traversal.getActionUrl()).isNull();
        assertThat(internal.getActionUrl()).isEqualTo("/dashboard/trips/1?tab=chat");
    }

    @Test
    @DisplayName("getUnreadCount returns count of unread notifications")
    void getUnreadCountReturnsCorrectCount() {
        notificationService.createNotification(USER_ID, NotificationType.WELCOME,
                "Welcome", "msg1", null);
        notificationService.createNotification(USER_ID, NotificationType.TRIP_UPDATED,
                "Updated", "msg2", null);

        long count = notificationService.getUnreadCount(USER_ID);

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("markAsRead marks a single notification as read")
    void markAsReadMarksSingle() {
        Notification n = notificationService.createNotification(USER_ID,
                NotificationType.WELCOME, "Welcome", "msg", null);

        notificationService.markAsRead(n.getId(), USER_ID);

        Notification updated = notificationRepository.findById(n.getId()).orElseThrow();
        assertThat(updated.isRead()).isTrue();
        assertThat(updated.getReadAt()).isNotNull();
        assertThat(notificationService.getUnreadCount(USER_ID)).isZero();
    }

    @Test
    @DisplayName("markAsRead ignores notifications owned by another user")
    void markAsReadRequiresOwner() {
        Notification n = notificationService.createNotification(USER_ID,
                NotificationType.WELCOME, "Welcome", "msg", null);

        notificationService.markAsRead(n.getId(), UUID.randomUUID());

        Notification updated = notificationRepository.findById(n.getId()).orElseThrow();
        assertThat(updated.isRead()).isFalse();
        assertThat(updated.getReadAt()).isNull();
    }

    @Test
    @DisplayName("markAllAsRead marks all user notifications as read")
    void markAllAsReadMarksAll() {
        notificationService.createNotification(USER_ID, NotificationType.WELCOME,
                "Welcome", "msg1", null);
        notificationService.createNotification(USER_ID, NotificationType.TRIP_UPDATED,
                "Updated", "msg2", null);
        notificationService.createNotification(USER_ID, NotificationType.PAYMENT_SUCCESS,
                "Payment", "msg3", null);

        assertThat(notificationService.getUnreadCount(USER_ID)).isEqualTo(3);

        notificationService.markAllAsRead(USER_ID);

        assertThat(notificationService.getUnreadCount(USER_ID)).isZero();
    }

    @Test
    @DisplayName("getNotifications returns empty page for unknown user")
    void getNotificationsEmptyForUnknownUser() {
        notificationService.createNotification(USER_ID, NotificationType.WELCOME,
                "Welcome", "msg", null);

        Page<NotificationResponse> page = notificationService
                .getNotifications(UUID.randomUUID(), 0, 10);

        assertThat(page.getContent()).isEmpty();
    }
}
