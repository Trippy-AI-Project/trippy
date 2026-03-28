package pse.trippy.tripservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pse.trippy.tripservice.model.entity.Activity;

import java.util.List;
import java.util.UUID;

/**
 * JPA repository for {@link Activity} entities.
 */
@Repository
public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    /**
     * Returns all activities within the given day plan, ordered by {@code orderIndex} ascending.
     *
     * @param dayPlanId the day plan's UUID
     * @return ordered list of activities
     */
    List<Activity> findByDayPlanIdOrderByOrderIndexAsc(UUID dayPlanId);

    /**
     * Deletes all activities belonging to the given day plan.
     * Used when a day plan is removed from the itinerary.
     *
     * @param dayPlanId the day plan's UUID
     */
    @Modifying
    @Query("DELETE FROM Activity a WHERE a.dayPlan.id = :dayPlanId")
    void deleteAllByDayPlanId(@Param("dayPlanId") UUID dayPlanId);
}
