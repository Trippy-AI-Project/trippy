package pse.trippy.aiservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import pse.trippy.aiservice.logging.LogSanitizer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiCacheService {

    private static final String KEY_PREFIX = "ai:cache:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<String> getCachedResponse(String type, String requestHash) {
        try {
            String key = buildKey(type, requestHash);
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.debug("AI cache hit type={} hash={}", type, LogSanitizer.shortHash(requestHash));
                return Optional.of(value);
            }
            log.debug("AI cache miss type={} hash={}", type, LogSanitizer.shortHash(requestHash));
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("Redis read failed type={} hash={} error={}",
                    type, LogSanitizer.shortHash(requestHash), LogSanitizer.safeError(ex));
            return Optional.empty();
        }
    }

    public void cacheResponse(String type, String requestHash, String jsonResponse, Duration ttl) {
        try {
            String key = buildKey(type, requestHash);
            redisTemplate.opsForValue().set(key, jsonResponse, ttl);
            log.debug("AI response cached type={} hash={} ttl={}",
                    type, LogSanitizer.shortHash(requestHash), ttl);
        } catch (Exception ex) {
            log.warn("Redis write failed type={} hash={} error={}",
                    type, LogSanitizer.shortHash(requestHash), LogSanitizer.safeError(ex));
        }
    }

    public void evictCache(String type) {
        try {
            String pattern = KEY_PREFIX + type + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Evicted {} cache entries for type: {}", keys.size(), type);
            }
        } catch (Exception ex) {
            log.warn("Redis eviction failed type={} error={}", type, LogSanitizer.safeError(ex));
        }
    }

    public String generateHash(Object request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
            log.warn("Hash generation failed error={}", LogSanitizer.safeError(ex));
            return "unknown";
        }
    }

    private String buildKey(String type, String requestHash) {
        return KEY_PREFIX + type + ":" + requestHash;
    }
}
