package pse.trippy.chatservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pse.trippy.chatservice.model.entity.ChatRoom;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link ChatRoom} entities.
 */
@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

    Optional<ChatRoom> findByTripId(UUID tripId);

    boolean existsByTripId(UUID tripId);
}
