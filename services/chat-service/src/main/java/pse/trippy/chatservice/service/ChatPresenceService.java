package pse.trippy.chatservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which users are currently connected to each chat room (in-memory).
 */
@Service
@Slf4j
public class ChatPresenceService {

    private final Map<UUID, Set<UUID>> roomParticipants = new ConcurrentHashMap<>();

    /**
     * Adds a user to a trip's chat room.
     *
     * @return true if the user was newly added (first join), false if already present
     */
    public boolean addUser(UUID tripId, UUID userId) {
        Set<UUID> users = roomParticipants.computeIfAbsent(tripId,
                k -> ConcurrentHashMap.newKeySet());
        boolean added = users.add(userId);
        if (added) {
            log.info("User {} joined chat for trip {} (total: {})", userId, tripId, users.size());
        }
        return added;
    }

    /**
     * Removes a user from a trip's chat room.
     */
    public void removeUser(UUID tripId, UUID userId) {
        Set<UUID> users = roomParticipants.get(tripId);
        if (users != null) {
            users.remove(userId);
            log.info("User {} left chat for trip {} (total: {})", userId, tripId, users.size());
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
}
