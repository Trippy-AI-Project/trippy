package pse.trippy.aiservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiCacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private AiCacheService aiCacheService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        aiCacheService = new AiCacheService(redisTemplate, objectMapper);
    }

    @Test
    void getCachedResponse_cacheHit_returnsValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ai:cache:suggestion:abc123")).thenReturn("{\"data\":\"cached\"}");

        Optional<String> result = aiCacheService.getCachedResponse("suggestion", "abc123");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("{\"data\":\"cached\"}");
    }

    @Test
    void getCachedResponse_cacheMiss_returnsEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ai:cache:itinerary:xyz789")).thenReturn(null);

        Optional<String> result = aiCacheService.getCachedResponse("itinerary", "xyz789");

        assertThat(result).isEmpty();
    }

    @Test
    void getCachedResponse_redisFailure_returnsEmptyGracefully() {
        when(redisTemplate.opsForValue()).thenThrow(
                new RedisConnectionFailureException("Connection refused"));

        Optional<String> result = aiCacheService.getCachedResponse("suggestion", "hash123");

        assertThat(result).isEmpty();
    }

    @Test
    void cacheResponse_storesValueWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Duration ttl = Duration.ofHours(1);

        aiCacheService.cacheResponse("suggestion", "abc123", "{\"data\":\"value\"}", ttl);

        verify(valueOperations).set("ai:cache:suggestion:abc123", "{\"data\":\"value\"}", ttl);
    }

    @Test
    void cacheResponse_redisFailure_handlesGracefully() {
        when(redisTemplate.opsForValue()).thenThrow(
                new RedisConnectionFailureException("Connection refused"));

        // Should not throw
        aiCacheService.cacheResponse("suggestion", "abc123", "{\"data\":\"value\"}", Duration.ofHours(1));
    }

    @Test
    void evictCache_deletesMatchingKeys() {
        Set<String> keys = Set.of("ai:cache:suggestion:a", "ai:cache:suggestion:b");
        when(redisTemplate.keys("ai:cache:suggestion:*")).thenReturn(keys);

        aiCacheService.evictCache("suggestion");

        verify(redisTemplate).delete(keys);
    }

    @Test
    void evictCache_noKeys_skipsDelete() {
        when(redisTemplate.keys("ai:cache:itinerary:*")).thenReturn(Set.of());

        aiCacheService.evictCache("itinerary");

        verify(redisTemplate, never()).delete(any(Set.class));
    }

    @Test
    void evictCache_redisFailure_handlesGracefully() {
        when(redisTemplate.keys(anyString())).thenThrow(
                new RedisConnectionFailureException("Connection refused"));

        // Should not throw
        aiCacheService.evictCache("suggestion");
    }

    @Test
    void generateHash_sameInput_producesSameHash() {
        String hash1 = aiCacheService.generateHash("test input");
        String hash2 = aiCacheService.generateHash("test input");

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).isNotEqualTo("unknown");
    }

    @Test
    void generateHash_differentInput_producesDifferentHash() {
        String hash1 = aiCacheService.generateHash("input one");
        String hash2 = aiCacheService.generateHash("input two");

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void generateHash_complexObject_producesConsistentHash() {
        record TestRequest(String name, int value) {}
        TestRequest request = new TestRequest("test", 42);

        String hash1 = aiCacheService.generateHash(request);
        String hash2 = aiCacheService.generateHash(request);

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64); // SHA-256 hex is 64 chars
    }
}
