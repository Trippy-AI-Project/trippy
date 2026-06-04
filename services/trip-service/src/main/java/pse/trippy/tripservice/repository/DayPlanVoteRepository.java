package pse.trippy.tripservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import pse.trippy.tripservice.model.entity.DayPlanVote;
import pse.trippy.tripservice.model.enums.VoteType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DayPlanVoteRepository extends JpaRepository<DayPlanVote, UUID> {

    Optional<DayPlanVote> findByDayPlanIdAndUserId(UUID dayPlanId, UUID userId);

    List<DayPlanVote> findByDayPlanId(UUID dayPlanId);

    long countByDayPlanIdAndVoteType(UUID dayPlanId, VoteType voteType);

    @Modifying
    void deleteByDayPlanIdAndUserId(UUID dayPlanId, UUID userId);

    @Modifying
    void deleteAllByDayPlanId(UUID dayPlanId);
}
