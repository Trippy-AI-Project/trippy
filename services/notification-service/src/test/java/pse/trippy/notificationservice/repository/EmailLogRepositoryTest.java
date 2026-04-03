package pse.trippy.notificationservice.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import pse.trippy.notificationservice.model.entity.EmailLog;
import pse.trippy.notificationservice.model.enums.EmailStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("test")
@DisplayName("EmailLogRepository")
class EmailLogRepositoryTest {

    @Autowired
    private EmailLogRepository emailLogRepository;

    @BeforeEach
    void setUp() {
        emailLogRepository.deleteAll();
    }

    private EmailLog saved(String recipient, EmailStatus status) {
        return emailLogRepository.save(EmailLog.builder()
                .recipient(recipient)
                .subject("Test Subject")
                .templateName("welcome")
                .status(status)
                .build());
    }

    @Nested
    @DisplayName("findByRecipientOrderBySentAtDesc")
    class FindByRecipient {

        @Test
        @DisplayName("returns logs for recipient")
        void returnsLogsForRecipient() {
            saved("user@test.com", EmailStatus.SENT);
            saved("other@test.com", EmailStatus.SENT);

            List<EmailLog> logs = emailLogRepository
                    .findByRecipientOrderBySentAtDesc("user@test.com");

            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getRecipient()).isEqualTo("user@test.com");
        }
    }

    @Nested
    @DisplayName("findByStatus")
    class FindByStatus {

        @Test
        @DisplayName("returns logs by status")
        void returnsLogsByStatus() {
            saved("a@test.com", EmailStatus.SENT);
            saved("b@test.com", EmailStatus.FAILED);
            saved("c@test.com", EmailStatus.SENT);

            List<EmailLog> sent = emailLogRepository.findByStatus(EmailStatus.SENT);
            assertThat(sent).hasSize(2);

            List<EmailLog> failed = emailLogRepository.findByStatus(EmailStatus.FAILED);
            assertThat(failed).hasSize(1);
        }
    }
}
