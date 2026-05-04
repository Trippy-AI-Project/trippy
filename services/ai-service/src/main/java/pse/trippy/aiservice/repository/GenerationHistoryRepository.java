package pse.trippy.aiservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pse.trippy.aiservice.model.entity.GenerationHistory;

import java.util.UUID;

@Repository
public interface GenerationHistoryRepository extends JpaRepository<GenerationHistory, UUID> {
}
