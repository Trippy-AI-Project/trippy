package pse.trippy.notificationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pse.trippy.notificationservice.model.entity.EmailLog;
import pse.trippy.notificationservice.model.enums.EmailStatus;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, UUID> {

    List<EmailLog> findByRecipientOrderBySentAtDesc(String recipient);

    List<EmailLog> findByStatus(EmailStatus status);
}
