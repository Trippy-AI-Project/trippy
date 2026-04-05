package pse.trippy.tripservice.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pse.trippy.tripservice.model.enums.TripStatus;
import pse.trippy.tripservice.model.enums.TripVisibility;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Core trip entity persisted in {@code trip_schema.trips}.
 *
 * <p>{@code endDate} must be on or after {@code startDate}; this invariant is
 * enforced at the service layer. Timestamps are managed automatically via
 * JPA lifecycle callbacks.
 */
@Entity
@Table(name = "trips", schema = "trip_schema")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank
    @Size(max = 200)
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @NotBlank
    @Column(name = "destination", nullable = false, length = 500)
    private String destination;

    @Size(max = 2000)
    @Column(name = "description", length = 2000)
    private String description;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TripStatus status = TripStatus.DRAFT;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    @Builder.Default
    private TripVisibility visibility = TripVisibility.PRIVATE;

    @Min(1)
    @Max(20)
    @Column(name = "max_participants", nullable = false)
    @Builder.Default
    private int maxParticipants = 20;

    @Column(name = "cover_image_url", length = 2048)
    private String coverImageUrl;

    @NotNull
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Sets {@code createdAt} and {@code updatedAt} before the first persist. */
    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Refreshes {@code updatedAt} on every subsequent update. */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
