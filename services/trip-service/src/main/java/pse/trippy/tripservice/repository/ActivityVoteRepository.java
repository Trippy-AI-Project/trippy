package pse.trippy.tripservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import pse.trippy.tripservice.model.entity.ActivityVote;
import pse.trippy.tripservice.model.enums.VoteType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActivityVoteRepository extends JpaRepository<ActivityVote, UUID> {

    Optional<ActivityVote> findByActivityIdAndUserId(UUID activityId, UUID userId);

    List<ActivityVote> findByActivityId(UUID activityId);

    long countByActivityIdAndVoteType(UUID activityId, VoteType voteType);

    @Modifying
    void deleteByActivityIdAndUserId(UUID activityId, UUID userId);

    @Modifying
    void deleteAllByActivityId(UUID activityId);
}
