package pse.trippy.userservice.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisHealthIndicator implements HealthIndicator {
    private final StringRedisTemplate redisTemplate;

    public RedisHealthIndicator(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Health health() {
        try {
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            if ("PONG".equalsIgnoreCase(pong)) {
                return Health.up().withDetail("redis", "Redis is up").build();
            } else {
                return Health.down().withDetail("redis", "Redis ping failed").build();
            }
        } catch (Exception ex) {
            return Health.down(ex).withDetail("redis", "Redis is down").build();
        }
    }
}package pse.trippy.userservice.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisHealthIndicator implements HealthIndicator {
    private final StringRedisTemplate redisTemplate;

    public RedisHealthIndicator(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Health health() {
        try {
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            if ("PONG".equalsIgnoreCase(pong)) {
                return Health.up().withDetail("redis", "Redis is up").build();
            } else {
                return Health.down().withDetail("redis", "Redis ping failed").build();
            }
        } catch (Exception ex) {
            return Health.down(ex).withDetail("redis", "Redis is down").build();
        }
    }
}
