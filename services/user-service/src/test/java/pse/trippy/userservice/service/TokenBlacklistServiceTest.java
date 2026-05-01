package pse.trippy.userservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TokenBlacklistService}.
 *
 * <p>All dependencies are mocked — no Redis connection required.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenBlacklistService")
class TokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        tokenBlacklistService = new TokenBlacklistService(redisTemplate);
    }

    // =========================================================================
    // blacklistToken
    // =========================================================================

    @Nested
    @DisplayName("blacklistToken")
    class BlacklistToken {

        @Test
        @DisplayName("stores token in Redis with correct key and TTL")
        void storesTokenWithTtl() {
            String jti = UUID.randomUUID().toString();
            long ttl = 600;

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            tokenBlacklistService.blacklistToken(jti, ttl);

            verify(valueOperations).set(
                    TokenBlacklistService.TOKEN_BLACKLIST_PREFIX + jti,
                    "1",
                    ttl,
                    TimeUnit.SECONDS
            );
        }

        @Test
        @DisplayName("does nothing when remaining seconds is zero")
        void skipsWhenZeroSeconds() {
            tokenBlacklistService.blacklistToken("some-jti", 0);

            verifyNoInteractions(redisTemplate);
        }

        @Test
        @DisplayName("does nothing when remaining seconds is negative")
        void skipsWhenNegativeSeconds() {
            tokenBlacklistService.blacklistToken("some-jti", -10);

            verifyNoInteractions(redisTemplate);
        }
    }

    // =========================================================================
    // blacklistUser
    // =========================================================================

    @Nested
    @DisplayName("blacklistUser")
    class BlacklistUser {

        @Test
        @DisplayName("stores user-level blacklist in Redis with correct key and TTL")
        void storesUserBlacklistWithTtl() {
            UUID userId = UUID.randomUUID();
            long ttl = 900;

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            tokenBlacklistService.blacklistUser(userId, ttl);

            verify(valueOperations).set(
                    TokenBlacklistService.USER_BLACKLIST_PREFIX + userId,
                    "1",
                    ttl,
                    TimeUnit.SECONDS
            );
        }
    }

    // =========================================================================
    // isTokenBlacklisted
    // =========================================================================

    @Nested
    @DisplayName("isTokenBlacklisted")
    class IsTokenBlacklisted {

        @Test
        @DisplayName("returns true when token key exists in Redis")
        void returnsTrueWhenBlacklisted() {
            String jti = UUID.randomUUID().toString();
            when(redisTemplate.hasKey(TokenBlacklistService.TOKEN_BLACKLIST_PREFIX + jti))
                    .thenReturn(Boolean.TRUE);

            assertThat(tokenBlacklistService.isTokenBlacklisted(jti)).isTrue();
        }

        @Test
        @DisplayName("returns false when token key does not exist in Redis")
        void returnsFalseWhenNotBlacklisted() {
            String jti = UUID.randomUUID().toString();
            when(redisTemplate.hasKey(TokenBlacklistService.TOKEN_BLACKLIST_PREFIX + jti))
                    .thenReturn(Boolean.FALSE);

            assertThat(tokenBlacklistService.isTokenBlacklisted(jti)).isFalse();
        }

        @Test
        @DisplayName("returns false when hasKey returns null")
        void returnsFalseWhenNull() {
            String jti = UUID.randomUUID().toString();
            when(redisTemplate.hasKey(TokenBlacklistService.TOKEN_BLACKLIST_PREFIX + jti))
                    .thenReturn(null);

            assertThat(tokenBlacklistService.isTokenBlacklisted(jti)).isFalse();
        }
    }

    // =========================================================================
    // isUserBlacklisted
    // =========================================================================

    @Nested
    @DisplayName("isUserBlacklisted")
    class IsUserBlacklisted {

        @Test
        @DisplayName("returns true when user key exists in Redis")
        void returnsTrueWhenBlacklisted() {
            UUID userId = UUID.randomUUID();
            when(redisTemplate.hasKey(TokenBlacklistService.USER_BLACKLIST_PREFIX + userId))
                    .thenReturn(Boolean.TRUE);

            assertThat(tokenBlacklistService.isUserBlacklisted(userId)).isTrue();
        }

        @Test
        @DisplayName("returns false when user key does not exist in Redis")
        void returnsFalseWhenNotBlacklisted() {
            UUID userId = UUID.randomUUID();
            when(redisTemplate.hasKey(TokenBlacklistService.USER_BLACKLIST_PREFIX + userId))
                    .thenReturn(Boolean.FALSE);

            assertThat(tokenBlacklistService.isUserBlacklisted(userId)).isFalse();
        }
    }
}
