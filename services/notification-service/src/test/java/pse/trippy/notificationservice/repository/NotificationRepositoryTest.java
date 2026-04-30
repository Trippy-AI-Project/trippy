package pse.trippy.notificationservice.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import pse.trippy.notificationservice.model.entity.Notification;
import pse.trippy.notificationservice.model.enums.NotificationChannel;
import pse.trippy.notificationservice.model.enums.NotificationType;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("test")
@DisplayName("NotificationRepository")
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    private Notification saved(NotificationType type, boolean read) {
        return notificationRepository.save(Notification.builder()
                .userId(USER_ID)
                .type(type)
                .title("Test " + type.name())
                .message("Test message")
                .channel(NotificationChannel.IN_APP)
                .read(read)
                .build());
    }

    @Nested
    @DisplayName("findByUserIdOrderByCreatedAtDesc")
    class FindByUserId {

        @Test
        @DisplayName("returns paginated notifications for user")
        void returnsPaginatedNotifications() {
            saved(NotificationType.WELCOME, false);
            saved(NotificationType.TRIP_INVITATION, false);

            Page<Notification> page = notificationRepository
                    .findByUserIdOrderByCreatedAtDesc(USER_ID, PageRequest.of(0, 10));

            assertThat(page.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("returns empty for unknown user")
        void returnsEmptyForUnknownUser() {
            saved(NotificationType.WELCOME, false);

            Page<Notification> page = notificationRepository
                    .findByUserIdOrderByCreatedAtDesc(UUID.randomUUID(), PageRequest.of(0, 10));

            assertThat(page.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUserIdAndReadFalse")
    class FindUnread {

        @Test
        @DisplayName("returns only unread notifications")
        void returnsOnlyUnread() {
            saved(NotificationType.WELCOME, false);
            saved(NotificationType.TRIP_UPDATED, true);

            List<Notification> unread = notificationRepository
                    .findByUserIdAndReadFalseOrderByCreatedAtDesc(USER_ID);

            assertThat(unread).hasSize(1);
            assertThat(unread.get(0).getType()).isEqualTo(NotificationType.WELCOME);
        }
    }

    @Nested
    @DisplayName("countByUserIdAndReadFalse")
    class CountUnread {

        @Test
        @DisplayName("counts unread notifications")
        void countsUnread() {
            saved(NotificationType.WELCOME, false);
            saved(NotificationType.SYSTEM, false);
            saved(NotificationType.TRIP_UPDATED, true);

            long count = notificationRepository.countByUserIdAndReadFalse(USER_ID);

            assertThat(count).isEqualTo(2);
        }
    }
}
