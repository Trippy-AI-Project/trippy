package pse.trippy.userservice.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Stores email verification tokens for users, persisted in {@code user_schema.email_verification_tokens}.
 *
 * <p>Each token has a 24-hour expiry window. A unique index on the {@code token}
 * column prevents duplicate tokens.
 */
@Entity
@Table(
        name = "email_verification_tokens",
        schema = "user_schema",
        indexes = @Index(name = "idx_email_verification_tokens_token", columnList = "token", unique = true)
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank
    @Column(name = "token", unique = true, nullable = false)
    private String token;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Sets {@code createdAt} before the first persist. */
    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }

    /** Returns {@code true} when this token has passed its expiry time. */
    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }
}
