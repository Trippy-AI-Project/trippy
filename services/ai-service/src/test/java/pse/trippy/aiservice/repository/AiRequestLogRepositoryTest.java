package pse.trippy.aiservice.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import pse.trippy.aiservice.model.entity.AiRequestLog;
import pse.trippy.aiservice.model.enums.RequestStatus;
import pse.trippy.aiservice.model.enums.RequestType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AiRequestLogRepositoryTest {

    @Autowired
    private AiRequestLogRepository aiRequestLogRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        aiRequestLogRepository.deleteAll();
        userId = UUID.randomUUID();

        aiRequestLogRepository.save(AiRequestLog.builder()
                .userId(userId)
                .requestType(RequestType.DESTINATION_SUGGESTION)
                .promptHash("abc123")
                .responseTimeMs(450L)
                .inputTokens(100)
                .outputTokens(200)
                .status(RequestStatus.SUCCESS)
                .build());

        aiRequestLogRepository.save(AiRequestLog.builder()
                .userId(userId)
                .requestType(RequestType.DESTINATION_SUGGESTION)
                .promptHash("def456")
                .responseTimeMs(320L)
                .inputTokens(80)
                .outputTokens(150)
                .status(RequestStatus.SUCCESS)
                .build());

        aiRequestLogRepository.save(AiRequestLog.builder()
                .userId(userId)
                .requestType(RequestType.ITINERARY_GENERATION)
                .promptHash("ghi789")
                .responseTimeMs(600L)
                .inputTokens(200)
                .outputTokens(400)
                .status(RequestStatus.SUCCESS)
                .build());
    }

    @Test
    void countByUserId_returnsTotalCount() {
        long count = aiRequestLogRepository.countByUserId(userId);
        assertThat(count).isEqualTo(3);
    }

    @Test
    void countByUserIdAndRequestType_returnsTypeCount() {
        long count = aiRequestLogRepository.countByUserIdAndRequestType(userId, RequestType.DESTINATION_SUGGESTION);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void sumTokensByUserId_returnsTotalTokens() {
        long tokens = aiRequestLogRepository.sumTokensByUserId(userId);
        // (100+200) + (80+150) + (200+400) = 1130
        assertThat(tokens).isEqualTo(1130);
    }

    @Test
    void findTopByUserIdOrderByCreatedAtDesc_returnsLatestEntry() {
        AiRequestLog latest = aiRequestLogRepository.findTopByUserIdOrderByCreatedAtDesc(userId).orElse(null);
        assertThat(latest).isNotNull();
        assertThat(latest.getUserId()).isEqualTo(userId);
    }

    @Test
    void countByUserId_returnsZeroForUnknownUser() {
        long count = aiRequestLogRepository.countByUserId(UUID.randomUUID());
        assertThat(count).isZero();
    }

    @Test
    void save_persistsWithDefaults() {
        AiRequestLog entry = AiRequestLog.builder()
                .userId(UUID.randomUUID())
                .requestType(RequestType.TRAVEL_ADVICE)
                .promptHash("test")
                .responseTimeMs(100L)
                .build();

        AiRequestLog saved = aiRequestLogRepository.save(entry);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(RequestStatus.SUCCESS);
        assertThat(saved.getCreatedAt()).isNotNull();
    }
}
