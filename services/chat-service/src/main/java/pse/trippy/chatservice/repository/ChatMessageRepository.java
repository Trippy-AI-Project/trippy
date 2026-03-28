package pse.trippy.chatservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pse.trippy.chatservice.model.entity.ChatMessage;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA repository for {@link ChatMessage} entities.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    Page<ChatMessage> findByChatRoomIdOrderByCreatedAtDesc(UUID roomId, Pageable pageable);

    Page<ChatMessage> findByChatRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(
            UUID roomId, Instant before, Pageable pageable);

    long countByChatRoomId(UUID roomId);
}
