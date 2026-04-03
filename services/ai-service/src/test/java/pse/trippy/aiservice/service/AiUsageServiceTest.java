package pse.trippy.aiservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pse.trippy.aiservice.dto.response.AiUsageResponse;
import pse.trippy.aiservice.model.entity.AiRequestLog;
import pse.trippy.aiservice.model.enums.RequestStatus;
import pse.trippy.aiservice.model.enums.RequestType;
import pse.trippy.aiservice.repository.AiRequestLogRepository;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiUsageServiceTest {

    @Mock
    private AiRequestLogRepository aiRequestLogRepository;

    @InjectMocks
    private AiUsageService aiUsageService;

    @Test
    void getUsage_returnsAggregatedStats() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        AiRequestLog latest = AiRequestLog.builder()
                .userId(userId)
                .requestType(RequestType.DESTINATION_SUGGESTION)
                .status(RequestStatus.SUCCESS)
                .createdAt(now)
                .build();

        when(aiRequestLogRepository.countByUserId(userId)).thenReturn(42L);
        when(aiRequestLogRepository.countByUserIdAndRequestType(userId, RequestType.DESTINATION_SUGGESTION)).thenReturn(30L);
        when(aiRequestLogRepository.countByUserIdAndRequestType(userId, RequestType.ITINERARY_GENERATION)).thenReturn(8L);
        when(aiRequestLogRepository.countByUserIdAndRequestType(userId, RequestType.TRAVEL_ADVICE)).thenReturn(4L);
        when(aiRequestLogRepository.countByUserIdAndRequestType(userId, RequestType.PREFERENCE_CONSOLIDATION)).thenReturn(0L);
        when(aiRequestLogRepository.sumTokensByUserId(userId)).thenReturn(15420L);
        when(aiRequestLogRepository.findLatestByUserId(userId)).thenReturn(latest);

        AiUsageResponse usage = aiUsageService.getUsage(userId);

        assertThat(usage.userId()).isEqualTo(userId);
        assertThat(usage.totalRequests()).isEqualTo(42);
        assertThat(usage.requestsByType()).containsEntry("DESTINATION_SUGGESTION", 30L);
        assertThat(usage.requestsByType()).containsEntry("ITINERARY_GENERATION", 8L);
        assertThat(usage.requestsByType()).containsEntry("TRAVEL_ADVICE", 4L);
        assertThat(usage.requestsByType()).doesNotContainKey("PREFERENCE_CONSOLIDATION");
        assertThat(usage.tokensConsumed()).isEqualTo(15420);
        assertThat(usage.lastUsedAt()).isEqualTo(now);
    }

    @Test
    void getUsage_noLogs_returnsZeros() {
        UUID userId = UUID.randomUUID();

        when(aiRequestLogRepository.countByUserId(userId)).thenReturn(0L);
        when(aiRequestLogRepository.countByUserIdAndRequestType(userId, RequestType.DESTINATION_SUGGESTION)).thenReturn(0L);
        when(aiRequestLogRepository.countByUserIdAndRequestType(userId, RequestType.ITINERARY_GENERATION)).thenReturn(0L);
        when(aiRequestLogRepository.countByUserIdAndRequestType(userId, RequestType.TRAVEL_ADVICE)).thenReturn(0L);
        when(aiRequestLogRepository.countByUserIdAndRequestType(userId, RequestType.PREFERENCE_CONSOLIDATION)).thenReturn(0L);
        when(aiRequestLogRepository.sumTokensByUserId(userId)).thenReturn(0L);
        when(aiRequestLogRepository.findLatestByUserId(userId)).thenReturn(null);

        AiUsageResponse usage = aiUsageService.getUsage(userId);

        assertThat(usage.totalRequests()).isZero();
        assertThat(usage.requestsByType()).isEmpty();
        assertThat(usage.lastUsedAt()).isNull();
        assertThat(usage.tokensConsumed()).isZero();
    }
}
