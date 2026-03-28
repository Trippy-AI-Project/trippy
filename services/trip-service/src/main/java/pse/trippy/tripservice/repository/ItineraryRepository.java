package pse.trippy.tripservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pse.trippy.tripservice.model.entity.Itinerary;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link Itinerary} entities.
 */
@Repository
public interface ItineraryRepository extends JpaRepository<Itinerary, UUID> {

    /**
     * Returns the itinerary for the given trip, if one exists.
     * Each trip has at most one itinerary (1:1 relationship).
     *
     * @param tripId the trip's UUID
     * @return an {@link Optional} containing the itinerary, or empty
     */
    Optional<Itinerary> findByTripId(UUID tripId);
}
