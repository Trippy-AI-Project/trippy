package pse.trippy.chatservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which users are currently connected to each chat room (in-memory)
 * and broadcasts participant list updates to STOMP subscribers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatPresenceService {

    private static final String PARTICIPANTS_TOPIC = "/topic/trips/%s/participants";

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<UUID, Set<UUID>> roomParticipants = new ConcurrentHashMap<>();

    /**
     * Adds a user to a trip's chat room and broadcasts the updated participant list.
     *
     * @return true if the user was newly added (first join), false if already present
     */
    public boolean addUser(UUID tripId, UUID userId) {
        Set<UUID> users = roomParticipants.computeIfAbsent(tripId,
                k -> ConcurrentHashMap.newKeySet());
        boolean added = users.add(userId);
        if (added) {
            log.info("User {} joined chat for trip {} (total: {})", userId, tripId, users.size());
            broadcastParticipants(tripId, users);
        }
        return added;
    }

    /**
     * Removes a user from a trip's chat room and broadcasts the updated participant list.
     */
    public void removeUser(UUID tripId, UUID userId) {
        Set<UUID> users = roomParticipants.get(tripId);
        if (users != null && users.remove(userId)) {
            log.info("User {} left chat for trip {} (total: {})", userId, tripId, users.size());
            // Broadcast a snapshot before potentially removing the empty room.
            broadcastParticipants(tripId, users);
            if (users.isEmpty()) {
                roomParticipants.remove(tripId);
            }
        }
    }

    /**
     * Returns the set of user IDs currently in the trip's chat room.
     */
    public Set<UUID> getConnectedUsers(UUID tripId) {
        return Collections.unmodifiableSet(
                roomParticipants.getOrDefault(tripId, Collections.emptySet()));
    }

    private void broadcastParticipants(UUID tripId, Set<UUID> users) {
        try {
            messagingTemplate.convertAndSend(
                    String.format(PARTICIPANTS_TOPIC, tripId),
                    Set.copyOf(users));
        } catch (Exception e) {
            log.warn("Failed to broadcast participant update for trip {}: {}", tripId, e.getMessage());
        }
    }
}
