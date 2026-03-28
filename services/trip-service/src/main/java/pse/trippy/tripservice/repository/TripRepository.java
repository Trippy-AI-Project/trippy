package pse.trippy.tripservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pse.trippy.tripservice.model.entity.Trip;
import pse.trippy.tripservice.model.enums.TripStatus;
import pse.trippy.tripservice.model.enums.TripVisibility;

import java.util.List;
import java.util.UUID;

/**
 * JPA repository for {@link Trip} entities.
 */
@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {

    /**
     * Returns all trips created by the given user.
     *
     * @param createdBy the UUID of the trip creator
     * @return list of trips owned by this user
     */
    List<Trip> findByCreatedBy(UUID createdBy);

    /**
     * Returns all trips with the given status.
     *
     * @param status the trip lifecycle status to filter by
     * @return list of trips matching the status
     */
    List<Trip> findByStatus(TripStatus status);

    /**
     * Returns all trips with the given visibility level.
     * Primarily used to fetch all {@link TripVisibility#PUBLIC} trips for the discovery feed.
     *
     * @param visibility the visibility level
     * @return list of trips with that visibility
     */
    List<Trip> findByVisibility(TripVisibility visibility);

    /**
     * Returns all trips created by the given user that have a specific status.
     *
     * @param createdBy the UUID of the trip creator
     * @param status    the trip lifecycle status
     * @return list of matching trips
     */
    List<Trip> findByCreatedByAndStatus(UUID createdBy, TripStatus status);
}
