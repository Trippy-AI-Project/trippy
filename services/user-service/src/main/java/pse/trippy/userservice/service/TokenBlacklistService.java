package pse.trippy.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service managing JWT token blacklisting via Redis.
 *
 * <p>Blacklisted tokens are stored with a TTL matching their remaining lifetime,
 * ensuring automatic eviction by Redis once the token would have expired anyway.
 *
 * <p>Key patterns:
 * <ul>
 *   <li>{@code blacklist:token:{jti}} – single token blacklist entry</li>
 *   <li>{@code blacklist:user:{userId}} – user-level blacklist (all-devices logout)</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TokenBlacklistService {

    static final String TOKEN_BLACKLIST_PREFIX = "blacklist:token:";
    static final String USER_BLACKLIST_PREFIX = "blacklist:user:";
    private static final String BLACKLISTED_VALUE = "1";

    private final StringRedisTemplate redisTemplate;

    /**
     * Blacklists a single access token by its JTI.
     *
     * @param jti              the JWT ID claim
     * @param remainingSeconds seconds until the token expires
     */
    public void blacklistToken(String jti, long remainingSeconds) {
        if (remainingSeconds <= 0) {
            return;
        }
        String key = TOKEN_BLACKLIST_PREFIX + jti;
        redisTemplate.opsForValue().set(key, BLACKLISTED_VALUE, remainingSeconds, TimeUnit.SECONDS);
        log.info("Blacklisted token jti={} for {}s", jti, remainingSeconds);
    }

    /**
     * Blacklists all tokens for a user (used for logout-all-devices).
     *
     * @param userId                 the user's UUID
     * @param maxTokenLifetimeSeconds TTL for the blacklist entry
     */
    public void blacklistUser(UUID userId, long maxTokenLifetimeSeconds) {
        String key = USER_BLACKLIST_PREFIX + userId;
        redisTemplate.opsForValue().set(key, BLACKLISTED_VALUE, maxTokenLifetimeSeconds, TimeUnit.SECONDS);
        log.info("Blacklisted all tokens for userId={} for {}s", userId, maxTokenLifetimeSeconds);
    }

    /**
     * Checks whether a specific token JTI is blacklisted.
     *
     * @param jti the JWT ID claim
     * @return {@code true} if the token is blacklisted
     */
    public boolean isTokenBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_BLACKLIST_PREFIX + jti));
    }

    /**
     * Checks whether all tokens for a user are blacklisted.
     *
     * @param userId the user's UUID
     * @return {@code true} if the user's tokens are blacklisted
     */
    public boolean isUserBlacklisted(UUID userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(USER_BLACKLIST_PREFIX + userId));
    }
}
