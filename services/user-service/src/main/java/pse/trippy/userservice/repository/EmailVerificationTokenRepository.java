package pse.trippy.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pse.trippy.userservice.model.entity.EmailVerificationToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link EmailVerificationToken} entities.
 */
@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    /**
     * Returns the verification token matching the given token string.
     */
    Optional<EmailVerificationToken> findByToken(String token);

    /**
     * Returns the most recent verification token for the given user.
     */
    Optional<EmailVerificationToken> findByUserId(UUID userId);

    /**
     * Deletes all verification tokens for the given user.
     */
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.user.id = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);

    /**
     * Deletes all tokens that have expired before the given timestamp.
     */
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.expiresAt < :now")
    void deleteAllExpiredBefore(@Param("now") Instant now);
}
