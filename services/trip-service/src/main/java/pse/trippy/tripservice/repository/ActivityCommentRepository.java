package pse.trippy.tripservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pse.trippy.tripservice.model.entity.ActivityComment;

import java.util.List;
import java.util.UUID;

public interface ActivityCommentRepository extends JpaRepository<ActivityComment, UUID> {

    List<ActivityComment> findByActivityIdOrderByCreatedAtAsc(UUID activityId);

    long countByActivityId(UUID activityId);
}
