package pse.trippy.aiservice.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pse.trippy.aiservice.model.enums.RequestStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "generation_history",
        schema = "ai_schema",
        indexes = {
                @Index(name = "idx_generation_history_user_id", columnList = "user_id"),
                @Index(name = "idx_generation_history_trip_id", columnList = "trip_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationHistory {

    @Id
    @Column(name = "generation_id", updatable = false, nullable = false)
    private UUID generationId;

    @Column(name = "trip_id")
    private UUID tripId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "prompt_hash", length = 64, nullable = false)
    private String promptHash;

    @Lob
    @Column(name = "response_json")
    private String responseJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RequestStatus status;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (generationId == null) {
            generationId = UUID.randomUUID();
        }
        createdAt = Instant.now();
    }
}
