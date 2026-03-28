package pse.trippy.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pse.trippy.userservice.model.entity.RefreshToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link RefreshToken} entities.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Returns the refresh token record matching the given raw token hash.
     *
     * @param token hashed token value
     * @return an {@link Optional} containing the matching record, or empty
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Returns all active refresh tokens that belong to the specified user.
     *
     * @param userId the owner's UUID
     * @return list of refresh token records for this user
     */
    List<RefreshToken> findAllByUserId(UUID userId);

    /**
     * Deletes all refresh tokens belonging to the given user.
     * Used during logout-all-devices and account deletion flows.
     *
     * @param userId the owner's UUID
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);

    /**
     * Removes all refresh tokens whose expiry is before the given timestamp.
     * Intended for scheduled cleanup jobs.
     *
     * @param now the current instant; tokens with {@code expiresAt < now} are removed
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    void deleteAllExpiredBefore(@Param("now") Instant now);
}
