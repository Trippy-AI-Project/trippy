package pse.trippy.aiservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pse.trippy.aiservice.dto.response.AiUsageResponse;
import pse.trippy.aiservice.model.entity.AiRequestLog;
import pse.trippy.aiservice.model.enums.RequestType;
import pse.trippy.aiservice.repository.AiRequestLogRepository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiUsageService {

    private final AiRequestLogRepository aiRequestLogRepository;

    public AiUsageResponse getUsage(UUID userId) {
        long totalRequests = aiRequestLogRepository.countByUserId(userId);

        Map<String, Long> requestsByType = new LinkedHashMap<>();
        for (RequestType type : RequestType.values()) {
            long count = aiRequestLogRepository.countByUserIdAndRequestType(userId, type);
            if (count > 0) {
                requestsByType.put(type.name(), count);
            }
        }

        long tokensConsumed = aiRequestLogRepository.sumTokensByUserId(userId);

        AiRequestLog latest = aiRequestLogRepository.findTopByUserIdOrderByCreatedAtDesc(userId).orElse(null);

        return new AiUsageResponse(
                userId,
                totalRequests,
                requestsByType,
                latest != null ? latest.getCreatedAt() : null,
                tokensConsumed
        );
    }
}
