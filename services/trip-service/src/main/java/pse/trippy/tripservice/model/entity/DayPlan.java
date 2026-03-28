package pse.trippy.tripservice.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a single day within an itinerary, persisted in {@code trip_schema.day_plans}.
 */
@Entity
@Table(
        name = "day_plans",
        schema = "trip_schema",
        indexes = @Index(name = "idx_day_plans_itinerary_id", columnList = "itinerary_id")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "itinerary_id", nullable = false, updatable = false)
    private Itinerary itinerary;

    @Min(1)
    @Column(name = "day_number", nullable = false)
    private int dayNumber;

    @Column(name = "date")
    private LocalDate date;

    @Size(max = 200)
    @Column(name = "title", length = 200)
    private String title;
}
