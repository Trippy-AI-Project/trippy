package pse.trippy.tripservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pse.trippy.tripservice.model.entity.DayPlan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link DayPlan} entities.
 */
@Repository
public interface DayPlanRepository extends JpaRepository<DayPlan, UUID> {

    /**
     * Returns all day plans for the given itinerary, ordered by day number ascending.
     *
     * @param itineraryId the itinerary's UUID
     * @return ordered list of day plans
     */
    List<DayPlan> findByItineraryIdOrderByDayNumberAsc(UUID itineraryId);

    /**
     * Returns the day plan at the given day number within an itinerary, if it exists.
     *
     * @param itineraryId the itinerary's UUID
     * @param dayNumber   the 1-based day number
     * @return an {@link Optional} containing the day plan, or empty
     */
    Optional<DayPlan> findByItineraryIdAndDayNumber(UUID itineraryId, int dayNumber);
}
