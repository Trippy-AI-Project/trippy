package pse.trippy.aiservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pse.trippy.aiservice.model.entity.AiRequestLog;
import pse.trippy.aiservice.model.enums.RequestType;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiRequestLogRepository extends JpaRepository<AiRequestLog, UUID> {

    long countByUserId(UUID userId);

    long countByUserIdAndRequestType(UUID userId, RequestType requestType);

    @Query("SELECT COALESCE(SUM(a.inputTokens + a.outputTokens), 0) FROM AiRequestLog a WHERE a.userId = :userId")
    long sumTokensByUserId(@Param("userId") UUID userId);

    @Query("SELECT a FROM AiRequestLog a WHERE a.userId = :userId ORDER BY a.createdAt DESC LIMIT 1")
    AiRequestLog findLatestByUserId(@Param("userId") UUID userId);

    List<AiRequestLog> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
