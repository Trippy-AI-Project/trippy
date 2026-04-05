package pse.trippy.chatservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pse.trippy.chatservice.model.entity.MessageAttachment;

import java.util.List;
import java.util.UUID;

/**
 * JPA repository for {@link MessageAttachment} entities.
 */
@Repository
public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, UUID> {

    List<MessageAttachment> findByMessageId(UUID messageId);
}
